package com.soulknight.pet;

/**
 * Danh sách các loại pet hiện có trong game.
 *
 * Sau này có thể bổ sung:
 * - Giá tiền
 * - Sát thương
 * - Tốc độ đánh
 * - Kỹ năng
 * - Độ hiếm
 */
public enum PetType {

    NONE(
            "Không sử dụng",
            null,
            0,
            0,
            0
    ),

    SLIME(
            "Slime",
            "/assets/pet/bocau.png",
            24,
            24,
            185
    ),

    CAT(
            "Mèo",
            "/assets/pet/cat.png",
            42,
            42,
            195
    ),

    WOLF(
            "Sói",
            "/assets/pet/wolf.png",
            48,
            42,
            210
    ),

    GHOST(
            "Hồn ma",
            "/assets/Pet/ghost.png",
            42,
            48,
            175
    );

    private final String displayName;
    private final String imagePath;
    private final double renderWidth;
    private final double renderHeight;
    private final double moveSpeed;

    PetType(
            String displayName,
            String imagePath,
            double renderWidth,
            double renderHeight,
            double moveSpeed
    ) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.moveSpeed = moveSpeed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getRenderWidth() {
        return renderWidth;
    }

    public double getRenderHeight() {
        return renderHeight;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public boolean hasPet() {
        return this != NONE;
    }
}