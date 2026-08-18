package com.soulknight.ui;

import com.soulknight.animation.PortalEffect;
import com.soulknight.animation.ScreenShake;
import com.soulknight.animation.TypingEffect;
import com.soulknight.cinematic.CinematicPlayer;
import com.soulknight.cinematic.VisualNovelScene;
import com.soulknight.model.StoryConfigLoader;
import com.soulknight.model.StoryFrame;
import com.soulknight.utils.SoundManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class StoryIntroController {
    @FXML private StackPane rootPane;
    @FXML private VBox bootPanel;
    @FXML private Label bootText;
    @FXML private VBox idePanel;
    @FXML private Label codeLabel;
    @FXML private Label cursorLabel;
    @FXML private Label consoleLabel;
    @FXML private AnchorPane signalOverlay;
    @FXML private VBox signalPanel;
    @FXML private Label signalText;
    @FXML private AnchorPane bugOverlay;
    @FXML private VBox bugPanel;
    @FXML private Label bugTitle;
    @FXML private StackPane visualNovelScene;
    @FXML private ImageView storyImage;
    @FXML private VBox dialoguePanel;
    @FXML private Label storyTitle;
    @FXML private Label storyText;
    @FXML private Label storyProgress;
    @FXML private Rectangle storyBlackFade;
    @FXML private Button continueButton;
    @FXML private Pane portalPane;
    @FXML private Pane whiteFlash;

    private final TypingEffect typingEffect = new TypingEffect();
    private final CinematicPlayer cinematicPlayer = new CinematicPlayer();
    private final List<Animation> activeAnimations = new ArrayList<>();

    private VisualNovelScene visualNovelCinematic;
    private Timeline cursorTimeline;
    private Runnable onIntroFinished;

    private boolean initialized;
    private boolean finished;

    private StoryConfigLoader storyConfig;

    public void setOnIntroFinished(Runnable onIntroFinished) {
        this.onIntroFinished = onIntroFinished;
    }

    public void startStory() {
        if (initialized) {
            return;
        }
        stopAllSounds();

        storyConfig = StoryConfigLoader.loadFromJson("/assets/story/story_intro.json");
        initialized = true;
        finished = false;
        initializeVisualNovelScene();
        configureCinematic();
        startCursor();
        cinematicPlayer.play();
    }

    private void stopAllSounds() {
        SoundManager.getInstance().stopAll();
    }

    private void initializeVisualNovelScene() {
        validateVisualNovelFXML();
        List<StoryFrame> frames = new ArrayList<>();
        if (storyConfig != null && storyConfig.frames != null) {
            for (StoryConfigLoader.StoryFrameData frameData : storyConfig.frames) {
                frames.add(frameData.toStoryFrame());
            }
        }
        visualNovelCinematic = new VisualNovelScene(visualNovelScene, storyImage, dialoguePanel, storyTitle, storyText, storyProgress, storyBlackFade, continueButton, frames);
    }

    private void validateVisualNovelFXML() {
        if (visualNovelScene == null || storyImage == null || dialoguePanel == null || storyTitle == null || storyText == null || storyProgress == null || storyBlackFade == null || continueButton == null) {
            throw new IllegalStateException("FXML Visual Novel chua khai bao day du.");
        }
    }
//them cac khung canh vao intro
    private void configureCinematic() {
        cinematicPlayer
                .addScene(this::playBootScene)
                .addScene(this::playCodingScene)
                .addScene(this::playConsoleScene)
                .addScene(this::playSignalScene)
                .addScene(this::playCorruptionScene)
                .addScene(this::playBugScene)
                .addScene(this::playVisualNovelScene)
                .addScene(this::playPortalScene);
        cinematicPlayer.setOnFinished(this::finishIntro);
    }
//boot may tinh
    private void playBootScene(Runnable onFinished) {
        if (finished) return;
        showNode(bootPanel);
        bootPanel.setOpacity(1);
        bootText.setText("");
        String text = (storyConfig != null) ? storyConfig.bootScript : "";

        SoundManager.getInstance().playLoopSFX("BIOS");

        typingEffect.play(bootText, text, Duration.millis(17), () -> {
            SoundManager.getInstance().stopSFX("BIOS");
            pause(0.7, () -> fadeOutAndHide(bootPanel, 0.6, onFinished));
        });
    }
//coding
    private void playCodingScene(Runnable onFinished) {
        if (finished) return;
        showNode(idePanel);
        idePanel.setOpacity(0);
        idePanel.setScaleX(0.96);
        idePanel.setScaleY(0.96);
        codeLabel.setText("");
        consoleLabel.setText("");
        codeLabel.setStyle("");

        FadeTransition fade = new FadeTransition(Duration.seconds(0.7), idePanel);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.7), idePanel);
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1);
        scale.setToY(1);

        fade.setOnFinished(event -> {
            if (!finished) {
                String text = (storyConfig != null) ? storyConfig.javaCode : "";
                SoundManager.getInstance().playLoopSFX("Typing");

                typingEffect.play(codeLabel, text, Duration.millis(11), () -> {
                    SoundManager.getInstance().stopSFX("Typing");
                    pause(0.45, onFinished);
                });
            }
        });

        trackAndPlay(fade);
        trackAndPlay(scale);
    }

    private void playConsoleScene(Runnable onFinished) {
        if (finished) return;
        consoleLabel.setText("");
        String text = (storyConfig != null) ? storyConfig.consoleScript : "";

        SoundManager.getInstance().playLoopSFX("BIOS");

        typingEffect.play(consoleLabel, text, Duration.millis(18), () -> {
            SoundManager.getInstance().stopSFX("BIOS");
            pause(0.9, onFinished);
        });
    }

    private void playSignalScene(Runnable onFinished) {
        if (finished) return;
        showNode(signalOverlay);
        showNode(signalPanel);
        signalOverlay.setOpacity(0);
        signalPanel.setOpacity(0);
        signalPanel.setScaleX(0.88);
        signalPanel.setScaleY(0.88);
        signalText.setText("");

        FadeTransition overlayFade = new FadeTransition(Duration.millis(450), signalOverlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition panelFade = new FadeTransition(Duration.millis(450), signalPanel);
        panelFade.setFromValue(0);
        panelFade.setToValue(1);

        ScaleTransition panelScale = new ScaleTransition(Duration.millis(500), signalPanel);
        panelScale.setFromX(0.88);
        panelScale.setFromY(0.88);
        panelScale.setToX(1);
        panelScale.setToY(1);

        panelFade.setOnFinished(event -> {
            if (finished) return;
            String text = (storyConfig != null) ? storyConfig.signalScript : "";

            SoundManager.getInstance().playLoopSFX("Radio");

            typingEffect.play(signalText, text, Duration.millis(27), () -> {
                SoundManager.getInstance().stopSFX("Radio");
                pause(1, () -> {
                    fadeOutAndHide(signalPanel, 0.4, null);
                    fadeOutAndHide(signalOverlay, 0.45, onFinished);
                });
            });
        });

        trackAndPlay(overlayFade);
        trackAndPlay(panelFade);
        trackAndPlay(panelScale);
    }

    private void playCorruptionScene(Runnable onFinished) {
        if (finished) return;
        ScreenShake.play(rootPane, 7, 6);
        codeLabel.setStyle("-fx-text-fill: #ff596e; -fx-font-size: 15px; -fx-line-spacing: 4px;");

        String text = (storyConfig != null) ? storyConfig.corruptedCode : "";

        SoundManager.getInstance().playLoopSFX("Typing");

        typingEffect.play(codeLabel, text, Duration.millis(19), () -> {
            SoundManager.getInstance().stopSFX("Typing");
            SoundManager.getInstance().stopSFX("Radio");
            pause(0.8, onFinished);
        });
    }

    private void playBugScene(Runnable onFinished) {
        if (finished) return;
        showNode(bugOverlay);
        showNode(bugPanel);

        bugOverlay.setOpacity(0);
        bugPanel.setOpacity(0);
        bugPanel.setScaleX(0.58);
        bugPanel.setScaleY(0.58);

        FadeTransition redFlash = new FadeTransition(Duration.millis(105), bugOverlay);
        redFlash.setFromValue(0);
        redFlash.setToValue(0.92);
        redFlash.setCycleCount(9);
        redFlash.setAutoReverse(true);

        FadeTransition showPanel = new FadeTransition(Duration.millis(300), bugPanel);
        showPanel.setFromValue(0);
        showPanel.setToValue(1);

        ScaleTransition scalePanel = new ScaleTransition(Duration.millis(430), bugPanel);
        scalePanel.setFromX(0.58);
        scalePanel.setFromY(0.58);
        scalePanel.setToX(1);
        scalePanel.setToY(1);

        FadeTransition darkenIde = new FadeTransition(Duration.millis(350), idePanel);
        darkenIde.setToValue(0.12);

        Timeline bugPulse = new Timeline(
                new KeyFrame(Duration.ZERO, event -> bugTitle.setOpacity(1)),
                new KeyFrame(Duration.millis(110), event -> bugTitle.setOpacity(0.28)),
                new KeyFrame(Duration.millis(220), event -> bugTitle.setOpacity(1))
        );
        bugPulse.setCycleCount(6);
        SoundManager.getInstance().playSFX("BUG");

        ScreenShake.play(rootPane, 18, 12);

        trackAndPlay(redFlash);
        trackAndPlay(showPanel);
        trackAndPlay(scalePanel);
        trackAndPlay(darkenIde);
        trackAndPlay(bugPulse);

        pause(2.8, () -> {
            SoundManager.getInstance().stopSFX("BUG");
            fadeOutAndHide(bugPanel, 0.55, null);
            fadeOutAndHide(bugOverlay, 0.65, null);
            fadeOutAndHide(idePanel, 0.65, onFinished);
        });
    }

//    xem class playVisualNovel no tao man hinh cinematic anh va hieu ung typing
    private void playVisualNovelScene(Runnable onFinished) {
        if (finished) return;
        stopCursor();
        stopAllSounds();

        if (visualNovelCinematic == null) {
            runCallback(onFinished);
            return;
        }
        visualNovelCinematic.play(onFinished);
    }

    private void playPortalScene(Runnable onFinished) {
        if (finished) return;
        if (portalPane == null) {
            runCallback(onFinished);
            return;
        }
        PortalEffect.play(portalPane, () -> playWhiteFlash(onFinished));
    }

    private void playWhiteFlash(Runnable onFinished) {
        if (finished) return;
        if (whiteFlash == null) {
            runCallback(onFinished);
            return;
        }
        showNode(whiteFlash);
        whiteFlash.setOpacity(0);

        FadeTransition flashIn = new FadeTransition(Duration.millis(180), whiteFlash);
        flashIn.setFromValue(0);
        flashIn.setToValue(1);

        FadeTransition flashOut = new FadeTransition(Duration.millis(420), whiteFlash);
        flashOut.setFromValue(1);
        flashOut.setToValue(0);

        flashIn.setOnFinished(event -> {
            if (!finished) flashOut.play();
        });

        flashOut.setOnFinished(event -> {
            hideNode(whiteFlash);
            runCallback(onFinished);
        });

        trackAndPlay(flashIn);
    }


    @FXML
    private void skipIntro() {
        if (finished) return;

//      dung ngay lap tuc hieu ung hinh anh va am thanh
        typingEffect.stop();
        stopAllSounds();
        stopCursor();
        stopAllActiveAnimations();

        //  Dừng scene Visual Novel nếu đang chạy
        if (visualNovelCinematic != null) {
            visualNovelCinematic.stop();
        }

        //  Hoàn tất Intro và chuyển Scene chính ngay lập tức
        finishIntro();
    }

    @FXML
    private void continueStory() {
        if (finished || visualNovelCinematic == null) return;
        visualNovelCinematic.continueToNextFrame();
    }

    private void startCursor() {
        if (cursorLabel == null) return;
        stopCursor();
        cursorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> cursorLabel.setVisible(true)),
                new KeyFrame(Duration.millis(420), event -> cursorLabel.setVisible(false))
        );
        cursorTimeline.setAutoReverse(true);
        cursorTimeline.setCycleCount(Animation.INDEFINITE);
        cursorTimeline.play();
    }

    private void stopCursor() {
        if (cursorTimeline != null) {
            cursorTimeline.stop();
            cursorTimeline = null;
        }
        if (cursorLabel != null) cursorLabel.setVisible(false);
    }

    private void pause(double seconds, Runnable onFinished) {
        if (finished) return;
        PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
        pause.setOnFinished(event -> {
            activeAnimations.remove(pause);
            if (!finished) runCallback(onFinished);
        });
        trackAndPlay(pause);
    }

    private void fadeOutAndHide(Node node, double seconds, Runnable onFinished) {
        if (node == null) {
            runCallback(onFinished);
            return;
        }
        if (finished) return;

        FadeTransition fade = new FadeTransition(Duration.seconds(seconds), node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);

        fade.setOnFinished(event -> {
            activeAnimations.remove(fade);
            hideNode(node);
            if (!finished) runCallback(onFinished);
        });
        trackAndPlay(fade);
    }

    private void trackAndPlay(Animation animation) {
        if (finished || animation == null) return;
        activeAnimations.add(animation);
        animation.play();
    }

    private void stopAllActiveAnimations() {
        for (Animation anim : activeAnimations) {
            if (anim != null) {
                anim.stop();
            }
        }
        activeAnimations.clear();
    }

    private void showNode(Node node) {
        if (node != null) {
            node.setManaged(true);
            node.setVisible(true);
        }
    }

    private void hideNode(Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    private void runCallback(Runnable callback) {
        if (callback != null && !finished) callback.run();
    }

    private void finishIntro() {
        if (finished) return;
        finished = true;

        typingEffect.stop();
        stopAllSounds();
        stopCursor();
        stopAllActiveAnimations();

        // Ẩn tất cả các panel UI giới thiệu
        hideNode(bootPanel);
        hideNode(idePanel);
        hideNode(signalOverlay);
        hideNode(bugOverlay);
        hideNode(visualNovelScene);

        rootPane.setTranslateX(0);
        rootPane.setTranslateY(0);
        if (onIntroFinished != null) {
            onIntroFinished.run();
        }
    }
}