package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

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
 * M08 master sect data from {@code sects.json} + {@code sect_specialty_map}.
 * Entry conditions consume M01 {@link ProgressionGateApi}.
 */
public final class SectMasterDataService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private SectMasterDataService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<SectMaster> find(String sectId) {
        String id = SectDefinitionService.canonicalizeSectId(sectId);
        SectMaster master = BUILTIN.sects().get(normalize(id));
        if (master != null) {
            return Optional.of(master);
        }
        return Optional.ofNullable(BUILTIN.sects().get(normalize(sectId)));
    }

    public static Optional<Specialty> specialty(String sectId) {
        String canonical = normalize(SectDefinitionService.canonicalizeSectId(sectId));
        Specialty specialty = BUILTIN.specialties().get(canonical);
        if (specialty != null) {
            return Optional.of(specialty);
        }
        specialty = BUILTIN.specialties().get(normalize(sectId));
        if (specialty != null) {
            return Optional.of(specialty);
        }
        return find(sectId).map(SectMaster::specialty);
    }

    /**
     * Server-side entry check combining quest-side gates (caller) and corpus/M01 gates.
     */
    public static EntryCheck canEnter(Player player, String sectId) {
        Optional<SectMaster> optional = find(sectId);
        if (optional.isEmpty()) {
            // Unknown to corpus: allow playable hardcoded defs (legacy).
            return EntryCheck.allow();
        }
        SectMaster master = optional.get();
        if (player == null) {
            return EntryCheck.deny("no_player");
        }

        // Ghost ban first — righteous/corpus forbidden.
        if (GhostSectBanService.isJoinRejected(player, master.id())) {
            return EntryCheck.deny("ghost_ban");
        }

        if (!master.forbiddenPaths().isEmpty()) {
            String path = "";
            if (player instanceof ServerPlayer serverPlayer) {
                path = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(serverPlayer)
                        .map(PlayerCultivation::getCultivationPathId)
                        .orElse("");
            }
            String normalizedPath = normalize(path);
            for (String forbidden : master.forbiddenPaths()) {
                String f = normalize(forbidden);
                if (f.isBlank()) continue;
                if (normalizedPath.contains(f) || f.contains(normalizedPath) && !normalizedPath.isBlank()) {
                    return EntryCheck.deny("forbidden_path:" + forbidden);
                }
                if ((f.contains("ghost") || f.contains("demonic"))
                        && player instanceof ServerPlayer sp
                        && com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(sp)
                        .map(PlayerCultivation::isGhostPath).orElse(false)) {
                    return EntryCheck.deny("forbidden_path:" + forbidden);
                }
            }
        }

        if (!master.joinRealmMax().isBlank()) {
            // join_realm_max means applicant realm must not exceed this (classic outer-disciple recruitment).
            Realm max = Realm.fromDesignId(master.joinRealmMax());
            if (max != null && player instanceof ServerPlayer serverPlayer) {
                Optional<PlayerCultivation> cultivation = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(serverPlayer);
                if (cultivation.isPresent() && cultivation.get().getRealm().ordinal() > max.ordinal()) {
                    return EntryCheck.deny("realm_too_high:" + master.joinRealmMax());
                }
            }
        }

        if (!master.joinRealmMin().isBlank()
                && !ProgressionGateApi.meetsRealm(player, master.joinRealmMin())) {
            return EntryCheck.deny("realm_too_low:" + master.joinRealmMin());
        }
        if (!master.joinRoot().isBlank()
                && !ProgressionGateApi.meetsRoot(player, master.joinRoot())) {
            return EntryCheck.deny("root:" + master.joinRoot());
        }
        if (!master.joinPath().isBlank()
                && !ProgressionGateApi.meetsPath(player, master.joinPath())) {
            return EntryCheck.deny("path:" + master.joinPath());
        }
        return EntryCheck.allow();
    }

    private static Snapshot loadBuiltin() {
        Map<String, SectMaster> sects = new LinkedHashMap<>();
        Map<String, Specialty> specialties = new LinkedHashMap<>();

        JsonObject specialtyRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/sect_specialty_map.json");
        if (specialtyRoot != null) {
            JsonArray arr = specialtyRoot.getAsJsonArray("sects");
            if (arr != null) {
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    List<String> secondary = stringList(o.get("secondary"));
                    List<String> primary = stringList(o.get("specialty"));
                    if (primary.isEmpty() && o.has("specialty") && o.get("specialty").isJsonPrimitive()) {
                        primary = List.of(o.get("specialty").getAsString());
                    }
                    JsonObject gameplay = o.has("gameplay") && o.get("gameplay").isJsonObject()
                            ? o.getAsJsonObject("gameplay") : new JsonObject();
                    List<MethodGrant> methodGrants = new ArrayList<>();
                    JsonArray grants = gameplay.getAsJsonArray("method_grants");
                    if (grants != null) {
                        for (JsonElement grantElement : grants) {
                            if (!grantElement.isJsonObject()) continue;
                            JsonObject grant = grantElement.getAsJsonObject();
                            String methodId = normalize(str(grant, "method"));
                            if (!methodId.isBlank()) {
                                methodGrants.add(new MethodGrant(methodId, intOr(grant, "stage", 2)));
                            }
                        }
                    }
                    Specialty specialty = new Specialty(id, str(o, "display"), primary, secondary,
                            str(o, "craft_loop_ref"), str(o, "skill_tree_ref"),
                            methodGrants,
                            intOr(gameplay, "shop_discount_percent", 0),
                            intOr(gameplay, "mission_contribution_bonus", 0),
                            normalize(str(gameplay, "mission_skill")));
                    specialties.put(id, specialty);
                    specialties.put(SectDefinitionService.canonicalizeSectId(id), specialty);
                }
            }
        }

        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/sects.json");
        if (root != null) {
            JsonArray arr = root.getAsJsonArray("sects");
            if (arr != null) {
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = normalize(str(o, "id"));
                    if (id.isBlank()) continue;
                    String canonical = SectDefinitionService.canonicalizeSectId(id);
                    List<String> specialtyTags = stringList(o.get("specialty"));
                    Specialty specialty = specialties.getOrDefault(canonical,
                            specialties.getOrDefault(id, new Specialty(canonical, str(o, "display"), specialtyTags,
                                    List.of(), "", "", List.of(), 0, 0, "")));
                    String joinRealmMin = "";
                    String joinRoot = "";
                    String joinPath = "";
                    if (o.has("setting") && o.get("setting").isJsonObject()) {
                        JsonObject setting = o.getAsJsonObject("setting");
                        if (setting.has("join_requirements") && setting.get("join_requirements").isJsonObject()) {
                            JsonObject jr = setting.getAsJsonObject("join_requirements");
                            joinRealmMin = firstNonBlank(str(jr, "realm_min"), str(jr, "min_realm"));
                            joinRoot = str(jr, "root");
                            joinPath = str(jr, "path");
                        }
                    }
                    if (o.has("learn_requirements") && o.get("learn_requirements").isJsonObject()) {
                        JsonObject lr = o.getAsJsonObject("learn_requirements");
                        if (lr.has("join") && lr.get("join").isJsonObject()) {
                            JsonObject join = lr.getAsJsonObject("join");
                            joinRealmMin = firstNonBlank(joinRealmMin, str(join, "realm_min"), str(join, "min_realm"));
                            joinRoot = firstNonBlank(joinRoot, str(join, "root"));
                            joinPath = firstNonBlank(joinPath, str(join, "path"));
                        }
                    }
                    SectMaster master = new SectMaster(
                            canonical,
                            str(o, "display"),
                            str(o, "region"),
                            str(o, "alignment"),
                            specialtyTags,
                            str(o, "join_realm_max"),
                            joinRealmMin,
                            joinRoot,
                            joinPath,
                            str(o, "reputation_id"),
                            str(o, "shop_tier"),
                            stringList(o.get("forbidden_paths")),
                            specialty);
                    sects.put(canonical, master);
                    sects.putIfAbsent(id, master);
                }
            }
        }
        return new Snapshot(Collections.unmodifiableMap(sects), Collections.unmodifiableMap(specialties));
    }

    private static List<String> stringList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return list;
        }
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SectMasterDataService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load sect master data {}", path, exception);
            return null;
        }
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record MethodGrant(String methodId, int stage) {
        public MethodGrant {
            methodId = normalize(methodId);
            stage = Math.max(SectContributionService.STAGE_OUTER_DISCIPLE,
                    Math.min(SectContributionService.STAGE_PHASE10_COMPLETE, stage));
        }
    }

    public record Specialty(String sectId, String display, List<String> primary, List<String> secondary,
                            String craftLoopRef, String skillTreeRef, List<MethodGrant> methodGrants,
                            int shopDiscountPercent, int missionContributionBonus, String missionSkill) {
        public Specialty {
            primary = primary == null ? List.of() : List.copyOf(primary);
            secondary = secondary == null ? List.of() : List.copyOf(secondary);
            methodGrants = methodGrants == null ? List.of() : List.copyOf(methodGrants);
            shopDiscountPercent = Math.max(0, Math.min(25, shopDiscountPercent));
            missionContributionBonus = Math.max(0, Math.min(100, missionContributionBonus));
            missionSkill = normalize(missionSkill);
        }
    }

    public record SectMaster(String id, String display, String region, String alignment, List<String> specialtyTags,
                             String joinRealmMax, String joinRealmMin, String joinRoot, String joinPath,
                             String reputationId, String shopTier, List<String> forbiddenPaths,
                             Specialty specialty) {}

    public record EntryCheck(boolean allowed, String reason) {
        public static EntryCheck allow() {
            return new EntryCheck(true, "");
        }

        public static EntryCheck deny(String reason) {
            return new EntryCheck(false, reason == null ? "denied" : reason);
        }
    }

    public record Snapshot(Map<String, SectMaster> sects, Map<String, Specialty> specialties) {
        public int sectCount() {
            return (int) sects.values().stream().map(SectMaster::id).distinct().count();
        }
    }
}
