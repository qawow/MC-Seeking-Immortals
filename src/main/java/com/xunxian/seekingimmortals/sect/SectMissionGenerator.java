package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sect mission generator (Wave52 Phase10 depth).
 * Wave489: kill/escort use server-authoritative player progress counters;
 * beast/formation mission flavors practice special/life skills.
 */
public final class SectMissionGenerator {
    private static final String PROGRESS_ROOT = "seeking_immortals_sect_mission_progress";
    private static final String ACTIVE_ROOT = "seeking_immortals_active_generated_mission";

    private SectMissionGenerator() {}

    public record Mission(String id, String type, String target, int count, int rewardContribution) {}

    public static Mission generate(String sectId) {
        int roll = ThreadLocalRandom.current().nextInt(5);
        String sid = sectId == null ? "qinglan" : sectId.toLowerCase(Locale.ROOT);
        return switch (roll) {
            case 0 -> new Mission(sid + "_gather_grass", "gather", "spirit_grass", 8, 15);
            // Wave491: typed kill targets (zombie/skeleton/spider/any monster).
            case 1 -> new Mission(sid + "_kill_hostiles", "kill", pickKillTarget(), 5, 20);
            case 2 -> new Mission(sid + "_escort_proxy", "escort", "steward_marker", 1, 25);
            case 3 -> new Mission(sid + "_beast_patrol", "beast", "contract", 1, 22);
            default -> new Mission(sid + "_formation_drill", "formation", "array", 1, 18);
        };
    }

    private static String pickKillTarget() {
        int roll = ThreadLocalRandom.current().nextInt(4);
        return switch (roll) {
            case 0 -> "zombie";
            case 1 -> "skeleton";
            case 2 -> "spider";
            default -> "monster";
        };
    }

    /** Wave490: persist full generated mission payload for steward turn-in. */
    public static boolean acceptGenerated(ServerPlayer player, Mission mission) {
        if (player == null || mission == null) {
            return false;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", mission.id() == null ? "" : mission.id());
        tag.putString("type", mission.type() == null ? "" : mission.type());
        tag.putString("target", mission.target() == null ? "" : mission.target());
        tag.putInt("count", Math.max(1, mission.count()));
        tag.putInt("reward", Math.max(0, mission.rewardContribution()));
        // Reset type-specific progress counters on accept.
        CompoundTag root = player.getPersistentData().getCompound(PROGRESS_ROOT).copy();
        if ("kill".equalsIgnoreCase(mission.type())) {
            root.putInt("kills", 0);
            root.putString("kill_target", mission.target() == null ? "monster" : mission.target());
        }
        if ("escort".equalsIgnoreCase(mission.type())) {
            root.putBoolean("escort", false);
            if (!EscortMissionService.startEscort(player)) {
                clearGenerated(player);
                return false;
            }
        } else {
            EscortMissionService.clearEscort(player, true);
        }
        player.getPersistentData().put(ACTIVE_ROOT, tag);
        player.getPersistentData().put(PROGRESS_ROOT, root);
        return true;
    }

    public static Mission activeGenerated(ServerPlayer player) {
        if (player == null || !player.getPersistentData().contains(ACTIVE_ROOT)) {
            return null;
        }
        CompoundTag tag = player.getPersistentData().getCompound(ACTIVE_ROOT);
        String id = tag.getString("id");
        if (id == null || id.isBlank()) {
            return null;
        }
        return new Mission(id, tag.getString("type"), tag.getString("target"),
                Math.max(1, tag.getInt("count")), Math.max(0, tag.getInt("reward")));
    }

    public static void clearGenerated(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().remove(ACTIVE_ROOT);
        }
    }

    public static void onHostileKill(ServerPlayer player) {
        onHostileKill(player, "monster");
    }

    /** Wave491: typed kill progress — only counts when target filter matches. */
    public static void onHostileKill(ServerPlayer player, String killedTypeId) {
        if (player == null) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(PROGRESS_ROOT).copy();
        String needed = root.getString("kill_target");
        if (needed == null || needed.isBlank()) {
            needed = "monster";
        }
        String killed = killedTypeId == null ? "" : killedTypeId.toLowerCase(Locale.ROOT);
        if (!matchesKillTarget(needed, killed)) {
            return;
        }
        root.putInt("kills", root.getInt("kills") + 1);
        player.getPersistentData().put(PROGRESS_ROOT, root);
    }

    private static boolean matchesKillTarget(String needed, String killed) {
        String n = needed == null ? "monster" : needed.toLowerCase(Locale.ROOT);
        String k = killed == null ? "" : killed.toLowerCase(Locale.ROOT);
        if (n.isBlank() || "monster".equals(n) || "any".equals(n) || "hostile".equals(n)) {
            return true;
        }
        return k.contains(n) || n.contains(k);
    }

    public static void onStewardEscortMark(ServerPlayer player, SectStewardEntity steward) {
        if (player == null) {
            return;
        }
        Mission active = activeGenerated(player);
        if (active == null || !"escort".equalsIgnoreCase(active.type())
                || !EscortMissionService.onStewardContact(player, steward)) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(PROGRESS_ROOT).copy();
        root.putBoolean("escort", true);
        root.putLong("escort_pos", player.blockPosition().asLong());
        player.getPersistentData().put(PROGRESS_ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.escort.arrived"), false);
    }

    public static boolean turnIn(ServerPlayer player, Mission mission) {
        if (mission == null || player == null) {
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(PROGRESS_ROOT).copy();
        String type = mission.type() == null ? "" : mission.type().toLowerCase(Locale.ROOT);
        if ("gather".equals(type)) {
            int have = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getDescriptionId().contains(mission.target())) {
                    have += stack.getCount();
                }
            }
            if (have < mission.count() && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.gather_missing",
                        mission.target(), mission.count(), have), false);
                return false;
            }
        } else if ("kill".equals(type)) {
            int kills = root.getInt("kills");
            if (kills < mission.count() && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.kill_missing",
                        mission.count(), kills), false);
                return false;
            }
            root.putInt("kills", Math.max(0, kills - mission.count()));
        } else if ("escort".equals(type)) {
            if (!root.getBoolean("escort") && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.escort_missing"), false);
                return false;
            }
            EscortMissionService.clearEscort(player, true);
            root.putBoolean("escort", false);
        } else if ("beast".equals(type)) {
            int contracts = com.xunxian.seekingimmortals.cultivation.BeastContractService.list(player).size();
            if (contracts < mission.count() && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.beast_missing",
                        mission.count(), contracts), false);
                return false;
            }
            LifeSkillService.grantPractice(player, SkillType.BEAST_TAMING, 16, 6);
        } else if ("formation".equals(type)) {
            LifeSkillService.grantPractice(player, SkillType.FORMATION, 16, 6);
        }

        player.getPersistentData().put(PROGRESS_ROOT, root);
        LifeSkillService.grantPractice(player, SkillType.TALISMAN_CRAFTING, 6, 2);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.complete",
                mission.id(), mission.rewardContribution()), true);
        clearGenerated(player);
        return true;
    }

    /** Wave490: try turn-in of active generated mission if present. */
    public static boolean tryTurnInActive(ServerPlayer player) {
        Mission mission = activeGenerated(player);
        return mission != null && turnIn(player, mission);
    }
}
