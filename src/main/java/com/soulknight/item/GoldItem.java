package com.soulknight.item;

import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class GoldItem extends Item {

    private static final String IMAGE_PATH = "/assets/icon/gold (1).png";
    private final int amount;

    public GoldItem(Vector2D position, int amount, GameEventListener listener) {
        super("Gold", position, getRadiusForAmount(amount), Color.GOLD, listener, IMAGE_PATH);
        this.amount = Math.max(1, amount);

        // Vang hut xa va nhanh hon de nhat lien tuc trong combat
        enableMagnet(155.0, 320.0);
    }

    private static double getRadiusForAmount(int amount) {
        if (amount >= 20) return 11.0;
        if (amount >= 10) return 9.5;
        return 8.0;
    }

    public int getAmount() {
        return amount;
    }
}