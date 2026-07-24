package com.soulknight.cinematic;

@FunctionalInterface
public interface CinematicScene {

    /**
     * Phát một cảnh cinematic.
     *
     * @param onFinished gọi khi cảnh đã hoàn tất
     */
    void play(Runnable onFinished);
}
