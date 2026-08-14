package com.soulknight.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class BossIntroController {

    @FXML
    private StackPane rootPane;

    @FXML
    private Region bannerBackground;

    @FXML
    private VBox textContent;

    @FXML
    private ImageView bossImageView;

    @FXML
    public void initialize() {
        try {
            Font.loadFont(getClass().getResourceAsStream("/assets/fonts/PressStart2P-Regular.ttf"), 10);
        } catch (Exception ignored) {
            // Trường hợp không tìm thấy đường dẫn font, hệ thống sẽ dùng font fallback trong CSS
        }
    }
    public void playIntroSequence(double totalDurationSeconds, Runnable onFinished) {
        if (rootPane == null) return;

        rootPane.setVisible(true);
        rootPane.setOpacity(1.0);
        if (bannerBackground != null) {
            bannerBackground.setScaleY(0.0);
        }

        if (textContent != null) {
            textContent.setTranslateX(-400);
            textContent.setOpacity(0.0);
        }

        if (bossImageView != null) {
            bossImageView.setTranslateX(400);
            bossImageView.setScaleX(1.4);
            bossImageView.setScaleY(1.4);
            bossImageView.setOpacity(0.0);
        }

        ScaleTransition openBanner = new ScaleTransition(Duration.seconds(0.15), bannerBackground);
        openBanner.setFromY(0.0);
        openBanner.setToY(1.0);
        openBanner.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideText = new TranslateTransition(Duration.seconds(0.25), textContent);
        slideText.setFromX(-400);
        slideText.setToX(0);
        slideText.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeInText = new FadeTransition(Duration.seconds(0.15), textContent);
        fadeInText.setFromValue(0.0);
        fadeInText.setToValue(1.0);

        TranslateTransition slideBoss = new TranslateTransition(Duration.seconds(0.25), bossImageView);
        slideBoss.setFromX(400);
        slideBoss.setToX(0);
        slideBoss.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleBoss = new ScaleTransition(Duration.seconds(0.25), bossImageView);
        scaleBoss.setFromX(1.4);
        scaleBoss.setFromY(1.4);
        scaleBoss.setToX(1.0);
        scaleBoss.setToY(1.0);
        scaleBoss.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeInBoss = new FadeTransition(Duration.seconds(0.15), bossImageView);
        fadeInBoss.setFromValue(0.0);
        fadeInBoss.setToValue(1.0);

        ParallelTransition introImpact = new ParallelTransition(
                openBanner, slideText, fadeInText, slideBoss, scaleBoss, fadeInBoss
        );

        double holdTime = Math.max(0.6, totalDurationSeconds - 0.6);
        PauseTransition hold = new PauseTransition(Duration.seconds(holdTime));

        TranslateTransition textOut = new TranslateTransition(Duration.seconds(0.2), textContent);
        textOut.setToX(-500);
        textOut.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition bossOut = new TranslateTransition(Duration.seconds(0.2), bossImageView);
        bossOut.setToX(500);
        bossOut.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition closeBanner = new ScaleTransition(Duration.seconds(0.2), bannerBackground);
        closeBanner.setToY(0.0);
        closeBanner.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOutRoot = new FadeTransition(Duration.seconds(0.25), rootPane);
        fadeOutRoot.setFromValue(1.0);
        fadeOutRoot.setToValue(0.0);

        ParallelTransition outroImpact = new ParallelTransition(textOut, bossOut, closeBanner, fadeOutRoot);
        SequentialTransition fullSequence = new SequentialTransition(introImpact, hold, outroImpact);

        fullSequence.setOnFinished(e -> {
            rootPane.setVisible(false);
            if (onFinished != null) {
                onFinished.run();
            }
        });

        fullSequence.play();
    }
}