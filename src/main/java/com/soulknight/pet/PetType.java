package com.soulknight.pet;

/**
 * Danh sách các loại pet hiện có trong game hỗ trợ Sprite Sheet Animation.
 */
public enum PetType {

    NONE(
            "Không sử dụng",
            null, null,
            0, 0,
            0, 0,
            32, 32,
            0, 0.12
    ),

    SLIME(
            "Slime / Bồ câu",
            "/assets/pet/bird_IDLE.png",
            "/assets/pet/bird_RUN.png",
            4, 5, // Ví dụ: IDLE có 4 frame, RUN có 6 frame (chỉnh theo thực tế của bạn)
            24, 24, // Render size
            32, 32, // Frame size gốc trong sprite sheet
            185,
            0.12 // Tốc độ đổi frame (giây/frame)
    ),

    CAT(
            "Mèo",
            "/assets/pet/cat_IDLE.png",
            "/assets/pet/cat_RUN.png",
            4, 4,
            24, 24,
            32, 32,
            195,
            0.10
    ),

    WOLF(
            "Sói",
            "/assets/pet/wolf_IDLE.png",
            "/assets/pet/wolf_RUN.png",
            4, 4,
            28, 28,
            32, 32,
            210,
            0.10
    ),

    GHOST(
            "Hồn ma",
            "/assets/pet/ghost_IDLE.png",
            "/assets/pet/ghost_RUN.png",
            4, 4,
            26, 26,
            32, 32,
            175,
            0.15
    );

    private final String displayName;
    private final String idleImagePath;
    private final String runImagePath;
    private final int idleFrameCount;
    private final int runFrameCount;
    private final double renderWidth;
    private final double renderHeight;
    private final double frameWidth;
    private final double frameHeight;
    private final double moveSpeed;
    private final double frameDuration;

    PetType(
            String displayName,
            String idleImagePath,
            String runImagePath,
            int idleFrameCount,
            int runFrameCount,
            double renderWidth,
            double renderHeight,
            double frameWidth,
            double frameHeight,
            double moveSpeed,
            double frameDuration
    ) {
        this.displayName = displayName;
        this.idleImagePath = idleImagePath;
        this.runImagePath = runImagePath;
        this.idleFrameCount = idleFrameCount;
        this.runFrameCount = runFrameCount;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.moveSpeed = moveSpeed;
        this.frameDuration = frameDuration;
    }

    public String getDisplayName() { return displayName; }
    public String getIdleImagePath() { return idleImagePath; }
    public String getRunImagePath() { return runImagePath; }
    public int getIdleFrameCount() { return idleFrameCount; }
    public int getRunFrameCount() { return runFrameCount; }
    public double getRenderWidth() { return renderWidth; }
    public double getRenderHeight() { return renderHeight; }
    public double getFrameWidth() { return frameWidth; }
    public double getFrameHeight() { return frameHeight; }
    public double getMoveSpeed() { return moveSpeed; }
    public double getFrameDuration() { return frameDuration; }

    public boolean hasPet() {
        return this != NONE;
    }
}