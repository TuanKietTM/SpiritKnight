package com.soulknight.buff;

import com.soulknight.entity.Player;

public class SpeedBuff extends Buff {

    public SpeedBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        player.setBuffSpeedMultiplier(1.0 + getType().getValue());
    }

    @Override
    protected void onRemove(Player player) {
        player.setBuffSpeedMultiplier(1.0);
    }
}