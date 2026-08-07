package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import javafx.scene.canvas.GraphicsContext;

/**
 * Interface chung cho cac hieu ung combat ngan han , cac buff tac dong truc tiep len enemy
 */
public interface CombatEffect {
    void update(double deltaSeconds);
    void render(GraphicsContext graphicsContext, Camera camera);
    boolean isFinished();
}