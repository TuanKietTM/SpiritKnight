package com.soulknight.entity;

import com.soulknight.buff.BuffManager;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.SlashEffect;
import com.soulknight.weapon.Weapon;
import com.soulknight.entity.PlayerAnimator;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

public final class Player extends Entity {

    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0)
            .withImage("/assets/WeaponImage/GunImage/OldPistol.png");

    private final HeroType heroType;
    private final PlayerAnimator animator;
    private boolean isFacingLeft = false;
    // Goc ngam ban hien tai (radian), 0 = huong sang phai
    private double aimAngle = 0.0;
    // Vi tri nong sung theo chieu cao anh (0 = dinh anh, 1 = day anh).
    // Dieu chinh gia tri nay de dau nong nam dung tren duong ngam (noi dan bay ra).
    // 0.5 = giua anh; tang len neu dan bay cao hon nong, giam neu dan bay thap hon.
    private static final double BARREL_HEIGHT_FRACTION = 0.5;
    private PlayerAnimator.State movementState = PlayerAnimator.State.IDLE;
    private double invulnerabilityTimer = 0.0;
    // Thời gian bất tử khi trúng đòn
    private final double MAX_INVULNERABILITY_TIME = 0.3;
    // Dem nguoc thoi gian vung chem: khi > 0 thi an vu khi dang cam,
    // vi SlashEffect da ve san thanh kiem trong sprite sheet
    private double meleeSwingTimer = 0.0;

    // Quan ly cac buff dang hoat dong tren Player
    private final BuffManager buffManager;

    // He so buff, 1.0 = giu nguyen chi so goc
    private double buffSpeedMultiplier = 1.0;
    private double buffDamageMultiplier = 1.0;

    // Phan tram giam sat thuong, vi du 0.5 = giam 50%
    private double buffDamageReduction = 0.0;

    public Player(Vector2D spawnPoint) {
        this(spawnPoint, HeroSelectionManager.getInstance().getSelectedHero());
    }

    public Player(Vector2D spawnPoint, HeroType heroType) {
        super(spawnPoint, Constants.PLAYER_RADIUS, heroType == null ? Constants.PLAYER_HEALTH : heroType.getMaxHealth(),
                Color.DODGERBLUE
        );

        this.heroType = heroType == null ? HeroType.KNIGHT : heroType;

        this.animator = new PlayerAnimator(this.heroType);
        this.buffManager = new BuffManager(this);
    }

    public String getWeaponName() {
        return weapon.getName();
    }

    public void equipWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    // Vu khi dang cam (dung de kiem tra loai vu khi khi doi qua lai)
    public Weapon getWeapon() {
        return weapon;
    }

    public HeroType getHeroType() {
        return heroType;
    }

    // Tạo thời gian bất tử để giảm đòn đánh liên tục
    @Override
    public void takeDamage(int amount) {
        if (invulnerabilityTimer > 0.0 || amount <= 0) {
            return;
        }

        int finalDamage = (int) Math.round(amount * (1.0 - buffDamageReduction));
        super.takeDamage(Math.max(0, finalDamage));

        if (finalDamage > 0) {
            this.invulnerabilityTimer = MAX_INVULNERABILITY_TIME;
        }
    }

    // Bat dau vung chem: an kiem dang cam trong suot thoi luong hieu ung chem
    public void startMeleeSwing() {
        meleeSwingTimer = SlashEffect.SWING_DURATION;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (invulnerabilityTimer > 0.0) {
            invulnerabilityTimer = Math.max(0.0, invulnerabilityTimer - deltaSeconds);
        }
        if (meleeSwingTimer > 0.0) {
            meleeSwingTimer = Math.max(0.0, meleeSwingTimer - deltaSeconds);
        }
        weapon.tick(deltaSeconds);
        buffManager.tick(deltaSeconds);

        double dx = 0.0;
        double dy = 0.0;

        boolean isTouchpad = world.getInputHandler().isTouchpadModeEnabled();

        //  Kiểm tra di chuyển bằng Touchpad Joystick (360 do)
        Vector2D touchpadDir = world.getInputHandler().getTouchpadJoystick().getMoveDirection();

        if (touchpadDir.length() > 0.0) {
            dx = touchpadDir.getX();
            dy = touchpadDir.getY();
        } else {
            // Neu khong dung touchpad dung WASD , mui ten
            if (world.getInputHandler().isDown(KeyCode.W) || world.getInputHandler().isDown(KeyCode.UP)) {
                dy -= 1.0;
            }
            if (world.getInputHandler().isDown(KeyCode.S) || world.getInputHandler().isDown(KeyCode.DOWN)) {
                dy += 1.0;
            }
            if (world.getInputHandler().isDown(KeyCode.A) || world.getInputHandler().isDown(KeyCode.LEFT)) {
                dx -= 1.0;
            }
            if (world.getInputHandler().isDown(KeyCode.D) || world.getInputHandler().isDown(KeyCode.RIGHT)) {
                dx += 1.0;
            }
        }

        // Xử lý di chuyển
        Vector2D movement = new Vector2D(dx, dy);
        if (movement.length() > 0.0) {
            this.movementState = PlayerAnimator.State.RUN;

            if (touchpadDir.length() == 0.0) {
                movement.normalize();
            }

            movement.scale(getSpeed() * deltaSeconds);
            this.move(world, movement.getX(), movement.getY());
        } else {
            this.movementState = PlayerAnimator.State.IDLE;
        }

        // 3. Xử lý hướng nhìn (Facing) và Ngắm bắn (Aiming)
        Vector2D attackTargetPos = null;

        if (isTouchpad) {
            // MODE  TOUCHPAD: AUTO-AIM & AUTO-FACE
            Enemy nearestEnemy = findNearestEnemy(world.getEnemies());

            if (nearestEnemy != null) {
                attackTargetPos = nearestEnemy.getPosition();
                // Quay mặt về phía kẻ địch đang bị khóa mục tiêu
                double diffX = attackTargetPos.getX() - getPosition().getX();
                isFacingLeft = (diffX < 0);
            } else {
                // Nếu không có kẻ địch xung quanh, quay mặt theo hướng di chuyển Joystick
                if (dx < 0) {
                    isFacingLeft = true;
                } else if (dx > 0) {
                    isFacingLeft = false;
                }
            }
        } else {
            //  MODE MOUSE & KEYBOARD
            Vector2D mousePos = world.getMouseWorldPosition();
            if (mousePos != null) {
                attackTargetPos = mousePos;
//               Quay mat knight theo con tro chuot
                double diffX = mousePos.getX() - getPosition().getX();
                isFacingLeft = (diffX < 0);
            } else if (movement.length() > 0.0) {
                if (dx < 0) isFacingLeft = true;
                else if (dx > 0) isFacingLeft = false;
            }
        }

        // Cap nhat goc ngam de sung xoay theo huong ban
        if (attackTargetPos != null) {
            double aimDx = attackTargetPos.getX() - getPosition().getX();
            double aimDy = attackTargetPos.getY() - getPosition().getY();
            if (aimDx != 0.0 || aimDy != 0.0) {
                aimAngle = Math.atan2(aimDy, aimDx);
            }
        } else if (movement.length() > 0.0) {
            // Khong co muc tieu thi sung huong theo huong di chuyen
            aimAngle = Math.atan2(dy, dx);
        }

        // Xu ly tan cong
        if (world.getInputHandler().isFireHeld() && attackTargetPos != null) {
            weapon.attack(world, this, attackTargetPos);
        }

        animator.update(movementState, deltaSeconds);
    }

    // Thuat toan tim kiem ke thu gan nhat

    private Enemy findNearestEnemy(List<Enemy> enemies) {
        if (enemies == null || enemies.isEmpty()) {
            return null;
        }

        Enemy nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }

            double distSq = getPosition().distanceSquared(enemy.getPosition());
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearest = enemy;
            }
        }

        return nearest;
    }

    @Override
    public void render(javafx.scene.canvas.GraphicsContext graphicsContext, com.soulknight.engine.Camera camera) {
        double worldWidth = 24.0;
        double worldHeight = 24.0;
        animator.render(graphicsContext, camera, getPosition().getX(), getPosition().getY(), worldWidth, worldHeight,
                getRadius(), isFacingLeft
        );

        // Ve sung tren tay nhan vat, xoay theo huong ban
        renderWeapon(graphicsContext, camera);

        // Ve hieu ung buff
        buffManager.render(graphicsContext);
    }

    /**
     * Ve vu khi nhan vat dang cam.
     * Sung duoc xoay quanh vi tri tay theo goc aimAngle,
     * dau nong sung trung voi vi tri dan bay ra (Gun.MUZZLE_DISTANCE).
     */
    private void renderWeapon(javafx.scene.canvas.GraphicsContext gc, com.soulknight.engine.Camera camera) {
        if (weapon == null) {
            return;
        }
        // Dang vung chem: SlashEffect ve thanh kiem thay the, an kiem dang cam
        if (meleeSwingTimer > 0.0) {
            return;
        }
        Image weaponImage = weapon.getImage();
        if (weaponImage == null || weaponImage.getWidth() <= 1.0) {
            return;
        }

        double zoom = camera.getZoom();

        // Mot so anh vu khi ve theo chieu doc (vi du Wand: dau huong len tren).
        // Nhan dien de xoay lai cho dau vu khi chi dung huong ngam.
        boolean portraitSprite = weaponImage.getHeight() > weaponImage.getWidth();
        // Kich thuoc anh tinh theo truc vu khi: chieu dai doc theo huong ngam,
        // be day vuong goc voi huong ngam
        double spriteLength = portraitSprite ? weaponImage.getHeight() : weaponImage.getWidth();
        double spriteThickness = portraitSprite ? weaponImage.getWidth() : weaponImage.getHeight();

        // Chieu dai vu khi (world): du dai de dau nong cham toi MUZZLE_DISTANCE,
        // phan du ra phia sau la bao tay cam nam sau diem xoay.
        // Scale theo canh dai nhat cua anh nen vu khi luon vua tam nhan vat.
        double gunWorldLength = Gun.MUZZLE_DISTANCE + 6.0;
        double gunWorldHeight = gunWorldLength * (spriteThickness / spriteLength);

        // Diem xoay = tam nhan vat (cung goc voi noi Gun tinh diem dan bay ra)
        double pivotScreenX = camera.worldToScreenX(getPosition().getX());
        double pivotScreenY = camera.worldToScreenY(getPosition().getY());

        double renderLength = gunWorldLength * zoom;
        double renderHeight = gunWorldHeight * zoom;

        // Dat canh phai (dau nong) cua anh dung ngay tai MUZZLE_DISTANCE
        // -> dan bay ra trung khop voi dau nong sung tren man hinh
        double muzzleScreenX = Gun.MUZZLE_DISTANCE * zoom;
        double leftX = muzzleScreenX - renderLength;
        // Dich sprite theo phuong doc sao cho nong sung (BARREL_HEIGHT_FRACTION)
        // nam dung tren duong ngam (y = 0). Diem nay bat bien khi lat doc.
        double topY = -BARREL_HEIGHT_FRACTION * renderHeight;

        gc.save();
        gc.setImageSmoothing(false);

        // Dua he toa do ve tam nhan vat va xoay theo goc ngam
        gc.translate(pivotScreenX, pivotScreenY);
        gc.rotate(Math.toDegrees(aimAngle));
        // Khi ngam sang trai, lat doc sprite de sung khong bi nguoc dau
        if (isFacingLeft) {
            gc.scale(1, -1);
        }

        // Ve vu khi: dau (nong sung / dau wand) ben phai tai MUZZLE_DISTANCE, tay cam ben trai
        if (portraitSprite) {
            // Anh doc: xoay them 90 do de "huong len" cua anh trung voi huong ngam.
            // Sau khi xoay, chieu cao anh chay doc theo huong ngam.
            gc.rotate(90);
            gc.drawImage(weaponImage,
                    -BARREL_HEIGHT_FRACTION * renderHeight, -muzzleScreenX,
                    renderHeight, renderLength);
        } else {
            gc.drawImage(weaponImage, leftX, topY, renderLength, renderHeight);
        }

        gc.restore();
    }
    public BuffManager getBuffManager() {
        return buffManager;
    }

    public void setBuffSpeedMultiplier(double multiplier) {
        buffSpeedMultiplier = Math.max(0.1, multiplier);
    }

    public double getBuffSpeedMultiplier() {
        return buffSpeedMultiplier;
    }

    public void setBuffDamageMultiplier(double multiplier) {
        buffDamageMultiplier = Math.max(0.0, multiplier);
    }

    public double getBuffDamageMultiplier() {
        return buffDamageMultiplier;
    }

    public int calculateBuffedDamage(int baseDamage) {
        return Math.max(0, (int) Math.round(baseDamage * buffDamageMultiplier));
    }

    public void setBuffDamageReduction(double reduction) {
        buffDamageReduction = Math.max(0.0, Math.min(0.9, reduction));
    }

    public double getBuffDamageReduction() {
        return buffDamageReduction;
    }

    public double getSpeed() {
        return Constants.PLAYER_SPEED * buffSpeedMultiplier;
    }

    public void clearBuffs() {
        buffManager.clear();
        buffSpeedMultiplier = 1.0;
        buffDamageMultiplier = 1.0;
        buffDamageReduction = 0.0;
    }
}