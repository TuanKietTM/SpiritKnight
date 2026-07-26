package com.soulknight.weapon;

/**
 * Danh sách các loại vũ khí hiện có trong game (ảnh lấy từ assets/WeaponImage).
 */
public enum WeaponType {

    OLD_PISTOL(
            "Old Pistol",
            "/assets/WeaponImage/GunImage/OldPistol.png",
            12, 0.18,
            true, 580.0, 0.0
    ),

    SMG(
            "SMG",
            "/assets/WeaponImage/GunImage/SMG.png",
            8, 0.09,
            true, 620.0, 0.0
    ),

    SHOTGUN(
            "Shotgun",
            "/assets/WeaponImage/GunImage/Shotgun.png",
            20, 0.6,
            true, 520.0, 0.0
    ),

    SNIPER(
            "Sniper",
            "/assets/WeaponImage/GunImage/Sniper.png",
            40, 1.1,
            true, 900.0, 0.0
    ),

    OLD_SWORD(
            "Old Sword",
            "/assets/WeaponImage/MeleeImage/Sprite_Old_Sword_of_Royal_Guard.png",
            25, 0.35,
            false, 0.0, 60.0
    ),

    FISH(
            "Fish",
            "/assets/WeaponImage/MeleeImage/Fish.png",
            18, 0.3,
            false, 0.0, 55.0
    ),

    WAND(
            "Wand",
            "/assets/WeaponImage/MeleeImage/Wand.png",
            15, 0.25,
            false, 0.0, 70.0
    );

    private final String displayName;
    private final String imagePath;
    private final int damage;
    private final double cooldownSeconds;
    private final boolean ranged;
    private final double bulletSpeed;
    private final double meleeRange;

    WeaponType(
            String displayName,
            String imagePath,
            int damage,
            double cooldownSeconds,
            boolean ranged,
            double bulletSpeed,
            double meleeRange
    ) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        this.ranged = ranged;
        this.bulletSpeed = bulletSpeed;
        this.meleeRange = meleeRange;
    }

    public String getDisplayName() { return displayName; }
    public String getImagePath() { return imagePath; }
    public int getDamage() { return damage; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public boolean isRanged() { return ranged; }
    public double getBulletSpeed() { return bulletSpeed; }
    public double getMeleeRange() { return meleeRange; }

    /**
     * Tạo instance Weapon tương ứng để trang bị cho Player.
     */
    public Weapon createWeapon() {
        if (ranged) {
            return new Gun(displayName, damage, cooldownSeconds, bulletSpeed, 0.0)
                    .withImage(imagePath);
        }
        return new Melee(displayName, damage, cooldownSeconds, meleeRange)
                .withImage(imagePath);
    }
}
