package com.soulknight.pet;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.map.Room;
import com.soulknight.utils.Vector2D;

/**
 * Quản lý quá trình chờ Pet đi vào phòng.
 *
 * Luồng:
 * 1. Knight vào phòng.
 * 2. Chờ Pet tự chạy theo.
 * 3. Pet vào phòng thì cho phép đóng cửa.
 * 4. Hết thời gian thì PetRoomPlacementService xử lý.
 */
public final class PetRoomEntryController {

    public enum EntryResult {
        WAITING,
        READY,
        CANCELLED
    }

    private static final double PET_ENTRY_TIMEOUT = 1.8;

    private boolean pending;
    private double waitTimer;

    public void begin() {
        pending = true;
        waitTimer = PET_ENTRY_TIMEOUT;
    }

    public EntryResult update(GameWorld gameWorld, Room room, Player player, double deltaSeconds) {
        if (!pending) {
            return EntryResult.READY;
        }

        if (gameWorld == null || room == null || player == null || player.getPosition() == null) {
            cancel();
            return EntryResult.CANCELLED;
        }

        Vector2D playerPosition = player.getPosition();

        // Knight chạy ra khỏi phòng trước khi cửa đóng.
        if (!room.containsPosition(playerPosition, 0.0)) {
            cancel();
            return EntryResult.CANCELLED;
        }

        PetRoomInfo petInfo = PetRoomInfo.from(gameWorld);

        // Không trang bị Pet thì phòng được kích hoạt ngay.
        if (!petInfo.exists()) {
            finish();
            return EntryResult.READY;
        }

        // Pet đã nằm hoàn toàn trong phòng.
        if (room.containsPosition(petInfo.getPosition(), petInfo.getRadius())) {
            finish();
            return EntryResult.READY;
        }

        waitTimer -= Math.max(0.0, deltaSeconds);

        if (waitTimer > 0.0) {
            return EntryResult.WAITING;
        }

        // Pet bị kẹt hoặc chưa kịp vào, tìm vị trí hợp lệ trong phòng.
        PetRoomPlacementService.placePetInsideRoom(gameWorld, room);

        finish();
        return EntryResult.READY;
    }

    public void finish() {
        pending = false;
        waitTimer = 0.0;
    }

    public void cancel() {
        pending = false;
        waitTimer = 0.0;
    }

    public boolean isPending() {
        return pending;
    }

    public double getWaitTimer() {
        return waitTimer;
    }
}