package com.soulknight.engine;

import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Weapon;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Màn hình chọn phần thưởng sau khi hoàn thành nhiệm vụ của màn.
 * Hiển thị 2 ô lựa chọn: một ô vàng (số lượng ngẫu nhiên) và một ô vũ khí.
 * Người chơi click vào ô để nhận phần thưởng tương ứng.
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

    private final Weapon weapon;
    private final int goldAmount;

    private final double goldCardX;
    private final double goldCardY;
    private final double weaponCardX;
    private final double weaponCardY;

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

    public void render(GraphicsContext graphicsContext, Vector2D mousePosition) {
        graphicsContext.save();
        graphicsContext.setFill(OVERLAY_COLOR);
        graphicsContext.fillRect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        graphicsContext.restore();

        graphicsContext.save();
        graphicsContext.setFill(Color.GOLD);
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 34));
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.fillText("CHOOSE YOUR REWARD",
                VIEWPORT_WIDTH / 2.0, goldCardY - 60);
        graphicsContext.restore();

        graphicsContext.save();
        graphicsContext.setFill(Color.LIGHTGRAY);
        graphicsContext.setFont(Font.font("System", 16));
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.fillText("Click a card to claim the reward",
                VIEWPORT_WIDTH / 2.0, goldCardY - 30);
        graphicsContext.restore();

        boolean hoverGold = isInside(mousePosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT);
        boolean hoverWeapon = isInside(mousePosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT);

        renderGoldCard(graphicsContext, hoverGold);
        renderWeaponCard(graphicsContext, hoverWeapon);
    }

    public String handleClick(Vector2D clickPosition) {
        if (clickPosition == null) {
            return null;
        }
        if (isInside(clickPosition, goldCardX, goldCardY, CARD_WIDTH, CARD_HEIGHT)) {
            return "gold";
        }
        if (isInside(clickPosition, weaponCardX, weaponCardY, CARD_WIDTH, CARD_HEIGHT)) {
            return "weapon";
        }
        return null;
    }

    private boolean isInside(Vector2D point, double rectX, double rectY,
                             double rectWidth, double rectHeight) {
        if (point == null) {
            return false;
        }
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
        double coinRadius = 40;

        gc.save();
        gc.setFill(Color.rgb(180, 130, 0));
        gc.fillOval(coinCenterX - coinRadius - 3, coinCenterY - coinRadius - 3,
                (coinRadius + 3) * 2, (coinRadius + 3) * 2);
        gc.setFill(Color.GOLD);
        gc.fillOval(coinCenterX - coinRadius, coinCenterY - coinRadius,
                coinRadius * 2, coinRadius * 2);
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
        gc.fillText(weaponName, centerX, weaponCardY + 110);
        gc.restore();

        int damage = weapon != null ? weapon.getDamage() : 0;

        gc.save();
        gc.setFill(Color.ORANGERED);
        gc.setFont(Font.font("System", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("DMG: " + damage, centerX, weaponCardY + 160);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTCYAN);
        gc.setFont(Font.font("System", 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Power: " + (damage >= 30 ? "High" : damage >= 18 ? "Medium" : "Low"),
                centerX, weaponCardY + 188);
        gc.restore();

        double iconCenterX = centerX;
        double iconCenterY = weaponCardY + 240;
        gc.save();
        gc.setStroke(Color.SILVER);
        gc.setLineWidth(4);
        gc.strokeLine(iconCenterX - 22, iconCenterY + 22,
                iconCenterX + 22, iconCenterY - 22);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3);
        gc.strokeLine(iconCenterX - 8, iconCenterY - 8,
                iconCenterX + 8, iconCenterY + 8);
        gc.restore();

        gc.save();
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Equips immediately", centerX, weaponCardY + 290);
        gc.fillText("Replaces current weapon", centerX, weaponCardY + 310);
        gc.restore();
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
