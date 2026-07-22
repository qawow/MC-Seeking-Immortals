package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Wave55: world NPC authority entry for text quest chains.
 * Named villagers matching npc ids / display aliases open dialogue and can start chains.
 */
public final class TextQuestNpcHookService {
    private static final double INTERACT_RANGE = 6.0D;

    private static final Map<String, String> DISPLAY_ALIASES = Map.ofEntries(
            Map.entry("墨老先生", "npc_mo_lao"),
            Map.entry("文本任务向导", "npc_text_quest_guide"),
            Map.entry("木兰使者", "npc_mulan_envoy"),
            Map.entry("阴罗执事", "npc_yinluo_steward"),
            Map.entry("星宫掮客", "npc_star_palace_broker"),
            Map.entry("昆吾执事", "npc_kunwu_steward"),
            Map.entry("mo lao", "npc_mo_lao"),
            Map.entry("text quest guide", "npc_text_quest_guide"),
            Map.entry("mulan envoy", "npc_mulan_envoy"),
            Map.entry("yinluo steward", "npc_yinluo_steward"),
            Map.entry("star palace broker", "npc_star_palace_broker"),
            Map.entry("kunwu steward", "npc_kunwu_steward")
    );

    private TextQuestNpcHookService() {}

    public static String npcIdForChain(String chainId) {
        return TextQuestChainService.npcFor(chainId);
    }

    public static Optional<String> chainForNpcId(String npcIdOrName) {
        String key = normalize(npcIdOrName);
        if (key.isBlank()) {
            return Optional.empty();
        }
        String npcId = DISPLAY_ALIASES.getOrDefault(key, key);
        // Prefer an active chain bound to this NPC.
        // Without a player we only resolve static mapping by chain keyword rules.
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            if (npcIdForChain(chain.id()).equals(npcId)) {
                return Optional.of(chain.id());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> chainForNpcId(ServerPlayer player, String npcIdOrName) {
        String key = normalize(npcIdOrName);
        if (key.isBlank()) {
            return Optional.empty();
        }
        String npcId = DISPLAY_ALIASES.getOrDefault(key, key);
        String active = null;
        String fallback = null;
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            if (!npcIdForChain(chain.id()).equals(npcId)) {
                continue;
            }
            if (fallback == null) {
                fallback = chain.id();
            }
            TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chain.id());
            if (progress.stage() > 0 && !progress.complete()) {
                active = chain.id();
                break;
            }
            if (progress.stage() <= 0 && active == null) {
                active = chain.id();
            }
        }
        return Optional.ofNullable(active != null ? active : fallback);
    }

    public static boolean handleNamedVillagerInteraction(ServerPlayer player, Villager villager) {
        if (player == null || villager == null) {
            return false;
        }
        String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
        Optional<String> chain = chainForNpcId(player, name);
        if (chain.isEmpty()) {
            // Also accept exact npc id as custom name.
            chain = chainForNpcId(player, normalize(name));
        }
        if (chain.isEmpty()) {
            return false;
        }
        return openDialogue(player, chain.get(), true);
    }

    public static boolean openDialogue(ServerPlayer player, String chainId, boolean autoStart) {
        Optional<ExtendedCatalogService.QuestChain> optional = TextQuestChainService.find(chainId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown",
                    Component.translatable("text.seeking_immortals.unknown_quest")), false);
            return false;
        }
        String id = optional.get().id();
        if (autoStart) {
            TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
            if (progress.stage() <= 0) {
                TextQuestChainService.start(player, id);
            }
        }
        TextQuestDialogueService.talk(player, id);
        OpenDialogueScreenPacket.send(player, id);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.npc_interact",
                displayNameForNpc(TextQuestChainService.getNpc(player, id)),
                PlayerDisplayText.safeLiteral(optional.get().display(), "text.seeking_immortals.unknown_quest")), true);
        return true;
    }

    public static boolean isNearBoundNpc(ServerPlayer player, String chainId) {
        if (player == null || player.getAbilities().instabuild) {
            return true;
        }
        String expected = TextQuestChainService.getNpc(player, chainId);
        if (expected == null || expected.isBlank()) {
            expected = npcIdForChain(chainId);
        }
        String expectedNorm = normalize(expected);
        AABB box = player.getBoundingBox().inflate(INTERACT_RANGE);
        for (Villager villager : player.serverLevel().getEntitiesOfClass(Villager.class, box)) {
            String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
            String nameNorm = normalize(name);
            String alias = DISPLAY_ALIASES.getOrDefault(nameNorm, nameNorm);
            if (expectedNorm.equals(nameNorm) || expectedNorm.equals(alias) || alias.equals(expectedNorm)) {
                return true;
            }
        }
        return false;
    }

    public static boolean requireNearbyNpcOrWarn(ServerPlayer player, String chainId) {
        if (isNearBoundNpc(player, chainId)) {
            return true;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.text_quest.dialogue.need_npc",
                displayNameForNpc(TextQuestChainService.getNpc(player, chainId))), true);
        return false;
    }

    public static String displayNameForNpc(String npcId) {
        String id = normalize(npcId);
        return switch (id) {
            case "npc_mo_lao" -> "墨老先生";
            case "npc_mulan_envoy" -> "木兰使者";
            case "npc_yinluo_steward" -> "阴罗执事";
            case "npc_star_palace_broker" -> "星宫掮客";
            case "npc_kunwu_steward" -> "昆吾执事";
            default -> "文本任务向导";
        };
    }

    public static Map<String, String> sampleBindings(int limit) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            map.put(chain.id(), npcIdForChain(chain.id()));
            if (++i >= Math.max(1, limit)) {
                break;
            }
        }
        return map;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
