package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.quest.QuestProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SectDefinitionService {
    public static final String LEGACY_SEVEN_MYSTERIES_SECT_ID = "seven_mysteries";
    public static final String CANDIDATE_ROLE = "候选弟子";
    public static final String OUTER_DISCIPLE_ROLE = "外门弟子";
    public static final String INNER_DISCIPLE_ROLE = "内门弟子";

    private static final List<SectDefinition> CORE_DEFINITIONS = List.of(
            new SectDefinition(
                    "qinglan_sect",
                    "青岚宗",
                    "Qinglan Sect",
                    "灵草、炼丹与稳健筑基路线",
                    "Herbs, alchemy, and stable Foundation Establishment",
                    "qinglan_contribution_hall",
                    "青岚宗执事",
                    "seeking_immortals:qinglan_sect_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "lingxiao_sword_sect",
                    "凌霄剑派",
                    "Lingxiao Sword Sect",
                    "金木双系剑修与御剑身法",
                    "Metal/wood sword cultivation and flying-sword movement",
                    "lingxiao_sword_sect_contribution_hall",
                    "凌霄剑派执事",
                    "seeking_immortals:lingxiao_sword_sect_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "danxia_valley",
                    "丹霞谷",
                    "Danxia Valley",
                    "丹道、火候与稀有药材",
                    "Alchemy, fire control, and rare herbs",
                    "danxia_valley_contribution_hall",
                    "丹霞谷执事",
                    "seeking_immortals:danxia_valley_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "yuling_pavilion",
                    "御灵阁",
                    "Yuling Pavilion",
                    "御灵、神识与生灵亲和",
                    "Spirit taming, divine sense, and creature affinity",
                    "yuling_pavilion_contribution_hall",
                    "御灵阁执事",
                    "seeking_immortals:yuling_pavilion_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "wuyue_hall",
                    "五岳堂",
                    "Wuyue Hall",
                    "土行阵法、体修与守御",
                    "Earth arrays, body refinement, and defense",
                    "wuyue_hall_contribution_hall",
                    "五岳堂执事",
                    "seeking_immortals:wuyue_hall_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "cangming_isle",
                    "沧溟岛",
                    "Cangming Isle",
                    "外海商道、水行与秘境行走",
                    "Outer-sea trade, water arts, and secret-realm travel",
                    "cangming_isle_contribution_hall",
                    "沧溟岛执事",
                    "seeking_immortals:cangming_isle_outpost",
                    SectContributionService.STAGE_KNOCKING,
                    true)
    );

    private static final List<SectDefinition> DEFINITIONS = buildDefinitions();

    private SectDefinitionService() {}

    public static List<SectDefinition> definitions() {
        return DEFINITIONS;
    }

    public static List<SectDefinition> playableDefinitions() {
        return DEFINITIONS.stream().filter(SectDefinition::playable).toList();
    }

    public static int catalogSectCount() {
        return ExtendedCatalogService.builtin().sects().size();
    }

    public static Optional<SectDefinition> find(String sectId) {
        String normalized = normalizeId(sectId);
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(normalized))
                .findFirst();
    }

    public static List<SectDefinition> candidates(QuestProgress progress) {
        if (!entryGateOpen(progress)) {
            return List.of();
        }
        String currentSect = progress.getSectId();
        if (currentSect.isBlank() || LEGACY_SEVEN_MYSTERIES_SECT_ID.equals(currentSect)) {
            return playableDefinitions();
        }
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(currentSect))
                .toList();
    }

    public static ApplyResult apply(QuestProgress progress, String sectId) {
        Optional<SectDefinition> definitionOptional = find(sectId);
        if (definitionOptional.isEmpty()) {
            return new ApplyResult(ApplyStatus.UNKNOWN_SECT, null);
        }
        SectDefinition definition = definitionOptional.get();
        if (!definition.playable()) {
            return new ApplyResult(ApplyStatus.UNKNOWN_SECT, definition);
        }
        if (!entryGateOpen(progress)) {
            return new ApplyResult(ApplyStatus.LOCKED, definition);
        }
        String currentSect = progress.getSectId();
        if (definition.id().equals(currentSect)) {
            return new ApplyResult(ApplyStatus.ALREADY_MEMBER, definition);
        }
        if (!currentSect.isBlank() && !LEGACY_SEVEN_MYSTERIES_SECT_ID.equals(currentSect)) {
            return new ApplyResult(ApplyStatus.OTHER_SECT, definition);
        }
        progress.setSect(definition.id(), CANDIDATE_ROLE);
        progress.setSectQuestStage(definition.initialStage());
        progress.addSectFlag(definition.id() + "_applied");
        return new ApplyResult(ApplyStatus.SUCCESS, definition);
    }

    public static boolean entryGateOpen(QuestProgress progress) {
        return progress.isComplete() && progress.hasYueArrived();
    }

    private static List<SectDefinition> buildDefinitions() {
        Map<String, SectDefinition> map = new LinkedHashMap<>();
        for (SectDefinition core : CORE_DEFINITIONS) {
            map.put(core.id(), core);
        }
        for (ExtendedCatalogService.SectEntry entry : ExtendedCatalogService.builtin().sects().values()) {
            String id = normalizeId(entry.id());
            if (id.isBlank() || map.containsKey(id)) {
                continue;
            }
            String display = entry.display() == null || entry.display().isBlank() ? id : entry.display();
            String focus = (entry.specialty() == null || entry.specialty().isBlank())
                    ? (entry.alignment() == null ? "" : entry.alignment())
                    : entry.specialty();
            String shopId = guessShopId(id);
            map.put(id, new SectDefinition(
                    id,
                    display,
                    display,
                    focus,
                    focus,
                    shopId,
                    display + " steward",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    false));
        }
        return List.copyOf(new ArrayList<>(map.values()));
    }

    private static String guessShopId(String sectId) {
        if (sectId.contains("yanyue")) {
            return "yanyue_contribution_hall";
        }
        if (sectId.contains("luoyun")) {
            return "luoyun_contribution_hall";
        }
        if (sectId.contains("huangfeng")) {
            return "huangfeng_contribution_hall";
        }
        if (sectId.contains("hehuan")) {
            return "hehuan_contribution_pavilion";
        }
        return "";
    }

    private static String normalizeId(String sectId) {
        return sectId == null ? "" : sectId.trim().toLowerCase(Locale.ROOT);
    }

    public record SectDefinition(String id, String displayZh, String displayEn, String focusZh, String focusEn,
                                 String shopId, String stewardName, String structureId, int initialStage,
                                 boolean playable) {
        public SectDefinition(String id, String displayZh, String displayEn, String focusZh, String focusEn,
                              String shopId, String stewardName, String structureId, int initialStage) {
            this(id, displayZh, displayEn, focusZh, focusEn, shopId, stewardName, structureId, initialStage, true);
        }
    }

    public record ApplyResult(ApplyStatus status, SectDefinition definition) {
        public boolean success() {
            return status == ApplyStatus.SUCCESS;
        }
    }

    public enum ApplyStatus {
        SUCCESS,
        UNKNOWN_SECT,
        LOCKED,
        ALREADY_MEMBER,
        OTHER_SECT
    }
}
