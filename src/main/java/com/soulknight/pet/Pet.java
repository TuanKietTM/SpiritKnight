package com.soulknight.pet;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Pet đi theo Player bằng breadcrumb trail.
 *
 * Cơ chế:
 * 1. Ghi lại con đường Player đã đi.
 * 2. Pet đi lần lượt qua các waypoint.
 * 3. Mỗi bước di chuyển đều gọi GameWorld.canMoveTo().
 * 4. Nếu bị chặn, Pet thử trượt theo trục X/Y và các góc lệch.
 * 5. Nếu bị kẹt quá lâu hoặc quá xa Player, Pet dịch chuyển về điểm an toàn.
 */
public final class Pet {

    public enum State {
        IDLE,
        FOLLOWING,
        CATCHING_UP,
        STUCK
    }

    /*
     * Bán kính dùng để kiểm tra va chạm.
     * Nên nhỏ hơn Player để Pet đi qua hành lang dễ hơn.
     */
    private static final double COLLISION_RADIUS = 13.0;

    /*
     * Khoảng cách Pet bắt đầu chạy theo.
     */
    private static final double START_FOLLOW_DISTANCE = 72.0;

    /*
     * Khoảng cách Pet dừng gần waypoint.
     */
    private static final double WAYPOINT_REACH_DISTANCE = 12.0;

    /*
     * Khoảng cách giữa hai breadcrumb liên tiếp.
     * Số càng nhỏ thì Pet đi theo đường càng chính xác,
     * nhưng danh sách waypoint sẽ dài hơn.
     */
    private static final double BREADCRUMB_SPACING = 18.0;

    /*
     * Giới hạn số waypoint để tránh tăng bộ nhớ vô hạn.
     */
    private static final int MAX_BREADCRUMBS = 180;

    /*
     * Pet quá xa Player sẽ chạy nhanh hơn.
     */
    private static final double CATCH_UP_DISTANCE = 190.0;

    /*
     * Pet quá xa mức này sẽ dịch chuyển.
     */
    private static final double TELEPORT_DISTANCE = 560.0;

    /*
     * Thời gian đứng yên bất thường trước khi coi là bị kẹt.
     */
    private static final double STUCK_TIME_LIMIT = 1.15;

    /*
     * Nếu bị kẹt lâu hơn nữa thì teleport.
     */
    private static final double FORCE_TELEPORT_TIME = 2.6;

    private final PetType type;
    private final Image image;

    /*
     * Đường Player đã đi.
     */
    private final Deque<Vector2D> breadcrumbs = new ArrayDeque<>();

    private final Vector2D position;

    private Vector2D lastRecordedPlayerPosition;
    private Vector2D lastPetPosition;

    private State state = State.IDLE;

    private double idleTime;
    private double walkTime;
    private double stuckTime;

    private boolean active = true;
    private boolean facingRight = true;

    public Pet(
            PetType type,
            double spawnX,
            double spawnY
    ) {
        if (type == null || !type.hasPet()) {
            throw new IllegalArgumentException(
                    "PetType phải khác null và khác NONE."
            );
        }

        this.type = type;
        this.position = new Vector2D(spawnX, spawnY);

        this.lastRecordedPlayerPosition =
                new Vector2D(spawnX, spawnY);

        this.lastPetPosition =
                new Vector2D(spawnX, spawnY);

        this.image = loadImage(type.getImagePath());
    }

    /**
     * Update được GameWorld gọi mỗi frame.
     */
    public void update(
            double deltaSeconds,
            double playerX,
            double playerY,
            GameWorld world
    ) {
        if (!active ||
                deltaSeconds <= 0.0 ||
                world == null ||
                world.getMapManager() == null) {
            return;
        }

        /*
         * Tránh delta quá lớn khi cửa sổ bị lag hoặc vừa resume.
         */
        double safeDelta = Math.min(deltaSeconds, 0.05);

        Vector2D playerPosition =
                new Vector2D(playerX, playerY);

        recordPlayerPath(playerPosition, world);

        double distanceToPlayer =
                position.distance(playerPosition);

        /*
         * Trường hợp Pet tụt lại quá xa.
         */
        if (distanceToPlayer >= TELEPORT_DISTANCE) {
            teleportToSafePosition(playerPosition, world);
            return;
        }

        Vector2D target = chooseTarget(playerPosition);

        double distanceToTarget =
                position.distance(target);

        if (distanceToTarget <= WAYPOINT_REACH_DISTANCE) {
            removeReachedBreadcrumbs();

            target = chooseTarget(playerPosition);
            distanceToTarget = position.distance(target);
        }

        if (breadcrumbs.isEmpty() &&
                distanceToPlayer <= START_FOLLOW_DISTANCE) {
            state = State.IDLE;
            stuckTime = 0.0;
            idleTime += safeDelta;

            lastPetPosition.set(position);
            return;
        }

        if (distanceToPlayer >= CATCH_UP_DISTANCE) {
            state = State.CATCHING_UP;
        } else {
            state = State.FOLLOWING;
        }

        double speedMultiplier =
                calculateSpeedMultiplier(distanceToPlayer);

        double moveDistance =
                type.getMoveSpeed()
                        * speedMultiplier
                        * safeDelta;

        boolean moved = moveSmart(
                target,
                moveDistance,
                world
        );

        updateStuckState(
                moved,
                safeDelta,
                playerPosition,
                world
        );

        updateFacingDirection();

        walkTime += safeDelta;
        idleTime += safeDelta;

        lastPetPosition.set(position);
    }

    /**
     * Ghi lại đường Player đi qua.
     *
     * Chỉ ghi những vị trí hợp lệ để Pet không nhận waypoint
     * nằm trong tường hoặc vật cản.
     */
    private void recordPlayerPath(
            Vector2D playerPosition,
            GameWorld world
    ) {
        if (lastRecordedPlayerPosition == null) {
            lastRecordedPlayerPosition =
                    playerPosition.copy();
            return;
        }

        double distance =
                lastRecordedPlayerPosition.distance(
                        playerPosition
                );

        if (distance < BREADCRUMB_SPACING) {
            return;
        }

        /*
         * Player đang đứng ở vị trí hợp lệ, nhưng vẫn kiểm tra
         * để hệ thống an toàn khi Player vừa teleport.
         */
        if (!world.canMoveTo(
                playerPosition,
                COLLISION_RADIUS
        )) {
            return;
        }

        /*
         * Khi Player teleport hoặc chuyển phòng quá xa,
         * đường cũ không còn ý nghĩa.
         */
        if (distance > 220.0) {
            breadcrumbs.clear();
        }

        breadcrumbs.addLast(
                playerPosition.copy()
        );

        lastRecordedPlayerPosition.set(
                playerPosition
        );

        while (breadcrumbs.size() > MAX_BREADCRUMBS) {
            breadcrumbs.removeFirst();
        }
    }

    /**
     * Pet đi theo waypoint cũ nhất trước.
     *
     * Nhờ vậy Pet sẽ đi đúng con đường Knight vừa đi,
     * không cắt xuyên tường để đến Player.
     */
    private Vector2D chooseTarget(
            Vector2D playerPosition
    ) {
        Vector2D waypoint =
                breadcrumbs.peekFirst();

        if (waypoint != null) {
            return waypoint;
        }

        return playerPosition;
    }

    private void removeReachedBreadcrumbs() {
        while (!breadcrumbs.isEmpty()) {
            Vector2D waypoint =
                    breadcrumbs.peekFirst();

            if (waypoint == null ||
                    position.distance(waypoint)
                            > WAYPOINT_REACH_DISTANCE) {
                break;
            }

            breadcrumbs.removeFirst();
        }
    }

    /**
     * Di chuyển thông minh:
     *
     * 1. Thử đi thẳng.
     * 2. Nếu bị chặn, thử trượt X.
     * 3. Thử trượt Y.
     * 4. Thử nhiều hướng lệch quanh hướng đích.
     */
    private boolean moveSmart(
            Vector2D target,
            double moveDistance,
            GameWorld world
    ) {
        Vector2D difference =
                target.copy().subtract(position);

        double distance = difference.length();

        if (distance <= 0.001) {
            return false;
        }

        difference.normalize();

        double actualMovement =
                Math.min(moveDistance, distance);

        double moveX =
                difference.getX() * actualMovement;

        double moveY =
                difference.getY() * actualMovement;

        /*
         * Ưu tiên đi thẳng đến waypoint.
         */
        if (tryMove(moveX, moveY, world)) {
            return true;
        }

        /*
         * Wall sliding:
         * Nếu đường chéo bị chặn, thử từng trục riêng.
         */
        boolean preferX =
                Math.abs(moveX) >= Math.abs(moveY);

        if (preferX) {
            if (tryMove(moveX, 0.0, world)) {
                return true;
            }

            if (tryMove(0.0, moveY, world)) {
                return true;
            }
        } else {
            if (tryMove(0.0, moveY, world)) {
                return true;
            }

            if (tryMove(moveX, 0.0, world)) {
                return true;
            }
        }

        /*
         * Nếu vẫn bị chặn, quét các góc hai bên.
         */
        double baseAngle =
                Math.atan2(
                        difference.getY(),
                        difference.getX()
                );

        double[] angleOffsets = {
                Math.toRadians(22.5),
                Math.toRadians(-22.5),
                Math.toRadians(45.0),
                Math.toRadians(-45.0),
                Math.toRadians(67.5),
                Math.toRadians(-67.5),
                Math.toRadians(90.0),
                Math.toRadians(-90.0),
                Math.toRadians(135.0),
                Math.toRadians(-135.0)
        };

        for (double offset : angleOffsets) {
            double angle = baseAngle + offset;

            double alternativeX =
                    Math.cos(angle) * actualMovement;

            double alternativeY =
                    Math.sin(angle) * actualMovement;

            if (tryMove(
                    alternativeX,
                    alternativeY,
                    world
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * Thử di chuyển và kiểm tra trước bằng GameWorld.canMoveTo().
     *
     * Vì canMoveTo() đã kiểm tra:
     * - Tile tường
     * - Biên map
     * - Cửa đóng
     * - Obstacle
     */
    private boolean tryMove(
            double moveX,
            double moveY,
            GameWorld world
    ) {
        if (Math.abs(moveX) < 0.0001 &&
                Math.abs(moveY) < 0.0001) {
            return false;
        }

        Vector2D candidate =
                position.copy().add(moveX, moveY);

        if (!world.canMoveTo(
                candidate,
                COLLISION_RADIUS
        )) {
            return false;
        }

        position.set(candidate);
        return true;
    }

    private void updateStuckState(
            boolean moved,
            double deltaSeconds,
            Vector2D playerPosition,
            GameWorld world
    ) {
        double actualMovement =
                lastPetPosition.distance(position);

        if (moved && actualMovement > 0.2) {
            stuckTime = 0.0;
            return;
        }

        stuckTime += deltaSeconds;

        if (stuckTime >= STUCK_TIME_LIMIT) {
            state = State.STUCK;

            /*
             * Waypoint hiện tại có thể đã bị cửa đóng hoặc
             * obstacle mới chặn. Bỏ waypoint đó để thử điểm sau.
             */
            if (!breadcrumbs.isEmpty()) {
                breadcrumbs.removeFirst();
            }
        }

        if (stuckTime >= FORCE_TELEPORT_TIME) {
            teleportToSafePosition(
                    playerPosition,
                    world
            );
        }
    }

    /**
     * Tìm một điểm an toàn quanh Player để teleport.
     *
     * Không đặt Pet trực tiếp lên Player vì có thể nhìn xấu
     * hoặc nằm trong vật cản sát Player.
     */
    private void teleportToSafePosition(
            Vector2D playerPosition,
            GameWorld world
    ) {
        double[][] offsets = {
                {-52.0, 22.0},
                {52.0, 22.0},
                {-44.0, -30.0},
                {44.0, -30.0},
                {0.0, 55.0},
                {0.0, -55.0},
                {-75.0, 0.0},
                {75.0, 0.0}
        };

        for (double[] offset : offsets) {
            Vector2D candidate =
                    playerPosition.copy().add(
                            offset[0],
                            offset[1]
                    );

            if (world.canMoveTo(
                    candidate,
                    COLLISION_RADIUS
            )) {
                position.set(candidate);
                resetPath(playerPosition);
                return;
            }
        }

        /*
         * Trường hợp hiếm: không có điểm quanh Player hợp lệ.
         * Dùng đúng vị trí Player vì Player chắc chắn đang
         * đứng ở khu vực đi được.
         */
        position.set(playerPosition);
        resetPath(playerPosition);
    }

    private void resetPath(
            Vector2D playerPosition
    ) {
        breadcrumbs.clear();

        lastRecordedPlayerPosition =
                playerPosition.copy();

        lastPetPosition =
                position.copy();

        stuckTime = 0.0;
        state = State.IDLE;
    }

    private double calculateSpeedMultiplier(
            double distanceToPlayer
    ) {
        if (distanceToPlayer >= 340.0) {
            return 2.1;
        }

        if (distanceToPlayer >= CATCH_UP_DISTANCE) {
            return 1.55;
        }

        return 1.0;
    }

    private void updateFacingDirection() {
        double differenceX =
                position.getX()
                        - lastPetPosition.getX();

        if (differenceX > 0.1) {
            facingRight = true;
        } else if (differenceX < -0.1) {
            facingRight = false;
        }
    }

    /**
     * Gọi khi chuyển level, đổi phòng đặc biệt hoặc teleport.
     */
    public void onRoomChanged(
            double playerX,
            double playerY
    ) {
        position.set(playerX - 45.0, playerY + 20.0);

        resetPath(
                new Vector2D(playerX, playerY)
        );
    }

    public void render(
            GraphicsContext graphicsContext,
            Camera camera
    ) {
        if (!active ||
                graphicsContext == null ||
                camera == null ||
                image == null) {
            return;
        }

        double zoom = camera.getZoom();

        double renderX =
                camera.worldToScreenX(
                        position.getX()
                );

        double renderY =
                camera.worldToScreenY(
                        position.getY()
                );

        if (state == State.IDLE) {
            renderY +=
                    Math.sin(idleTime * 4.2)
                            * 2.0
                            * zoom;
        } else {
            renderY -=
                    Math.abs(
                            Math.sin(walkTime * 10.0)
                    ) * 2.5 * zoom;
        }

        double width =
                type.getRenderWidth() * zoom;

        double height =
                type.getRenderHeight() * zoom;

        /*
         * Lật ảnh khi Pet quay trái.
         */
        if (facingRight) {
            graphicsContext.drawImage(
                    image,
                    renderX - width / 2.0,
                    renderY - height,
                    width,
                    height
            );
        } else {
            graphicsContext.save();

            graphicsContext.translate(
                    renderX,
                    0.0
            );

            graphicsContext.scale(
                    -1.0,
                    1.0
            );

            graphicsContext.drawImage(
                    image,
                    -width / 2.0,
                    renderY - height,
                    width,
                    height
            );

            graphicsContext.restore();
        }
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        URL resource =
                Pet.class.getResource(path);

        if (resource == null) {
            System.err.println(
                    "Không tìm thấy ảnh Pet: " + path
            );
            return null;
        }

        return new Image(
                resource.toExternalForm(),
                false
        );
    }

    public void teleport(
            double newX,
            double newY
    ) {
        position.set(newX, newY);

        breadcrumbs.clear();

        lastPetPosition.set(position);
        lastRecordedPlayerPosition.set(position);

        stuckTime = 0.0;
        state = State.IDLE;
    }

    public PetType getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public double getRadius() {
        return COLLISION_RADIUS;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFollowing() {
        return state == State.FOLLOWING ||
                state == State.CATCHING_UP;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public int getBreadcrumbCount() {
        return breadcrumbs.size();
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}