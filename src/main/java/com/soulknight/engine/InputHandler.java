package com.soulknight.engine;

import com.soulknight.utils.Vector2D;
import java.util.EnumSet;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

public final class InputHandler {
    // Xu ly su kien dau vao: ban phim, chuot va touchpad
    private final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private boolean fireHeld;
    private boolean confirmRequested;
    private boolean escapeRequested;
    private boolean toggleMuteRequested;

    private double mouseX;
    private double mouseY;

    // Direct input bổ trợ cho Touchpad Joystick
    private final TouchpadJoystick touchpadJoystick = new TouchpadJoystick();
    private boolean touchpadModeEnabled = false; // Mặc định: False (Keyboard & Mouse)

    public void bind(Scene scene) {
        // Sử dụng EventFilter cho ESC & M để không bị JavaFX UI swallow/focus nút
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE) {
                escapeRequested = true;
                event.consume(); // Chặn sự kiện truyền vào UI
            } else if (code == KeyCode.M) {
                toggleMuteRequested = true;
                event.consume();
            }
        });

        // Bắt các sự kiện phím nhấn
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (!pressedKeys.contains(code)) {
                if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                    confirmRequested = true;
                }
            }
            pressedKeys.add(code);

            // Cập nhật trạng thái bắn (fireHeld) dựa trên phím phím phụ khi ở Touchpad mode
            updateFireState();
        });

        scene.setOnKeyReleased(event -> {
            pressedKeys.remove(event.getCode());
            updateFireState();
        });

        // Xử lý di chuyển chuột
        scene.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });

        scene.setOnMouseDragged(event -> {
            mouseX = event.getX();
            mouseY = event.getY();

            // Nếu dùng Touchpad: Kéo chuột/vuốt touchpad để điều khiển Joystick di chuyển
            if (touchpadModeEnabled && touchpadJoystick.isActive()) {
                touchpadJoystick.onTouchMove(event.getX(), event.getY());
            }
        });

        scene.setOnMousePressed(event -> {
            mouseX = event.getX();
            mouseY = event.getY();

            if (event.getButton() == MouseButton.PRIMARY) {
                if (touchpadModeEnabled) {
                    // Chế độ Touchpad: Chuột dùng để vuốt Joystick di chuyển, không giữ bắn
                    touchpadJoystick.onTouchStart(event.getX(), event.getY());
                    fireHeld = true; // Tự động bắn khi đang giữ cảm ứng/chuột
                } else {
                    // Chế độ Keyboard & Mouse: Chuột trái dùng để bắn
                    fireHeld = true;
                }
            }
            confirmRequested = true;
        });

        scene.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (touchpadModeEnabled) {
                    touchpadJoystick.onTouchEnd();
                    fireHeld = false;// Ngừng bắn khi nhả cảm ứng
                } else {
                    fireHeld = false;
                }
            }
        });

        scene.setOnKeyTyped(event -> {
            String character = event.getCharacter();
            if (" ".equals(character) || "\r".equals(character)) {
                confirmRequested = true;
            }
        });
    }

    /**
     * Cập nhật trạng thái đạn bắn (fireHeld)
     */
    private void updateFireState() {
        if (touchpadModeEnabled) {
            // Ở chế độ Touchpad: Bắn đạn bằng phím SPACE hoặc phím J
            fireHeld = pressedKeys.contains(KeyCode.SPACE) || pressedKeys.contains(KeyCode.J);
        } else {
            // Ở chế độ Mouse: fireHeld được quản lý trực tiếp bởi sự kiện MousePressed/Released
            // (Tuy nhiên vẫn hỗ trợ thêm phím J/SPACE nếu muốn)
            if (pressedKeys.contains(KeyCode.J)) {
                fireHeld = true;
            }
        }
    }

    // Dọn dẹp trạng thái phím tránh tình trạng kẹt phím
    public void clearState() {
        pressedKeys.clear();
        fireHeld = false;
        confirmRequested = false;
        escapeRequested = false;
        toggleMuteRequested = false;
        touchpadJoystick.onTouchEnd();
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

    public TouchpadJoystick getTouchpadJoystick() {
        return touchpadJoystick;
    }

    public boolean isTouchpadModeEnabled() {
        return touchpadModeEnabled;
    }

    public void setTouchpadModeEnabled(boolean enabled) {
        this.touchpadModeEnabled = enabled;
        // Reset trạng thái bắn và joystick khi chuyển đổi mode
        fireHeld = false;
        touchpadJoystick.onTouchEnd();
    }
}