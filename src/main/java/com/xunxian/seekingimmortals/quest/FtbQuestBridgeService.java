package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M11: FTB SNBT chapter ↔ native text-chain mapping maintenance.
 * Does not require FTB at class-load time for pure queries; registration stays gated.
 */
public final class FtbQuestBridgeService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private FtbQuestBridgeService() {}

    public record ChapterSeed(String relativePath, String chapterId, String title) {}

    public record Snapshot(List<ChapterSeed> chapters,
                           Map<String, String> chainToChapter,
                           Set<String> registeredChainIds) {
        public int chapterCount() {
            return chapters.size();
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int chapterCount() {
        return BUILTIN.chapterCount();
    }

    public static Optional<String> chapterForChain(String chainId) {
        String id = normalize(chainId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BUILTIN.chainToChapter().get(id));
    }

    /** All 62 chains have a chapter mapping (keyword or explicit). */
    public static boolean allChainsMapped() {
        for (String id : ExtendedCatalogService.builtin().questChains().keySet()) {
            if (chapterForChain(id).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Snapshot loadBuiltin() {
        List<ChapterSeed> chapters = new ArrayList<>();
        // Keep in sync with FtbDefaultPackManifest.FILES chapter list.
        chapters.add(new ChapterSeed("chapters/seeking_immortals_main.snbt",
                "seeking_immortals_main", "寻仙问道：主线任务"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_chaotic_sea.snbt",
                "seeking_immortals_chaotic_sea", "寻仙问道：乱星海与虚天殿"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_dajin_kunwu.snbt",
                "seeking_immortals_dajin_kunwu", "寻仙问道：大晋与昆吾山"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_fallen_demon_yin.snbt",
                "seeking_immortals_fallen_demon_yin", "寻仙问道：坠魔谷与阴司鬼道"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_mulan_demonic.snbt",
                "seeking_immortals_mulan_demonic", "寻仙问道：慕兰天澜与魔道六宗"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_spirit_realm_service.snbt",
                "seeking_immortals_spirit_realm_service", "寻仙问道：灵界天渊与风元百族"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_tiannan_seven_sects.snbt",
                "seeking_immortals_tiannan_seven_sects", "寻仙问道：天南七派与百艺旁支"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_star_palace_inverse.snbt",
                "seeking_immortals_star_palace_inverse", "寻仙问道：星宫派系与逆星暗线"));
        chapters.add(new ChapterSeed("chapters/seeking_immortals_ascension_border.snbt",
                "seeking_immortals_ascension_border", "寻仙问道：飞升边境与终局劫线"));

        Map<String, String> chainToChapter = new LinkedHashMap<>();
        // Every native chain is assigned to the chapter that carries its authored SNBT tag.
        putAll(chainToChapter, "seeking_immortals_main",
                "huangfeng_cultivation_path", "qixuan_mortal_path", "blood_forbidden_campaign");
        putAll(chainToChapter, "seeking_immortals_mulan_demonic",
                "mulan_war_campaign", "mulan_tianlan_war", "mulan_fashi_path",
                "tianlan_defense_line", "wutu_mulan_feud_line", "chain_mulan_war_campaign",
                "demonic_six_path",
                "demonic_six_expanded");
        putAll(chainToChapter, "seeking_immortals_fallen_demon_yin",
                "ghost_path", "yin_luo_ghost_sect", "fallen_demon_campaign",
                "ancient_demon_line", "nether_river_campaign");
        putAll(chainToChapter, "seeking_immortals_chaotic_sea",
                "chaotic_sea_politics", "void_palace_campaign", "inverse_star_recruit",
                "inverse_star_smuggle_arc", "chaotic_sea_civil_war");
        putAll(chainToChapter, "seeking_immortals_dajin_kunwu",
                "dajin_kunwu_line", "kunwu_mountain_campaign",
                "dajin_wanbao_route", "dajin_clan_line", "dajin_righteous_demon_line");
        putAll(chainToChapter, "seeking_immortals_spirit_realm_service",
                "spirit_realm_rise", "tianyuan_merit_path", "chain_tianyuan_enlist", "diyuan_campaign",
                "human_clan_neutral_intro", "spirit_eighteen_clans", "spirit_eighteen_pilgrimage",
                "fengyuan_explorer", "clan_array_mo_line", "clan_refinement_yu_line",
                "clan_alchemy_gu_line", "clan_talisman_ning_line", "human_clan_league_hub",
                "barbarian_kings_line", "barbarian_king_hunt");
        putAll(chainToChapter, "seeking_immortals_tiannan_seven_sects",
                "craft_master", "huadao_blade_path", "giant_sword_gate_path",
                "qianzhu_puppet_path", "yuling_puppet_path", "yanyue_illusion_path",
                "tianfu_talisman_path");
        putAll(chainToChapter, "seeking_immortals_star_palace_inverse",
                "star_palace_internal_politics", "inverse_star_void_heist",
                "chain_void_palace_expedition");
        putAll(chainToChapter, "seeking_immortals_ascension_border",
                "high_realm_endgame", "void_great_cultivation_arc", "diyuan_depth_delve",
                "mortal_to_spirit_bridge", "chain_ascension_spirit_world",
                "yin_cluster_pilgrim", "fallen_demon_expedition", "kunwu_mountain_expedition",
                "spirit_realm_border", "ghost_sect_ban_arc", "chain_seven_sect_outer_to_inner");

        Set<String> registered = new LinkedHashSet<>(chainToChapter.keySet());
        return new Snapshot(List.copyOf(chapters), Collections.unmodifiableMap(chainToChapter),
                Collections.unmodifiableSet(registered));
    }

    private static void putAll(Map<String, String> map, String chapter, String... chainIds) {
        for (String id : chainIds) {
            map.putIfAbsent(normalize(id), chapter);
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
