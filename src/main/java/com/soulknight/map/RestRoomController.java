package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;

public final class RestRoomController {

    private final Room room;
    private RestShrine shrine;

    private boolean initialized;

    public RestRoomController(Room room) {
        this.room = room;
    }

    public void update(GameWorld world, Player player, double deltaSeconds) {
        if (world == null || player == null || room == null) return;

        if (!initialized) initialize();

        if (shrine != null) {
            shrine.update(world, player, deltaSeconds);
        }
    }

    private void initialize() {
        initialized = true;

        double centerX = room.getBound().getMinX() + room.getBound().getWidth() * 0.5;
        double centerY = room.getBound().getMinY() + room.getBound().getHeight() * 0.5;

        shrine = new RestShrine(new Vector2D(centerX, centerY));
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (shrine != null) shrine.render(gc, camera);
    }

    public RestShrine getShrine() {
        return shrine;
    }
}