package com.xunxian.seekingimmortals.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight dialogue-tree authority for text quests (Wave46).
 * Not a full dialogue GUI; provides talk/choice nodes tied to chain+branch+npc.
 */
public final class TextQuestDialogueService {
    private TextQuestDialogueService() {}

    public record DialogueLine(String speaker, String textKey, String choiceId) {}

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
                lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.branch_remember", branch));
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
            lines.add(new DialogueLine(npc, "message.seeking_immortals.text_quest.dialogue.branch_remember", branch));
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.text_quest.unknown", chainId), false);
            return false;
        }
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
        String branch = TextQuestChainService.getBranch(player, chainId);
        for (DialogueLine line : lines) {
            player.displayClientMessage(Component.translatable(line.textKey(),
                    line.speaker(),
                    progress.stage(),
                    branch), false);
        }
        return true;
    }

    public static boolean act(ServerPlayer player, String chainId, String choiceId) {
        String choice = choiceId == null ? "" : choiceId.trim().toLowerCase(Locale.ROOT);
        // Wave55: world-NPC authority gate for advancing/branching through dialogue.
        if (("advance".equals(choice) || choice.startsWith("branch")
                || "righteous".equals(choice) || "demonic".equals(choice) || "neutral".equals(choice))
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
            default -> {
                talk(player, chainId);
                yield true;
            }
        };
    }

    public static Map<String, String> sampleNpcHooks(int limit) {
        return TextQuestNpcHookService.sampleBindings(limit);
    }

    public static String npcFor(String chainId) {
        return TextQuestChainService.npcFor(chainId);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
