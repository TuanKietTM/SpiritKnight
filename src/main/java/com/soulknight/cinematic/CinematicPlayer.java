package com.soulknight.cinematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CinematicPlayer {

    private final List<CinematicScene> scenes =
            new ArrayList<>();

    private int currentSceneIndex;
    private int generation;

    private boolean playing;
    private Runnable onFinished;

    public CinematicPlayer addScene(
            CinematicScene scene
    ) {
        scenes.add(
                Objects.requireNonNull(scene)
        );

        return this;
    }

    public void setOnFinished(
            Runnable onFinished
    ) {
        this.onFinished = onFinished;
    }

    public void play() {
        if (playing) {
            return;
        }

        playing = true;
        currentSceneIndex = 0;
        generation++;

        playCurrentScene(generation);
    }

    public void skip() {
        if (!playing) {
            return;
        }

        playing = false;
        generation++;

        runFinishedAction();
    }

    public boolean isPlaying() {
        return playing;
    }

    private void playCurrentScene(
            int currentGeneration
    ) {
        if (!playing) {
            return;
        }

        if (currentSceneIndex >= scenes.size()) {
            playing = false;
            runFinishedAction();
            return;
        }

        CinematicScene scene =
                scenes.get(currentSceneIndex);

        scene.play(() -> {
            if (!playing) {
                return;
            }

            if (generation != currentGeneration) {
                return;
            }

            currentSceneIndex++;

            playCurrentScene(
                    currentGeneration
            );
        });
    }

    private void runFinishedAction() {
        if (onFinished != null) {
            onFinished.run();
        }
    }
}