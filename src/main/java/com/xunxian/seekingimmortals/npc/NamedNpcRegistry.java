package com.xunxian.seekingimmortals.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.shop.ShopService;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M12 named-NPC authority: id / region / faction / role / shop ref / dialogue tree ref.
 * Source: {@code text_material/named_npcs_v116.json} + seeds + template bindings.
 */
public final class NamedNpcRegistry {
    private static final Pattern SHOP_TIER = Pattern.compile("货架档[：:]\\s*(\\d+)");
    private static final Pattern REP_TRACK = Pattern.compile("声望轨[：:]\\s*([A-Za-z0-9_]+)");

    private static final Snapshot BUILTIN = loadBuiltin();

    private NamedNpcRegistry() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<NamedNpc> find(String npcId) {
        return Optional.ofNullable(BUILTIN.npcs().get(normalize(npcId)));
    }

    public static List<NamedNpc> all() {
        return List.copyOf(BUILTIN.npcs().values());
    }

    public static List<NamedNpc> byRegion(String regionId) {
        String region = normalize(regionId);
        if (region.isBlank()) {
            return List.of();
        }
        List<NamedNpc> out = new ArrayList<>();
        for (NamedNpc npc : BUILTIN.npcs().values()) {
            if (region.equals(npc.regionId())) {
                out.add(npc);
            }
        }
        return List.copyOf(out);
    }

    public static List<NamedNpc> bySect(String sectId) {
        String sect = normalize(sectId);
        if (sect.isBlank()) {
            return List.of();
        }
        List<NamedNpc> out = new ArrayList<>();
        for (NamedNpc npc : BUILTIN.npcs().values()) {
            if (sect.equals(npc.sectId()) || sect.equals(npc.factionId())) {
                out.add(npc);
            }
        }
        return List.copyOf(out);
    }

    public static List<NamedNpc> byRole(String role) {
        String key = normalize(role);
        if (key.isBlank()) {
            return List.of();
        }
        List<NamedNpc> out = new ArrayList<>();
        for (NamedNpc npc : BUILTIN.npcs().values()) {
            if (key.equals(npc.role())) {
                out.add(npc);
            }
        }
        return List.copyOf(out);
    }

    public static boolean isKnown(String npcId) {
        return BUILTIN.npcs().containsKey(normalize(npcId));
    }

    public static int count() {
        return BUILTIN.npcs().size();
    }

    /**
     * Validation: every named NPC's region / dialogue tree / shop (when present) should resolve.
     * Returns human-readable issues (empty = ok).
     */
    public static List<String> validateReferences() {
        List<String> issues = new ArrayList<>();
        for (NamedNpc npc : BUILTIN.npcs().values()) {
            if (!npc.regionId().isBlank() && !RegionRegistry.isKnown(npc.regionId())) {
                // soft: region cards may use coarser ids than named_npcs; only flag if completely unknown tokens.
                if (!RegionRegistry.isKnown(npc.regionId().split("_")[0])) {
                    issues.add("region_unresolved:" + npc.id() + "->" + npc.regionId());
                }
            }
            if (!npc.dialogueTreeId().isBlank() && DialogueBranchService.findTree(npc.dialogueTreeId()).isEmpty()) {
                // template trees may not cover every role; only hard-fail explicit seed/template bindings.
                if (npc.seeded() || npc.bindingBound()) {
                    issues.add("tree_unresolved:" + npc.id() + "->" + npc.dialogueTreeId());
                }
            }
            if (!npc.shopId().isBlank()) {
                try {
                    ShopService.Shop shop = ShopService.getShop(npc.shopId());
                    if (shop == null || shop.entries() == null) {
                        issues.add("shop_unresolved:" + npc.id() + "->" + npc.shopId());
                    }
                } catch (Exception ex) {
                    issues.add("shop_error:" + npc.id() + "->" + npc.shopId());
                }
            }
        }
        return List.copyOf(issues);
    }

    private static Snapshot loadBuiltin() {
        Map<String, NamedNpc> npcs = new LinkedHashMap<>();
        Map<String, String> bindings = loadTemplateBindings();
        Map<String, String> dialoguePacks = loadDialoguePackNpcMap();
        Map<String, String> vendorShops = loadVendorShops();

        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/named_npcs_v116.json");
        if (root != null && root.has("npcs") && root.get("npcs").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("npcs")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id", ""));
                if (id.isBlank()) {
                    continue;
                }
                String display = str(o, "display", id);
                String sectId = normalize(str(o, "sect_id", ""));
                String region = normalize(str(o, "region", ""));
                String role = normalize(str(o, "role", ""));
                String alignment = normalize(str(o, "alignment", ""));
                String description = str(o, "description", "");
                int shopTier = parseShopTier(description);
                String repTrack = parseRepTrack(description);
                String factionId = sectId;
                String archetype = bindings.getOrDefault(id, defaultArchetypeForRole(role));
                String treeId = DialogueBranchService.treeIdForArchetype(archetype);
                String shopId = defaultShopFor(role, sectId, shopTier, vendorShops.get(id));
                String dialoguePackId = dialoguePacks.getOrDefault(id, "");
                npcs.put(id, new NamedNpc(
                        id, display, sectId, factionId, region, role, alignment,
                        archetype, treeId, shopId, repTrack, shopTier,
                        dialoguePackId, description, false, bindings.containsKey(id),
                        List.of()));
            }
        }

        // Seeds enrich / add dedicated story NPCs with explicit archetypes/services.
        JsonObject seeds = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/named_npc_seeds_v137.json");
        if (seeds != null && seeds.has("npcs") && seeds.get("npcs").isJsonArray()) {
            for (JsonElement element : seeds.getAsJsonArray("npcs")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id", ""));
                if (id.isBlank()) {
                    continue;
                }
                String display = str(o, "display", id);
                String faction = normalize(str(o, "faction", ""));
                String role = normalize(str(o, "role", str(o, "role", "")));
                // seeds use freeform Chinese role; keep raw lowercased token if blank-ish
                if (role.isBlank()) {
                    role = normalize(str(o, "role", "seed"));
                }
                String archetype = normalize(str(o, "dialogue_archetype", bindings.getOrDefault(id, "")));
                if (archetype.isBlank()) {
                    archetype = bindings.getOrDefault(id, "");
                }
                String treeId = DialogueBranchService.treeIdForArchetype(archetype);
                if (treeId.isBlank() && !archetype.isBlank()) {
                    treeId = "tree_" + archetype;
                }
                List<String> services = stringList(o.get("services"));
                String shopId = shopFromServices(services, faction);
                String region = inferRegionFromFaction(faction);
                NamedNpc existing = npcs.get(id);
                if (existing != null) {
                    npcs.put(id, existing.withSeed(
                            archetype.isBlank() ? existing.archetype() : archetype,
                            treeId.isBlank() ? existing.dialogueTreeId() : treeId,
                            shopId.isBlank() ? existing.shopId() : shopId,
                            services,
                            true));
                } else {
                    npcs.put(id, new NamedNpc(
                            id, display, faction, faction, region, role, "",
                            archetype, treeId, shopId, "", 0,
                            dialoguePacks.getOrDefault(id, ""),
                            str(o, "description", ""), true, bindings.containsKey(id),
                            services));
                }
            }
        }

        // Template bindings may reference NPCs not present in v116 list — ensure they exist.
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            String id = binding.getKey();
            if (npcs.containsKey(id)) {
                NamedNpc cur = npcs.get(id);
                String archetype = binding.getValue();
                String treeId = DialogueBranchService.treeIdForArchetype(archetype);
                if (!archetype.equals(cur.archetype()) || (!treeId.isBlank() && !treeId.equals(cur.dialogueTreeId()))) {
                    npcs.put(id, cur.withSeed(
                            archetype,
                            treeId.isBlank() ? cur.dialogueTreeId() : treeId,
                            cur.shopId(),
                            cur.services(),
                            cur.seeded()));
                }
                continue;
            }
            String archetype = binding.getValue();
            String treeId = DialogueBranchService.treeIdForArchetype(archetype);
            npcs.put(id, new NamedNpc(
                    id, id, "", "", "", archetype, "",
                    archetype, treeId, defaultShopFor(archetype, "", 1, ""),
                    "", 0, "", "", false, true, List.of()));
        }

        return new Snapshot(Collections.unmodifiableMap(npcs), Map.copyOf(bindings));
    }

    private static Map<String, String> loadTemplateBindings() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String path : List.of(
                "data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogue_templates_v138.json",
                "data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogue_templates.json")) {
            JsonObject root = readJson(path);
            if (root == null || !root.has("named_npc_bindings")) {
                continue;
            }
            for (JsonElement element : root.getAsJsonArray("named_npc_bindings")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String npcId = normalize(str(o, "npc_id", ""));
                String archetype = normalize(str(o, "archetype", ""));
                if (!npcId.isBlank() && !archetype.isBlank()) {
                    map.put(npcId, archetype);
                }
            }
            if (!map.isEmpty()) {
                break;
            }
        }
        return map;
    }

    private static Map<String, String> loadDialoguePackNpcMap() {
        Map<String, String> npcToPack = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogues_v117.json");
        if (root == null || !root.has("dialogues")) {
            return npcToPack;
        }
        for (JsonElement element : root.getAsJsonArray("dialogues")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String packId = normalize(str(o, "id", ""));
            String npc = normalize(str(o, "npc", ""));
            if (!npc.isBlank() && !packId.isBlank()) {
                npcToPack.putIfAbsent(npc, packId);
            }
        }
        return npcToPack;
    }

    private static Map<String, String> loadVendorShops() {
        Map<String, String> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/npc_vendor_roster_v96.json");
        if (root == null || !root.has("vendors")) {
            return map;
        }
        for (JsonElement element : root.getAsJsonArray("vendors")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = normalize(str(o, "id", ""));
            if (id.isBlank()) {
                continue;
            }
            // vendor ids are not always named_npc ids; map by heuristic shop.
            String location = normalize(str(o, "location", ""));
            map.put(id, shopFromVendorLocation(location));
        }
        return map;
    }

    private static String defaultArchetypeForRole(String role) {
        return switch (normalize(role)) {
            case "outer_deacon", "patrol_captain" -> "sect_contribution_clerk";
            case "alchemy_elder", "great_elder" -> "sect_contribution_clerk";
            case "sect_master" -> "sect_contribution_clerk";
            case "black_market_contact" -> "inverse_star_contact";
            case "quest_giver_main" -> "market_vendor";
            default -> "";
        };
    }

    private static String defaultShopFor(String roleOrArchetype, String sectId, int shopTier, String vendorShop) {
        if (vendorShop != null && !vendorShop.isBlank()) {
            return vendorShop;
        }
        String key = normalize(roleOrArchetype);
        if (key.contains("market") || key.contains("vendor") || key.contains("black_market")) {
            if (key.contains("inverse") || key.contains("black")) {
                return ShopService.INVERSE_STAR_BLACK_MARKET;
            }
            return ShopService.MARKET_HERBAL_STALL;
        }
        if (key.contains("star_palace") || key.contains("registrar")) {
            return ShopService.STAR_PALACE_PATROL_SUPPLY;
        }
        if (key.contains("nether") || key.contains("reincarnation")) {
            return ShopService.NETHER_FERRY_VENDOR;
        }
        if (key.contains("contribution") || key.contains("deacon") || key.contains("elder") || key.contains("sect")) {
            String sect = normalize(sectId);
            if (sect.contains("huangfeng")) {
                return ShopService.HUANGFENG_CONTRIBUTION_HALL;
            }
            if (sect.contains("qinglan")) {
                return ShopService.QINGLAN_CONTRIBUTION_HALL;
            }
            if (sect.contains("qianzhu")) {
                return ShopService.QIANZHU_PUPPET_HALL;
            }
            if (sect.contains("mulan")) {
                return ShopService.MULAN_FASHI_SUPPLY;
            }
            return shopTier >= 3 ? ShopService.DAJIN_WANBAO_PAVILION : ShopService.HUANGFENG_CONTRIBUTION_HALL;
        }
        return "";
    }

    private static String shopFromServices(List<String> services, String faction) {
        for (String service : services) {
            String s = normalize(service);
            if (s.contains("blood_forbidden") || s.contains("quota")) {
                return ShopService.HUANGFENG_CONTRIBUTION_HALL;
            }
            if (s.contains("auction") || s.contains("appraisal")) {
                return ShopService.DAJIN_WANBAO_PAVILION;
            }
            if (s.contains("registration") || s.contains("tax") || s.contains("teleport_permit")) {
                return ShopService.STAR_PALACE_PATROL_SUPPLY;
            }
            if (s.contains("soul_trade") || s.contains("contract")) {
                return ShopService.NETHER_FERRY_VENDOR;
            }
            if (s.contains("smuggle") || s.contains("black")) {
                return ShopService.INVERSE_STAR_BLACK_MARKET;
            }
        }
        return defaultShopFor("", faction, 1, "");
    }

    private static String shopFromVendorLocation(String location) {
        String loc = normalize(location);
        if (loc.contains("heishi") || loc.contains("inverse") || loc.contains("black")) {
            return ShopService.INVERSE_STAR_BLACK_MARKET;
        }
        if (loc.contains("huangfeng") || loc.contains("hfg")) {
            return ShopService.HUANGFENG_CONTRIBUTION_HALL;
        }
        if (loc.contains("wanbao") || loc.contains("auction")) {
            return ShopService.DAJIN_WANBAO_PAVILION;
        }
        if (loc.contains("nether")) {
            return ShopService.NETHER_FERRY_VENDOR;
        }
        if (loc.contains("chaotic") || loc.contains("outer_sea")) {
            return ShopService.CHAOTIC_SEA_ISLAND_GENERAL;
        }
        return ShopService.MARKET_HERBAL_STALL;
    }

    private static String inferRegionFromFaction(String faction) {
        String f = normalize(faction);
        if (f.contains("star") || f.contains("inverse") || f.contains("heifeng") || f.contains("chaotic")) {
            return "chaotic_sea";
        }
        if (f.contains("tianyuan") || f.contains("fengyuan")) {
            return "spirit_fengyuan";
        }
        if (f.contains("mulan") || f.contains("tianlan")) {
            return "mulan";
        }
        if (f.contains("nether") || f.contains("reincarnation") || f.contains("gray")) {
            return "yinming";
        }
        if (f.contains("heaven")) {
            return "outer_sea_market";
        }
        return "tiannan";
    }

    private static int parseShopTier(String description) {
        if (description == null) {
            return 0;
        }
        Matcher matcher = SHOP_TIER.matcher(description);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String parseRepTrack(String description) {
        if (description == null) {
            return "";
        }
        Matcher matcher = REP_TRACK.matcher(description);
        return matcher.find() ? normalize(matcher.group(1)) : "";
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive()) {
                    String value = normalize(child.getAsString());
                    if (!value.isBlank()) {
                        list.add(value);
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String value = normalize(element.getAsString());
            if (!value.isBlank()) {
                list.add(value);
            }
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = NamedNpcRegistry.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load named NPC registry {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return String.valueOf(object.get(key));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Snapshot(Map<String, NamedNpc> npcs, Map<String, String> templateBindings) {
        public int count() {
            return npcs.size();
        }
    }

    public record NamedNpc(
            String id,
            String display,
            String sectId,
            String factionId,
            String regionId,
            String role,
            String alignment,
            String archetype,
            String dialogueTreeId,
            String shopId,
            String reputationTrack,
            int shopTier,
            String dialoguePackId,
            String description,
            boolean seeded,
            boolean bindingBound,
            List<String> services) {

        public NamedNpc withSeed(String archetype, String treeId, String shopId,
                                    List<String> services, boolean seeded) {
            return new NamedNpc(
                    id, display, sectId, factionId, regionId, role, alignment,
                    archetype == null ? this.archetype : archetype,
                    treeId == null ? this.dialogueTreeId : treeId,
                    shopId == null || shopId.isBlank() ? this.shopId : shopId,
                    reputationTrack, shopTier, dialoguePackId, description,
                    seeded, bindingBound,
                    services == null ? this.services : List.copyOf(services));
        }
    }
}
