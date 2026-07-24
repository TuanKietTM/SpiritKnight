package com.soulknight.animation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

public final class ScreenShake {

    private ScreenShake() {
    }

    public static void play(Node node) {
        play(node, 14, 9);
    }

    public static void play(
            Node node,
            double strength,
            int shakeCount
    ) {
        Timeline timeline = new Timeline();

        double frameDuration = 48;

        for (int index = 0;
             index < shakeCount;
             index++) {

            int currentIndex = index;

            timeline.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(
                                    currentIndex
                                            * frameDuration
                            ),
                            event -> {
                                double decrease =
                                        1.0
                                                - (
                                                (double) currentIndex
                                                        / shakeCount
                                        );

                                double direction =
                                        currentIndex % 2 == 0
                                                ? -1
                                                : 1;

                                double x =
                                        direction
                                                * strength
                                                * decrease;

                                double y =
                                        direction
                                                * strength
                                                * 0.25
                                                * decrease;

                                node.setTranslateX(x);
                                node.setTranslateY(y);
                            }
                    )
            );
        }

        timeline.getKeyFrames().add(
                new KeyFrame(
                        Duration.millis(
                                shakeCount
                                        * frameDuration
                        ),
                        event -> {
                            node.setTranslateX(0);
                            node.setTranslateY(0);
                        }
                )
        );

        timeline.play();
    }
}