package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;

public class FireballSpell extends ElementalProjectileSpell {
    public FireballSpell() {
        super(10, 40, 6.0D, CultivationFireballEntity.SpellElement.FIRE,
                "message.seeking_immortals.spell.fireball.success");
    }
}
