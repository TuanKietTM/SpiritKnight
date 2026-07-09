package com.soulknight.ui;

import com.soulknight.engine.GameState;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class UIManager {

    private final StackPane rootNode;
    private HUD hudController;
    private Menu menuController;
    private LevelClearScreen levelClearController;
    private VictoryScreen victoryController;
    private GameOverScreen gameOverController;
    private PauseScreen pauseController;

    private Parent hudRoot;
    private Parent menuRoot;
    private Parent levelClearRoot;
    private Parent victoryRoot;
    private Parent gameOverRoot;
    private Parent pauseRoot;

    public UIManager(StackPane rootNode) {
        this.rootNode = rootNode;
        initViews();
    }

    private void initViews() {
        try {
            FXMLLoader menuLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/Menu.fxml"));
            menuRoot = menuLoader.load();
            menuController = menuLoader.getController();
            configFullRegion(menuRoot);

            FXMLLoader hudLoader = new FXMLLoader(com.soulknight.Main.class.getResource("/assets/fxml/HUD.fxml"));
            hudRoot = hudLoader.load();
            hudController = hudLoader.getController();

            if (hudRoot instanceof Region) {
                Region hudRegion = (Region) hudRoot;
                hudRegion.setMinWidth(Constants.WINDOW_WIDTH);
                hudRegion.setPrefWidth(Constants.WINDOW_WIDTH);
                hudRegion.setMinHeight(86.0);
                hudRegion.setPrefHeight(86.0);
                hudRegion.setMaxHeight(86.0);
            }

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

            menuRoot.setPickOnBounds(false);
            hudRoot.setPickOnBounds(false);
            levelClearRoot.setPickOnBounds(false);
            victoryRoot.setPickOnBounds(false);
            gameOverRoot.setPickOnBounds(false);

            rootNode.getChildren().addAll(menuRoot, hudRoot, levelClearRoot, victoryRoot, gameOverRoot, pauseRoot);

            menuRoot.setVisible(true);
            hudRoot.setVisible(false);
            levelClearRoot.setVisible(false);
            victoryRoot.setVisible(false);
            gameOverRoot.setVisible(false);
            pauseRoot.setVisible(false);

            StackPane.setAlignment(menuRoot, Pos.CENTER);
            StackPane.setAlignment(hudRoot, Pos.BOTTOM_CENTER);
            StackPane.setAlignment(levelClearRoot, Pos.CENTER);
            StackPane.setAlignment(victoryRoot, Pos.CENTER);
            StackPane.setAlignment(gameOverRoot, Pos.CENTER);
            StackPane.setAlignment(pauseRoot, Pos.CENTER);


            handleStateChange(GameState.MAIN_MENU);

        } catch (Exception e) {
            System.err.println("Loi FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleStateChange(GameState state) {
        if (menuRoot == null || hudRoot == null || levelClearRoot == null || victoryRoot == null || gameOverRoot == null)
            return;

        Platform.runLater(() -> {
            switch (state) {
                case MAIN_MENU -> {
                    menuRoot.setVisible(true);
                    hudRoot.setVisible(false);
                    levelClearRoot.setVisible(false);
                    victoryRoot.setVisible(false);
                    gameOverRoot.setVisible(false);
                    menuRoot.toFront();
                }
                case PLAYING -> {
                    menuRoot.setVisible(false);
                    hudRoot.setVisible(true);
                    levelClearRoot.setVisible(false);
                    victoryRoot.setVisible(false);
                    gameOverRoot.setVisible(false);
                    pauseRoot.setVisible(false);
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
                    menuRoot.setVisible(false);
                    hudRoot.setVisible(true);
                    levelClearRoot.setVisible(true);
                    victoryRoot.setVisible(false);
                    gameOverRoot.setVisible(false);
                    levelClearRoot.toFront();
                }
                case GAME_VICTORY -> {
                    menuRoot.setVisible(false);
                    hudRoot.setVisible(false);
                    levelClearRoot.setVisible(false);
                    victoryRoot.setVisible(true);
                    gameOverRoot.setVisible(false);
                    victoryRoot.toFront();
                }
                case GAME_OVER -> {
                    menuRoot.setVisible(false);
                    hudRoot.setVisible(false);
                    levelClearRoot.setVisible(false);
                    victoryRoot.setVisible(false);
                    gameOverRoot.setVisible(true);
                    gameOverRoot.toFront();
                }
                case PAUSED -> {
                    menuRoot.setVisible(false);
                    hudRoot.setVisible(true);
                    levelClearRoot.setVisible(false);
                    victoryRoot.setVisible(false);
                    gameOverRoot.setVisible(false);
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
        if (menuController != null) {
            menuController.setOnPlayRequested(() -> {
                world.changeState(com.soulknight.engine.GameState.PLAYING);
            });
        }
    }

    public void bindGameActions(GameWorld world) {
        if (menuController != null) {
            menuController.setOnPlayRequested(() -> world.changeState(GameState.PLAYING));
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
//                     resume game
                    () -> world.changeState(GameState.PLAYING),
//                    restart game
                    () -> {
                        world.changeState(GameState.PLAYING);
//                        tam thoi chua reset lai game duoc dung tam chu nut nay tac dung van giong nut resume
                    },
//                    return to main menu
                    () -> world.changeState(GameState.MAIN_MENU)
            );
        }
    }
}