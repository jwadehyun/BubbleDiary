package com.example;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/** Fan usernames and the quick generic reactions (from fans and anti-fans) that fill out the feed without AI. */
public final class Fans {

    private static final List<String> FIRST = List.of("star", "moon", "luv", "honey", "peach", "cloud", "mochi",
            "sunny", "berry", "dream", "sugar", "bunny", "cherry", "milk", "soft", "tiny", "angel", "daisy", "lemon",
            "kiwi");
    private static final List<String> SECOND = List.of("lover", "stan", "heart", "baby", "fairy", "bear", "girl",
            "boy", "pie", "cat", "bun", "dust", "light", "blossom", "drop", "tea");

    private static final List<String> ENGLISH = List.of(
            "😭😭😭", "ILY", "omg hiii", "first!!", "🥹🥹", "CRYING", "notified!!", "i love you sm", "ahhh", "💜💜💜",
            "hii bestie", "proud of you", "my comfort person", "🫶", "screaming", "eat well!!", "drink water pls",
            "stay healthy 🥺", "you deserve the world", "ok but why is this so cute", "my heart", "HELLO??",
            "we love you", "ㅠㅠㅠ", "saranghae 💕", "rest well!!", "bestie update yay", "thank you for sharing",
            "missed you", "can't stop smiling", "icon", "✨✨");
    private static final List<String> KOREAN = List.of(
            "ㅠㅠㅠㅠ", "사랑해!!", "헐 대박", "보고싶었어 ㅠㅠ", "밥 꼭 챙겨먹어", "귀여워 미쳐", "🥹🥹", "항상 응원해", "오늘도 고마워",
            "푹 쉬어!!", "❤️❤️❤️", "ㅋㅋㅋㅋㅋ", "너무 좋다", "감기 조심해", "최고야", "심장 아파", "알림 보고 바로 왔어", "꿀잠 자",
            "사랑해 사랑해", "행복하자", "울컥했어", "천사다", "헉", "내 비타민 ✨", "오늘 하루도 수고했어", "진짜 귀엽다");

    // Anti-fans are snarky, never cruel: no slurs and nothing about looks, bodies or self-harm.
    private static final List<String> ENGLISH_ANTI = List.of(
            "who asked", "mid", "flop era fr", "this is so boring", "ok and?", "nobody cares tbh", "not this again 🙄",
            "overrated", "the bar is on the floor", "skipped", "zzz", "and this is news because?", "pass",
            "they're trying way too hard");
    private static final List<String> KOREAN_ANTI = List.of(
            "그래서 어쩌라고", "노잼", "관심 없음", "또 이거야?", "별로다", "ㅋ 그래", "패스", "지루해", "이게 왜 인기임?",
            "그냥 그래");

    private Fans() {
    }

    public static String randomName() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double style = rnd.nextDouble();
        if (style < 0.4) return pick(FIRST) + pick(SECOND) + rnd.nextInt(999);
        if (style < 0.7) return pick(FIRST) + "_" + pick(SECOND);
        return pick(FIRST) + "." + pick(FIRST) + (rnd.nextBoolean() ? "♡" : "");
    }

    /** The generic fan and anti-fan reactions the user's plan gets. */
    public static List<Reply> filler(Message message, Plan plan, FanVoice voice) {
        List<String> fans = switch (voice) {
            case ENGLISH -> ENGLISH;
            case KOREAN -> KOREAN;
            case MIXED -> concat(ENGLISH, KOREAN);
        };
        List<String> antis = switch (voice) {
            case ENGLISH -> ENGLISH_ANTI;
            case KOREAN -> KOREAN_ANTI;
            case MIXED -> concat(ENGLISH_ANTI, KOREAN_ANTI);
        };
        return Stream.concat(
                        replies(message, fans, plan.genericFans(), ReplyKind.FAN),
                        replies(message, antis, plan.genericAntis(), ReplyKind.ANTI))
                .toList();
    }

    private static Stream<Reply> replies(Message message, List<String> pool, int count, ReplyKind kind) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return Stream.generate(() -> new Reply(
                        message,
                        randomName(),
                        pick(pool),
                        // Spread over the first ~10 minutes, weighted toward the start like a real notification rush.
                        spread(message.getCreatedAt(), Math.pow(rnd.nextDouble(), 2)),
                        // Nobody likes the haters much.
                        (int) (Math.pow(rnd.nextDouble(), 3) * (kind == ReplyKind.ANTI ? 3 : 40)),
                        false,
                        kind))
                .limit(count);
    }

    /** A time up to 10 minutes after the post; fraction is 0..1. */
    public static Instant spread(Instant start, double fraction) {
        return start.plusMillis((long) (fraction * 600_000));
    }

    private static <T> T pick(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static List<String> concat(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }
}
