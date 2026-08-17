package com.soulknight.pet;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lưu pet hiện đang được trang bị.
 */
public final class PetSelectionManager {

    private static final PetSelectionManager INSTANCE =
            new PetSelectionManager();

    /*
     * Pet mặc định để test.
     */
    private PetType selectedPet = PetType.CAT;

    private Consumer<PetType> selectionListener;

    private PetSelectionManager() {
    }

    public static PetSelectionManager getInstance() {
        return INSTANCE;
    }

    public PetType getSelectedPet() {
        return selectedPet;
    }

    public void selectPet(PetType type) {
        PetType safeType = Objects.requireNonNullElse(
                type,
                PetType.NONE
        );

        if (selectedPet == safeType) {
            return;
        }

        selectedPet = safeType;

        if (selectionListener != null) {
            selectionListener.accept(selectedPet);
        }
    }

    public boolean isSelected(PetType type) {
        return selectedPet == type;
    }

}