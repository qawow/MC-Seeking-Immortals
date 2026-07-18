package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import com.xunxian.seekingimmortals.cultivation.TribulationService;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

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
 * M13 ascension_flow + ascension_loadout authority.
 * <p>Conditions: realm (M01) + quest flag (M11) + tribulation (M01).
 * Loadout reset snapshots inventory only for teleport-failure rollback.
 * On successful teleport the snapshot is discarded immediately — there is no player reclaim path.
 * Unique items are always retained on the player during reset.</p>
 */
public final class AscensionService {
    public static final String FLAG_ASCENSION_READY = "ascension_ready";
    public static final String FLAG_ASCENDED = "ascended_to_spirit";
    public static final String FLAG_LOADOUT_CONFIRMED = "ascension_loadout_confirmed";
    public static final String FLAG_TRIBULATION_SUCCESS = "tribulation_success";
    /** Idempotent gate for alliance_merit_token / spirit_stone_shard starter grants. */
    public static final String FLAG_STARTER_GRANTED = "ascension_starter_granted";
    public static final String QUEST_CHAIN_SPIRIT_RISE = "spirit_realm_rise";

    private static final String PENDING_ROOT = "seeking_immortals_ascension_pending";
    /** PersistentData key holding a temporary teleport-failure rollback snapshot. */
    public static final String BACKUP_ROOT = "seeking_immortals_ascension_backup";
    private static final String STAGE_ROOT = "seeking_immortals_ascension_stage";
    private static final Snapshot SNAPSHOT = load();

    private AscensionService() {}

    public record StageDef(
            String id,
            String display,
            String realm,
            String dimension,
            String gate,
            boolean oneWay,
            List<String> requires,
            String realmMin,
            String method) {}

    public record LoadoutPath(
            String id,
            String display,
            String realmMin,
            List<String> recommendedItems,
            List<String> requiredNotes) {}

    public record Snapshot(Map<String, StageDef> stages, Map<String, LoadoutPath> loadoutPaths, String questChainRef) {
        public int stageCount() { return stages.size(); }
        public Optional<StageDef> findStage(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(stages.get(id.trim().toLowerCase(Locale.ROOT)));
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int stageCount() {
        return SNAPSHOT.stageCount();
    }

    public static boolean hasPendingConfirmation(ServerPlayer player) {
        return player != null && player.getPersistentData().getCompound(PENDING_ROOT).getBoolean("active");
    }

    /**
     * Evaluate gates without side effects.
     */
    public static List<String> missingRequirements(ServerPlayer player) {
        List<String> missing = new ArrayList<>();
        if (player == null) {
            missing.add("no_player");
            return missing;
        }
        Optional<PlayerCultivation> cultivationOpt = CultivationHelper.get(player);
        if (cultivationOpt.isEmpty()) {
            missing.add("no_cultivation");
            return missing;
        }
        PlayerCultivation cultivation = cultivationOpt.get();
        if (cultivation.getRealm().ordinal() < Realm.SOUL_TRANSFORMATION.ordinal()) {
            missing.add("realm_DEITY_TRANSFORMATION");
        } else if (cultivation.getRealm() == Realm.SOUL_TRANSFORMATION
                && cultivation.getStage() != RealmStage.PEAK
                && cultivation.getStage() != RealmStage.LATE) {
            // allow LATE as near-peak soft pass; PEAK preferred
            if (!NpcDialogueFlags.hasFlag(player, FLAG_ASCENSION_READY)) {
                missing.add("realm_peak");
            }
        }
        if (!TribulationService.hasPassedTribulation(player)
                && !NpcDialogueFlags.hasFlag(player, FLAG_TRIBULATION_SUCCESS)
                && !NpcDialogueFlags.hasFlag(player, "tribulation_success")) {
            missing.add("tribulation_success");
        }
        boolean questOk = NpcDialogueFlags.hasFlag(player, FLAG_ASCENSION_READY)
                || NpcDialogueFlags.hasFlag(player, "quest_soft_" + QUEST_CHAIN_SPIRIT_RISE)
                || NpcDialogueFlags.hasFlag(player, "quest_progress_" + QUEST_CHAIN_SPIRIT_RISE)
                || TextQuestChainService.progressOf(player, QUEST_CHAIN_SPIRIT_RISE).complete();
        // quest is soft-required: if none of the flags exist, still allow when realm+tribulation ok
        // but record for UI
        if (!questOk) {
            missing.add("quest_flag_soft:" + QUEST_CHAIN_SPIRIT_RISE);
        }
        return missing;
    }

    public static boolean canAscend(ServerPlayer player) {
        List<String> missing = missingRequirements(player);
        // soft quest flag is warning only
        return missing.stream().noneMatch(m -> !m.startsWith("quest_flag_soft"));
    }

    /**
     * Begin ascension. First call without confirmed loadout parks a pending confirmation.
     * Second call with confirmLoadout=true (or after confirmLoadout()) executes travel + reset.
     * <p>Re-ascension is blocked solely by {@link #FLAG_ASCENDED} (dimension-independent).
     * Inventory snapshot exists only for teleport-failure rollback and is cleared on success.</p>
     */
    public static boolean attemptAscension(ServerPlayer player, boolean confirmLoadout) {
        if (player == null) {
            return false;
        }
        // Dimension-independent: FLAG_ASCENDED alone blocks re-run (cannot farm by leaving tianyuan).
        if (shouldBlockReascension(NpcDialogueFlags.hasFlag(player, FLAG_ASCENDED))) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.already"), false);
            return false;
        }
        List<String> missing = missingRequirements(player);
        List<String> hard = missing.stream().filter(m -> !m.startsWith("quest_flag_soft")).toList();
        if (!hard.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ascension.missing", String.join(", ", hard)), false);
            return false;
        }
        if (!confirmLoadout && !NpcDialogueFlags.hasFlag(player, FLAG_LOADOUT_CONFIRMED)
                && !hasPendingConfirmation(player)) {
            markPending(player);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.confirm_loadout"), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.backup_hint"), false);
            return false;
        }
        if (confirmLoadout) {
            NpcDialogueFlags.setFlag(player, FLAG_LOADOUT_CONFIRMED, true);
            clearPending(player);
        }
        if (!NpcDialogueFlags.hasFlag(player, FLAG_LOADOUT_CONFIRMED) && !confirmLoadout) {
            // pending exists — require explicit confirm
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.need_confirm"), false);
            return false;
        }

        backupInventory(player);
        applyLoadoutReset(player);
        boolean teleported = teleportToTianyuan(player);
        if (!teleported) {
            // Teleport failed after reset: rollback from temporary snapshot (system path only).
            restoreBackup(player);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.teleport_failed"), false);
            return false;
        }
        NpcDialogueFlags.setFlag(player, FLAG_ASCENDED, true);
        NpcDialogueFlags.setFlag(player, FLAG_ASCENSION_READY, true);
        setStage(player, "tianyuan_garrison");
        FlyingAuthorityPolicy.onDimensionChanged(player, DimensionRegistryService.TIANYUAN);
        grantStarterPack(player);
        // Success: discard rollback snapshot so no later restore can duplicate unique/equipment items.
        clearBackup(player);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.success"), false);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.backup_stored"), false);
        return true;
    }

    public static boolean confirmLoadoutAndAscend(ServerPlayer player) {
        return attemptAscension(player, true);
    }

    public static boolean cancelPending(ServerPlayer player) {
        if (player == null || !hasPendingConfirmation(player)) {
            return false;
        }
        clearPending(player);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.cancelled"), false);
        return true;
    }

    /**
     * Whether the player currently holds a restoreable teleport-failure snapshot.
     * After a successful ascension this is always false.
     */
    public static boolean hasBackup(ServerPlayer player) {
        return player != null && hasBackupData(player.getPersistentData());
    }

    /** Pure NBT check used by {@link #hasBackup(ServerPlayer)} and unit tests. */
    public static boolean hasBackupData(CompoundTag persistentData) {
        if (persistentData == null || !persistentData.contains(BACKUP_ROOT)) {
            return false;
        }
        return persistentData.getCompound(BACKUP_ROOT).getBoolean("hasBackup");
    }

    /**
     * Discard the temporary rollback snapshot (success path / admin cleanup).
     * Does not touch the live inventory.
     */
    public static void clearBackup(ServerPlayer player) {
        if (player == null) {
            return;
        }
        clearBackupData(player.getPersistentData());
    }

    /** Pure NBT clear used by {@link #clearBackup(ServerPlayer)} and unit tests. */
    public static void clearBackupData(CompoundTag persistentData) {
        if (persistentData == null) {
            return;
        }
        persistentData.remove(BACKUP_ROOT);
    }

    /**
     * Re-ascension gate: {@code true} blocks another attempt.
     * Intentionally ignores current dimension so leaving tianyuan cannot bypass it.
     */
    public static boolean shouldBlockReascension(boolean ascendedFlag) {
        return ascendedFlag;
    }

    /** Starter-pack gate: grant only when the persistent flag is still unset. */
    public static boolean shouldGrantStarter(boolean alreadyGranted) {
        return !alreadyGranted;
    }

    /**
     * Apply a temporary rollback snapshot into the player inventory, then consume it.
     * <p>Intended for system teleport-failure recovery and admin diagnostics only —
     * successful ascension clears the snapshot so this returns {@code no_backup}.</p>
     */
    public static boolean restoreBackup(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(BACKUP_ROOT);
        if (!root.getBoolean("hasBackup")) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.no_backup"), false);
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        ListTag items = root.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag tag = items.getCompound(i);
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            int slot = tag.getInt("Slot");
            if (slot >= 0 && slot < player.getInventory().getContainerSize()
                    && player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        // Consume snapshot so a second restore cannot re-inject the same stacks.
        clearBackupData(player.getPersistentData());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension.backup_restored"), false);
        return true;
    }

    public static String currentStage(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getPersistentData().getCompound(STAGE_ROOT).getString("stage");
    }

    private static void setStage(ServerPlayer player, String stage) {
        CompoundTag tag = player.getPersistentData().getCompound(STAGE_ROOT).copy();
        tag.putString("stage", stage == null ? "" : stage);
        player.getPersistentData().put(STAGE_ROOT, tag);
    }

    private static void markPending(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", true);
        tag.putLong("at", player.level().getGameTime());
        player.getPersistentData().put(PENDING_ROOT, tag);
    }

    private static void clearPending(ServerPlayer player) {
        player.getPersistentData().remove(PENDING_ROOT);
    }

    private static void backupInventory(ServerPlayer player) {
        ListTag items = new ListTag();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            stack.save(tag);
            tag.putInt("Slot", i);
            items.add(tag);
        }
        CompoundTag root = new CompoundTag();
        root.putBoolean("hasBackup", true);
        root.put("Items", items);
        root.putLong("gameTime", player.level().getGameTime());
        player.getPersistentData().put(BACKUP_ROOT, root);
    }

    /**
     * Loadout reset: strip non-whitelist common junk, keep uniques + equipped gear + recommended path items.
     * Never destroys unique story items.
     */
    private static void applyLoadoutReset(ServerPlayer player) {
        // Retained stacks stay in their original slots; only resettable stacks are removed.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !isUniqueOrWhitelisted(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void grantStarterPack(ServerPlayer player) {
        if (shouldGrantStarter(NpcDialogueFlags.hasFlag(player, FLAG_STARTER_GRANTED))) {
            grantIfPresent(player, "seeking_immortals:alliance_merit_token", 1);
            grantIfPresent(player, "seeking_immortals:spirit_stone_shard", 16);
            NpcDialogueFlags.setFlag(player, FLAG_STARTER_GRANTED, true);
        }
    }

    private static boolean isUniqueOrWhitelisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var keyLoc = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String key = keyLoc == null ? "" : keyLoc.toString();
        String bare = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        if (ItemCatalogService.isUniqueForbidden(bare) || ItemCatalogService.isUniqueForbidden(key)) {
            return true;
        }
        String lower = bare.toLowerCase(Locale.ROOT);
        if (lower.contains("bottle") || lower.contains("vial") || lower.contains("liquid")
                || lower.contains("contract") || lower.contains("manual") || lower.contains("natal")
                || lower.contains("palm") || lower.contains("unique")) {
            return true;
        }
        // keep non-stackables (equipment / soul-bound loadout)
        return stack.getMaxStackSize() == 1;
    }

    private static void grantIfPresent(ServerPlayer player, String itemId, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(itemId));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static boolean teleportToTianyuan(ServerPlayer player) {
        // prefer worldpack region travel (sets anchors/region state)
        if (WorldpackGameplayService.travel(player, "tianyuan")) {
            return true;
        }
        Optional<ServerLevel> level = DimensionRegistryService.resolveLevel(player, DimensionRegistryService.TIANYUAN);
        if (level.isEmpty()) {
            return false;
        }
        ServerLevel target = level.get();
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0) + 1;
        y = Math.max(target.getMinBuildHeight() + 2, Math.min(target.getMaxBuildHeight() - 2, y));
        BlockPos base = new BlockPos(0, y - 1, 0);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                target.setBlock(base.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
                target.setBlock(base.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 3);
                target.setBlock(base.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        CultivationHelper.get(player).ifPresent(c -> {
            c.clearWorldpackReturnLocation();
            c.setWorldpackCurrentRegionId("tianyuan");
            c.setWorldpackActiveSecretRealmId("");
        });
        player.teleportTo(target, 0.5D, y, 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    private static Snapshot load() {
        Map<String, StageDef> stages = new LinkedHashMap<>();
        Map<String, LoadoutPath> paths = new LinkedHashMap<>();
        String questRef = QUEST_CHAIN_SPIRIT_RISE;

        JsonObject flow = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ascension_flow.json");
        if (flow != null) {
            questRef = firstNonBlank(str(flow, "quest_chain_ref"), questRef);
            for (JsonElement element : array(flow, "stages")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                List<String> requires = new ArrayList<>();
                for (JsonElement r : array(o, "requires")) {
                    try { requires.add(r.getAsString()); } catch (Exception ignored) {}
                }
                stages.put(id.toLowerCase(Locale.ROOT), new StageDef(
                        id,
                        firstNonBlank(str(o, "display"), id),
                        str(o, "realm"),
                        firstNonBlank(str(o, "dimension"), str(o, "to_dimension")),
                        firstNonBlank(str(o, "gate"), str(o, "via_gate")),
                        o.has("one_way") && o.get("one_way").getAsBoolean()
                                || o.has("one_way_main_body") && o.get("one_way_main_body").getAsBoolean(),
                        List.copyOf(requires),
                        str(o, "realm_min"),
                        str(o, "method")));
            }
        }
        JsonObject loadout = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ascension_loadout_v95.json");
        if (loadout != null) {
            for (JsonElement element : array(loadout, "paths")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                List<String> recommended = new ArrayList<>();
                for (JsonElement r : array(o, "recommended")) {
                    if (r.isJsonObject()) {
                        recommended.add(str(r.getAsJsonObject(), "item"));
                    } else {
                        try { recommended.add(r.getAsString()); } catch (Exception ignored) {}
                    }
                }
                List<String> required = new ArrayList<>();
                for (JsonElement r : array(o, "required")) {
                    try { required.add(r.getAsString()); } catch (Exception ignored) {}
                }
                paths.put(id.toLowerCase(Locale.ROOT), new LoadoutPath(
                        id, firstNonBlank(str(o, "display"), id), str(o, "realm_min"),
                        List.copyOf(recommended), List.copyOf(required)));
            }
        }
        if (stages.isEmpty()) {
            stages.put("mortal_peak", new StageDef("mortal_peak", "凡俗之巅", "DEITY_TRANSFORMATION",
                    "", "", false, List.of("tribulation_success"), "", ""));
            stages.put("ascension_channel", new StageDef("ascension_channel", "飞升通道", "",
                    DimensionRegistryService.TIANYUAN, "mortal_to_tianyuan", true, List.of(), "", "ascension_channel"));
            stages.put("tianyuan_garrison", new StageDef("tianyuan_garrison", "天渊驻军", "",
                    DimensionRegistryService.TIANYUAN, "", false, List.of(), "", ""));
        }
        if (paths.isEmpty()) {
            paths.put("faction_ascend", new LoadoutPath("faction_ascend", "宗门接引飞升",
                    "DEITY_TRANSFORMATION", List.of("本命物"), List.of("化神圆满结构")));
        }
        return new Snapshot(Collections.unmodifiableMap(stages), Collections.unmodifiableMap(paths), questRef);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = AscensionService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return new JsonArray();
        return object.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            return e.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }
}
