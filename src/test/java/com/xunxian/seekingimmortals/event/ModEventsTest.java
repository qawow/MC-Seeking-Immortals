package com.xunxian.seekingimmortals.event;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModEventsTest {
    @Test
    void techniqueGradeMultiplierMatchesChineseSourceTokens() {
        assertEquals(1.60D, ModEvents.getTechniqueGradeMultiplier(technique("spirit_transformation_manual", "化神灵界通天灵宝")), 0.0001D);
        assertEquals(1.45D, ModEvents.getTechniqueGradeMultiplier(technique("nascent_soul_manual", "元婴高级古宝")), 0.0001D);
        assertEquals(1.30D, ModEvents.getTechniqueGradeMultiplier(technique("core_formation_manual", "结丹金丹剑诀")), 0.0001D);
        assertEquals(1.18D, ModEvents.getTechniqueGradeMultiplier(technique("foundation_manual", "筑基中阶阵法")), 0.0001D);
    }

    private static TechniqueDataManager.TechniqueEntry technique(String id, String source) {
        return new TechniqueDataManager.TechniqueEntry(id, "", source, "", 0, 0, Realm.QI_REFINING);
    }
}
