package com.soulknight;

import com.soulknight.engine.GameLoop;
import com.soulknight.engine.GameWorld;
import com.soulknight.engine.InputHandler;
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
        scene.setOnMouseClicked(event -> canvas.requestFocus());

        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        InputHandler inputHandler = new InputHandler();
        inputHandler.bind(scene);

        GameWorld world = new GameWorld(inputHandler);
        GameLoop gameLoop = new GameLoop(delta -> {
            world.update(delta, canvas.getWidth(), canvas.getHeight());
            world.render(graphicsContext, canvas.getWidth(), canvas.getHeight());
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
