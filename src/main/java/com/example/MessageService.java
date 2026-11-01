package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageService {
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TEXT = 5000;

    private final MessageRepository messageRepo;
    private final ReplyRepository replyRepo;

    public MessageService(MessageRepository messageRepo, ReplyRepository replyRepo) {
        this.messageRepo = messageRepo;
        this.replyRepo = replyRepo;
    }

    public List<MessageSummary> list(User user) {
        return messageRepo.findSummaries(user);
    }

    public MessageSummary get(Long id, User user) {
        return messageRepo.findSummary(id, user).orElseThrow(() -> new MessageNotFoundException(id));
    }

    public Message getWithImage(Long id, User user) {
        return messageRepo.findByMessageIdAndUser(id, user).orElseThrow(() -> new MessageNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ReplyView> replies(Long id, User user) {
        get(id, user);
        return replyRepo.findByMessageId(id).stream().map(ReplyView::of).toList();
    }

    /** Saves the message with its generic replies. The caller then starts the AI replies. */
    @Transactional
    public Long create(User user, CreateMessageRequest request) {
        String text = request.getText() == null ? "" : request.getText().trim();
        String image = request.getImage();
        if (text.isEmpty() && (image == null || image.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Write something or add a photo");
        }
        if (text.length() > MAX_TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diary entries are limited to " + MAX_TEXT + " characters");
        }
        Message message = new Message(text, Instant.now(), user);
        if (image != null && !image.isBlank()) attachImage(message, image);
        // Nothing for the AI to do when the user turned AI replies off.
        if (user.getReplyCount() == 0) message.setStatus(MessageStatus.DONE);
        messageRepo.save(message);
        replyRepo.saveAll(Fans.filler(message, user.getFillerCount(), user.getVoice()));
        return message.getMessageId();
    }

    /** Clears earlier AI replies (keeping the generic ones) so they can be written again. */
    @Transactional
    public void resetForRetry(Long id, User user) {
        Message message = getWithImage(id, user);
        replyRepo.deleteAiReplies(id);
        message.setStatus(MessageStatus.PENDING);
        message.setError(null);
    }

    @Transactional
    public void delete(Long id, User user) {
        if (messageRepo.deleteOwned(id, user) == 0) throw new MessageNotFoundException(id);
    }

    @Transactional
    public void deleteAll(User user) {
        messageRepo.deleteAllOwned(user);
    }

    // Used by ReplyGenerator

    @Transactional(readOnly = true)
    public Optional<ReplyGenerator.Job> prepareJob(Long id) {
        return messageRepo.findById(id).map(m -> {
            User u = m.getUser();
            String image = m.getImage() == null ? null
                    : "data:" + m.getImageType() + ";base64," + Base64.getEncoder().encodeToString(m.getImage());
            List<String> history = messageRepo.findRecentTexts(u, m.getCreatedAt()).reversed();
            return new ReplyGenerator.Job(id, m.getText() == null ? "" : m.getText(), image, history,
                    u.getDisplayName(), u.getFandom(), u.getVoice(), u.getModel(), u.getReplyCount());
        });
    }

    /** Returns false when the message no longer exists. */
    @Transactional
    public boolean saveAiReplies(Long id, List<ReplyGenerator.FanReply> batch) {
        Optional<Message> message = messageRepo.findById(id);
        if (message.isEmpty()) return false;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Instant start = message.get().getCreatedAt();
        replyRepo.saveAll(batch.stream()
                .map(r -> new Reply(message.get(), r.name(), r.text(), Fans.spread(start, rnd.nextDouble()),
                        (int) (Math.pow(rnd.nextDouble(), 2) * 300), true))
                .toList());
        return true;
    }

    @Transactional
    public void finish(Long id, MessageStatus status, String error) {
        messageRepo.findById(id).ifPresent(m -> {
            m.setStatus(status);
            m.setError(error == null ? null : error.length() > 1000 ? error.substring(0, 1000) : error);
        });
    }

    @Transactional
    public void failAllPending(String error) {
        messageRepo.updateAllStatus(MessageStatus.PENDING, MessageStatus.ERROR, error);
    }

    private static void attachImage(Message message, String dataUrl) {
        Matcher m = DATA_URL.matcher(dataUrl);
        if (!m.matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be an image data URL");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo data is not valid base64");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is too large (max 5 MB)");
        }
        message.setImage(bytes);
        message.setImageType(m.group(1));
    }
}
