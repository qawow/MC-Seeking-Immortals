package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FtbQuestSnbtTest {
    private static final Path PACKAGED_ROOT = Path.of(
            "src/main/resources/seeking_immortals/ftbquests/quests");
    private static final Path PROJECTION = Path.of(
            "src/main/resources/data/seeking_immortals/catalog/ftb_native_stage_projection.json");
    private static final Map<Path, ExpectedChapter> EXPECTED_CHAPTERS = Map.of(
            Path.of("chapters/seeking_immortals_main.snbt"),
            new ExpectedChapter("seeking_immortals_main", "寻仙问道：主线任务", 21),
            Path.of("chapters/seeking_immortals_chaotic_sea.snbt"),
            new ExpectedChapter("seeking_immortals_chaotic_sea", "寻仙问道：乱星海与虚天殿", 18),
            Path.of("chapters/seeking_immortals_dajin_kunwu.snbt"),
            new ExpectedChapter("seeking_immortals_dajin_kunwu", "寻仙问道：大晋与昆吾山", 13),
            Path.of("chapters/seeking_immortals_fallen_demon_yin.snbt"),
            new ExpectedChapter("seeking_immortals_fallen_demon_yin", "寻仙问道：坠魔谷与阴司鬼道", 18),
            Path.of("chapters/seeking_immortals_mulan_demonic.snbt"),
            new ExpectedChapter("seeking_immortals_mulan_demonic", "寻仙问道：慕兰天澜与魔道六宗", 25),
            Path.of("chapters/seeking_immortals_spirit_realm_service.snbt"),
            new ExpectedChapter("seeking_immortals_spirit_realm_service", "寻仙问道：灵界天渊与风元百族", 36),
            Path.of("chapters/seeking_immortals_tiannan_seven_sects.snbt"),
            new ExpectedChapter("seeking_immortals_tiannan_seven_sects", "寻仙问道：天南七派与百艺旁支", 28),
            Path.of("chapters/seeking_immortals_star_palace_inverse.snbt"),
            new ExpectedChapter("seeking_immortals_star_palace_inverse", "寻仙问道：星宫派系与逆星暗线", 24),
            Path.of("chapters/seeking_immortals_ascension_border.snbt"),
            new ExpectedChapter("seeking_immortals_ascension_border", "寻仙问道：飞升边境与终局劫线", 32)
    );

    @Test
    void packagedProjectionCoversEveryNativeStageWithNativeAuthority() throws IOException {
        SNBTCompoundTag data = SNBT.read(PACKAGED_ROOT.resolve("data.snbt"));
        assertNotNull(data, "FTB data.snbt did not parse");
        assertEquals(13, data.getInt("version"));
        assertEquals("linear", data.getString("progression_mode"));

        Map<String, Integer> nativeSteps = new HashMap<>();
        for (ExtendedCatalogService.QuestChain chain
                : ExtendedCatalogService.builtin().questChains().values()) {
            nativeSteps.put(chain.id(), chain.stepCount());
        }
        assertEquals(62, nativeSteps.size(), "Authoritative native chain count changed");
        assertEquals(241, nativeSteps.values().stream().mapToInt(Integer::intValue).sum(),
                "Authoritative native stage count changed");
        Set<FtbNativeQuestSync.Target> expectedTargets = expectedTargets(nativeSteps);

        Set<String> questIds = new HashSet<>();
        Set<String> taskIds = new HashSet<>();
        Set<FtbNativeQuestSync.Target> mirrorTargets = new LinkedHashSet<>();
        Set<FtbNativeQuestSync.Target> writeTargets = new LinkedHashSet<>();
        Set<FtbNativeQuestSync.Target> readyTargets = new LinkedHashSet<>();
        Map<String, String> questChapter = new HashMap<>();
        Map<String, ListTag> dependenciesByQuest = new HashMap<>();
        int totalQuests = 0;

        for (Map.Entry<Path, ExpectedChapter> chapterEntry : EXPECTED_CHAPTERS.entrySet()) {
            Path path = PACKAGED_ROOT.resolve(chapterEntry.getKey());
            assertTrue(Files.exists(path), "Missing FTB chapter " + chapterEntry.getKey());
            SNBTCompoundTag chapter = SNBT.read(path);
            assertNotNull(chapter, "FTB chapter did not parse: " + chapterEntry.getKey());
            ExpectedChapter expected = chapterEntry.getValue();
            assertEquals(expected.filename(), chapter.getString("filename"));
            assertEquals(expected.title(), chapter.getString("title"));

            ListTag quests = chapter.getList("quests", Tag.TAG_COMPOUND);
            assertEquals(expected.questCount(), quests.size(), "Unexpected quest count for " + expected.filename());
            totalQuests += quests.size();
            for (Tag rawQuest : quests) {
                CompoundTag quest = (CompoundTag) rawQuest;
                String questId = quest.getString("id");
                assertFalse(questId.isBlank(), "Quest id must not be blank");
                assertTrue(questIds.add(questId), "Duplicate quest id " + questId);
                assertFalse(quest.getString("title").isBlank(), "Quest title must not be blank: " + questId);
                assertTrue(quest.contains("guide_page", Tag.TAG_STRING), "Quest missing guide_page: " + questId);
                assertGuidePage(quest.getString("guide_page"), expected.filename(), nativeSteps);
                assertTrue(quest.getList("rewards", Tag.TAG_COMPOUND).isEmpty(),
                        "FTB rewards must not bypass native authority: " + questId);
                questChapter.put(questId, expected.filename());
                dependenciesByQuest.put(questId, quest.getList("dependencies", Tag.TAG_STRING));

                Set<String> questTags = strings(quest.getList("tags", Tag.TAG_STRING));
                List<FtbNativeQuestSync.Target> questWrites = questTags.stream()
                        .filter(tag -> tag.startsWith(FtbNativeQuestSync.WRITE_PREFIX))
                        .map(tag -> FtbNativeQuestSync.parseWriteTag(tag).orElseThrow(
                                () -> new AssertionError("Invalid native write tag " + tag)))
                        .toList();
                assertTrue(questWrites.size() <= 1, "Quest has multiple native write targets: " + questId);
                if (!questWrites.isEmpty()) {
                    FtbNativeQuestSync.Target target = questWrites.get(0);
                    assertTrue(writeTargets.add(target), "Duplicate native write target " + target);
                    assertTrue(quest.getBoolean("require_sequential_tasks"),
                            "Native write quest must require sequential tasks: " + questId);
                    Set<String> nativeChainTags = new HashSet<>(questTags);
                    nativeChainTags.retainAll(nativeSteps.keySet());
                    assertEquals(Set.of(target.chainId()), nativeChainTags,
                            "Write quest chain tag must match its target: " + questId);
                }

                ListTag tasks = quest.getList("tasks", Tag.TAG_COMPOUND);
                assertFalse(tasks.isEmpty(), "Quest must have at least one task: " + questId);
                Set<FtbNativeQuestSync.Target> questReady = new LinkedHashSet<>();
                for (Tag rawTask : tasks) {
                    CompoundTag task = (CompoundTag) rawTask;
                    String taskId = task.getString("id");
                    assertFalse(taskId.isBlank(), "Task id must not be blank in quest " + questId);
                    assertTrue(taskIds.add(taskId), "Duplicate task id " + taskId);
                    switch (task.getString("type")) {
                        case "item" -> assertItemTask(task);
                        case "kill" -> assertKillTask(task);
                        case "advancement" -> assertAdvancementTask(task);
                        case "dimension" -> assertDimensionTask(task);
                        case "custom" -> {
                            for (String taskTag : strings(task.getList("tags", Tag.TAG_STRING))) {
                                if (taskTag.startsWith(FtbNativeQuestSync.READY_PREFIX)) {
                                    FtbNativeQuestSync.Target target = parseTarget(
                                            taskTag, FtbNativeQuestSync.READY_PREFIX, nativeSteps);
                                    assertTrue(questReady.add(target), "Duplicate readiness target in quest " + questId);
                                    assertTrue(readyTargets.add(target), "Duplicate readiness target " + target);
                                } else if (taskTag.startsWith(FtbNativeQuestSync.MIRROR_PREFIX)
                                        && !taskTag.startsWith(FtbNativeQuestSync.WRITE_PREFIX)) {
                                    FtbNativeQuestSync.Target target = FtbNativeQuestSync.parseMirrorTag(taskTag)
                                            .orElseThrow(() -> new AssertionError("Invalid mirror tag " + taskTag));
                                    mirrorTargets.add(target);
                                }
                                if (taskTag.startsWith("si_")) {
                                    FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag(taskTag);
                                    assertNotNull(spec, "SI task tag must parse: " + taskTag);
                                    assertFalse(spec instanceof FtbCustomTaskHooks.Spec.Unknown,
                                            "Unknown SI task tag shipped: " + taskTag);
                                }
                            }
                        }
                        default -> fail("Unsupported FTB task type " + task.getString("type") + " for " + taskId);
                    }
                }
                if (!questWrites.isEmpty()) {
                    assertEquals(Set.copyOf(questWrites), questReady,
                            "Write quest must end in one matching native readiness task: " + questId);
                } else {
                    assertTrue(questReady.isEmpty(), "Readiness task without write tag: " + questId);
                }
            }
        }

        assertEquals(9, EXPECTED_CHAPTERS.size());
        assertEquals(215, totalQuests, "Authored FTB node count must remain stable");
        assertEquals(expectedTargets, mirrorTargets,
                "Every one of the 241 native stages must have an FTB mirror task");
        assertEquals(writeTargets, readyTargets,
                "Every explicit reverse-sync target must have one transactional task");
        assertEquals(15, writeTargets.size(), "Unexpected reverse-sync surface change");

        for (Map.Entry<String, ListTag> entry : dependenciesByQuest.entrySet()) {
            for (Tag dependency : entry.getValue()) {
                assertTrue(questIds.contains(dependency.getAsString()),
                        "Missing dependency " + dependency.getAsString() + " for " + entry.getKey());
            }
        }
        assertEquals(questIds, questChapter.keySet());
        assertProjectionManifest(expectedTargets, nativeSteps);
    }

    private static void assertProjectionManifest(Set<FtbNativeQuestSync.Target> expectedTargets,
                                                 Map<String, Integer> nativeSteps) throws IOException {
        JsonObject manifest = JsonParser.parseString(Files.readString(PROJECTION)).getAsJsonObject();
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals(62, manifest.get("chain_count").getAsInt());
        assertEquals(241, manifest.get("stage_count").getAsInt());
        assertEquals(9, manifest.get("chapter_count").getAsInt());
        assertEquals(215, manifest.get("quest_node_count").getAsInt());
        assertEquals("native_player_ledger", manifest.get("authority").getAsString());

        Set<FtbNativeQuestSync.Target> manifestTargets = new LinkedHashSet<>();
        JsonArray stages = manifest.getAsJsonArray("stages");
        for (var element : stages) {
            JsonObject row = element.getAsJsonObject();
            FtbNativeQuestSync.Target target = new FtbNativeQuestSync.Target(
                    row.get("chain").getAsString(), row.get("stage").getAsInt());
            assertTrue(manifestTargets.add(target), "Duplicate projection row " + target);
            assertEquals(FtbQuestBridgeService.chapterForChain(target.chainId()).orElseThrow(),
                    row.get("chapter").getAsString());
            assertTrue(row.getAsJsonArray("mirrors").size() >= 1, "Projection row lacks a mirror " + target);
            assertTrue(target.stage() <= nativeSteps.get(target.chainId()));
        }
        assertEquals(expectedTargets, manifestTargets);
    }

    private static Set<FtbNativeQuestSync.Target> expectedTargets(Map<String, Integer> nativeSteps) {
        Set<FtbNativeQuestSync.Target> targets = new LinkedHashSet<>();
        for (ExtendedCatalogService.QuestChain chain
                : ExtendedCatalogService.builtin().questChains().values()) {
            for (int stage = 1; stage <= nativeSteps.get(chain.id()); stage++) {
                targets.add(new FtbNativeQuestSync.Target(chain.id(), stage));
            }
        }
        return targets;
    }

    private static void assertGuidePage(String guidePage, String chapter, Map<String, Integer> nativeSteps) {
        String expectedEntry = "seeking_immortals:quest_native_"
                + chapter.substring("seeking_immortals_".length());
        assertTrue(guidePage.equals(expectedEntry) || guidePage.startsWith(expectedEntry + "#chain_"),
                "Guide page does not target its chapter handbook: " + guidePage);
        int anchor = guidePage.indexOf("#chain_");
        if (anchor >= 0) {
            String chain = guidePage.substring(anchor + "#chain_".length());
            assertTrue(nativeSteps.containsKey(chain), "Guide page targets unknown chain " + chain);
            assertEquals(chapter, FtbQuestBridgeService.chapterForChain(chain).orElseThrow(),
                    "Guide page chain belongs to a different FTB chapter");
        }
    }

    private static FtbNativeQuestSync.Target parseTarget(String tag,
                                                         String prefix,
                                                         Map<String, Integer> nativeSteps) {
        String body = tag.substring(prefix.length());
        int split = body.lastIndexOf('_');
        assertTrue(split > 0 && split < body.length() - 1, "Malformed target tag " + tag);
        String chain = body.substring(0, split);
        int stage = Integer.parseInt(body.substring(split + 1));
        assertTrue(nativeSteps.containsKey(chain), "Unknown chain in tag " + tag);
        assertTrue(stage >= 1 && stage <= nativeSteps.get(chain), "Out-of-range stage in tag " + tag);
        return new FtbNativeQuestSync.Target(chain, stage);
    }

    private static Set<String> strings(ListTag tags) {
        Set<String> values = new LinkedHashSet<>();
        for (Tag tag : tags) {
            values.add(tag.getAsString());
        }
        return values;
    }

    private static void assertItemTask(CompoundTag task) {
        String taskId = task.getString("id");
        assertFalse(task.getString("item").isBlank(), "Item task missing item: " + taskId);
        assertTrue((task.contains("count") ? task.getLong("count") : 1L) >= 1L,
                "Item task count must be positive: " + taskId);
        assertTrue(task.contains("consume_items"), "Item task must declare consume_items: " + taskId);
        assertFalse(task.getBoolean("consume_items"),
                "FTB item checks are display-only; native authority consumes costs: " + taskId);
        assertTrue(task.contains("match_nbt"), "Item task must declare match_nbt: " + taskId);
        assertFalse(task.getBoolean("match_nbt"), "Bundled item task must ignore NBT: " + taskId);
    }

    private static void assertKillTask(CompoundTag task) {
        assertFalse(task.getString("entity").isBlank(), "Kill task missing entity: " + task.getString("id"));
        assertTrue((task.contains("value") ? task.getLong("value") : 1L) >= 1L);
    }

    private static void assertAdvancementTask(CompoundTag task) {
        assertFalse(task.getString("advancement").isBlank(),
                "Advancement task missing id: " + task.getString("id"));
    }

    private static void assertDimensionTask(CompoundTag task) {
        String dimension = task.getString("dimension");
        assertTrue(dimension.contains(":"), "Dimension task needs namespaced id: " + task.getString("id"));
    }

    private record ExpectedChapter(String filename, String title, int questCount) {
    }
}
