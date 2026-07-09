package com.soulknight;

import com.soulknight.engine.GameLoop;
import com.soulknight.engine.GameWorld;
import com.soulknight.engine.InputHandler;
import com.soulknight.ui.UIManager;
import com.soulknight.utils.Constants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class Main extends Application {

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        canvas.setFocusTraversable(true);
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());
        InputHandler inputHandler = new InputHandler();
        inputHandler.bind(scene);
        GameWorld world = new GameWorld(inputHandler);
        UIManager uiManager = new UIManager(root);
        uiManager.bindGameActions(world);
        world.setGameStateListener(uiManager::handleStateChange);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (world.getState() == com.soulknight.engine.GameState.PLAYING) {
                    world.changeState(com.soulknight.engine.GameState.PAUSED);
                } else if (world.getState() == com.soulknight.engine.GameState.PAUSED) {
                    world.changeState(com.soulknight.engine.GameState.PLAYING);
                }
                event.consume();
            }
        });


        GameLoop gameLoop = new GameLoop(delta -> {
            if (world.getState() == com.soulknight.engine.GameState.PAUSED) {
                world.render(graphicsContext, canvas.getWidth(), canvas.getHeight());
            } else {
                world.update(delta, canvas.getWidth(), canvas.getHeight());
                world.render(graphicsContext, canvas.getWidth(), canvas.getHeight());
                uiManager.updateHUD(world);
            }
        });

        stage.setTitle(Constants.GAME_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(Constants.WINDOW_WIDTH);
        stage.setMinHeight(Constants.WINDOW_HEIGHT);
        stage.setOnCloseRequest(event -> gameLoop.stop());
        stage.show();
        canvas.requestFocus();
        gameLoop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}