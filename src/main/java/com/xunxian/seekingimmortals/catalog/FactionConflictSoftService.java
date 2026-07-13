package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService.Entry;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestNpcHookService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Faction conflict browser + Wave461 authority bridge into text quest chains.
 * accept starts a mapped chain; side chooses righteous/neutral/demonic branch.
 * Wave479: accept opens a short SectWar window; side pick contributes war score.
 */
public final class FactionConflictSoftService {
    private static final Map<String, String> CONFLICT_TO_CHAIN = buildConflictMap();
    private static final Map<String, Sides> CONFLICT_TO_SIDES = buildSidesMap();

    public record Sides(String sideA, String sideB, String branchA, String branchB) {}

    private FactionConflictSoftService() {}

    public static int count() {
        return FactionQuestCatalogService.builtin().factionConflicts().size();
    }

    public static List<String> sample(int limit) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (Entry entry : FactionQuestCatalogService.builtin().factionConflicts().values()) {
            String mapped = mappedChainId(entry.id()).orElse("-");
            Sides sides = mappedSides(entry.id()).orElse(null);
            String sideText = sides == null ? "-" : sides.sideA() + "/" + sides.sideB();
            list.add(entry.id() + " | " + entry.display() + " -> " + mapped + " | sides=" + sideText);
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    public static Optional<String> mappedChainId(String conflictId) {
        String id = norm(conflictId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String direct = CONFLICT_TO_CHAIN.get(id);
        if (direct != null) {
            return firstPresent(direct);
        }
        if (TextQuestChainService.find(id).isPresent()) {
            return Optional.of(id);
        }
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            String chainId = chain.id();
            if (id.contains(chainId) || chainId.contains(id)) {
                return Optional.of(chainId);
            }
        }
        if (id.contains("mulan") || id.contains("tianlan") || id.contains("wutu") || id.contains("fashi")) {
            return firstPresent("mulan_tianlan_war", "mulan_war_campaign", "tianlan_defense_line");
        }
        if (id.contains("star") || id.contains("inverse") || id.contains("chaotic") || id.contains("pirate")) {
            return firstPresent("star_palace_internal_politics", "chaotic_sea_politics", "inverse_star_smuggle_arc");
        }
        if (id.contains("ghost") || id.contains("yin") || id.contains("guiling") || id.contains("zhengmo")) {
            return firstPresent("ghost_path", "yin_luo_ghost_sect", "demonic_six_path");
        }
        if (id.contains("huangfeng") || id.contains("yanyue") || id.contains("qingxu") || id.contains("sect")) {
            return firstPresent("huangfeng_cultivation_path", "chain_seven_sect_outer_to_inner", "yanyue_illusion_path");
        }
        if (id.contains("dajin") || id.contains("kunwu") || id.contains("clan") || id.contains("auction")) {
            return firstPresent("dajin_kunwu_line", "dajin_wanbao_route", "human_clan_neutral_intro");
        }
        if (id.contains("spirit") || id.contains("tianyuan") || id.contains("feiling") || id.contains("diyuan")) {
            return firstPresent("spirit_realm_rise", "tianyuan_merit_path", "spirit_realm_border");
        }
        if (id.contains("void") || id.contains("demon") || id.contains("ancient") || id.contains("fallen")) {
            return firstPresent("void_palace_campaign", "ancient_demon_line", "fallen_demon_campaign");
        }
        return Optional.empty();
    }

    public static Optional<Sides> mappedSides(String conflictId) {
        String id = norm(conflictId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        Sides direct = CONFLICT_TO_SIDES.get(id);
        if (direct != null) {
            return Optional.of(direct);
        }
        if (mappedChainId(id).isEmpty()) {
            return Optional.empty();
        }
        // Generic dual-side fallback.
        return Optional.of(new Sides("righteous", "demonic",
                TextQuestChainService.BRANCH_RIGHTEOUS, TextQuestChainService.BRANCH_DEMONIC));
    }

    public static boolean preview(ServerPlayer player, String id) {
        String key = norm(id);
        Entry entry = FactionQuestCatalogService.builtin().factionConflicts().get(key);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.unknown", id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.preview",
                entry.id(), entry.display()), false);
        Optional<String> mapped = mappedChainId(entry.id());
        if (mapped.isPresent()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.mapped",
                    mapped.get()), false);
            mappedSides(entry.id()).ifPresent(sides -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.faction_conflict.sides",
                    sides.sideA(), sides.branchA(), sides.sideB(), sides.branchB()), false));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.accept_hint"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.soft_only"), false);
        }
        return true;
    }

    public static boolean accept(ServerPlayer player, String conflictId) {
        String key = norm(conflictId);
        Entry entry = FactionQuestCatalogService.builtin().factionConflicts().get(key);
        Optional<String> mapped = mappedChainId(key);
        if (entry == null && mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.unknown", conflictId), false);
            return false;
        }
        if (mapped.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.soft_only"), false);
            return false;
        }
        boolean started = TextQuestChainService.start(player, mapped.get());
        if (started) {
            String display = entry == null ? key : entry.id();
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.accepted",
                    display, mapped.get()), true);
            // Wave479: open a short battlefield window for mapped dual-side conflicts.
            tryOpenConflictWar(player, key);
            TextQuestNpcHookService.openDialogue(player, mapped.get(), false);
        }
        return started;
    }

    /**
     * Choose a conflict side. Auto-starts mapped chain if not yet started.
     */
    public static boolean chooseSide(ServerPlayer player, String conflictId, String sideToken) {
        String key = norm(conflictId);
        Optional<String> mapped = mappedChainId(key);
        Optional<Sides> sidesOpt = mappedSides(key);
        if (mapped.isEmpty() || sidesOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.soft_only"), false);
            return false;
        }
        Sides sides = sidesOpt.get();
        String branch = resolveSideBranch(sides, sideToken);
        if (branch.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.side_unknown",
                    sideToken, sides.sideA(), sides.sideB()), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, mapped.get());
        if (progress.stage() <= 0) {
            if (!TextQuestChainService.start(player, mapped.get())) {
                return false;
            }
        }
        boolean ok = TextQuestChainService.chooseBranch(player, mapped.get(), branch);
        if (ok) {
            // Wave466: real faction rep + stage advance after side pick.
            String faction = realFactionForSide(key, sides, branch);
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, faction, 3);
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, "conflict_" + key, 2);
            TextQuestChainService.ChainProgress after = TextQuestChainService.progressOf(player, mapped.get());
            if (after.stage() == 1 && !after.complete()) {
                TextQuestChainService.advance(player, mapped.get());
            }
            try {
                com.xunxian.seekingimmortals.phase.SoftPhaseShellService.mark(player, "phase16_sect_war_arc", false);
            } catch (Throwable ignored) {
                // optional
            }
            // Wave479: ensure war window is open and contribute score for chosen side.
            tryOpenConflictWar(player, key);
            String sideName = branch.equals(sides.branchA()) ? sides.sideA() : sides.sideB();
            boolean scored = com.xunxian.seekingimmortals.sect.SectWarService.contributeForFaction(player, sideName, 2);
            if (!scored) {
                // Fallback contribute by real faction id used for rep.
                com.xunxian.seekingimmortals.sect.SectWarService.contributeForFaction(player, faction, 2);
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.side_chosen",
                    key, sideToken, branch, mapped.get()), true);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.battlefield",
                    key, sideName), false);
        }
        return ok;
    }

    /**
     * Wave479: open a short multiplayer war window using conflict side labels as factions.
     */
    private static void tryOpenConflictWar(ServerPlayer player, String conflictId) {
        if (player == null || player.server == null) {
            return;
        }
        Optional<Sides> sidesOpt = mappedSides(conflictId);
        if (sidesOpt.isEmpty()) {
            return;
        }
        Sides sides = sidesOpt.get();
        // If a war is already active, leave it alone.
        if (com.xunxian.seekingimmortals.sect.SectWarService.isActive(player.server)) {
            return;
        }
        String a = sides.sideA() == null || sides.sideA().isBlank() ? "righteous" : sides.sideA();
        String b = sides.sideB() == null || sides.sideB().isBlank() ? "demonic" : sides.sideB();
        com.xunxian.seekingimmortals.sect.SectWarService.start(player.server, a, b, 10);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.faction_conflict.war_opened",
                a, b), false);
    }

    private static String realFactionForSide(String conflictId, Sides sides, String branch) {
        String id = norm(conflictId);
        String sideName = branch.equals(sides.branchA()) ? sides.sideA() : sides.sideB();
        String token = norm(sideName);
        if (token.contains("mulan") || id.contains("mulan")) {
            return branch.equals(sides.branchA()) && token.contains("mulan") || token.contains("mulan") ? "mulan" : "tianlan";
        }
        if (token.contains("tianlan") || id.contains("tianlan")) {
            return "tianlan";
        }
        if (token.contains("star") || id.contains("star") || id.contains("inverse") || id.contains("chaotic")) {
            return token.contains("inverse") || branch.contains("demon") ? "chaotic_sea" : "chaotic_sea";
        }
        if (token.contains("demon") || token.contains("ghost") || token.contains("yin")
                || branch.equals(TextQuestChainService.BRANCH_DEMONIC)) {
            return "demonic_path";
        }
        if (token.contains("dajin") || token.contains("kunwu") || token.contains("sect")
                || branch.equals(TextQuestChainService.BRANCH_RIGHTEOUS)) {
            return "dajin";
        }
        if (token.contains("spirit") || token.contains("tianyuan")) {
            return "tianyuan";
        }
        return "mortal_realm";
    }

    private static String resolveSideBranch(Sides sides, String sideToken) {
        String token = norm(sideToken);
        if (token.isBlank()) {
            return "";
        }
        // Direct branch names / aliases.
        String asBranch = switch (token) {
            case "righteous", "zheng", "good", "dao" -> TextQuestChainService.BRANCH_RIGHTEOUS;
            case "demonic", "mo", "evil", "xie" -> TextQuestChainService.BRANCH_DEMONIC;
            case "neutral", "zhong", "balance" -> TextQuestChainService.BRANCH_NEUTRAL;
            default -> "";
        };
        if (!asBranch.isBlank()) {
            if (asBranch.equals(sides.branchA()) || asBranch.equals(sides.branchB())) {
                return asBranch;
            }
        }
        if (token.equals(sides.sideA()) || token.contains(sides.sideA()) || sides.sideA().contains(token)
                || token.equals("a") || token.equals("left") || token.equals("1")) {
            return sides.branchA();
        }
        if (token.equals(sides.sideB()) || token.contains(sides.sideB()) || sides.sideB().contains(token)
                || token.equals("b") || token.equals("right") || token.equals("2")) {
            return sides.branchB();
        }
        return "";
    }

    private static Optional<String> firstPresent(String... ids) {
        for (String id : ids) {
            if (TextQuestChainService.find(id).isPresent()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> buildConflictMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("huangfeng_yanyue_rivalry", "huangfeng_cultivation_path");
        map.put("star_palace_inverse_war", "star_palace_internal_politics");
        map.put("mulan_tianlan_campaign", "mulan_tianlan_war");
        map.put("human_demon_alliance_council", "tianyuan_merit_path");
        map.put("feiling_border_skirmish", "spirit_realm_border");
        map.put("qingxu_talisman_embargo", "chain_seven_sect_outer_to_inner");
        map.put("dajin_clan_auction_rival", "dajin_kunwu_line");
        map.put("tiannan_mulan_border_war", "mulan_tianlan_war");
        map.put("tianlan_holy_beast_ritual", "tianlan_defense_line");
        map.put("mulan_smuggle_caravan", "inverse_star_smuggle_arc");
        map.put("seven_sects_joint_defense", "chain_seven_sect_outer_to_inner");
        map.put("chaotic_sea_blockade", "chaotic_sea_politics");
        map.put("inverse_star_raid_tax", "chaotic_sea_politics");
        map.put("ancient_demon_seal_breach", "ancient_demon_line");
        map.put("chaotic_sea_patrol_clash", "chaotic_sea_politics");
        map.put("void_palace_cycle_open", "void_palace_campaign");
        map.put("mulan_tianlan_war_outbreak", "mulan_tianlan_war");
        map.put("conflict_zhengmo_border_skirmish", "ghost_path");
        map.put("conflict_mulan_invasion_wave", "mulan_war_campaign");
        map.put("conflict_star_palace_raid", "star_palace_internal_politics");
        map.put("conflict_clan_vs_sect_mine", "human_clan_neutral_intro");
        return map;
    }

    private static Map<String, Sides> buildSidesMap() {
        Map<String, Sides> map = new LinkedHashMap<>();
        map.put("huangfeng_yanyue_rivalry", sides("huangfeng", "yanyue", "righteous", "demonic"));
        map.put("star_palace_inverse_war", sides("star_palace", "inverse_star", "righteous", "demonic"));
        map.put("mulan_tianlan_campaign", sides("mulan", "tianlan", "demonic", "righteous"));
        map.put("human_demon_alliance_council", sides("human", "demon", "righteous", "demonic"));
        map.put("feiling_border_skirmish", sides("feiling", "demon_lords", "righteous", "demonic"));
        map.put("qingxu_talisman_embargo", sides("qingxu", "huadao", "righteous", "neutral"));
        map.put("dajin_clan_auction_rival", sides("clans", "sect", "neutral", "righteous"));
        map.put("tiannan_mulan_border_war", sides("tiannan", "mulan", "righteous", "demonic"));
        map.put("tianlan_holy_beast_ritual", sides("tianlan", "abstain", "righteous", "neutral"));
        map.put("mulan_smuggle_caravan", sides("mulan", "scatter", "demonic", "neutral"));
        map.put("seven_sects_joint_defense", sides("seven_sects", "mulan", "righteous", "demonic"));
        map.put("chaotic_sea_blockade", sides("star_palace", "inverse_star", "righteous", "demonic"));
        map.put("inverse_star_raid_tax", sides("inverse_star", "star_palace", "demonic", "righteous"));
        map.put("ancient_demon_seal_breach", sides("seal_guard", "breach", "righteous", "demonic"));
        map.put("chaotic_sea_patrol_clash", sides("star_palace", "pirate", "righteous", "demonic"));
        map.put("void_palace_cycle_open", sides("explore", "raid", "neutral", "demonic"));
        map.put("mulan_tianlan_war_outbreak", sides("mulan", "tianlan", "demonic", "righteous"));
        map.put("conflict_zhengmo_border_skirmish", sides("huangfeng", "guiling", "righteous", "demonic"));
        map.put("conflict_mulan_invasion_wave", sides("tiannan_alliance", "mulan_fashi", "righteous", "demonic"));
        map.put("conflict_star_palace_raid", sides("star_palace", "inverse_star", "righteous", "demonic"));
        map.put("conflict_clan_vs_sect_mine", sides("human_clan", "minor_sect", "neutral", "righteous"));
        return map;
    }

    private static Sides sides(String a, String b, String branchA, String branchB) {
        return new Sides(a, b, branchA, branchB);
    }

    private static String norm(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
