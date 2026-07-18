package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Wave466: realm + light sect gates.
 * Wave467: requires_method (sect membership or learned method) + light faction rep gates.
 * Wave468: cast-path region/dimension affinity for high-tier tagged arts.
 */
public final class TechniqueGateService {
    private static final Map<String, String> REQUIRES_METHOD = loadRequiresMethod();
    private static final Map<String, String> METHOD_HOME_SECT = buildMethodHomeSects();
    private static final Map<String, String> METHOD_HOME_REP = buildMethodHomeRep();

    private TechniqueGateService() {}

    public record GateResult(boolean allowed, String messageKey, Object[] args) {
        public static GateResult ok() {
            return new GateResult(true, "", new Object[0]);
        }

        public static GateResult deny(String messageKey, Object... args) {
            return new GateResult(false, messageKey, args == null ? new Object[0] : args);
        }
    }

    public static GateResult canLearn(ServerPlayer player, PlayerCultivation cultivation,
                                      TechniqueDataManager.TechniqueEntry technique) {
        return evaluate(player, cultivation, technique, false);
    }

    public static GateResult canCast(ServerPlayer player, PlayerCultivation cultivation,
                                     TechniqueDataManager.TechniqueEntry technique) {
        return canCast(player, cultivation, technique, StatusRegistry.blocksTechnique(player));
    }

    static GateResult canCast(ServerPlayer player, PlayerCultivation cultivation,
                              TechniqueDataManager.TechniqueEntry technique, boolean statusBlocked) {
        if (statusBlocked) {
            return GateResult.deny("message.seeking_immortals.technique_gate.status_blocked");
        }
        return evaluate(player, cultivation, technique, true);
    }

    private static GateResult evaluate(ServerPlayer player, PlayerCultivation cultivation,
                                       TechniqueDataManager.TechniqueEntry technique, boolean castPath) {
        if (player != null && player.getAbilities().instabuild) {
            return GateResult.ok();
        }
        if (technique == null) {
            return GateResult.ok();
        }
        GateResult realm = checkRealm(cultivation, technique);
        if (!realm.allowed()) {
            return realm;
        }
        // M02: secret/ultimate (神通/禁术) hard-require their declared realm on both learn and cast.
        if (isHighTierArt(technique)) {
            GateResult highTier = checkRealm(cultivation, technique);
            if (!highTier.allowed()) {
                return highTier;
            }
        }
        GateResult sect = checkSectHint(cultivation, technique);
        if (!sect.allowed()) {
            return sect;
        }
        GateResult method = checkMethod(player, cultivation, technique);
        if (!method.allowed()) {
            return method;
        }
        if (castPath) {
            GateResult rep = checkReputation(player, technique);
            if (!rep.allowed()) {
                return rep;
            }
            GateResult region = checkRegion(player, cultivation, technique);
            if (!region.allowed()) {
                return region;
            }
        }
        return GateResult.ok();
    }

    private static boolean isHighTierArt(TechniqueDataManager.TechniqueEntry technique) {
        String tier = technique.tier() == null ? "" : technique.tier().toLowerCase(Locale.ROOT);
        String type = technique.effectType() == null ? "" : technique.effectType().toLowerCase(Locale.ROOT);
        String blob = (technique.id() + " " + technique.source() + " " + technique.name()).toLowerCase(Locale.ROOT);
        return tier.contains("secret") || type.contains("secret") || type.contains("ultimate")
                || blob.contains("禁术") || blob.contains("神通") || blob.contains("forbidden");
    }

    /**
     * Wave468 cast-only: high-tier arts with clear region tags must be cast in matching
     * worldpack region / secret realm / dimension. Unspecified arts stay open.
     */
    private static GateResult checkRegion(ServerPlayer player, PlayerCultivation cultivation,
                                          TechniqueDataManager.TechniqueEntry technique) {
        if (player == null || cultivation == null || technique == null) {
            return GateResult.ok();
        }
        Realm required = technique.requiredRealm();
        if (required == null || required.ordinal() < Realm.CORE_FORMATION.ordinal()) {
            return GateResult.ok();
        }
        String blob = normalize(technique.id()) + " " + normalize(technique.source())
                + " " + normalize(technique.attribute());
        String need = inferRegionTag(blob);
        if (need.isBlank()) {
            return GateResult.ok();
        }
        String where = currentLocationBlob(player, cultivation);
        if (locationMatches(where, need)) {
            return GateResult.ok();
        }
        return GateResult.deny("message.seeking_immortals.technique_gate.region",
                technique.name().isBlank() ? technique.id() : technique.name(),
                need,
                summarizeLocation(where));
    }

    private static String inferRegionTag(String blob) {
        if (blob.contains("yin") || blob.contains("nether") || blob.contains("ghost")
                || blob.contains("xuan_yin") || blob.contains("underworld") || blob.contains("阴")
                || blob.contains("鬼") || blob.contains("冥")) {
            return "yin_underworld";
        }
        if (blob.contains("fallen_demon") || blob.contains("demon_rift") || blob.contains("asura")
                || blob.contains("blood_forbidden") || blob.contains("魔渊") || blob.contains("修罗")) {
            return "demon_rift";
        }
        if (blob.contains("void_palace") || blob.contains("void") || blob.contains("chaotic")
                || blob.contains("star_palace") || blob.contains("inverse_star")
                || blob.contains("虚空") || blob.contains("乱星") || blob.contains("星宫")) {
            return "chaotic_void";
        }
        if (blob.contains("diyuan") || blob.contains("地渊") || blob.contains("kunwu") || blob.contains("昆吾")) {
            return "diyuan_kunwu";
        }
        if (blob.contains("spirit_realm") || blob.contains("tianyuan") || blob.contains("灵界") || blob.contains("天元")) {
            return "spirit_realm";
        }
        return "";
    }

    private static String currentLocationBlob(ServerPlayer player, PlayerCultivation cultivation) {
        String region = normalize(cultivation.getWorldpackCurrentRegionId());
        String realm = normalize(cultivation.getWorldpackActiveSecretRealmId());
        String dim = player.level() != null && player.level().dimension() != null
                ? normalize(player.level().dimension().location().toString())
                : "";
        return region + " " + realm + " " + dim;
    }

    private static boolean locationMatches(String where, String need) {
        return switch (need) {
            case "yin_underworld" -> containsAny(where, "yin", "nether", "ghost", "underworld", "yinming", "冥", "阴");
            case "demon_rift" -> containsAny(where, "demon", "fallen", "asura", "blood", "魔", "修罗");
            case "chaotic_void" -> containsAny(where, "void", "chaotic", "star", "inverse", "palace", "虚空", "乱星", "星");
            case "diyuan_kunwu" -> containsAny(where, "diyuan", "kunwu", "地渊", "昆吾");
            case "spirit_realm" -> containsAny(where, "spirit", "tianyuan", "fengyuan", "灵界", "天元");
            default -> true;
        };
    }

    private static String summarizeLocation(String where) {
        String trimmed = where == null ? "" : where.trim().replaceAll("\\s+", " ");
        if (trimmed.isBlank()) {
            return "-";
        }
        return trimmed.length() > 48 ? trimmed.substring(0, 48) + "…" : trimmed;
    }

    private static boolean containsAny(String text, String... tokens) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && text.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static GateResult checkRealm(PlayerCultivation cultivation, TechniqueDataManager.TechniqueEntry technique) {
        Realm required = technique.requiredRealm();
        if (required == null) {
            return GateResult.ok();
        }
        Realm current = cultivation.getRealm();
        if (current.ordinal() < required.ordinal()) {
            return GateResult.deny("message.seeking_immortals.technique_gate.realm",
                    technique.name().isBlank() ? technique.id() : technique.name(),
                    required.getDisplayName(),
                    current.getDisplayName());
        }
        return GateResult.ok();
    }

    private static GateResult checkSectHint(PlayerCultivation cultivation, TechniqueDataManager.TechniqueEntry technique) {
        Optional<String> requiredSect = inferSectId(technique);
        if (requiredSect.isEmpty()) {
            return GateResult.ok();
        }
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        String current = progress == null ? "" : normalize(progress.getSectId());
        if (current.isBlank()) {
            return GateResult.ok();
        }
        if (!current.equals(requiredSect.get()) && !current.contains(requiredSect.get()) && !requiredSect.get().contains(current)) {
            return GateResult.deny("message.seeking_immortals.technique_gate.sect",
                    technique.name().isBlank() ? technique.id() : technique.name(),
                    requiredSect.get(),
                    current);
        }
        return GateResult.ok();
    }

    /**
     * Wave467/473: if technique declares requires_method, allow when:
     * 0) Wave473: method explicitly learned via ManualCatalogService.learnMethod, or
     * 1) method id itself is learned as a technique, or
     * 2) player is outer+ disciple of the method's home sect, or
     * 3) any learned technique id/source contains the method token.
     */
    private static GateResult checkMethod(ServerPlayer player, PlayerCultivation cultivation,
                                          TechniqueDataManager.TechniqueEntry technique) {
        String method = technique.requiresMethod() == null ? "" : normalize(technique.requiresMethod());
        if (method.isBlank()) {
            method = REQUIRES_METHOD.getOrDefault(normalize(technique.id()), "");
        }
        if (method.isBlank()) {
            method = inferMethodFromBlob(technique);
        }
        if (method.isBlank()) {
            return GateResult.ok();
        }
        // Wave473: authoritative learned-method flag.
        if (player != null) {
            try {
                if (com.xunxian.seekingimmortals.catalog.ManualCatalogService.hasLearnedMethod(player, method)) {
                    return GateResult.ok();
                }
            } catch (Throwable ignored) {
                // optional in tests
            }
        }
        if (cultivation.hasLearnedTechnique(method)) {
            return GateResult.ok();
        }
        for (String learned : cultivation.getLearnedTechniques()) {
            String token = normalize(learned);
            if (token.contains(method) || method.contains(token)) {
                return GateResult.ok();
            }
        }
        String homeSect = METHOD_HOME_SECT.getOrDefault(method, "");
        if (!homeSect.isBlank()) {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (progress != null) {
                String current = normalize(progress.getSectId());
                int stage = progress.getSectQuestStage();
                if ((current.equals(homeSect) || current.contains(homeSect) || homeSect.contains(current))
                        && stage >= SectContributionService.STAGE_OUTER_DISCIPLE) {
                    return GateResult.ok();
                }
            }
        }
        // Generic formation seal method: allow once any formation technique is learned.
        if ("kunwu_seal_art".equals(method)) {
            for (String learned : cultivation.getLearnedTechniques()) {
                String token = normalize(learned);
                if (token.contains("formation") || token.contains("array") || token.contains("seal") || token.contains("kunwu")) {
                    return GateResult.ok();
                }
            }
        }
        return GateResult.deny("message.seeking_immortals.technique_gate.method",
                technique.name().isBlank() ? technique.id() : technique.name(),
                method);
    }

    /**
     * Wave467 cast-only: high-tier school arts need friendly faction reputation.
     */
    private static GateResult checkReputation(ServerPlayer player, TechniqueDataManager.TechniqueEntry technique) {
        if (player == null) {
            return GateResult.ok();
        }
        Realm required = technique.requiredRealm();
        if (required == null || required.ordinal() < Realm.FOUNDATION_ESTABLISHMENT.ordinal()) {
            return GateResult.ok();
        }
        String method = REQUIRES_METHOD.getOrDefault(normalize(technique.id()), "");
        if (method.isBlank()) {
            method = inferMethodFromBlob(technique);
        }
        String faction = METHOD_HOME_REP.getOrDefault(method, "");
        String blob = normalize(technique.id()) + " " + normalize(technique.source());
        if (faction.isBlank()) {
            if (blob.contains("star_palace") || blob.contains("chaotic") || blob.contains("void_palace")) {
                faction = "chaotic_sea";
            } else if (blob.contains("demon") || blob.contains("ghost") || blob.contains("xuewu")
                    || blob.contains("tianmo") || blob.contains("guiling") || blob.contains("wanhu")
                    || blob.contains("xuan_yin") || blob.contains("blood")) {
                faction = "demonic_path";
            } else if (blob.contains("dajin") || blob.contains("kunwu") || blob.contains("qinglan")
                    || blob.contains("huangfeng") || blob.contains("yanyue")) {
                faction = "dajin";
            }
        }
        if (faction.isBlank()) {
            return GateResult.ok();
        }
        // Only hard-gate from Core Formation upward for faction arts.
        if (required.ordinal() < Realm.CORE_FORMATION.ordinal() && !"chaotic_sea".equals(faction)) {
            return GateResult.ok();
        }
        int need = ReputationService.FRIENDLY_THRESHOLD;
        int have = ReputationService.get(player, faction);
        if (have < need) {
            return GateResult.deny("message.seeking_immortals.technique_gate.rep",
                    technique.name().isBlank() ? technique.id() : technique.name(),
                    faction, need, have);
        }
        return GateResult.ok();
    }

    private static String inferMethodFromBlob(TechniqueDataManager.TechniqueEntry technique) {
        String blob = normalize(technique.id()) + " " + normalize(technique.source());
        for (String method : METHOD_HOME_SECT.keySet()) {
            if (blob.contains(method) || blob.contains(method.replace("_art", "")) || blob.contains(method.replace("_", ""))) {
                return method;
            }
        }
        if (blob.contains("formation") || blob.contains("array") || blob.contains("seal")) {
            return REQUIRES_METHOD.containsValue("kunwu_seal_art") ? "kunwu_seal_art" : "";
        }
        return "";
    }

    private static Optional<String> inferSectId(TechniqueDataManager.TechniqueEntry technique) {
        String blob = ((technique.id() == null ? "" : technique.id()) + " "
                + (technique.source() == null ? "" : technique.source())).toLowerCase(Locale.ROOT);
        if (blob.contains("huangfeng")) {
            return Optional.of("huangfeng_valley");
        }
        if (blob.contains("yanyue") || blob.contains("yan_yue")) {
            return Optional.of("yanyue_sect");
        }
        if (blob.contains("qinglan") || blob.contains("qing_lan")) {
            return Optional.of("qinglan_sect");
        }
        if (blob.contains("qixuan") || blob.contains("七玄")) {
            return Optional.of("qixuan_men");
        }
        if (blob.contains("star_palace") || blob.contains("星宫")) {
            return Optional.of("star_palace");
        }
        if (blob.contains("guiling") || blob.contains("鬼灵")) {
            return Optional.of("guiling_gate");
        }
        if (blob.contains("hehuan") || blob.contains("合欢")) {
            return Optional.of("hehuan_sect");
        }
        if (blob.contains("luoyun") || blob.contains("落云")) {
            return Optional.of("luoyun_sect");
        }
        if (blob.contains("qingxu") || blob.contains("清虚")) {
            return Optional.of("qingxu_gate");
        }
        if (blob.contains("giant_sword") || blob.contains("巨剑")) {
            return Optional.of("giant_sword_gate");
        }
        if (blob.contains("tianlan") || blob.contains("天岚")) {
            return Optional.of("tianlan_temple");
        }
        if (blob.contains("yuling") || blob.contains("御灵")) {
            return Optional.of("yuling_pavilion");
        }
        if (blob.contains("spirit_beast") || blob.contains("灵兽山")) {
            return Optional.of("spirit_beast_mountain");
        }
        if (blob.contains("qianzhu") || blob.contains("千竹")) {
            return Optional.of("qianzhu_sect");
        }
        if (blob.contains("wanhu") || blob.contains("万狐")) {
            return Optional.of("wanhu_sect");
        }
        if (blob.contains("tianmo") || blob.contains("天魔")) {
            return Optional.of("tianmo_sect");
        }
        if (blob.contains("xuewu") || blob.contains("血巫")) {
            return Optional.of("xuewu_sect");
        }
        if (blob.contains("inverse_star") || blob.contains("逆星")) {
            return Optional.of("inverse_star_alliance");
        }
        return Optional.empty();
    }

    private static Map<String, String> loadRequiresMethod() {
        Map<String, String> map = new LinkedHashMap<>();
        ClassLoader loader = TechniqueGateService.class.getClassLoader();
        for (String file : Set.of(
                "qi_refining_techniques.json",
                "foundation_establishment_techniques.json",
                "core_formation_techniques.json",
                "nascent_soul_techniques.json",
                "spirit_transformation_plus_techniques.json",
                "special_common_techniques.json")) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/cultivation/" + file;
            loadRequiresMethodFromPath(loader, path, map, false);
        }
        // M02: full text_material technique corpus (20 school files) for requires_method.
        for (String file : Set.of(
                "body.json", "buddhist.json", "confucian.json", "dao.json", "demon_path.json",
                "demonic.json", "divine_sense.json", "elemental.json", "fashi.json", "formation.json",
                "ghost.json", "illusion.json", "misc.json", "movement.json", "puppet.json",
                "recovery.json", "secret_arts.json", "sword.json", "talisman.json", "xuan_yin.json")) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/techniques/" + file;
            loadRequiresMethodFromPath(loader, path, map, true);
        }
        // Builtin TechniqueEntry.requiresMethod fills any residual gaps.
        try {
            for (TechniqueDataManager.TechniqueEntry entry : TechniqueDataManager.builtinTechniques().values()) {
                if (entry != null && !entry.id().isBlank() && entry.requiresMethod() != null
                        && !entry.requiresMethod().isBlank()) {
                    map.putIfAbsent(normalize(entry.id()), normalize(entry.requiresMethod()));
                }
            }
        } catch (Throwable ignored) {
            // optional during early class init
        }
        return Map.copyOf(map);
    }

    private static void loadRequiresMethodFromPath(ClassLoader loader, String path,
                                                   Map<String, String> map, boolean putIfAbsent) {
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray techniques = root.getAsJsonArray("techniques");
                if (techniques == null) {
                    return;
                }
                for (JsonElement element : techniques) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject object = element.getAsJsonObject();
                    String id = object.has("id") && !object.get("id").isJsonNull() ? object.get("id").getAsString() : "";
                    String method = "";
                    if (object.has("requires_method") && !object.get("requires_method").isJsonNull()
                            && object.get("requires_method").isJsonPrimitive()) {
                        method = object.get("requires_method").getAsString();
                    } else if (object.has("source_method") && !object.get("source_method").isJsonNull()
                            && object.get("source_method").isJsonPrimitive()) {
                        method = object.get("source_method").getAsString();
                    }
                    if (!id.isBlank() && !method.isBlank()) {
                        if (putIfAbsent) {
                            map.putIfAbsent(normalize(id), normalize(method));
                        } else {
                            map.put(normalize(id), normalize(method));
                        }
                    }
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load technique method gates from {}", path, exception);
        }
    }

    private static Map<String, String> buildMethodHomeSects() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("tianmo_body_art", "tianmo_sect");
        map.put("guiling_soul_art", "guiling_gate");
        map.put("xuewu_blood_art", "xuewu_sect");
        map.put("wanhu_phantom_art", "wanhu_sect");
        map.put("xuan_yin_art", "guiling_gate");
        map.put("kunwu_seal_art", "qinglan_sect");
        return Map.copyOf(map);
    }

    private static Map<String, String> buildMethodHomeRep() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("tianmo_body_art", "demonic_path");
        map.put("guiling_soul_art", "demonic_path");
        map.put("xuewu_blood_art", "demonic_path");
        map.put("wanhu_phantom_art", "demonic_path");
        map.put("xuan_yin_art", "demonic_path");
        map.put("kunwu_seal_art", "dajin");
        return Map.copyOf(map);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
