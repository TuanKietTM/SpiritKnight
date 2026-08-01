package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class FloatingText {

    private final String text;
    private final Vector2D position;
    private final Vector2D velocity;
    private final Color color;
    private final Color outlineColor;
    private final double maxLifeTime;
    private final double startScale;
    private final double endScale;
    private final double fontSize;

    private double lifeTime;
    private double outlineWidth;
    private double gravity;
    private double drag;
    private boolean bounceAnimation;

    public FloatingText(String text, Vector2D position, Color color, double lifeTime, double fontSize) {
        this(text, position, new Vector2D(0.0, -28.0), color, Color.BLACK,
                lifeTime, fontSize, 0.8, 1.0);
    }

    public FloatingText(String text, Vector2D position, Vector2D velocity, Color color, Color outlineColor,
                        double lifeTime, double fontSize, double startScale, double endScale) {

        this.text = text == null ? "" : text;
        this.position = copyVector(position);
        this.velocity = copyVector(velocity);
        this.color = color == null ? Color.WHITE : color;
        this.outlineColor = outlineColor == null ? Color.BLACK : outlineColor;
        this.lifeTime = Math.max(0.01, lifeTime);
        this.maxLifeTime = this.lifeTime;
        this.fontSize = Math.max(1.0, fontSize);
        this.startScale = Math.max(0.01, startScale);
        this.endScale = Math.max(0.01, endScale);
        this.outlineWidth = 2.0;
    }

    public FloatingText withGravity(double gravity) {
        this.gravity = gravity;
        return this;
    }

    public FloatingText withDrag(double drag) {
        this.drag = Math.max(0.0, drag);
        return this;
    }

    public FloatingText withOutline(double outlineWidth) {
        this.outlineWidth = Math.max(0.0, outlineWidth);
        return this;
    }

    public FloatingText withBounce(boolean enabled) {
        this.bounceAnimation = enabled;
        return this;
    }

    public void update(double deltaSeconds) {
        if (isDead() || deltaSeconds <= 0.0) return;

        velocity.setY(velocity.getY() + gravity * deltaSeconds);

        if (drag > 0.0) {
            double dragFactor = Math.max(0.0, 1.0 - drag * deltaSeconds);
            velocity.setX(velocity.getX() * dragFactor);
            velocity.setY(velocity.getY() * dragFactor);
        }

        position.setX(position.getX() + velocity.getX() * deltaSeconds);
        position.setY(position.getY() + velocity.getY() * deltaSeconds);

        lifeTime -= deltaSeconds;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (isDead() || gc == null || camera == null || text.isBlank()) return;

        double progress = getProgress();
        double alpha = calculateAlpha(progress);
        double scale = calculateScale(progress);
        double zoom = camera.getZoom();

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double renderFontSize = fontSize * scale * zoom;

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, renderFontSize));

        if (outlineWidth > 0.0) {
            gc.setStroke(outlineColor);
            gc.setLineWidth(outlineWidth * zoom);
            gc.strokeText(text, screenX, screenY);
        }

        gc.setFill(color);
        gc.fillText(text, screenX, screenY);
        gc.restore();
    }

    public boolean isDead() {
        return lifeTime <= 0.0;
    }

    private double getProgress() {
        return 1.0 - Math.max(0.0, Math.min(1.0, lifeTime / maxLifeTime));
    }

    private double calculateAlpha(double progress) {
        if (progress < 0.12) return progress / 0.12;
        if (progress < 0.65) return 1.0;
        return Math.max(0.0, 1.0 - (progress - 0.65) / 0.35);
    }

    private double calculateScale(double progress) {
        if (!bounceAnimation) {
            return lerp(startScale, endScale, progress);
        }

        if (progress < 0.18) {
            return lerp(startScale, 1.35, progress / 0.18);
        }

        if (progress < 0.36) {
            return lerp(1.35, 1.0, (progress - 0.18) / 0.18);
        }

        return lerp(1.0, endScale, (progress - 0.36) / 0.64);
    }

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static Vector2D copyVector(Vector2D source) {
        if (source == null) return new Vector2D(0.0, 0.0);
        return new Vector2D(source.getX(), source.getY());
    }
}