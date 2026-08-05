package com.soulknight.item;

import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class GemItem extends Item {

    private static final String IMAGE_PATH = "/assets/icon/gem.png";
    private final int amount;

    public GemItem(Vector2D position, int amount, GameEventListener listener) {
        super("Gem", position, 9.0, Color.AQUA, listener, IMAGE_PATH);
        this.amount = Math.max(1, amount);

        // Gem hiem nen co tam hut lon hon vang
        enableMagnet(180.0, 350.0);
    }

    public int getAmount() {
        return amount;
    }
}