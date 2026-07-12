package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Soft progress for main_story_chapters (7). Persistent stage flags only.
 */
public final class MainStorySoftService {
    private static final String ROOT = "seeking_immortals_main_story";

    private MainStorySoftService() {}

    public static int chapterCount() {
        return ExtendedCatalogService.builtin().chapters().size();
    }

    public static List<String> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<String> lines = new ArrayList<>();
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            boolean done = root.getBoolean(chapter.id());
            lines.add(chapter.id() + " | " + chapter.display() + " | " + (done ? "DONE" : "OPEN"));
        }
        return lines;
    }

    public static boolean complete(ServerPlayer player, String chapterId) {
        Optional<ExtendedCatalogService.StoryChapter> optional =
                Optional.ofNullable(ExtendedCatalogService.builtin().chapters().get(normalize(chapterId)));
        if (optional.isEmpty()) {
            // try raw key match
            for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
                if (chapter.id().equalsIgnoreCase(chapterId)) {
                    optional = Optional.of(chapter);
                    break;
                }
            }
        }
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.unknown", chapterId), false);
            return false;
        }
        ExtendedCatalogService.StoryChapter chapter = optional.get();
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (root.getBoolean(chapter.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.already",
                    chapter.display()), false);
            return false;
        }
        root.putBoolean(chapter.id(), true);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.completed",
                chapter.display()), true);
        if (chapter.summary() != null && !chapter.summary().isBlank()) {
            player.displayClientMessage(Component.literal(chapter.summary()), false);
        }
        return true;
    }

    public static int completedCount(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        int count = 0;
        for (String key : root.getAllKeys()) {
            if (root.getBoolean(key)) count++;
        }
        return count;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
