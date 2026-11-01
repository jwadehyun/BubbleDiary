package com.example;

/** What a user's subscription gets them: how many fans of each kind reply, and how often they can post. */
public enum Plan {
    //      generic fans, AI fans, generic antis, AI antis, posts per day
    FREE(80, 15, 4, 1, 5),
    PREMIUM(200, 30, 0, 0, Integer.MAX_VALUE);

    private final int genericFans;
    private final int aiFans;
    private final int genericAntis;
    private final int aiAntis;
    private final int postsPerDay;

    Plan(int genericFans, int aiFans, int genericAntis, int aiAntis, int postsPerDay) {
        this.genericFans = genericFans;
        this.aiFans = aiFans;
        this.genericAntis = genericAntis;
        this.aiAntis = aiAntis;
        this.postsPerDay = postsPerDay;
    }

    public int genericFans() { return genericFans; }
    public int aiFans() { return aiFans; }
    public int genericAntis() { return genericAntis; }
    public int aiAntis() { return aiAntis; }
    public int postsPerDay() { return postsPerDay; }

    public int aiReplies() { return aiFans + aiAntis; }

    public String json() {
        return name().toLowerCase();
    }
}
