package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.craft.PuppetCraftService;
import com.xunxian.seekingimmortals.craft.TalismanCraftService;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.structure.MultiblockOperationalService;
import com.xunxian.seekingimmortals.structure.MultiblockStationService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog browsers for refinement / formations with Wave463 authority actions.
 * craft -> ArtifactRefinementService; deploy -> FormationFieldService free field.
 */
public final class CraftWorldSoftService {
    private static final Map<String, FormationFieldService.FieldKind> FORMATION_MAP = buildFormationMap();

    private CraftWorldSoftService() {}

    public static int refinementRecipeCount() {
        return size("refinement_recipes_index");
    }

    public static int formationCount() {
        return size("formation_catalog_index");
    }

    public static int talismanRecipeCount() {
        return size("talisman_recipes_index");
    }

    public static int puppetRecipeCount() {
        return size("puppet_craft_recipes_index");
    }

    public static List<String> sample(String indexName, int limit) {
        Optional<BulkCatalogIndexService.IndexFile> optional = BulkCatalogIndexService.builtin().find(indexName);
        if (optional.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        int i = 0;
        for (BulkCatalogIndexService.Entry entry : optional.get().entries().values()) {
            String extra = "";
            if ("refinement_recipes_index".equals(indexName)) {
                extra = ArtifactDataService.builtin().findRecipe(entry.id()).isPresent() ? " -> craftable" : " -> soft";
            } else if ("formation_catalog_index".equals(indexName)) {
                extra = mappedFieldKind(entry.id()).map(k -> " -> " + k.name()).orElse(" -> soft");
            }
            list.add(entry.id() + " | " + entry.display() + extra);
            if (++i >= Math.max(1, limit)) break;
        }
        return list;
    }

    public static boolean preview(ServerPlayer player, String indexName, String id, String unknownKey, String previewKey, String softKey) {
        BulkCatalogIndexService.Entry entry = findEntry(indexName, id);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(unknownKey, id), false);
            return false;
        }
        player.displayClientMessage(Component.translatable(previewKey, entry.id(), entry.display()), false);
        if ("refinement_recipes_index".equals(indexName)) {
            Optional<ArtifactDataService.RefinementRecipe> recipe = ArtifactDataService.builtin().findRecipe(entry.id());
            if (recipe.isPresent()) {
                ArtifactDataService.RefinementRecipe r = recipe.get();
                player.displayClientMessage(Component.translatable("message.seeking_immortals.refine.mapped",
                        r.id(), r.forgeGrade(), String.format(Locale.ROOT, "%.0f%%", r.baseSuccessRate() * 100.0D)), false);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.refine.craft_hint"), false);
                return true;
            }
        } else if ("formation_catalog_index".equals(indexName)) {
            Optional<FormationFieldService.FieldKind> kind = mappedFieldKind(entry.id());
            if (kind.isPresent()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.mapped",
                        kind.get().name()), false);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.deploy_hint"), false);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable(softKey), false);
        return true;
    }

    public static boolean craft(ServerPlayer player, String recipeId, int forgeGrade) {
        if (!requiresNearbyStation(player, "refinement_forge", "refinement_forge_g1", "refinement_forge_g3")) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refine.need_station"), true);
            return false;
        }
        BulkCatalogIndexService.Entry entry = findEntry("refinement_recipes_index", recipeId);
        String id = entry == null ? norm(recipeId) : entry.id();
        Optional<ArtifactDataService.RefinementRecipe> recipe = ArtifactDataService.builtin().findRecipe(id);
        if (recipe.isEmpty()) {
            // Wave492: try fuzzy recipe match against catalog display/id tokens.
            recipe = fuzzyRecipe(id);
        }
        if (recipe.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refine.soft_only"), false);
            return false;
        }
        boolean ok = ArtifactRefinementService.refine(player, recipe.get().id(), Math.max(1, forgeGrade));
        if (ok) {
            SoftPhaseShellMark.markIfPresent(player, "phase13_refinement_full");
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refine.crafted", recipe.get().display()), true);
        }
        return ok;
    }

    private static Optional<ArtifactDataService.RefinementRecipe> fuzzyRecipe(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String needle = id.toLowerCase(Locale.ROOT);
        for (ArtifactDataService.RefinementRecipe recipe : ArtifactDataService.builtin().refinementRecipes().values()) {
            String rid = recipe.id() == null ? "" : recipe.id().toLowerCase(Locale.ROOT);
            String display = recipe.display() == null ? "" : recipe.display().toLowerCase(Locale.ROOT);
            if (rid.equals(needle) || rid.contains(needle) || needle.contains(rid) || display.contains(needle)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static Optional<FormationFieldService.FieldKind> mappedFieldKind(String formationId) {
        String id = norm(formationId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        FormationFieldService.FieldKind direct = FORMATION_MAP.get(id);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Map.Entry<String, FormationFieldService.FieldKind> e : FORMATION_MAP.entrySet()) {
            if (id.contains(e.getKey()) || e.getKey().contains(id)) {
                return Optional.of(e.getValue());
            }
        }
        // Keyword fallback for free-field disks/flags that only carry partial ids.
        if (id.contains("spirit_gather") || id.contains("juling") || id.contains("聚灵")) {
            return Optional.of(FormationFieldService.FieldKind.SPIRIT_GATHER);
        }
        if (id.contains("kill") || id.contains("sword") || id.contains("杀")) {
            return Optional.of(FormationFieldService.FieldKind.KILL_SWORD);
        }
        if (id.contains("seal") || id.contains("demon") || id.contains("禁") || id.contains("镇")) {
            return Optional.of(FormationFieldService.FieldKind.SEAL_DEMON);
        }
        if (id.contains("illusion") || id.contains("maze") || id.contains("迷")) {
            return Optional.of(FormationFieldService.FieldKind.ILLUSION_MAZE);
        }
        if (id.contains("defense") || id.contains("barrier") || id.contains("wall") || id.contains("护")) {
            return Optional.of(FormationFieldService.FieldKind.DEFENSE);
        }
        // Wave492: unknown formation catalog ids deploy as generic free fields (no soft_only dead-end).
        return Optional.of(FormationFieldService.FieldKind.CATALOG_GENERIC);
    }

    public static boolean deploy(ServerPlayer player, String formationId) {
        BulkCatalogIndexService.Entry entry = findEntry("formation_catalog_index", formationId);
        if (entry == null) {
            // Wave492: still allow direct deploy by id when index misses but kind maps.
            Optional<FormationFieldService.FieldKind> kindDirect = mappedFieldKind(formationId);
            if (kindDirect.isEmpty() || !(player.level() instanceof ServerLevel level)) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.unknown", formationId), false);
                return false;
            }
            boolean ok = FormationFieldService.activateFreeField(level, player.blockPosition(), kindDirect.get(), 20 * 90, player, formationId);
            if (ok) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.deployed",
                        formationId, kindDirect.get().name()), true);
            }
            return ok;
        }
        Optional<FormationFieldService.FieldKind> kind = mappedFieldKind(entry.id());
        if (kind.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.soft_only"), false);
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        // Wave466: small shard cost for free-field deploy (authority loop).
        int shardCost = switch (kind.get()) {
            case DEFENSE, KILL_SWORD, SEAL_DEMON, CATALOG_GENERIC -> 6;
            case ILLUSION_MAZE -> 5;
            case SPIRIT_GATHER -> 3;
        };
        if (!player.getAbilities().instabuild && !consumeShards(player, shardCost)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.formation_catalog.missing_shards", shardCost), true);
            return false;
        }
        boolean ok = FormationFieldService.activateFreeField(level, player.blockPosition(), kind.get(), 20 * 90, player, entry.id());
        if (ok) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.formation_catalog.deployed",
                    entry.display(), kind.get().name()), true);
        }
        return ok;
    }

    /** Wave466: catalog → TalismanCraftService authority bridge. */
    public static boolean craftTalisman(ServerPlayer player, String recipeId) {
        if (!requiresNearbyStation(player, "talisman_table")) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_table.need_station"), true);
            return false;
        }
        TalismanCraftService.CraftResult result = TalismanCraftService.craftById(player, recipeId);
        if (result.messageKey() != null && !result.messageKey().isBlank()) {
            if (result.success() && result.recipe() != null) {
                player.displayClientMessage(Component.translatable(result.messageKey(), result.recipe().display()), true);
            } else if (result.recipe() != null) {
                player.displayClientMessage(Component.translatable(result.messageKey()), true);
            } else {
                player.displayClientMessage(Component.translatable(result.messageKey(), recipeId), true);
            }
        }
        if (result.success()) {
            SoftPhaseShellMark.markIfPresent(player, "phase13_talisman_craft");
        }
        return result.success();
    }

    /** Wave466: catalog → PuppetCraftService authority bridge. */
    public static boolean craftPuppet(ServerPlayer player, String recipeId) {
        if (!requiresNearbyStation(player, "puppet_assembly_bench")) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet_assembly_bench.need_station"), true);
            return false;
        }
        PuppetCraftService.CraftResult result = PuppetCraftService.craftById(player, recipeId);
        if (result.messageKey() != null && !result.messageKey().isBlank()) {
            if (result.success() && result.recipe() != null) {
                player.displayClientMessage(Component.translatable(result.messageKey(), result.recipe().display()), true);
            } else if (result.recipe() != null) {
                player.displayClientMessage(Component.translatable(result.messageKey()), true);
            } else {
                player.displayClientMessage(Component.translatable(result.messageKey(), recipeId), true);
            }
        }
        if (result.success()) {
            SoftPhaseShellMark.markIfPresent(player, "phase13_puppet_craft");
        }
        return result.success();
    }


    private static boolean requiresNearbyStation(ServerPlayer player, String... stationIds) {
        if (player == null || player.level() == null || stationIds == null || stationIds.length == 0) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (MultiblockOperationalService.bestNearbyEfficiency(player, stationIds) > 0.0D) {
            return true;
        }
        // Formed shells that were never commissioned: attempt form once, then recheck.
        MultiblockOperationalService.tryCommissionNearby(player, stationIds);
        return MultiblockOperationalService.bestNearbyEfficiency(player, stationIds) > 0.0D;
    }

    /** Nearby station efficiency for success scaling (1.0 when creative / absent gate). */
    public static double nearbyStationEfficiency(ServerPlayer player, String... stationIds) {
        if (player == null || player.getAbilities().instabuild) {
            return 1.0D;
        }
        if (stationIds == null || stationIds.length == 0) {
            return 1.0D;
        }
        return MultiblockOperationalService.bestNearbyEfficiency(player, stationIds);
    }

    private static boolean consumeShards(ServerPlayer player, int count) {
        var shard = com.xunxian.seekingimmortals.registry.ModItems.SPIRIT_STONE_SHARD.get();
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private static BulkCatalogIndexService.Entry findEntry(String indexName, String id) {
        Optional<BulkCatalogIndexService.IndexFile> optional = BulkCatalogIndexService.builtin().find(indexName);
        if (optional.isEmpty()) {
            return null;
        }
        String key = norm(id);
        BulkCatalogIndexService.Entry entry = optional.get().entries().get(key);
        if (entry != null) {
            return entry;
        }
        for (BulkCatalogIndexService.Entry e : optional.get().entries().values()) {
            if (e.id().equalsIgnoreCase(id)) {
                return e;
            }
        }
        return null;
    }

    private static int size(String indexName) {
        return BulkCatalogIndexService.builtin().find(indexName).map(BulkCatalogIndexService.IndexFile::size).orElse(0);
    }

    private static Map<String, FormationFieldService.FieldKind> buildFormationMap() {
        Map<String, FormationFieldService.FieldKind> map = new LinkedHashMap<>();
        map.put("spirit_gather", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("spirit_gathering_array", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("spirit_gathering_minor", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("defense_wall", FormationFieldService.FieldKind.DEFENSE);
        map.put("defense_formation", FormationFieldService.FieldKind.DEFENSE);
        map.put("kill_sword", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("kill_sword_formation", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("seal_demon_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("demon_seal_pillar_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("illusion_maze", FormationFieldService.FieldKind.ILLUSION_MAZE);
        map.put("illusion_maze_array", FormationFieldService.FieldKind.ILLUSION_MAZE);
        // Wave463/492: catalog aliases map to nearest playable field kinds.
        map.put("five_elements_mountain", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("barrier_sect_protection", FormationFieldService.FieldKind.DEFENSE);
        map.put("sword_array_bagua", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("thunder_tribulation_array", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("blood_sacrifice_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("teleport_array", FormationFieldService.FieldKind.DEFENSE);
        map.put("teleport_array_long_range", FormationFieldService.FieldKind.DEFENSE);
        map.put("seal_barrier", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("portable_seal", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("kill_array", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("kill_array_core", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("spirit_gather_flag", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("spirit_gather_flag_set", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("nine_palace", FormationFieldService.FieldKind.DEFENSE);
        map.put("nine_palace_disk", FormationFieldService.FieldKind.DEFENSE);
        map.put("array_blueprint", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("array_blueprint_scroll", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("mulan_wind_ride_array", FormationFieldService.FieldKind.DEFENSE);
        map.put("blood_forbidden_gate", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("nine_dragon_flame_barrier", FormationFieldService.FieldKind.DEFENSE);
        map.put("inverted_five_elements_array", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("vajra_prison_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("nine_dragon_flame_barrier_formation_core", FormationFieldService.FieldKind.DEFENSE);
        map.put("kill_sword_formation", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("seal_demon_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("demon_seal_pillar_array", FormationFieldService.FieldKind.SEAL_DEMON);
        map.put("illusion_maze", FormationFieldService.FieldKind.ILLUSION_MAZE);
        map.put("illusion_maze_array", FormationFieldService.FieldKind.ILLUSION_MAZE);
        // remaining catalog cores as generic spirit gather pressure
        map.put("five_elements_mountain", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("barrier_sect_protection", FormationFieldService.FieldKind.DEFENSE);
        map.put("sword_array_bagua", FormationFieldService.FieldKind.KILL_SWORD);
        map.put("thunder_tribulation_array", FormationFieldService.FieldKind.SPIRIT_GATHER);
        map.put("blood_sacrifice_array", FormationFieldService.FieldKind.SEAL_DEMON);
        return map;
    }

    private static String norm(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    /** Tiny bridge so CraftWorld does not hard-depend on phase package cycles in tests. */
    private static final class SoftPhaseShellMark {
        private SoftPhaseShellMark() {}
        static void markIfPresent(ServerPlayer player, String phaseId) {
            try {
                com.xunxian.seekingimmortals.phase.SoftPhaseShellService.mark(player, phaseId, false);
            } catch (Throwable ignored) {
                // optional
            }
        }
    }
}
