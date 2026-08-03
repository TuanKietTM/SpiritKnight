package com.soulknight.ui;

import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.engine.GameState;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.utils.Constants;
import com.soulknight.utils.SoundManager;
import com.soulknight.weapon.Weapon;
import com.soulknight.weapon.WeaponType;
import com.soulknight.database.UserDAO;
import com.soulknight.database.UserSession;
import com.soulknight.database.EquipmentLoader;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.CompletableFuture;

public final class UIManager {

    private final StackPane rootNode;
    private IntroController introController;
    private StoryIntroController storyIntroController;
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
    private LeaderboardController leaderboardController;
    private AccountController accountController;

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
    private Parent leaderboardRoot;
    private Parent accountRoot;

    private final UserDAO userDAO = new UserDAO();
    private final CatLoadingOverlay loadingOverlay = new CatLoadingOverlay();
    private final EquipmentLoader equipmentLoader = new EquipmentLoader();

    private final PortalOverlay portalOverlay = new PortalOverlay();
    // Luu trang thai Continue cua tai khoan dang dang nhap
    private boolean continueAvailable;

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

            FXMLLoader leaderboardLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Leaderboard.fxml"));
            leaderboardRoot = leaderboardLoader.load();
            leaderboardController = leaderboardLoader.getController();
            configFullRegion(leaderboardRoot);

            FXMLLoader accountLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Account.fxml"));
            accountRoot = accountLoader.load();
            accountController = accountLoader.getController();
            configFullRegion(accountRoot);
            portalOverlay.setMinSize(0, 0);
            portalOverlay.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            portalOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            configFullRegion(portalOverlay);
            portalOverlay.setVisible(false);
            portalOverlay.setManaged(false);
            configFullRegion(portalOverlay);
            portalOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            menuRoot.setPickOnBounds(false);
            hudRoot.setPickOnBounds(false);
            levelClearRoot.setPickOnBounds(false);
            victoryRoot.setPickOnBounds(false);
            gameOverRoot.setPickOnBounds(false);

            rootNode.getChildren().addAll(introRoot, loginRoot, registerRoot, storyIntroRoot, menuRoot, hudRoot, levelClearRoot, victoryRoot,
                    gameOverRoot, pauseRoot, settingRoot, shopRoot, leaderboardRoot, accountRoot, portalOverlay, loadingOverlay);

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
            StackPane.setAlignment(accountRoot, Pos.CENTER);
            StackPane.setAlignment(portalOverlay, Pos.CENTER);
            StackPane.setAlignment(loadingOverlay, Pos.CENTER);


            loadingOverlay.setVisible(false);
            loadingOverlay.setManaged(false);

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
        if (introController == null
                || introRoot == null
                || loginRoot == null) {
            return;
        }

        introController.setOnIntroFinished(() ->
                Platform.runLater(this::transitionIntroToLogin)
        );
    }
    private void transitionIntroToLogin() {
        // Login phải được chuẩn bị trước khi animation bắt đầu.
        loginRoot.setVisible(true);
        loginRoot.setManaged(true);
        loginRoot.setOpacity(1.0);
        loginRoot.toBack();

        // Intro đang nằm phía trên Login.
        introRoot.setVisible(true);
        introRoot.setManaged(true);
        introRoot.setOpacity(1.0);
        introRoot.toFront();

        FadeTransition fadeIntro =
                new FadeTransition(
                        Duration.millis(550),
                        introRoot
                );

        fadeIntro.setFromValue(1.0);
        fadeIntro.setToValue(0.0);

        fadeIntro.setOnFinished(event -> {
            introRoot.setVisible(false);
            introRoot.setManaged(false);
            introRoot.setOpacity(1.0);

            loginRoot.setOpacity(1.0);
            loginRoot.setVisible(true);
            loginRoot.setManaged(true);
            loginRoot.toFront();

            loginRoot.applyCss();
            loginRoot.layout();

            if (loginController != null) {
                loginController.clearForm();
            }
        });

        fadeIntro.play();
    }

    private void bindLoginActions(GameWorld world) {
        if (loginController == null || menuController == null) return;

        loginController.setLoadingCallbacks(this::showLoading, this::hideLoading);
        loginController.setOnRegisterRequested(this::showRegisterScreen);

        loginController.setOnLoginSuccess(() -> {
            String username = UserSession.getCurrentUsername();
            world.setCurrentPlayerName(username);

            // Mac dinh khoa Continue trong luc dang kiem tra save
            continueAvailable = false;
            menuController.setContinueAvailable(false);
            showLoading("Checking save...");

            CompletableFuture
                    .supplyAsync(() -> {
                        int userId = UserSession.getCurrentUserId();

                        // Tai pet, weapon va hero dang trang bi cua tai khoan
                        equipmentLoader.loadForUser(userId);

                        PlayerSaveDAO saveDAO = new PlayerSaveDAO();
                        boolean hasSave = saveDAO.findByName(username).isPresent();

                        return hasSave && !UserSession.isFirstPlay();
                    })
                    .thenAccept(canContinue -> Platform.runLater(() -> {
                        continueAvailable = canContinue;
                        menuController.setContinueAvailable(canContinue);

                        hideLoading();
                        showMainMenu(world);
                    }))
                    .exceptionally(exception -> {
                        exception.printStackTrace();

                        Platform.runLater(() -> {
                            continueAvailable = false;
                            menuController.setContinueAvailable(false);

                            hideLoading();
                            showMainMenu(world);
                        });

                        return null;
                    });
        });
    }
    private void bindRegisterActions() {
        if (registerController == null) {
            return;
        }
        registerController.setLoadingCallbacks(this::showLoading, this::hideLoading);


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
            menuController.setOnNewGameRequested(() -> {
                if (world.isSaveLoadInProgress()) return;

                sound.playSFX("button");

                /*
                 * Lan dau choi chay StoryIntro.
                 * StoryIntro da co portal o cuoi nen khong chay portal lan hai.
                 */
                if (UserSession.isFirstPlay() && storyIntroController != null) {
                    showFirstStory(world);
                    return;
                }

                // Cac lan sau chi chay portal roi tao game moi
                playPortalBeforeGame(world::startNewGameFromMenu);
            });

            menuController.setOnContinueRequested(() -> {
                if (!continueAvailable || world.isSaveLoadInProgress()) return;

                sound.playSFX("button");
                loadSaveAndContinue(world);
            });
            menuController.setOnAccountRequested(() -> {
                sound.playSFX("button");

                menuRoot.setVisible(false);
                menuRoot.setManaged(false);

                accountRoot.setVisible(true);
                accountRoot.setManaged(true);
                accountRoot.toFront();

                if (accountController != null) {
                    accountController.setup(
                            () -> {
                                sound.playSFX("button");

                                accountRoot.setVisible(false);
                                accountRoot.setManaged(false);

                                menuRoot.setVisible(true);
                                menuRoot.setManaged(true);
                                menuRoot.toFront();

                                menuController.setContinueAvailable(continueAvailable);
                                menuController.startAnimation();
                            },
                            () -> handleLogout(world),
                            this::showLoading,
                            this::hideLoading
                    );
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
            menuController.setOnLeaderboardRequested(() -> {
                sound.playSFX("button");

                menuRoot.setVisible(false);
                leaderboardRoot.setVisible(true);
                leaderboardRoot.toFront();

                leaderboardController.setup(
                        () -> {
                            leaderboardRoot.setVisible(false);
                            menuRoot.setVisible(true);
                            menuRoot.toFront();

                            menuController.setContinueAvailable(continueAvailable);
                            menuController.startAnimation();
                        },
                        this::showLoading,
                        this::hideLoading
                );
            });

            menuController.setOnShopRequested(() -> {
                sound.playSFX("button");
                menuRoot.setVisible(false);
                shopRoot.setVisible(true);
                shopRoot.toFront();

                if (shopController != null) {
                    shopController.setLoadingCallbacks(this::showLoading, this::hideLoading);
                    shopController.setup(world, () -> {
                        sound.playSFX("button");

                        shopRoot.setVisible(false);
                        menuRoot.setVisible(true);
                        menuRoot.toFront();

                        menuController.setContinueAvailable(continueAvailable);
                        menuController.startAnimation();
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
    private void handleLogout(GameWorld world) {
        UserSession.logout();
        continueAvailable = false;

        if (world != null) {
            world.setCurrentPlayerName("");
        }

        if (menuController != null) {
            menuController.setContinueAvailable(false);
        }

        if (loginController != null) {
            loginController.clearForm();
        }

        hideAllScreens();
        showLoginScreen();
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
        if (introRoot != null) {
            introRoot.setVisible(false);
            introRoot.setManaged(false);
            introRoot.setOpacity(1.0);
        }

        if (loginRoot != null) {
            loginRoot.setOpacity(1.0);
            loginRoot.setVisible(true);
            loginRoot.setManaged(true);
            loginRoot.toFront();

            loginRoot.applyCss();
            loginRoot.layout();
        }
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
        menuRoot.setManaged(true);
        menuRoot.toFront();

        if (menuController != null) {
            menuController.startAnimation();
            menuController.setContinueAvailable(continueAvailable);
        }
    }
    public void showLoading(String message) {
        if (Platform.isFxApplicationThread()) {
            loadingOverlay.show(message);
            loadingOverlay.toFront();
        } else {
            Platform.runLater(() -> {
                loadingOverlay.show(message);
                loadingOverlay.toFront();
            });
        }
    }

    public void hideLoading() {
        if (Platform.isFxApplicationThread()) {
            loadingOverlay.hide();
        } else {
            Platform.runLater(loadingOverlay::hide);
        }
    }
    //    chay portal truoc moi game
    private void playPortalBeforeGame(Runnable onFinished) {
        if (menuRoot != null) {
            menuRoot.setVisible(false);
            menuRoot.setManaged(false);
        }

        if (storyIntroRoot != null) {
            storyIntroRoot.setVisible(false);
            storyIntroRoot.setManaged(false);
        }

        // PortalOverlay tu quan ly nen, sprite va animation
        portalOverlay.play(() -> Platform.runLater(() -> {
            if (onFinished != null) {
                onFinished.run();
            }
        }));
    }
    private void showFirstStory(GameWorld world) {
        if (storyIntroController == null || storyIntroRoot == null) {
            world.startNewGameFromMenu();
            return;
        }

        menuRoot.setVisible(false);
        storyIntroRoot.setVisible(true);
        storyIntroRoot.setManaged(true);
        storyIntroRoot.toFront();

        storyIntroController.setOnIntroFinished(() -> Platform.runLater(() -> {
            int userId = UserSession.getCurrentUserId();

            UserSession.setFirstPlay(false);

            // Cap nhat firstPlay ngam de khong lam dung JavaFX thread
            Thread updateThread = new Thread(
                    () -> userDAO.setFirstPlay(userId, false),
                    "update-first-play-thread"
            );

            updateThread.setDaemon(true);
            updateThread.start();

            storyIntroRoot.setVisible(false);
            storyIntroRoot.setManaged(false);

            // Story da co portal, khong chay them portal tai day
            world.startNewGameFromMenu();
        }));

        storyIntroController.startStory();
    }

    // Chi load save khi nguoi choi bam Continue
    private void loadSaveAndContinue(GameWorld world) {
        String username = UserSession.getCurrentUsername();

        if (username == null || username.isBlank()) {
            continueAvailable = false;
            menuController.setContinueAvailable(false);
            return;
        }

        showLoading("Loading save...");

        world.loadGameAsync(username, success -> {
            hideLoading();

            if (!success) {
                continueAvailable = false;
                menuController.setContinueAvailable(false);

                System.err.println(
                        "Khong the tai save de Continue: " + username
                );
                return;
            }

            /*
             * Chi chay portal sau khi pendingPlayerSave
             * da duoc gan trong GameWorld.
             */
            playPortalBeforeGame(world::continueGameFromMenu);
        });
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
        if (leaderboardRoot != null) leaderboardRoot.setVisible(false);
        if (accountRoot != null) {accountRoot.setVisible(false);accountRoot.setManaged(false);
        }
    }
}