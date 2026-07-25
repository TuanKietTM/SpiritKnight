package com.soulknight.pet;

import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Vector2D;

/**
 * Dữ liệu Pet được cung cấp cho hệ thống Room.
 *
 * Room không cần biết trực tiếp class Pet.
 * Nó chỉ cần vị trí và bán kính để tránh sinh vật cản đè lên Pet.
 */
public final class PetRoomInfo {

    private static final PetRoomInfo EMPTY = new PetRoomInfo(null, 0.0);

    private final Vector2D position;
    private final double radius;

    private PetRoomInfo(Vector2D position, double radius) {
        this.position = position;
        this.radius = Math.max(0.0, radius);
    }

    public static PetRoomInfo from(GameWorld gameWorld) {
        if (gameWorld == null || gameWorld.getCurrentPet() == null || gameWorld.getCurrentPet().getPosition() == null) {
            return EMPTY;
        }

        return new PetRoomInfo(gameWorld.getCurrentPet().getPosition().copy(), gameWorld.getCurrentPet().getRadius());
    }

    public boolean exists() {
        return position != null;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }
}