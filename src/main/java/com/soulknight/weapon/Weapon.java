package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.entity.Player;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.image.Image;

public abstract class Weapon {

    private final String name;
    private final int damage;
    private final double cooldownSeconds;
    private double cooldownRemaining;
    // Mana tieu hao moi lan weapon tan cong.
    private double manaCost;
    // Duong dan anh vu khi (co the null neu vu khi khong co anh)
    private String imagePath;
    private String soundPath = "Bullet";

    protected Weapon(String name, int damage, double cooldownSeconds) {
        this(name, damage, cooldownSeconds, 0.0);
    }

    protected Weapon(String name, int damage, double cooldownSeconds, double manaCost) {
        this.name = name;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        this.manaCost = Math.max(0.0, manaCost);
    }
    public abstract void attack(GameWorld world, Entity owner, Vector2D targetPosition);

    public void tick(double deltaSeconds) {
        cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaSeconds);
    }

    protected boolean isReady() {
        return cooldownRemaining <= 0.0;
    }
    protected boolean isReady(Entity owner) {
        if (!isReady()) return false;
        if (owner instanceof Player player) {
            if (!player.getDebuffManager().canAttack()) {
                return false;
            }
        }
        return true;
    }

    protected void resetCooldown() {
        cooldownRemaining = cooldownSeconds;
    }

    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    // Gan duong dan anh cho vu khi, tra ve chinh no de tien goi noi tiep
    public Weapon withImage(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }
    public Weapon withSound(String soundPath) {
        this.soundPath = soundPath;
        return this;
    }

    public String getSoundPath() {
        return soundPath;
    }

    // Lay anh vu khi (null neu vu khi khong co anh)
    public Image getImage() {
        return imagePath == null ? null : ResourceLoader.image(imagePath);
    }
    public double getManaCost() {
        return manaCost;
    }

    public Weapon withManaCost(double manaCost) {
        this.manaCost = Math.max(0.0, manaCost);
        return this;
    }

    // Kiem tra va tru mana cua Player.
// Enemy hoac owner khong phai Player thi khong ton mana.
    protected boolean consumeMana(Entity owner) {
        if (!(owner instanceof Player player)) return true;

        return player.consumeMana(manaCost);
    }
    // Weapon charge nhu Ion/Railgun co the truyen cost rieng.
    protected boolean consumeMana(Entity owner, double amount) {
        if (!(owner instanceof Player player)) return true;

        return player.consumeMana(Math.max(0.0, amount));
    }
}
