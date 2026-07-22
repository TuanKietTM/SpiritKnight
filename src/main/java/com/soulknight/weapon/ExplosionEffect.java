package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class ExplosionEffect {
    
    private final Vector2D position;
    private final double maxRadius;
    private final double duration;
    private double elapsedTime;
    private boolean active = true;
    private final Color color;
    
    public ExplosionEffect(Vector2D position, double maxRadius, double duration, Color color) {
        this.position = position.copy();
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.color = color;
        this.elapsedTime = 0.0;
    }
    
    public void update(double deltaSeconds) {
        elapsedTime += deltaSeconds;
        if (elapsedTime >= duration) {
            active = false;
        }
    }
    
    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (!active) {
            return;
        }

        double progress = elapsedTime / duration;        // 0 -> 1
        double zoom = camera.getZoom();
        // No banh ra nhanh luc dau roi cham dan (ease-out)
        double expand = Math.sqrt(progress);
        // Ban kinh tinh theo world roi nhan zoom de dong bo voi cac vat the khac
        double currentRadius = maxRadius * expand * zoom;
        double opacity = 1.0 - progress;                 // mo dan theo thoi gian

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());

        // Vong ngoai (mau chinh, mo dan)
        graphicsContext.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity * 0.85));
        graphicsContext.fillOval(
            screenX - currentRadius,
            screenY - currentRadius,
            currentRadius * 2.0,
            currentRadius * 2.0
        );

        // Vong giua vang sang
        double midRadius = currentRadius * 0.6;
        graphicsContext.setFill(new Color(1.0, 0.85, 0.3, opacity * 0.9));
        graphicsContext.fillOval(
            screenX - midRadius,
            screenY - midRadius,
            midRadius * 2.0,
            midRadius * 2.0
        );

        // Loi trang chinh giua tao cam giac bung sang
        double coreRadius = currentRadius * 0.3;
        graphicsContext.setFill(new Color(1.0, 1.0, 1.0, opacity));
        graphicsContext.fillOval(
            screenX - coreRadius,
            screenY - coreRadius,
            coreRadius * 2.0,
            coreRadius * 2.0
        );
    }
    
    public boolean isActive() {
        return active;
    }
    
    public Vector2D getPosition() {
        return position;
    }
    
    public double getRadius() {
        return maxRadius * (elapsedTime / duration);
    }
}
