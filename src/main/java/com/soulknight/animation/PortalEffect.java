package com.soulknight.animation;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class PortalEffect {
    private static final String SPRITESHEET_PATH = "/assets/story/portal1.png";
    private static final double FRAME_WIDTH = 200;
    private static final double FRAME_HEIGHT = 250;
    private static final int TOTAL_FRAMES = 3;

    private PortalEffect() {
    }

    public static void play(Pane portalPane, Runnable onFinished) {
        portalPane.getChildren().clear();

        portalPane.setManaged(true);
        portalPane.setVisible(true);
        portalPane.setOpacity(1.0);
        portalPane.setScaleX(0.05);
        portalPane.setScaleY(0.05);

        Image spriteSheet = new Image(PortalEffect.class.getResourceAsStream(SPRITESHEET_PATH));
        ImageView portalView = new ImageView(spriteSheet);

        portalView.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));

        portalView.setFitWidth(200);
        portalView.setFitHeight(200);

        portalView.setX(500 - (portalView.getFitWidth() / 2));
        portalView.setY(300 - (portalView.getFitHeight() / 2));

        DropShadow glow = new DropShadow(30, Color.rgb(67, 161, 255));
        glow.setInput(new Bloom(0.35));
        portalView.setEffect(glow);

        portalPane.getChildren().add(portalView);

        Timeline animation = new Timeline();
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(150 * i), // Đổi frame sau mỗi 150ms
                    e -> {
                        double offsetX = frameIndex * FRAME_WIDTH;
                        portalView.setViewport(new Rectangle2D(offsetX, 0, FRAME_WIDTH, FRAME_HEIGHT));
                    }
            );
            animation.getKeyFrames().add(keyFrame);
        }
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
        ScaleTransition open = new ScaleTransition(Duration.seconds(1.45), portalPane);
        open.setToX(1.0);
        open.setToY(1.0);

        open.setOnFinished(event -> expand(portalPane, animation, onFinished));
        open.play();
    }

    private static void expand(Pane portalPane, Timeline animation, Runnable onFinished) {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.9), portalPane);
        scale.setToX(7.0);
        scale.setToY(7.0);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.9), portalPane);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ParallelTransition transition = new ParallelTransition(scale, fade);

        transition.setOnFinished(event -> {
            animation.stop();

            portalPane.setVisible(false);
            portalPane.setManaged(false);

            if (onFinished != null) {
                onFinished.run();
            }
        });

        transition.play();
    }
}