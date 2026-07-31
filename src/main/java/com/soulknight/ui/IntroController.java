package com.soulknight.ui;

import com.soulknight.utils.Constants;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;
import com.soulknight.utils.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class IntroController {

    @FXML
    private StackPane rootStackPane;
    @FXML
    private Pane skyLayer;
    @FXML
    private Pane cloudLayer;
    @FXML
    private Pane castleLayer;
    @FXML
    private Pane fireLayer;
    @FXML
    private Pane fogLayer;
    @FXML
    private Pane ribbonLayer;

    private static final double WIDTH = Constants.WINDOW_WIDTH;
    private static final double HEIGHT = Constants.WINDOW_HEIGHT;
    private static final double CLOUD_SPEED = 0.5;

    private ImageView cloudImg1;
    private ImageView cloudImg2;

    private Runnable onIntroFinished;

    public void setOnIntroFinished(Runnable callback) {
        this.onIntroFinished = callback;
    }

    private static class FireParticle {
        Rectangle rect;
        double speedY;
        double speedX;
        double opacity = 1.0;
        double fadeSpeed;

        FireParticle(double x, double y) {
            Random rand = new Random();
            double size = 2 + rand.nextInt(3);
            rect = new Rectangle(x, y, size, size);

            Color[] fireColors = {
                    Color.web("#ff1a00"),
                    Color.web("#ff4500"),
                    Color.web("#ff7b00"),
                    Color.web("#ffd700")
            };
            rect.setFill(fireColors[rand.nextInt(fireColors.length)]);

            speedY = -0.6 - rand.nextDouble() * 1.2;
            speedX = -0.4 + rand.nextDouble() * 0.8;
            fadeSpeed = 0.002 + rand.nextDouble() * 0.006;
        }

        void update() {
            rect.setTranslateX(rect.getTranslateX() + speedX);
            rect.setTranslateY(rect.getTranslateY() + speedY);
            opacity -= fadeSpeed;
            rect.setOpacity(opacity);
        }
    }

    @FXML
    public void initialize() {
        SoundManager.getInstance().playBGM("/assets/Audio/trailer.mp3");
//       Layer 1 bau troi
        try {
            Image skyImage = new Image(getClass().getResourceAsStream("/assets/intro/sky.png"));
            ImageView skyView = new ImageView(skyImage);
            skyView.setSmooth(false);
            skyView.setFitWidth(WIDTH);
            skyView.setFitHeight(HEIGHT);
            skyLayer.getChildren().add(skyView);
        } catch (Exception e) {
            System.err.println("Không tìm thấy file sky.png!");
        }

//       Layer 2 may troi
        try {
            Image cloudImage = new Image(getClass().getResourceAsStream("/assets/intro/cloud.png"));
            cloudImg1 = new ImageView(cloudImage);
            cloudImg1.setSmooth(false);
            cloudImg1.setFitWidth(WIDTH);
            cloudImg1.setFitHeight(HEIGHT);
            cloudImg1.setTranslateX(0);

            cloudImg2 = new ImageView(cloudImage);
            cloudImg2.setSmooth(false);
            cloudImg2.setFitWidth(WIDTH);
            cloudImg2.setFitHeight(HEIGHT);
            cloudImg2.setTranslateX(WIDTH);

            cloudLayer.getChildren().addAll(cloudImg1, cloudImg2);

            AnimationTimer cloudScroller = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    cloudImg1.setTranslateX(cloudImg1.getTranslateX() - CLOUD_SPEED);
                    cloudImg2.setTranslateX(cloudImg2.getTranslateX() - CLOUD_SPEED);

                    if (cloudImg1.getTranslateX() <= -WIDTH) {
                        cloudImg1.setTranslateX(cloudImg2.getTranslateX() + WIDTH);
                    }
                    if (cloudImg2.getTranslateX() <= -WIDTH) {
                        cloudImg2.setTranslateX(cloudImg1.getTranslateX() + WIDTH);
                    }
                }
            };
            cloudScroller.start();
        } catch (Exception e) {
            System.err.println("Không tìm thấy file cloud.png!");
        }

//   Layer 3 Lau dai
        try {
            Image spriteSheet = new Image(getClass().getResourceAsStream("/assets/intro/castle.png"));
            ImageView castleView = new ImageView(spriteSheet);
            castleView.setSmooth(false);

            double frameWidth = 1354;
            double frameHeight = 738;

            Rectangle2D frame1Viewport = new Rectangle2D(0, 0, frameWidth, frameHeight);
            Rectangle2D frame2Viewport = new Rectangle2D(frameWidth, 0, frameWidth, frameHeight);

            castleView.setViewport(frame1Viewport);
            castleView.setFitWidth(WIDTH);
            castleView.setFitHeight(HEIGHT);

//           can chinh tu dong
            castleView.setTranslateY(HEIGHT * 0.09);
            castleLayer.getChildren().add(castleView);

            AnimationTimer castleAnimator = new AnimationTimer() {
                private long lastUpdate = 0;
                private boolean isFrame1 = true;
                private final long FRAME_DURATION_NS = 166_666_667;

                @Override
                public void handle(long now) {
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        return;
                    }
                    if (now - lastUpdate >= FRAME_DURATION_NS) {
                        castleView.setViewport(isFrame1 ? frame2Viewport : frame1Viewport);
                        isFrame1 = !isFrame1;
                        lastUpdate = now;
                    }
                }
            };
            castleAnimator.start();
        } catch (Exception e) {
            System.err.println("Khong tim thay anh ");
        }

//        Layer 4 suong mu
        for (int i = 0; i < 3; i++) {
            double fogY = HEIGHT - 70 + (i * 15);
            Ellipse fog = new Ellipse(WIDTH / 2.0, fogY, WIDTH * 0.65, 50);
            fog.setFill(Color.web("#d1dbed", 0.1));
            BoxBlur blur = new BoxBlur(90, 35, 3);
            fog.setEffect(blur);
            fogLayer.getChildren().add(fog);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(12 + i * 4), fog);
            tt.setByX(60 - (i * 35));
            tt.setByY(4 - (i * 3));
            tt.setAutoReverse(true);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.play();
        }

//Layer 5 tan lua
        List<FireParticle> particles = new ArrayList<>();
        Random random = new Random();
        AnimationTimer fireSystem = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (random.nextDouble() < 0.45) {
                    double spawnX = random.nextDouble() * WIDTH;
                    double spawnY = (HEIGHT - 60) + random.nextInt(50);
                    FireParticle p = new FireParticle(spawnX, spawnY);
                    particles.add(p);
                    fireLayer.getChildren().add(p.rect);
                }
                for (int i = particles.size() - 1; i >= 0; i--) {
                    FireParticle p = particles.get(i);
                    p.update();
                    if (p.opacity <= 0 || p.rect.getTranslateY() < -50) {
                        fireLayer.getChildren().remove(p.rect);
                        particles.remove(i);
                    }
                }
            }
        };
        fireSystem.start();

//       layer 6 ruy bang
        try {
            if (ribbonLayer != null) {
                Image ribbonImage = new Image(getClass().getResourceAsStream("/assets/intro/ribbon.png"));
                ImageView ribbonView = new ImageView(ribbonImage);
                ribbonView.setSmooth(false);
//                tu dong scale theo kich thuoc man hinh
                double ribbonWidth = WIDTH * 0.8;
                ribbonView.setFitWidth(ribbonWidth);
                ribbonView.setPreserveRatio(true);
                ribbonView.setTranslateX((WIDTH - ribbonWidth) / 2);
                ribbonView.setTranslateY(HEIGHT * 0.11);

                ribbonLayer.getChildren().add(ribbonView);

                TranslateTransition ribbonAnimation = new TranslateTransition(Duration.seconds(4.0), ribbonView);
                ribbonAnimation.setByY(-10);
                ribbonAnimation.setAutoReverse(true);
                ribbonAnimation.setCycleCount(Animation.INDEFINITE);
                ribbonAnimation.play();
            }
        } catch (Exception e) {
            System.err.println("Không tìm thấy file ribbon.png!");
        }

//        Layer 7 kiem
        try {
            Image swordImg = new Image(getClass().getResourceAsStream("/assets/intro/sword.png"));
            Image startImg = new Image(getClass().getResourceAsStream("/assets/intro/start.png"));

            ImageView swordView = new ImageView(swordImg);
            swordView.setSmooth(false);
            double swordWidth = WIDTH * 0.38;
            swordView.setFitWidth(swordWidth);
            swordView.setPreserveRatio(true);

            ImageView startView = new ImageView(startImg);
            startView.setSmooth(false);
            double startWidth = WIDTH * 0.13;
            startView.setFitWidth(startWidth);
            startView.setPreserveRatio(true);
            double centerY = HEIGHT * 0.53;

            swordView.setTranslateX(-WIDTH * 0.4);
            swordView.setTranslateY(centerY);
            swordView.setRotate(-45);

            startView.setTranslateX((WIDTH - startWidth) / 2);
            startView.setTranslateY(centerY);
            startView.setOpacity(0.0);
            startView.setDisable(true);

            ribbonLayer.getChildren().addAll(swordView, startView);

            TranslateTransition swordIn = new TranslateTransition(Duration.seconds(0.5), swordView);
            swordIn.setToX((WIDTH - swordWidth) / 2);
            swordIn.setToY(centerY);

            javafx.animation.RotateTransition swordRotate = new javafx.animation.RotateTransition(Duration.seconds(0.5), swordView);
            swordRotate.setToAngle(0);

            javafx.animation.ParallelTransition swordEntrance = new javafx.animation.ParallelTransition(swordIn, swordRotate);

            swordEntrance.setOnFinished(event -> {
                TranslateTransition shake = new TranslateTransition(Duration.seconds(0.05), rootStackPane);
                shake.setByY(4);
                shake.setAutoReverse(true);
                shake.setCycleCount(4);
                shake.play();

                javafx.animation.FadeTransition startFadeIn = new javafx.animation.FadeTransition(Duration.seconds(0.4), startView);
                startFadeIn.setToValue(1.0);
                startFadeIn.setOnFinished(e -> {
                    startView.setDisable(false);
                    javafx.animation.FadeTransition blink = new javafx.animation.FadeTransition(Duration.seconds(0.7), startView);
                    blink.setFromValue(1.0);
                    blink.setToValue(0.4);
                    blink.setAutoReverse(true);
                    blink.setCycleCount(Animation.INDEFINITE);
                    blink.play();
                    startView.setUserData(blink);
                });
                startFadeIn.play();
            });

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1.0));
            delay.setOnFinished(e -> {
                SoundManager.getInstance().playSFX("Sword");
                swordEntrance.play();
            });
            delay.play();

            // SỰ KIỆN CLICK VÀO CHỮ START
            startView.setOnMouseClicked(event -> {
                startView.setDisable(true);
                if (startView.getUserData() instanceof javafx.animation.FadeTransition) {
                    ((javafx.animation.FadeTransition) startView.getUserData()).stop();
                }

                javafx.animation.FadeTransition startFadeOut = new javafx.animation.FadeTransition(Duration.seconds(0.15), startView);
                startFadeOut.setToValue(0.0);
                startFadeOut.play();

                TranslateTransition swordOut = new TranslateTransition(Duration.seconds(0.35), swordView);
                swordOut.setToX(WIDTH * 1.1);
                swordOut.setToY(HEIGHT * 0.5);

                javafx.animation.RotateTransition swordRotateOut = new javafx.animation.RotateTransition(Duration.seconds(0.35), swordView);
                swordRotateOut.setToAngle(30);

                javafx.animation.ParallelTransition swordExit = new javafx.animation.ParallelTransition(swordOut, swordRotateOut);

                swordExit.setOnFinished(e -> {
                    SoundManager.getInstance().stopBGM();
                    SoundManager.getInstance().stopSFX("Sword");
                    SoundManager.getInstance().stopSFX("button");

                    if (onIntroFinished != null) {
                        onIntroFinished.run();
                    }
                });
                SoundManager.getInstance().playSFX("Sword");
                swordExit.play();
            });

        } catch (Exception e) {
            System.err.println("Lỗi tải tài nguyên: " + e.getMessage());
        }
    }
}