package com.soulknight;

import com.soulknight.database.DatabaseInitializer;
import com.soulknight.database.DatabaseManager;
import com.soulknight.engine.*;
import com.soulknight.ui.UIManager;
import com.soulknight.utils.Constants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class Main extends Application {

    private static final String PLAYER_NAME = "Knight";

    @Override
    public void start(Stage stage) {

        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        canvas.setFocusTraversable(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        InputHandler inputHandler = new InputHandler();
        inputHandler.bind(scene);

        GameWorld world = new GameWorld(inputHandler);
        world.setCurrentPlayerName(PLAYER_NAME);

        UIManager uiManager = new UIManager(root);
        world.setGameStateListener(uiManager::handleStateChange);
        uiManager.bindGameActions(world);

        GameLoop gameLoop = new GameLoop(delta -> {
            world.handleGlobalInput();

            if (world.getState() == GameState.PAUSED) {
                world.render(gc, canvas.getWidth(), canvas.getHeight());
            } else {
                world.update(delta, canvas.getWidth(), canvas.getHeight());
                world.render(gc, canvas.getWidth(), canvas.getHeight());
                uiManager.updateHUD(world);
            }
        });

        Thread databaseThread = new Thread(() -> {
            if (!DatabaseManager.testConnection()) {
                System.out.println("Database không khả dụng.");
                return;
            }

            System.out.println("Aiven MySQL đang hoạt động.");

            if (DatabaseInitializer.initialize()) {
                world.loadGameAsync(PLAYER_NAME);
            }
        });

        databaseThread.setName("database-thread");
        databaseThread.setDaemon(true);
        databaseThread.start();

        stage.setTitle(Constants.GAME_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(Constants.WINDOW_WIDTH);
        stage.setMinHeight(Constants.WINDOW_HEIGHT);

        stage.setOnCloseRequest(event -> {
            gameLoop.stop();

            Thread saveThread = new Thread(world::saveGameNow);
            saveThread.start();

            try {
                saveThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        stage.show();
        canvas.requestFocus();
        gameLoop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}