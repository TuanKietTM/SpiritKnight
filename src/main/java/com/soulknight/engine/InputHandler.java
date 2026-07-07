package com.soulknight.engine;

import com.soulknight.utils.Vector2D;
import java.util.EnumSet;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

public final class InputHandler {

    private final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private boolean fireHeld;
    private boolean confirmRequested;
    private double mouseX;
    private double mouseY;

    public void bind(Scene scene) {
        scene.setOnKeyPressed(event -> {
            pressedKeys.add(event.getCode());
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                confirmRequested = true;
            }
        });
        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));
        scene.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        scene.setOnMouseDragged(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        scene.setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            fireHeld = event.getButton() == MouseButton.PRIMARY;
            confirmRequested = true;
        });
        scene.setOnMouseReleased(event -> fireHeld = false);
        scene.setOnKeyTyped(event -> {
            String character = event.getCharacter();
            if (" ".equals(character) || "\r".equals(character)) {
                confirmRequested = true;
            }
        });
    }

    public boolean isDown(KeyCode keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public boolean isFireHeld() {
        return fireHeld;
    }

    public boolean consumeConfirmRequest() {
        boolean requested = confirmRequested;
        confirmRequested = false;
        return requested;
    }

    public Vector2D getMousePosition() {
        return new Vector2D(mouseX, mouseY);
    }
}
