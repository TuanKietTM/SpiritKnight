package com.soulknight.utils;

public final class Vector2D {

    private double x;
    private double y;

    public Vector2D() {
        this(0.0, 0.0);
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D copy() {
        return new Vector2D(x, y);
    }

    public Vector2D add(double deltaX, double deltaY) {
        x += deltaX;
        y += deltaY;
        return this;
    }

    public Vector2D add(Vector2D other) {
        return add(other.x, other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return add(-other.x, -other.y);
    }

    public Vector2D scale(double factor) {
        x *= factor;
        y *= factor;
        return this;
    }

    public Vector2D normalize() {
        double length = length();
        if (length > 0.0) {
            x /= length;
            y /= length;
        }
        return this;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double distance(Vector2D other) {
        double deltaX = x - other.x;
        double deltaY = y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    public double distanceSquared(Vector2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    public void set(Vector2D other) {
        x = other.x;
        y = other.y;
    }


    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }
}
