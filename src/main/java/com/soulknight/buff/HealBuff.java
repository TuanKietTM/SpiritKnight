package com.soulknight.buff;

import com.soulknight.entity.Player;

public class HealBuff extends Buff {

    public HealBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        player.heal(getType().getValue());
    }

    @Override
    protected void onRemove(Player player) {
        // Buff tuc thoi khong can khoi phuc
    }
}