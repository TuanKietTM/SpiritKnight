package com.soulknight.buff;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
/**
* Qunan li so luong buff trong inventory cua player / nguonn du lieu cua HUD, SHOP , PAUSE MENU
 **/
public final class BuffInventoryManager {

    private static final BuffInventoryManager INSTANCE =
            new BuffInventoryManager();

    private final Map<BuffType, Integer> quantities =
            new EnumMap<>(BuffType.class);

    private BiConsumer<BuffType, Integer> inventoryListener;

    private BuffInventoryManager() {
    }

    public static BuffInventoryManager getInstance() {
        return INSTANCE;
    }

    public int getQuantity(BuffType type) {
        if (type == null) {
            return 0;
        }

        return quantities.getOrDefault(type, 0);
    }

    public void setQuantity(BuffType type, int quantity) {
        if (type == null) {
            return;
        }

        int safeQuantity = Math.max(0, quantity);

        if (safeQuantity == 0) {
            quantities.remove(type);
        } else {
            quantities.put(type, safeQuantity);
        }

        notifyChanged(type);
    }

    public void add(BuffType type, int amount) {
        if (type == null || amount <= 0) {
            return;
        }

        setQuantity(type, getQuantity(type) + amount);
    }

    public boolean consume(BuffType type) {
        if (type == null) {
            return false;
        }

        int currentQuantity = getQuantity(type);

        if (currentQuantity <= 0) {
            return false;
        }

        setQuantity(type, currentQuantity - 1);
        return true;
    }

    public boolean hasBuff(BuffType type) {
        return getQuantity(type) > 0;
    }

    public Map<BuffType, Integer> getAllBuffs() {
        return Collections.unmodifiableMap(
                new EnumMap<>(quantities)
        );
    }

    public void replaceAll(Map<BuffType, Integer> loadedBuffs) {
        quantities.clear();

        if (loadedBuffs != null) {
            for (Map.Entry<BuffType, Integer> entry :
                    loadedBuffs.entrySet()) {

                if (entry.getKey() == null
                        || entry.getValue() == null
                        || entry.getValue() <= 0) {
                    continue;
                }

                quantities.put(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        if (inventoryListener != null) {
            inventoryListener.accept(null, 0);
        }
    }

    public void clear() {
        quantities.clear();

        if (inventoryListener != null) {
            inventoryListener.accept(null, 0);
        }
    }

    public void setInventoryListener(
            BiConsumer<BuffType, Integer> listener
    ) {
        inventoryListener = listener;
    }

    private void notifyChanged(BuffType type) {
        if (inventoryListener != null) {
            inventoryListener.accept(
                    type,
                    getQuantity(type)
            );
        }
    }
}