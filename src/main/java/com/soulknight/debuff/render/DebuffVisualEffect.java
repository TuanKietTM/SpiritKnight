package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

public interface DebuffVisualEffect {
    void update(double deltaSeconds);
    void render(GraphicsContext gc, Camera camera, Player player);
}