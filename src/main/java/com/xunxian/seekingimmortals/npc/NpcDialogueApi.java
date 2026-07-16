package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Public server-authoritative API for data-driven NPC dialogue. */
public final class NpcDialogueApi {
    private static final String SESSION_ROOT = "seeking_immortals_dialogue_session";
    private static final String CHOICE_SEPARATOR = "\u001f";
    private static final String EFFECT_CHOICE_PREFIX = "effect:";
    private static final double MAX_SESSION_DISTANCE_SQR = 64.0D;

    private NpcDialogueApi() {}

    public record Session(String npcId, String treeId, String nodeId, String context, List<String> choiceIds,
                          String dimension, double anchorX, double anchorY, double anchorZ) {}

    public record View(String context, String npcId, String treeId, String nodeId, Component speaker,
                       List<Component> lines, List<OpenDialogueScreenPacket.Choice> choices) {}

    public static boolean startDialogue(ServerPlayer player, String npcId) {
        return startDialogue(player, npcId, null);
    }

    /** Starts a dialogue from its server-resolved root. */
    public static boolean startDialogue(ServerPlayer player, String npcId, String treeId) {
        if (player == null) {
            return false;
        }
        TextQuestDialogueService.clearSession(player);
        clearSession(player);
        String npc = normalize(npcId);
        String tree = normalize(treeId);
        if (tree.isBlank()) {
            tree = resolveTreeId(npc);
        }
        if (tree.isBlank()) {
            return openTemplateOnly(player, npc);
        }
        Optional<DialogueBranchService.Tree> treeOpt = DialogueBranchService.findTree(tree);
        if (treeOpt.isEmpty()) {
            player.displayClientMessage(Component.literal("[对话] 未找到对话树：" + tree), false);
            return false;
        }
        Optional<DialogueBranchService.Node> root = DialogueBranchService.resolveRoot(player, npc, treeOpt.get());
        return root.isPresent() && presentNode(player, npc, treeOpt.get(), root.get(), true, captureAnchor(player));
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

    /** Trusted server-side convenience overload. Network handlers must pass the context overload. */
    public static boolean selectNext(ServerPlayer player, String choice) {
        return getSession(player)
                .map(session -> selectNext(player, session.context(), choice))
                .orElse(false);
    }

    /** Applies one choice from the currently issued view. */
    public static boolean selectNext(ServerPlayer player, String context, String choice) {
        Session session = sessionForContext(player, context).orElse(null);
        if (session == null) {
            return false;
        }
        DialogueBranchService.Tree tree = DialogueBranchService.findTree(session.treeId()).orElse(null);
        if (tree == null) {
            return false;
        }
        DialogueBranchService.Node current = tree.nodes().get(normalize(session.nodeId()));
        if (current == null) {
            clearSession(player);
            return false;
        }

        String pick = normalize(choice);
        if (pick.isBlank() || !session.choiceIds().contains(pick)) {
            refresh(player, context);
            return false;
        }

        DeferredEffect deferred = deferredEffect(current, pick).orElse(null);
        if (deferred != null) {
            if (!DialogueBranchService.matches(player, session.npcId(), current.when())) {
                refresh(player, context);
                return false;
            }
            clearSession(player);
            return DialogueActionExecutor.execute(player, session.npcId(), session.treeId(),
                    current.id(), deferred.effect());
        }

        DialogueBranchService.Node target = tree.nodes().get(pick);
        if (target == null || !DialogueBranchService.isDirectNext(current, pick)) {
            refresh(player, context);
            return false;
        }
        List<DialogueBranchService.Node> eligible = DialogueBranchService.availableNext(
                player, session.npcId(), tree, current);
        boolean stillAllowed = eligible.stream().anyMatch(node -> node.id().equals(target.id()));
        if (!stillAllowed || !DialogueBranchService.matches(player, session.npcId(), target.when())) {
            player.displayClientMessage(Component.literal("[对话] 条件不足，无法选择该分支。"), false);
            refresh(player, context);
            return false;
        }
        return presentNode(player, session.npcId(), tree, target, true, anchorOf(session));
    }

    /** Reissues the current node without replaying events, rewards, favor, or node effects. */
    public static boolean refresh(ServerPlayer player, String context) {
        Session session = sessionForContext(player, context).orElse(null);
        if (session == null) {
            return false;
        }
        // Capture the requested context; present* generates a new nonce. If the session is closed
        // before sendView, the late view must not re-cover the page.
        String expected = session.context();
        if (session.treeId().startsWith("template:")) {
            boolean ok = presentTemplate(player, session.npcId(), session.treeId(), false, anchorOf(session));
            return ok && getSession(player).isPresent();
        }
        DialogueBranchService.Tree tree = DialogueBranchService.findTree(session.treeId()).orElse(null);
        DialogueBranchService.Node node = tree == null ? null : tree.nodes().get(normalize(session.nodeId()));
        if (tree == null || node == null) {
            clearSession(player);
            return false;
        }
        // Bail if session was closed between lookup and present (nonce race with close).
        if (sessionForContext(player, expected).isEmpty()) {
            return false;
        }
        boolean ok = presentNode(player, session.npcId(), tree, node, false, anchorOf(session));
        return ok && getSession(player).isPresent();
    }

    public static Optional<View> currentView(ServerPlayer player) {
        Session session = getSession(player).orElse(null);
        if (session == null || session.context().isBlank()) {
            return Optional.empty();
        }
        if (session.treeId().startsWith("template:")) {
            return Optional.of(templateView(session.context(), session.npcId(), session.treeId()));
        }
        DialogueBranchService.Tree tree = DialogueBranchService.findTree(session.treeId()).orElse(null);
        DialogueBranchService.Node node = tree == null ? null : tree.nodes().get(normalize(session.nodeId()));
        if (tree == null || node == null) {
            return Optional.empty();
        }
        View view = buildView(player, session.context(), session.npcId(), tree, node);
        List<OpenDialogueScreenPacket.Choice> issued = view.choices().stream()
                .filter(candidate -> session.choiceIds().contains(normalize(candidate.id())))
                .toList();
        return Optional.of(new View(view.context(), view.npcId(), view.treeId(), view.nodeId(),
                view.speaker(), view.lines(), issued));
    }

    public static Optional<Session> getSession(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        CompoundTag tag = player.getPersistentData().getCompound(SESSION_ROOT);
        String npcId = tag.getString("NpcId");
        String treeId = tag.getString("TreeId");
        if (npcId.isBlank() && treeId.isBlank()) {
            return Optional.empty();
        }
        String rawChoices = tag.getString("ChoiceIds");
        List<String> choices = rawChoices.isBlank()
                ? List.of()
                : List.of(rawChoices.split(CHOICE_SEPARATOR, -1)).stream()
                .map(NpcDialogueApi::normalize)
                .filter(value -> !value.isBlank())
                .toList();
        return Optional.of(new Session(npcId, treeId, tag.getString("NodeId"),
                tag.getString("Context"), choices, tag.getString("Dimension"),
                tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ")));
    }

    public static boolean matchesContext(ServerPlayer player, String context) {
        return sessionForContext(player, context).isPresent();
    }

    public static boolean close(ServerPlayer player, String context) {
        if (sessionForContext(player, context).isEmpty()) {
            return false;
        }
        clearSession(player);
        return true;
    }

    public static void clearSession(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().remove(SESSION_ROOT);
        }
    }

    private static Optional<Session> sessionForContext(ServerPlayer player, String context) {
        String expected = context == null ? "" : context.trim();
        if (expected.isBlank()) {
            return Optional.empty();
        }
        Session session = getSession(player).filter(value -> expected.equals(value.context())).orElse(null);
        if (session == null) {
            return Optional.empty();
        }
        if (!withinAnchor(player, session)) {
            clearSession(player);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    private static boolean presentNode(ServerPlayer player, String npcId, DialogueBranchService.Tree tree,
                                       DialogueBranchService.Node node, boolean runEffects, Anchor anchor) {
        String claimKey = normalize(tree.id()) + ":" + normalize(node.id());
        boolean alreadyClaimed = NamedNpcRewardService.hasClaimed(player, claimKey);
        String context = newContext();
        writeSession(player, npcId, tree.id(), node.id(), context, List.of(), anchor);
        boolean terminal = false;
        if (runEffects && !alreadyClaimed) {
            fireNodeReached(player, npcId, tree.id(), node.id());
            boolean hasEffects = node.effects() != null && !node.effects().isEmpty();
            if (hasEffects) {
                DialogueActionExecutor.executeImmediate(player, npcId, tree.id(), node.id(), node.effects());
                // tree:node claim latches rewards, immediate effects, and favor for this node.
                NamedNpcRewardService.grantIfUnclaimed(player, claimKey);
                NamedNpcRewardService.grantIfUnclaimed(player, npcId);
                NpcFavorService.add(player, npcId, 1);
                terminal = DialogueActionExecutor.hasTerminalEffect(node.effects());
            }
            for (String line : node.lines()) {
                player.displayClientMessage(Component.literal(displayName(npcId) + "：" + line), false);
            }
        } else if (runEffects) {
            // Re-entry after claim: show chat lines only, never re-run effects/rewards/favor.
            for (String line : node.lines()) {
                player.displayClientMessage(Component.literal(displayName(npcId) + "：" + line), false);
            }
            terminal = DialogueActionExecutor.hasTerminalEffect(node.effects());
        }
        if (terminal) {
            clearSession(player);
            return true;
        }
        View view = buildView(player, context, npcId, tree, node);
        // Guard against close/refresh races: only re-issue a view if the session we just wrote is still current.
        Session live = getSession(player).orElse(null);
        if (live == null || !context.equals(live.context())) {
            return false;
        }
        writeSession(player, npcId, tree.id(), node.id(), context, choiceIds(view), anchor);
        // Re-check after choice write; a concurrent close must not resurrect the page.
        live = getSession(player).orElse(null);
        if (live == null || !context.equals(live.context())) {
            return false;
        }
        sendView(player, view);
        return true;
    }

    private static View buildView(ServerPlayer player, String context, String npcId,
                                  DialogueBranchService.Tree tree, DialogueBranchService.Node node) {
        List<Component> lines = node.lines().stream()
                .limit(OpenDialogueScreenPacket.MAX_LINES)
                .map(Component::literal)
                .map(Component.class::cast)
                .toList();
        List<OpenDialogueScreenPacket.Choice> choices = new ArrayList<>();
        for (DialogueBranchService.Node target : DialogueBranchService.availableNext(player, npcId, tree, node)) {
            if (choices.size() >= OpenDialogueScreenPacket.MAX_CHOICES) {
                break;
            }
            Component label = target.lines().isEmpty()
                    ? Component.literal(target.id())
                    : Component.literal(target.lines().get(0));
            choices.add(new OpenDialogueScreenPacket.Choice(target.id(), label));
        }
        for (int index = 0; index < node.effects().size()
                && choices.size() < OpenDialogueScreenPacket.MAX_CHOICES; index++) {
            DialogueBranchService.Effect effect = node.effects().get(index);
            if (DialogueActionExecutor.isDeferredChoice(effect)) {
                choices.add(new OpenDialogueScreenPacket.Choice(effectChoiceId(effect, index),
                        Component.translatable("screen.seeking_immortals.dialogue.choice.open_shop")));
            }
        }
        return new View(context, npcId, tree.id(), node.id(), Component.literal(displayName(npcId)),
                lines, List.copyOf(choices));
    }

    private static boolean openTemplateOnly(ServerPlayer player, String npcId) {
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(npcId);
        String archetype = npc.map(NamedNpcRegistry.NamedNpc::archetype).orElse("");
        if (archetype.isBlank()) {
            archetype = DialogueTemplateService.archetypeForNpc(npcId).orElse("");
        }
        return presentTemplate(player, npcId, "template:" + archetype, true, captureAnchor(player));
    }

    private static boolean presentTemplate(ServerPlayer player, String npcId, String treeId, boolean runEffects,
                                           Anchor anchor) {
        String claimKey = normalize(treeId) + ":greeting";
        boolean alreadyClaimed = NamedNpcRewardService.hasClaimed(player, claimKey);
        String context = newContext();
        View view = templateView(context, npcId, treeId);
        writeSession(player, npcId, treeId, "greeting", context, List.of(), anchor);
        if (runEffects && !alreadyClaimed) {
            fireNodeReached(player, npcId, treeId, "greeting");
            NpcFavorService.add(player, npcId, 1);
            NamedNpcRewardService.markClaimed(player, claimKey);
            for (Component line : view.lines()) {
                player.displayClientMessage(Component.literal(displayName(npcId) + "：").append(line), false);
            }
        }
        Session live = getSession(player).orElse(null);
        if (live == null || !context.equals(live.context())) {
            return false;
        }
        writeSession(player, npcId, treeId, "greeting", context, choiceIds(view), anchor);
        live = getSession(player).orElse(null);
        if (live == null || !context.equals(live.context())) {
            return false;
        }
        sendView(player, view);
        return true;
    }

    private static View templateView(String context, String npcId, String treeId) {
        String archetype = treeId.startsWith("template:") ? treeId.substring("template:".length()) : "";
        List<String> raw = DialogueTemplateService.lines(archetype, "greeting");
        if (raw.isEmpty()) {
            raw = List.of("……");
        }
        List<Component> lines = raw.stream()
                .limit(OpenDialogueScreenPacket.MAX_LINES)
                .map(Component::literal)
                .map(Component.class::cast)
                .toList();
        return new View(context, npcId, treeId, "greeting", Component.literal(displayName(npcId)),
                lines, List.of());
    }

    private static Optional<DeferredEffect> deferredEffect(DialogueBranchService.Node node, String choiceId) {
        for (int index = 0; index < node.effects().size(); index++) {
            DialogueBranchService.Effect effect = node.effects().get(index);
            if (DialogueActionExecutor.isDeferredChoice(effect)
                    && effectChoiceId(effect, index).equals(choiceId)) {
                return Optional.of(new DeferredEffect(effect));
            }
        }
        return Optional.empty();
    }

    static String effectChoiceId(DialogueBranchService.Effect effect, int index) {
        return EFFECT_CHOICE_PREFIX + normalize(effect == null ? "" : effect.type()) + ":" + Math.max(0, index);
    }

    private static List<String> choiceIds(View view) {
        return view.choices().stream().map(choice -> normalize(choice.id())).filter(id -> !id.isBlank()).toList();
    }

    private static void sendView(ServerPlayer player, View view) {
        OpenDialogueScreenPacket.send(player, new OpenDialogueScreenPacket(
                view.context(), view.treeId(), view.npcId(), view.nodeId(),
                view.speaker(), view.lines(), view.choices()));
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

    private static void writeSession(ServerPlayer player, String npcId, String treeId, String nodeId,
                                     String context, List<String> choiceIds, Anchor anchor) {
        CompoundTag tag = new CompoundTag();
        tag.putString("NpcId", npcId == null ? "" : npcId);
        tag.putString("TreeId", treeId == null ? "" : treeId);
        tag.putString("NodeId", nodeId == null ? "" : nodeId);
        tag.putString("Context", context == null ? "" : context);
        tag.putString("ChoiceIds", String.join(CHOICE_SEPARATOR, choiceIds == null ? List.of() : choiceIds));
        tag.putString("Dimension", anchor.dimension());
        tag.putDouble("AnchorX", anchor.x());
        tag.putDouble("AnchorY", anchor.y());
        tag.putDouble("AnchorZ", anchor.z());
        player.getPersistentData().put(SESSION_ROOT, tag);
    }

    private static String newContext() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Anchor captureAnchor(ServerPlayer player) {
        return new Anchor(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ());
    }

    private static Anchor anchorOf(Session session) {
        return new Anchor(session.dimension(), session.anchorX(), session.anchorY(), session.anchorZ());
    }

    private static boolean withinAnchor(ServerPlayer player, Session session) {
        if (player == null || session == null || session.dimension().isBlank()) {
            return false;
        }
        ResourceLocation current = player.level().dimension().location();
        if (!current.toString().equals(session.dimension())) {
            return false;
        }
        return distanceSqr(player.getX(), player.getY(), player.getZ(),
                session.anchorX(), session.anchorY(), session.anchorZ()) <= MAX_SESSION_DISTANCE_SQR;
    }

    static double distanceSqr(double x, double y, double z, double anchorX, double anchorY, double anchorZ) {
        double dx = x - anchorX;
        double dy = y - anchorY;
        double dz = z - anchorZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static String displayName(String npcId) {
        return NamedNpcRegistry.find(npcId)
                .map(NamedNpcRegistry.NamedNpc::display)
                .filter(value -> value != null && !value.isBlank())
                .orElse(npcId == null || npcId.isBlank() ? "NPC" : npcId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DeferredEffect(DialogueBranchService.Effect effect) {}

    private record Anchor(String dimension, double x, double y, double z) {}
}
