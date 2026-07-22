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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FtbQuestSnbtTest {
    private static final Pattern PLAYER_CODE_LEAK = Pattern.compile(
            "(?i)(?:[a-z_]|\\.json|[/\\\\]|后续|占位|暂不|来源：|源自|同步原生任务阶段|提交原生任务阶段)");
    private static final Path PACKAGED_ROOT = Path.of(
            "src/main/resources/seeking_immortals/ftbquests/quests");
    private static final Path PROJECTION = Path.of(
            "src/main/resources/data/seeking_immortals/catalog/ftb_native_stage_projection.json");
    private static final Map<String, String> NARRATIVE_ALIAS_SUBTITLES = Map.ofEntries(
            Map.entry("1000000000000501", "乱星海势力线 · 星宫巡防"),
            Map.entry("1000000000000502", "乱星海势力线 · 星宫巡防"),
            Map.entry("1000000000000505", "逆星入盟 · 逆星暗号试炼"),
            Map.entry("1000000000000507", "走私补给线 · 逆星走私补给"),
            Map.entry("1000000000000509", "走私补给线 · 虚天殿情报出售"),
            Map.entry("1000000000000517", "星宫逆星对峙 · 逆星伏击"),
            Map.entry("1000000000000605", "大晋正魔边境线 · 正道巡边"),
            Map.entry("1000000000000702", "坠魔谷探秘 · 备驱魔丹与辟邪物"),
            Map.entry("1000000000000704", "坠魔谷探秘 · 裂隙层心魔"),
            Map.entry("1000000000000706", "古魔封印线 · 协助稳固裂隙"),
            Map.entry("1000000000000709", "鬼道线 · 敛魂符阴体"),
            Map.entry("1000000000000711", "阴罗殿线 · 魂幡委托"),
            Map.entry("1000000000000713", "冥河之地行 · 阴雾入界"),
            Map.entry("1000000000000714", "冥河之地行 · 魂滩采集"),
            Map.entry("1000000000000715", "鬼道线 · 聚魂石魂锚"),
            Map.entry("1000000000000716", "鬼道线 · 冥河地结冥核"),
            Map.entry("1000000000000718", "冥河之地行 · 守关"),
            Map.entry("1000000000000820", "魔道六宗线 · 鬼灵门招募"),
            Map.entry("1000000000000822", "魔道六宗线 · 天魔血祭"),
            Map.entry("1000000000000824", "魔道六宗线 · 正道悬赏反噬"),
            Map.entry("1000000000000825", "魔道六宗深入线 · 荒原古冢夺宝"),
            Map.entry("1000000000000901", "人界化神·赴灵界 · 人界境界极限感悟"),
            Map.entry("1000000000000904", "天渊功勋线 · 妖兽攻城防守"),
            Map.entry("1000000000000905", "天渊城服役 · 累积天渊功勋"),
            Map.entry("1000000000000909", "天渊城服役 · 换取灵界资源与通行资格"),
            Map.entry("1000000000000932", "蛮荒妖王线 · 蛮荒兽潮求生"),
            Map.entry("1000000000001003", "化刀坞刀道 · 铸刀委托"),
            Map.entry("1000000000001007", "巨剑门传承 · 古剑残片重铸"),
            Map.entry("1000000000001011", "千竹傀儡线 · 维护高阶傀"),
            Map.entry("1000000000001013", "御灵兽傀线 · 兽魂封傀"),
            Map.entry("1000000000001014", "御灵兽傀线 · 灵兽契约"),
            Map.entry("1000000000001104", "星宫派系线 · 星宫执法派任务"),
            Map.entry("1000000000001106", "星宫派系线 · 星宫商贸派任务"),
            Map.entry("1000000000001108", "星宫派系线 · 星宫商贸派任务"),
            Map.entry("1000000000001120", "虚天情报战 · 虚天钥匙残片传闻"),
            Map.entry("1000000000001122", "虚天殿探险链 · 换取入殿钥令"),
            Map.entry("1000000000001124", "虚天殿探险链 · 夺取宝物并安全撤离"),
            Map.entry("1000000000001211", "阴司朝圣线 · 冥河朝圣"),
            Map.entry("1000000000001217", "七派外门晋升内门 · 累积宗门贡献"),
            Map.entry("1000000000001222", "人界化神·赴灵界 · 灵界界门凭证"),
            Map.entry("1000000000001229", "地渊深层 · 地渊核心晶兽")
    );
    private static final Set<String> NARRATIVE_ONLY_QUEST_IDS = Set.of(
            "1000000000000907",
            "1000000000001114",
            "1000000000001232"
    );
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
        Set<String> narrativeAliases = new HashSet<>();
        Set<String> narrativeOnlyQuests = new HashSet<>();

        for (Map.Entry<Path, ExpectedChapter> chapterEntry : EXPECTED_CHAPTERS.entrySet()) {
            Path path = PACKAGED_ROOT.resolve(chapterEntry.getKey());
            assertTrue(Files.exists(path), "Missing FTB chapter " + chapterEntry.getKey());
            SNBTCompoundTag chapter = SNBT.read(path);
            assertNotNull(chapter, "FTB chapter did not parse: " + chapterEntry.getKey());
            ExpectedChapter expected = chapterEntry.getValue();
            assertEquals(expected.filename(), chapter.getString("filename"));
            assertEquals(expected.title(), chapter.getString("title"));
            assertPlayerVisibleText(chapter.getString("title"), expected.filename() + " chapter title");
            for (Tag subtitle : chapter.getList("subtitle", Tag.TAG_STRING)) {
                String visible = subtitle.getAsString();
                assertPlayerVisibleText(visible, expected.filename() + " chapter subtitle");
                assertTrue(visible.contains("原生任务线奖励由原生任务账本结算"),
                        "Chapter subtitle must distinguish native-chain rewards: " + expected.filename());
                assertFalse(visible.contains("所有奖励均由原生任务账本结算"),
                        "Chapter subtitle must not claim ownership of independent-system rewards: "
                                + expected.filename());
            }

            ListTag quests = chapter.getList("quests", Tag.TAG_COMPOUND);
            assertEquals(expected.questCount(), quests.size(), "Unexpected quest count for " + expected.filename());
            totalQuests += quests.size();
            for (Tag rawQuest : quests) {
                CompoundTag quest = (CompoundTag) rawQuest;
                String questId = quest.getString("id");
                assertFalse(questId.isBlank(), "Quest id must not be blank");
                assertTrue(questIds.add(questId), "Duplicate quest id " + questId);
                assertFalse(quest.getString("title").isBlank(), "Quest title must not be blank: " + questId);
                assertPlayerVisibleText(quest.getString("title"), "quest title " + questId);
                assertPlayerVisibleText(quest.getString("subtitle"), "quest subtitle " + questId);
                boolean hasNativeTarget = hasNativeTarget(quest, nativeSteps.keySet());
                ListTag description = quest.getList("description", Tag.TAG_STRING);
                assertFalse(description.isEmpty(), "Quest description must not be blank: " + questId);
                StringBuilder descriptionText = new StringBuilder();
                for (Tag line : description) {
                    String visible = line.getAsString();
                    assertPlayerVisibleText(visible, "quest description " + questId);
                    descriptionText.append(visible).append('\n');
                }
                String visibleDescription = descriptionText.toString();
                assertTrue(visibleDescription.contains("任务提示："),
                        "Quest description lacks a useful hint: " + questId);
                assertTrue(visibleDescription.contains("完成条件："),
                        "Quest description lacks concrete completion conditions: " + questId);
                assertTrue(visibleDescription.contains("完成奖励："),
                        "Quest description lacks concrete completion rewards: " + questId);
                String expectedAliasSubtitle = NARRATIVE_ALIAS_SUBTITLES.get(questId);
                if (!hasNativeTarget && expectedAliasSubtitle != null) {
                    assertTrue(narrativeAliases.add(questId), "Duplicate narrative alias " + questId);
                    assertEquals(expectedAliasSubtitle, quest.getString("subtitle"),
                            "Narrative alias points at the wrong authored chain stage: " + questId);
                    assertTrue(visibleDescription.contains("原生任务账本"),
                            "Narrative alias must identify native reward authority: " + questId);
                    assertTrue(visibleDescription.contains("整条"),
                            "Narrative alias must show its linked chain finale reward: " + questId);
                    assertTrue(visibleDescription.contains("分支"),
                            "Narrative alias must show its linked branch reward: " + questId);
                    assertTrue(visibleDescription.matches("(?s).*×\\s*\\d+.*"),
                            "Narrative alias reward description needs item quantities: " + questId);
                } else if (!hasNativeTarget) {
                    assertTrue(NARRATIVE_ONLY_QUEST_IDS.contains(questId),
                            "Unregistered narrative node must not silently borrow a chain: " + questId);
                    assertTrue(narrativeOnlyQuests.add(questId), "Duplicate narrative-only node " + questId);
                    assertEquals("章节剧情节点", quest.getString("subtitle"),
                            "Narrative node must not impersonate a native quest chain: " + questId);
                    assertTrue(visibleDescription.contains("不计入原生任务链")
                                    || visibleDescription.contains("未纳入原生任务链"),
                            "Narrative node must deny a fabricated native-chain binding: " + questId);
                    assertTrue(visibleDescription.contains("不发放")
                                    || visibleDescription.contains("不重复发放"),
                            "Narrative node must state its no-reward behavior: " + questId);
                    assertFalse(visibleDescription.contains("整条"),
                            "Narrative node must not quote another chain's finale reward: " + questId);
                    assertFalse(visibleDescription.contains("原生任务账本一次性结算"),
                            "Narrative node must not promise native-ledger rewards: " + questId);
                } else {
                    assertFalse(expectedAliasSubtitle != null || NARRATIVE_ONLY_QUEST_IDS.contains(questId),
                            "Narrative record unexpectedly gained an authoritative target: " + questId);
                    assertTrue(visibleDescription.contains("原生任务账本"),
                            "Quest must identify native reward authority: " + questId);
                    assertTrue(visibleDescription.contains("中段"),
                            "Quest must explain its mid-chain reward: " + questId);
                    assertTrue(visibleDescription.contains("分支"),
                            "Quest must explain its branch reward: " + questId);
                    assertTrue(visibleDescription.matches("(?s).*×\\s*\\d+.*"),
                            "Quest reward description needs item quantities: " + questId);
                }
                if ("1000000000000820".equals(questId)) {
                    assertFalse(hasNativeTarget,
                            "Ghost Spirit Gate recruitment alias must remain display-only");
                    assertEquals("魔道六宗线 · 鬼灵门招募", quest.getString("subtitle"));
                    assertTrue(visibleDescription.contains("整条魔道六宗线完成后结算"),
                            "Ghost Spirit Gate recruitment must use the demonic-six reward line");
                    assertFalse(visibleDescription.contains("慕兰天澜战役")
                                    || visibleDescription.contains("战事令"),
                            "Ghost Spirit Gate recruitment must not borrow Mulan campaign rewards");
                }
                if ("1000000000001001".equals(questId)) {
                    assertTrue(visibleDescription.contains("先在原生任务追踪中接取七派外门晋升内门"));
                    assertTrue(visibleDescription.contains("境界不低于炼气"));
                    assertTrue(visibleDescription.contains("当前区域为天南"));
                    assertTrue(visibleDescription.contains("已加入天南七派"));
                }
                if ("1000000000001232".equals(questId)) {
                    assertEquals("大乘飞升仙界", quest.getString("title"),
                            "Ascension narrative node must retain its authored title");
                }
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
                    assertPlayerVisibleText(task.getString("title"), "task title " + taskId);
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
                                    assertNativeTaskTitle(task, target);
                                    assertTrue(questReady.add(target), "Duplicate readiness target in quest " + questId);
                                    assertTrue(readyTargets.add(target), "Duplicate readiness target " + target);
                                } else if (taskTag.startsWith(FtbNativeQuestSync.MIRROR_PREFIX)
                                        && !taskTag.startsWith(FtbNativeQuestSync.WRITE_PREFIX)) {
                                    FtbNativeQuestSync.Target target = FtbNativeQuestSync.parseMirrorTag(taskTag)
                                            .orElseThrow(() -> new AssertionError("Invalid mirror tag " + taskTag));
                                    assertNativeTaskTitle(task, target);
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
        assertEquals(41, NARRATIVE_ALIAS_SUBTITLES.size());
        assertEquals(3, NARRATIVE_ONLY_QUEST_IDS.size());
        assertEquals(NARRATIVE_ALIAS_SUBTITLES.keySet(), narrativeAliases,
                "All 41 display-only narrative aliases must retain their explicit authored mapping");
        assertEquals(NARRATIVE_ONLY_QUEST_IDS, narrativeOnlyQuests,
                "Only the three independent-system nodes may remain without a native-chain mapping");
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

    private static boolean hasNativeTarget(CompoundTag quest, Set<String> nativeChains) {
        if (strings(quest.getList("tags", Tag.TAG_STRING)).stream().anyMatch(nativeChains::contains)) {
            return true;
        }
        for (Tag rawTask : quest.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag task = (CompoundTag) rawTask;
            for (String tag : strings(task.getList("tags", Tag.TAG_STRING))) {
                if (tag.startsWith(FtbNativeQuestSync.READY_PREFIX)
                        || tag.startsWith(FtbNativeQuestSync.MIRROR_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
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
        String entity = task.getString("entity");
        assertFalse(entity.isBlank(), "Kill task missing entity: " + task.getString("id"));
        assertTrue((task.contains("value") ? task.getLong("value") : 1L) >= 1L);
        assertTrue(task.getString("title").contains(switch (entity) {
            case "minecraft:drowned" -> "溺尸";
            case "minecraft:iron_golem" -> "铁傀儡";
            case "minecraft:pillager" -> "掠夺者";
            case "minecraft:skeleton" -> "骷髅";
            case "minecraft:wither_skeleton" -> "凋灵骷髅";
            default -> "目标生物";
        }), "Kill task title must name its actual target: " + task.getString("id"));
    }

    private static void assertAdvancementTask(CompoundTag task) {
        String advancement = task.getString("advancement");
        assertFalse(advancement.isBlank(),
                "Advancement task missing id: " + task.getString("id"));
        String expected = switch (advancement) {
            case "minecraft:story/root" -> "进入世界";
            case "minecraft:story/mine_stone" -> "圆石";
            case "minecraft:story/iron_tools" -> "铁镐";
            case "minecraft:story/enter_the_nether", "minecraft:nether/root" -> "进入下界";
            case "minecraft:story/follow_ender_eye" -> "要塞";
            case "minecraft:adventure/root" -> "生物战斗";
            case "minecraft:end/root" -> "进入末地";
            default -> "检测目标";
        };
        assertTrue(task.getString("title").contains(expected),
                "Advancement title must name its actual detection target: " + task.getString("id"));
    }

    private static void assertDimensionTask(CompoundTag task) {
        String dimension = task.getString("dimension");
        assertTrue(dimension.contains(":"), "Dimension task needs namespaced id: " + task.getString("id"));
        assertFalse(task.getString("title").contains("seeking_immortals"),
                "Dimension title must not expose its registry id: " + task.getString("id"));
    }

    private static void assertPlayerVisibleText(String text, String context) {
        assertFalse(text == null || text.isBlank(), "Blank player-visible text: " + context);
        assertFalse(PLAYER_CODE_LEAK.matcher(text).find(),
                "Player-visible text leaks code/path/development wording in " + context + ": " + text);
    }

    private static void assertNativeTaskTitle(CompoundTag task, FtbNativeQuestSync.Target target) {
        String title = task.getString("title");
        assertTrue(title.contains("："), "Native task title must include chain and stage names: " + task.getString("id"));
        if (target.stage() == 1) {
            assertTrue(title.contains("接取里程碑"),
                    "Native stage 1 must be presented as an acceptance milestone: " + task.getString("id"));
        } else {
            assertFalse(title.contains("同步") || title.contains("提交"),
                    "Native mirror title must not expose implementation wording: " + task.getString("id"));
        }
    }

    private record ExpectedChapter(String filename, String title, int questCount) {
    }
}
