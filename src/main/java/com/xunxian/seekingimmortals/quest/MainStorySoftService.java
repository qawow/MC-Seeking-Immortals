package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Soft progress for main_story_chapters (7). Persistent stage flags.
 * Wave457: chain finale can auto-complete chapters; complete can start linked chains.
 */
public final class MainStorySoftService {
    private static final String ROOT = "seeking_immortals_main_story";
    private static final Map<String, List<String>> CHAPTER_CHAINS = buildChapterChains();

    private MainStorySoftService() {}

    public static int chapterCount() {
        return ExtendedCatalogService.builtin().chapters().size();
    }

    public static List<String> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<String> lines = new ArrayList<>();
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            boolean done = root.getBoolean(chapter.id());
            List<String> refs = CHAPTER_CHAINS.getOrDefault(chapter.id(), List.of());
            lines.add(chapter.id() + " | " + chapter.display() + " | " + (done ? "DONE" : "OPEN")
                    + (refs.isEmpty() ? "" : " chains=" + String.join(",", refs)));
        }
        return lines;
    }

    public static boolean complete(ServerPlayer player, String chapterId) {
        return completeInternal(player, chapterId, true, true);
    }

    /**
     * Wave457: silent/idempotent complete used by text-quest finale (no spam if already done).
     */
    public static boolean completeQuiet(ServerPlayer player, String chapterId) {
        return completeInternal(player, chapterId, false, false);
    }

    public static boolean isComplete(ServerPlayer player, String chapterId) {
        if (player == null) {
            return false;
        }
        String id = normalize(chapterId);
        return player.getPersistentData().getCompound(ROOT).getBoolean(id)
                || player.getPersistentData().getCompound(ROOT).getBoolean(chapterId == null ? "" : chapterId.trim());
    }

    private static boolean completeInternal(ServerPlayer player, String chapterId, boolean startLinked, boolean loud) {
        Optional<ExtendedCatalogService.StoryChapter> optional = findChapter(chapterId);
        if (optional.isEmpty()) {
            if (loud) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.unknown", chapterId), false);
            }
            return false;
        }
        ExtendedCatalogService.StoryChapter chapter = optional.get();
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (root.getBoolean(chapter.id())) {
            if (loud) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.already",
                        chapter.display()), false);
            }
            return false;
        }
        root.putBoolean(chapter.id(), true);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.completed",
                chapter.display()), true);
        if (chapter.summary() != null && !chapter.summary().isBlank() && loud) {
            player.displayClientMessage(Component.literal(chapter.summary()), false);
        }
        if (startLinked) {
            startLinkedChains(player, chapter.id());
        }
        return true;
    }

    /**
     * Wave457: starting a chapter can open the first linked text-quest chain if not active.
     */
    public static boolean startChapter(ServerPlayer player, String chapterId) {
        Optional<ExtendedCatalogService.StoryChapter> optional = findChapter(chapterId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.unknown", chapterId), false);
            return false;
        }
        if (isComplete(player, optional.get().id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.already",
                    optional.get().display()), false);
            return false;
        }
        List<String> started = startLinkedChains(player, optional.get().id());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.started",
                optional.get().display(), started.size()), true);
        return true;
    }

    private static List<String> startLinkedChains(ServerPlayer player, String chapterId) {
        List<String> started = new ArrayList<>();
        for (String chainId : CHAPTER_CHAINS.getOrDefault(chapterId, List.of())) {
            if (TextQuestChainService.find(chainId).isEmpty()) {
                continue;
            }
            TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, chainId);
            if (progress.stage() > 0 || progress.complete()) {
                continue;
            }
            if (TextQuestChainService.start(player, chainId)) {
                started.add(chainId);
            }
        }
        return started;
    }

    public static int completedCount(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        int count = 0;
        for (String key : root.getAllKeys()) {
            if (root.getBoolean(key)) count++;
        }
        return count;
    }

    public static List<String> chainsForChapter(String chapterId) {
        return CHAPTER_CHAINS.getOrDefault(normalize(chapterId), List.of());
    }

    private static Optional<ExtendedCatalogService.StoryChapter> findChapter(String chapterId) {
        Optional<ExtendedCatalogService.StoryChapter> optional =
                Optional.ofNullable(ExtendedCatalogService.builtin().chapters().get(normalize(chapterId)));
        if (optional.isPresent()) {
            return optional;
        }
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            if (chapter.id().equalsIgnoreCase(chapterId)) {
                return Optional.of(chapter);
            }
        }
        return Optional.empty();
    }

    private static Map<String, List<String>> buildChapterChains() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        // Sourced from text_material main_story_chapters quest_chain_refs.
        map.put("chapter_0_mortal", List.of("huangfeng_cultivation_path"));
        map.put("chapter_1_sect", List.of("huangfeng_cultivation_path", "mulan_war_campaign"));
        map.put("chapter_2_foundation_secret", List.of("blood_forbidden_campaign"));
        map.put("chapter_3_chaotic_sea", List.of("chaotic_sea_politics"));
        map.put("chapter_4_great_jin", List.of("void_great_cultivation_arc", "kunwu_mountain_campaign"));
        map.put("chapter_5_deity_transformation", List.of("fallen_demon_campaign"));
        map.put("chapter_6_spirit_realm", List.of("spirit_realm_rise", "spirit_realm_border"));
        return map;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
