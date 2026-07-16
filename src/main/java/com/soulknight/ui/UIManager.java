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
            // Đồng bộ sử dụng com.soulknight.Main.class để tránh lỗi đường dẫn null
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

            // Tối ưu hóa click xuyên thấu
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

            // Giao diện khởi tạo ban đầu tuân thủ nghiêm ngặt trạng thái mặc định của GameWorld (INTRO)
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
                    // Hiển thị Menu chính rõ ràng
                    menuRoot.setOpacity(1.0);
                    menuRoot.setVisible(true);
                    menuRoot.toFront();
                }
                case PLAYING -> {
                    hideAllScreens();
                    hudRoot.setVisible(true);
                    hudRoot.toFront();

                    // Focus vào Canvas để xử lý sự kiện phím di chuyển không bị kẹt ở các thành phần UI khác
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
                    hudRoot.setVisible(true); // Vẫn giữ HUD hiển thị làm nền mờ phía dưới
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
                    hudRoot.setVisible(true); // Giữ HUD ở dưới màn hình tạm dừng
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

    // Gán liên kết từ Main GameWorld sang UIManager
    public void bindGameWorld(GameWorld world) {
        // ĐĂNG KÝ: Cho phép GameWorld tự động bắn sự kiện thay đổi trạng thái sang UIManager
        world.setGameStateListener(this::handleStateChange);

        if (introController != null) {
            introController.setOnIntroFinished(() -> {
                Platform.runLater(() -> {
                    // 1. Tạo hiệu ứng Fade Out Intro mượt mà trước
                    FadeTransition fadeIntro = new FadeTransition(Duration.seconds(0.5), introRoot);
                    fadeIntro.setFromValue(1.0);
                    fadeIntro.setToValue(0.0);
                    fadeIntro.setOnFinished(event -> {
                        introRoot.setVisible(false);

                        // 2. Yêu cầu GameWorld chuyển sang MAIN_MENU (báo cáo đồng bộ)
                        world.changeState(GameState.MAIN_MENU);

                        // 3. Hiệu ứng Fade In cho Main Menu nhẹ nhàng xuất hiện cùng lúc
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

        // Đăng ký các sự kiện tương tác giữa các nút bấm UI và thế giới GameWorld
        bindGameActions(world);
    }

    public void bindGameActions(GameWorld world) {
        double currentBgm = 0.5; // Ví dụ minh họa, nên lấy từ SoundManager.getInstance().getBgmVolume()
        double currentSfx = 0.7; // Ví dụ minh họa, nên lấy từ SoundManager.getInstance().getSfxVolume()

        if (menuController != null) {
            // Nhấn chơi game -> chuyển trạng thái GameWorld thành PLAYING
            menuController.setOnPlayRequested(() -> world.changeState(GameState.PLAYING));

            // Mở cài đặt từ màn hình Menu chính
            menuController.setOnSettingsRequested(() -> {
                menuRoot.setVisible(false);
                settingRoot.setVisible(true);
                settingRoot.toFront();

                if (settingController != null) {
                    settingController.setup(
                            () -> { // Nút Close quay về Menu chính
                                settingRoot.setVisible(false);
                                menuRoot.setVisible(true);
                                menuRoot.toFront();
                            },
                            (bgm) -> System.out.println("Menu BGM: " + bgm),
                            (sfx) -> System.out.println("Menu SFX: " + sfx),
                            currentBgm, currentSfx
                    );
                }
            });

            menuController.setOnShopRequested(() -> {
                System.out.println("🛒 Đang chuyển đến màn hình Shop...");
            });
        }

        if (hudController != null) {
            hudController.setOnPauseRequested(() -> {
                if (world.getState() == GameState.PLAYING) {
                    world.changeState(GameState.PAUSED);
                }
            });
        }

        if (pauseController != null) {
            pauseController.setCallbacks(
                    () -> world.changeState(GameState.PLAYING), // Tiếp tục chơi
                    () -> { // Mở Cài đặt từ màn hình Pause
                        pauseRoot.setVisible(false);
                        settingRoot.setVisible(true);
                        settingRoot.toFront();

                        if (settingController != null) {
                            settingController.setup(
                                    () -> { // Nút Close quay lại màn hình Pause
                                        settingRoot.setVisible(false);
                                        pauseRoot.setVisible(true);
                                        pauseRoot.toFront();
                                    },
                                    (bgm) -> {},
                                    (sfx) -> {},
                                    currentBgm, currentSfx
                            );
                        }
                    },
                    () -> { // Quay lại màn hình Menu chính
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