package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Explicit server-side gates for authored secret-realm opening conditions. */
public final class SecretRealmOpenPolicy {
    static final int DAYS_PER_YEAR = 360;

    private SecretRealmOpenPolicy() {}

    public static Optional<String> validate(ServerPlayer player, SecretRealmCatalogService.RealmDef realm) {
        if (player == null || realm == null) {
            return Optional.of("policy_invalid");
        }
        if (player.getAbilities().instabuild) {
            return Optional.empty();
        }
        String id = normalize(realm.id());
        long day = Math.max(0L, player.serverLevel().getDayTime() / 24000L);
        return switch (id) {
            case "blood_forbidden", "void_palace" -> validateCycleRealm(realm, day);
            case "fallen_demon_valley" -> require(anyFlag(player,
                    "fallen_demon_seal_weaken", "hook_fallen_demon_seal_weaken", "ancient_demon_seal_breach")
                    || activeEvent(player, "ancient_demon_seal_breach", "void_rift_sighting"), "seal_locked");
            case "kunwu_mountain" -> require(anyFlag(player,
                    "cycle_kunwu_open", "kunwu_permit", "hook_kunwu_intel", "hook_kunwu_expedition")
                    || questStarted(player, "kunwu_mountain_expedition", "dajin_kunwu_line"), "quest_locked");
            case "guanghan_realm" -> require(anyFlag(player,
                    "cycle_guanghan_fragment", "guanghan_fragment_complete", "guanghan_realm_open"), "cycle_locked");
            case "demon_gold_mountain" -> require(anyFlag(player,
                    "demon_qi_tide_low", "demon_gold_mountain_open")
                    || activeEvent(player, "evt_demonic_scout", "demonic_qi_eruption"), "tide_locked");
            case "minor_asura_realm" -> require(anyFlag(player,
                    "asura_trial", "asura_trial_invite", "hook_asura_trial")
                    || questStarted(player, "spirit_realm_rise"), "invitation_required");
            case "jiuxian_seclusion" -> require(anyFlag(player,
                    "jiuxian_seclusion_permit", "hook_jiuxian_seclusion_permit", "seclusion_qualified"),
                    "qualification_required");
            case "ancient_cultivator_ruins" -> require(anyFlag(player,
                    "ancient_ruins_open", "hook_kunwu_intel", "hook_ancient_ruin_explore")
                    || activeEvent(player, "ancient_ruins_whisper", "ancient_ruin_whisper"), "event_locked");
            case "wild_ancient_tomb" -> require(isNight(player), "night_required");
            case "thousand_bamboo_puppet_tower" -> require(hasPuppetAccess(player), "puppet_access_required");
            case "wild_ancient_ruins" -> require(anyFlag(player,
                    "wild_ancient_ruins_open", "spatial_rift_stable", "hook_ancient_ruin_explore")
                    || activeEvent(player, "void_rift_sighting", "spatial_rift_storm"), "rift_locked");
            case "tianlan_secret_grotto" -> require(anyFlag(player,
                    "mulan_tianlan_war", "tianlan_secret_grotto_open")
                    || activeEvent(player, "mulan_border_patrol", "mulan_soul_array_supply", "tiannan_war_merit_muster")
                    || questStarted(player, "mulan_tianlan_war", "mulan_war_campaign", "tianlan_defense_line"),
                    "war_locked");
            case "chaotic_sea_abyss_rift" -> require(isNight(player)
                    || anyFlag(player, "abyss_rift_tide", "chaotic_sea_abyss_open"), "stars_required");
            case "spirit_grass_valley" -> require(anyFlag(player,
                    "spirit_grass_valley_open", "ascension_ready", "spirit_realm_node_unlocked")
                    || questStarted(player, "chain_ascension_spirit_world", "spirit_realm_rise"), "node_locked");
            case "yin_mountain_catacomb" -> require(isGhostPath(player)
                    || anyFlag(player, "yin_luo_initiation", "ghost_path", "hook_ghost_yin_body"),
                    "ghost_path_required");
            case "seven_meridian_cave" -> require(anyFlag(player,
                    "seven_meridian_qualified", "outer_sect_tournament_qualified") || hasSect(player),
                    "qualification_required");
            // Nether River and Diyuan already execute their alternate access through
            // realm, region, ticket and ferry/permit checks in the entry transaction.
            case "nether_river_land", "diyuan" -> Optional.empty();
            default -> Optional.empty();
        };
    }

    static Optional<String> validateCycle(String cycleId, List<Integer> windowDays, long absoluteDay) {
        int cycleDays = cycleLengthDays(cycleId);
        if (cycleDays <= 0) {
            return Optional.of("cycle_policy_missing");
        }
        return isCycleWindowOpen(absoluteDay, cycleDays, windowDays)
                ? Optional.empty() : Optional.of("window_closed");
    }

    static int cycleLengthDays(String cycleId) {
        return switch (normalize(cycleId)) {
            case "cycle_blood_forbidden" -> 5 * DAYS_PER_YEAR;
            case "cycle_void_palace" -> 300 * DAYS_PER_YEAR;
            default -> 0;
        };
    }

    static boolean isCycleWindowOpen(long absoluteDay, int cycleDays, List<Integer> windowDays) {
        if (cycleDays <= 0 || windowDays == null || windowDays.isEmpty()) {
            return false;
        }
        int dayOfCycle = (int) Math.floorMod(Math.max(0L, absoluteDay), cycleDays) + 1;
        if (windowDays.size() >= 2) {
            int start = Math.max(1, Math.min(windowDays.get(0), windowDays.get(1)));
            int end = Math.min(cycleDays, Math.max(windowDays.get(0), windowDays.get(1)));
            return dayOfCycle >= start && dayOfCycle <= end;
        }
        return dayOfCycle == Math.max(1, Math.min(cycleDays, windowDays.get(0)));
    }

    private static Optional<String> validateCycleRealm(SecretRealmCatalogService.RealmDef realm, long day) {
        // Entry materials are reserved atomically by SpatialNodeRequiresService before this policy runs.
        return validateCycle(realm.cycleId(), realm.openWindowDays(), day);
    }

    private static Optional<String> require(boolean allowed, String reason) {
        return allowed ? Optional.empty() : Optional.of(reason);
    }

    private static boolean activeEvent(ServerPlayer player, String... eventIds) {
        String active = CultivationHelper.get(player)
                .map(cultivation -> normalize(cultivation.getWorldpackActiveDailyEventId()))
                .orElse("");
        if (active.isBlank()) {
            return false;
        }
        for (String id : eventIds) {
            if (active.equals(normalize(id))) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyFlag(ServerPlayer player, String... flags) {
        for (String flag : flags) {
            if (NpcDialogueFlags.hasFlag(player, flag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean questStarted(ServerPlayer player, String... chainIds) {
        for (String chainId : chainIds) {
            if (TextQuestChainService.find(chainId).isPresent()
                    && TextQuestChainService.progressOf(player, chainId).stage() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPuppetAccess(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            String sect = SectDefinitionService.canonicalizeSectId(
                    cultivation.getSevenMysteriesQuest().getSectId());
            return "thousand_bamboo_sect".equals(sect) || "qianzhu_sect".equals(sect)
                    || cultivation.hasSkill(SkillType.PUPPET_CONTROL)
                    || cultivation.hasSkill(SkillType.PUPPET_CONTROL_BASIC);
        }).orElse(false);
    }

    private static boolean hasSect(ServerPlayer player) {
        return CultivationHelper.get(player)
                .map(cultivation -> !SectDefinitionService.canonicalizeSectId(
                        cultivation.getSevenMysteriesQuest().getSectId()).isBlank())
                .orElse(false);
    }

    private static boolean isGhostPath(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> cultivation.isGhostPath()).orElse(false);
    }

    private static boolean isNight(ServerPlayer player) {
        long time = Math.floorMod(player.serverLevel().getDayTime(), 24000L);
        return time >= 13000L && time <= 23000L;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
