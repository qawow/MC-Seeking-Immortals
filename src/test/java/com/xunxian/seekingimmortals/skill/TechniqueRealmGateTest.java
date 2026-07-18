package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class TechniqueRealmGateTest {
    @Test
    void unsupportedCorpusRealmCannotCast() {
        TechniqueDataManager.TechniqueEntry technique = TechniqueDataManager.builtinTechniques()
                .get("time_slow_field");

        assertNull(technique.requiredRealm());
        assertFalse(TechniqueGateService.canCast(null, new PlayerCultivation(), technique, false).allowed());
    }
}
