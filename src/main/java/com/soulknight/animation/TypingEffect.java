package com.soulknight.animation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Hieu ung go chu cho phan intro story , ending story - cinematic
 */
public final class TypingEffect {

    private Timeline timeline;

    public void play(
            Label label,
            String content,
            Duration characterDelay,
            Runnable onFinished
    ) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(content);
        Objects.requireNonNull(characterDelay);

        stop();

        label.setText("");

        if (content.isEmpty()) {
            if (onFinished != null) {
                onFinished.run();
            }

            return;
        }

        timeline = new Timeline();

        for (int index = 0;
             index < content.length();
             index++) {

            int characterIndex = index;

            KeyFrame frame = new KeyFrame(
                    characterDelay.multiply(index + 1),
                    event -> label.setText(
                            label.getText()
                                    + content.charAt(
                                    characterIndex
                            )
                    )
            );

            timeline.getKeyFrames().add(frame);
        }

        timeline.setOnFinished(event -> {
            timeline = null;

            if (onFinished != null) {
                onFinished.run();
            }
        });

        timeline.play();
    }

    public void stop() {
        if (timeline == null) {
            return;
        }

        timeline.stop();
        timeline = null;
    }
}