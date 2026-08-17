package com.soulknight.debuff;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public enum DebuffType {
    POISON("POISON", Color.PURPLE, 5.0, "/assets/icon/poison.png"),
    SLOW("SLOW", Color.LIGHTBLUE, 7.0, "/assets/icon/slow.png"),
    CONFUSION("CONFUSION", Color.ORANGE, 7.0, "/assets/icon/confusion.png"),
    WEAKNESS("WEAKNESS", Color.DARKRED, 8.0, "/assets/icon/weakness.png"),
    FREEZE("FREEZE", Color.CYAN, 5.0, "/assets/icon/freeze.png");

    private final String displayName;
    private final Color color;
    private final double duration;
    private final Image sprite;

    DebuffType(String displayName, Color color, double duration, String spritePath) {
        this.displayName = displayName;
        this.color = color;
        this.duration = duration;
        Image tempImage = null;
        try {
            tempImage = new Image(getClass().getResourceAsStream(spritePath));
        } catch (Exception e) {
            // Nạp dự phòng nếu chưa có ảnh
        }
        this.sprite = tempImage;
    }

    public String getDisplayName() { return displayName; }
    public Color getColor() { return color; }
    public double getDuration() { return duration; }
    public Image getSprite() { return sprite; }
}