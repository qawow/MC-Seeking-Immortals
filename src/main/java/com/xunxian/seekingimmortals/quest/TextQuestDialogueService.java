package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lightweight dialogue-tree authority for text quests (Wave46).
 * Not a full dialogue GUI; provides talk/choice nodes tied to chain+branch+npc.
 */
public final class TextQuestDialogueService {
    private static final String SESSION_ROOT = "seeking_immortals_text_dialogue_session";
    private static final String CHOICE_SEPARATOR = "\u001f";
    private static final double MAX_SESSION_DISTANCE_SQR = 64.0D;

    private TextQuestDialogueService() {}

    public record DialogueLine(String speaker, String textKey, String choiceId) {}

    public record Session(String chainId, String context, List<String> choiceIds,
                          String dimension, double anchorX, double anchorY, double anchorZ) {}

    public static List<DialogueLine> linesFor(ServerPlayer player, String chainId) {
        String id = normalize(chainId);
        String branch = TextQuestChainService.getBranch(player, id);
        String npc = TextQuestChainService.getNpc(player, id);
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        // Wave491: prefer multi-node demo trees for 10 chains.
        var nodeOpt = TextQuestDialogueTreeService.nodeFor(id, progress.stage(), progress.complete());
        if (nodeOpt.isPresent()) {
            TextQuestDialogueTreeService.Node node = nodeOpt.get();
            List<DialogueLine> lines = new ArrayList<>();
            lines.add(new DialogueLine(npc, node.textKey(), ""));
            for (TextQuestDialogueTreeService.Choice choice : node.choices()) {
                lines.add(new DialogueLine(npc, choice.labelKey(), choice.action()));
            }
            if (!branch.isBlank()) {
                lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.branch_remember", ""));
            }
            return lines;
        }
        List<DialogueLine> lines = new ArrayList<>();
        lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.greeting", ""));
        if (progress.stage() <= 0) {
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.not_started", "start"));
            return lines;
        }
        if (progress.complete()) {
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.complete", ""));
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.branch_remember", ""));
            return lines;
        }
        lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.in_progress", "advance"));
        lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.branch_prompt", "branch"));
        if (TextQuestChainService.BRANCH_RIGHTEOUS.equals(branch)) {
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.righteous", ""));
        } else if (TextQuestChainService.BRANCH_DEMONIC.equals(branch)) {
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.demonic", ""));
        } else {
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.neutral", ""));
        }
        return lines;
    }

    public static boolean talk(ServerPlayer player, String chainId) {
        List<DialogueLine> lines = linesFor(player, chainId);
        if (lines.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown",
                    Component.translatable("text.seeking_immortals.unknown_quest")), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
        String branch = TextQuestChainService.getBranch(player, chainId);
        for (DialogueLine line : lines) {
            player.displayClientMessage(componentFor(line, progress, branch), false);
        }
        return true;
    }

    public static boolean act(ServerPlayer player, String chainId, String choiceId) {
        String choice = choiceId == null ? "" : choiceId.trim().toLowerCase(Locale.ROOT);
        // Wave55: world-NPC authority gate for starting/advancing/branching through dialogue.
        if (("advance".equals(choice) || choice.startsWith("branch")
                || "righteous".equals(choice) || "demonic".equals(choice) || "neutral".equals(choice)
                || "start".equals(choice))
                && !TextQuestNpcHookService.requireNearbyNpcOrWarn(player, chainId)) {
            return false;
        }
        return switch (choice) {
            case "start" -> TextQuestChainService.start(player, chainId);
            case "advance" -> TextQuestChainService.advance(player, chainId);
            case "branch_righteous", "righteous" ->
                    TextQuestChainService.chooseBranch(player, chainId, TextQuestChainService.BRANCH_RIGHTEOUS);
            case "branch_demonic", "demonic" ->
                    TextQuestChainService.chooseBranch(player, chainId, TextQuestChainService.BRANCH_DEMONIC);
            case "branch_neutral", "neutral", "branch" ->
                    TextQuestChainService.chooseBranch(player, chainId, TextQuestChainService.BRANCH_NEUTRAL);
            case "talk", "" -> {
                talk(player, chainId);
                yield true;
            }
            default -> false;
        };
    }

    /** Creates and sends a bounded view for the current text-quest stage. */
    public static boolean openScreen(ServerPlayer player, String chainId) {
        if (player == null) {
            return false;
        }
        String id = normalize(chainId);
        if (TextQuestChainService.find(id).isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.text_quest.unknown",
                    Component.translatable("text.seeking_immortals.unknown_quest")), false);
            return false;
        }
        NpcDialogueApi.clearSession(player);
        clearSession(player);
        return present(player, id, captureAnchor(player));
    }

    /** Refreshes the current view without replaying chat output or quest actions. */
    public static boolean refresh(ServerPlayer player, String context) {
        Session session = sessionForContext(player, context).orElse(null);
        return session != null && present(player, session.chainId(), anchorOf(session));
    }

    /** Applies only a choice that was issued in the current nonce-bound view. */
    public static boolean actCurrent(ServerPlayer player, String context, String choiceId) {
        Session session = sessionForContext(player, context).orElse(null);
        if (session == null) {
            return false;
        }
        String choice = normalize(choiceId);
        if (choice.isBlank() || !session.choiceIds().contains(choice)
                || !isChoiceAllowed(linesFor(player, session.chainId()), choice)) {
            present(player, session.chainId(), anchorOf(session));
            return false;
        }
        boolean result = act(player, session.chainId(), choice);
        present(player, session.chainId(), anchorOf(session));
        return result;
    }

    public static boolean matchesContext(ServerPlayer player, String context) {
        return sessionForContext(player, context).isPresent();
    }

    public static boolean close(ServerPlayer player, String context) {
        if (sessionForContext(player, context).isEmpty()) {
            return false;
        }
        player.getPersistentData().remove(SESSION_ROOT);
        return true;
    }

    public static void clearSession(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().remove(SESSION_ROOT);
        }
    }

    public static Optional<Session> getSession(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        CompoundTag tag = player.getPersistentData().getCompound(SESSION_ROOT);
        String chainId = tag.getString("ChainId");
        String context = tag.getString("Context");
        if (chainId.isBlank() || context.isBlank()) {
            return Optional.empty();
        }
        String rawChoices = tag.getString("ChoiceIds");
        List<String> choices = rawChoices.isBlank()
                ? List.of()
                : List.of(rawChoices.split(CHOICE_SEPARATOR, -1)).stream()
                .map(TextQuestDialogueService::normalize)
                .filter(value -> !value.isBlank())
                .toList();
        return Optional.of(new Session(chainId, context, choices, tag.getString("Dimension"),
                tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ")));
    }

    static boolean isChoiceAllowed(List<DialogueLine> lines, String choiceId) {
        String choice = normalize(choiceId);
        if (choice.isBlank() || lines == null) {
            return false;
        }
        return lines.stream().anyMatch(line -> choice.equals(normalize(line.choiceId())));
    }

    private static boolean present(ServerPlayer player, String chainId, Anchor anchor) {
        List<DialogueLine> dialogue = linesFor(player, chainId);
        if (dialogue.isEmpty()) {
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
        String branch = TextQuestChainService.getBranch(player, chainId);
        String npc = TextQuestChainService.getNpc(player, chainId);
        List<Component> lines = new ArrayList<>();
        List<OpenDialogueScreenPacket.Choice> choices = new ArrayList<>();
        Set<String> seenChoices = new LinkedHashSet<>();
        for (DialogueLine line : dialogue) {
            Component text = componentFor(line, progress, branch);
            String choice = normalize(line.choiceId());
            if (choice.isBlank()) {
                if (lines.size() < OpenDialogueScreenPacket.MAX_LINES) {
                    lines.add(text);
                }
            } else if (seenChoices.add(choice) && choices.size() < OpenDialogueScreenPacket.MAX_CHOICES) {
                choices.add(new OpenDialogueScreenPacket.Choice(choice, text));
            }
        }
        String context = UUID.randomUUID().toString().replace("-", "");
        List<String> choiceIds = choices.stream().map(OpenDialogueScreenPacket.Choice::id).toList();
        writeSession(player, chainId, context, choiceIds, anchor);
        OpenDialogueScreenPacket.send(player, new OpenDialogueScreenPacket(
                context,
                chainId,
                npc,
                nodeId(player, chainId),
                Component.literal(npcDisplay(npc == null || npc.isBlank() ? npcFor(chainId) : npc)),
                List.copyOf(lines),
                List.copyOf(choices)));
        return true;
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
            player.getPersistentData().remove(SESSION_ROOT);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    private static void writeSession(ServerPlayer player, String chainId, String context, List<String> choiceIds,
                                     Anchor anchor) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ChainId", chainId == null ? "" : chainId);
        tag.putString("Context", context == null ? "" : context);
        tag.putString("ChoiceIds", String.join(CHOICE_SEPARATOR, choiceIds == null ? List.of() : choiceIds));
        tag.putString("Dimension", anchor.dimension());
        tag.putDouble("AnchorX", anchor.x());
        tag.putDouble("AnchorY", anchor.y());
        tag.putDouble("AnchorZ", anchor.z());
        player.getPersistentData().put(SESSION_ROOT, tag);
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
        return current.toString().equals(session.dimension())
                && distanceSqr(player.getX(), player.getY(), player.getZ(),
                session.anchorX(), session.anchorY(), session.anchorZ()) <= MAX_SESSION_DISTANCE_SQR;
    }

    static double distanceSqr(double x, double y, double z, double anchorX, double anchorY, double anchorZ) {
        double dx = x - anchorX;
        double dy = y - anchorY;
        double dz = z - anchorZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static String nodeId(ServerPlayer player, String chainId) {
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
        return TextQuestDialogueTreeService.nodeFor(chainId, progress.stage(), progress.complete())
                .map(TextQuestDialogueTreeService.Node::id)
                .orElse("stage_" + Math.max(0, progress.stage()));
    }

    private static Component componentFor(DialogueLine line, TextQuestChainService.ChainProgress progress,
                                          String branch) {
        String speaker = npcDisplay(line.speaker());
        if (line.textKey().endsWith(".branch_remember") || line.textKey().endsWith(".node.mid2")) {
            return Component.translatable(line.textKey(), speaker, branch);
        }
        return Component.translatable(line.textKey(), speaker, progress.stage(), branch);
    }

    public static Map<String, String> sampleNpcHooks(int limit) {
        return TextQuestNpcHookService.sampleBindings(limit);
    }

    public static String npcFor(String chainId) {
        return TextQuestChainService.npcFor(chainId);
    }

    private static String npcDisplay(String raw) {
        return NamedNpcRegistry.find(raw)
                .map(NamedNpcRegistry.NamedNpc::display)
                .filter(PlayerDisplayText::isSafe)
                .map(String::trim)
                .orElse("引路人");
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private record Anchor(String dimension, double x, double y, double z) {}
}
