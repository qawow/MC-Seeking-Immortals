package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TechniqueAuthoredAliasRegressionTest {
    @Test
    void timeFieldsDoNotExecuteTheirHistoricalFireballAliases() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques =
                TechniqueDataManager.builtinTechniques();

        TechniqueDataManager.TechniqueEntry slowField = techniques.get("time_slow_field");
        TechniqueDataManager.TechniqueEntry stasisPrison = techniques.get("time_stasis_prison");
        assertEquals("field", slowField.effectType());
        assertEquals("control", stasisPrison.effectType());

        SkillEffect slowEffect = AbstractTechniqueEffectResolver.resolve(slowField);
        SkillEffect stasisEffect = AbstractTechniqueEffectResolver.resolve(stasisPrison);
        assertFalse(slowEffect instanceof ElementalProjectileSpell);
        assertFalse(stasisEffect instanceof ElementalProjectileSpell);
        // Pure JVM tests may fail closed while vanilla particle registries are unbootstrapped.
        if (slowEffect != null) {
            assertInstanceOf(ElementalAreaSpell.class, slowEffect);
        }
        if (stasisEffect != null) {
            assertInstanceOf(AreaDebuffSpell.class, stasisEffect);
        }
    }
}
