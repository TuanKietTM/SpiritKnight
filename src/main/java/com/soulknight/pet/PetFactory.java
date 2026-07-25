package com.soulknight.pet;

/**
 * Chịu trách nhiệm tạo pet.
 *
 * Shop chỉ cần truyền PetType vào đây thay vì tự gọi new Pet().
 */
public final class PetFactory {

    private PetFactory() {
    }

    public static Pet create(
            PetType type,
            double playerX,
            double playerY
    ) {
        if (type == null || type == PetType.NONE) {
            return null;
        }

        double spawnX = playerX - 48;
        double spawnY = playerY + 18;

        return new Pet(
                type,
                spawnX,
                spawnY
        );
    }

    /**
     * Tạo pet đang được chọn trong Shop.
     */
    public static Pet createSelected(
            double playerX,
            double playerY
    ) {
        PetType selectedType =
                PetSelectionManager.getInstance().getSelectedPet();

        return create(
                selectedType,
                playerX,
                playerY
        );
    }
}