package com.soulknight.entity;

import com.soulknight.buff.BuffManager;
import com.soulknight.buff.BuffType;
import com.soulknight.debuff.DebuffType;
import com.soulknight.debuff.PlayerDebuffManager;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.*;
import com.soulknight.weapon.render.IonChargeRenderer;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

public final class Player extends Entity {

    private static final double BARREL_HEIGHT_FRACTION = 0.5;
    private static final double SHIELD_REGEN_DELAY = 4.0;
    private static final double SHIELD_REGEN_INTERVAL = 1.0;
    private final double MAX_INVULNERABILITY_TIME = 0.3;
    // Quan ly cac buff dang hoat dong tren Player
    private final BuffManager buffManager;
    private final PlayerDebuffManager debuffManager = new PlayerDebuffManager();
    private final double maxMana = 200.0;
    private final int maxShield = 6;
    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0)
            .withImage("/assets/WeaponImage/GunImage/OldPistol.png");
    private boolean isFacingLeft = false;
    private double aimAngle = 0.0;
    private double invulnerabilityTimer = 0.0;
    private double meleeSwingTimer = 0.0;
    private double buffSpeedMultiplier = 1.0;
    private double buffDamageMultiplier = 1.0;
    private double buffDamageReduction = 0.0;
    private double mana = 200.0;
    // Shield hap thu damage truoc HP.
    private int shield = 6;
    private double shieldRegenDelay;
    private double shieldRegenTimer;
    private double lastMoveX = 1.0;
    private double lastMoveY = 0.0;
    private boolean fireHeldLastFrame;
    private final HeroType heroType;
    private final PlayerAnimator animator;
    private final BuffManager buffManager;
    private Runnable dragonBreathAction;
    private Runnable holyNovaAction;
    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0)
            .withImage("/assets/WeaponImage/GunImage/OldPistol.png");
    private PlayerAnimator.State movementState = PlayerAnimator.State.IDLE;

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

    public Weapon getWeapon() {
        return weapon;
    }

    public HeroType getHeroType() {
        return heroType;
    }

    @Override
    public void takeDamage(int amount) {
        if (invulnerabilityTimer > 0.0 || amount <= 0 || !isAlive()) {
            return;
        }
        int modifiedAmount = debuffManager.modifyIncomingDamage(amount);
        int finalDamage = (int) Math.round(modifiedAmount * (1.0 - buffDamageReduction));
        finalDamage = Math.max(0, finalDamage);
        if (finalDamage <= 0) {
            return;
        }
        /*
         * HOLY NOVA
         * Neu don damage nay du de giet Player,
         * huy hoan toan damage va kich hoat Holy Nova.
         */
        // Moi lan trung damage thi reset thoi gian hoi Shield.
        shieldRegenDelay = SHIELD_REGEN_DELAY;
        shieldRegenTimer = 0.0;

        int remainingDamage = finalDamage;

        // Tru damege vao shield truoc
        if (shield > 0) {
            int absorbedDamage = Math.min(shield, remainingDamage);

            shield -= absorbedDamage;
            remainingDamage -= absorbedDamage;
        }

        if (remainingDamage <= 0) {
            invulnerabilityTimer = MAX_INVULNERABILITY_TIME;

            if (buffManager.isActive(BuffType.SHIELD)) {
                buffManager.notifyPlayerHit();
            }

            return;
        }

        boolean lethalDamage = remainingDamage >= getHealth();

        if (lethalDamage && buffManager.isActive(BuffType.HOLY_NOVA)) {
            invulnerabilityTimer = MAX_INVULNERABILITY_TIME;
            requestHolyNova();
            return;
        }

        int healthBefore = getHealth();

        super.takeDamage(remainingDamage);

        int realDamage = healthBefore - getHealth();
        if (realDamage <= 0) {
            return;
        }
        invulnerabilityTimer = MAX_INVULNERABILITY_TIME;
        if (buffManager.isActive(BuffType.SHIELD)) {
            buffManager.notifyPlayerHit();
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
        updateShield(deltaSeconds);
        weapon.tick(deltaSeconds);
        buffManager.tick(deltaSeconds);
//cap nhat dem nguoc thoi gian debuff va kich hoat rut mau khi trung doc
        debuffManager.update(this, deltaSeconds);

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
// Dao chieu huong di chuyen khi dinh phai confusion
        double dirMultiplier = debuffManager.getMovementDirectionMultiplier();
        dx *= dirMultiplier;
        dy *= dirMultiplier;

        // Xử lý di chuyển
        Vector2D movement = new Vector2D(dx, dy);
        if (movement.length() > 0.0) {
            double moveLength = movement.length();
            if (moveLength > 0.001) {
                lastMoveX = movement.getX() / moveLength;
                lastMoveY = movement.getY() / moveLength;
            }
            this.movementState = PlayerAnimator.State.RUN;

            if (touchpadDir.length() == 0.0) {
                movement.normalize();
            }
            double weaponSpeedMultiplier = 1.0;

// Ion Gun lam cham Player theo muc charge hien tai.
            if (weapon instanceof IonElectromagneticGun ionGun && ionGun.isCharging()) {
                weaponSpeedMultiplier = ionGun.getMoveSpeedMultiplier();
            }
//            slow khi gap phai giam toc
            double debuffSpeedMultiplier = debuffManager.getSpeedModifier();

            movement.scale(getSpeed() * weaponSpeedMultiplier * debuffSpeedMultiplier * deltaSeconds);
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

        // Xu ly tan cong.
        boolean fireHeld = world.getInputHandler().isFireHeld();

        if (weapon instanceof PrototypeRailgun railgun) {

            if (fireHeld && attackTargetPos != null) {
                railgun.charge(deltaSeconds, attackTargetPos);
            }

            if (!fireHeld && fireHeldLastFrame) {
                railgun.release(world, this, attackTargetPos);
            }

        } else if (weapon instanceof IonElectromagneticGun ionGun) {

            if (fireHeld && attackTargetPos != null) {
                ionGun.charge(deltaSeconds, attackTargetPos);
            }

            if (!fireHeld && fireHeldLastFrame) {
                ionGun.release(world, this, attackTargetPos);
            }

        } else if (fireHeld && attackTargetPos != null) {

            weapon.attack(world, this, attackTargetPos);
        }

        fireHeldLastFrame = fireHeld;

        animator.update(movementState, deltaSeconds);
    }

    // Thuat toan tim kiem ke thu gan nhat de phu tro cho viec tu dong nham ban cua touchpad

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
    public void render(
            javafx.scene.canvas.GraphicsContext graphicsContext,
            com.soulknight.engine.Camera camera
    ) {
        double worldWidth = 24.0;
        double worldHeight = 24.0;

        /*
         * Ve nua sau cua quy dao Shield truoc Player. va ve nua sau cua vong tron confusion
         */
        buffManager.renderBehind(graphicsContext, camera);
        debuffManager.renderBack(graphicsContext, camera, this);

        /*
         * Ve Player.
         */
        animator.render(graphicsContext, camera, getPosition().getX(), getPosition().getY(), worldWidth, worldHeight, getRadius(), isFacingLeft);

        /*
         * Ve vu khi tren tay.
         */
        renderWeapon(graphicsContext, camera);
//        neu lan sung ion thi co them giai doan nao nang luong
        if (weapon instanceof IonElectromagneticGun ionGun) {
            IonChargeRenderer.render(this, ionGun, graphicsContext, camera, aimAngle);
        }

        /*
         * Ve nua truoc xuat hien cua cac buff. nua truoc cua confusion(debuff)
         */
        buffManager.renderFront(graphicsContext, camera);
        debuffManager.renderFront(graphicsContext, camera, this);
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

    public void setBuffDamageMultiplier(double multiplier) {
        buffDamageMultiplier = Math.max(0.0, multiplier);
    }

    public void setBuffDamageReduction(double reduction) {
        buffDamageReduction = Math.max(0.0, Math.min(0.9, reduction));
    }

    public double getSpeed() {
        return Constants.PLAYER_SPEED * buffSpeedMultiplier;
    }

    public void setDragonBreathAction(Runnable dragonBreathAction) {
        this.dragonBreathAction = dragonBreathAction;
    }

    public void requestDragonBreath() {
        if (dragonBreathAction != null) {
            dragonBreathAction.run();
        }
    }

    public void setHolyNovaAction(Runnable holyNovaAction) {
        this.holyNovaAction = holyNovaAction;
    }

    private void requestHolyNova() {
        if (holyNovaAction != null) {
            holyNovaAction.run();
        }
    }

    public double getLastMoveX() {
        return lastMoveX;
    }

    public double getLastMoveY() {
        return lastMoveY;
    }

    // Shield hoi tung diem sau khi Player khong bi danh mot khoang thoi gian.
    private void updateShield(double deltaSeconds) {
        if (shield >= maxShield) return;

        if (shieldRegenDelay > 0.0) {
            shieldRegenDelay = Math.max(0.0, shieldRegenDelay - deltaSeconds);
            return;
        }

        shieldRegenTimer += Math.max(0.0, deltaSeconds);

        if (shieldRegenTimer >= SHIELD_REGEN_INTERVAL) {
            shieldRegenTimer -= SHIELD_REGEN_INTERVAL;
            shield = Math.min(maxShield, shield + 1);
        }
    }

    public double getMana() {
        return mana;
    }

    public void setMana(double mana) {
        this.mana = Math.max(
                0.0,
                Math.min(maxMana, mana)
        );
    }

    public double getMaxMana() {
        return maxMana;
    }

    // Tru mana, false neu khong du.
    public boolean consumeMana(double amount) {
        double cost = Math.max(0.0, amount);

        if (cost <= 0.0) return true;
        if (mana < cost) return false;

        mana -= cost;
        return true;
    }

    // Hoi mana nhung khong vuot max.
    public void restoreMana(double amount) {
        if (amount <= 0.0) return;

        mana = Math.min(maxMana, mana + amount);
    }

    // Hoi HP nhung khong vuot qua max health.
    public void restoreHealth(int amount) {
        if (amount <= 0 || !isAlive()) return;

        int newHealth = Math.min(getMaxHealth(), getHealth() + amount);
        setHealth(newHealth);
    }

    public int getShield() {
        return shield;
    }

    public void setShield(int shield) {
        this.shield = Math.max(
                0,
                Math.min(maxShield, shield)
        );
    }

    public int getMaxShield() {
        return maxShield;
    }

    public PlayerDebuffManager getDebuffManager() {
        return debuffManager;
    }

    // Hoi Shield nhung khong vuot qua max.
    public void restoreShield(int amount) {
        if (amount <= 0) return;

        shield = Math.min(maxShield, shield + amount);
    }
}