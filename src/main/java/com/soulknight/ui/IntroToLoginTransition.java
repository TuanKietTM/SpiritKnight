package com.soulknight.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Chuyen canh Intro -> Login theo thu tu:
 * intro -> fade den -> phong toi -> man hinh bat -> mua -> login hien ra.
 */
public final class IntroToLoginTransition {

    private final StackPane host;
    private final Parent introRoot;
    private final Parent loginRoot;
    private final LoginController loginController;
    private final Rectangle blackOverlay = new Rectangle();
    private SequentialTransition sequence;

    public IntroToLoginTransition(StackPane host, Parent introRoot, Parent loginRoot, LoginController loginController) {
        this.host = host;
        this.introRoot = introRoot;
        this.loginRoot = loginRoot;
        this.loginController = loginController;

        blackOverlay.setFill(Color.BLACK);
        blackOverlay.setMouseTransparent(true);
        blackOverlay.widthProperty().bind(host.widthProperty());
        blackOverlay.heightProperty().bind(host.heightProperty());
        blackOverlay.setOpacity(0.0);
        blackOverlay.setVisible(false);
    }

    public void play(Runnable onFinished) {
        stop();
        prepareLayers();

        FadeTransition fadeToBlack = new FadeTransition(Duration.millis(700), blackOverlay);
        fadeToBlack.setFromValue(0.0);
        fadeToBlack.setToValue(1.0);

        fadeToBlack.setOnFinished(event -> {
            introRoot.setVisible(false);
            introRoot.setManaged(false);

            loginRoot.setVisible(true);
            loginRoot.setManaged(true);
            loginRoot.toFront();
            blackOverlay.toFront();
        });

        PauseTransition blackPause = new PauseTransition(Duration.millis(260));

        Timeline revealDarkRoom = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(loginController.roomRevealProperty(), 0.0),
                        new KeyValue(blackOverlay.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(650),
                        new KeyValue(loginController.roomRevealProperty(), 1.0),
                        new KeyValue(blackOverlay.opacityProperty(), 0.0))
        );

        PauseTransition roomPause = new PauseTransition(Duration.millis(180));

        Timeline monitorBoot = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(loginController.monitorPowerProperty(), 0.0)),
                new KeyFrame(Duration.millis(70),
                        new KeyValue(loginController.monitorPowerProperty(), 0.18)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(loginController.monitorPowerProperty(), 0.04)),
                new KeyFrame(Duration.millis(210),
                        new KeyValue(loginController.monitorPowerProperty(), 0.68)),
                new KeyFrame(Duration.millis(430),
                        new KeyValue(loginController.monitorPowerProperty(), 1.0))
        );

        Timeline rainAndAmbient = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(loginController.rainRevealProperty(), 0.0),
                        new KeyValue(loginController.ambientRevealProperty(), 0.0)),
                new KeyFrame(Duration.millis(620),
                        new KeyValue(loginController.rainRevealProperty(), 1.0),
                        new KeyValue(loginController.ambientRevealProperty(), 1.0))
        );

        FadeTransition loginFade = new FadeTransition(Duration.millis(520), loginController.getLoginBox());
        loginFade.setFromValue(0.0);
        loginFade.setToValue(1.0);

        ScaleTransition loginScale = new ScaleTransition(Duration.millis(520), loginController.getLoginBox());
        loginScale.setFromX(0.97);
        loginScale.setFromY(0.97);
        loginScale.setToX(1.0);
        loginScale.setToY(1.0);

        ParallelTransition revealLogin = new ParallelTransition(loginFade, loginScale);

        sequence = new SequentialTransition(
                fadeToBlack,
                blackPause,
                revealDarkRoom,
                roomPause,
                monitorBoot,
                rainAndAmbient,
                revealLogin
        );

        sequence.setOnFinished(event -> {
            blackOverlay.setVisible(false);
            loginController.finishCinematicReveal();
            if (onFinished != null) onFinished.run();
        });

        sequence.playFromStart();
    }

    public void stop() {
        if (sequence != null) {
            sequence.stop();
            sequence = null;
        }
        blackOverlay.setVisible(false);
        blackOverlay.setOpacity(0.0);
    }

    private void prepareLayers() {
        if (!host.getChildren().contains(blackOverlay)) {
            host.getChildren().add(blackOverlay);
        }

        loginController.clearForm();
        loginController.prepareCinematicReveal();

        loginRoot.setVisible(true);
        loginRoot.setManaged(true);
        loginRoot.setOpacity(1.0);
        loginRoot.toBack();

        introRoot.setVisible(true);
        introRoot.setManaged(true);
        introRoot.setOpacity(1.0);
        introRoot.toFront();

        blackOverlay.setVisible(true);
        blackOverlay.setOpacity(0.0);
        blackOverlay.toFront();
    }
}
