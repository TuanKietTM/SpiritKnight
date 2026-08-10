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
            true, 580.0, 0.0,
            0
    ),

    OLD_PISTOL(
            "Old Pistol",
            "/assets/WeaponImage/GunImage/OldPistol.png",
            "Pistol_Fire",
            12, 0.18,
            true, 580.0, 0.0,100
    ),

    SMG(
            "SMG",
            "/assets/WeaponImage/GunImage/SMG.png",
            "SMG_Fire",
            8, 0.09,
            true, 620.0, 0.0,1000
    ),
    PROTOTYPE_RAILGUN(
            "Prototype Railgun",// loai dung dac biet : theo muc nang luong : an len nong
            // cang lau thi do con gpa cang lon
            "/assets/WeaponImage/GunImage/PrototypeRailgun.png",
            "railgun_fire",
            12, 0.65,
            true, 680.0, 0.0, 65
    ),

    ION_ELECTROMAGNETIC_GUN(
            "Ion Electromagnetic Gun",
            // sung dac biet tich nang luon groi ban ra qua cau ion co hieu ung no rieng
            "/assets/WeaponImage/GunImage/ion.png",
            "ion_gun",
            18, 0.70,
            true, 900.0, 0.0, 85
    ),

    SHOTGUN(
            "Shotgun",
            "/assets/WeaponImage/GunImage/Shotgun.png",
            "Shotgun_Fire",
            20, 0.6,
            true, 520.0, 0.0,2000
    ),

    SNIPER(
            "Sniper",
            "/assets/WeaponImage/GunImage/Sniper.png",
            "Sniper_Fire",
            40, 1.1,
            true, 900.0, 0.0,3000
    ),

    LASER_RIFLE(
            "Laser Rifle",
            "/assets/WeaponImage/GunImage/sunglaser.png",
            "laser_gun",
            28, 0.25,
            true, 780.0, 0.0,1
    ),

    SOUND_WAVE_GUN(
            "Sound Wave Gun",
            "/assets/WeaponImage/GunImage/sungsam.png",
            "holy_nova",
            35, 0.8,
            true, 450.0, 0.0,4000
    ),

    OLD_SWORD(
            "Old Sword",
            "/assets/WeaponImage/MeleeImage/Sprite_Old_Sword_of_Royal_Guard.png",
            "Sword_Swing",
            25, 0.35,
            false, 0.0, 30.0,2500
    ),

    FISH(
            "Fish",
            "/assets/WeaponImage/MeleeImage/Fish.png",
            "Fish_Slap",
            18, 0.3,
            false, 0.0, 28.0,5000
    ),

    WAND(
            "Wand",
            "/assets/WeaponImage/MeleeImage/Wand.png",
            "Magic_Cast",
            15, 0.25,
            false, 0.0, 34.0,3600
    );

    private final String displayName;
    private final String imagePath;
    private final String soundPath;
    private final int damage;
    private final double cooldownSeconds;
    private final boolean ranged;
    private final double bulletSpeed;
    private final double meleeRange;
    private final int price;


    WeaponType(
            String displayName,
            String imagePath,
            String soundPath,
            int damage,
            double cooldownSeconds,
            boolean ranged,
            double bulletSpeed,
            double meleeRange,int price
    ) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.soundPath = soundPath;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        this.ranged = ranged;
        this.bulletSpeed = bulletSpeed;
        this.meleeRange = meleeRange;
        this.price=price;
    }

    public String getDisplayName() { return displayName; }
    public String getImagePath() { return imagePath; }
    public String getSoundPath() { return soundPath; }
    public int getDamage() { return damage; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public boolean isRanged() { return ranged; }
    public double getBulletSpeed() { return bulletSpeed; }
    public double getMeleeRange() { return meleeRange; }
    public int getPrice() {
        return price;
    }


    public Weapon createWeapon() {

        // Ion Gun co co che charge va projectile rieng.
        if (this == ION_ELECTROMAGNETIC_GUN) {
            return new IonElectromagneticGun(displayName, cooldownSeconds)
                    .withSound(soundPath)
                    .withImage(imagePath);
        }
        // Railgun co logic charge rieng, khong tao bang Gun thuong. (chu y loai vu khi dac biet nay )
        if (this == PROTOTYPE_RAILGUN) {
            return new PrototypeRailgun(displayName, cooldownSeconds)
                    .withSound(soundPath)
                    .withImage(imagePath);
        }
        if (ranged) {
            Gun gun = new Gun(displayName, damage, cooldownSeconds, bulletSpeed, 0.0);
            // Sung laser ban dan laser xuyen quai (mot tia trung nhieu con)
            if (this == LASER_RIFLE) {
                gun.withPiercing();
            }
            return gun
                    .withSound(soundPath)
                    .withImage(imagePath);
        }
        return new Melee(displayName, damage, cooldownSeconds, meleeRange)
                .withSound(soundPath)
                .withImage(imagePath);
    }
}