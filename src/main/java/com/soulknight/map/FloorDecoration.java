package com.soulknight.map;

import javafx.scene.image.Image;

public final class FloorDecoration {

    private final double worldX;
    private final double worldY;
    private final double width;
    private final double height;
    private final Image image;

    public FloorDecoration(
            double worldX,
            double worldY,
            double width,
            double height,
            Image image
    ) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Image getImage() {
        return image;
    }
}