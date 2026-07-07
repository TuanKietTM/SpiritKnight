package com.soulknight.item;

import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class EnergyCrystal extends Item {

    public EnergyCrystal(Vector2D position, GameEventListener listener) {
        super("Energy Crystal", position, 10.0, Color.AQUA, listener);
    }
}
