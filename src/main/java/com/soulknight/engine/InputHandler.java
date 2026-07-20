package com.soulknight.engine;

import com.soulknight.utils.Vector2D;
import java.util.EnumSet;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

public final class InputHandler {
//(cuongpluss) xu ly su kien dau vao : ban phim va chuot
//    class nay se quy linh viec xu ly chung cua game
//    xu ly di chuyen cua player se duoc goi rieng trong player.java

    private final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private boolean fireHeld;
    private boolean confirmRequested;
    private boolean escapeRequested;
    private boolean toggleMuteRequested;

    private double mouseX;
    private double mouseY;

    public void bind(Scene scene) {

//        do su dung javafx nen phai dung EvenFilter de giup cac button khong bi nuot
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
//ESC de pause game va tiep tuc
            if (code == KeyCode.ESCAPE) {
                escapeRequested = true;
                event.consume(); // Chan su kien truyen vao UI de tranh focus vao cac button lam cho ko bam duoc
            } else if (code == KeyCode.M) {// M de muted
                toggleMuteRequested = true;// duoc goi trong soundmanager
                event.consume();
            }
        });

        // Bat cac su kien ban phim khac WASD , mui ten , Enter ,  Space
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (!pressedKeys.contains(code)) {
                if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                    confirmRequested = true;
                }
            }
            pressedKeys.add(code);
        });

        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));
//        Xu li chuot
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
            if (event.getButton() == MouseButton.PRIMARY) {
                fireHeld = true;
            }
            confirmRequested = true;
        });

        scene.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                fireHeld = false;
            }
        });

        scene.setOnKeyTyped(event -> {
            String character = event.getCharacter();
            if (" ".equals(character) || "\r".equals(character)) {
                confirmRequested = true;
            }
        });
    }

    // Don dep trang thai phim tranh nuot phim
    public void clearState() {
        pressedKeys.clear();
        fireHeld = false;
        confirmRequested = false;
        escapeRequested = false;
        toggleMuteRequested = false;
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
// ham bo tro cho ham su dung evenfilter phia tren
    public boolean consumeEscapeRequest() {
        boolean requested = escapeRequested;
        escapeRequested = false;
        return requested;
    }

    public boolean consumeToggleMuteRequest() {
        boolean requested = toggleMuteRequested;
        toggleMuteRequested = false;
        return requested;
    }

    public Vector2D getMousePosition() {
        return new Vector2D(mouseX, mouseY);
    }
}