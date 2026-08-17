package com.soulknight.entity;

import com.soulknight.debuff.DebuffItem;
import com.soulknight.debuff.DebuffSpawner;
import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class UFOEvent {

    public enum Type {
        DEBUFF,
        REINFORCEMENT
    }

    private Type eventType;
    private static int nextDebuffIndex = 0;
    private static final int TOTAL_DEBUFF_TYPES = 5;

    private static Image ufoSprite;
    private Vector2D position;
    private Vector2D targetPosition;
    private double speed = 350.0;
    private double hoverTimer = 0.0;
    private boolean actionExecuted = false; // Đổi tên từ debuffSummoned để dùng chung
    private boolean finished = false;

    private boolean isLaserAttack = false;
    private boolean hasDealtDamage = false;

    private State state = State.ENTERING;
    private enum State { ENTERING, HOVERING, LEAVING }

    static {
        try {
            ufoSprite = new Image(UFOEvent.class.getResourceAsStream("/assets/Enemy/UFO.png"));
        } catch (Exception e) {
            // Ignored if image missing
        }
    }

    // Constructor mặc định (tạo sự kiện Debuff)
    public UFOEvent(Vector2D playerPos) {
        this(playerPos, Type.DEBUFF);
    }

    // Constructor linh hoạt nhận loại sự kiện (DEBUFF hoặc REINFORCEMENT)
    public UFOEvent(Vector2D playerPos, Type type) {
        this.position = new Vector2D(playerPos.getX() + (Math.random() * 200 - 100), playerPos.getY() - 500);
        this.targetPosition = playerPos.copy();
        this.eventType = type;
        this.isLaserAttack = Math.random() < 0.25;
        SoundManager.getInstance().playSFX("UFO");
    }

    public void update(GameWorld world, double deltaSeconds) {
        if (finished) return;

        switch (state) {
            case ENTERING:
                // Cập nhật vị trí đuổi theo Player khi đang bay vào
                if (world.getPlayer() != null) {
                    this.targetPosition = world.getPlayer().getPosition().copy();
                }
                Vector2D overheadPos = new Vector2D(targetPosition.getX(), targetPosition.getY() - 120);
                Vector2D dir = overheadPos.copy().subtract(position);

                if (dir.length() <= 15.0) {
                    state = State.HOVERING;
                    hoverTimer = 1.2;
                    // Chốt vị trí nhắm bắn / thả viện binh
                    if (world.getPlayer() != null) {
                        this.targetPosition = world.getPlayer().getPosition().copy();
                    }
                } else {
                    dir.normalize().scale(speed * deltaSeconds);
                    position.add(dir.getX(), dir.getY());
                }
                break;

            case HOVERING:
                hoverTimer -= deltaSeconds;

                // Thực thi hành động tương ứng với eventType khi hover timer đạt mốc
                if (!actionExecuted && hoverTimer <= 0.6) {
                    if (eventType == Type.DEBUFF) {
                        spawnSequentialDebuff(world);
                    } else if (eventType == Type.REINFORCEMENT) {
                        world.checkAndTriggerUFOSummon(targetPosition);
                    }
                    actionExecuted = true;
                }

                if (isLaserAttack && !hasDealtDamage && hoverTimer <= 0.9) {
                    checkLaserHitPlayer(world);
                }

                if (hoverTimer <= 0) {
                    state = State.LEAVING;
                    targetPosition = new Vector2D(position.getX() + 400, position.getY() - 600);
                }
                break;

            case LEAVING:
                Vector2D leaveDir = targetPosition.copy().subtract(position);
                if (leaveDir.length() <= 15.0) {
                    finished = true;
                } else {
                    leaveDir.normalize().scale(speed * 1.5 * deltaSeconds);
                    position.add(leaveDir.getX(), leaveDir.getY());
                }
                break;
        }
    }

    private void spawnSequentialDebuff(GameWorld world) {
        DebuffItem debuff = DebuffSpawner.spawnTypeAtIndex(nextDebuffIndex, targetPosition);
        if (debuff != null) {
            world.getItems().add(debuff);
        }
        nextDebuffIndex = (nextDebuffIndex + 1) % TOTAL_DEBUFF_TYPES;
    }

    private void checkLaserHitPlayer(GameWorld world) {
        if (world.getPlayer() == null) return;
        Vector2D playerPos = world.getPlayer().getPosition();

        double dx = playerPos.getX() - targetPosition.getX();
        double dy = playerPos.getY() - targetPosition.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 75.0) {
            world.getPlayer().takeDamage(15);
            hasDealtDamage = true;
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (finished) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double targetScreenX = camera.worldToScreenX(targetPosition.getX());
        double targetScreenY = camera.worldToScreenY(targetPosition.getY());

        gc.save();
        if (state == State.HOVERING) {
            renderBeamEffect(gc, screenX, screenY, targetScreenX, targetScreenY);
        }

        if (ufoSprite != null) {
            gc.drawImage(ufoSprite, screenX - 32, screenY - 32, 64, 64);
        } else {
            gc.setFill(Color.SILVER);
            gc.fillOval(screenX - 25, screenY - 10, 50, 20);
            gc.setFill(Color.CYAN);
            gc.fillOval(screenX - 12, screenY - 15, 24, 12);
        }

        gc.restore();
    }

    private void renderBeamEffect(GraphicsContext gc, double sx, double sy, double tx, double ty) {
        double beamWidthTop = 20.0;
        double beamWidthBottom = isLaserAttack ? 80.0 : 45.0;

        double[] xPoints = { sx - beamWidthTop / 2, sx + beamWidthTop / 2, tx + beamWidthBottom / 2, tx - beamWidthBottom / 2 };
        double[] yPoints = { sy + 15, sy + 15, ty, ty };

        Color mainColor = isLaserAttack ? Color.RED : Color.LIMEGREEN;

        LinearGradient gradient = new LinearGradient(
                0, sy, 0, ty, false, CycleMethod.NO_CYCLE,
                new Stop(0, mainColor.deriveColor(0, 1, 1, 0.8)),
                new Stop(1, mainColor.deriveColor(0, 1, 1, 0.15))
        );

        gc.setFill(gradient);
        gc.fillPolygon(xPoints, yPoints, 4);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(isLaserAttack ? 5 : 2);
        gc.strokeLine(sx, sy + 15, tx, ty);
    }

    public boolean isFinished() {
        return finished;
    }

    public Type getEventType() {
        return eventType;
    }
}