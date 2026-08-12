package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public final class EndingPortal {

    private static final String SPRITE_PATH = "/assets/story/portal1.png";
    private static final int FRAME_COUNT = 3;
    private static final double FRAME_WIDTH = 200.0;
    private static final double FRAME_HEIGHT = 250.0;
    private static final double RENDER_WIDTH = 72.0;
    private static final double RENDER_HEIGHT = 90.0;
    private static final double FRAME_DURATION = 0.16;
    private static final double OPEN_DURATION = 0.65;
    private static final double COLLISION_RADIUS = 24.0;
    private final Vector2D position;
    private final Image spriteSheet;
    private double frameTimer;
    private double openProgress;
    private int currentFrame;
    private boolean active;

    public EndingPortal(Vector2D position) {
        this.position = position.copy();
        this.spriteSheet = loadImage();
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0) return;
        updateOpening(deltaSeconds);
        updateAnimation(deltaSeconds);
    }

    // Portal mo dan truoc khi cho phep Player di vao.
    private void updateOpening(double deltaSeconds) {
        if (active) return;

        openProgress = Math.min(1.0, openProgress + deltaSeconds / OPEN_DURATION);

        if (openProgress >= 1.0) active = true;
    }

    // Lap animation 3 frame cua portal.
    private void updateAnimation(double deltaSeconds) {
        frameTimer += deltaSeconds;
        while (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;
            currentFrame = (currentFrame + 1) % FRAME_COUNT;
        }
    }

    public boolean intersects(Player player) {
        if (!active || player == null || player.getPosition() == null) return false;

        /*
         * Position nam tai chan portal nen day tam collision len tren
         * de Player phai thuc su buoc vao ben trong portal.
         */
        double centerX = position.getX();
        double centerY = position.getY() - RENDER_HEIGHT * 0.38;

        double dx = player.getPosition().getX() - centerX;
        double dy = player.getPosition().getY() - centerY;
        double radius = COLLISION_RADIUS + player.getRadius();

        return dx * dx + dy * dy <= radius * radius;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || spriteSheet == null) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());

        double openScale = easeOut(openProgress);
        double zoom = camera.getZoom();

        double width = RENDER_WIDTH * zoom * openScale;
        double height = RENDER_HEIGHT * zoom * openScale;

        double sourceX = currentFrame * FRAME_WIDTH;

        gc.save();
        gc.setImageSmoothing(false);
        gc.drawImage(spriteSheet, sourceX, 0.0,
                FRAME_WIDTH, FRAME_HEIGHT, Math.floor(screenX - width * 0.5),
                Math.floor(screenY - height), width, height);

        gc.restore();
    }

    private Image loadImage() {
        try {
            var resource = EndingPortal.class.getResource(SPRITE_PATH);

            if (resource == null) {
                System.err.println("Khong tim thay sprite Ending Portal: " + SPRITE_PATH);
                return null;
            }

            return new Image(resource.toExternalForm());
        } catch (Exception exception) {
            System.err.println("Khong the load Ending Portal: " + exception.getMessage());
            return null;
        }
    }

    private double easeOut(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    public Vector2D getPosition() {
        return position;
    }

    public boolean isActive() {
        return active;
    }
}