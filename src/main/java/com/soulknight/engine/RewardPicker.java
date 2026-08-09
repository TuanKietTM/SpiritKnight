package com.soulknight.engine;

import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Weapon;
import com.soulknight.weapon.WeaponType;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Man hinh chon phan thuong sau khi clear room.
 *
 * Buoc 1:
 * - Chon GOLD
 * - Chon WEAPON
 *
 * Buoc 2 neu chon WEAPON:
 * - Chon thay Slot 1
 * - Chon thay Slot 2
 */
public final class RewardPicker {

    private static final double VIEWPORT_WIDTH = Constants.WINDOW_WIDTH;
    private static final double VIEWPORT_HEIGHT = Constants.WINDOW_HEIGHT;

    private static final double CARD_WIDTH = 280.0;
    private static final double CARD_HEIGHT = 340.0;
    private static final double CARD_GAP = 60.0;

    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.72);
    private static final Color CARD_FILL = Color.rgb(30, 30, 45, 0.95);
    private static final Color CARD_BORDER = Color.rgb(120, 120, 150);
    private static final Color CARD_HOVER_BORDER = Color.GOLD;

    private enum Mode {
        REWARD,
        WEAPON_SLOT
    }

    private final Weapon weapon;
    private final int goldAmount;

    private final double goldCardX;
    private final double goldCardY;
    private final double weaponCardX;
    private final double weaponCardY;

    private Mode mode = Mode.REWARD;

    private WeaponType slot1Weapon;
    private WeaponType slot2Weapon;

    public RewardPicker(Weapon weapon, int goldAmount) {
        this.weapon = weapon;
        this.goldAmount = Math.max(0, goldAmount);

        double totalWidth = CARD_WIDTH * 2 + CARD_GAP;
        double startX = (VIEWPORT_WIDTH - totalWidth) / 2.0;
        double centerY = (VIEWPORT_HEIGHT - CARD_HEIGHT) / 2.0;

        this.goldCardX = startX;
        this.goldCardY = centerY;
        this.weaponCardX = startX + CARD_WIDTH + CARD_GAP;
        this.weaponCardY = centerY;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public int getGoldAmount() {
        return goldAmount;
    }

    public boolean isChoosingWeaponSlot() {
        return mode == Mode.WEAPON_SLOT;
    }

    /**
     * Chuyen picker sang buoc chon slot se bi thay the.
     * Reward weapon luc nay chua duoc equip.
     */
    public void showWeaponSlotSelection(WeaponType slot1Weapon, WeaponType slot2Weapon) {
        this.slot1Weapon = slot1Weapon;
        this.slot2Weapon = slot2Weapon;
        this.mode = Mode.WEAPON_SLOT;
    }

    public void render(GraphicsContext gc, Vector2D mousePosition) {
        if (gc == null) return;

        renderOverlay(gc);

        if (mode == Mode.WEAPON_SLOT) {
            renderWeaponSlotSelection(gc, mousePosition);
            return;
        }

        renderRewardSelection(gc, mousePosition);
    }

    private void renderOverlay(GraphicsContext gc) {
        gc.save();
        gc.setFill(OVERLAY_COLOR);
        gc.fillRect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        gc.restore();
    }

    /**
     * Buoc 1: chon vang hoac weapon.
     */
    private void renderRewardSelection(GraphicsContext gc, Vector2D mousePosition) {
        drawTitle(gc, "CHOOSE YOUR REWARD", "Click a card to claim the reward");

        boolean hoverGold = isInside(mousePosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT);
        boolean hoverWeapon = isInside(mousePosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT);

        renderGoldCard(gc, hoverGold);
        renderWeaponCard(gc, hoverWeapon);
    }

    /**
     * Buoc 2: chon slot nao se bi weapon reward thay the.
     */
    private void renderWeaponSlotSelection(GraphicsContext gc, Vector2D mousePosition) {
        String rewardName = weapon != null ? weapon.getName() : "Unknown";

        drawTitle(gc, "REPLACE WEAPON SLOT", "Choose a slot for " + rewardName);

        boolean hoverSlot1 = isInside(mousePosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT);
        boolean hoverSlot2 = isInside(mousePosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT);

        renderSlotCard(gc, goldCardX, goldCardY, 1, slot1Weapon, hoverSlot1);
        renderSlotCard(gc, weaponCardX, weaponCardY, 2, slot2Weapon, hoverSlot2);
    }

    private void drawTitle(GraphicsContext gc, String title, String subtitle) {
        gc.save();
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("System", FontWeight.BOLD, 34));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(title, VIEWPORT_WIDTH / 2.0, goldCardY - 60);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(subtitle, VIEWPORT_WIDTH / 2.0, goldCardY - 30);
        gc.restore();
    }

    /**
     * Tra ve:
     * - gold
     * - weapon
     * - slot1
     * - slot2
     */
    public String handleClick(Vector2D clickPosition) {
        if (clickPosition == null) return null;

        if (mode == Mode.WEAPON_SLOT) {
            if (isInside(clickPosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT)) return "slot1";
            if (isInside(clickPosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT)) return "slot2";
            return null;
        }

        if (isInside(clickPosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT)) return "gold";
        if (isInside(clickPosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT)) return "weapon";

        return null;
    }

    private boolean isInside(Vector2D point, double rectX, double rectY, double rectWidth, double rectHeight) {
        if (point == null) return false;

        return point.getX() >= rectX && point.getX() <= rectX + rectWidth
                && point.getY() >= rectY && point.getY() <= rectY + rectHeight;
    }

    private void renderGoldCard(GraphicsContext gc, boolean hovered) {
        drawCardBackground(gc, goldCardX, goldCardY, hovered);

        double centerX = goldCardX + CARD_WIDTH / 2.0;

        gc.save();
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GOLD", centerX, goldCardY + 40);
        gc.restore();

        double coinCenterX = centerX;
        double coinCenterY = goldCardY + 130;
        double coinRadius = 40.0;

        gc.save();
        gc.setFill(Color.rgb(180, 130, 0));
        gc.fillOval(coinCenterX - coinRadius - 3, coinCenterY - coinRadius - 3, (coinRadius + 3) * 2, (coinRadius + 3) * 2);

        gc.setFill(Color.GOLD);
        gc.fillOval(coinCenterX - coinRadius, coinCenterY - coinRadius, coinRadius * 2, coinRadius * 2);

        gc.setFill(Color.rgb(120, 80, 0));
        gc.setFont(Font.font("System", FontWeight.BOLD, 40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("$", coinCenterX, coinCenterY + 2);
        gc.restore();

        gc.save();
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("+" + goldAmount, centerX, goldCardY + 220);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Instant currency", centerX, goldCardY + 260);
        gc.fillText("for the shop", centerX, goldCardY + 280);
        gc.restore();
    }

    private void renderWeaponCard(GraphicsContext gc, boolean hovered) {
        drawCardBackground(gc, weaponCardX, weaponCardY, hovered);

        double centerX = weaponCardX + CARD_WIDTH / 2.0;

        gc.save();
        gc.setFill(Color.DEEPSKYBLUE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("WEAPON", centerX, weaponCardY + 40);
        gc.restore();

        String weaponName = weapon != null ? weapon.getName() : "Unknown";

        gc.save();
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(weaponName, centerX, weaponCardY + 105);
        gc.restore();

        int damage = weapon != null ? weapon.getDamage() : 0;

        gc.save();
        gc.setFill(Color.ORANGERED);
        gc.setFont(Font.font("System", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("DMG: " + damage, centerX, weaponCardY + 155);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTCYAN);
        gc.setFont(Font.font("System", 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Power: " + getWeaponPowerLabel(damage), centerX, weaponCardY + 185);
        gc.restore();

        renderWeaponSymbol(gc, centerX, weaponCardY + 235);

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Choose which slot", centerX, weaponCardY + 290);
        gc.fillText("will be replaced", centerX, weaponCardY + 310);
        gc.restore();
    }

    private void renderSlotCard(GraphicsContext gc, double x, double y, int slot, WeaponType currentWeapon, boolean hovered) {
        drawCardBackground(gc, x, y, hovered);

        double centerX = x + CARD_WIDTH / 2.0;
        String currentName = currentWeapon != null ? currentWeapon.getDisplayName() : "EMPTY";
        String rewardName = weapon != null ? weapon.getName() : "Unknown";

        gc.save();
        gc.setFill(slot == 1 ? Color.DEEPSKYBLUE : Color.ORANGE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 24));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("SLOT " + slot, centerX, y + 45);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("CURRENT", centerX, y + 90);
        gc.restore();

        gc.save();
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(currentName, centerX, y + 120);
        gc.restore();

        gc.save();
        gc.setStroke(Color.rgb(100, 100, 130));
        gc.setLineWidth(2.0);
        gc.strokeLine(x + 45, y + 155, x + CARD_WIDTH - 45, y + 155);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("REPLACE WITH", centerX, y + 195);
        gc.restore();

        gc.save();
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(rewardName, centerX, y + 230);
        gc.restore();

        renderWeaponSymbol(gc, centerX, y + 270);

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Click to replace Slot " + slot, centerX, y + 315);
        gc.restore();
    }

    private void renderWeaponSymbol(GraphicsContext gc, double centerX, double centerY) {
        gc.save();
        gc.setStroke(Color.SILVER);
        gc.setLineWidth(4);
        gc.strokeLine(centerX - 22, centerY + 22, centerX + 22, centerY - 22);

        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3);
        gc.strokeLine(centerX - 8, centerY - 8, centerX + 8, centerY + 8);
        gc.restore();
    }

    private String getWeaponPowerLabel(int damage) {
        if (damage >= 30) return "High";
        if (damage >= 18) return "Medium";
        return "Low";
    }

    private void drawCardBackground(GraphicsContext gc, double x, double y, boolean hovered) {
        gc.save();

        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(x + 4, y + 6, CARD_WIDTH, CARD_HEIGHT, 18, 18);

        gc.setFill(CARD_FILL);
        gc.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 18, 18);

        gc.setStroke(hovered ? CARD_HOVER_BORDER : CARD_BORDER);
        gc.setLineWidth(hovered ? 3.5 : 2.0);
        gc.strokeRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 18, 18);

        gc.restore();
    }
}