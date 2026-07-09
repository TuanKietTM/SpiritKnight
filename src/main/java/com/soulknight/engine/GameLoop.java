package com.soulknight.engine;

import java.util.function.DoubleConsumer;
import javafx.animation.AnimationTimer;

public final class GameLoop {

    private final AnimationTimer animationTimer;

    public GameLoop(DoubleConsumer frameHandler) {
        this.animationTimer = new AnimationTimer() {
            private long lastFrameTime = -1L;

            @Override
            public void handle(long now) {
                if (lastFrameTime < 0L) {
                    lastFrameTime = now;
                    return;
                }

                double deltaSeconds = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;
                frameHandler.accept(deltaSeconds);
            }
        };
    }

    public void start() {
        animationTimer.start();
    }

    public void stop() {
        animationTimer.stop();
    }

    public void changeState(GameState gameState) {
    }
}
