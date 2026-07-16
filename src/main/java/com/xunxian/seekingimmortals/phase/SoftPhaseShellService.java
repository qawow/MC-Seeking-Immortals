package com.xunxian.seekingimmortals.phase;

import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Soft runtime shells for roadmap Phase 12-18.
 * Wave463: mark packages start linked text-quest chains and support enter.
 * <p>
 * M05 note: numeric new-game-plus / difficulty {@code price_mod} is owned by
 * {@link com.xunxian.seekingimmortals.catalog.NewGamePlusEconomyService}, not this shell.
 * Soft phases remain narrative unlock flags only.
 */
public final class SoftPhaseShellService {
    private static final String ROOT = "seeking_immortals_soft_phases";
    private static final Map<String, String> PHASE_CHAINS = buildPhaseChains();
    private static final String[] PHASE_KEYS = PHASE_CHAINS.keySet().toArray(String[]::new);

    private SoftPhaseShellService() {}

    public static boolean mark(ServerPlayer player, String phaseId) {
        return mark(player, phaseId, true);
    }

    public static boolean mark(ServerPlayer player, String phaseId, boolean startPackage) {
        if (player == null || phaseId == null || phaseId.isBlank()) {
            return false;
        }
        String id = phaseId.trim().toLowerCase(Locale.ROOT);
        var tag = player.getPersistentData().getCompound(ROOT).copy();
        boolean already = tag.getBoolean(id);
        tag.putBoolean(id, true);
        player.getPersistentData().put(ROOT, tag);
        if (!already) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.soft_phase.marked", id), true);
        }
        if (startPackage) {
            enterPackage(player, id);
        }
        return true;
    }

    public static boolean enter(ServerPlayer player, String phaseId) {
        if (player == null || phaseId == null || phaseId.isBlank()) {
            return false;
        }
        String id = phaseId.trim().toLowerCase(Locale.ROOT);
        mark(player, id, false);
        return enterPackage(player, id);
    }

    public static boolean isMarked(ServerPlayer player, String phaseId) {
        return player != null && player.getPersistentData().getCompound(ROOT).getBoolean(phaseId);
    }

    public static String status(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(ROOT);
        StringBuilder sb = new StringBuilder();
        for (String key : PHASE_KEYS) {
            sb.append(key).append('=').append(tag.getBoolean(key));
            String chain = PHASE_CHAINS.get(key);
            if (chain != null) {
                sb.append('(').append(chain).append(')');
            }
            sb.append(';');
        }
        return sb.toString();
    }

    private static boolean enterPackage(ServerPlayer player, String phaseId) {
        String chain = PHASE_CHAINS.get(phaseId);
        if (chain == null || chain.isBlank()) {
            return true;
        }
        if (TextQuestChainService.find(chain).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.soft_phase.no_chain", phaseId), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chain);
        if (progress.complete()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.soft_phase.chain_done", phaseId, chain), false);
            return true;
        }
        if (progress.stage() <= 0) {
            return TextQuestChainService.start(player, chain);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.soft_phase.chain_active",
                phaseId, chain, progress.stage(), progress.stepCount()), false);
        return true;
    }

    private static Map<String, String> buildPhaseChains() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("phase12_south_sea", "chaotic_sea_politics");
        map.put("phase13_refinement_full", "dajin_kunwu_line");
        map.put("phase14_tiannan_auction", "dajin_wanbao_route");
        map.put("phase15_nascent_tree", "void_palace_campaign");
        map.put("phase16_sect_war_arc", "mulan_tianlan_war");
        map.put("phase17_spirit_entry", "chain_ascension_spirit_world");
        map.put("phase18_spirit_seven", "spirit_realm_border");
        return map;
    }
}
