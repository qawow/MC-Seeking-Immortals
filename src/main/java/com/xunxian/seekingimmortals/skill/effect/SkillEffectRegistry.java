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

    public static boolean hasEffect(SkillType type) {
        return EFFECTS.containsKey(type);
    }

    static {
        // 法术
        register(SkillType.FIREBALL, new com.xunxian.seekingimmortals.skill.effect.spell.FireballSpell());
        register(SkillType.DETECTION, new com.xunxian.seekingimmortals.skill.effect.spell.DetectionSpell());
        register(SkillType.INVISIBILITY, new com.xunxian.seekingimmortals.skill.effect.spell.InvisibilitySpell());
        register(SkillType.LIGHTNESS_SKILL, new com.xunxian.seekingimmortals.skill.effect.spell.LightBodySpell());
        register(SkillType.EARTH_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.EarthWallSpell());
    }
}
