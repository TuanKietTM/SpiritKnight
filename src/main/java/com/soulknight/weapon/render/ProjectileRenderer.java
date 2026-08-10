package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.weapon.Bullet;
import javafx.scene.canvas.GraphicsContext;

public interface ProjectileRenderer {

    void render(Bullet bullet, GraphicsContext gc, Camera camera);
}