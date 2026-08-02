package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Quest-hook browser + Wave55 authority accept bridge into text quest chains.
 * Wave457: honest empty mapping (no silent huangfeng fallback); expanded hook map.
 * Preview remains soft; accept starts a mapped chain server-side.
 */
public final class QuestHookSoftService {
    private static final Map<String, String> HOOK_TO_CHAIN = buildHookMap();

    private QuestHookSoftService() {}

    public static int hookCount() {
        return FactionQuestCatalogService.builtin().questHooks().size();
    }

    public static List<String> sampleHooks(int limit) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (FactionQuestCatalogService.Entry entry : FactionQuestCatalogService.builtin().questHooks().values()) {
            String mapped = mappedChainId(entry.id()).orElse("-");
            list.add(entry.id() + " | " + entry.display() + " -> " + mapped);
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    public static Optional<String> mappedChainId(String hookId) {
        String id = normalize(hookId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String direct = HOOK_TO_CHAIN.get(id);
        if (direct != null && TextQuestChainService.find(direct).isPresent()) {
            return Optional.of(direct);
        }
        // Fallback: same-id chain first.
        if (TextQuestChainService.find(id).isPresent()) {
            return Optional.of(id);
        }
        // Keyword heuristics for common hook prefixes run before substring matching so
        // broad tokens (sect/void/nether/diyuan) follow the authored intent instead of
        // whichever index row happens to sort first.
        Optional<String> keyword = keywordMatch(id);
        if (keyword.isPresent()) {
            return keyword;
        }
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            String chainId = chain.id();
            if (chainId.length() >= 3 && (id.contains(chainId) || chainId.contains(id))) {
                return Optional.of(chainId);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> keywordMatch(String id) {
        if (id.contains("huangfeng") || id.contains("qixuan") || id.contains("alchemy")) {
            return firstPresent("huangfeng_cultivation_path", "qixuan_mortal_path");
        }
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("wutu")) {
            return firstPresent("mulan_tianlan_war", "mulan_war_campaign", "tianlan_defense_line");
        }
        if (id.contains("ghost") || id.contains("yin") || id.contains("nether")) {
            return firstPresent("ghost_path", "yin_luo_ghost_sect", "yin_cluster_pilgrim");
        }
        if (id.contains("blood") || id.contains("diyuan") || id.contains("void")) {
            return firstPresent("blood_forbidden_campaign", "nether_river_campaign", "diyuan_campaign",
                    "void_palace_campaign");
        }
        if (id.contains("star") || id.contains("chaotic") || id.contains("inverse")) {
            return firstPresent("star_palace_internal_politics", "chaotic_sea_politics");
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("wanbao") || id.contains("sect")) {
            return firstPresent("dajin_kunwu_line", "kunwu_mountain_expedition", "dajin_wanbao_route");
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension")) {
            return firstPresent("spirit_realm_rise", "tianyuan_merit_path", "spirit_realm_border");
        }
        if (id.contains("demon") || id.contains("fallen") || id.contains("ancient")) {
            return firstPresent("ancient_demon_line", "fallen_demon_expedition", "demonic_six_path");
        }
        if (id.contains("craft") || id.contains("refine") || id.contains("talisman") || id.contains("puppet")
                || id.contains("formation")) {
            return firstPresent("craft_master", "tianfu_talisman_path", "qianzhu_puppet_path", "yuling_puppet_path");
        }
        if (id.contains("barbarian") || id.contains("clan") || id.contains("fengyuan") || id.contains("human")) {
            return firstPresent("barbarian_kings_line", "human_clan_neutral_intro", "spirit_eighteen_clans",
                    "human_clan_league_hub");
        }
        return Optional.empty();
    }

    public static boolean preview(ServerPlayer player, String hookId) {
        String id = normalize(hookId);
        FactionQuestCatalogService.Entry entry = FactionQuestCatalogService.builtin().questHooks().get(id);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown",
                    Component.translatable("text.seeking_immortals.unknown_quest")), false);
            return false;
        }
        Optional<String> mapped = mappedChainId(entry.id());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.preview",
                PlayerDisplayText.safeLiteral(entry.id(), "text.seeking_immortals.unknown_quest"),
                hookDisplay(entry)), false);
        if (mapped.isPresent()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.mapped",
                    chainDisplay(mapped.get())), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accept_hint"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.soft_only"), false);
        }
        return true;
    }

    /**
     * Wave55 authority bridge: accept a catalog hook by starting its mapped text quest chain.
     */
    public static boolean accept(ServerPlayer player, String hookId) {
        String id = normalize(hookId);
        FactionQuestCatalogService.Entry entry = FactionQuestCatalogService.builtin().questHooks().get(id);
        if (entry == null) {
            // Still allow direct accept of unknown-but-mappable ids for OP tooling.
            Optional<String> mappedUnknown = mappedChainId(id);
            if (mappedUnknown.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown",
                        Component.translatable("text.seeking_immortals.unknown_quest")), false);
                return false;
            }
            boolean started = TextQuestChainService.start(player, mappedUnknown.get());
            if (started) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accepted",
                        Component.translatable("text.seeking_immortals.unknown_quest"),
                        chainDisplay(mappedUnknown.get())), true);
            }
            return started;
        }
        Optional<String> mapped = mappedChainId(entry.id());
        if (mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.soft_only"), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
        if (progress.stage() > 0) {
            // Wave466: already started — open dialogue / advance instead of hard-fail.
            boolean advanced = false;
            if (!progress.complete()) {
                advanced = TextQuestChainService.advance(player, mapped.get());
                if (!advanced) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.quest_hook.advance_locked", chainDisplay(mapped.get())), false);
                }
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accepted",
                    hookDisplay(entry), chainDisplay(mapped.get())), true);
            TextQuestNpcHookService.openDialogue(player, mapped.get(), false);
            return advanced || progress.complete();
        }
        boolean started = TextQuestChainService.start(player, mapped.get());
        if (started) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accepted",
                    hookDisplay(entry), chainDisplay(mapped.get())), true);
            // Open dialogue so the player has an immediate authority surface.
            TextQuestNpcHookService.openDialogue(player, mapped.get(), false);
        }
        return started;
    }

    private static Component hookDisplay(FactionQuestCatalogService.Entry entry) {
        return entry == null
                ? Component.translatable("text.seeking_immortals.unknown_quest")
                : PlayerDisplayText.safeLiteral(entry.display(), "text.seeking_immortals.unknown_quest");
    }

    private static Component chainDisplay(String chainId) {
        return TextQuestChainService.find(chainId)
                .map(chain -> PlayerDisplayText.safeLiteral(
                        chain.display(), "text.seeking_immortals.unknown_quest"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_quest"));
    }

    private static Optional<String> firstPresent(String... ids) {
        for (String id : ids) {
            if (TextQuestChainService.find(id).isPresent()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> buildHookMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("huangfeng_entry", "huangfeng_cultivation_path");
        map.put("alchemy_apprentice", "huangfeng_cultivation_path");
        map.put("sect_fire_room", "huangfeng_cultivation_path");
        map.put("herb_gather_contract", "huangfeng_cultivation_path");
        map.put("blood_forbidden_run", "ancient_demon_line");
        map.put("void_palace_key", "chaotic_sea_politics");
        map.put("mulan_side", "mulan_tianlan_war");
        map.put("tianlan_side", "tianlan_defense_line");
        map.put("inverse_star_contact", "star_palace_internal_politics");
        map.put("inverse_star_sabotage", "star_palace_internal_politics");
        map.put("kunwu_expedition", "kunwu_mountain_expedition");
        map.put("nether_river_pilgrim", "yin_cluster_pilgrim");
        map.put("star_palace_patrol", "star_palace_internal_politics");
        map.put("beast_tide_defense", "spirit_realm_rise");
        map.put("tianyuan_enlist", "tianyuan_merit_path");
        map.put("diyuan_permit_apply", "spirit_realm_border");
        map.put("auction_invite_dajin", "dajin_kunwu_line");
        map.put("fallen_demon_seal_weaken", "fallen_demon_campaign");
        map.put("ghost_expose_event", "ghost_path");
        map.put("ascension_gate_rumor", "chain_ascension_spirit_world");
        map.put("stolen_jade_slip", "chaotic_sea_politics");
        map.put("pirate_bounty", "chaotic_sea_politics");
        map.put("mulan_scout_clash", "mulan_tianlan_war");
        map.put("yinming_escape", "ghost_path");
        map.put("hook_ghost_yin_body", "ghost_path");
        map.put("hook_ghost_soul_anchor", "ghost_path");
        map.put("hook_ghost_nether_core", "ghost_path");
        map.put("nether_river_guardian", "ghost_path");
        map.put("kunwu_seal_research", "high_realm_endgame");
        map.put("fallen_demon_scout", "high_realm_endgame");
        map.put("king_territory_intrusion", "barbarian_kings_line");
        map.put("ancient_ruin_explore", "spirit_eighteen_clans");
        map.put("demonic_alliance", "ancient_demon_line");
        map.put("tianyuan_demon_contract", "spirit_realm_rise");
        map.put("treasure_fair_invite", "spirit_realm_rise");
        map.put("diyuan_scout", "spirit_realm_rise");
        map.put("asura_trial", "spirit_realm_rise");
        map.put("jiuxian_seclusion_permit", "spirit_realm_rise");
        map.put("puppet_maintain", "craft_master");
        map.put("dajin_auction_preview", "dajin_kunwu_line");
        map.put("kunwu_intel", "dajin_kunwu_line");
        map.put("demon_rift_closure", "ancient_demon_line");
        map.put("qixuan_village_intro", "qixuan_mortal_path");
        map.put("qixuan_herb_errand", "qixuan_mortal_path");
        map.put("qixuan_sect_trial", "qixuan_mortal_path");
        map.put("qixuan_decline_legacy", "qixuan_mortal_path");
        map.put("huadao_entry", "huadao_blade_path");
        map.put("huadao_forge_contract", "huadao_blade_path");
        map.put("huadao_duel_trial", "huadao_blade_path");
        map.put("giant_sword_entry", "giant_sword_gate_path");
        map.put("giant_sword_relic_restore", "giant_sword_gate_path");
        map.put("qianzhu_puppet_apprentice", "qianzhu_puppet_path");
        map.put("thousand_bamboo_tower_run", "qianzhu_puppet_path");
        map.put("dayan_fragment_quest", "qianzhu_puppet_path");
        map.put("yuling_secret_contact", "yuling_puppet_path");
        map.put("beast_soul_bind_puppet", "yuling_puppet_path");
        map.put("lingshou_contract_beast", "yuling_puppet_path");
        map.put("clan_territory_patrol", "dajin_clan_line");
        map.put("clan_ancestor_trial", "dajin_clan_line");
        map.put("demonic_north_rumor", "demonic_six_path");
        map.put("guiling_gate_recruit", "demonic_six_path");
        map.put("hehuan_sect_trial", "demonic_six_path");
        map.put("tianmo_blood_rite", "demonic_six_path");
        map.put("righteous_bounty_hunt", "demonic_six_path");
        map.put("nether_river_fog_event", "yin_cluster_pilgrim");
        map.put("barbarian_border_contact", "barbarian_kings_line");
        map.put("king_tribute_quest", "barbarian_kings_line");
        map.put("spatial_rift_escape", "barbarian_kings_line");
        map.put("clan_generic_tribute", "spirit_eighteen_clans");
        map.put("star_palace_branch_enforcement", "star_palace_internal_politics");
        map.put("star_palace_branch_commerce", "star_palace_internal_politics");
        map.put("internal_tax_vote", "star_palace_internal_politics");
        map.put("tiannan_defense_mobilize", "mulan_war_campaign");
        map.put("fashi_soul_array_battle", "mulan_war_campaign");
        map.put("holy_bird_blessing_quest", "mulan_war_campaign");
        map.put("war_ceasefire_negotiation", "mulan_war_campaign");
        map.put("mulan_fashi_initiation", "mulan_fashi_path");
        map.put("holy_bird_altar_visit", "mulan_fashi_path");
        map.put("tianhu_beast_tame_optional", "mulan_fashi_path");
        map.put("tianlan_oath_renew", "tianlan_defense_line");
        map.put("tianlan_beast_soul_call_training", "tianlan_defense_line");
        map.put("mulan_war_campaign", "tianlan_defense_line");
        map.put("wutu_raid_mulan_camp", "wutu_mulan_feud_line");
        map.put("mulan_counter_hunt", "wutu_mulan_feud_line");
        map.put("feud_truce_broker", "wutu_mulan_feud_line");
        map.put("seal_weak_event", "fallen_demon_expedition");
        map.put("demon_qi_purge_side", "fallen_demon_expedition");
        map.put("ancient_demon_projection_boss", "fallen_demon_expedition");
        map.put("dajin_clan_feud_choice", "dajin_wanbao_route");
        map.put("kunwu_rumor", "dajin_wanbao_route");
        map.put("yin_luo_initiation", "yin_luo_ghost_sect");
        map.put("soul_banner_quest", "yin_luo_ghost_sect");
        map.put("nether_ferry_pass", "yin_luo_ghost_sect");
        map.put("kunwu_map_fragment_turnin", "kunwu_mountain_expedition");
        map.put("kunwu_cold_snap_survive", "kunwu_mountain_expedition");
        map.put("kunwu_puppet_king", "kunwu_mountain_expedition");
        map.put("tianyuan_merit_enlist", "tianyuan_merit_path");
        map.put("demon_beast_siege_defense", "tianyuan_merit_path");
        map.put("wild_land_beast_horde", "spirit_realm_border");
        map.put("spatial_rift_flutter_survive", "spirit_realm_border");
        map.put("righteous_sect_ghost_hunt", "ghost_sect_ban_arc");
        map.put("sect_ban_lift_rare_quest", "ghost_sect_ban_arc");
        map.put("fengyuan_clan_intro", "fengyuan_explorer");
        map.put("spirit_fengyuan_border_patrol", "fengyuan_explorer");
        map.put("wild_land_rumor", "fengyuan_explorer");
        map.put("barbarian_beast_tide_survive", "barbarian_king_hunt");
        map.put("barbarian_king_token_hunt", "barbarian_king_hunt");
        map.put("clan_guest_register", "human_clan_league_hub");
        map.put("dajin_clan_feud", "human_clan_league_hub");
        map.put("spirit_eighteen_pilgrimage", "spirit_eighteen_pilgrimage");
        map.put("void_rift_surge", "void_great_cultivation_arc");
        map.put("diyuan_core_probe", "void_great_cultivation_arc");
        map.put("great_vehicle_insight", "void_great_cultivation_arc");
        map.put("tribulation_cloud_gather", "void_great_cultivation_arc");
        map.put("diyuan_pressure_wave", "diyuan_depth_delve");
        map.put("diyuan_core_crystal_boss", "diyuan_depth_delve");
        map.put("mortal_realm_cap_insight", "mortal_to_spirit_bridge");
        map.put("spirit_realm_gate_voucher", "mortal_to_spirit_bridge");
        map.put("star_palace_patrol_quest", "chaotic_sea_civil_war");
        map.put("inverse_star_ambush_event", "chaotic_sea_civil_war");
        map.put("star_palace_enforcement_raid", "chaotic_sea_civil_war");
        map.put("inverse_star_smuggle_run", "chaotic_sea_civil_war");
        map.put("chaotic_sea_neutral_ending_optional", "chaotic_sea_civil_war");
        map.put("blood_forbidden_rumor", "blood_forbidden_campaign");
        map.put("hook_blood_forbidden_ticket", "blood_forbidden_campaign");
        map.put("blood_forbidden_entry_scar", "blood_forbidden_campaign");
        map.put("blood_forbidden_outer_layer", "blood_forbidden_campaign");
        map.put("blood_forbidden_inner_layer", "blood_forbidden_campaign");
        map.put("blood_jiao_guardian", "blood_forbidden_campaign");
        map.put("void_palace_cycle_notice", "void_palace_campaign");
        map.put("void_key_fragment_hunt", "void_palace_campaign");
        map.put("void_palace_outer_hall", "void_palace_campaign");
        map.put("void_palace_herb_garden", "void_palace_campaign");
        map.put("void_palace_cold_jade_hall", "void_palace_campaign");
        map.put("void_palace_treasury", "void_palace_campaign");
        map.put("demon_qi_purge_prep", "fallen_demon_campaign");
        map.put("fallen_demon_outer_gorge", "fallen_demon_campaign");
        map.put("fallen_demon_rift_layer", "fallen_demon_campaign");
        map.put("ancient_demon_projection", "fallen_demon_campaign");
        map.put("kunwu_open_permit", "kunwu_mountain_campaign");
        map.put("kunwu_outer_array", "kunwu_mountain_campaign");
        map.put("kunwu_mine_tunnel", "kunwu_mountain_campaign");
        map.put("kunwu_puppet_hall", "kunwu_mountain_campaign");
        map.put("kunwu_peak_seal", "kunwu_mountain_campaign");
        map.put("nether_ferry_ticket", "nether_river_campaign");
        map.put("nether_river_fog_entry", "nether_river_campaign");
        map.put("nether_soul_shoal", "nether_river_campaign");
        map.put("nether_ghost_hall", "nether_river_campaign");
        map.put("diyuan_rift_mouth", "diyuan_campaign");
        map.put("diyuan_shallow_mine", "diyuan_campaign");
        map.put("diyuan_pressure_zone", "diyuan_campaign");
        map.put("diyuan_ancient_beast", "diyuan_campaign");
        map.put("dajin_border_rumor", "dajin_righteous_demon_line");
        map.put("dajin_righteous_patrol_hook", "dajin_righteous_demon_line");
        map.put("dajin_demon_infiltrate_hook", "dajin_righteous_demon_line");
        map.put("wanbao_auction_lead", "dajin_righteous_demon_line");
        map.put("yanyue_entry_trial", "yanyue_illusion_path");
        map.put("illusion_array_practice", "yanyue_illusion_path");
        map.put("seven_sect_tournament", "yanyue_illusion_path");
        map.put("inner_veil_secret", "yanyue_illusion_path");
        map.put("tianfu_paper_grind", "tianfu_talisman_path");
        map.put("low_talisman_cert", "tianfu_talisman_path");
        map.put("mid_talisman_inner", "tianfu_talisman_path");
        map.put("high_talisman_secret", "tianfu_talisman_path");
        map.put("sect_branch_choice", "demonic_six_expanded");
        map.put("north_waste_ruin", "demonic_six_expanded");
        return map;
    }

    private static String normalize(String hookId) {
        return hookId == null ? "" : hookId.trim().toLowerCase(Locale.ROOT);
    }
}
