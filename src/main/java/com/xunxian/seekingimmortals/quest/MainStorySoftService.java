package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmCatalogService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Main story chapters (schema 3 + map v145). Persistent stage flags.
 * M11: data-driven quest_chain_refs, unlock gates (realm/region/reputation/secret clear).
 */
public final class MainStorySoftService {
    private static final String ROOT = "seeking_immortals_main_story";
    private static final Map<String, List<String>> CHAPTER_CHAINS = buildChapterChains();
    private static final Map<String, UnlockGate> CHAPTER_GATES = buildChapterGates();

    public record UnlockGate(String realmMin, String region, String reputationFaction, int reputationMin,
                             String secretRealmClear, String ticketFlag) {}

    private MainStorySoftService() {}

    public static int chapterCount() {
        return ExtendedCatalogService.builtin().chapters().size();
    }

    public static List<String> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<String> lines = new ArrayList<>();
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            boolean done = root.getBoolean(chapter.id());
            List<String> refs = CHAPTER_CHAINS.getOrDefault(chapter.id(), chapter.questChainRefs());
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
                player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.unknown",
                        chapterDisplay(chapterId)), false);
            }
            return false;
        }
        ExtendedCatalogService.StoryChapter chapter = optional.get();
        if (!meetsUnlock(player, chapter.id(), loud)) {
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (root.getBoolean(chapter.id())) {
            if (loud) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.already",
                        chapterDisplay(chapter)), false);
            }
            return false;
        }
        root.putBoolean(chapter.id(), true);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.completed",
                chapterDisplay(chapter)), true);
        if (chapter.summary() != null && !chapter.summary().isBlank() && loud) {
            player.displayClientMessage(PlayerDisplayText.safeLiteral(chapter.summary(),
                    "message.seeking_immortals.main_story.summary_unavailable"), false);
        }
        TimelineChronicleService.onChronicleDiscovered(player, chapter.id());
        if (startLinked) {
            startLinkedChains(player, chapter.id());
        }
        return true;
    }

    /**
     * Wave457: starting a chapter can open the first linked text-quest chain if not active.
     * M11: enforces unlock gates before starting.
     */
    public static boolean startChapter(ServerPlayer player, String chapterId) {
        Optional<ExtendedCatalogService.StoryChapter> optional = findChapter(chapterId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.unknown",
                    chapterDisplay(chapterId)), false);
            return false;
        }
        if (isComplete(player, optional.get().id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.already",
                    chapterDisplay(optional.get())), false);
            return false;
        }
        if (!meetsUnlock(player, optional.get().id(), true)) {
            return false;
        }
        List<String> started = startLinkedChains(player, optional.get().id());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.main_story.started",
                chapterDisplay(optional.get()), started.size()), true);
        return true;
    }

    public static boolean meetsUnlock(ServerPlayer player, String chapterId, boolean warn) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        UnlockGate gate = CHAPTER_GATES.get(normalize(chapterId));
        if (gate == null) {
            return true;
        }
        boolean[] ok = {true};
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            if (gate.realmMin() != null && !gate.realmMin().isBlank()
                    && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), gate.realmMin())) {
                if (warn) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.main_story.realm_locked",
                            chapterDisplay(chapterId), ArtifactDisplayTexts.realm(gate.realmMin()),
                            ArtifactDisplayTexts.realm(cultivation.getRealm().name())), true);
                }
                ok[0] = false;
                return;
            }
            if (gate.region() != null && !gate.region().isBlank()) {
                String current = normalize(cultivation.getWorldpackCurrentRegionId());
                if (!gate.region().equals(current) && !current.contains(gate.region()) && !gate.region().contains(current)) {
                    if (warn) {
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.main_story.region_locked",
                                chapterDisplay(chapterId), regionDisplay(gate.region()),
                                regionDisplay(current)), true);
                    }
                    ok[0] = false;
                    return;
                }
            }
            if (gate.reputationFaction() != null && !gate.reputationFaction().isBlank() && gate.reputationMin() > 0) {
                int rep = ReputationService.get(player, gate.reputationFaction());
                if (rep < gate.reputationMin()) {
                    if (warn) {
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.main_story.rep_locked",
                                chapterDisplay(chapterId), factionDisplay(gate.reputationFaction()),
                                gate.reputationMin(), rep), true);
                    }
                    ok[0] = false;
                    return;
                }
            }
            if (gate.secretRealmClear() != null && !gate.secretRealmClear().isBlank()) {
                String realm = gate.secretRealmClear();
                CompoundTag core = player.getPersistentData().getCompound("seeking_immortals_secret_realm_core_clear");
                CompoundTag mid = player.getPersistentData().getCompound("seeking_immortals_secret_realm_mid_clear");
                if (!core.getBoolean(realm) && !mid.getBoolean(realm)) {
                    if (warn) {
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.main_story.secret_locked",
                                chapterDisplay(chapterId), secretRealmDisplay(realm)), true);
                    }
                    ok[0] = false;
                    return;
                }
            }
            if (gate.ticketFlag() != null && !gate.ticketFlag().isBlank()) {
                if (!com.xunxian.seekingimmortals.npc.NpcDialogueFlags.hasFlag(player, gate.ticketFlag())
                        && !player.getPersistentData().getCompound("seeking_immortals_quest_authority_rewards")
                        .getBoolean(gate.ticketFlag())) {
                    // Soft: do not hard-block chapter_2 if ticket missing; warn only.
                    if (warn) {
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.main_story.ticket_missing"), false);
                    }
                }
            }
        }, () -> ok[0] = false);
        return ok[0];
    }

    private static List<String> startLinkedChains(ServerPlayer player, String chapterId) {
        List<String> started = new ArrayList<>();
        for (String chainId : chainsForChapter(chapterId)) {
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
        String id = normalize(chapterId);
        List<String> fromMap = CHAPTER_CHAINS.get(id);
        if (fromMap != null && !fromMap.isEmpty()) {
            return fromMap;
        }
        ExtendedCatalogService.StoryChapter chapter = ExtendedCatalogService.builtin().chapters().get(id);
        if (chapter != null && chapter.questChainRefs() != null && !chapter.questChainRefs().isEmpty()) {
            return chapter.questChainRefs();
        }
        return List.of();
    }

    private static Optional<ExtendedCatalogService.StoryChapter> findChapter(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            return Optional.empty();
        }
        Optional<ExtendedCatalogService.StoryChapter> optional =
                Optional.ofNullable(ExtendedCatalogService.builtin().chapters().get(normalize(chapterId)));
        if (optional.isPresent()) {
            return optional;
        }
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            if (chapter != null && chapter.id() != null && chapter.id().equalsIgnoreCase(chapterId)) {
                return Optional.of(chapter);
            }
        }
        return Optional.empty();
    }

    private static Map<String, List<String>> buildChapterChains() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        // Prefer catalog quest_chain_refs (data-driven).
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            if (chapter.questChainRefs() != null && !chapter.questChainRefs().isEmpty()) {
                map.put(chapter.id(), List.copyOf(chapter.questChainRefs()));
            }
        }
        // Overlay main_story_quest_map_v145 legacy + playable refs when present.
        JsonObject mapRoot = readJson(path("catalog/main_story_quest_map_index.json"));
        if (mapRoot == null) {
            mapRoot = readJson(path("text_material/main_story_quest_map_v145.json"));
        }
        for (JsonElement element : array(mapRoot, "chapters")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) {
                continue;
            }
            List<String> refs = new ArrayList<>(map.getOrDefault(id, List.of()));
            for (String r : stringList(o.get("legacy_quest_chain_refs"))) {
                if (!refs.contains(r)) {
                    refs.add(r);
                }
            }
            // playable ids are recorded as soft targets even if not in 62-chain index
            for (String r : stringList(o.get("playable_quests"))) {
                if (!refs.contains(r) && TextQuestChainService.find(r).isPresent()) {
                    refs.add(r);
                }
            }
            if (!refs.isEmpty()) {
                map.put(id, List.copyOf(refs));
            }
        }
        // Hardcoded fallback for older indexes without refs.
        map.putIfAbsent("chapter_0_mortal", List.of("huangfeng_cultivation_path"));
        map.putIfAbsent("chapter_1_sect", List.of("huangfeng_cultivation_path", "mulan_war_campaign"));
        map.putIfAbsent("chapter_2_foundation_secret", List.of("blood_forbidden_campaign"));
        map.putIfAbsent("chapter_3_chaotic_sea", List.of("chaotic_sea_politics"));
        map.putIfAbsent("chapter_4_great_jin", List.of("void_great_cultivation_arc", "kunwu_mountain_campaign"));
        map.putIfAbsent("chapter_5_deity_transformation", List.of("fallen_demon_campaign"));
        map.putIfAbsent("chapter_6_spirit_realm", List.of("spirit_realm_rise", "spirit_realm_border"));
        return map;
    }

    private static Map<String, UnlockGate> buildChapterGates() {
        Map<String, UnlockGate> gates = new LinkedHashMap<>();
        // From main_story_chapters learn_requirements + M11 unlock rules.
        gates.put("chapter_0_mortal", new UnlockGate("", "", "", 0, "", ""));
        gates.put("chapter_1_sect", new UnlockGate("QI_REFINING", "", "mortal_realm", 0, "", ""));
        gates.put("chapter_2_foundation_secret", new UnlockGate("QI_REFINING", "", "", 0, "blood_forbidden", "blood_forbidden_token"));
        gates.put("chapter_3_chaotic_sea", new UnlockGate("FOUNDATION", "chaotic_sea", "chaotic_sea", 5, "", ""));
        gates.put("chapter_4_great_jin", new UnlockGate("CORE_FORMATION", "", "dajin", 5, "", ""));
        gates.put("chapter_5_deity_transformation", new UnlockGate("NASCENT_SOUL", "", "demonic_path", 0, "fallen_demon", ""));
        gates.put("chapter_6_spirit_realm", new UnlockGate("DEITY_TRANSFORMATION", "", "tianyuan", 10, "", "mortal_to_tianyuan"));
        // Overlay from full chapters file if present.
        JsonObject full = readJson(path("text_material/main_story_chapters.json"));
        for (JsonElement element : array(full, "chapters")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) {
                continue;
            }
            JsonObject req = o.has("learn_requirements") && o.get("learn_requirements").isJsonObject()
                    ? o.getAsJsonObject("learn_requirements") : new JsonObject();
            JsonObject progress = req.has("progress") && req.get("progress").isJsonObject()
                    ? req.getAsJsonObject("progress") : new JsonObject();
            String realmMin = str(progress, "realm_min");
            String region = str(progress, "region");
            String ticket = str(progress, "ticket");
            String gate = str(progress, "gate");
            UnlockGate existing = gates.getOrDefault(id, new UnlockGate("", "", "", 0, "", ""));
            gates.put(id, new UnlockGate(
                    realmMin.isBlank() ? existing.realmMin() : realmMin,
                    region.isBlank() ? existing.region() : region,
                    existing.reputationFaction(),
                    existing.reputationMin(),
                    existing.secretRealmClear(),
                    !ticket.isBlank() ? ticket : (!gate.isBlank() ? gate : existing.ticketFlag())
            ));
        }
        return gates;
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = MainStorySoftService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null) {
            return List.of();
        }
        if (element.isJsonPrimitive()) {
            return List.of(element.getAsString());
        }
        if (!element.isJsonArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            try {
                list.add(child.getAsString());
            } catch (Exception ignored) {
                list.add(String.valueOf(child));
            }
        }
        return List.copyOf(list);
    }

    private static Component chapterDisplay(String chapterId) {
        return findChapter(chapterId)
                .map(MainStorySoftService::chapterDisplay)
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_chapter"));
    }

    private static Component chapterDisplay(ExtendedCatalogService.StoryChapter chapter) {
        return chapter == null
                ? Component.translatable("text.seeking_immortals.unknown_chapter")
                : PlayerDisplayText.safeLiteral(chapter.display(), "text.seeking_immortals.unknown_chapter");
    }

    private static Component regionDisplay(String regionId) {
        return RegionRegistry.find(regionId)
                .map(region -> PlayerDisplayText.safeLiteral(
                        region.display(), "text.seeking_immortals.unknown_region"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_region"));
    }

    private static Component factionDisplay(String factionId) {
        String id = normalize(factionId);
        return switch (id) {
            case "mortal_realm" -> Component.translatable("text.seeking_immortals.faction.mortal_realm");
            case "chaotic_sea" -> Component.translatable("text.seeking_immortals.faction.chaotic_sea");
            case "dajin" -> Component.translatable("text.seeking_immortals.faction.dajin");
            case "demonic_path" -> Component.translatable("text.seeking_immortals.faction.demonic_path");
            case "tianyuan" -> Component.translatable("text.seeking_immortals.faction.tianyuan");
            case "mulan" -> Component.translatable("text.seeking_immortals.faction.mulan");
            case "righteous_alliance" -> Component.translatable("text.seeking_immortals.faction.righteous_alliance");
            case "merchant_guild" -> Component.translatable("text.seeking_immortals.faction.merchant_guild");
            default -> Component.translatable("text.seeking_immortals.unknown_faction");
        };
    }

    private static Component secretRealmDisplay(String realmId) {
        return SecretRealmCatalogService.find(realmId)
                .map(realm -> PlayerDisplayText.safeLiteral(
                        realm.display(), "text.seeking_immortals.unknown_secret_realm"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_secret_realm"));
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
