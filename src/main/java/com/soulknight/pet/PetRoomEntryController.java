package com.soulknight.pet;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.map.Room;
import com.soulknight.utils.Vector2D;

/**
 * Quản lý quá trình chờ Pet đi vào phòng trước khi khóa cửa kích hoạt trận đấu.
 *
 * Luồng xử lý:
 * 1. Knight bước vào phòng chiến đấu.
 * 2. Cửa phòng vẫn mở, đếm ngược đếm chờ Pet tự chạy vào.
 * 3. Nếu Pet vào phòng thành công -> Cho phép đóng cửa kích hoạt Wave.
 * 4. Nếu Knight quay đít chạy ra khỏi phòng -> Hủy kích hoạt.
 * 5. Nếu hết thời gian đếm ngược (Pet kẹt tường/đi chậm) -> Dùng PetRoomPlacementService dịch chuyển Pet vào.
 */
public final class PetRoomEntryController {

    public enum EntryResult {
        WAITING,
        READY,
        CANCELLED
    }

    private static final double PET_ENTRY_TIMEOUT = 1.8; // Thời gian chờ tối đa (giây)

    private boolean pending;
    private double waitTimer;

    /**
     * Bắt đầu đếm ngược chờ Pet vào phòng.
     */
    public void begin() {
        this.pending = true;
        this.waitTimer = PET_ENTRY_TIMEOUT;
    }

    /**
     * Cập nhật tiến trình từng frame.
     */
    public EntryResult update(GameWorld gameWorld, Room room, Player player, double deltaSeconds) {
        if (!pending) {
            return EntryResult.READY;
        }

        // Kiểm tra tính hợp lệ của tham số
        if (gameWorld == null || room == null || player == null || player.getPosition() == null) {
            cancel();
            return EntryResult.CANCELLED;
        }

        Vector2D playerPosition = player.getPosition();

        // 1. Nếu Knight chạy ngược ra ngoài phòng trước khi cửa đóng -> Hủy kích hoạt
        if (!room.containsPosition(playerPosition, 0.0)) {
            cancel();
            return EntryResult.CANCELLED;
        }

        PetRoomInfo petInfo = PetRoomInfo.from(gameWorld);

        // 2. Không mang theo Pet hoặc Pet không còn sống/tồn tại -> Cho phép kích hoạt phòng ngay lập tức
        if (!petInfo.exists()) {
            finish();
            return EntryResult.READY;
        }

        // 3. Pet đã đi vào an toàn bên trong phòng -> Hoàn tất chờ, sẵn sàng đóng cửa
        if (room.containsPosition(petInfo.getPosition(), petInfo.getRadius())) {
            finish();
            return EntryResult.READY;
        }

        // 4. Giảm thời gian đếm ngược
        waitTimer -= Math.max(0.0, deltaSeconds);

        if (waitTimer > 0.0) {
            return EntryResult.WAITING;
        }

        // 5. Quá thời gian chờ (Pet bị kẹt đường/đi chậm): Tự động đặt Pet vào vị trí an toàn trong phòng
        try {
            PetRoomPlacementService.placePetInsideRoom(gameWorld, room);
        } catch (Exception e) {
            System.err.println("[PetRoomEntryController] Lỗi khi dịch chuyển Pet vào phòng: " + e.getMessage());
        }

        finish();
        return EntryResult.READY;
    }

    /**
     * Hoàn thành quá trình chờ thành công.
     */
    public void finish() {
        this.pending = false;
        this.waitTimer = 0.0;
    }

    /**
     * Hủy bỏ quá trình chờ (Player đi ra ngoài hoặc có lỗi).
     */
    public void cancel() {
        this.pending = false;
        this.waitTimer = 0.0;
    }

    public boolean isPending() {
        return pending;
    }
}