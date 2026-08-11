package com.soulknight.item;

import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class EnergyCrystal extends Item {

    private static final String IMAGE_PATH = "/assets/icon/lava.png";
    private final double amount;
    public EnergyCrystal(Vector2D position, double amount) {
        super("Energy Crystal", position, 10.0, Color.AQUA, null, IMAGE_PATH);
        this.amount = Math.max(0.0, amount);
        // Crystal bi hut ve Player khi lai gan.
        enableMagnet(150.0, 300.0);
    }
    public double getAmount() {
        return amount;
    }
}