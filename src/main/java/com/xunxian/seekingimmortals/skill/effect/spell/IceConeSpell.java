package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;

public class IceConeSpell extends ElementalProjectileSpell {
    public IceConeSpell() {
        this("message.seeking_immortals.spell.ice_cone.success");
    }

    public IceConeSpell(String successKey) {
        super(10, 40, 5.0D, 1.10D, CultivationFireballEntity.SpellElement.ICE, successKey);
    }
}
