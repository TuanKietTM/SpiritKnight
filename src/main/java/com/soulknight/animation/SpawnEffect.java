package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Hieu ung cot sang trieu hoi
 */
public class SpawnEffect {

    private final Vector2D position;
    private final double delayDuration;   // thoi gian phong rong
    private final double beamDuration;    // thoi gian cot sang chieu xuong

    private double timer = 0.0;
    private boolean active = true;
    private boolean finished = false;
    private final double[] sparkOffX = new double[8];
    private final double[] sparkOffY = new double[8];

    public SpawnEffect(Vector2D position, double delayDuration, double beamDuration) {
        this.position = position;
        this.delayDuration = delayDuration;
        this.beamDuration = beamDuration;
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sparkOffX[i] = (random.nextDouble() - 0.5) * 40.0;
            sparkOffY[i] = (random.nextDouble() - 0.5) * 20.0;
        }
    }

    public void update(double deltaSeconds) {
        if (!active || finished) return;

        timer += deltaSeconds;
        if (timer >= delayDuration + beamDuration) {
            finished = true;
            active = false;
        }
    }

    /**
     * An toan bo nhan vat khi phat sang , hien khi xong
     */
    public double getEntityAlpha() {
        if (timer < delayDuration) return 0.0;
        double progress = (timer - delayDuration) / beamDuration;
        if (progress < 0.4) return 0.0;
        return Math.min(1.0, (progress - 0.4) / 0.6);
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || timer < delayDuration || finished) return;

        double progress = (timer - delayDuration) / beamDuration;
        double zoom = camera.getZoom();

//       don vi 1 pĩel tren zoom
        double pixelSize = Math.max(2.0, Math.floor(3.0 * zoom));

        // lay vi tri va lam tron theo tam luoi
        double rawX = camera.worldToScreenX(position.getX());
        double rawY = camera.worldToScreenY(position.getY());
        double screenX = Math.floor(rawX / pixelSize) * pixelSize;
        double screenY = Math.floor(rawY / pixelSize) * pixelSize;

        // tinh toan do rong cot sang
        double maxBeamWidth = 48.0 * zoom;
        double currentWidth;

        if (progress < 0.25) {
            currentWidth = (progress / 0.25) * maxBeamWidth;
        } else {
            currentWidth = (1.0 - (progress - 0.25) / 0.75) * maxBeamWidth;
        }

        // lam tron do rong pixel
        double beamWidth = Math.floor((currentWidth) / pixelSize) * pixelSize;
        if (beamWidth < pixelSize * 2) return;

        gc.save();

        double topY = Math.floor((screenY - 500 * zoom) / pixelSize) * pixelSize;
        double beamHeight = screenY - topY;

//        ve cot sang
        gc.setFill(Color.rgb(120, 230, 255, 0.6));
        gc.fillRect(screenX - beamWidth / 2.0, topY, beamWidth, beamHeight);

        double coreWidth = Math.floor((beamWidth * 0.6) / pixelSize) * pixelSize;
        if (coreWidth >= pixelSize) {
            gc.setFill(Color.rgb(220, 245, 255, 0.85));
            gc.fillRect(screenX - coreWidth / 2.0, topY, coreWidth, beamHeight);
        }

        double centerWidth = Math.floor((beamWidth * 0.25) / pixelSize) * pixelSize;
        if (centerWidth >= pixelSize) {
            gc.setFill(Color.WHITE);
            gc.fillRect(screenX - centerWidth / 2.0, topY, centerWidth, beamHeight);
        }

//      ve de sang duoi chan
        double padWidth = beamWidth + 8 * pixelSize;
        double padHeight = 4 * pixelSize;
        gc.setFill(Color.rgb(0, 210, 255, 0.9));
        gc.fillRect(screenX - padWidth / 2.0, screenY - padHeight / 2.0, padWidth, padHeight);

        gc.setFill(Color.WHITE);
        gc.fillRect(screenX - beamWidth / 2.0, screenY - (padHeight - 2 * pixelSize) / 2.0, beamWidth, padHeight - 2 * pixelSize);
//        Hat sang ban tung toe
        gc.setFill(Color.CYAN);
        for (int i = 0; i < 8; i++) {
            double sparkX = screenX + (sparkOffX[i] * zoom * (0.5 + progress));
            double sparkY = screenY + (sparkOffY[i] * zoom * (0.5 + progress));
            double pX = Math.floor(sparkX / pixelSize) * pixelSize;
            double pY = Math.floor(sparkY / pixelSize) * pixelSize;

            gc.fillRect(pX, pY, pixelSize, pixelSize);
        }

        gc.restore();
    }

    public boolean isSpawning() {
        return active && !finished;
    }

    public boolean isFinished() {
        return finished;
    }
}