package com.soulknight.entity;

public final class HeroSelectionManager {

    private static final HeroSelectionManager INSTANCE =
            new HeroSelectionManager();

    private HeroType selectedHero = HeroType.KNIGHT;

    private HeroSelectionManager() {
    }

    public static HeroSelectionManager getInstance() {
        return INSTANCE;
    }

    public HeroType getSelectedHero() {
        return selectedHero;
    }

    public void selectHero(HeroType hero) {
        selectedHero = hero == null ? HeroType.KNIGHT : hero;
    }

    public boolean isSelected(HeroType hero) {
        return selectedHero == hero;
    }
}