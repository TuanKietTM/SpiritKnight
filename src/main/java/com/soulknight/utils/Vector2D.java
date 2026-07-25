package com.soulknight.utils;

import java.util.Objects;

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
        Objects.requireNonNull(other, "other không được null");

        return add(
                other.x,
                other.y
        );
    }

    public Vector2D subtract(Vector2D other) {
        Objects.requireNonNull(other, "other không được null");

        return add(
                -other.x,
                -other.y
        );
    }

    public Vector2D scale(double factor) {
        x *= factor;
        y *= factor;
        return this;
    }

    public Vector2D normalize() {
        double vectorLength = length();

        if (vectorLength > 0.0) {
            x /= vectorLength;
            y /= vectorLength;
        }

        return this;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double distance(Vector2D other) {
        return Math.sqrt(distanceSquared(other));
    }

    public double distanceSquared(Vector2D other) {
        Objects.requireNonNull(other, "other không được null");

        double deltaX = x - other.x;
        double deltaY = y - other.y;

        return deltaX * deltaX + deltaY * deltaY;
    }

    /**
     * Gán tọa độ từ một Vector2D khác.
     */
    public Vector2D set(Vector2D other) {
        Objects.requireNonNull(other, "other không được null");

        this.x = other.x;
        this.y = other.y;

        return this;
    }

    /**
     * Gán trực tiếp tọa độ X và Y.
     *
     * Phương thức này xử lý lỗi trong Pet.java:
     * position.set(playerX - 45.0, playerY + 20.0);
     */
    public Vector2D set(double x, double y) {
        this.x = x;
        this.y = y;

        return this;
    }

    public Vector2D zero() {
        this.x = 0.0;
        this.y = 0.0;

        return this;
    }

    public boolean isZero() {
        return x == 0.0 && y == 0.0;
    }

    public boolean isZero(double epsilon) {
        return Math.abs(x) <= epsilon &&
                Math.abs(y) <= epsilon;
    }

    public double dot(Vector2D other) {
        Objects.requireNonNull(other, "other không được null");

        return x * other.x + y * other.y;
    }

    public Vector2D limit(double maxLength) {
        if (maxLength < 0.0) {
            throw new IllegalArgumentException(
                    "maxLength không được nhỏ hơn 0."
            );
        }

        double currentLengthSquared = lengthSquared();
        double maxLengthSquared = maxLength * maxLength;

        if (currentLengthSquared > maxLengthSquared &&
                currentLengthSquared > 0.0) {

            double currentLength =
                    Math.sqrt(currentLengthSquared);

            double scaleFactor =
                    maxLength / currentLength;

            x *= scaleFactor;
            y *= scaleFactor;
        }

        return this;
    }

    public Vector2D lerp(
            Vector2D target,
            double amount
    ) {
        Objects.requireNonNull(target, "target không được null");

        double safeAmount =
                Math.max(0.0, Math.min(1.0, amount));

        x += (target.x - x) * safeAmount;
        y += (target.y - y) * safeAmount;

        return this;
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

    @Override
    public String toString() {
        return "Vector2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}