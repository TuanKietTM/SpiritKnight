package com.soulknight.buff.effect;

import com.soulknight.buff.BuffType;

public final class BuffVisualEffectFactory {
    /**
     * class nay quyet dinh loai buff nao tao hieu ung nao co che nay tuong tu voi debuff
     */
    private BuffVisualEffectFactory() {
    }

    public static BuffVisualEffect create(BuffType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case SHIELD -> new ShieldOrbitEffect();
            case SPEED -> new SpeedWindEffect();
            case DAMAGE -> new DamageAuraEffect();
            default -> null;
        };
    }
}