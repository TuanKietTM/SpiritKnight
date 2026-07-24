package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Hieu ung chem cua vu khi can chien (kiem).
 * Ve hinh cung chem (Buff_Melee_Range) truoc mat nhan vat, xoay theo huong ngam,
 * kem hoat anh quet nhanh + mo dan de trong sinh dong nhung khong che khuat UI.
 */
public final class SlashEffect {

    private static final String SPRITE_PATH = "/assets/effects/Buff_Melee_Range-removebg.png";
    // Anh cung chem tai mot lan, dung chung cho moi nhat chem
    private static final Image SPRITE = ResourceLoader.image(SPRITE_PATH);

    // Ti le be rong hieu ung so voi tam danh (range) -> giu vua khung, khong qua to
    private static final double WIDTH_TO_RANGE = 1.0;
    // Khoang cach day hieu ung ra truoc mat theo huong ngam (theo range)
    private static final double FORWARD_TO_RANGE = 0.5;
    // Bien do quet cung (radian) tao cam giac vung tay chem
    private static final double SWING_ARC = 0.5;

    private final Vector2D origin;
    private final double aimAngle;
    private final double range;
    private final double duration;
    private double elapsedTime;
    private boolean active = true;

    public SlashEffect(Vector2D origin, double aimAngle, double range, double duration) {
        this.origin = origin.copy();
        this.aimAngle = aimAngle;
        this.range = range;
        this.duration = duration;
    }

    public void update(double deltaSeconds) {
        elapsedTime += deltaSeconds;
        if (elapsedTime >= duration) {
            active = false;
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || SPRITE == null || SPRITE.getWidth() <= 1.0) {
            return;
        }

        double progress = Math.min(1.0, elapsedTime / duration);
        double zoom = camera.getZoom();

        // Kich thuoc ve giu nguyen ti le anh goc
        double drawWidth = range * WIDTH_TO_RANGE * zoom;
        double drawHeight = drawWidth * (SPRITE.getHeight() / SPRITE.getWidth());

        // Hoat anh: phong to nhe (0.75 -> 1.10) + mo dan ve cuoi (dam luc dau)
        double scale = 0.75 + 0.35 * progress;
        double alpha = 1.0 - progress * progress;
        // Quet cung theo tien do tao dong tac vung tay chem
        double swing = (progress - 0.5) * SWING_ARC;

        double pivotX = camera.worldToScreenX(origin.getX());
        double pivotY = camera.worldToScreenY(origin.getY());
        double forward = range * FORWARD_TO_RANGE * zoom;

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setImageSmoothing(true);
        // Dua he toa do ve tam nhan vat va xoay theo huong ngam
        gc.translate(pivotX, pivotY);
        gc.rotate(Math.toDegrees(aimAngle + swing));
        // Khi ngam sang trai, lat doc de cung chem khong bi nguoc (dong bo voi cach ve vu khi)
        if (Math.cos(aimAngle) < 0.0) {
            gc.scale(1, -1);
        }
        double w = drawWidth * scale;
        double h = drawHeight * scale;
        gc.drawImage(SPRITE, forward - w / 2.0, -h / 2.0, w, h);
        gc.restore();
    }

    public boolean isActive() {
        return active;
    }
}
