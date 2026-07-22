package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only player-facing metadata for the native quest tracker.
 *
 * <p>The authority remains in {@link TextQuestChainService}. This service only joins the authored
 * chain, hook and handbook resources into safe titles, summaries and stage labels. Internal ids are
 * retained solely as lookup keys and are never returned as a display fallback.</p>
 */
public final class QuestPresentationService {
    private static final String ROOT = "data/" + SeekingImmortalsMod.MODID + "/text_material/";
    private static final Snapshot BUILTIN = loadBuiltin();

    private QuestPresentationService() {}

    public record RequirementPresentation(String textZh, String textEn, boolean enforced) {}

    public record StagePresentation(
            int number,
            String hookId,
            String titleZh,
            String titleEn,
            String summaryZh,
            String summaryEn,
            boolean optional,
            boolean boss,
            List<RequirementPresentation> requirements
    ) {}

    public record ChainPresentation(
            String id,
            String titleZh,
            String titleEn,
            String descriptionZh,
            String descriptionEn,
            String regionId,
            String realmMin,
            String factionId,
            List<StagePresentation> stages,
            List<String> authoredFinaleRewards,
            List<RequirementPresentation> requirements
    ) {
        public int stepCount() {
            return stages.size();
        }

        public Optional<StagePresentation> stage(int oneBasedStage) {
            if (oneBasedStage <= 0 || oneBasedStage > stages.size()) {
                return Optional.empty();
            }
            return Optional.of(stages.get(oneBasedStage - 1));
        }
    }

    public record Snapshot(Map<String, ChainPresentation> chains) {
        public Optional<ChainPresentation> find(String chainId) {
            return Optional.ofNullable(chains.get(normalize(chainId)));
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<ChainPresentation> find(String chainId) {
        return BUILTIN.find(chainId);
    }

    public static String title(String chainId, boolean chinese) {
        return find(chainId).map(chain -> chinese ? chain.titleZh() : chain.titleEn()).orElse("");
    }

    public static String description(String chainId, boolean chinese) {
        return find(chainId).map(chain -> chinese ? chain.descriptionZh() : chain.descriptionEn()).orElse("");
    }

    public static String stageLabel(String chainId, int stage, boolean chinese) {
        return find(chainId).flatMap(chain -> chain.stage(stage))
                .map(value -> chinese ? value.titleZh() : value.titleEn()).orElse("");
    }

    /** Returns the same catalog-first finale preview used by the native reward authority. */
    public static List<TextQuestChainService.RewardPreview> finaleRewards(String chainId) {
        try {
            return TextQuestChainService.finaleRewardPreview(chainId);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    /** Pure description of the next transition's material cost, when one exists. */
    public static Optional<TextQuestChainService.StageCost> nextStageCost(String chainId, int currentStage) {
        Optional<ChainPresentation> chain = find(chainId);
        if (chain.isEmpty()) {
            return Optional.empty();
        }
        return TextQuestChainService.stageCostFor(chain.get().id(), currentStage + 1, chain.get().stepCount());
    }

    /** Mid-chain milestone reward; the server grants this once at the midpoint. */
    public static List<TextQuestChainService.RewardPreview> midpointRewards(String chainId) {
        Optional<ChainPresentation> chain = find(chainId);
        if (chain.isEmpty() || chain.get().stepCount() < 4) {
            return List.of();
        }
        return List.of(new TextQuestChainService.RewardPreview("seeking_immortals:spirit_stone_shard", 2));
    }

    /** Branch bonus preview mirrors TextQuestChainService's one-time branch bonus. */
    public static List<TextQuestChainService.RewardPreview> branchRewards(String branch) {
        String value = normalize(branch);
        return switch (value) {
            case "righteous" -> List.of(new TextQuestChainService.RewardPreview(
                    "seeking_immortals:alliance_merit_token", 1));
            case "demonic" -> List.of(new TextQuestChainService.RewardPreview(
                    "seeking_immortals:yin_stone", 4));
            default -> List.of(new TextQuestChainService.RewardPreview(
                    "seeking_immortals:spirit_stone_shard", 2));
        };
    }

    /** Human fallback for a reward whose item translation is unavailable; never returns its id. */
    public static String rewardFallback(String itemId, boolean chinese) {
        String id = normalize(itemId);
        int separator = id.indexOf(':');
        if (separator >= 0) {
            id = id.substring(separator + 1);
        }
        String zh = switch (id) {
            case "spirit_stone_shard" -> "灵石碎片";
            case "alliance_merit_token" -> "功勋令";
            case "yin_stone" -> "阴石";
            case "soul_fragment" -> "魂魄碎片";
            case "immortal_jade" -> "仙玉";
            case "void_crystal" -> "虚空结晶";
            case "void_marrow" -> "虚空髓";
            case "war_contribution_token" -> "战勋牌";
            case "jade_slip_blank" -> "空白玉简";
            case "soul_gathering_stone" -> "聚魂石";
            case "demonic_blood_coral" -> "魔血珊瑚";
            case "foundation_building_pill_low" -> "下品筑基丹";
            case "spirit_recovery_pill" -> "回灵丹";
            case "jiangchen_pill" -> "降尘丹";
            case "star_palace_tax_receipt" -> "星宫税契";
            default -> "剧情奖励";
        };
        if (chinese) {
            return zh;
        }
        return switch (id) {
            case "spirit_stone_shard" -> "Spirit Stone Shard";
            case "alliance_merit_token" -> "Merit Token";
            case "yin_stone" -> "Yin Stone";
            case "soul_fragment" -> "Soul Fragment";
            case "immortal_jade" -> "Immortal Jade";
            case "void_crystal" -> "Void Crystal";
            case "void_marrow" -> "Void Marrow";
            case "war_contribution_token" -> "War Merit Token";
            case "jade_slip_blank" -> "Blank Jade Slip";
            case "soul_gathering_stone" -> "Soul-Gathering Stone";
            case "demonic_blood_coral" -> "Demonic Blood Coral";
            case "foundation_building_pill_low" -> "Low-grade Foundation Building Pill";
            case "spirit_recovery_pill" -> "Spirit Recovery Pill";
            case "jiangchen_pill" -> "Jiangchen Pill";
            case "star_palace_tax_receipt" -> "Star Palace Tax Receipt";
            default -> "Story reward";
        };
    }

    private static Snapshot loadBuiltin() {
        JsonObject handbook = readJson(ROOT + "quest_handbook_i18n_v1.json");
        JsonObject hookRoot = readJson(ROOT + "quest_hooks.json");
        JsonObject chainRoot = readJson(ROOT + "quest_chains.json");

        Map<String, String> EnglishTitles = stringMap(object(handbook, "chain_titles_en"), false);
        Map<String, String> handbookHookLabels = stringMap(object(handbook, "hook_labels_zh"), true);
        Map<String, List<LocalizedLabel>> numericLabels = numericLabels(handbook);
        Map<String, HookPresentation> hooks = hooks(hookRoot, handbookHookLabels);
        Map<String, ChainPresentation> chains = new LinkedHashMap<>();
        Map<String, String> chainTitlesZh = chainTitlesZh(chainRoot);

        for (JsonElement element : array(chainRoot, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String id = normalize(str(chain, "id"));
            if (id.isBlank()) {
                continue;
            }
            String titleZh = safeZh(str(chain, "display"));
            if (titleZh.isBlank()) {
                titleZh = "未命名任务";
            }
            String titleEn = safeEnglish(EnglishTitles.get(id));
            if (titleEn.isBlank()) {
                titleEn = "Unnamed Quest";
            }

            List<StagePresentation> stages = stages(chain, hooks, numericLabels.get(id));
            JsonObject start = object(object(chain, "learn_requirements"), "start");
            String region = firstNonBlank(str(start, "region"), str(chain, "region"));
            String realm = firstNonBlank(str(start, "realm_min"), firstString(chain.get("realm_span")));
            String faction = str(start, "faction");
            String loreZh = safeZh(str(object(chain, "setting"), "lore"));
            String descriptionZh = chainDescriptionZh(titleZh, loreZh, stages);
            String descriptionEn = chainDescriptionEn(titleEn, stages);

            chains.put(id, new ChainPresentation(id, titleZh, titleEn, descriptionZh, descriptionEn,
                    normalize(region), normalize(realm), normalize(faction), List.copyOf(stages),
                    stringList(chain.get("rewards_finale")),
                    chainRequirements(chain, chainTitlesZh, EnglishTitles)));
        }
        return new Snapshot(Collections.unmodifiableMap(chains));
    }

    private static List<StagePresentation> stages(JsonObject chain,
                                                   Map<String, HookPresentation> hooks,
                                                   List<LocalizedLabel> numericLabels) {
        JsonElement raw = chain.get("steps");
        int numericCount = raw != null && raw.isJsonPrimitive() ? asInt(raw, 0) : 0;
        JsonArray steps = raw != null && raw.isJsonArray() ? raw.getAsJsonArray() : new JsonArray();
        int count = Math.max(numericCount, steps.size());
        List<StagePresentation> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            JsonElement rawStep = index < steps.size() ? steps.get(index) : null;
            JsonObject step = rawStep != null && rawStep.isJsonObject()
                    ? rawStep.getAsJsonObject() : null;
            String hookId = step == null
                    ? primitiveString(rawStep)
                    : normalize(str(step, "hook"));
            HookPresentation hook = hooks.get(hookId);
            LocalizedLabel numeric = numericLabels != null && index < numericLabels.size()
                    ? numericLabels.get(index) : null;

            String authoredSummary = safeZh(str(step, "summary"));
            String titleZh = firstNonBlank(authoredSummary,
                    numeric == null ? "" : numeric.zh(),
                    hook == null ? "" : hook.labelZh());
            if (titleZh.isBlank()) {
                titleZh = "第" + (index + 1) + "阶段";
            }
            String titleEn = numeric == null ? "" : safeEnglish(numeric.en());
            if (titleEn.isBlank()) {
                titleEn = "Stage " + (index + 1);
            }
            String summaryZh = firstNonBlank(authoredSummary,
                    hook == null ? "" : hook.summaryZh(), titleZh);
            String summaryEn = numeric == null ? "" : safeEnglish(numeric.en());
            if (summaryEn.isBlank()) {
                summaryEn = "Complete the next story objective.";
            }
            result.add(new StagePresentation(index + 1, hookId, titleZh, titleEn,
                    summaryZh, summaryEn, bool(step, "optional"), bool(step, "boss"),
                    stageRequirements(step)));
        }
        return result;
    }

    private static Map<String, String> chainTitlesZh(JsonObject chainRoot) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonElement element : array(chainRoot, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String id = normalize(str(chain, "id"));
            String title = safeZh(str(chain, "display"));
            if (!id.isBlank() && !title.isBlank()) {
                result.put(id, title);
            }
        }
        return result;
    }

    private static List<RequirementPresentation> chainRequirements(
            JsonObject chain, Map<String, String> titlesZh, Map<String, String> titlesEn) {
        List<RequirementPresentation> result = new ArrayList<>();
        String path = normalize(firstString(chain == null ? null : chain.get("requires")));
        if (!path.isBlank()) {
            addRequirement(result,
                    switch (path) {
                        case "ghost", "ghost_cultivator" -> "需踏上鬼修道途";
                        default -> "需满足指定修行道途";
                    },
                    switch (path) {
                        case "ghost", "ghost_cultivator" -> "Requires the ghost cultivation path";
                        default -> "Requires the specified cultivation path";
                    }, true);
        }
        String race = normalize(str(chain, "race_required"));
        if (!race.isBlank()) {
            addRequirement(result,
                    "mulan_fashi".equals(race) ? "需具备慕兰法士身份" : "需具备指定修行身份",
                    "mulan_fashi".equals(race) ? "Requires Mulan mage identity" : "Requires the specified identity",
                    true);
        }
        String karma = normalize(str(chain, "karma_required"));
        if (!karma.isBlank()) {
            addRequirement(result,
                    "demonic_karma".equals(karma) ? "需具备魔道因果" : "需满足指定因果条件",
                    "demonic_karma".equals(karma) ? "Requires demonic karma" : "Requires the specified karma condition",
                    false);
        }
        String parent = normalize(str(chain, "parent_chain"));
        if (!parent.isBlank()) {
            addRequirement(result,
                    "需先完成“" + chainTitle(parent, titlesZh, true) + "”",
                    "Complete " + chainTitle(parent, titlesEn, false) + " first", true);
        }
        String extension = normalize(str(chain, "extends_chain"));
        if (!extension.isBlank()) {
            addRequirement(result,
                    "剧情承接“" + chainTitle(extension, titlesZh, true) + "”",
                    "Continues " + chainTitle(extension, titlesEn, false), false);
        }
        appendConstraintRequirements(result, object(chain, "constraints"));
        return List.copyOf(result);
    }

    private static List<RequirementPresentation> stageRequirements(JsonObject step) {
        if (step == null) {
            return List.of();
        }
        List<RequirementPresentation> result = new ArrayList<>();
        String branch = normalize(str(step, "requires_branch"));
        if (!branch.isBlank()) {
            String zh = switch (branch) {
                case "rebel" -> "需选择逆星盟路线";
                case "loyalist" -> "需选择星宫路线";
                default -> "需选择指定剧情路线";
            };
            String en = switch (branch) {
                case "rebel" -> "Requires the Inverse Star Alliance route";
                case "loyalist" -> "Requires the Star Palace route";
                default -> "Requires the specified story route";
            };
            addRequirement(result, zh, en, false);
        }
        String required = normalize(firstString(step.get("requires")));
        if (!required.isBlank()) {
            addRequirement(result,
                    "m4_holy_bird_mulan".equals(required) ? "需先完成慕兰圣禽见闻" : "需先完成指定剧情前置",
                    "m4_holy_bird_mulan".equals(required)
                            ? "Requires the Mulan sacred-bird chronicle" : "Requires the specified story prerequisite",
                    false);
        }
        List<String> branches = stringList(step.get("branch_any"));
        if (!branches.isEmpty()) {
            List<String> zhNames = branches.stream().map(QuestPresentationService::branchChoiceZh).toList();
            List<String> enNames = branches.stream().map(QuestPresentationService::branchChoiceEn).toList();
            addRequirement(result,
                    "需在" + String.join("、", zhNames) + "中选择一支入门",
                    "Join one of " + String.join(", ", enNames), false);
        }
        return List.copyOf(result);
    }

    private static void appendConstraintRequirements(List<RequirementPresentation> result, JsonObject constraints) {
        int partyMax = positiveInt(constraints, "party_size_max");
        if (partyMax > 0) {
            addRequirement(result, "队伍人数不超过 " + partyMax + " 人",
                    "Party size at most " + partyMax, false);
        }
        int cycleYears = positiveInt(constraints, "cycle_years");
        if (cycleYears > 0) {
            addRequirement(result, "剧情周期为 " + cycleYears + " 年",
                    "Story cycle spans " + cycleYears + " years", false);
        }
        String suggestedRealm = normalize(str(constraints, "realm_suggested"));
        if (!suggestedRealm.isBlank()) {
            addRequirement(result, "建议境界达到" + realmName(suggestedRealm, true),
                    "Recommended realm: " + realmName(suggestedRealm, false), false);
        }
        String minimumRealm = normalize(str(constraints, "realm_min"));
        if (!minimumRealm.isBlank()) {
            addRequirement(result, "场景标注最低境界为" + realmName(minimumRealm, true),
                    "Scenario minimum realm: " + realmName(minimumRealm, false), false);
        }
        if (bool(constraints, "miasma_debuff")) {
            addRequirement(result, "需应对魔瘴侵蚀", "Prepare for demonic miasma", false);
        }
        if (bool(constraints, "cold_debuff")) {
            addRequirement(result, "需应对极寒侵蚀", "Prepare for extreme cold", false);
        }
        if (bool(constraints, "puppet_waves")) {
            addRequirement(result, "需应对多轮傀儡守卫", "Prepare for multiple puppet waves", false);
        }
    }

    private static void addRequirement(List<RequirementPresentation> result,
                                       String zh, String en, boolean enforced) {
        if (result == null || zh == null || zh.isBlank() || en == null || en.isBlank()) {
            return;
        }
        result.add(new RequirementPresentation(zh, en, enforced));
    }

    private static String chainTitle(String id, Map<String, String> titles, boolean chinese) {
        String title = titles == null ? "" : titles.getOrDefault(normalize(id), "");
        if (title != null && !title.isBlank()) {
            return title;
        }
        return chinese ? "前置任务线" : "the prerequisite quest line";
    }

    private static String branchChoiceZh(String raw) {
        return switch (normalize(raw)) {
            case "guiling_gate" -> "鬼灵门";
            case "hehuan_sect" -> "合欢宗";
            case "tianmo_sect" -> "天魔宗";
            case "qingluo_sect" -> "青罗宗";
            case "wanhu_sect" -> "万狐宗";
            case "xuewu_sect" -> "血巫宗";
            default -> "指定宗门";
        };
    }

    private static String branchChoiceEn(String raw) {
        return switch (normalize(raw)) {
            case "guiling_gate" -> "Ghost Spirit Gate";
            case "hehuan_sect" -> "Hehuan Sect";
            case "tianmo_sect" -> "Heavenly Demon Sect";
            case "qingluo_sect" -> "Qingluo Sect";
            case "wanhu_sect" -> "Wanhu Sect";
            case "xuewu_sect" -> "Blood Shaman Sect";
            default -> "the specified sect";
        };
    }

    private static String realmName(String raw, boolean chinese) {
        return switch (normalize(raw)) {
            case "mortal" -> chinese ? "凡人" : "Mortal";
            case "qi_refining" -> chinese ? "炼气期" : "Qi Refining";
            case "foundation", "foundation_establishment" -> chinese ? "筑基期" : "Foundation Establishment";
            case "core_formation" -> chinese ? "结丹期" : "Core Formation";
            case "nascent_soul" -> chinese ? "元婴期" : "Nascent Soul";
            case "spirit_transformation" -> chinese ? "化神期" : "Spirit Transformation";
            case "void_refining" -> chinese ? "炼虚期" : "Void Refining";
            default -> chinese ? "指定境界" : "the specified realm";
        };
    }

    private static int positiveInt(JsonObject object, String key) {
        try {
            return object != null && object.has(key) ? Math.max(0, object.get(key).getAsInt()) : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static Map<String, HookPresentation> hooks(JsonObject hookRoot,
                                                       Map<String, String> handbookLabels) {
        Map<String, HookPresentation> hooks = new LinkedHashMap<>();
        for (JsonElement element : array(hookRoot, "hooks")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject hook = element.getAsJsonObject();
            String id = normalize(str(hook, "id"));
            if (id.isBlank()) {
                continue;
            }
            String label = firstNonBlank(handbookLabels.get(id), safeZh(str(hook, "display")));
            String summary = safeZh(str(object(hook, "setting"), "lore"));
            hooks.put(id, new HookPresentation(label, summary));
        }
        // Some authored chains intentionally use handbook-only milestones that have no
        // executable quest_hooks entry. Keep those labels visible instead of falling back to
        // the generic "第 N 阶段" placeholder in the journal.
        for (Map.Entry<String, String> entry : handbookLabels.entrySet()) {
            String id = normalize(entry.getKey());
            String label = safeZh(entry.getValue());
            if (!id.isBlank() && !label.isBlank()) {
                hooks.putIfAbsent(id, new HookPresentation(label, ""));
            }
        }
        return hooks;
    }

    private static Map<String, List<LocalizedLabel>> numericLabels(JsonObject handbook) {
        Map<String, List<LocalizedLabel>> result = new LinkedHashMap<>();
        JsonObject root = object(handbook, "numeric_stage_labels");
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            List<LocalizedLabel> labels = new ArrayList<>();
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject label = element.getAsJsonObject();
                labels.add(new LocalizedLabel(safeZh(str(label, "zh")), safeEnglish(str(label, "en"))));
            }
            result.put(normalize(entry.getKey()), List.copyOf(labels));
        }
        return result;
    }

    private static String chainDescriptionZh(String title, String lore, List<StagePresentation> stages) {
        if (!lore.isBlank() && !lore.equals(title)) {
            return lore;
        }
        if (stages.isEmpty()) {
            return "循“" + title + "”展开历练，完成任务角色交付的目标。";
        }
        String first = stages.get(0).titleZh();
        String last = stages.get(stages.size() - 1).titleZh();
        return "从“" + first + "”起步，依次完成" + stages.size()
                + "段历练，最终达成“" + last + "”。";
    }

    private static String chainDescriptionEn(String title, List<StagePresentation> stages) {
        if (stages.isEmpty()) {
            return "Follow " + title + " and complete the objectives assigned by the quest guide.";
        }
        return "Follow " + title + " through " + stages.size()
                + " stages and report each completed objective to the bound quest guide.";
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = QuestPresentationService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return new JsonObject();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                return root.isJsonObject() ? root.getAsJsonObject() : new JsonObject();
            }
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonArray()
                ? root.getAsJsonArray(key) : new JsonArray();
    }

    private static JsonObject object(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonObject()
                ? root.getAsJsonObject(key) : new JsonObject();
    }

    private static String str(JsonObject root, String key) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) {
            return "";
        }
        try {
            return root.get(key).getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String primitiveString(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return "";
        }
        return normalize(value.getAsString());
    }

    private static boolean bool(JsonObject root, String key) {
        try {
            return root != null && root.has(key) && root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int asInt(JsonElement value, int fallback) {
        try {
            return value == null ? fallback : Math.max(0, value.getAsInt());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> stringList(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                try {
                    String text = element.getAsString().trim();
                    if (!text.isBlank()) {
                        result.add(text);
                    }
                } catch (Exception ignored) {
                    // Ignore structured reward rows; the native authority resolves the real grant.
                }
            }
        } else if (value.isJsonPrimitive()) {
            String text = value.getAsString().trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static String firstString(JsonElement value) {
        List<String> values = stringList(value);
        return values.isEmpty() ? "" : values.get(0);
    }

    private static Map<String, String> stringMap(JsonObject root, boolean chinese) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            try {
                String value = entry.getValue().getAsString();
                value = chinese ? safeZh(value) : safeEnglish(value);
                if (!value.isBlank()) {
                    result.put(normalize(entry.getKey()), value);
                }
            } catch (Exception ignored) {
                // Skip malformed handbook entries.
            }
        }
        return result;
    }

    private static String safeZh(String value) {
        return PlayerDisplayText.sanitizeCatalogText(value);
    }

    private static String safeEnglish(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.contains("_") || trimmed.matches("[a-z0-9./:-]+")) {
            return "";
        }
        return trimmed;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record HookPresentation(String labelZh, String summaryZh) {}

    private record LocalizedLabel(String zh, String en) {}
}
