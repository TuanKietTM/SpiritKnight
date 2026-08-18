package com.soulknight.buff.effect;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

public interface BuffVisualEffect {

    void start(Player player);

    void update(
            Player player,
            double deltaSeconds,
            double remainingSeconds,
            double durationSeconds
    );

    /**
     * Chia ra ve cac hieu ung tac dong len player thanh behind va before de theo chuan 2.5D
     * Ve cac thanh phan nam sau Player.
     */
    void renderBehind(
            GraphicsContext graphicsContext,
            Camera camera,
            Player player,
            double remainingSeconds,
            double durationSeconds
    );

    /**
     * Ve cac thanh phan nam truoc Player.
     */
    void renderFront(
            GraphicsContext graphicsContext,
            Camera camera,
            Player player,
            double remainingSeconds,
            double durationSeconds
    );

    void notifyPlayerHit();

    void stop();

    boolean isActive();
}