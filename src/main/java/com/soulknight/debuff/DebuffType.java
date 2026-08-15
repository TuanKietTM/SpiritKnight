package com.soulknight.debuff;

import javafx.scene.paint.Color;

public enum DebuffType {
    POISON("POISON", Color.PURPLE, 5.0),
    SLOW("SLOW", Color.LIGHTBLUE, 7.0),
    CONFUSION("CONFUSION", Color.ORANGE, 7.0),
    WEAKNESS("WEAKNESS", Color.DARKRED, 8.0);

    private final String displayName;
    private final Color color;
    private final double duration;

    DebuffType(String displayName, Color color, double duration) {
        this.displayName = displayName;
        this.color = color;
        this.duration = duration;
    }

    public String getDisplayName() { return displayName; }
    public Color getColor() { return color; }
    public double getDuration() { return duration; }
}