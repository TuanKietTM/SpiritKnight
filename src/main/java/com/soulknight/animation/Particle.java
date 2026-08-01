package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {

    public enum Shape {
        SQUARE, CIRCLE, DIAMOND
    }

    private final Vector2D position;
    private final Vector2D velocity;
    private final Vector2D acceleration;

    private final Color color;
    private final double maxLifeTime;
    private final double startSize;

    private double lifeTime;
    private double endSize;
    private double drag;
    private double rotation;
    private double rotationSpeed;
    private double fadeInRatio;
    private Shape shape;

    public Particle(Vector2D position, Vector2D velocity, double size, Color color, double lifeTime) {
        this(position, velocity, new Vector2D(0.0, 0.0), size, size, color, lifeTime, Shape.SQUARE);
    }

    public Particle(Vector2D position, Vector2D velocity, Vector2D acceleration, double startSize, double endSize,
                    Color color, double lifeTime, Shape shape) {

        this.position = copyVector(position);
        this.velocity = copyVector(velocity);
        this.acceleration = copyVector(acceleration);
        this.startSize = Math.max(0.0, startSize);
        this.endSize = Math.max(0.0, endSize);
        this.color = color == null ? Color.WHITE : color;
        this.lifeTime = Math.max(0.001, lifeTime);
        this.maxLifeTime = this.lifeTime;
        this.shape = shape == null ? Shape.SQUARE : shape;
        this.fadeInRatio = 0.0;
    }

    public Particle withAcceleration(double x, double y) {
        acceleration.setX(x);
        acceleration.setY(y);
        return this;
    }

    public Particle withDrag(double drag) {
        this.drag = Math.max(0.0, drag);
        return this;
    }

    public Particle withEndSize(double endSize) {
        this.endSize = Math.max(0.0, endSize);
        return this;
    }

    public Particle withRotation(double rotation, double rotationSpeed) {
        this.rotation = rotation;
        this.rotationSpeed = rotationSpeed;
        return this;
    }

    public Particle withFadeIn(double fadeInRatio) {
        this.fadeInRatio = Math.max(0.0, Math.min(0.95, fadeInRatio));
        return this;
    }

    public Particle withShape(Shape shape) {
        if (shape != null) this.shape = shape;
        return this;
    }

    public void update(double deltaSeconds) {
        if (isDead() || deltaSeconds <= 0.0) return;

        velocity.setX(velocity.getX() + acceleration.getX() * deltaSeconds);
        velocity.setY(velocity.getY() + acceleration.getY() * deltaSeconds);

        if (drag > 0.0) {
            double dragFactor = Math.max(0.0, 1.0 - drag * deltaSeconds);
            velocity.setX(velocity.getX() * dragFactor);
            velocity.setY(velocity.getY() * dragFactor);
        }

        position.setX(position.getX() + velocity.getX() * deltaSeconds);
        position.setY(position.getY() + velocity.getY() * deltaSeconds);

        rotation += rotationSpeed * deltaSeconds;
        lifeTime -= deltaSeconds;
    }

    public boolean isDead() {
        return lifeTime <= 0.0;
    }

    public double getLifeProgress() {
        return 1.0 - Math.max(0.0, Math.min(1.0, lifeTime / maxLifeTime));
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (isDead() || gc == null || camera == null) return;

        double progress = getLifeProgress();
        double renderSize = lerp(startSize, endSize, progress) * camera.getZoom();
        if (renderSize <= 0.0) return;

        double screenX = camera.worldToScreenX(position.getX()) - renderSize / 2.0;
        double screenY = camera.worldToScreenY(position.getY()) - renderSize / 2.0;
        double alpha = calculateAlpha(progress);

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.translate(screenX + renderSize / 2.0, screenY + renderSize / 2.0);
        gc.rotate(rotation);

        switch (shape) {
            case CIRCLE -> gc.fillOval(-renderSize / 2.0, -renderSize / 2.0, renderSize, renderSize);
            case DIAMOND -> {
                gc.rotate(45.0);
                gc.fillRect(-renderSize / 2.0, -renderSize / 2.0, renderSize, renderSize);
            }
            case SQUARE -> gc.fillRect(-renderSize / 2.0, -renderSize / 2.0, renderSize, renderSize);
        }

        gc.restore();
    }

    private double calculateAlpha(double progress) {
        if (fadeInRatio > 0.0 && progress < fadeInRatio) {
            return Math.max(0.0, Math.min(1.0, progress / fadeInRatio));
        }

        double fadeStart = Math.max(fadeInRatio, 0.55);
        if (progress <= fadeStart) return 1.0;

        return Math.max(0.0, 1.0 - (progress - fadeStart) / (1.0 - fadeStart));
    }

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static Vector2D copyVector(Vector2D source) {
        if (source == null) return new Vector2D(0.0, 0.0);
        return new Vector2D(source.getX(), source.getY());
    }
}