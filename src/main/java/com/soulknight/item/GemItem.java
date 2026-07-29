package com.soulknight.item;

import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class GemItem extends Item {

    private final int amount;

    public GemItem(Vector2D position, int amount, GameEventListener listener) {

        super("Gem", position, 8.0, Color.AQUA, listener);

        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}