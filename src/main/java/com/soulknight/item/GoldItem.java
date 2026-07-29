package com.soulknight.item;

import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class GoldItem extends Item {

    private final int amount;

    public GoldItem(Vector2D position, int amount, GameEventListener listener) {

        super("Gold", position, 8.0, Color.GOLD, listener);

        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}