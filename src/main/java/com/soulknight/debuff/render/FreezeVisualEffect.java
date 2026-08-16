package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class FreezeVisualEffect implements DebuffVisualEffect {

    private double stateTime = 0.0;

    private static final Color COLOR_ICE_BASE  = Color.web("#80DEEA"); // Xanh băng nhạt
    private static final Color COLOR_ICE_DARK  = Color.web("#00ACC1"); // Xanh băng đậm
    private static final Color COLOR_ICE_SHINE = Color.web("#E0F7FA"); // Ánh kim tuyết sáng
    private static final Color COLOR_SNOWFLAKE = Color.web("#E0F7FA"); // Bông tuyết sáng

    @Override
    public void update(double deltaSeconds) {
        this.stateTime += deltaSeconds;
    }

    @Override
    public void render(GraphicsContext gc, Camera camera, Player player) {
        if (player == null) return;

        // 1. PHỦ LỚP SƯƠNG BĂNG GIÁ & BÔNG TUYẾT RƠI TOÀN MÀN HÌNH
        renderFullMapFreezeOverlay(gc);

        // 2. VẼ KHỐI BĂNG BAO TRỌN PLAYER
        renderPlayerIceBlock(gc, camera, player);
    }

    /**
     * Phủ hiệu ứng băng giá + Tuyết rơi lên toàn bộ màn hình Game
     */
    private void renderFullMapFreezeOverlay(GraphicsContext gc) {
        Canvas canvas = gc.getCanvas();
        if (canvas == null) return;

        double screenWidth = canvas.getWidth();
        double screenHeight = canvas.getHeight();

        gc.save();

        // 1. Lớp phủ xanh Cyan mờ nhấp nháy
        double pulseAlpha = 0.22 + Math.sin(stateTime * 4.0) * 0.04;
        gc.setGlobalAlpha(pulseAlpha);
        gc.setFill(Color.web("#00E5FF"));
        gc.fillRect(0, 0, screenWidth, screenHeight);

        // 2. Viền đóng băng (Frost Vignette) ở 4 góc
        RadialGradient frostVignette = new RadialGradient(
                0, 0,
                screenWidth / 2.0, screenHeight / 2.0,
                Math.max(screenWidth, screenHeight) * 0.65,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.65, Color.web("#00838F", 0.2)),
                new Stop(1.0, Color.web("#00E5FF", 0.6))
        );

        gc.setGlobalAlpha(0.85);
        gc.setFill(frostVignette);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        // 3. HỆ THỐNG BÔNG TUYẾT PIXEL RƠI LÊN MÀN HÌNH
        renderFallingSnowflakes(gc, screenWidth, screenHeight);

        gc.restore();
    }

    /**
     * Thuật toán vẽ các bông tuyết Pixel Art rơi & chao đảo nhẹ
     */
    private void renderFallingSnowflakes(GraphicsContext gc, double width, double height) {
        int snowflakeCount = 20; // 20 bông tuyết rơi liên tục trên màn hình

        gc.setImageSmoothing(false);
        gc.setFill(COLOR_SNOWFLAKE);

        for (int i = 0; i < snowflakeCount; i++) {
            // Tốc độ rơi riêng từng bông
            double fallSpeed = 120.0 + (i % 5) * 30.0;

            // Tọa độ Y rơi từ đỉnh màn hình xuống dưới theo vòng lặp %
            double y = (stateTime * fallSpeed + i * (height / snowflakeCount)) % height;

            // Tọa độ X dao động nhẹ hình sin (tuyết chao đảo trong gió)
            double waveX = Math.sin(stateTime * 2.5 + i * 1.5) * 18.0;
            double x = ((i * 73.0) % width) + waveX;

            // Xử lý độ mờ ở biên màn hình
            double alpha = 0.85;
            if (y < 30) alpha = y / 30.0 * 0.85; // Hiện dần ở viền trên
            else if (y > height - 40) alpha = (height - y) / 40.0 * 0.85; // Mờ dần ở viền dưới

            gc.setGlobalAlpha(Math.max(0.0, alpha));

            // Chọn kiểu vẽ: Hạt tuyết nhỏ (2x2) hoặc Bông tuyết chữ thập Pixel (5x5)
            if (i % 3 == 0) {
                renderPixelCrossSnowflake(gc, x, y); // Bông tuyết dấu cộng 5x5
            } else {
                gc.fillRect(Math.round(x), Math.round(y), 2.0, 2.0); // Hạt nhỏ
            }
        }
    }

    /**
     * Vẽ Bông Tuyết Chữ Thập Pixel Art (Cross Snowflake)
     */
    private void renderPixelCrossSnowflake(GraphicsContext gc, double cx, double cy) {
        int px = (int) Math.round(cx);
        int py = (int) Math.round(cy);

        // Tâm
        gc.fillRect(px, py, 2, 2);
        // 4 Cánh chữ thập
        gc.fillRect(px - 2, py, 2, 2);
        gc.fillRect(px + 2, py, 2, 2);
        gc.fillRect(px, py - 2, 2, 2);
        gc.fillRect(px, py + 2, 2, 2);
    }

    /**
     * Vẽ tảng băng nhốt Player
     */
    private void renderPlayerIceBlock(GraphicsContext gc, Camera camera, Player player) {
        Vector2D playerPos = player.getPosition();
        double playerRadius = player.getRadius();

        double screenX = camera.worldToScreenX(playerPos.getX());
        double screenY = camera.worldToScreenY(playerPos.getY());
        double zoom = camera.getZoom();

        gc.save();
        gc.setImageSmoothing(false);

        double w = playerRadius * 2.2 * zoom;
        double h = playerRadius * 2.6 * zoom;
        double drawX = screenX - w / 2.0;
        double drawY = screenY - h / 2.0 + (playerRadius * 0.2 * zoom);

        gc.setGlobalAlpha(0.7);
        gc.setFill(COLOR_ICE_BASE);
        gc.fillRect(drawX, drawY, w, h);

        gc.setGlobalAlpha(0.9);
        gc.setFill(COLOR_ICE_DARK);
        gc.fillRect(drawX, drawY, w, 3);
        gc.fillRect(drawX, drawY + h - 3, w, 3);
        gc.fillRect(drawX, drawY, 3, h);
        gc.fillRect(drawX + w - 3, drawY, 3, h);

        gc.setFill(COLOR_ICE_SHINE);
        gc.fillRect(drawX + w * 0.2, drawY + h * 0.15, w * 0.15, h * 0.5);
        gc.fillRect(drawX + w * 0.65, drawY + h * 0.4, w * 0.1, h * 0.3);

        gc.restore();
    }
}