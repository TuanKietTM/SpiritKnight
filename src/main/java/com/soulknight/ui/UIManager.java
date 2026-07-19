package com.soulknight.ui;

import com.soulknight.engine.GameState;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class UIManager {

    private final StackPane rootNode;
    private IntroController introController;
    private HUD hudController;
    private Menu menuController;
    private LevelClearScreen levelClearController;
    private VictoryScreen victoryController;
    private GameOverScreen gameOverController;
    private PauseScreen pauseController;
    private SettingScreen settingController;

    private Parent introRoot;
    private Parent hudRoot;
    private Parent menuRoot;
    private Parent levelClearRoot;
    private Parent victoryRoot;
    private Parent gameOverRoot;
    private Parent pauseRoot;
    private Parent settingRoot;

    public UIManager(StackPane rootNode) {
        this.rootNode = rootNode;
        initViews();
    }

    private void initViews() {
        try {
            FXMLLoader introLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Intro.fxml"));
            introRoot = introLoader.load();
            introController = introLoader.getController();
            configFullRegion(introRoot);

            FXMLLoader menuLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Menu.fxml"));
            menuRoot = menuLoader.load();
            menuController = menuLoader.getController();
            configFullRegion(menuRoot);

            FXMLLoader hudLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/HUD.fxml"));
            hudRoot = hudLoader.load();
            hudController = hudLoader.getController();
            configFullRegion(hudRoot);

            FXMLLoader clearLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/LevelClearScreen.fxml"));
            levelClearRoot = clearLoader.load();
            levelClearController = clearLoader.getController();
            configFullRegion(levelClearRoot);

            FXMLLoader victoryLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/VictoryScreen.fxml"));
            victoryRoot = victoryLoader.load();
            victoryController = victoryLoader.getController();
            configFullRegion(victoryRoot);

            FXMLLoader gameOverLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/GameOverScreen.fxml"));
            gameOverRoot = gameOverLoader.load();
            gameOverController = gameOverLoader.getController();
            configFullRegion(gameOverRoot);

            FXMLLoader pauseLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/PauseScreen.fxml"));
            pauseRoot = pauseLoader.load();
            pauseController = pauseLoader.getController();
            configFullRegion(pauseRoot);
            pauseRoot.setPickOnBounds(false);

            FXMLLoader settingLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/SettingScreen.fxml"));
            settingRoot = settingLoader.load();
            settingController = settingLoader.getController();
            configFullRegion(settingRoot);
            settingRoot.setPickOnBounds(false);


            menuRoot.setPickOnBounds(false);
            hudRoot.setPickOnBounds(false);
            levelClearRoot.setPickOnBounds(false);
            victoryRoot.setPickOnBounds(false);
            gameOverRoot.setPickOnBounds(false);

            rootNode.getChildren().addAll(introRoot, menuRoot, hudRoot, levelClearRoot, victoryRoot, gameOverRoot, pauseRoot, settingRoot);

            StackPane.setAlignment(introRoot, Pos.CENTER);
            StackPane.setAlignment(menuRoot, Pos.CENTER);
            StackPane.setAlignment(hudRoot, Pos.BOTTOM_CENTER);
            StackPane.setAlignment(levelClearRoot, Pos.CENTER);
            StackPane.setAlignment(victoryRoot, Pos.CENTER);
            StackPane.setAlignment(gameOverRoot, Pos.CENTER);
            StackPane.setAlignment(pauseRoot, Pos.CENTER);
            StackPane.setAlignment(settingRoot, Pos.CENTER);

            hideAllScreens();
            introRoot.setVisible(true);
            introRoot.toFront();

        } catch (Exception e) {
            System.err.println("Loi FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleStateChange(GameState state) {
        if (introRoot == null || menuRoot == null || hudRoot == null || levelClearRoot == null ||
                victoryRoot == null || gameOverRoot == null || pauseRoot == null || settingRoot == null)
            return;

        Platform.runLater(() -> {
            switch (state) {
                case INTRO -> {
                    hideAllScreens();
                    introRoot.setOpacity(1.0);
                    introRoot.setVisible(true);
                    introRoot.toFront();
                }
                case MAIN_MENU -> {
                    hideAllScreens();
                    menuRoot.setOpacity(1.0);
                    menuRoot.setVisible(true);
                    menuRoot.toFront();
                }
                case PLAYING -> {
                    hideAllScreens();
                    hudRoot.setVisible(true);
                    hudRoot.toFront();

                    Platform.runLater(() -> {
                        for (javafx.scene.Node node : rootNode.getChildren()) {
                            if (node instanceof javafx.scene.canvas.Canvas) {
                                node.setFocusTraversable(true);
                                node.requestFocus();
                                break;
                            }
                        }
                    });
                }
                case LEVEL_CLEAR -> {
                    hideAllScreens();
                    hudRoot.setVisible(true);
                    levelClearRoot.setVisible(true);
                    levelClearRoot.toFront();
                }
                case GAME_VICTORY -> {
                    hideAllScreens();
                    victoryRoot.setVisible(true);
                    victoryRoot.toFront();
                }
                case GAME_OVER -> {
                    hideAllScreens();
                    gameOverRoot.setVisible(true);
                    gameOverRoot.toFront();
                }
                case PAUSED -> {
                    hideAllScreens();
                    hudRoot.setVisible(true);
                    pauseRoot.setVisible(true);
                    pauseRoot.toFront();
                }
            }
        });
    }

    public void updateHUD(GameWorld world) {
        if (hudController != null && world.isPlaying()) {
            hudController.updateData(
                    world.getPlayer(),
                    world.getLevelManager(),
                    world.getMissionManager(),
                    world.getEnemies().size(),
                    world.getItems().size()
            );
        }

        if (world.getState() == GameState.LEVEL_CLEAR && levelClearController != null) {
            levelClearController.updateData(world.getLevelManager(), world.getMissionManager());
        }
    }

    private void configFullRegion(Parent root) {
        if (root instanceof Region region) {
            region.setMinWidth(Constants.WINDOW_WIDTH);
            region.setMinHeight(Constants.WINDOW_HEIGHT);
            region.setPrefWidth(Constants.WINDOW_WIDTH);
            region.setPrefHeight(Constants.WINDOW_HEIGHT);
        }
    }

    public void bindGameWorld(GameWorld world) {
        world.setGameStateListener(this::handleStateChange);

        if (introController != null) {
            introController.setOnIntroFinished(() -> {
                Platform.runLater(() -> {
                    FadeTransition fadeIntro = new FadeTransition(Duration.seconds(0.5), introRoot);
                    fadeIntro.setFromValue(1.0);
                    fadeIntro.setToValue(0.0);
                    fadeIntro.setOnFinished(event -> {
                        introRoot.setVisible(false);

//                        chuyen sang main menu
                        world.changeState(GameState.MAIN_MENU);
                        menuRoot.setOpacity(0.0);
                        menuRoot.setVisible(true);
                        FadeTransition fadeInMenu = new FadeTransition(Duration.seconds(0.6), menuRoot);
                        fadeInMenu.setFromValue(0.0);
                        fadeInMenu.setToValue(1.0);
                        fadeInMenu.play();
                    });
                    fadeIntro.play();
                });
            });
        }
        bindGameActions(world);
    }

    public void bindGameActions(GameWorld world) {
        // Lấy cấu hình âm lượng mặc định hoặc hiện tại từ SoundManager làm gốc
        final com.soulknight.utils.SoundManager sound = com.soulknight.utils.SoundManager.getInstance();

        if (menuController != null) {
            menuController.setOnPlayRequested(() -> {
                sound.playSFX("button"); // Tiếng ấn nút
                world.changeState(GameState.PLAYING);
            });

            menuController.setOnSettingsRequested(() -> {
                sound.playSFX("button");
                menuRoot.setVisible(false);
                settingRoot.setVisible(true);
                settingRoot.toFront();

                if (settingController != null) {
                    settingController.setup(
                            () -> { // Nút Close quay lại màn hình chính
                                sound.playSFX("button");
                                settingRoot.setVisible(false);
                                menuRoot.setVisible(true);
                                menuRoot.toFront();
                            },
                            (bgmValue) -> {
                                // Đồng bộ âm lượng nhạc nền khi kéo Slider
                                sound.setBGMVolume(bgmValue);
                            },
                            (sfxValue) -> {
                                // Đồng bộ âm lượng hiệu ứng khi kéo Slider
                                sound.setSFXVolume(sfxValue);
                            },
                            // Truyền giá trị thực tế đang có trong SoundManager lên thanh Slider giao diện
                            sound.getBgmVolume(), sound.getSfxVolume()
                    );
                }
            });

            menuController.setOnShopRequested(() -> {
                sound.playSFX("button");
                System.out.println("Open Shop");
            });
        }

        if (hudController != null) {
            hudController.setOnPauseRequested(() -> {
                if (world.getState() == GameState.PLAYING) {
                    sound.playSFX("button");
                    world.changeState(GameState.PAUSED);
                }
            });
        }

        if (pauseController != null) {
            pauseController.setCallbacks(
                    () -> {
                        sound.playSFX("button");
                        world.changeState(GameState.PLAYING); // tiếp tục chơi
                    },
                    () -> {
                        sound.playSFX("button");
                        pauseRoot.setVisible(false);
                        settingRoot.setVisible(true);
                        settingRoot.toFront();

                        if (settingController != null) {
                            settingController.setup(
                                    () -> { // Nút Close quay lại Pause
                                        sound.playSFX("button");
                                        settingRoot.setVisible(false);
                                        pauseRoot.setVisible(true);
                                        pauseRoot.toFront();
                                    },
                                    (bgmValue) -> sound.setBGMVolume(bgmValue),
                                    (sfxValue) -> sound.setSFXVolume(sfxValue),
                                    sound.getBgmVolume(), sound.getSfxVolume()
                            );
                        }
                    },
                    () -> { // Quay lại Menu chính
                        sound.playSFX("button");
                        sound.stopBGM(); // Dừng nhạc nền màn chơi khi thoát ra ngoài
                        world.changeState(GameState.MAIN_MENU);
                    }
            );
        }
    }

    private void hideAllScreens() {
        if (introRoot != null) introRoot.setVisible(false);
        if (menuRoot != null) menuRoot.setVisible(false);
        if (hudRoot != null) hudRoot.setVisible(false);
        if (levelClearRoot != null) levelClearRoot.setVisible(false);
        if (victoryRoot != null) victoryRoot.setVisible(false);
        if (gameOverRoot != null) gameOverRoot.setVisible(false);
        if (pauseRoot != null) pauseRoot.setVisible(false);
        if (settingRoot != null) settingRoot.setVisible(false);
    }
}