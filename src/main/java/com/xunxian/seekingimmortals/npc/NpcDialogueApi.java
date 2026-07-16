package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * M12 public dialogue API for M11 and entity interact hooks.
 * <p>{@link #startDialogue(ServerPlayer, String, String)} opens/resumes a tree;
 * {@link #selectNext(ServerPlayer, String)} advances by choice id / next node id;
 * node reach always publishes {@link DialogueNodeReachedEvent}.</p>
 */
public final class NpcDialogueApi {
    private static final String SESSION_ROOT = "seeking_immortals_dialogue_session";

    private NpcDialogueApi() {}

    public record Session(String npcId, String treeId, String nodeId) {}

    public record View(String npcId, String treeId, String nodeId, List<String> lines, List<String> choices,
                       List<DialogueBranchService.Effect> effects) {}

    public static boolean startDialogue(ServerPlayer player, String npcId) {
        return startDialogue(player, npcId, null);
    }

    /**
     * Start or resume dialogue for an NPC.
     * @param treeId optional override; blank resolves from NamedNpcRegistry / branch bindings
     */
    public static boolean startDialogue(ServerPlayer player, String npcId, String treeId) {
        if (player == null) {
            return false;
        }
        String npc = normalize(npcId);
        String tree = normalize(treeId);
        if (tree.isBlank()) {
            tree = resolveTreeId(npc);
        }
        if (tree.isBlank()) {
            // Fall back to greeting template lines without a branch tree.
            return openTemplateOnly(player, npc);
        }
        Optional<DialogueBranchService.Tree> treeOpt = DialogueBranchService.findTree(tree);
        if (treeOpt.isEmpty()) {
            player.displayClientMessage(Component.literal("[对话] 未找到对话树：" + tree), false);
            return false;
        }
        Optional<DialogueBranchService.Node> root = DialogueBranchService.resolveRoot(player, npc, treeOpt.get());
        if (root.isEmpty()) {
            return false;
        }
        return presentNode(player, npc, treeOpt.get(), root.get(), true);
    }

    public static boolean onDialogueNodeReached(ServerPlayer player, String npcId, String nodeId) {
        if (player == null) {
            return false;
        }
        Session session = getSession(player).orElse(null);
        String treeId = session == null ? "" : session.treeId();
        String npc = npcId == null || npcId.isBlank()
                ? (session == null ? "" : session.npcId())
                : normalize(npcId);
        fireNodeReached(player, npc, treeId, nodeId);
        return true;
    }

    /**
     * Client/server choice intent: choice may be a next node id, effect type shortcut, or empty (auto-advance).
     */
    public static boolean selectNext(ServerPlayer player, String choice) {
        if (player == null) {
            return false;
        }
        Optional<Session> sessionOpt = getSession(player);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        Session session = sessionOpt.get();
        Optional<DialogueBranchService.Tree> treeOpt = DialogueBranchService.findTree(session.treeId());
        if (treeOpt.isEmpty()) {
            return false;
        }
        DialogueBranchService.Tree tree = treeOpt.get();
        DialogueBranchService.Node current = tree.nodes().get(normalize(session.nodeId()));
        if (current == null) {
            return startDialogue(player, session.npcId(), session.treeId());
        }

        String pick = normalize(choice);
        DialogueBranchService.Node target = null;
        if (!pick.isBlank()) {
            // Explicit next node id.
            target = tree.nodes().get(pick);
            if (target == null) {
                // Choice equals one of current.next labels.
                for (String nextId : current.next()) {
                    if (normalize(nextId).equals(pick)) {
                        target = tree.nodes().get(normalize(nextId));
                        break;
                    }
                }
            }
            // Effect-type shortcut (e.g. "open_shop") — execute matching current effect and stay/auto-advance.
            if (target == null) {
                for (DialogueBranchService.Effect effect : current.effects()) {
                    if (pick.equals(normalize(effect.type())) || pick.equals(normalize(effect.param("shop")))) {
                        DialogueActionExecutor.execute(player, session.npcId(), session.treeId(), current.id(), effect);
                        // After action, auto-advance if possible.
                        break;
                    }
                }
            }
        }
        if (target == null) {
            Optional<DialogueBranchService.Node> resolved =
                    DialogueBranchService.resolveNext(player, session.npcId(), tree, current);
            if (resolved.isEmpty()) {
                // End of tree — still re-present current lines.
                presentNode(player, session.npcId(), tree, current, false);
                return true;
            }
            target = resolved.get();
        }
        // Only accept next if condition matches (server authority).
        if (!DialogueBranchService.matches(player, session.npcId(), target.when())
                && !target.when().isEmpty()
                && !(target.when().containsKey("default") && target.when().size() == 1)) {
            player.displayClientMessage(Component.literal("[对话] 条件不足，无法选择该分支。"), false);
            presentNode(player, session.npcId(), tree, current, false);
            return false;
        }
        return presentNode(player, session.npcId(), tree, target, true);
    }

    public static Optional<View> currentView(ServerPlayer player) {
        Optional<Session> sessionOpt = getSession(player);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessionOpt.get();
        Optional<DialogueBranchService.Node> node =
                DialogueBranchService.node(session.treeId(), session.nodeId());
        if (node.isEmpty()) {
            return Optional.empty();
        }
        DialogueBranchService.Node n = node.get();
        List<String> choices = new ArrayList<>(n.next());
        // Also expose effect shortcuts as choices for GUI/commands.
        for (DialogueBranchService.Effect effect : n.effects()) {
            String type = normalize(effect.type());
            if (!type.isBlank() && !choices.contains(type)) {
                choices.add(type);
            }
        }
        return Optional.of(new View(session.npcId(), session.treeId(), n.id(), n.lines(), choices, n.effects()));
    }

    public static Optional<Session> getSession(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        CompoundTag tag = player.getPersistentData().getCompound(SESSION_ROOT);
        String npcId = tag.getString("NpcId");
        String treeId = tag.getString("TreeId");
        String nodeId = tag.getString("NodeId");
        if (npcId.isBlank() && treeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Session(npcId, treeId, nodeId));
    }

    public static void clearSession(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(SESSION_ROOT);
    }

    private static boolean presentNode(ServerPlayer player, String npcId, DialogueBranchService.Tree tree,
                                       DialogueBranchService.Node node, boolean runEffects) {
        writeSession(player, npcId, tree.id(), node.id());
        fireNodeReached(player, npcId, tree.id(), node.id());
        if (runEffects && node.effects() != null && !node.effects().isEmpty()) {
            DialogueActionExecutor.executeAll(player, npcId, tree.id(), node.id(), node.effects());
            // Node-scoped reward id: treeId:nodeId (idempotent).
            NamedNpcRewardService.grantIfUnclaimed(player, tree.id() + ":" + node.id());
            // Also try npc-scoped reward catalog entry when ids match.
            NamedNpcRewardService.grantIfUnclaimed(player, npcId);
        }
        // Small favor bump for successful talk nodes.
        NpcFavorService.add(player, npcId, 1);
        for (String line : node.lines()) {
            player.displayClientMessage(Component.literal(displayName(npcId) + "：" + line), false);
        }
        if (!node.next().isEmpty()) {
            player.displayClientMessage(Component.literal("[选项] " + String.join(" / ", node.next())), false);
        }
        // Keep visual dialogue GUI open for chain-compat clients (tree id as chainId payload).
        OpenDialogueScreenPacket.send(player, tree.id());
        return true;
    }

    private static boolean openTemplateOnly(ServerPlayer player, String npcId) {
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(npcId);
        String archetype = npc.map(NamedNpcRegistry.NamedNpc::archetype).orElse("");
        if (archetype.isBlank()) {
            archetype = DialogueTemplateService.archetypeForNpc(npcId).orElse("");
        }
        List<String> lines = DialogueTemplateService.lines(archetype, "greeting");
        if (lines.isEmpty()) {
            lines = List.of("……");
        }
        writeSession(player, npcId, "template:" + archetype, "greeting");
        fireNodeReached(player, npcId, "template:" + archetype, "greeting");
        for (String line : lines) {
            player.displayClientMessage(Component.literal(displayName(npcId) + "：" + line), false);
        }
        OpenDialogueScreenPacket.send(player, npcId);
        return true;
    }

    private static String resolveTreeId(String npcId) {
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(npcId);
        if (npc.isPresent() && !npc.get().dialogueTreeId().isBlank()) {
            return npc.get().dialogueTreeId();
        }
        Optional<DialogueBranchService.Tree> byNpc = DialogueBranchService.treeForNpc(npcId);
        if (byNpc.isPresent()) {
            return byNpc.get().id();
        }
        String archetype = npc.map(NamedNpcRegistry.NamedNpc::archetype)
                .orElse(DialogueTemplateService.archetypeForNpc(npcId).orElse(""));
        return DialogueBranchService.treeIdForArchetype(archetype);
    }

    private static void fireNodeReached(ServerPlayer player, String npcId, String treeId, String nodeId) {
        try {
            MinecraftForge.EVENT_BUS.post(new DialogueNodeReachedEvent(player, npcId, treeId, nodeId));
        } catch (Throwable ignored) {
            // Event bus may be unavailable in pure unit tests.
        }
    }

    private static void writeSession(ServerPlayer player, String npcId, String treeId, String nodeId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("NpcId", npcId == null ? "" : npcId);
        tag.putString("TreeId", treeId == null ? "" : treeId);
        tag.putString("NodeId", nodeId == null ? "" : nodeId);
        player.getPersistentData().put(SESSION_ROOT, tag);
    }

    private static String displayName(String npcId) {
        return NamedNpcRegistry.find(npcId)
                .map(NamedNpcRegistry.NamedNpc::display)
                .filter(s -> s != null && !s.isBlank())
                .orElse(npcId == null || npcId.isBlank() ? "NPC" : npcId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
