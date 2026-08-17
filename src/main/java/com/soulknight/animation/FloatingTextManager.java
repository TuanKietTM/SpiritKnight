package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/**
 * Quan li viec sinh Floating text len trn man hinh choi de bao diem
 */
public final class FloatingTextManager {

    private static final int MAX_FLOATING_TEXTS = 120;

    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Random random = new Random();

    //text khi gay sat thuong cho quai
    public void spawnDamage(Vector2D position, int damage) {
        Vector2D spawnPosition = randomOffset(position, 7.0, 4.0);

        FloatingText text = new FloatingText(
                "-" + Math.max(0, damage),
                spawnPosition,
                new Vector2D(randomRange(-8.0, 8.0), -32.0),
                Color.rgb(255, 90, 90),
                Color.rgb(65, 10, 15),
                0.75,
                12.0,
                0.75,
                0.95
        ).withBounce(true)
                .withDrag(1.2)
                .withOutline(2.0);

        add(text);
    }
    public void spawnGold(Vector2D position, int amount) {
        FloatingText text = new FloatingText(
                "+" + Math.max(0, amount) + " GOLD",
                randomOffset(position, 5.0, 3.0),
                new Vector2D(0.0, -28.0),
                Color.GOLD,
                Color.rgb(90, 55, 0),
                0.9,
                12.0,
                0.8,
                1.0
        ).withBounce(true)
                .withDrag(1.0)
                .withOutline(2.0);

        add(text);
    }
    public void spawnCustom(String text, Vector2D position, Color color) {
        FloatingText floatingText = new FloatingText(
                text,
                position,
                color,
                0.85,
                12.0
        ).withBounce(true)
                .withOutline(2.0);

        add(floatingText);
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0) return;

        for (FloatingText text : floatingTexts) {
            text.update(deltaSeconds);
        }

        floatingTexts.removeIf(FloatingText::isDead);
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null) return;

        for (FloatingText text : floatingTexts) {
            text.render(gc, camera);
        }
    }

    public void clear() {
        floatingTexts.clear();
    }

    public int size() {
        return floatingTexts.size();
    }

    private void add(FloatingText text) {
        if (text == null) return;

        while (floatingTexts.size() >= MAX_FLOATING_TEXTS) {
            floatingTexts.remove(0);
        }

        floatingTexts.add(text);
    }

    private Vector2D randomOffset(Vector2D position, double maxX, double maxY) {
        if (position == null) return new Vector2D(0.0, 0.0);

        return new Vector2D(
                position.getX() + randomRange(-maxX, maxX),
                position.getY() + randomRange(-maxY, maxY)
        );
    }

    private double randomRange(double min, double max) {
        if (max <= min) return min;
        return min + random.nextDouble() * (max - min);
    }
}