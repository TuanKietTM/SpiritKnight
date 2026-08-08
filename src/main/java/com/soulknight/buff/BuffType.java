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

    CHAIN_LIGHTNING(
            "Chain Lightning",
            "/assets/buff/chain_lighting.png",
            "Strikes the target and chains to up to 2 nearby enemies.",
            15.0,
            12.0,
            1,
            false
    ),
    DEATH_EXPLOSION(
            "Death Explosion",
            "/assets/buff/burn_buff.png",
            "Enemies have a 50% chance to explode on death, damaging and burning nearby enemies.",
            15.0,
            10.0,
            1,
            false
    ),
    DRAGON_BREATH(
            "Dragon Breath",
            "/assets/buff/dragon.png",
            "Breathes fire every 3 seconds, burning enemies and detonating burning targets.",
            15.0,
            5.0,
            1,
            false
    ),
    HOLY_NOVA(
            "Holy Nova",
            "/assets/buff/holy_nova.png",
            "Blocks lethal damage, heals 50% max HP and releases a powerful holy nova.",
            40.0,
            200.0,
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

    BuffType(
            String displayName,
            String imagePath,
            String description,
            double durationSeconds,
            double value,
            int price,
            boolean instant
    ) {
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
            case CHAIN_LIGHTNING -> new ChainLightningBuff(this);
            case DEATH_EXPLOSION -> new DeathExplosionBuff(this);
            case DRAGON_BREATH -> new DragonBreathBuff(this);
            case HOLY_NOVA ->new HolyNovaBuff(this);
            case DAMAGE -> new DamageBuff(this);
        };
    }
}