package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Runtime authority for quest fields not represented by the thin quest index. */
public final class QuestAuthorityCatalog {
    private static final boolean[] RULES_LOAD_OK = { true };
    private static final Map<String, ChainRule> RULES = loadBuiltin();
    private static final boolean RULES_LOAD_FAILED = !RULES_LOAD_OK[0];

    public enum Gate {
        OPEN,
        KARMA,
        PARENT,
        REALM,
        PARTY,
        BRANCH,
        PREREQUISITE,
        FACTION,
        LOAD_FAILED
    }

    public record StageRule(int stage, String requiresBranch, String prerequisite, List<String> branchAny) {}

    public record ChainRule(
            String chainId,
            String karmaRequired,
            String extendsChain,
            int partySizeMax,
            String constraintRealmMin,
            Map<Integer, StageRule> stages) {}

    private QuestAuthorityCatalog() {}

    public static Optional<ChainRule> find(String chainId) {
        return Optional.ofNullable(RULES.get(normalize(chainId)));
    }

    public static Gate startGate(ServerPlayer player, String chainId) {
        if (RULES_LOAD_FAILED) {
            return Gate.LOAD_FAILED;
        }
        ChainRule rule = RULES.get(normalize(chainId));
        if (player == null || rule == null || player.getAbilities().instabuild) {
            return Gate.OPEN;
        }
        if (!rule.karmaRequired().isBlank() && !meetsKarma(player, rule.karmaRequired())) {
            return Gate.KARMA;
        }
        if (!rule.extendsChain().isBlank()
                && !TextQuestChainService.progressOf(player, rule.extendsChain()).complete()) {
            return Gate.PARENT;
        }
        if (!rule.constraintRealmMin().isBlank()) {
            boolean realm = CultivationHelper.get(player)
                    .map(cultivation -> WorldpackGameplayService.meetsMinRealm(
                            cultivation.getRealm(), rule.constraintRealmMin()))
                    .orElse(false);
            if (!realm) {
                return Gate.REALM;
            }
        }
        if (rule.partySizeMax() > 0 && nearbyPartySize(player) > rule.partySizeMax()) {
            return Gate.PARTY;
        }
        return Gate.OPEN;
    }

    public static Gate stageGate(ServerPlayer player, String chainId, int targetStage) {
        if (RULES_LOAD_FAILED) {
            return Gate.LOAD_FAILED;
        }
        ChainRule rule = RULES.get(normalize(chainId));
        StageRule stage = rule == null ? null : rule.stages().get(targetStage);
        if (player == null || stage == null || player.getAbilities().instabuild) {
            return Gate.OPEN;
        }
        if (!stage.requiresBranch().isBlank()
                && !matchesBranch(TextQuestChainService.getBranch(player, chainId), stage.requiresBranch())) {
            return Gate.BRANCH;
        }
        if (!stage.prerequisite().isBlank() && !meetsPrerequisite(player, stage.prerequisite())) {
            return Gate.PREREQUISITE;
        }
        if (!stage.branchAny().isEmpty()) {
            String faction = CultivationHelper.get(player)
                    .map(cultivation -> SectDefinitionService.canonicalizeSectId(
                            cultivation.getSevenMysteriesQuest().getSectId()))
                    .orElse("");
            boolean matches = stage.branchAny().stream()
                    .map(SectDefinitionService::canonicalizeSectId)
                    .anyMatch(faction::equals);
            if (!matches) {
                return Gate.FACTION;
            }
        }
        return Gate.OPEN;
    }

    private static boolean meetsKarma(ServerPlayer player, String karma) {
        String key = normalize(karma);
        if ("demonic_karma".equals(key)) {
            return ReputationService.get(player, "demonic_path") > 0
                    || NpcDialogueFlags.hasFlag(player, key)
                    || TextQuestChainService.BRANCH_DEMONIC.equals(findAnyCommittedBranch(player));
        }
        return NpcDialogueFlags.hasFlag(player, key);
    }

    private static String findAnyCommittedBranch(ServerPlayer player) {
        for (String chainId : RULES.keySet()) {
            String branch = TextQuestChainService.getBranch(player, chainId);
            if (!branch.isBlank() && !TextQuestChainService.BRANCH_NEUTRAL.equals(branch)) {
                return branch;
            }
        }
        return "";
    }

    private static boolean meetsPrerequisite(ServerPlayer player, String prerequisite) {
        String id = normalize(prerequisite);
        if (TextQuestChainService.find(id).isPresent()) {
            return TextQuestChainService.progressOf(player, id).complete();
        }
        return ChronicleTradeSoftService.hasDiscovered(player, id)
                || NpcDialogueFlags.hasFlag(player, id)
                || NpcDialogueFlags.hasFlag(player, "quest_progress_" + id);
    }

    private static boolean matchesBranch(String current, String required) {
        String branch = normalize(current);
        return switch (normalize(required)) {
            case "rebel" -> TextQuestChainService.BRANCH_DEMONIC.equals(branch);
            case "loyalist" -> TextQuestChainService.BRANCH_RIGHTEOUS.equals(branch);
            default -> normalize(required).equals(branch);
        };
    }

    private static int nearbyPartySize(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(player.blockPosition()).inflate(64.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()).size();
    }

    private static Map<String, ChainRule> loadBuiltin() {
        String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/quest_chains.json";
        try (InputStream stream = QuestAuthorityCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                RULES_LOAD_OK[0] = false;
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (Exception exception) {
            RULES_LOAD_OK[0] = false;
            SeekingImmortalsMod.LOGGER.error("Failed to load quest authority rules from {}", path, exception);
            return Map.of();
        }
    }

    static Map<String, ChainRule> parse(JsonObject root) {
        JsonArray chains = root == null ? null : root.getAsJsonArray("chains");
        if (chains == null) {
            return Map.of();
        }
        Map<String, ChainRule> rules = new LinkedHashMap<>();
        for (JsonElement element : chains) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject chain = element.getAsJsonObject();
            String id = normalize(str(chain, "id"));
            if (id.isBlank()) {
                continue;
            }
            JsonObject constraints = object(chain, "constraints");
            Map<Integer, StageRule> stages = new LinkedHashMap<>();
            JsonArray steps = chain.has("steps") && chain.get("steps").isJsonArray()
                    ? chain.getAsJsonArray("steps") : null;
            if (steps != null) {
                for (int i = 0; i < steps.size(); i++) {
                    if (!steps.get(i).isJsonObject()) {
                        continue;
                    }
                    JsonObject step = steps.get(i).getAsJsonObject();
                    boolean optional = asBool(step, "optional");
                    String branch = normalize(str(step, "requires_branch"));
                    String prerequisite = normalize(firstString(step.get("requires")));
                    List<String> branchAny = strings(step, "branch_any");
                    // Authored-optional steps must not hard-gate the chain with their requires.
                    if (!optional && (!branch.isBlank() || !prerequisite.isBlank() || !branchAny.isEmpty())) {
                        stages.put(i + 1, new StageRule(i + 1, branch, prerequisite, branchAny));
                    }
                }
            }
            rules.put(id, new ChainRule(
                    id,
                    normalize(str(chain, "karma_required")),
                    normalize(str(chain, "extends_chain")),
                    integer(constraints, "party_size_max"),
                    normalize(str(constraints, "realm_min")),
                    Collections.unmodifiableMap(stages)));
        }
        return Collections.unmodifiableMap(rules);
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : null;
    }

    private static boolean asBool(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                && Boolean.parseBoolean(object.get(key).getAsString());
    }

    private static String str(JsonObject object, String key) {
        return object == null || !object.has(key) || object.get(key).isJsonNull()
                ? "" : object.get(key).getAsString();
    }

    private static int integer(JsonObject object, String key) {
        try {
            return object == null ? 0 : Math.max(0, object.get(key).getAsInt());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String firstString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray().isEmpty() ? "" : element.getAsJsonArray().get(0).getAsString();
        }
        return element.getAsString();
    }

    private static List<String> strings(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            String value = normalize(element.getAsString());
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
