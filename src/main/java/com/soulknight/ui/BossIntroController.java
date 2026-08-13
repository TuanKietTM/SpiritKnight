package com.soulknight.ui;

import javafx.animation.FadeTransition;

import javafx.fxml.FXML;

import javafx.scene.image.ImageView;

import javafx.scene.layout.StackPane;

import javafx.util.Duration;

public class BossIntroController {

    @FXML

    private StackPane rootPane;

    @FXML

    private ImageView bossImageView;

    /**

     * Gọi khi xuất hiện màn Boss Intro

     */

    public void playIntroAnimation() {

        if (rootPane == null) return;

        rootPane.setVisible(true);

        rootPane.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), rootPane);

        fadeIn.setFromValue(0.0);

        fadeIn.setToValue(1.0);

        fadeIn.play();

    }

    /**

     * Gọi khi kết thúc Intro để ẩn màn hình

     */

    public void hideIntroAnimation(Runnable onFinished) {

        if (rootPane == null) return;

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rootPane);

        fadeOut.setFromValue(1.0);

        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {

            rootPane.setVisible(false);

            if (onFinished != null) {

                onFinished.run();

            }

        });

        fadeOut.play();

    }

}