package com.soulknight.ui;

import com.soulknight.engine.GameState;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.utils.Constants;
import com.soulknight.utils.SoundManager;
import com.soulknight.weapon.Weapon;
import com.soulknight.weapon.WeaponType;
import com.soulknight.database.UserDAO;
import com.soulknight.database.UserSession;
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
    private StoryIntroController storyIntroController; // <-- Thêm Controller Story
    private HUD hudController;
    private Menu menuController;
    private LevelClearScreen levelClearController;
    private VictoryScreen victoryController;
    private GameOverScreen gameOverController;
    private PauseScreen pauseController;
    private SettingScreen settingController;
    private ShopController shopController;
    private LoginController loginController;
    private RegisterController registerController;

    private Parent introRoot;
    private Parent storyIntroRoot;
    private Parent hudRoot;
    private Parent menuRoot;
    private Parent levelClearRoot;
    private Parent victoryRoot;
    private Parent gameOverRoot;
    private Parent pauseRoot;
    private Parent settingRoot;
    private Parent shopRoot;
    private Parent loginRoot;
    private Parent registerRoot;

    private final UserDAO userDAO = new UserDAO();



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

            // Tải FXML của StoryIntro
            FXMLLoader storyIntroLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/StoryIntro.fxml"));
            storyIntroRoot = storyIntroLoader.load();
            storyIntroController = storyIntroLoader.getController();
            configFullRegion(storyIntroRoot);
//            Tai FXML login , register
            FXMLLoader loginLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Login.fxml"));
            loginRoot = loginLoader.load();
            loginController = loginLoader.getController();
            configFullRegion(loginRoot);

            FXMLLoader registerLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Register.fxml"));
            registerRoot = registerLoader.load();
            registerController = registerLoader.getController();
            configFullRegion(registerRoot);

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

            FXMLLoader shopLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Shop.fxml"));
            shopRoot = shopLoader.load();
            shopController = shopLoader.getController();
            configFullRegion(shopRoot);
            shopRoot.setPickOnBounds(false);

            menuRoot.setPickOnBounds(false);
            hudRoot.setPickOnBounds(false);
            levelClearRoot.setPickOnBounds(false);
            victoryRoot.setPickOnBounds(false);
            gameOverRoot.setPickOnBounds(false);

            rootNode.getChildren().addAll(introRoot, loginRoot, registerRoot, storyIntroRoot, menuRoot, hudRoot, levelClearRoot, victoryRoot,
                    gameOverRoot, pauseRoot, settingRoot,shopRoot);

            StackPane.setAlignment(introRoot, Pos.CENTER);
            StackPane.setAlignment(storyIntroRoot, Pos.CENTER);
            StackPane.setAlignment(menuRoot, Pos.CENTER);
            StackPane.setAlignment(hudRoot, Pos.BOTTOM_CENTER);
            StackPane.setAlignment(levelClearRoot, Pos.CENTER);
            StackPane.setAlignment(victoryRoot, Pos.CENTER);
            StackPane.setAlignment(gameOverRoot, Pos.CENTER);
            StackPane.setAlignment(pauseRoot, Pos.CENTER);
            StackPane.setAlignment(settingRoot, Pos.CENTER);
            StackPane.setAlignment(loginRoot, Pos.CENTER);
            StackPane.setAlignment(registerRoot, Pos.CENTER);

            hideAllScreens();
            introRoot.setVisible(true);
            introRoot.toFront();

        } catch (Exception e) {
            System.err.println("Loi FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleStateChange(GameState state) {
        if (introRoot == null || storyIntroRoot == null || menuRoot == null || hudRoot == null || levelClearRoot == null ||
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
        if (world == null) return;

        if (hudController != null && world.isPlaying()) {
            hudController.updateData(
                    world,
                    world.getPlayer(),
                    world.getLevelManager(),
                    world.getMissionManager(),
                    world.getEnemies() != null ? world.getEnemies().size() : 0,
                    world.getItems() != null ? world.getItems().size() : 0
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
        if (world == null) {
            return;
        }

        world.setGameStateListener(this::handleStateChange);

        bindIntroActions(world);
        bindLoginActions(world);
        bindRegisterActions();
        bindGameActions(world);
    }
    private void bindIntroActions(GameWorld world) {
        if (introController == null) {
            return;
        }

        introController.setOnIntroFinished(() -> {
            Platform.runLater(() -> {

                // Chuẩn bị Login ở phía sau Intro
                loginRoot.setOpacity(0.0);
                loginRoot.setVisible(true);
                loginRoot.toBack();
                introRoot.toFront();

                FadeTransition fadeOut =
                        new FadeTransition(Duration.seconds(0.6), introRoot);

                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                FadeTransition fadeIn =
                        new FadeTransition(Duration.seconds(0.6), loginRoot);

                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                fadeOut.play();
                fadeIn.play();

                fadeOut.setOnFinished(event -> {
                    introRoot.setVisible(false);
                    introRoot.setOpacity(1.0);

                    loginRoot.setOpacity(1.0);
                    loginRoot.toFront();
                });
            });
        });
    }

    private void bindLoginActions(GameWorld world) {
        if (loginController == null) {
            return;
        }

        loginController.setOnRegisterRequested(
                this::showRegisterScreen
        );

        loginController.setOnLoginSuccess(() -> {
            String username =
                    com.soulknight.database.UserSession
                            .getCurrentUsername();

            /*
             * Username đăng nhập trở thành player_name
             * dùng cho hệ thống save hiện tại.
             */
            world.setCurrentPlayerName(username);

            /*
             * Chỉ load save sau khi người chơi
             * đăng nhập thành công.
             */
            world.loadGameAsync(username);

            showMainMenu(world);
        });
    }

    private void bindRegisterActions() {
        if (registerController == null) {
            return;
        }

        registerController.setOnLoginRequested(() -> {
            String username =
                    registerController.getEnteredUsername();

            showLoginScreen();

            if (loginController != null) {
                loginController.setUsername(username);
            }
        });
    }

    public void bindGameActions(GameWorld world) {
        if (world == null) return;

        final SoundManager sound = SoundManager.getInstance();

        if (menuController != null) {
            menuController.setOnPlayRequested(() -> {
                sound.playSFX("button");

                // KIỂM TRA LẦN ĐẦU CHƠI:
                if (UserSession.isFirstPlay() && storyIntroController != null) {
                    // 1. Ẩn Main Menu
                    menuRoot.setVisible(false);

                    // 2. Hiện StoryIntro
                    storyIntroRoot.setVisible(true);
                    storyIntroRoot.toFront();

                    // 3. Đăng ký sự kiện khi cốt truyện chạy xong (hoặc bị skip)
                    storyIntroController.setOnIntroFinished(() -> {
                        Platform.runLater(() -> {UserSession.setFirstPlay(false);

                            new Thread(() -> userDAO.setFirstPlay(UserSession.getCurrentUserId(),
                                    false)).start();

                            storyIntroRoot.setVisible(false);
                            world.changeState(GameState.PLAYING);
                        });
                    });

                    // 4. Bắt đầu phát hoạt ảnh Story
                    storyIntroController.startStory();
                } else {
                    // Từ lần chơi thứ 2 trở đi: Vào thẳng game
                    world.changeState(GameState.PLAYING);
                }
            });

            menuController.setOnSettingsRequested(() -> {
                sound.playSFX("button");
                menuRoot.setVisible(false);
                settingRoot.setVisible(true);
                settingRoot.toFront();

                if (settingController != null) {
                    boolean isTouchpad = world.getInputHandler() != null && world.getInputHandler().isTouchpadModeEnabled();

                    settingController.setup(
                            () -> { // Nút Close quay lại Main Menu
                                sound.playSFX("button");
                                settingRoot.setVisible(false);
                                menuRoot.setVisible(true);
                                menuRoot.toFront();
                            },
                            sound::setBGMVolume,
                            sound::setSFXVolume,
                            (touchpadEnabled) -> { // Callback khi thay đổi Control Mode
                                if (world.getInputHandler() != null) {
                                    world.getInputHandler().setTouchpadModeEnabled(touchpadEnabled);
                                }
                            },
                            sound.getBgmVolume(),
                            sound.getSfxVolume(),
                            isTouchpad
                    );
                }
            });

            menuController.setOnShopRequested(() -> {
                sound.playSFX("button");
                menuRoot.setVisible(false);
                shopRoot.setVisible(true);
                shopRoot.toFront();

                if (shopController != null) {
                    shopController.setup(world, () -> {
                        sound.playSFX("button");
                        shopRoot.setVisible(false);
                        menuRoot.setVisible(true);
                        menuRoot.toFront();
                    });
                }
            });
        }

        if (hudController != null) {
            hudController.setOnPauseRequested(() -> {
                if (world.getState() == GameState.PLAYING) {
                    sound.playSFX("button");
                    world.changeState(GameState.PAUSED);
                }
            });

            // Doi vu khi (sung <-> kiem) khi bam vao vong vu khi tren HUD
            hudController.setOnWeaponSwitchRequested(() -> {
                if (world.getState() == GameState.PLAYING) {
                    sound.playSFX("button");
                    world.switchPlayerWeapon();
                    Player player = world.getPlayer();
                    if (player != null && player.getWeapon() != null) {
                        Weapon currentWeapon = player.getWeapon();
                        String weaponName = currentWeapon.getName();
                        WeaponType type = findWeaponTypeByName(weaponName);
                        if (type != null) {
                            currentWeapon.withSound(type.getSoundPath())
                                    .withImage(type.getImagePath());
                        }
                    }
                }
            });
        }

        if (pauseController != null) {
            pauseController.setCallbacks(
                    () -> { // Tiếp tục chơi
                        sound.playSFX("button");
                        world.changeState(GameState.PLAYING);
                    },
                    () -> { // Mở Cài đặt từ Pause
                        sound.playSFX("button");
                        pauseRoot.setVisible(false);
                        settingRoot.setVisible(true);
                        settingRoot.toFront();

                        if (settingController != null) {
                            boolean isTouchpad = world.getInputHandler() != null && world.getInputHandler().isTouchpadModeEnabled();

                            settingController.setup(
                                    () -> { // Nút Close quay lại Pause Menu
                                        sound.playSFX("button");
                                        settingRoot.setVisible(false);
                                        pauseRoot.setVisible(true);
                                        pauseRoot.toFront();
                                    },
                                    sound::setBGMVolume,
                                    sound::setSFXVolume,
                                    (touchpadEnabled) -> { // Callback khi thay đổi Control Mode
                                        if (world.getInputHandler() != null) {
                                            world.getInputHandler().setTouchpadModeEnabled(touchpadEnabled);
                                        }
                                    },
                                    sound.getBgmVolume(),
                                    sound.getSfxVolume(),
                                    isTouchpad
                            );
                        }
                    },
                    () -> { // Quay lại Main Menu
                        sound.playSFX("button");
                        sound.stopBGM();
                        world.changeState(GameState.MAIN_MENU);
                    }
            );
        }
    }
    private WeaponType findWeaponTypeByName(String rawName) {
        if (rawName == null) return null;

        String normalizedInput = rawName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        for (WeaponType type : WeaponType.values()) {
            String normalizedDisplayName = type.getDisplayName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedDisplayName.equals(normalizedInput)) {
                return type;
            }

            String normalizedEnumName = type.name().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedEnumName.equals(normalizedInput)) {
                return type;
            }
        }

        return null;
    }
    private void showLoginScreen() {

        loginRoot.setOpacity(1);
        loginRoot.setVisible(true);
        loginRoot.toFront();

        introRoot.setVisible(false);
    }

    private void showRegisterScreen() {
        hideAllScreens();

        registerRoot.setOpacity(1.0);
        registerRoot.setVisible(true);
        registerRoot.toFront();
    }

    private void showMainMenu(GameWorld world) {
        hideAllScreens();

        world.changeState(GameState.MAIN_MENU);

        menuRoot.setOpacity(1.0);
        menuRoot.setVisible(true);
        menuRoot.toFront();
    }

    private void hideAllScreens() {
        if (introRoot != null) introRoot.setVisible(false);
        if (loginRoot != null) loginRoot.setVisible(false);
        if (registerRoot != null) registerRoot.setVisible(false);
        if (storyIntroRoot != null) storyIntroRoot.setVisible(false);
        if (menuRoot != null) menuRoot.setVisible(false);
        if (hudRoot != null) hudRoot.setVisible(false);
        if (levelClearRoot != null) levelClearRoot.setVisible(false);
        if (victoryRoot != null) victoryRoot.setVisible(false);
        if (gameOverRoot != null) gameOverRoot.setVisible(false);
        if (pauseRoot != null) pauseRoot.setVisible(false);
        if (settingRoot != null) settingRoot.setVisible(false);
        if (shopRoot != null) shopRoot.setVisible(false);
    }
}