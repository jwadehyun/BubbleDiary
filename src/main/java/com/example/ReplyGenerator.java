package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes AI fan replies in the background. The front end polls the message until its status leaves PENDING,
 * and the reply count ticks up as each batch is saved.
 */
@Component
public class ReplyGenerator {
    private static final Logger log = LoggerFactory.getLogger(ReplyGenerator.class);
    // Free models allow few tokens/requests, so ask for replies in modest chunks.
    private static final int BATCH_SIZE = 25;

    private static final Map<FanVoice, String> VOICE_GUIDE = Map.of(
            FanVoice.ENGLISH, "Write in casual English with K-pop fandom flavor: occasional Korean words in romanization or hangul (saranghae, unnie/oppa, ㅠㅠ, ㅋㅋㅋ), stan-Twitter energy, emoji.",
            FanVoice.KOREAN, "Write mainly in casual Korean (반말/fan-style), like real fans on the Bubble app. Use ㅠㅠ, ㅋㅋㅋ, emoji. A few replies may be in English.",
            FanVoice.MIXED, "Fans are global: mix English, Korean, and a few other languages (Spanish, Portuguese, Japanese, Indonesian, Thai, etc.).");

    /** Everything needed to write replies for one message, read up front so no database session stays open. */
    public record Job(Long messageId, String text, String imageDataUrl, List<String> history, String displayName,
                      String fandom, FanVoice voice, String model, int replyCount) {}

    public record FanReply(String name, String text) {}

    private final MessageService messages;
    private final OpenRouterClient openRouter;
    private final JsonMapper json;

    public ReplyGenerator(MessageService messages, OpenRouterClient openRouter, JsonMapper json) {
        this.messages = messages;
        this.openRouter = openRouter;
        this.json = json;
    }

    /** A restart stops any generation that was running, so don't leave those messages stuck as pending. */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterrupted() {
        messages.failAllPending("Interrupted. Tap retry.");
    }

    @Async
    public void generate(Long messageId) {
        Job job = messages.prepareJob(messageId).orElse(null);
        if (job == null) return;
        try {
            run(job);
            messages.finish(messageId, MessageStatus.DONE, null);
        } catch (RuntimeException e) {
            log.warn("AI replies failed for message {}: {}", messageId, e.getMessage());
            messages.finish(messageId, MessageStatus.ERROR, e.getMessage());
        }
    }

    private void run(Job job) {
        if (!openRouter.isConfigured()) {
            throw new IllegalStateException("The server has no OpenRouter API key. Set OPENROUTER_API_KEY in .env.");
        }
        var model = openRouter.resolve(job.model());
        String modelId = model.map(OpenRouterClient.ModelInfo::id).orElse(job.model());
        boolean vision = model.map(OpenRouterClient.ModelInfo::vision).orElse(false);

        int made = 0;
        while (made < job.replyCount()) {
            int n = Math.min(BATCH_SIZE, job.replyCount() - made);
            List<FanReply> batch;
            try {
                String raw = openRouter.chat(modelId, systemPrompt(job, n), userContent(job, vision));
                batch = parseReplies(raw);
            } catch (RuntimeException e) {
                if (made > 0) return; // partial success is still a good post
                throw e;
            }
            if (batch.isEmpty()) break;
            // Stop if the user deleted the message meanwhile.
            if (!messages.saveAiReplies(job.messageId(), batch)) return;
            made += batch.size();
        }
    }

    private static String systemPrompt(Job job, int count) {
        return """
                You simulate the fans of "%s", a beloved celebrity, on a private fan messaging app like Bubble. \
                The fandom is called "%s". %s just posted a diary-style message (and maybe a photo), and fans are replying.

                Write exactly %d distinct fan replies.
                - %s
                - React specifically to what they wrote or showed: details, feelings, questions back, advice, inside jokes, compliments, concern for their wellbeing.
                - Vary length (a few words up to 2-3 sentences), tone (excited, soft, funny, dramatic, caring), and style (lowercase, CAPS, emoji spam).
                - Keep it warm and supportive. No hate, no creepy or sexual content.
                - Each fan gets a cute, realistic username.

                Respond with ONLY a JSON array, no prose, no markdown fences:
                [{"name": "username", "text": "reply"}, ...]"""
                .formatted(job.displayName(), job.fandom(), job.displayName(), count, VOICE_GUIDE.get(job.voice()));
    }

    private static Object userContent(Job job, boolean vision) {
        StringBuilder text = new StringBuilder();
        if (!job.history().isEmpty()) {
            text.append("Recent earlier posts for context (oldest first):\n");
            job.history().forEach(h -> text.append("- ").append(h == null || h.isBlank() ? "(photo)" : h).append('\n'));
            text.append('\n');
        }
        text.append("New post:\n").append(job.text().isBlank() ? "(no caption)" : job.text());
        if (job.imageDataUrl() != null && !vision) {
            text.append("\n[They also attached a photo, which you cannot see. Fans react to it in general terms.]");
        }
        if (job.imageDataUrl() != null && vision) {
            return List.of(
                    Map.of("type", "text", "text", text.toString()),
                    Map.of("type", "image_url", "image_url", Map.of("url", job.imageDataUrl())));
        }
        return text.toString();
    }

    /** Models often wrap JSON in fences or add chatter, so pull out the array leniently. */
    List<FanReply> parseReplies(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start == -1 || end <= start) throw new IllegalStateException("Model did not return a JSON list");
        JsonNode arr;
        try {
            arr = json.readTree(raw.substring(start, end + 1));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Model returned invalid JSON");
        }
        if (!arr.isArray()) throw new IllegalStateException("Model did not return a JSON list");
        List<FanReply> replies = new ArrayList<>();
        for (JsonNode r : arr) {
            String name = r.isString() ? "" : r.path("name").asString("");
            String text = (r.isString() ? r.asString() : r.path("text").asString("")).trim();
            if (text.isEmpty()) continue;
            name = name.replaceFirst("^@", "").trim();
            replies.add(new FanReply(name.isEmpty() ? Fans.randomName() : clip(name, 60), clip(text, 2000)));
        }
        return replies;
    }

    private static String clip(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
