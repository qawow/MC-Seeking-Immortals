package com.xunxian.seekingimmortals.lore;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.catalog.LoreCatalogService;
import com.xunxian.seekingimmortals.design.SettingCatalogSummary;
import com.xunxian.seekingimmortals.design.SettingCatalogSummaryService;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M16 hub: full-category lore summaries for HUD / tooltip / encyclopedia screens.
 * Read-only over previously shipped module data; never mutates gameplay state.
 */
public final class LoreCompendiumService {
    public enum ScreenKind {
        BESTIARY,
        CHRONICLE,
        COMPENDIUM,
        GLOSSARY,
        NUMERIC
    }

    private LoreCompendiumService() {}

    public record CategorySummary(String id, String display, int count, String hint) {}

    public record HubSummary(
            int glossary,
            int numericPresent,
            int visualPresent,
            int bestiaryTotal,
            int chronicleTotal,
            int timelinePhases,
            int loreCatalogTotal,
            List<CategorySummary> categories) {
        public List<String> lines() {
            List<String> lines = new ArrayList<>();
            lines.add("glossary=" + glossary + " numeric=" + numericPresent + " visual=" + visualPresent);
            lines.add("bestiary=" + bestiaryTotal + " chronicle=" + chronicleTotal + " timeline=" + timelinePhases);
            lines.add("lore_catalog=" + loreCatalogTotal);
            for (CategorySummary category : categories) {
                lines.add(category.display() + ": " + category.count() + " — " + category.hint());
            }
            return lines;
        }
    }

    public static HubSummary hub() {
        LoreCatalogService.Snapshot lore = LoreCatalogService.builtin();
        List<CategorySummary> categories = new ArrayList<>();
        categories.add(new CategorySummary("techniques", "功法术法",
                SettingCatalogSummaryService.categoryCount("techniques"), "M02 techniques / methods"));
        categories.add(new CategorySummary("pills", "丹药",
                SettingCatalogSummaryService.categoryCount("pills"), "alchemy / pill catalogs"));
        categories.add(new CategorySummary("artifacts", "法宝",
                SettingCatalogSummaryService.categoryCount("artifacts"), "M15 artifact corpus"));
        categories.add(new CategorySummary("beasts", "妖兽",
                BeastBestiaryService.size(), "M10 bestiary runtime"));
        categories.add(new CategorySummary("sects", "宗门势力",
                SettingCatalogSummaryService.categoryCount("sects"), "M08 faction graph"));
        categories.add(new CategorySummary("realms", "秘境区域",
                SettingCatalogSummaryService.categoryCount("secret_realms"), "M09 secret realms"));
        categories.add(new CategorySummary("dimensions", "维度飞升",
                lore.dimensions().size() + lore.dimensionRegistry().size() + lore.ascensionStages().size(),
                "M13 dimensions / ascension"));
        categories.add(new CategorySummary("glossary", "术语",
                NameAliasGlossaryService.size(), "name_alias_glossary_v103"));
        categories.add(new CategorySummary("numeric", "数值",
                NumericOverviewService.present() ? 1 : 0, "numeric_overview_v103"));
        categories.add(new CategorySummary("visual", "视觉",
                VisualStyleService.present() ? 1 : 0, "visual_style_v118 / look_cards_v122"));
        categories.add(new CategorySummary("chronicle", "编年",
                TimelineChronicleService.chronicleCount(), "chronicle_events + timeline"));
        return new HubSummary(
                NameAliasGlossaryService.size(),
                NumericOverviewService.present() ? 1 : 0,
                VisualStyleService.present() ? 1 : 0,
                BeastBestiaryService.size(),
                TimelineChronicleService.chronicleCount(),
                TimelineChronicleService.phaseCount(),
                lore.totalEntries(),
                List.copyOf(categories)
        );
    }

    public static Optional<String> tooltipFor(String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return Optional.empty();
        }
        Optional<NameAliasGlossaryService.GlossaryEntry> glossary = NameAliasGlossaryService.find(idOrAlias);
        if (glossary.isPresent()) {
            NameAliasGlossaryService.GlossaryEntry e = glossary.get();
            String aliases = e.aliases().isEmpty() ? "-" : String.join("/", e.aliases());
            return Optional.of(e.primary() + " (" + e.type() + ") [" + aliases + "]");
        }
        Optional<BeastBestiaryService.BeastEntry> beast = BeastBestiaryService.find(idOrAlias);
        if (beast.isPresent()) {
            BeastBestiaryService.BeastEntry b = beast.get();
            return Optional.of(b.display() + " T" + b.tier() + " threat=" + b.threat()
                    + (b.trueSpirit() ? " 真灵" : ""));
        }
        FactionQuestCatalogService.Entry chronicle =
                FactionQuestCatalogService.builtin().chronicleEvents().get(idOrAlias.trim().toLowerCase(Locale.ROOT));
        if (chronicle != null) {
            return Optional.of(chronicle.display() + " [" + chronicle.id() + "]");
        }
        return Optional.empty();
    }

    public static List<String> playerProgressLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        if (player == null) {
            return lines;
        }
        lines.add("bestiary_unlocked=" + BestiaryUnlockService.unlockedCount(player)
                + "/" + BeastBestiaryService.size());
        lines.add("bestiary_kills=" + BestiaryUnlockService.killCount(player)
                + " contracts=" + BestiaryUnlockService.contractCount(player));
        lines.add("chronicle_discovered=" + ChronicleTradeSoftService.discoveredCount(player)
                + "/" + ChronicleTradeSoftService.chronicleCount());
        lines.add("timeline_phases=" + TimelineChronicleService.unlockedPhaseCount(player)
                + "/" + TimelineChronicleService.phaseCount());
        return lines;
    }

    public static Map<String, Integer> categoryCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (CategorySummary category : hub().categories()) {
            map.put(category.id(), category.count());
        }
        return map;
    }

    /** Author-side setting pack summary when {@code 文本材料/data} is present. */
    public static Optional<SettingCatalogSummary> authorSettingSummary() {
        Path dataRoot = Path.of("文本材料", "data");
        if (!Files.exists(dataRoot)) {
            return Optional.empty();
        }
        return Optional.of(SettingCatalogSummaryService.summarize(dataRoot));
    }
}
