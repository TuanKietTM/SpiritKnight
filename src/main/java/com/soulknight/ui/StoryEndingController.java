package com.soulknight.ui;

import com.soulknight.cinematic.CinematicPlayer;
import com.soulknight.cinematic.VisualNovelScene;
import com.soulknight.model.StoryConfigLoader;
import com.soulknight.model.StoryFrame;
import com.soulknight.utils.SoundManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class StoryEndingController {

    @FXML private StackPane rootPane;
    @FXML private StackPane visualNovelScene;
    @FXML private ImageView storyImage;
    @FXML private javafx.scene.layout.VBox dialoguePanel;
    @FXML private Label storyTitle;
    @FXML private Label storyText;
    @FXML private Label storyProgress;
    @FXML private Rectangle storyBlackFade;
    @FXML private Button continueButton;
    @FXML private VBox creditsContainer;
    @FXML private Rectangle endingFade;
    @FXML private Label endingChronicleLabel;
    @FXML private StackPane chronicleOverlay;

    private final CinematicPlayer cinematicPlayer = new CinematicPlayer();
    private final List<Animation> activeAnimations = new ArrayList<>();

    private VisualNovelScene visualNovelCinematic;
    private StoryConfigLoader storyConfig;
    private Runnable onEndingFinished;

    private boolean running;
    private boolean finished;

    public void setOnEndingFinished(Runnable callback) {
        this.onEndingFinished = callback;
    }

    public void startEnding() {
        if (running) return;

        running = true;
        finished = false;

        SoundManager.getInstance().stopAll();

        storyConfig = StoryConfigLoader.loadFromJson("/assets/story/story_ending.json");

        initializeVisualNovelScene();
        configureCinematic();

        cinematicPlayer.play();
    }

    private void initializeVisualNovelScene() {
        if (visualNovelScene == null || storyImage == null || dialoguePanel == null ||
                storyTitle == null || storyText == null || storyProgress == null ||
                storyBlackFade == null || continueButton == null) {

            throw new IllegalStateException("StoryEnding FXML chua khai bao day du.");
        }

        List<StoryFrame> frames = new ArrayList<>();

        if (storyConfig != null && storyConfig.frames != null) {
            for (StoryConfigLoader.StoryFrameData frameData : storyConfig.frames) {
                frames.add(frameData.toStoryFrame());
            }
        }

        visualNovelCinematic = new VisualNovelScene(
                visualNovelScene,
                storyImage,
                dialoguePanel,
                storyTitle,
                storyText,
                storyProgress,
                storyBlackFade,
                continueButton,
                frames
        );
    }
// them cac khun gcang vao ending
    private void configureCinematic() {
        cinematicPlayer
                .addScene(this::playOpeningFade)
                .addScene(this::playVisualNovel)
                .addScene(this::playChronicleTitle)
                .addScene(this::playCreditsRoll) // cuon dong gop
                .addScene(this::playEndingFade);
        cinematicPlayer.setOnFinished(this::finishEnding);
    }

    // Fade tu Boss Room vao Story.
    private void playOpeningFade(Runnable onFinished) {
        if (endingFade == null) {
            onFinished.run();
            return;
        }

        showNode(endingFade);
        endingFade.setOpacity(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(650), endingFade);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        fade.setOnFinished(event -> {
            activeAnimations.remove(fade);

            FadeTransition reveal = new FadeTransition(Duration.millis(650), endingFade);
            reveal.setFromValue(1.0);
            reveal.setToValue(0.0);

            reveal.setOnFinished(done -> {
                activeAnimations.remove(reveal);
                hideNode(endingFade);
                onFinished.run();
            });

            trackAndPlay(reveal);
        });

        trackAndPlay(fade);
    }

    private void playVisualNovel(Runnable onFinished) {
        if (visualNovelCinematic == null) {
            onFinished.run();
            return;
        }

        visualNovelCinematic.play(onFinished);
    }

    // Fade den truoc khi quay lai Main Menu.
    private void playEndingFade(Runnable onFinished) {
        if (endingFade == null) {
            onFinished.run();
            return;
        }

        showNode(endingFade);
        endingFade.setOpacity(0.0);

        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), endingFade);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        fade.setOnFinished(event -> {
            activeAnimations.remove(fade);
            onFinished.run();
        });

        trackAndPlay(fade);
    }

    @FXML
    private void continueStory() {
        if (finished || visualNovelCinematic == null) return;

        visualNovelCinematic.continueToNextFrame();
    }

    @FXML
    private void skipEnding() {
        if (finished) return;

        if (visualNovelCinematic != null) {
            visualNovelCinematic.stop();
        }

        finishEnding();
    }

    private void finishEnding() {
        if (finished) return;

        finished = true;
        running = false;

        stopAnimations();
        SoundManager.getInstance().stopAll();

        if (onEndingFinished != null) {
            onEndingFinished.run();
        }
    }

    private void trackAndPlay(Animation animation) {
        if (animation == null || finished) return;

        activeAnimations.add(animation);
        animation.play();
    }

    private void stopAnimations() {
        for (Animation animation : activeAnimations) {
            if (animation != null) animation.stop();
        }

        activeAnimations.clear();
    }

    private void showNode(Node node) {
        if (node == null) return;

        node.setManaged(true);
        node.setVisible(true);
    }

    private void hideNode(Node node) {
        if (node == null) return;

        node.setVisible(false);
        node.setManaged(false);
    }
    // Hien title ket chuong sau frame cuoi.
    private void playChronicleTitle(Runnable onFinished) {
        if (chronicleOverlay == null) {
            onFinished.run();
            return;
        }
        SoundManager.getInstance().playSFX("appear");

        // Bật cả StackPane phủ màn hình chứa Chronicle Title
        showNode(chronicleOverlay);

        chronicleOverlay.setOpacity(0.0);
        chronicleOverlay.setScaleX(0.92);
        chronicleOverlay.setScaleY(0.92);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), chronicleOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.8), chronicleOverlay);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition show = new ParallelTransition(fade, scale);

        show.setOnFinished(event -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(2.2));

            pause.setOnFinished(done -> {
                FadeTransition hide = new FadeTransition(Duration.seconds(0.8), chronicleOverlay);
                hide.setFromValue(1.0);
                hide.setToValue(0.0);

                hide.setOnFinished(end -> {
                    hideNode(chronicleOverlay);
                    onFinished.run();
                });

                trackAndPlay(hide);
            });

            trackAndPlay(pause);
        });

        trackAndPlay(show);
    }
//    hiue ung cuon dong gop
    private void playCreditsRoll(Runnable onFinished) {
        if (creditsContainer == null) {
            onFinished.run();
            return;
        }

        SoundManager.getInstance().playSFX("credit");

        if (endingFade != null) {
            showNode(endingFade);
            endingFade.setOpacity(1.0);
        }

        showNode(creditsContainer);
        creditsContainer.setOpacity(1.0);
        creditsContainer.toFront();
        creditsContainer.setTranslateY(600);

//     cuon len
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(12.0), creditsContainer);
        moveUp.setFromY(600);
        moveUp.setToY(-1000);

        moveUp.setOnFinished(event -> {
            activeAnimations.remove(moveUp);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.0), creditsContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(done -> {
                activeAnimations.remove(fadeOut);
                hideNode(creditsContainer);
                onFinished.run();
            });

            trackAndPlay(fadeOut);
        });

        trackAndPlay(moveUp);
    }
}