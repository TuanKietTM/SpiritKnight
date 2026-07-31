package com.soulknight.ui;

import com.soulknight.utils.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class PortalOverlay extends StackPane {

    private static final String SPRITE_PATH = "/assets/story/portal1.png";
    private static final double FRAME_WIDTH = 200.0;
    private static final double FRAME_HEIGHT = 250.0;
    private static final double PORTAL_SIZE = 200.0;
    private static final int TOTAL_FRAMES = 3;

    private final ImageView portalView = new ImageView();
    private Timeline spriteAnimation;
    private ParallelTransition closingTransition;
    private ScaleTransition openingTransition;
    private Runnable onFinished;

    public PortalOverlay() {
        setAlignment(Pos.CENTER);
        setVisible(false);
        setManaged(false);
        setMouseTransparent(false);
        setPickOnBounds(true);
        setStyle("-fx-background-color: #02010b;");

        loadPortalImage();
        configurePortalView();

        getChildren().add(portalView);
    }

    private void loadPortalImage() {
        var resource = getClass().getResource(SPRITE_PATH);

        if (resource == null) {
            System.err.println("Khong tim thay anh portal: " + SPRITE_PATH);
            return;
        }

        portalView.setImage(new Image(resource.toExternalForm(), false));
    }

    private void configurePortalView() {
        portalView.setFitWidth(PORTAL_SIZE);
        portalView.setFitHeight(PORTAL_SIZE);
        portalView.setPreserveRatio(false);
        portalView.setSmooth(false);
        portalView.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));

        DropShadow glow = new DropShadow(30, Color.rgb(67, 161, 255));
        glow.setInput(new Bloom(0.35));
        portalView.setEffect(glow);
    }

    public void play(Runnable onFinished) {
        stopAnimations();

        this.onFinished = onFinished;

        setOpacity(1.0);
        setScaleX(1.0);
        setScaleY(1.0);
        setVisible(true);
        setManaged(true);
        toFront();

        portalView.setOpacity(1.0);
        portalView.setScaleX(0.05);
        portalView.setScaleY(0.05);

        startSpriteAnimation();
        SoundManager.getInstance().playSFX("Portal");

        openingTransition = new ScaleTransition(Duration.seconds(1.45), portalView);
        openingTransition.setToX(1.0);
        openingTransition.setToY(1.0);
        openingTransition.setOnFinished(event -> closePortal());
        openingTransition.play();
    }

    private void startSpriteAnimation() {
        spriteAnimation = new Timeline();

        for (int i = 0; i < TOTAL_FRAMES; i++) {
            final int frameIndex = i;

            KeyFrame frame = new KeyFrame(Duration.millis(150.0 * i), event -> {
                double offsetX = frameIndex * FRAME_WIDTH;

                portalView.setViewport(
                        new Rectangle2D(offsetX, 0, FRAME_WIDTH, FRAME_HEIGHT)
                );
            });

            spriteAnimation.getKeyFrames().add(frame);
        }

        spriteAnimation.setCycleCount(Timeline.INDEFINITE);
        spriteAnimation.play();
    }

    private void closePortal() {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.9), portalView);
        scale.setToX(7.0);
        scale.setToY(7.0);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.9), this);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        closingTransition = new ParallelTransition(scale, fade);
        closingTransition.setOnFinished(event -> finish());
        closingTransition.play();
    }

    private void finish() {
        stopAnimations();

        SoundManager.getInstance().stopSFX("Portal");

        setOpacity(1.0);
        setVisible(false);
        setManaged(false);

        Runnable callback = onFinished;
        onFinished = null;

        if (callback != null) {
            callback.run();
        }
    }

    public void stop() {
        stopAnimations();

        SoundManager.getInstance().stopSFX("Portal");

        setOpacity(1.0);
        setVisible(false);
        setManaged(false);
        onFinished = null;
    }

    private void stopAnimations() {
        if (spriteAnimation != null) {
            spriteAnimation.stop();
            spriteAnimation = null;
        }

        if (openingTransition != null) {
            openingTransition.stop();
            openingTransition = null;
        }

        if (closingTransition != null) {
            closingTransition.stop();
            closingTransition = null;
        }
    }
}