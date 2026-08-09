package com.soulknight.cache;

import com.soulknight.buff.BuffType;
import com.soulknight.entity.HeroType;
import com.soulknight.pet.PetType;
import com.soulknight.weapon.WeaponType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Cache du lieu cua tai khoan trong suot session.
 *
 * UI va gameplay doc RAM truoc.
 * Database chi dung de load lan dau va dong bo background.
 */
public final class PlayerSessionCache {

    private static final PlayerSessionCache INSTANCE = new PlayerSessionCache();

    private int userId = -1;
    private int gold;
    private int gems;
    private boolean shopLoaded;
    private boolean buffsLoaded;
    private boolean equipmentLoaded;

    private final Set<PetType> ownedPets =
            EnumSet.noneOf(PetType.class);

    private final Set<WeaponType> ownedWeapons =
            EnumSet.noneOf(WeaponType.class);

    private final Set<HeroType> ownedHeroes =
            EnumSet.noneOf(HeroType.class);

    private final Map<BuffType, Integer> buffQuantities =
            new EnumMap<>(BuffType.class);

    private PlayerSessionCache() {
    }

    public static PlayerSessionCache getInstance() {
        return INSTANCE;
    }

    public synchronized void beginSession(int userId) {

        if (this.userId == userId) {
            return;
        }

        clear();

        this.userId = userId;
    }

    public synchronized void clear() {
        userId = -1;

        gold = 0;
        gems = 0;

        shopLoaded = false;
        buffsLoaded = false;
        equipmentLoaded = false;

        ownedPets.clear();
        ownedWeapons.clear();
        ownedHeroes.clear();
        buffQuantities.clear();
    }

    public synchronized int getUserId() {
        return userId;
    }

    public synchronized int getGold() {
        return gold;
    }

    public synchronized void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public synchronized void addGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    public synchronized int getGems() {
        return gems;
    }

    public synchronized void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public synchronized Set<PetType> getOwnedPets() {
        return Collections.unmodifiableSet(
                EnumSet.copyOf(ownedPets)
        );
    }

    public synchronized void replaceOwnedPets(Set<PetType> values) {
        ownedPets.clear();

        if (values != null) {
            ownedPets.addAll(values);
        }
    }

    public synchronized void addOwnedPet(PetType type) {
        if (type != null) {
            ownedPets.add(type);
        }
    }

    public synchronized Set<WeaponType> getOwnedWeapons() {
        return Collections.unmodifiableSet(EnumSet.copyOf(ownedWeapons));
    }

    public synchronized void replaceOwnedWeapons(Set<WeaponType> values) {
        ownedWeapons.clear();
        if (values != null) {
            ownedWeapons.addAll(values);
        }
    }

    public synchronized void addOwnedWeapon(
            WeaponType type
    ) {
        if (type != null) {
            ownedWeapons.add(type);
        }
    }

    public synchronized Set<HeroType> getOwnedHeroes() {
        return Collections.unmodifiableSet(
                EnumSet.copyOf(ownedHeroes)
        );
    }

    public synchronized void replaceOwnedHeroes(Set<HeroType> values) {
        ownedHeroes.clear();
        if (values != null) {
            ownedHeroes.addAll(values);
        }
    }

    public synchronized void addOwnedHero(HeroType type) {
        if (type != null) {
            ownedHeroes.add(type);
        }
    }

    public synchronized int getBuffQuantity(BuffType type) {
        if (type == null) {
            return 0;
        }
        return buffQuantities.getOrDefault(type, 0);
    }

    public synchronized void replaceBuffQuantities(Map<BuffType, Integer> values) {
        buffQuantities.clear();
        if (values == null) {
            return;
        }

        for (Map.Entry<BuffType, Integer> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }

            buffQuantities.put(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void addBuff(BuffType type, int amount) {
        if (type == null || amount <= 0) {
            return;
        }

        buffQuantities.merge(type, amount, Integer::sum);
    }

    public synchronized boolean consumeBuff(
            BuffType type
    ) {
        int current = getBuffQuantity(type);

        if (current <= 0) {
            return false;
        }

        if (current == 1) {
            buffQuantities.remove(type);
        } else {
            buffQuantities.put(type, current - 1);
        }

        return true;
    }

    public synchronized boolean isShopLoaded() {
        return shopLoaded;
    }

    public synchronized void setShopLoaded(
            boolean shopLoaded
    ) {
        this.shopLoaded = shopLoaded;
    }

    public synchronized boolean isBuffsLoaded() {
        return buffsLoaded;
    }

    public synchronized void setBuffsLoaded(boolean buffsLoaded) {
        this.buffsLoaded = buffsLoaded;
    }

    public synchronized boolean isEquipmentLoaded() {
        return equipmentLoaded;
    }

    public synchronized void setEquipmentLoaded(boolean equipmentLoaded) {
        this.equipmentLoaded = equipmentLoaded;
    }
}