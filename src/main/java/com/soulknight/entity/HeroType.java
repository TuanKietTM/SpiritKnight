package com.soulknight.entity;

public enum HeroType {

    KNIGHT(
            "Knight",
            "/assets/sprites/Knight_IDLE.png",
            "/assets/sprites/K_RUN.png",
            0,
            100,
            100,
            6,
            64,
            64,
            0.15
    ),
    RED_NIGHT(
            "UETot",
            "/assets/sprites/UET_IDLE.png",
            "/assets/sprites/UET_RUN.png",
            0,
            100,
            100,
            6,
            64,
            64,
            0.15
    ),

    CYBER(
            "Cyber Knight",
            "/assets/sprites/cyber_IDLE64.png",
            "/assets/sprites/cyber_RUN64.png",
            1,
            120,
            90,
            3,
            64,
            64,
            0.15
    ),
    CAPY(
            "Capybara",
            "/assets/sprites/capy_IDLE.png",
            "/assets/sprites/capy_RUN.png",
            1,
            120,
            90,
            3,
            64,
            64,
            0.15
    ),


    POPCORN (
            "Popcorn",
                    "/assets/sprites/proppon_IDLE.png",
                    "/assets/sprites/propon_RUN.png",
                    0,
                    100,
                    100,
                    3,
                    64,
                    64,
                    0.15
    );

    private final String displayName;
    private final String idleSpritePath;
    private final String runSpritePath;
    private final int price;
    private final int maxHealth;
    private final int maxEnergy;
    private final int idleFrameCount;
    private final double frameWidth;
    private final double frameHeight;
    private final double frameDuration;

    HeroType(String displayName, String idleSpritePath, String runSpritePath,
             int price, int maxHealth, int maxEnergy,
             int idleFrameCount, double frameWidth,
             double frameHeight, double frameDuration) {

        this.displayName = displayName;
        this.idleSpritePath = idleSpritePath;
        this.runSpritePath = runSpritePath;
        this.price = price;
        this.maxHealth = maxHealth;
        this.maxEnergy = maxEnergy;
        this.idleFrameCount = idleFrameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDuration = frameDuration;
    }
    public String getDisplayName() {
        return displayName;
    }

    public String getIdleSpritePath() {
        return idleSpritePath;
    }

    public String getRunSpritePath() {
        return runSpritePath;
    }

    public int getPrice() {
        return price;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }
    public int getIdleFrameCount() {
        return idleFrameCount;
    }

    public double getFrameWidth() {
        return frameWidth;
    }

    public double getFrameHeight() {
        return frameHeight;
    }

    public double getFrameDuration() {
        return frameDuration;
    }
}