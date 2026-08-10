package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.HashSet;
import java.util.Set;

public final class SoundWaveEffect {

    // Sprite sheet hiệu ứng sóng âm lan tỏa (songam.png) - giống ExplosionEffect
    private static final String SPRITE_SHEET_PATH = "/assets/effects/songam.png";
    private static final double FRAME_SIZE = 200.0; // Giống tất cả hiệu ứng khác
    private static final Image SPRITE_SHEET = ResourceLoader.image(SPRITE_SHEET_PATH);

    private final Vector2D position;
    private final double maxRadius;
    private final double duration;
    private final int damage;
    private double elapsedTime;
    private boolean active = true;
    private final int frameCount;
    private final Set<Object> damagedEnemies = new HashSet<>(); // Để không gây sát thương nhiều lần cho cùng 1 enemy

    public SoundWaveEffect(Vector2D position, double maxRadius, double duration, int damage) {
        this.position = position.copy();
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.damage = damage;
        this.elapsedTime = 0.0;
        // Tính số frame giống ExplosionEffect
        int detected = (int) (SPRITE_SHEET.getHeight() / FRAME_SIZE);
        this.frameCount = detected > 0 ? detected : 10;
    }

    public void update(double deltaSeconds, java.util.List<com.soulknight.entity.Enemy> enemies) {
        if (!active) return;

        elapsedTime += deltaSeconds;
        if (elapsedTime >= duration) {
            active = false;
            return;
        }

        // Tính bán kính hiện tại của sóng âm (lan tỏa theo thời gian)
        double currentRadius = getCurrentRadius();

        // Gây sát thương cho tất cả enemy nằm trong bán kính sóng âm
        for (com.soulknight.entity.Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) continue;
            if (damagedEnemies.contains(enemy)) continue; // Đã gây sát thương rồi, bỏ qua

            double distance = position.distance(enemy.getPosition());
            if (distance <= currentRadius + enemy.getRadius()) {
                damagedEnemies.add(enemy);
                enemy.takeDamage(damage);
            }
        }
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (!active || SPRITE_SHEET == null) return;

        double progress = elapsedTime / duration;        // 0 -> 1
        // Chọn khung hình hiện tại theo tiến độ, chạy hết một lượt rồi biến mất
        int frameIndex = (int) (progress * frameCount);
        if (frameIndex >= frameCount) {
            frameIndex = frameCount - 1;
        }

        double sourceY = frameIndex * FRAME_SIZE;
        double zoom = camera.getZoom();

        // Kích thước vẽ cực nhỏ gọn, chỉ mở rộng vừa đủ từ tâm đạn
        double drawSize = maxRadius * 1.5 * zoom;
        double screenX = camera.worldToScreenX(position.getX()) - drawSize / 2.0;
        double screenY = camera.worldToScreenY(position.getY()) - drawSize / 2.0;

        graphicsContext.save();
        graphicsContext.setImageSmoothing(false);
        graphicsContext.drawImage(
                SPRITE_SHEET,
                0.0, sourceY, FRAME_SIZE, FRAME_SIZE,
                screenX, screenY, drawSize, drawSize
        );
        graphicsContext.restore();
    }

    public boolean isActive() {
        return active;
    }

    public double getCurrentRadius() {
        return maxRadius * (elapsedTime / duration);
    }
}