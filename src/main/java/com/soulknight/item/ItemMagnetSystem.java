package com.soulknight.item;

import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Co che hut item ve phia nguoi choi khi item nam trong tam hut.
 * Item moi xuat hien se cho mot khoang ngan truoc khi bi hut.
 */
public final class ItemMagnetSystem {

    // Ban kinh hut: can tien lai gan item moi bat dau hut
    private static final double GOLD_RADIUS = 55.0;
    private static final double GEM_RADIUS = 65.0;
    private static final double ENERGY_RADIUS = 60.0;
    private static final double BUFF_RADIUS = 60.0;

    // Toc do toi da khi item den gan Player
    private static final double GOLD_SPEED = 260.0;
    private static final double GEM_SPEED = 290.0;
    private static final double ENERGY_SPEED = 250.0;
    private static final double BUFF_SPEED = 230.0;

    // Item moi sinh ra nam yen mot chut truoc khi bat dau bi hut
    private static final double MAGNET_DELAY = 0.25;

    // Luu thoi gian item da ton tai
    private final Map<Item, Double> itemAges = new IdentityHashMap<>();

    public void update(List<Item> items, Player player, double deltaSeconds) {
        if (items == null || items.isEmpty() || player == null || !player.isAlive()
                || player.getPosition() == null || deltaSeconds <= 0.0) {
            return;
        }

        removeMissingItems(items);

        Vector2D playerPosition = player.getPosition();

        for (Item item : items) {
            if (item == null || item.isCollected() || item.getPosition() == null) {
                continue;
            }

            double age = itemAges.getOrDefault(item, 0.0) + deltaSeconds;
            itemAges.put(item, age);

            if (age < MAGNET_DELAY) {
                continue;
            }

            MagnetSettings settings = getSettings(item);

            if (settings == null) {
                continue;
            }

            pullItem(item, playerPosition, settings.radius(), settings.maxSpeed(), deltaSeconds);
        }
    }

    private void pullItem(Item item, Vector2D target, double magnetRadius,
                          double maxSpeed, double deltaSeconds) {
        Vector2D position = item.getPosition();

        double dx = target.getX() - position.getX();
        double dy = target.getY() - position.getY();
        double distance = Math.hypot(dx, dy);

        if (distance <= 0.001 || distance > magnetRadius) {
            return;
        }

        /*
         * 0.0 khi item vua vao tam hut.
         * 1.0 khi item o rat gan Player.
         */
        double progress = 1.0 - distance / magnetRadius;
        progress = Math.max(0.0, Math.min(1.0, progress));

        /*
         * Tang toc theo duong cong:
         * - Xa: bay cham.
         * - Gan: lao nhanh ve Player.
         */
        double accelerationFactor = progress * progress;
        double minimumSpeed = maxSpeed * 0.22;
        double currentSpeed = minimumSpeed + (maxSpeed - minimumSpeed) * accelerationFactor;

        /*
         * Khi vao rat gan Player thi tang toc them,
         * tao cam giac item bi hut dut khoat.
         */
        if (distance < 18.0) {
            currentSpeed *= 1.55;
        }

        double moveDistance = Math.min(distance, currentSpeed * deltaSeconds);

        position.add(dx / distance * moveDistance, dy / distance * moveDistance);
    }

    private MagnetSettings getSettings(Item item) {
        if (item instanceof GemItem) {
            return new MagnetSettings(GEM_RADIUS, GEM_SPEED);
        }

        if (item instanceof GoldItem) {
            return new MagnetSettings(GOLD_RADIUS, GOLD_SPEED);
        }

        if (item instanceof EnergyCrystal) {
            return new MagnetSettings(ENERGY_RADIUS, ENERGY_SPEED);
        }

        if (item instanceof BuffItem) {
            return new MagnetSettings(BUFF_RADIUS, BUFF_SPEED);
        }

        return null;
    }

    private void removeMissingItems(List<Item> items) {
        Iterator<Item> iterator = itemAges.keySet().iterator();

        while (iterator.hasNext()) {
            Item trackedItem = iterator.next();

            if (trackedItem == null
                    || trackedItem.isCollected()
                    || !items.contains(trackedItem)) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        itemAges.clear();
    }

    private record MagnetSettings(double radius, double maxSpeed) {
    }
}