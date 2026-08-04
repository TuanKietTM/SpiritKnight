package com.soulknight;

import com.soulknight.database.DatabaseInitializer;
import com.soulknight.database.DatabaseManager;
import com.soulknight.engine.GameLoop;
import com.soulknight.engine.GameState;
import com.soulknight.engine.GameWorld;
import com.soulknight.engine.InputHandler;
import com.soulknight.ui.UIManager;
import com.soulknight.utils.Constants;
import javafx.application.Application;
import javafx.application.Platform;
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

        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);

        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        InputHandler inputHandler = new InputHandler();
        inputHandler.bind(scene);

        GameWorld world = new GameWorld(inputHandler);


        UIManager uiManager = new UIManager(root);
        uiManager.bindGameWorld(world);

        GameLoop gameLoop = new GameLoop(delta -> {world.handleGlobalInput();

            if (world.getState() == GameState.PAUSED) {world.render(gc, canvas.getWidth(), canvas.getHeight());
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

            DatabaseInitializer.initialize();
        });

        databaseThread.setName("database-initializer-thread");
        databaseThread.setDaemon(true);
        databaseThread.start();

        stage.setTitle(Constants.GAME_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(Constants.WINDOW_WIDTH);
        stage.setMinHeight(Constants.WINDOW_HEIGHT);

        stage.setOnCloseRequest(event -> {
            try {
                if (gameLoop != null) {
                    gameLoop.stop();
                }

                if (com.soulknight.database.UserSession.isLoggedIn()) {
                    Thread saveThread = new Thread(world::saveGameNow);
                    saveThread.setName("shutdown-save-thread");
                    saveThread.start();

                    try {
                        saveThread.join(3000);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (world != null) {
                    world.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Có lỗi xảy ra khi tắt game: " + e.getMessage());
            } finally {

                Platform.exit();
                System.exit(0);
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