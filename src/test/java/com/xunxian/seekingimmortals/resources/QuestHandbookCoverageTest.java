package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.quest.FtbQuestBridgeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestHandbookCoverageTest {
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/seeking_immortals");
    private static final Path TEXT_ROOT = DATA_ROOT.resolve("text_material");
    private static final Path BOOK_ROOT = DATA_ROOT.resolve("patchouli_books/seeking_immortals_guide");
    private static final Path CHAINS_PATH = TEXT_ROOT.resolve("quest_chains.json");
    private static final Path HOOKS_PATH = TEXT_ROOT.resolve("quest_hooks.json");
    private static final Path STORY_PATH = TEXT_ROOT.resolve("main_story_chapters.json");
    private static final Path LINES_PATH = TEXT_ROOT.resolve("quest_lines_full_descriptions_v147.json");
    private static final Path OVERLAY_PATH = TEXT_ROOT.resolve("quest_handbook_i18n_v1.json");

    private static final Pattern STAGE_ROW = Pattern.compile("(?:^|\\$\\(br\\))(\\d+)\\. ([^$]+)");
    private static final Pattern HAN = Pattern.compile("[\\x{3400}-\\x{4DBF}\\x{4E00}-\\x{9FFF}\\x{F900}-\\x{FAFF}]");
    private static final List<String> LANGUAGES = List.of("zh_cn", "en_us");

    @Test
    void sourceCountsAndFtbProjectionMappingAreExact() throws IOException {
        JsonObject chainsRoot = readJson(CHAINS_PATH);
        JsonObject storyRoot = readJson(STORY_PATH);
        JsonObject linesRoot = readJson(LINES_PATH);
        JsonObject overlay = readJson(OVERLAY_PATH);

        Map<String, JsonObject> chains = index(chainsRoot.getAsJsonArray("chains"), "native chain");
        Map<String, JsonObject> story = index(storyRoot.getAsJsonArray("chapters"), "story chapter");
        Map<String, JsonObject> lines = index(linesRoot.getAsJsonArray("lines"), "authored line");
        JsonArray ftbChapters = overlay.getAsJsonArray("ftb_chapters");
        JsonArray storyChapters = overlay.getAsJsonArray("story_chapters");

        assertEquals(62, chains.size(), "native chain count changed");
        assertEquals(241, chains.values().stream().mapToInt(QuestHandbookCoverageTest::stageCount).sum(),
                "native stage count changed");
        assertEquals(35, lines.size(), "authored line count changed");
        assertEquals(7, story.size(), "story source chapter count changed");
        assertEquals(7, storyChapters.size(), "story handbook chapter count changed");
        assertEquals(9, ftbChapters.size(), "native/FTB handbook chapter count changed");

        List<FtbQuestBridgeService.ChapterSeed> runtimeChapters = FtbQuestBridgeService.builtin().chapters();
        assertEquals(9, runtimeChapters.size());
        Map<String, String> overlayMapping = new LinkedHashMap<>();
        for (int index = 0; index < ftbChapters.size(); index++) {
            JsonObject chapter = ftbChapters.get(index).getAsJsonObject();
            FtbQuestBridgeService.ChapterSeed runtime = runtimeChapters.get(index);
            String chapterId = requiredString(chapter, "id", "FTB overlay chapter " + index);
            assertEquals(runtime.chapterId(), chapterId, "FTB chapter order/id drift at index " + index);
            assertEquals(runtime.title(), "寻仙问道：" + requiredString(chapter, "title_zh", chapterId),
                    "FTB Chinese title drift for " + chapterId);
            for (JsonElement chainElement : chapter.getAsJsonArray("chains")) {
                String chainId = chainElement.getAsString();
                assertTrue(chains.containsKey(chainId), "overlay references unknown chain " + chainId);
                assertNull(overlayMapping.put(chainId, chapterId),
                        "chain appears in multiple overlay chapters: " + chainId);
            }
        }
        assertEquals(FtbQuestBridgeService.builtin().chainToChapter(), overlayMapping,
                "handbook chapter projection must equal the runtime FTB bridge mapping");

        Set<String> mappedLines = new LinkedHashSet<>();
        Set<String> storyIds = new LinkedHashSet<>();
        for (JsonElement chapterElement : storyChapters) {
            JsonObject chapter = chapterElement.getAsJsonObject();
            String chapterId = requiredString(chapter, "id", "story overlay chapter");
            assertTrue(storyIds.add(chapterId), "duplicate story overlay chapter " + chapterId);
            assertTrue(story.containsKey(chapterId), "unknown story overlay chapter " + chapterId);
            for (JsonElement lineElement : chapter.getAsJsonArray("lines")) {
                String lineId = lineElement.getAsString();
                assertTrue(lines.containsKey(lineId), "overlay references unknown authored line " + lineId);
                assertTrue(mappedLines.add(lineId), "authored line appears more than once: " + lineId);
                assertTrue(asStrings(lines.get(lineId).getAsJsonArray("chapters")).contains(chapterId),
                        "authored line is assigned outside its source chapters: " + lineId + " -> " + chapterId);
            }
        }
        assertEquals(story.keySet(), storyIds, "story overlay must cover every source chapter exactly once");
        assertEquals(lines.keySet(), mappedLines, "story overlay must cover every authored line exactly once");
    }

    @Test
    void everyNativeStageHasOneBilingualGeneratedIndex() throws IOException {
        JsonObject chainsRoot = readJson(CHAINS_PATH);
        JsonObject hooksRoot = readJson(HOOKS_PATH);
        JsonObject overlay = readJson(OVERLAY_PATH);
        Map<String, JsonObject> chains = index(chainsRoot.getAsJsonArray("chains"), "native chain");
        Map<String, String> hookDisplays = new HashMap<>();
        for (JsonElement hookElement : hooksRoot.getAsJsonArray("hooks")) {
            JsonObject hook = hookElement.getAsJsonObject();
            if (hook.has("display") && hook.get("display").isJsonPrimitive()
                    && !hook.get("display").getAsString().isBlank()) {
                hookDisplays.put(hook.get("id").getAsString(), hook.get("display").getAsString());
            }
        }

        Map<String, String> chainEntries = chainEntries(overlay);
        JsonObject missingZh = overlay.getAsJsonObject("hook_labels_zh");
        JsonObject numeric = overlay.getAsJsonObject("numeric_stage_labels");

        for (String language : LANGUAGES) {
            Map<String, JsonObject> entries = generatedEntryRoots(language, expectedEntryStems(overlay));
            int generatedStageCount = 0;
            for (Map.Entry<String, JsonObject> source : chains.entrySet()) {
                String chainId = source.getKey();
                int expectedCount = stageCount(source.getValue());
                JsonObject entry = entries.get(chainEntries.get(chainId));
                assertNotNull(entry, "missing generated entry for chain " + chainId + " in " + language);
                Map<Integer, String> rows = stageRows(entry, "chain_" + chainId, language);
                Set<Integer> expectedIndexes = IntStream.rangeClosed(1, expectedCount)
                        .boxed().collect(Collectors.toCollection(LinkedHashSet::new));
                assertEquals(expectedIndexes, rows.keySet(),
                        "missing or duplicate stage index: " + language + " " + chainId);

                List<String> expectedZh = chineseStageLabels(source.getValue(), hookDisplays, missingZh, numeric);
                for (int stage = 1; stage <= expectedCount; stage++) {
                    String label = rows.get(stage);
                    assertNotNull(label, "missing stage label: " + language + " " + chainId + " " + stage);
                    assertFalse(label.isBlank(), "blank stage label: " + language + " " + chainId + " " + stage);
                    if ("zh_cn".equals(language)) {
                        assertEquals(expectedZh.get(stage - 1), label,
                                "Chinese stage label drift: " + chainId + " " + stage);
                    } else {
                        assertFalse(HAN.matcher(label).find(),
                                "English stage label contains Han script: " + chainId + " " + stage + " " + label);
                        assertFalse(label.contains("_"),
                                "English stage label was not humanized: " + chainId + " " + stage + " " + label);
                        assertTrue(label.matches(".*[A-Za-z].*"),
                                "English stage label is not human-readable: " + chainId + " " + stage + " " + label);
                        if (source.getValue().get("steps").isJsonPrimitive()) {
                            assertEquals(numeric.getAsJsonArray(chainId).get(stage - 1).getAsJsonObject()
                                            .get("en").getAsString(),
                                    label, "numeric English stage label drift: " + chainId + " " + stage);
                        }
                    }
                }
                generatedStageCount += rows.size();
            }
            assertEquals(241, generatedStageCount, "generated bilingual stage coverage changed for " + language);
        }
    }

    @Test
    void generatedEntriesAnchorsAndSourceLinksAreExact() throws IOException {
        JsonObject overlay = readJson(OVERLAY_PATH);
        Set<String> expectedStems = expectedEntryStems(overlay);
        Set<String> expectedAnchors = new LinkedHashSet<>();
        Map<String, String> chainEntries = chainEntries(overlay);
        Map<String, String> lineEntries = lineEntries(overlay);
        chainEntries.keySet().forEach(chainId -> expectedAnchors.add("chain_" + chainId));
        lineEntries.keySet().forEach(lineId -> expectedAnchors.add("line_" + lineId));
        assertEquals(62 + 35, expectedAnchors.size());

        for (String language : LANGUAGES) {
            Map<String, JsonObject> entries = generatedEntryRoots(language, expectedStems);
            assertEquals(expectedStems, questCategoryStems(language),
                    "quest category must contain exactly the generated 17 entries in " + language);

            Map<String, Integer> anchors = new LinkedHashMap<>();
            for (JsonObject entry : entries.values()) {
                for (JsonElement pageElement : entry.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    if (page.has("anchor")) {
                        anchors.merge(page.get("anchor").getAsString(), 1, Integer::sum);
                    }
                }
            }
            assertEquals(expectedAnchors, anchors.keySet(), "generated anchor coverage changed in " + language);
            assertTrue(anchors.values().stream().allMatch(count -> count == 1),
                    "every generated chain/line anchor must occur exactly once in " + language);

            JsonObject guide = entries.get("quest_system_guide");
            for (String stem : expectedStems) {
                if (!"quest_system_guide".equals(stem)) {
                    assertContainsLink(guide, stem, language + " guide -> " + stem);
                }
            }
            for (Map.Entry<String, String> chain : chainEntries.entrySet()) {
                assertContainsLink(entries.get(chain.getValue()),
                        chain.getValue() + "#chain_" + chain.getKey(),
                        language + " chain index -> " + chain.getKey());
            }
            for (Map.Entry<String, String> line : lineEntries.entrySet()) {
                assertContainsLink(entries.get(line.getValue()),
                        line.getValue() + "#line_" + line.getKey(),
                        language + " story index -> " + line.getKey());
            }
        }
    }

    @Test
    void staleFtbProjectionClaimsAreAbsent() throws IOException {
        List<String> banned = List.of(
                "任务章节仍待补全",
                "任务引导仍待补全",
                "an FTB Quests chapter are not yet implemented",
                "an FTB Quests chapter is not yet implemented",
                "FTB Quests chapters remain deferred",
                "FTB Quests chapter routing",
                "FTB Quests guidance are not yet implemented"
        );
        for (String language : LANGUAGES) {
            try (Stream<Path> paths = Files.list(BOOK_ROOT.resolve(language + "/entries"))) {
                for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    for (String phrase : banned) {
                        assertFalse(text.contains(phrase),
                                "stale FTB projection claim in " + path + ": " + phrase);
                    }
                }
            }
        }
    }

    private static Map<String, String> chainEntries(JsonObject overlay) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonElement chapterElement : overlay.getAsJsonArray("ftb_chapters")) {
            JsonObject chapter = chapterElement.getAsJsonObject();
            String entry = chapter.get("entry").getAsString();
            for (JsonElement chainElement : chapter.getAsJsonArray("chains")) {
                assertNull(result.put(chainElement.getAsString(), entry),
                        "duplicate chain in handbook overlay: " + chainElement.getAsString());
            }
        }
        return result;
    }

    private static Map<String, String> lineEntries(JsonObject overlay) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonElement chapterElement : overlay.getAsJsonArray("story_chapters")) {
            JsonObject chapter = chapterElement.getAsJsonObject();
            String entry = chapter.get("entry").getAsString();
            for (JsonElement lineElement : chapter.getAsJsonArray("lines")) {
                assertNull(result.put(lineElement.getAsString(), entry),
                        "duplicate line in handbook overlay: " + lineElement.getAsString());
            }
        }
        return result;
    }

    private static Set<String> expectedEntryStems(JsonObject overlay) {
        Set<String> result = new LinkedHashSet<>();
        result.add("quest_system_guide");
        for (JsonElement chapter : overlay.getAsJsonArray("story_chapters")) {
            result.add(chapter.getAsJsonObject().get("entry").getAsString());
        }
        for (JsonElement chapter : overlay.getAsJsonArray("ftb_chapters")) {
            result.add(chapter.getAsJsonObject().get("entry").getAsString());
        }
        assertEquals(17, result.size(), "expected one guide, seven story entries, and nine native entries");
        return result;
    }

    private static Set<String> questCategoryStems(String language) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        try (Stream<Path> paths = Files.list(BOOK_ROOT.resolve(language + "/entries"))) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                JsonObject entry = readJson(path);
                if (entry.has("category") && "seeking_immortals:quests".equals(entry.get("category").getAsString())) {
                    String filename = path.getFileName().toString();
                    result.add(filename.substring(0, filename.length() - ".json".length()));
                }
            }
        }
        return result;
    }

    private static Map<String, JsonObject> generatedEntryRoots(String language, Set<String> stems)
            throws IOException {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (String stem : stems) {
            Path path = BOOK_ROOT.resolve(language + "/entries/" + stem + ".json");
            assertTrue(Files.isRegularFile(path), "missing generated Patchouli entry " + path);
            result.put(stem, readJson(path));
        }
        return result;
    }

    private static Map<Integer, String> stageRows(JsonObject entry, String anchor, String context) {
        JsonArray pages = entry.getAsJsonArray("pages");
        int anchorPage = -1;
        for (int index = 0; index < pages.size(); index++) {
            JsonObject page = pages.get(index).getAsJsonObject();
            if (page.has("anchor") && anchor.equals(page.get("anchor").getAsString())) {
                assertEquals(-1, anchorPage, "duplicate chain anchor in " + context + ": " + anchor);
                anchorPage = index;
            }
        }
        assertTrue(anchorPage >= 0, "missing chain anchor in " + context + ": " + anchor);

        Map<Integer, String> rows = new LinkedHashMap<>();
        for (int index = anchorPage; index < pages.size(); index++) {
            JsonObject page = pages.get(index).getAsJsonObject();
            if (index > anchorPage && page.has("anchor")) {
                break;
            }
            Matcher matcher = STAGE_ROW.matcher(page.get("text").getAsString());
            while (matcher.find()) {
                int stage = Integer.parseInt(matcher.group(1));
                assertNull(rows.put(stage, matcher.group(2)),
                        "duplicate stage number in " + context + " " + anchor + ": " + stage);
            }
        }
        return rows;
    }

    private static List<String> chineseStageLabels(
            JsonObject chain,
            Map<String, String> hookDisplays,
            JsonObject missingZh,
            JsonObject numeric
    ) {
        JsonElement steps = chain.get("steps");
        if (steps.isJsonPrimitive()) {
            List<String> result = new ArrayList<>();
            for (JsonElement label : numeric.getAsJsonArray(chain.get("id").getAsString())) {
                result.add(label.getAsJsonObject().get("zh").getAsString());
            }
            return result;
        }

        List<String> result = new ArrayList<>();
        for (JsonElement stageElement : steps.getAsJsonArray()) {
            JsonObject stage = stageElement.isJsonObject() ? stageElement.getAsJsonObject() : null;
            String hook = stage == null ? stageElement.getAsString() : stage.get("hook").getAsString();
            String label = stage != null && stage.has("summary") && !stage.get("summary").getAsString().isBlank()
                    ? stage.get("summary").getAsString()
                    : hookDisplays.get(hook);
            if (label == null && missingZh.has(hook)) {
                label = missingZh.get(hook).getAsString();
            }
            assertNotNull(label, "missing Chinese stage label for " + chain.get("id").getAsString() + "/" + hook);
            assertFalse(label.isBlank(), "blank Chinese stage label for " + chain.get("id").getAsString() + "/" + hook);
            result.add(label);
        }
        return result;
    }

    private static void assertContainsLink(JsonObject entry, String target, String context) {
        String marker = "$(l:" + target + ")";
        assertTrue(flattenStrings(entry).stream().anyMatch(text -> text.contains(marker)),
                "missing generated source link: " + context + " expected " + marker);
    }

    private static List<String> flattenStrings(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            result.add(element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                result.addAll(flattenStrings(child));
            }
        } else if (element.isJsonObject()) {
            for (JsonElement child : element.getAsJsonObject().entrySet().stream()
                    .map(Map.Entry::getValue).toList()) {
                result.addAll(flattenStrings(child));
            }
        }
        return result;
    }

    private static int stageCount(JsonObject chain) {
        JsonElement steps = chain.get("steps");
        return steps.isJsonArray() ? steps.getAsJsonArray().size() : steps.getAsInt();
    }

    private static Set<String> asStrings(JsonArray values) {
        Set<String> result = new HashSet<>();
        for (JsonElement value : values) {
            result.add(value.getAsString());
        }
        return result;
    }

    private static Map<String, JsonObject> index(JsonArray values, String context) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement value : values) {
            JsonObject object = value.getAsJsonObject();
            String id = requiredString(object, "id", context);
            assertNull(result.put(id, object), "duplicate " + context + " id: " + id);
        }
        return result;
    }

    private static String requiredString(JsonObject root, String key, String context) {
        assertTrue(root.has(key) && root.get(key).isJsonPrimitive(),
                "string " + key + " required: " + context);
        String value = root.get(key).getAsString();
        assertFalse(value.isBlank(), "nonblank " + key + " required: " + context);
        return value;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
