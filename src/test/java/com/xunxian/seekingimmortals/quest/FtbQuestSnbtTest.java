package com.xunxian.seekingimmortals.quest;

import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FtbQuestSnbtTest {
    private static final Path PACKAGED_ROOT = Path.of(
            "src/main/resources/seeking_immortals/ftbquests/quests");
    private static final Path MAIN_CHAPTER = Path.of("chapters/seeking_immortals_main.snbt");
    private static final Path CHAOTIC_SEA_CHAPTER = Path.of("chapters/seeking_immortals_chaotic_sea.snbt");
    private static final Path DAJIN_KUNWU_CHAPTER = Path.of("chapters/seeking_immortals_dajin_kunwu.snbt");
    private static final Path FALLEN_DEMON_YIN_CHAPTER = Path.of("chapters/seeking_immortals_fallen_demon_yin.snbt");
    private static final Path MULAN_DEMONIC_CHAPTER = Path.of("chapters/seeking_immortals_mulan_demonic.snbt");
    private static final Path SPIRIT_REALM_SERVICE_CHAPTER = Path.of(
            "chapters/seeking_immortals_spirit_realm_service.snbt");
    private static final Path TIANNAN_SEVEN_SECTS_CHAPTER = Path.of(
            "chapters/seeking_immortals_tiannan_seven_sects.snbt");
    private static final Path STAR_PALACE_INVERSE_CHAPTER = Path.of(
            "chapters/seeking_immortals_star_palace_inverse.snbt");
    private static final Path ASCENSION_BORDER_CHAPTER = Path.of(
            "chapters/seeking_immortals_ascension_border.snbt");
    private static final Map<Path, ExpectedChapter> EXPECTED_CHAPTERS = Map.of(
            MAIN_CHAPTER, new ExpectedChapter("seeking_immortals_main", "寻仙问道：主线任务", 21),
            CHAOTIC_SEA_CHAPTER, new ExpectedChapter("seeking_immortals_chaotic_sea", "寻仙问道：乱星海与虚天殿", 18),
            DAJIN_KUNWU_CHAPTER, new ExpectedChapter("seeking_immortals_dajin_kunwu", "寻仙问道：大晋与昆吾山", 13),
            FALLEN_DEMON_YIN_CHAPTER, new ExpectedChapter("seeking_immortals_fallen_demon_yin", "寻仙问道：坠魔谷与阴司鬼道", 18),
            MULAN_DEMONIC_CHAPTER, new ExpectedChapter("seeking_immortals_mulan_demonic", "寻仙问道：慕兰天澜与魔道六宗", 25),
            SPIRIT_REALM_SERVICE_CHAPTER, new ExpectedChapter(
                    "seeking_immortals_spirit_realm_service", "寻仙问道：灵界天渊与风元百族", 36),
            TIANNAN_SEVEN_SECTS_CHAPTER, new ExpectedChapter(
                    "seeking_immortals_tiannan_seven_sects", "寻仙问道：天南七派与百艺旁支", 28),
            STAR_PALACE_INVERSE_CHAPTER, new ExpectedChapter(
                    "seeking_immortals_star_palace_inverse", "寻仙问道：星宫派系与逆星暗线", 24),
            ASCENSION_BORDER_CHAPTER, new ExpectedChapter(
                    "seeking_immortals_ascension_border", "寻仙问道：飞升边境与终局劫线", 32)
    );
    private static final Map<Path, Integer> EXPECTED_CHECKMARK_COUNTS = Map.of(
            MAIN_CHAPTER, 0,
            CHAOTIC_SEA_CHAPTER, 0,
            DAJIN_KUNWU_CHAPTER, 0,
            FALLEN_DEMON_YIN_CHAPTER, 0,
            MULAN_DEMONIC_CHAPTER, 0,
            SPIRIT_REALM_SERVICE_CHAPTER, 0,
            TIANNAN_SEVEN_SECTS_CHAPTER, 0,
            STAR_PALACE_INVERSE_CHAPTER, 0,
            ASCENSION_BORDER_CHAPTER, 0
    );
    private static final Map<String, String> EXPECTED_NATIVE_WRITES_BY_QUEST = Map.ofEntries(
            Map.entry("1000000000000101", "si_native_write_qixuan_mortal_path_1"),
            Map.entry("1000000000000102", "si_native_write_qixuan_mortal_path_2"),
            Map.entry("1000000000000103", "si_native_write_qixuan_mortal_path_3"),
            Map.entry("1000000000000104", "si_native_write_qixuan_mortal_path_4"),
            Map.entry("1000000000000201", "si_native_write_huangfeng_cultivation_path_1"),
            Map.entry("1000000000000202", "si_native_write_huangfeng_cultivation_path_2"),
            Map.entry("1000000000000301", "si_native_write_blood_forbidden_campaign_1"),
            Map.entry("1000000000000306", "si_native_write_blood_forbidden_campaign_6"),
            Map.entry("1000000000000401", "si_native_write_spirit_realm_rise_1"),
            Map.entry("1000000000000405", "si_native_write_spirit_realm_rise_5"),
            Map.entry("1000000000000510", "si_native_write_void_palace_campaign_1"),
            Map.entry("1000000000000511", "si_native_write_void_palace_campaign_2"),
            Map.entry("1000000000000514", "si_native_write_void_palace_campaign_5"),
            Map.entry("1000000000000801", "si_native_write_mulan_tianlan_war_1"),
            Map.entry("1000000000000805", "si_native_write_mulan_tianlan_war_2")
    );
    private static final Map<String, String> EXPECTED_NATIVE_MIRRORS_BY_TASK = Map.ofEntries(
            Map.entry("1000000000001203", "si_native_huangfeng_cultivation_path_3"),
            Map.entry("1000000000001204", "si_native_huangfeng_cultivation_path_4"),
            Map.entry("1000000000001205", "si_native_huangfeng_cultivation_path_5"),
            Map.entry("1000000000001302", "si_native_blood_forbidden_campaign_2"),
            Map.entry("1000000000001303", "si_native_blood_forbidden_campaign_3"),
            Map.entry("1000000000001304", "si_native_blood_forbidden_campaign_4"),
            Map.entry("1000000000001305", "si_native_blood_forbidden_campaign_5"),
            Map.entry("1000000000001402", "si_native_spirit_realm_rise_2"),
            Map.entry("1000000000001403", "si_native_spirit_realm_rise_3"),
            Map.entry("1000000000001404", "si_native_spirit_realm_rise_4"),
            Map.entry("1000000000001406", "si_native_spirit_realm_rise_6"),
            Map.entry("1000000000001504", "si_native_chaotic_sea_politics_2"),
            Map.entry("1000000000001506", "si_native_inverse_star_recruit_3"),
            Map.entry("1000000000001508", "si_native_inverse_star_smuggle_arc_3"),
            Map.entry("1000000000001512", "si_native_void_palace_campaign_3"),
            Map.entry("1000000000001513", "si_native_void_palace_campaign_4"),
            Map.entry("1000000000001515", "si_native_void_palace_campaign_6"),
            Map.entry("1000000000001516", "si_native_chaotic_sea_civil_war_1"),
            Map.entry("1000000000001518", "si_native_chaotic_sea_civil_war_5"),
            Map.entry("1000000000001601", "si_native_dajin_righteous_demon_line_1"),
            Map.entry("1000000000001603", "si_native_dajin_clan_line_1"),
            Map.entry("1000000000001604", "si_native_dajin_righteous_demon_line_1"),
            Map.entry("1000000000001606", "si_native_dajin_righteous_demon_line_3"),
            Map.entry("1000000000001701", "si_native_fallen_demon_campaign_1"),
            Map.entry("1000000000001703", "si_native_fallen_demon_campaign_3"),
            Map.entry("1000000000001707", "si_native_fallen_demon_campaign_5"),
            Map.entry("1000000000001802", "si_native_mulan_tianlan_war_1"),
            Map.entry("1000000000001804", "si_native_tianlan_defense_line_1"),
            Map.entry("1000000000001806", "si_native_chain_mulan_war_campaign_2"),
            Map.entry("1000000000001807", "si_native_chain_mulan_war_campaign_3"),
            Map.entry("1000000000001810", "si_native_mulan_fashi_path_1"),
            Map.entry("1000000000001811", "si_native_mulan_fashi_path_2"),
            Map.entry("1000000000001813", "si_native_tianlan_defense_line_1"),
            Map.entry("1000000000001815", "si_native_wutu_mulan_feud_line_1"),
            Map.entry("1000000000001816", "si_native_wutu_mulan_feud_line_2"),
            Map.entry("1000000000001817", "si_native_wutu_mulan_feud_line_3"),
            Map.entry("1000000000001818", "si_native_demonic_six_path_1"),
            Map.entry("1000000000001819", "si_native_demonic_six_path_2"),
            Map.entry("1000000000001821", "si_native_demonic_six_path_3"),
            Map.entry("1000000000001823", "si_native_demonic_six_expanded_4"),
            Map.entry("1000000000001903", "si_native_spirit_realm_rise_1"),
            Map.entry("1000000000001906", "si_native_chain_tianyuan_enlist_2"),
            Map.entry("1000000000001908", "si_native_tianyuan_merit_path_3"),
            Map.entry("1000000000001911", "si_native_human_clan_league_hub_1"),
            Map.entry("1000000000001912", "si_native_human_clan_league_hub_2"),
            Map.entry("1000000000001913", "si_native_clan_array_mo_line_1"),
            Map.entry("1000000000001914", "si_native_clan_refinement_yu_line_1"),
            Map.entry("1000000000001917", "si_native_human_clan_league_hub_3"),
            Map.entry("1000000000001918", "si_native_spirit_eighteen_clans_1"),
            Map.entry("1000000000001919", "si_native_spirit_eighteen_clans_2"),
            Map.entry("1000000000001920", "si_native_spirit_eighteen_clans_2"),
            Map.entry("1000000000001921", "si_native_spirit_eighteen_clans_2"),
            Map.entry("1000000000001922", "si_native_spirit_eighteen_clans_3"),
            Map.entry("1000000000001924", "si_native_spirit_eighteen_clans_3"),
            Map.entry("1000000000001925", "si_native_spirit_eighteen_clans_4"),
            Map.entry("1000000000001928", "si_native_diyuan_campaign_3"),
            Map.entry("1000000000001930", "si_native_diyuan_campaign_5"),
            Map.entry("1000000000001931", "si_native_barbarian_kings_line_2"),
            Map.entry("1000000000001933", "si_native_barbarian_kings_line_3"),
            Map.entry("1000000000001934", "si_native_barbarian_king_hunt_3"),
            Map.entry("1000000000001935", "si_native_barbarian_kings_line_4"),
            Map.entry("1000000000001936", "si_native_barbarian_kings_line_4"),
            Map.entry("1000000000002001", "si_native_chain_seven_sect_outer_to_inner_1"),
            Map.entry("1000000000002002", "si_native_huadao_blade_path_1"),
            Map.entry("1000000000002004", "si_native_huadao_blade_path_3"),
            Map.entry("1000000000002005", "si_native_giant_sword_gate_path_1"),
            Map.entry("1000000000002006", "si_native_giant_sword_gate_path_2"),
            Map.entry("1000000000002009", "si_native_qianzhu_puppet_path_2"),
            Map.entry("1000000000002010", "si_native_qianzhu_puppet_path_3"),
            Map.entry("1000000000002012", "si_native_yuling_puppet_path_1"),
            Map.entry("1000000000002015", "si_native_yanyue_illusion_path_1"),
            Map.entry("1000000000002016", "si_native_yanyue_illusion_path_2"),
            Map.entry("1000000000002017", "si_native_yanyue_illusion_path_3"),
            Map.entry("1000000000002018", "si_native_yanyue_illusion_path_4"),
            Map.entry("1000000000002021", "si_native_tianfu_talisman_path_3"),
            Map.entry("1000000000002022", "si_native_tianfu_talisman_path_4"),
            Map.entry("1000000000002023", "si_native_craft_master_1"),
            Map.entry("1000000000002025", "si_native_craft_master_2"),
            Map.entry("1000000000002026", "si_native_craft_master_3"),
            Map.entry("1000000000002027", "si_native_craft_master_4"),
            Map.entry("1000000000002028", "si_native_craft_master_4"),
            Map.entry("1000000000002101", "si_native_star_palace_internal_politics_1"),
            Map.entry("1000000000002102", "si_native_star_palace_internal_politics_2"),
            Map.entry("1000000000002103", "si_native_star_palace_internal_politics_2"),
            Map.entry("1000000000002105", "si_native_star_palace_internal_politics_2"),
            Map.entry("1000000000002107", "si_native_star_palace_internal_politics_3"),
            Map.entry("1000000000002109", "si_native_star_palace_internal_politics_4"),
            Map.entry("1000000000002110", "si_native_star_palace_internal_politics_4"),
            Map.entry("1000000000002111", "si_native_inverse_star_recruit_1"),
            Map.entry("1000000000002112", "si_native_inverse_star_recruit_2"),
            Map.entry("1000000000002113", "si_native_inverse_star_smuggle_arc_1"),
            Map.entry("1000000000002115", "si_native_inverse_star_smuggle_arc_2"),
            Map.entry("1000000000002116", "si_native_inverse_star_smuggle_arc_2"),
            Map.entry("1000000000002117", "si_native_inverse_star_smuggle_arc_3"),
            Map.entry("1000000000002119", "si_native_chaotic_sea_civil_war_3"),
            Map.entry("1000000000002121", "si_native_inverse_star_void_heist_1"),
            Map.entry("1000000000002123", "si_native_inverse_star_void_heist_3"),
            Map.entry("2000000000002201", "si_native_high_realm_endgame_4"),
            Map.entry("2000000000002202", "si_native_kunwu_mountain_expedition_4"),
            Map.entry("1000000000002203", "si_native_kunwu_mountain_expedition_2"),
            Map.entry("1000000000002204", "si_native_kunwu_mountain_expedition_3"),
            Map.entry("1000000000002205", "si_native_kunwu_mountain_expedition_4"),
            Map.entry("1000000000002206", "si_native_fallen_demon_expedition_4"),
            Map.entry("1000000000002207", "si_native_fallen_demon_expedition_1"),
            Map.entry("1000000000002208", "si_native_fallen_demon_expedition_2"),
            Map.entry("1000000000002209", "si_native_fallen_demon_expedition_4"),
            Map.entry("1000000000002210", "si_native_yin_cluster_pilgrim_3"),
            Map.entry("1000000000002212", "si_native_ghost_sect_ban_arc_3"),
            Map.entry("1000000000002213", "si_native_ghost_sect_ban_arc_1"),
            Map.entry("1000000000002214", "si_native_ghost_sect_ban_arc_2"),
            Map.entry("1000000000002215", "si_native_ghost_sect_ban_arc_3"),
            Map.entry("1000000000002216", "si_native_chain_seven_sect_outer_to_inner_1"),
            Map.entry("1000000000002218", "si_native_chain_seven_sect_outer_to_inner_2"),
            Map.entry("1000000000002219", "si_native_chain_seven_sect_outer_to_inner_3"),
            Map.entry("1000000000002220", "si_native_mortal_to_spirit_bridge_3"),
            Map.entry("1000000000002221", "si_native_chain_ascension_spirit_world_1"),
            Map.entry("1000000000002225", "si_native_spirit_realm_border_2"),
            Map.entry("1000000000002226", "si_native_void_great_cultivation_arc_4"),
            Map.entry("1000000000002227", "si_native_void_great_cultivation_arc_1"),
            Map.entry("1000000000002230", "si_native_void_great_cultivation_arc_3"),
            Map.entry("1000000000002231", "si_native_void_great_cultivation_arc_4"),
            Map.entry("2000000000001808", "si_native_mulan_tianlan_war_3"),
            Map.entry("2000000000002228", "si_native_diyuan_depth_delve_4")
    );
    private static final Map<String, ExpectedItemTask> EXPECTED_ITEM_TASKS = Map.ofEntries(
            Map.entry("1000000000001102", new ExpectedItemTask("seeking_immortals:spirit_grass", 4L)),
            Map.entry("1000000000001103", new ExpectedItemTask("seeking_immortals:ling_gen_test_stone", 1L)),
            Map.entry("1000000000001104", new ExpectedItemTask("seeking_immortals:spirit_stone_shard", 1L)),
            Map.entry("1000000000001202", new ExpectedItemTask("seeking_immortals:fasting_pill_low", 1L)),
            Map.entry("1000000000001501", new ExpectedItemTask("seeking_immortals:wind_feather_raft_ticket", 1L)),
            Map.entry("1000000000001502", new ExpectedItemTask("seeking_immortals:pearl_raw", 1L)),
            Map.entry("1000000000001503", new ExpectedItemTask("seeking_immortals:star_palace_tax_receipt", 1L)),
            Map.entry("1000000000001505", new ExpectedItemTask("seeking_immortals:jade_slip_blank", 1L)),
            Map.entry("1000000000001507", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000001509", new ExpectedItemTask("seeking_immortals:pearl_raw", 2L)),
            Map.entry("1000000000001511", new ExpectedItemTask("seeking_immortals:void_crystal", 1L)),
            Map.entry("1000000000001514", new ExpectedItemTask("seeking_immortals:cold_jade", 2L)),
            Map.entry("1000000000001702", new ExpectedItemTask("seeking_immortals:fire_talisman", 1L)),
            Map.entry("1000000000001704", new ExpectedItemTask("seeking_immortals:void_crystal", 1L)),
            Map.entry("1000000000001706", new ExpectedItemTask("seeking_immortals:demon_suppress_talisman_blank", 1L)),
            Map.entry("1000000000001709", new ExpectedItemTask("seeking_immortals:yin_stone", 8L)),
            Map.entry("1000000000001710", new ExpectedItemTask("seeking_immortals:yin_stone", 8L)),
            Map.entry("1000000000001711", new ExpectedItemTask("seeking_immortals:soul_fragment", 1L)),
            Map.entry("1000000000001712", new ExpectedItemTask("seeking_immortals:yin_stone", 30L)),
            Map.entry("1000000000001713", new ExpectedItemTask("seeking_immortals:yin_body_protection_charm", 1L)),
            Map.entry("1000000000001714", new ExpectedItemTask("seeking_immortals:soul_gathering_stone", 1L)),
            Map.entry("1000000000001715", new ExpectedItemTask("seeking_immortals:soul_gathering_stone", 1L)),
            Map.entry("1000000000001716", new ExpectedItemTask("seeking_immortals:soul_fragment", 2L)),
            Map.entry("1000000000001602", new ExpectedItemTask("seeking_immortals:immortal_jade", 1L)),
            Map.entry("1000000000001607", new ExpectedItemTask("seeking_immortals:jade_slip_blank", 1L)),
            Map.entry("1000000000001608", new ExpectedItemTask("seeking_immortals:cold_jade", 1L)),
            Map.entry("1000000000001609", new ExpectedItemTask("seeking_immortals:spirit_gathering_array", 1L)),
            Map.entry("1000000000001610", new ExpectedItemTask("seeking_immortals:kunwu_copper", 4L)),
            Map.entry("1000000000001611", new ExpectedItemTask("seeking_immortals:puppet_core_blank", 1L)),
            Map.entry("1000000000001613", new ExpectedItemTask("seeking_immortals:demon_suppress_talisman_blank", 1L)),
            Map.entry("1000000000001809", new ExpectedItemTask("seeking_immortals:war_contribution_token", 1L)),
            Map.entry("1000000000001812", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000001814", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000001820", new ExpectedItemTask("seeking_immortals:soul_fragment", 1L)),
            Map.entry("1000000000001822", new ExpectedItemTask("seeking_immortals:demonic_blood_coral", 1L)),
            Map.entry("1000000000001825", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000001904", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000001905", new ExpectedItemTask("seeking_immortals:alliance_merit_token", 1L)),
            Map.entry("1000000000001909", new ExpectedItemTask("seeking_immortals:alliance_merit_token", 1L)),
            Map.entry("1000000000001915", new ExpectedItemTask("seeking_immortals:fengyuan_clan_ginseng", 1L)),
            Map.entry("1000000000001916", new ExpectedItemTask("seeking_immortals:talisman_paper_mortal", 3L)),
            Map.entry("1000000000001926", new ExpectedItemTask("seeking_immortals:diyuan_permit", 1L)),
            Map.entry("1000000000001929", new ExpectedItemTask("seeking_immortals:pressure_resist_charm", 1L)),
            Map.entry("1000000000001932", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002104", new ExpectedItemTask("seeking_immortals:fire_talisman", 1L)),
            Map.entry("1000000000002106", new ExpectedItemTask("seeking_immortals:star_palace_tax_receipt", 1L)),
            Map.entry("1000000000002108", new ExpectedItemTask("seeking_immortals:immortal_jade", 1L)),
            Map.entry("1000000000002114", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002118", new ExpectedItemTask("seeking_immortals:immortal_jade", 1L)),
            Map.entry("1000000000002120", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002122", new ExpectedItemTask("seeking_immortals:immortal_jade", 1L)),
            Map.entry("1000000000002124", new ExpectedItemTask("seeking_immortals:void_crystal", 1L)),
            Map.entry("1000000000002003", new ExpectedItemTask("seeking_immortals:spirit_iron", 2L)),
            Map.entry("1000000000002007", new ExpectedItemTask("seeking_immortals:spirit_iron", 4L)),
            Map.entry("1000000000002008", new ExpectedItemTask("seeking_immortals:puppet_core_blank", 1L)),
            Map.entry("1000000000002011", new ExpectedItemTask("seeking_immortals:ironwood", 4L)),
            Map.entry("1000000000002013", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002014", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002019", new ExpectedItemTask("seeking_immortals:talisman_paper_mortal", 3L)),
            Map.entry("1000000000002020", new ExpectedItemTask("seeking_immortals:fire_talisman", 1L)),
            Map.entry("1000000000002024", new ExpectedItemTask("seeking_immortals:alchemy_formula_fasting_pill_paper", 1L)),
            Map.entry("1000000000002222", new ExpectedItemTask("seeking_immortals:alliance_merit_token", 1L)),
            Map.entry("1000000000002223", new ExpectedItemTask("seeking_immortals:wind_feather_raft_ticket", 1L)),
            Map.entry("1000000000002224", new ExpectedItemTask("seeking_immortals:beast_core", 1L)),
            Map.entry("1000000000002228", new ExpectedItemTask("seeking_immortals:diyuan_permit", 1L)),
            Map.entry("1000000000002229", new ExpectedItemTask("seeking_immortals:pressure_resist_charm", 1L))
    );

    @Test
    void packagedFtbQuestDefaultsParseAndReferenceValidDependencies() throws IOException {
        assertTrue(Files.exists(PACKAGED_ROOT.resolve("data.snbt")), "Missing FTB data.snbt");
        try (var paths = Files.walk(PACKAGED_ROOT.resolve("chapters"))) {
            List<Path> chapterFiles = paths
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".snbt"))
                    .toList();
            assertEquals(EXPECTED_CHAPTERS.size(), chapterFiles.size(), "Unexpected packaged FTB chapter file count");
        }

        SNBTCompoundTag data = SNBT.read(PACKAGED_ROOT.resolve("data.snbt"));
        assertNotNull(data, "FTB data.snbt did not parse");
        assertEquals(13, data.getInt("version"));
        assertEquals("linear", data.getString("progression_mode"));

        Map<Path, SNBTCompoundTag> chapters = new HashMap<>();
        Set<String> questIds = new HashSet<>();
        Set<String> taskIds = new HashSet<>();
        Set<String> itemTaskIds = new HashSet<>();
        Set<String> killTaskIds = new HashSet<>();
        Set<String> advancementTaskIds = new HashSet<>();
        Set<String> dimensionTaskIds = new HashSet<>();
        Set<String> customTaskIds = new HashSet<>();
        Map<String, String> nativeMirrorTagsByTask = new HashMap<>();
        Map<String, String> nativeWriteTagsByQuest = new HashMap<>();
        int totalQuests = 0;
        for (Map.Entry<Path, ExpectedChapter> entry : EXPECTED_CHAPTERS.entrySet()) {
            Path chapterPath = PACKAGED_ROOT.resolve(entry.getKey());
            assertTrue(Files.exists(chapterPath), "Missing FTB chapter " + entry.getKey());

            SNBTCompoundTag chapter = SNBT.read(chapterPath);
            ExpectedChapter expected = entry.getValue();
            assertNotNull(chapter, "FTB chapter SNBT did not parse: " + entry.getKey());
            assertEquals(expected.filename(), chapter.getString("filename"));
            assertEquals(expected.title(), chapter.getString("title"));

            ListTag quests = chapter.getList("quests", Tag.TAG_COMPOUND);
            assertEquals(expected.questCount(), quests.size(),
                    "Unexpected quest count for " + expected.filename());
            totalQuests += quests.size();
            chapters.put(entry.getKey(), chapter);
            int checkmarkCount = 0;

            for (Tag questTag : quests) {
                CompoundTag quest = (CompoundTag) questTag;
                String questId = quest.getString("id");
                assertFalse(questId.isBlank(), "Quest id must not be blank");
                assertTrue(questIds.add(questId), "Duplicate quest id " + questId);
                assertFalse(quest.getString("title").isBlank(), "Quest title must not be blank for " + questId);

                for (Tag tag : quest.getList("tags", Tag.TAG_STRING)) {
                    String value = tag.getAsString();
                    if (value.startsWith(FtbNativeQuestSync.WRITE_PREFIX)) {
                        assertTrue(FtbNativeQuestSync.parseWriteTag(value).isPresent(),
                                "Invalid native write tag on quest " + questId + ": " + value);
                        assertFalse(nativeWriteTagsByQuest.containsKey(questId),
                                "Quest must not write more than one native stage: " + questId);
                        nativeWriteTagsByQuest.put(questId, value);
                    }
                }

                ListTag tasks = quest.getList("tasks", Tag.TAG_COMPOUND);
                assertFalse(tasks.isEmpty(), "Quest must have at least one task: " + questId);
                for (Tag taskTag : tasks) {
                    CompoundTag task = (CompoundTag) taskTag;
                    String taskId = task.getString("id");
                    String taskType = task.getString("type");
                    if ("item".equals(taskType)) {
                        assertItemTask(task);
                        itemTaskIds.add(taskId);
                    } else if ("kill".equals(taskType)) {
                        // Wave484: broader FTB kill tasks for combat nodes.
                        assertKillTask(task);
                        killTaskIds.add(taskId);
                    } else if ("advancement".equals(taskType)) {
                        // Wave485: broader FTB advancement tasks for intro/travel nodes.
                        assertAdvancementTask(task);
                        advancementTaskIds.add(taskId);
                    } else if ("dimension".equals(taskType)) {
                        // Wave487: broader FTB dimension tasks for realm/travel nodes.
                        assertDimensionTask(task);
                        dimensionTaskIds.add(taskId);
                    } else if ("custom".equals(taskType)) {
                        // Wave488: custom tasks bound by SI tags via CustomTaskEvent hooks.
                        assertCustomTask(task);
                        customTaskIds.add(taskId);
                        for (Tag tag : task.getList("tags", Tag.TAG_STRING)) {
                            String value = tag.getAsString();
                            if (value.startsWith(FtbNativeQuestSync.MIRROR_PREFIX)
                                    && !value.startsWith(FtbNativeQuestSync.WRITE_PREFIX)) {
                                assertTrue(FtbNativeQuestSync.parseMirrorTag(value).isPresent(),
                                        "Invalid native mirror tag on task " + taskId + ": " + value);
                                assertFalse(nativeMirrorTagsByTask.containsKey(taskId),
                                        "Task must not mirror more than one native stage: " + taskId);
                                nativeMirrorTagsByTask.put(taskId, value);
                            }
                        }
                    } else {
                        checkmarkCount++;
                        fail("Unsupported/manual FTB task type for " + taskId + ": " + taskType);
                    }
                    assertTrue(taskIds.add(taskId), "Duplicate task id " + taskId);
                }
            }
            assertEquals(EXPECTED_CHECKMARK_COUNTS.get(entry.getKey()), checkmarkCount,
                    "Unexpected remaining checkmark count for " + expected.filename());
        }
        assertEquals(215, totalQuests, "Packaged FTB quest defaults should expose 215 nodes");
        assertEquals(EXPECTED_ITEM_TASKS.keySet(), itemTaskIds,
                "Expected implemented FTB item-task bridge coverage");
        // Wave482: every packaged item task is a consuming authority check.
        assertEquals(EXPECTED_ITEM_TASKS.size(), itemTaskIds.size());
        // Wave484: combat nodes upgraded to kill tasks.
        assertTrue(killTaskIds.size() >= 5, "Expected packaged kill tasks, got " + killTaskIds.size());
        assertTrue(killTaskIds.contains("1000000000001306"));
        assertTrue(killTaskIds.contains("1000000000001705"));
        assertTrue(killTaskIds.contains("1000000000001612"));
        // Wave485: intro/travel nodes upgraded to advancement tasks.
        assertTrue(advancementTaskIds.size() >= 6, "Expected packaged advancement tasks, got " + advancementTaskIds.size());
        assertTrue(advancementTaskIds.contains("1000000000001101"));
        assertTrue(advancementTaskIds.contains("1000000000001401"));
        assertTrue(advancementTaskIds.contains("1000000000001901"));
        assertTrue(advancementTaskIds.contains("1000000000002201"));
        assertTrue(advancementTaskIds.contains("1000000000002202"));
        // Wave487: realm/travel nodes upgraded to dimension tasks.
        assertTrue(dimensionTaskIds.size() >= 6, "Expected packaged dimension tasks, got " + dimensionTaskIds.size());
        assertTrue(dimensionTaskIds.contains("1000000000001910"));
        assertTrue(dimensionTaskIds.contains("1000000000001708"));
        assertTrue(dimensionTaskIds.contains("1000000000001405"));
        assertTrue(dimensionTaskIds.contains("1000000000002232"));
        // Wave488: war/reputation custom tasks bound via CustomTaskEvent.
        assertTrue(customTaskIds.size() >= 6, "Expected packaged custom tasks, got " + customTaskIds.size());
        assertTrue(customTaskIds.contains("1000000000001801"));
        assertTrue(customTaskIds.contains("1000000000001808"));
        assertTrue(customTaskIds.contains("1000000000002217"));
        assertTrue(customTaskIds.contains("1000000000001605"));
        assertEquals(EXPECTED_NATIVE_MIRRORS_BY_TASK, nativeMirrorTagsByTask,
                "Every native->FTB mirror mapping must remain explicit");
        assertEquals(EXPECTED_NATIVE_WRITES_BY_QUEST, nativeWriteTagsByQuest,
                "Every FTB->native write mapping must remain explicit");

        // Wave482/483: every packaged item task quest ships an item reward after consumption.
        int rewardedItemQuests = 0;
        for (SNBTCompoundTag chapter : chapters.values()) {
            for (Tag questTag : chapter.getList("quests", Tag.TAG_COMPOUND)) {
                CompoundTag quest = (CompoundTag) questTag;
                boolean hasItemTask = false;
                for (Tag taskTag : quest.getList("tasks", Tag.TAG_COMPOUND)) {
                    if ("item".equals(((CompoundTag) taskTag).getString("type"))) {
                        hasItemTask = true;
                        break;
                    }
                }
                if (!hasItemTask) {
                    continue;
                }
                ListTag rewards = quest.getList("rewards", Tag.TAG_COMPOUND);
                assertFalse(rewards.isEmpty(), "Item-task quest missing rewards: " + quest.getString("id"));
                boolean hasItemReward = false;
                for (Tag rewardTag : rewards) {
                    CompoundTag reward = (CompoundTag) rewardTag;
                    if ("item".equals(reward.getString("type"))) {
                        assertFalse(reward.getString("item").isBlank(), "Reward item blank for " + quest.getString("id"));
                        assertTrue((reward.contains("count") ? reward.getLong("count") : 1L) >= 1L);
                        hasItemReward = true;
                        break;
                    }
                }
                assertTrue(hasItemReward, "Item-task quest missing item reward: " + quest.getString("id"));
                rewardedItemQuests++;
            }
        }
        assertEquals(EXPECTED_ITEM_TASKS.size(), rewardedItemQuests,
                "Every packaged item task should ship an item reward");

        for (SNBTCompoundTag chapter : chapters.values()) {
            for (Tag questTag : chapter.getList("quests", Tag.TAG_COMPOUND)) {
                CompoundTag quest = (CompoundTag) questTag;
                ListTag dependencies = quest.getList("dependencies", Tag.TAG_STRING);
                for (Tag dependency : dependencies) {
                    assertTrue(questIds.contains(dependency.getAsString()),
                            "Missing dependency " + dependency.getAsString() + " for quest " + quest.getString("id"));
                }
            }
        }
    }

    private static void assertItemTask(CompoundTag task) {
        String taskId = task.getString("id");
        ExpectedItemTask expected = EXPECTED_ITEM_TASKS.get(taskId);
        assertNotNull(expected, "Unexpected item task " + taskId);
        assertEquals(expected.item(), task.getString("item"), "Unexpected item for task " + taskId);
        long count = task.contains("count") ? task.getLong("count") : 1L;
        assertEquals(expected.count(), count, "Unexpected item count for task " + taskId);
        // Wave482: packaged item tasks are consuming authority checks (交验/缴纳).
        assertTrue(task.contains("consume_items"), "Item task must explicitly declare consume_items: " + taskId);
        assertTrue(task.getBoolean("consume_items"), "Item task must consume resources for authority: " + taskId);
        assertTrue(task.contains("match_nbt"), "Item task must explicitly ignore NBT: " + taskId);
        assertFalse(task.getBoolean("match_nbt"), "Item task must ignore NBT for broad inventory checks: " + taskId);
    }

    private static void assertKillTask(CompoundTag task) {
        String taskId = task.getString("id");
        assertFalse(task.getString("entity").isBlank(), "Kill task missing entity: " + taskId);
        long value = task.contains("value") ? task.getLong("value") : 1L;
        assertTrue(value >= 1L, "Kill task value must be >= 1: " + taskId);
        assertFalse(task.getString("title").isBlank(), "Kill task missing title: " + taskId);
    }

    private static void assertAdvancementTask(CompoundTag task) {
        String taskId = task.getString("id");
        assertFalse(task.getString("advancement").isBlank(), "Advancement task missing advancement id: " + taskId);
        assertFalse(task.getString("title").isBlank(), "Advancement task missing title: " + taskId);
    }

    private static void assertDimensionTask(CompoundTag task) {
        String taskId = task.getString("id");
        String dimension = task.getString("dimension");
        assertFalse(dimension.isBlank(), "Dimension task missing dimension id: " + taskId);
        assertTrue(dimension.contains(":"), "Dimension id should be namespaced: " + taskId + " -> " + dimension);
        assertFalse(task.getString("title").isBlank(), "Dimension task missing title: " + taskId);
    }

    private static void assertCustomTask(CompoundTag task) {
        String taskId = task.getString("id");
        assertFalse(task.getString("title").isBlank(), "Custom task missing title: " + taskId);
        assertTrue(task.contains("tags"), "Custom task must declare SI tags: " + taskId);
        ListTag tags = task.getList("tags", Tag.TAG_STRING);
        assertFalse(tags.isEmpty(), "Custom task tags empty: " + taskId);
        boolean hasSiTag = false;
        for (Tag tag : tags) {
            String value = tag.getAsString();
            if (value.startsWith("si_")) {
                hasSiTag = true;
                assertTrue(value.matches("^[a-z0-9_]+$"), "Invalid FTB tag chars: " + value);
                FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag(value);
                assertNotNull(spec, "SI tag must parse: " + value);
                assertFalse(spec instanceof FtbCustomTaskHooks.Spec.Unknown,
                        "Unknown SI tag must not ship in packaged task: " + value);
            }
        }
        assertTrue(hasSiTag, "Custom task missing si_* authority tag: " + taskId);
    }

    private record ExpectedChapter(String filename, String title, int questCount) {
    }

    private record ExpectedItemTask(String item, long count) {
    }
}
