package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.image.Image;

public abstract class Weapon {

    private final String name;
    private final int damage;
    private final double cooldownSeconds;
    private double cooldownRemaining;
    // Duong dan anh vu khi (co the null neu vu khi khong co anh)
    private String imagePath;

    protected Weapon(String name, int damage, double cooldownSeconds) {
        this.name = name;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
    }

    public abstract void attack(GameWorld world, Entity owner, Vector2D targetPosition);

    public void tick(double deltaSeconds) {
        cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaSeconds);
    }

    protected boolean isReady() {
        return cooldownRemaining <= 0.0;
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

    // Lay anh vu khi (null neu vu khi khong co anh)
    public Image getImage() {
        return imagePath == null ? null : ResourceLoader.image(imagePath);
    }
}
