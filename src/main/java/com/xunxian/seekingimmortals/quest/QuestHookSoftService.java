package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
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
        // Fallback: same-id chain, then keyword match against known chains.
        if (TextQuestChainService.find(id).isPresent()) {
            return Optional.of(id);
        }
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            String chainId = chain.id();
            if (id.contains(chainId) || chainId.contains(id)) {
                return Optional.of(chainId);
            }
        }
        // Keyword heuristics for common hook prefixes.
        if (id.contains("huangfeng") || id.contains("qixuan") || id.contains("alchemy")) {
            return firstPresent("huangfeng_cultivation_path", "qixuan_mortal_path");
        }
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("wutu")) {
            return firstPresent("mulan_tianlan_war", "mulan_war_campaign", "tianlan_defense_line");
        }
        if (id.contains("ghost") || id.contains("yin") || id.contains("nether")) {
            return firstPresent("ghost_path", "yin_luo_ghost_sect", "yin_cluster_pilgrim");
        }
        if (id.contains("star") || id.contains("chaotic") || id.contains("void") || id.contains("inverse")) {
            return firstPresent("star_palace_internal_politics", "chaotic_sea_politics");
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("wanbao") || id.contains("sect")) {
            return firstPresent("dajin_kunwu_line", "kunwu_mountain_expedition", "dajin_wanbao_route");
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension") || id.contains("diyuan")) {
            return firstPresent("spirit_realm_rise", "tianyuan_merit_path", "spirit_realm_border");
        }
        if (id.contains("demon") || id.contains("fallen") || id.contains("ancient")) {
            return firstPresent("ancient_demon_line", "fallen_demon_expedition", "demonic_six_path");
        }
        return Optional.of("huangfeng_cultivation_path");
    }

    public static boolean preview(ServerPlayer player, String hookId) {
        String id = normalize(hookId);
        FactionQuestCatalogService.Entry entry = FactionQuestCatalogService.builtin().questHooks().get(id);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown", hookId), false);
            return false;
        }
        Optional<String> mapped = mappedChainId(entry.id());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.preview",
                entry.id(), entry.display()), false);
        if (mapped.isPresent()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.mapped",
                    mapped.get()), false);
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
                player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown", hookId), false);
                return false;
            }
            boolean started = TextQuestChainService.start(player, mappedUnknown.get());
            if (started) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accepted",
                        id, mappedUnknown.get()), true);
            }
            return started;
        }
        Optional<String> mapped = mappedChainId(entry.id());
        if (mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.soft_only"), false);
            return false;
        }
        boolean started = TextQuestChainService.start(player, mapped.get());
        if (started) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.accepted",
                    entry.id(), mapped.get()), true);
            // Open dialogue so the player has an immediate authority surface.
            TextQuestNpcHookService.openDialogue(player, mapped.get(), false);
        }
        return started;
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
        map.put("blood_forbidden_run", "ancient_demon_line");
        map.put("void_palace_key", "chaotic_sea_politics");
        map.put("mulan_side", "mulan_tianlan_war");
        map.put("tianlan_side", "tianlan_defense_line");
        map.put("inverse_star_contact", "star_palace_internal_politics");
        map.put("kunwu_expedition", "kunwu_mountain_expedition");
        map.put("nether_river_pilgrim", "yin_cluster_pilgrim");
        map.put("star_palace_patrol", "star_palace_internal_politics");
        map.put("beast_tide_defense", "spirit_realm_rise");
        return map;
    }

    private static String normalize(String hookId) {
        return hookId == null ? "" : hookId.trim().toLowerCase(Locale.ROOT);
    }
}
