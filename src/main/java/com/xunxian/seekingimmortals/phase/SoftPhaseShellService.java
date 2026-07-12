package com.xunxian.seekingimmortals.phase;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Soft runtime shells for roadmap Phase 12-18 (Wave52).
 * Provides command-visible progression hooks without full content art.
 */
public final class SoftPhaseShellService {
    private static final String ROOT = "seeking_immortals_soft_phases";

    private SoftPhaseShellService() {}

    public static boolean mark(ServerPlayer player, String phaseId) {
        if (player == null || phaseId == null || phaseId.isBlank()) {
            return false;
        }
        var tag = player.getPersistentData().getCompound(ROOT).copy();
        tag.putBoolean(phaseId, true);
        player.getPersistentData().put(ROOT, tag);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.soft_phase.marked", phaseId), true);
        return true;
    }

    public static boolean isMarked(ServerPlayer player, String phaseId) {
        return player != null && player.getPersistentData().getCompound(ROOT).getBoolean(phaseId);
    }

    public static String status(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(ROOT);
        StringBuilder sb = new StringBuilder();
        for (String key : new String[]{
                "phase12_south_sea", "phase13_refinement_full", "phase14_tiannan_auction",
                "phase15_nascent_tree", "phase16_sect_war_arc", "phase17_spirit_entry",
                "phase18_spirit_seven"}) {
            sb.append(key).append('=').append(tag.getBoolean(key)).append(';');
        }
        return sb.toString();
    }
}
