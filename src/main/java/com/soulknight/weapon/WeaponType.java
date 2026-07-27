package com.soulknight.weapon;

/**
 * Danh sách các loại vũ khí hiện có trong game (ảnh lấy từ assets/WeaponImage).
 */
public enum WeaponType {
    BLASTER(
            "BLASTER",
            "/assets/WeaponImage/GunImage/OldPistol.png",
            "Blaster_Fire",
            12, 0.18,
            true, 580.0, 0.0
    ),

    OLD_PISTOL(
            "Old Pistol",
            "/assets/WeaponImage/GunImage/OldPistol.png",
            "Pistol_Fire",
            12, 0.18,
            true, 580.0, 0.0
    ),

    SMG(
            "SMG",
            "/assets/WeaponImage/GunImage/SMG.png",
            "SMG_Fire",
            8, 0.09,
            true, 620.0, 0.0
    ),

    SHOTGUN(
            "Shotgun",
            "/assets/WeaponImage/GunImage/Shotgun.png",
            "Shotgun_Fire",
            20, 0.6,
            true, 520.0, 0.0
    ),

    SNIPER(
            "Sniper",
            "/assets/WeaponImage/GunImage/Sniper.png",
            "Sniper_Fire",
            40, 1.1,
            true, 900.0, 0.0
    ),

    OLD_SWORD(
            "Old Sword",
            "/assets/WeaponImage/MeleeImage/Sprite_Old_Sword_of_Royal_Guard.png",
            "Sword_Swing",
            25, 0.35,
            false, 0.0, 60.0
    ),

    FISH(
            "Fish",
            "/assets/WeaponImage/MeleeImage/Fish.png",
            "Fish_Slap",
            18, 0.3,
            false, 0.0, 55.0
    ),

    WAND(
            "Wand",
            "/assets/WeaponImage/MeleeImage/Wand.png",
            "Magic_Cast",
            15, 0.25,
            false, 0.0, 70.0
    );

    private final String displayName;
    private final String imagePath;
    private final String soundPath;
    private final int damage;
    private final double cooldownSeconds;
    private final boolean ranged;
    private final double bulletSpeed;
    private final double meleeRange;

    WeaponType(
            String displayName,
            String imagePath,
            String soundPath,
            int damage,
            double cooldownSeconds,
            boolean ranged,
            double bulletSpeed,
            double meleeRange
    ) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.soundPath = soundPath;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        this.ranged = ranged;
        this.bulletSpeed = bulletSpeed;
        this.meleeRange = meleeRange;
    }

    public String getDisplayName() { return displayName; }
    public String getImagePath() { return imagePath; }
    public String getSoundPath() { return soundPath; }
    public int getDamage() { return damage; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public boolean isRanged() { return ranged; }
    public double getBulletSpeed() { return bulletSpeed; }
    public double getMeleeRange() { return meleeRange; }


    public Weapon createWeapon() {
        if (ranged) {
            return new Gun(displayName, damage, cooldownSeconds, bulletSpeed, 0.0)
                    .withSound(soundPath)
                    .withImage(imagePath);
        }
        return new Melee(displayName, damage, cooldownSeconds, meleeRange)
                .withSound(soundPath)
                .withImage(imagePath);
    }
}