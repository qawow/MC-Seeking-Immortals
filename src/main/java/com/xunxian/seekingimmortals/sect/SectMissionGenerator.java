package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple sect mission generator (Wave52 Phase10 depth).
 * Types: gather / kill / escort-proxy.
 */
public final class SectMissionGenerator {
    private SectMissionGenerator() {}

    public record Mission(String id, String type, String target, int count, int rewardContribution) {}

    public static Mission generate(String sectId) {
        int roll = ThreadLocalRandom.current().nextInt(3);
        String sid = sectId == null ? "qinglan" : sectId.toLowerCase(Locale.ROOT);
        return switch (roll) {
            case 0 -> new Mission(sid + "_gather_grass", "gather", "spirit_grass", 8, 15);
            case 1 -> new Mission(sid + "_kill_hostiles", "kill", "monster", 5, 20);
            default -> new Mission(sid + "_escort_proxy", "escort", "steward_marker", 1, 25);
        };
    }

    public static boolean turnIn(ServerPlayer player, Mission mission) {
        if (mission == null) {
            return false;
        }
        if ("gather".equals(mission.type())) {
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
        }
        // kill/escort are soft-validated by prior flags or creative
        player.displayClientMessage(Component.translatable("message.seeking_immortals.sect_mission.complete",
                mission.id(), mission.rewardContribution()), true);
        return true;
    }
}
