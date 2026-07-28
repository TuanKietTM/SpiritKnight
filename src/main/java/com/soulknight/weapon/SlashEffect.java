package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Entity;
import com.soulknight.utils.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

// Hieu ung chem cua vu khi can chien (kiem).
// Sprite sheet da chua san thanh kiem, nen khi chay hieu ung nay
// Player se an vu khi dang cam de tranh ve 2 thanh kiem chong nhau.

public final class SlashEffect {

    private static final String SPRITE_PATH = "/assets/effects/slash_effect.png";
    // Sprite sheet 7 frame xep doc: kiem nghi -> vung chem -> kiem ve vi tri cu
    private static final Image SPRITE = ResourceLoader.image(SPRITE_PATH);
    private static final int FRAME_COUNT = 7;
    // Tong thoi luong mot nhat chem (giay), dung chung voi Player de an/hien vu khi
    public static final double SWING_DURATION = 0.22;

    // Vung pixel cua thanh kiem trong frame nghi (do truc tiep tu sprite sheet 200x121/frame)
    private static final double SWORD_PIXEL_LEFT = 12.0;
    private static final double SWORD_PIXEL_RIGHT = 189.0;
    private static final double SWORD_PIXEL_CENTER_Y = 59.5;

    // Hinh hoc thanh kiem dang cam (phai khop voi Player.renderWeapon):
    // dau kiem tai MUZZLE_DISTANCE, chieu dai MUZZLE_DISTANCE + 6 (phan chuoi nam sau diem xoay)
    private static final double SWORD_TIP_WORLD = Gun.MUZZLE_DISTANCE;
    private static final double SWORD_WORLD_LENGTH = Gun.MUZZLE_DISTANCE + 6.0;

    private final Entity owner;
    private final double aimAngle;
    private double elapsedTime;
    private boolean active = true;

    public SlashEffect(Entity owner, double aimAngle) {
        this.owner = owner;
        this.aimAngle = aimAngle;
    }

    public void update(double deltaSeconds) {
        elapsedTime += deltaSeconds;
        if (elapsedTime >= SWING_DURATION) {
            active = false;
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || SPRITE == null || SPRITE.getWidth() <= 1.0) {
            return;
        }

        double progress = Math.min(1.0, elapsedTime / SWING_DURATION);
        double zoom = camera.getZoom();

        // Chon frame theo tien do (frame cuoi giu den het thoi luong)
        int frameIndex = Math.min(FRAME_COUNT - 1, (int) (progress * FRAME_COUNT));
        double frameWidth = SPRITE.getWidth();
        double frameHeight = SPRITE.getHeight() / FRAME_COUNT;
        double sourceY = frameIndex * frameHeight;

        // Ti le world/pixel sao cho thanh kiem trong frame trung kich thuoc kiem dang cam
        double worldPerPixel = SWORD_WORLD_LENGTH / (SWORD_PIXEL_RIGHT - SWORD_PIXEL_LEFT);
        double drawWidth = frameWidth * worldPerPixel * zoom;
        double drawHeight = frameHeight * worldPerPixel * zoom;
        // Can chinh de pixel dau/cuoi thanh kiem nam dung vi tri kiem dang cam,
        // truc luoi kiem (CENTER_Y) nam tren duong ngam (y = 0)
        double drawLeft = (SWORD_TIP_WORLD - SWORD_WORLD_LENGTH - SWORD_PIXEL_LEFT * worldPerPixel) * zoom;
        double drawTop = -SWORD_PIXEL_CENTER_Y * worldPerPixel * zoom;

        // Bam theo vi tri nhan vat de nhat chem khong bi "roi lai" khi vua chem vua chay
        double pivotX = camera.worldToScreenX(owner.getPosition().getX());
        double pivotY = camera.worldToScreenY(owner.getPosition().getY());

        gc.save();
        gc.setImageSmoothing(false);
        // Dua he toa do ve tam nhan vat va xoay theo huong ngam (giong Player.renderWeapon)
        gc.translate(pivotX, pivotY);
        gc.rotate(Math.toDegrees(aimAngle));
        // Khi ngam sang trai, lat doc de nhat chem khong bi nguoc (dong bo voi cach ve vu khi)
        if (Math.cos(aimAngle) < 0.0) {
            gc.scale(1, -1);
        }
        gc.drawImage(SPRITE,
                0, sourceY, frameWidth, frameHeight,
                drawLeft, drawTop, drawWidth, drawHeight);
        gc.restore();
    }

    public boolean isActive() {
        return active;
    }
}
