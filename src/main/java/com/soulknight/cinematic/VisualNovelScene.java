package com.soulknight.cinematic;

import com.soulknight.animation.TypingEffect;
import com.soulknight.model.StoryFrame;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import com.soulknight.utils.SoundManager;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Objects;

public final class VisualNovelScene {

    private final StackPane sceneRoot;
    private final ImageView storyImage;
    private final VBox dialoguePanel;
    private final Label storyTitle;
    private final Label storyText;
    private final Label storyProgress;
    private final Rectangle blackFade;
    private final Button continueButton;
    private final List<StoryFrame> frames;

    private final TypingEffect typingEffect = new TypingEffect();

    private Animation activeTransition;
    private ScaleTransition activeZoom;
    private Runnable pendingContinueAction;

    private int currentFrameIndex;
    private int generation;
    private boolean playing;

    public VisualNovelScene(StackPane sceneRoot, ImageView storyImage, VBox dialoguePanel, Label storyTitle, Label storyText, Label storyProgress, Rectangle blackFade, Button continueButton, List<StoryFrame> frames) {
        this.sceneRoot = Objects.requireNonNull(sceneRoot, "sceneRoot không được null");
        this.storyImage = Objects.requireNonNull(storyImage, "storyImage không được null");
        this.dialoguePanel = Objects.requireNonNull(dialoguePanel, "dialoguePanel không được null");
        this.storyTitle = Objects.requireNonNull(storyTitle, "storyTitle không được null");
        this.storyText = Objects.requireNonNull(storyText, "storyText không được null");
        this.storyProgress = Objects.requireNonNull(storyProgress, "storyProgress không được null");
        this.blackFade = Objects.requireNonNull(blackFade, "blackFade không được null");
        this.continueButton = Objects.requireNonNull(continueButton, "continueButton không được null");
        this.frames = List.copyOf(Objects.requireNonNull(frames, "frames không được null"));
    }

    public void play(Runnable onFinished) {
        stop();

        playing = true;
        currentFrameIndex = 0;
        generation++;

        int currentGeneration = generation;

        prepareScene();
        playCurrentFrame(currentGeneration, onFinished);
    }

    public void stop() {
        playing = false;
        generation++;
        typingEffect.stop();
        SoundManager.getInstance().stopSFX("Typing");
        SoundManager.getInstance().stopBGM();

        stopAnimation(activeTransition);
        stopAnimation(activeZoom);

        activeTransition = null;
        activeZoom = null;
        pendingContinueAction = null;

        hideContinueButton();
    }

    public void continueToNextFrame() {
        if (!playing || pendingContinueAction == null) {
            return;
        }

        Runnable action = pendingContinueAction;
        pendingContinueAction = null;

        hideContinueButton();
        action.run();
    }

    private void prepareScene() {
        sceneRoot.setManaged(true);
        sceneRoot.setVisible(true);
        sceneRoot.setOpacity(1);

        storyImage.setImage(null);
        storyImage.setOpacity(0);
        storyImage.setScaleX(1);
        storyImage.setScaleY(1);
        storyImage.setTranslateX(0);
        storyImage.setTranslateY(0);

        dialoguePanel.setManaged(true);
        dialoguePanel.setVisible(true);
        dialoguePanel.setOpacity(1);

        storyTitle.setText("");
        storyText.setText("");
        storyProgress.setText("");

        blackFade.setVisible(true);
        blackFade.setOpacity(1);

        pendingContinueAction = null;
        hideContinueButton();
    }

    private void playCurrentFrame(int currentGeneration, Runnable onFinished) {
        if (!isValid(currentGeneration)) {
            return;
        }

        if (currentFrameIndex >= frames.size()) {
            fadeOutWholeScene(currentGeneration, onFinished);
            return;
        }

        StoryFrame frame = frames.get(currentFrameIndex);
        if (frame.bgmPath() != null && !frame.bgmPath().isBlank()) {
            double fadeTime = (frame.bgmFadeTime() > 0) ? frame.bgmFadeTime() : 0.8;
            SoundManager.getInstance().playBGM(frame.bgmPath(), fadeTime);
        }
        if (!loadImage(frame.imagePath())) {
            currentFrameIndex++;
            playCurrentFrame(currentGeneration, onFinished);
            return;
        }

        resetCurrentFrame();

        storyTitle.setText(frame.title());
        storyText.setText("");
        storyProgress.setText(String.format("%02d / %02d", currentFrameIndex + 1, frames.size()));

        fadeInCurrentFrame(currentGeneration, frame, onFinished);
    }

    private boolean loadImage(String resourcePath) {
        URL imageUrl = VisualNovelScene.class.getResource(resourcePath);

        if (imageUrl == null) {
            System.err.println("Khong thay  " + resourcePath);
            return false;
        }

        Image image = new Image(imageUrl.toExternalForm(), false);

        if (image.isError()) {
            System.err.println("Khong doc duoc : " + resourcePath);
            return false;
        }

        storyImage.setImage(image);
        return true;
    }

    private void resetCurrentFrame() {
        stopAnimation(activeZoom);
        activeZoom = null;

        typingEffect.stop();
        pendingContinueAction = null;
        hideContinueButton();

        storyImage.setOpacity(0);
        storyImage.setScaleX(1);
        storyImage.setScaleY(1);
        storyImage.setTranslateX(0);
        storyImage.setTranslateY(0);

        dialoguePanel.setManaged(true);
        dialoguePanel.setVisible(true);
        dialoguePanel.setOpacity(1);

        blackFade.setVisible(true);
        blackFade.setOpacity(1);
    }

    private void fadeInCurrentFrame(int currentGeneration, StoryFrame frame, Runnable onFinished) {
        dialoguePanel.setManaged(true);
        dialoguePanel.setVisible(true);
        dialoguePanel.setOpacity(1);

        FadeTransition imageFade = new FadeTransition(Duration.seconds(0.8), storyImage);
        imageFade.setFromValue(0);
        imageFade.setToValue(1);

        FadeTransition blackFadeOut = new FadeTransition(Duration.seconds(0.8), blackFade);
        blackFadeOut.setFromValue(1);
        blackFadeOut.setToValue(0);

        ParallelTransition transition = new ParallelTransition(imageFade, blackFadeOut);
        activeTransition = transition;

        transition.setOnFinished(event -> {
            if (activeTransition == transition) {
                activeTransition = null;
            }

            if (!isValid(currentGeneration)) {
                return;
            }

            blackFade.setVisible(false);
            startImageZoom(currentGeneration);
            typeCurrentFrame(currentGeneration, frame, onFinished);
        });

        transition.play();
    }

    private void startImageZoom(int currentGeneration) {
        if (!isValid(currentGeneration)) {
            return;
        }

        activeZoom = new ScaleTransition(Duration.seconds(12), storyImage);
        activeZoom.setFromX(1);
        activeZoom.setFromY(1);
        activeZoom.setToX(1.05);
        activeZoom.setToY(1.05);
        activeZoom.play();
    }

    private void typeCurrentFrame(int currentGeneration, StoryFrame frame, Runnable onFinished) {
        if (!isValid(currentGeneration)) {
            return;
        }
        SoundManager.getInstance().playLoopSFX("Typing");

        typingEffect.play(storyText, frame.text(), Duration.millis(28), () -> {
            SoundManager.getInstance().stopSFX("Typing");
            if (!isValid(currentGeneration)) {
                return;
            }

            showContinueButton();

            pendingContinueAction = () -> fadeOutCurrentFrame(currentGeneration, onFinished);
        });
    }

    private void fadeOutCurrentFrame(int currentGeneration, Runnable onFinished) {
        if (!isValid(currentGeneration)) {
            return;
        }

        typingEffect.stop();
        SoundManager.getInstance().stopSFX("Typing");
        pendingContinueAction = null;
        hideContinueButton();

        stopAnimation(activeZoom);
        activeZoom = null;

        blackFade.setVisible(true);
        blackFade.setOpacity(0);

        FadeTransition imageFade = new FadeTransition(Duration.seconds(0.6), storyImage);
        imageFade.setFromValue(storyImage.getOpacity());
        imageFade.setToValue(0);

        FadeTransition dialogueFade = new FadeTransition(Duration.seconds(0.35), dialoguePanel);
        dialogueFade.setFromValue(dialoguePanel.getOpacity());
        dialogueFade.setToValue(0);

        FadeTransition blackFadeIn = new FadeTransition(Duration.seconds(0.6), blackFade);
        blackFadeIn.setFromValue(0);
        blackFadeIn.setToValue(1);

        ParallelTransition transition = new ParallelTransition(imageFade, dialogueFade, blackFadeIn);
        activeTransition = transition;

        transition.setOnFinished(event -> {
            if (activeTransition == transition) {
                activeTransition = null;
            }

            if (!isValid(currentGeneration)) {
                return;
            }

            currentFrameIndex++;
            playCurrentFrame(currentGeneration, onFinished);
        });

        transition.play();
    }

    private void fadeOutWholeScene(int currentGeneration, Runnable onFinished) {
        if (!isValid(currentGeneration)) {
            return;
        }
        typingEffect.stop();
        SoundManager.getInstance().stopSFX("Typing");
        SoundManager.getInstance().stopBGM();

        pendingContinueAction = null;
        hideContinueButton();

        stopAnimation(activeZoom);
        activeZoom = null;

        blackFade.setVisible(true);
        blackFade.setOpacity(0);

        FadeTransition blackFadeIn = new FadeTransition(Duration.seconds(0.8), blackFade);
        blackFadeIn.setFromValue(0);
        blackFadeIn.setToValue(1);

        FadeTransition dialogueFade = new FadeTransition(Duration.seconds(0.4), dialoguePanel);
        dialogueFade.setFromValue(dialoguePanel.getOpacity());
        dialogueFade.setToValue(0);

        ParallelTransition transition = new ParallelTransition(blackFadeIn, dialogueFade);
        activeTransition = transition;

        transition.setOnFinished(event -> {
            if (activeTransition == transition) {
                activeTransition = null;
            }

            if (!isValid(currentGeneration)) {
                return;
            }

            playing = false;

            sceneRoot.setVisible(false);
            sceneRoot.setManaged(false);

            storyImage.setImage(null);
            storyTitle.setText("");
            storyText.setText("");
            storyProgress.setText("");

            if (onFinished != null) {
                onFinished.run();
            }
        });

        transition.play();
    }

    private void showContinueButton() {
        continueButton.setManaged(true);
        continueButton.setVisible(true);
        continueButton.setDisable(false);
        continueButton.setOpacity(1);
    }

    private void hideContinueButton() {
        continueButton.setDisable(true);
        continueButton.setVisible(false);
        continueButton.setManaged(false);
    }

    private boolean isValid(int currentGeneration) {
        return playing && generation == currentGeneration;
    }

    private void stopAnimation(Animation animation) {
        if (animation == null || animation.getStatus() == Animation.Status.STOPPED) {
            return;
        }

        animation.stop();
    }
}