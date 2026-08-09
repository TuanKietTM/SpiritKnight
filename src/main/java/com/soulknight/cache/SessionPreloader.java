package com.soulknight.cache;

import com.soulknight.buff.BuffInventoryManager;
import com.soulknight.buff.BuffType;
import com.soulknight.database.ShopDAO;
import com.soulknight.entity.HeroType;
import com.soulknight.pet.PetType;
import com.soulknight.weapon.WeaponType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SessionPreloader {

    private SessionPreloader() {
    }

    public static CompletableFuture<Void> preload(int userId, String username) {
        PlayerSessionCache cache = PlayerSessionCache.getInstance();
        cache.beginSession(userId);
        ShopDAO shopDAO = new ShopDAO();

        CompletableFuture<Integer> goldFuture =
                CompletableFuture.supplyAsync(() -> shopDAO.getGold(username));
        /*
         * Neu ShopDAO cua ban da co getGems theo userId
         * thi dung method hien tai cua ban o day.
         */
        CompletableFuture<Integer> gemsFuture =
                CompletableFuture.supplyAsync(
                        () -> shopDAO.getGems(userId)
                );

        CompletableFuture<Set<PetType>> petsFuture =
                CompletableFuture.supplyAsync(() ->
                        parsePets(shopDAO.getOwnedItems(userId, "PET")));

        CompletableFuture<Set<WeaponType>> weaponsFuture =
                CompletableFuture.supplyAsync(() ->
                        parseWeapons(shopDAO.getOwnedItems(userId, "WEAPON")));

        CompletableFuture<Set<HeroType>> heroesFuture =
                CompletableFuture.supplyAsync(() ->
                        parseHeroes(shopDAO.getOwnedItems(userId, "HERO")));

        CompletableFuture<Map<BuffType, Integer>> buffsFuture =
                CompletableFuture.supplyAsync(() -> parseBuffs(shopDAO.getBuffQuantities(userId)));

        return CompletableFuture
                .allOf(goldFuture, gemsFuture, petsFuture,
                        weaponsFuture, heroesFuture, buffsFuture
                )
                .thenRun(() -> {
                    cache.setGold(goldFuture.join());
                    cache.setGems(gemsFuture.join());
                    cache.replaceOwnedPets(petsFuture.join());
                    cache.replaceOwnedWeapons(weaponsFuture.join());
                    cache.replaceOwnedHeroes(heroesFuture.join());
                    Map<BuffType, Integer> buffs = buffsFuture.join();
                    cache.replaceBuffQuantities(buffs);

                    /*
                     * Dong bo manager gameplay hien tai.
                     */
                    BuffInventoryManager.getInstance().replaceAll(buffs);
                    cache.setShopLoaded(true);
                    cache.setBuffsLoaded(true);
                });
    }

    private static Map<BuffType, Integer> parseBuffs(Map<String, Integer> raw) {
        Map<BuffType, Integer> result =
                new EnumMap<>(BuffType.class);

        if (raw == null) {
            return result;
        }

        for (Map.Entry<String, Integer> entry
                : raw.entrySet()) {

            try {
                BuffType type = BuffType.valueOf(entry.getKey().trim().toUpperCase());

                int quantity = entry.getValue() == null ? 0 : entry.getValue();

                if (quantity > 0) {
                    result.put(type, quantity);
                }

            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static Set<PetType> parsePets(Set<String> raw) {
        Set<PetType> result = EnumSet.noneOf(PetType.class);
        if (raw == null) {
            return result;
        }
        for (String code : raw) {
            try {
                result.add(PetType.valueOf(code));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static Set<WeaponType> parseWeapons(Set<String> raw) {
        Set<WeaponType> result = EnumSet.noneOf(WeaponType.class);
        if (raw == null) {
            return result;
        }

        for (String code : raw) {
            try {
                result.add(WeaponType.valueOf(code));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static Set<HeroType> parseHeroes(
            Set<String> raw
    ) {
        Set<HeroType> result = EnumSet.noneOf(HeroType.class);

        if (raw == null) {
            return result;
        }

        for (String code : raw) {
            try {
                result.add(HeroType.valueOf(code)
                );
            } catch (Exception ignored) {
            }
        }

        return result;
    }
}