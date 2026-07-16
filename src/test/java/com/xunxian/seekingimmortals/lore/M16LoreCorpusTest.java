package com.xunxian.seekingimmortals.lore;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.design.SettingCatalogSummary;
import com.xunxian.seekingimmortals.design.SettingCatalogSummaryService;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class M16LoreCorpusTest {
    @Test
    void glossaryLoadsShippedCorpus() {
        assertTrue(NameAliasGlossaryService.size() >= 100, "glossary should load v103 entries");
        assertTrue(NameAliasGlossaryService.find("palm_bottle").isPresent());
        assertTrue(NameAliasGlossaryService.find("掌天瓶").isPresent()
                || NameAliasGlossaryService.find("小瓶").isPresent());
    }

    @Test
    void numericAndVisualPresent() {
        assertTrue(NumericOverviewService.present());
        assertFalse(NumericOverviewService.builtin().currency().isEmpty());
        assertTrue(VisualStyleService.present());
        assertFalse(VisualStyleService.builtin().palette().isEmpty());
    }

    @Test
    void hubSummarizesCategories() {
        LoreCompendiumService.HubSummary hub = LoreCompendiumService.hub();
        assertTrue(hub.bestiaryTotal() > 0);
        assertTrue(hub.chronicleTotal() >= TimelineChronicleService.chronicleCount()
                || hub.chronicleTotal() > 0);
        assertTrue(hub.glossary() >= 100);
        assertFalse(hub.categories().isEmpty());
        assertTrue(LoreCompendiumService.tooltipFor("palm_bottle").isPresent()
                || LoreCompendiumService.tooltipFor("掌天瓶").isPresent());
    }

    @Test
    void settingSummaryCategoryCountsIncludeNewCatalogs() {
        Path dataRoot = Path.of("文本材料", "data");
        assumeTrue(Files.exists(dataRoot), "author corpus present");
        SettingCatalogSummary summary = SettingCatalogSummaryService.summarize(dataRoot);
        assertEquals(747, summary.declaredTechniqueCount());
        assertTrue(summary.categoryCount("glossary") >= 100);
        assertTrue(summary.categoryCount("numeric") >= 1);
        assertTrue(summary.find("name_alias_glossary_v103.json").isPresent());
        assertTrue(summary.find("numeric_overview_v103.json").orElseThrow().present());
    }

    @Test
    void shippedCategoryCountsArePositiveForCoreSystems() {
        assertTrue(SettingCatalogSummaryService.categoryCount("beasts") > 0
                || BeastBestiaryService.size() > 0);
        assertTrue(SettingCatalogSummaryService.categoryCount("glossary") >= 100);
        assertTrue(SettingCatalogSummaryService.categoryCount("numeric") >= 1);
        assertTrue(SettingCatalogSummaryService.categoryCount("visual") >= 1);
        assertTrue(SettingCatalogSummaryService.categoryCount("chronicle") > 0);
    }
}
