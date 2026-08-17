package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class RestShrine {

    private static final double INTERACT_RADIUS = 34.0;

    private static final double HP_RESTORE_RATIO = 0.25;
    private static final double MANA_RESTORE_RATIO = 0.35;

    private final Vector2D position;

    private boolean used;
    private double age;

    public RestShrine(Vector2D position) {
        this.position = position.copy();
    }

    public void update(GameWorld world, Player player, double deltaSeconds) {
        if (used || player == null || player.getPosition() == null) return;

        age += Math.max(0.0, deltaSeconds);

        if (player.getPosition().distance(position) > INTERACT_RADIUS + player.getRadius()) return;

        activate(world, player);
    }

    private void activate(GameWorld world, Player player) {
        if (used) return;

        used = true;

        restorePlayer(player);

        SoundManager.getInstance().playSFX("heal");

        if (world != null) {
            world.showRestShrineEffect(position);
        }
    }

    private void restorePlayer(Player player) {
        if (player == null) return;

        int healAmount = Math.max(1, (int) Math.round(player.getMaxHealth() * HP_RESTORE_RATIO));

        player.restoreHealth(healAmount);
        player.restoreShield(player.getMaxShield());
        player.restoreMana(player.getMaxMana() * MANA_RESTORE_RATIO);
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null) return;

        double x = camera.worldToScreenX(position.getX());
        double y = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double pulse = Math.sin(age * 3.0) * 2.0 * zoom;

        gc.save();

        gc.setGlobalAlpha(used ? 0.25 : 0.75);
        gc.setFill(used ? Color.DARKSLATEGRAY : Color.AQUAMARINE);

        double radius = 16.0 * zoom + pulse;

        gc.fillOval(
                x - radius,
                y - radius,
                radius * 2.0,
                radius * 2.0
        );

        gc.setGlobalAlpha(used ? 0.35 : 1.0);
        gc.setFill(Color.LIGHTGOLDENRODYELLOW);

        double core = 6.0 * zoom;

        gc.fillOval(
                x - core,
                y - core,
                core * 2.0,
                core * 2.0
        );

        gc.restore();
    }

    public Vector2D getPosition() {
        return position;
    }
}