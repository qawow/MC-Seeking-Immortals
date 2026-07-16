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
                    true),
            // Wave462: shop-ready Tiannan expansions.
            new SectDefinition(
                    "huangfeng_valley",
                    "黄枫谷",
                    "Huangfeng Valley",
                    "黄枫入门、外门杂役与筑基起步",
                    "Huangfeng entry, outer labor, and early foundation",
                    "huangfeng_contribution_hall",
                    "黄枫谷执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "yanyue_sect",
                    "掩月宗",
                    "Yanyue Sect",
                    "幻术、影遁与秘传月法",
                    "Illusion, stealth, and moon-path secrets",
                    "yanyue_contribution_hall",
                    "掩月宗执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "luoyun_sect",
                    "落云宗",
                    "Luoyun Sect",
                    "灵焰、符箓与云海行走",
                    "Spirit flame, talismans, and cloud-sea travel",
                    "luoyun_contribution_hall",
                    "落云宗执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "hehuan_sect",
                    "合欢宗",
                    "Hehuan Sect",
                    "双修、魅术与人情交易",
                    "Dual-path arts, charm techniques, and social leverage",
                    "hehuan_contribution_pavilion",
                    "合欢宗执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            // Wave463: shop-ready expansions + content packages.
            new SectDefinition(
                    "star_palace",
                    "星宫",
                    "Star Palace",
                    "乱星海秩序、商税与巡航",
                    "Chaotic Sea order, tax, and patrols",
                    "star_palace_merit_hall",
                    "星宫执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "spirit_beast_mountain",
                    "灵兽山",
                    "Spirit Beast Mountain",
                    "御兽、灵宠与兽核交易",
                    "Beast taming, pets, and core trade",
                    "spirit_beast_mountain_contribution_hall",
                    "灵兽山执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "qianzhu_sect",
                    "千竹教",
                    "Qianzhu Sect",
                    "傀儡机关与竹林秘术",
                    "Puppet mechanisms and bamboo arts",
                    "qianzhu_contribution_hall",
                    "千竹教执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "mulan_fashi_council",
                    "慕兰神师府",
                    "Mulan Fashi Council",
                    "慕兰战阵与法师供奉",
                    "Mulan war arrays and fashi supply",
                    "mulan_fashi_contribution_hall",
                    "慕兰神师府执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            // Wave464: safest remaining tiannan/tianlan packages.
            new SectDefinition(
                    "qingxu_gate",
                    "清虚门",
                    "Qingxu Gate",
                    "符阵清修与虚静道途",
                    "Talisman arrays and quiet dao path",
                    "qingxu_gate_contribution_hall",
                    "清虚门执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "huadao_wu",
                    "化刀坞",
                    "Huadao Dock",
                    "刀道淬炼与锋芒试炼",
                    "Blade path tempering and edge trials",
                    "huadao_wu_contribution_hall",
                    "化刀坞执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "tianque_fort",
                    "天阙堡",
                    "Tianque Fort",
                    "守御关隘与壁垒 hardening",
                    "Border fort defense and wall hardening",
                    "tianque_fort_contribution_hall",
                    "天阙堡执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "giant_sword_gate",
                    "巨剑门",
                    "Giant Sword Gate",
                    "重剑开山与金石之力",
                    "Heavy sword mountain-breaking force",
                    "giant_sword_gate_contribution_hall",
                    "巨剑门执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "qixuan_men",
                    "七玄门",
                    "Qixuan Gate",
                    "凡人起步与七玄传承",
                    "Mortal start and Seven Mysteries legacy",
                    "qixuan_men_contribution_hall",
                    "七玄门执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            new SectDefinition(
                    "tianlan_temple",
                    "天澜圣殿",
                    "Tianlan Temple",
                    "圣兽信仰与天澜战阵",
                    "Holy beast faith and Tianlan war arrays",
                    "tianlan_temple_contribution_hall",
                    "天澜圣殿执事",
                    "seeking_immortals:sect_outpost_generic",
                    SectContributionService.STAGE_KNOCKING,
                    true),
            // Wave465 remaining unique catalog sects.
            new SectDefinition("guiling_gate", "鬼灵门", "Guiling Gate", "鬼道阴修与魂火", "Ghost path and soul-fire", "guiling_gate_contribution_hall", "鬼灵门执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("moyan_gate", "魔焰门", "Moyan Gate", "魔焰攻伐与邪火", "Demonic flame offense", "moyan_gate_contribution_hall", "魔焰门执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("tiansha_sect", "天煞宗", "Tiansha Sect", "煞气刺杀与杀伐", "Killing intent and assassination", "tiansha_sect_contribution_hall", "天煞宗执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("qianhuan_sect", "千幻宗", "Qianhuan Sect", "千变幻术与迷障", "Myriad illusions and mazes", "qianhuan_sect_contribution_hall", "千幻宗执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("tianmo_sect", "天魔宗", "Tianmo Sect", "天魔血脉与魔功", "Heavenly demon bloodline arts", "tianmo_sect_contribution_hall", "天魔宗执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("qingluo_sect", "青罗宗", "Qingluo Sect", "青罗魔罗与北荒", "Qingluo demonic north-waste path", "qingluo_sect_contribution_hall", "青罗宗执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("wanhu_sect", "万狐宗", "Wanhu Sect", "狐媚幻术与万变", "Fox illusions and transformations", "wanhu_sect_contribution_hall", "万狐宗执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("xuewu_sect", "血巫教", "Xuewu Cult", "血祭巫祝与诅咒", "Blood rites and hexes", "xuewu_sect_contribution_hall", "血巫教执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("inverse_star_alliance", "逆星盟", "Inverse Star Alliance", "逆星黑市与走私", "Inverse-star black market and smuggling", "inverse_star_alliance_contribution_hall", "逆星盟执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true),
            new SectDefinition("dajin_buddhist_temple_line", "大晋佛寺一脉", "Dajin Buddhist Line", "佛门金刚与大晋香火", "Buddhist vajra and Dajin temples", "dajin_buddhist_temple_line_contribution_hall", "大晋佛寺执事", "seeking_immortals:sect_outpost_generic", SectContributionService.STAGE_KNOCKING, true)
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
        String normalized = canonicalizeSectId(sectId);
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
            return new ApplyResult(ApplyStatus.NOT_PLAYABLE, definition);
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
            // Wave465: collapse alias catalog ids onto canonical playable sects.
            String canonical = canonicalizeSectId(id);
            if (!canonical.equals(id)) {
                if (map.containsKey(canonical)) {
                    continue; // alias already covered by playable core
                }
                id = canonical;
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
        if (sectId.contains("star_palace")) {
            return "star_palace_merit_hall";
        }
        if (sectId.contains("qianzhu")) {
            return "qianzhu_contribution_hall";
        }
        if (sectId.contains("spirit_beast") || sectId.contains("lingshou")) {
            return "spirit_beast_mountain_contribution_hall";
        }
        if (sectId.contains("mulan")) {
            return "mulan_fashi_contribution_hall";
        }
        if (sectId.contains("qingxu")) {
            return "qingxu_gate_contribution_hall";
        }
        if (sectId.contains("huadao")) {
            return "huadao_wu_contribution_hall";
        }
        if (sectId.contains("tianque")) {
            return "tianque_fort_contribution_hall";
        }
        if (sectId.contains("giant_sword")) {
            return "giant_sword_gate_contribution_hall";
        }
        if (sectId.contains("qixuan")) {
            return "qixuan_men_contribution_hall";
        }
        if (sectId.contains("tianlan")) {
            return "tianlan_temple_contribution_hall";
        }
        if (sectId.contains("guiling") || sectId.contains("ghost_spirit")) {
            return "guiling_gate_contribution_hall";
        }
        if (sectId.contains("moyan")) {
            return "moyan_gate_contribution_hall";
        }
        if (sectId.contains("tiansha")) {
            return "tiansha_sect_contribution_hall";
        }
        if (sectId.contains("qianhuan")) {
            return "qianhuan_sect_contribution_hall";
        }
        if (sectId.contains("tianmo")) {
            return "tianmo_sect_contribution_hall";
        }
        if (sectId.contains("qingluo")) {
            return "qingluo_sect_contribution_hall";
        }
        if (sectId.contains("wanhu")) {
            return "wanhu_sect_contribution_hall";
        }
        if (sectId.contains("xuewu")) {
            return "xuewu_sect_contribution_hall";
        }
        if (sectId.contains("inverse_star")) {
            return "inverse_star_alliance_contribution_hall";
        }
        if (sectId.contains("buddhist") || sectId.contains("dajin_buddhist")) {
            return "dajin_buddhist_temple_line_contribution_hall";
        }
        return "";
    }

    /**
     * Wave465 alias collapse: catalog twins map onto one playable definition.
     */
    public static String canonicalizeSectId(String sectId) {
        String id = normalizeId(sectId);
        return switch (id) {
            case "ghost_spirit_gate", "ghost_spirit", "guiling" -> "guiling_gate";
            case "qianzhu_teach", "qianzhu" -> "qianzhu_sect";
            case "yuling_sect", "yuling_sect_secret", "yuling" -> "yuling_pavilion";
            case "seven_mysteries", "qixuan" -> "qixuan_men";
            default -> id;
        };
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
        NOT_PLAYABLE,
        LOCKED,
        ALREADY_MEMBER,
        OTHER_SECT,
        /** M08/M01 corpus gate or ghost ban rejection. */
        ENTRY_DENIED
    }
}
