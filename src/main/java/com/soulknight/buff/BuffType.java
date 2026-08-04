package com.soulknight.buff;

/**
 * Danh sach cac loai buff co trong game.
 */
public enum BuffType {

    SHIELD(
            "Energy Shield",
            "/assets/buff/shield.png",
            "Reduces incoming damage by 50% for 15 seconds.",
            15.0,
            0.5,
            1,
            false
    ),

    HEAL(
            "Health Core",
            "/assets/buff/heal.png",
            "Restores 40 health.",
            0.0,
            40.0,
            1,
            true
    ),

    SPEED(
            "Speed Module",
            "/assets/buff/speed.png",
            "Increases movement speed by 30% for 12 seconds.",
            12.0,
            0.3,
            1,
            false
    ),

    DAMAGE(
            "Power Core",
            "/assets/buff/damage.png",
            "Increases damage by 25% for 12 seconds.",
            12.0,
            0.25,
            1,
            false
    );

    private final String displayName;
    private final String imagePath;
    private final String description;
    private final double durationSeconds;
    private final double value;
    private final int price;
    private final boolean instant;

    BuffType(String displayName, String imagePath, String description, double durationSeconds, double value, int price, boolean instant) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.value = value;
        this.price = price;
        this.instant = instant;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getDescription() {
        return description;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getValue() {
        return value;
    }

    public int getPrice() {
        return price;
    }

    public boolean isInstant() {
        return instant;
    }

    public Buff createBuff() {
        return switch (this) {
            case SHIELD -> new ShieldBuff(this);
            case HEAL -> new HealBuff(this);
            case SPEED -> new SpeedBuff(this);
            case DAMAGE -> new DamageBuff(this);
        };
    }
}