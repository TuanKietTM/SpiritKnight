package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FloatingTextManager {

    private static final int MAX_FLOATING_TEXTS = 120;

    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Random random = new Random();
//taext khi gay sat thuong cho quai
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

    public void spawnCritical(Vector2D position, int damage) {
        Vector2D spawnPosition = randomOffset(position, 5.0, 3.0);

        FloatingText text = new FloatingText(
                "CRIT! -" + Math.max(0, damage),
                spawnPosition,
                new Vector2D(randomRange(-6.0, 6.0), -42.0),
                Color.GOLD,
                Color.rgb(90, 25, 0),
                1.0,
                15.0,
                0.65,
                1.05
        ).withBounce(true)
                .withGravity(18.0)
                .withDrag(0.8)
                .withOutline(2.5);

        add(text);
    }

    public void spawnHeal(Vector2D position, int amount) {
        FloatingText text = new FloatingText(
                "+" + Math.max(0, amount) + " HP",
                randomOffset(position, 4.0, 2.0),
                new Vector2D(0.0, -26.0),
                Color.LIMEGREEN,
                Color.rgb(10, 55, 20),
                0.9,
                12.0,
                0.8,
                1.0
        ).withBounce(true)
                .withDrag(1.0)
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

    public void spawnEnergy(Vector2D position, int amount) {
        FloatingText text = new FloatingText(
                "+" + Math.max(0, amount) + " ENERGY",
                randomOffset(position, 4.0, 2.0),
                new Vector2D(0.0, -25.0),
                Color.DEEPSKYBLUE,
                Color.rgb(0, 30, 75),
                0.85,
                11.0,
                0.8,
                1.0
        ).withBounce(true)
                .withOutline(2.0);

        add(text);
    }

    public void spawnMiss(Vector2D position) {
        FloatingText text = new FloatingText(
                "MISS",
                randomOffset(position, 6.0, 3.0),
                new Vector2D(randomRange(-8.0, 8.0), -22.0),
                Color.LIGHTGRAY,
                Color.rgb(40, 40, 50),
                0.65,
                11.0,
                0.9,
                1.0
        ).withDrag(1.0)
                .withOutline(2.0);

        add(text);
    }

    public void spawnBuff(Vector2D position, String buffName, Color color) {
        String safeName = buffName == null || buffName.isBlank() ? "BUFF" : buffName.toUpperCase();

        FloatingText text = new FloatingText(
                safeName,
                randomOffset(position, 3.0, 2.0),
                new Vector2D(0.0, -30.0),
                color == null ? Color.WHITE : color,
                Color.BLACK,
                1.0,
                13.0,
                0.7,
                1.0
        ).withBounce(true)
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