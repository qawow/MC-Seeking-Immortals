package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.skill.SkillType;
import java.util.HashMap;
import java.util.Map;

public class SkillEffectRegistry {
    private static final Map<SkillType, SkillEffect> EFFECTS = new HashMap<>();

    public static void register(SkillType type, SkillEffect effect) {
        EFFECTS.put(type, effect);
    }

    public static SkillEffect get(SkillType type) {
        return EFFECTS.get(type);
    }

    public static SkillType byDisplayName(String displayName) {
        for (SkillType type : EFFECTS.keySet()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    public static boolean hasEffect(SkillType type) {
        return EFFECTS.containsKey(type);
    }

    static {
        // 法术
        register(SkillType.QI_GUIDING, new com.xunxian.seekingimmortals.skill.effect.spell.QiGuidingPassive());
        register(SkillType.FIREBALL, new com.xunxian.seekingimmortals.skill.effect.spell.FireballSpell());
        register(SkillType.ICE_CONE, new com.xunxian.seekingimmortals.skill.effect.spell.IceConeSpell());
        register(SkillType.THUNDER_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.ThunderStrikeSpell());
        register(SkillType.EARTH_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.EarthEscapeStepSpell());
        register(SkillType.FLYING_SWORD_BEGINNER, new com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordBeginnerSpell());
        register(SkillType.SINGLE_SWORD_THRUST, new com.xunxian.seekingimmortals.skill.effect.spell.SwordProjectileSpell(20, 20, 8.0D, 1));
        register(SkillType.THREE_TALENT_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordProjectileSpell(40, 60, 7.0D, 3));
        register(SkillType.DETECTION, new com.xunxian.seekingimmortals.skill.effect.spell.DetectionSpell());
        register(SkillType.INVISIBILITY, new com.xunxian.seekingimmortals.skill.effect.spell.InvisibilitySpell());
        register(SkillType.LIGHTNESS_SKILL, new com.xunxian.seekingimmortals.skill.effect.spell.LightBodySpell());
        register(SkillType.EARTH_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.EarthWallSpell());
    }
}
