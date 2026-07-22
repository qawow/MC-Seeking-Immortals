package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Runtime operational-state authority for multiblock stations.
 * form/inspect/repair/dismantle with material reserve → commit → refund on failure.
 */
public final class MultiblockOperationalService {
    private MultiblockOperationalService() {}

    public static MultiblockOperationalSavedData.StationState ensureState(
            ServerLevel level, String stationId, BlockPos origin) {
        if (level == null || origin == null || stationId == null || stationId.isBlank()) {
            return null;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        int maxHp = entry == null ? 100 : Math.max(1, entry.maxHp());
        String dim = level.dimension().location().toString();
        MultiblockOperationalSavedData data = MultiblockOperationalSavedData.get(level);
        Optional<MultiblockOperationalSavedData.StationState> existing = data.find(dim, stationId, origin);
        if (existing.isPresent()) {
            MultiblockOperationalSavedData.StationState state = existing.get();
            if (state.maxHp() != maxHp) {
                state = new MultiblockOperationalSavedData.StationState(
                        dim, stationId, origin.asLong(), state.state(),
                        Math.min(state.hp(), maxHp), maxHp);
                data.upsert(state);
            }
            return state;
        }
        // New stations stay uncommissioned (disabled) until form() pays structure materials.
        MultiblockOperationalSavedData.StationState created = new MultiblockOperationalSavedData.StationState(
                dim, stationId, origin.asLong(), MultiblockOperationalSavedData.OpState.DISABLED,
                0, maxHp);
        return data.upsert(created);
    }

    public static double efficiencyAt(Level level, String stationId, BlockPos origin) {
        if (!(level instanceof ServerLevel serverLevel) || origin == null || stationId == null) {
            return 1.0D;
        }
        MultiblockOperationalSavedData.StationState state = ensureState(serverLevel, stationId, origin);
        return state == null ? 1.0D : state.efficiency();
    }

    public static boolean isOperational(Level level, String stationId, BlockPos origin) {
        return efficiencyAt(level, stationId, origin) > 0.0D;
    }

    private static String unknownStationLabel() {
        // An unknown id is an internal lookup failure; never echo it to the player.
        return "未知工站";
    }

    private static Component stationDisplay(MultiblockStructureCatalog.StructureEntry entry) {
        return entry != null && PlayerDisplayText.isSafe(entry.display())
                ? Component.literal(entry.display().trim())
                : Component.literal(unknownStationLabel());
    }

    private static String stateDisplay(MultiblockOperationalSavedData.OpState state) {
        if (state == null) {
            return "未知状态";
        }
        return switch (state) {
            case INTACT -> "完好";
            case DAMAGED -> "受损";
            case CRITICAL -> "危急";
            case DISABLED -> "停用";
        };
    }

    public static boolean inspect(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.unknown", unknownStationLabel()), false);
            return false;
        }
        boolean formed = MultiblockStationService.isStationFormed(level, stationId, origin);
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (!formed && state.state() != MultiblockOperationalSavedData.OpState.DISABLED) {
            // Incomplete structure drifts toward damaged unless already worse.
            state = applyDamage(level, stationId, origin, Math.max(1, state.maxHp() / 10), false);
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.multiblock.inspect",
                stationDisplay(entry),
                stateDisplay(state.state()),
                state.hp(),
                state.maxHp(),
                (int) Math.round(state.efficiency() * 100.0D),
                formed ? Component.translatable("message.seeking_immortals.multiblock.formed")
                        : Component.translatable("message.seeking_immortals.multiblock.incomplete")), false);
        return true;
    }

    public static boolean repair(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.unknown", unknownStationLabel()), false);
            return false;
        }
        if (!MultiblockStationService.isStationFormed(level, stationId, origin)
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.repair_need_form", stationDisplay(entry)), true);
            return false;
        }
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (state.state() == MultiblockOperationalSavedData.OpState.INTACT
                && state.hp() >= state.maxHp()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.already_intact", stationDisplay(entry)), true);
            return false;
        }
        // Fully disabled stations (uncommissioned or destroyed) must pay structure
        // materials via form/overhaul; shard-only repair would bypass commissioning.
        if (state.state() == MultiblockOperationalSavedData.OpState.DISABLED
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.repair_need_commission", stationDisplay(entry)), true);
            return false;
        }

        int cost = repairCostShards(state);
        if (!player.getAbilities().instabuild) {
            // Reserve → commit materials first; refund on unexpected failure.
            List<ItemStack> taken = tryReserveShards(player, cost);
            if (taken == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.repair_need_shards", cost), true);
                return false;
            }
            try {
                MultiblockOperationalSavedData.StationState next = applyRepairStep(level, stationId, origin);
                if (next == null) {
                    refundStacks(player, taken);
                    return false;
                }
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.repaired",
                        stationDisplay(entry), stateDisplay(next.state()), next.hp(), next.maxHp(), cost), true);
                return true;
            } catch (RuntimeException exception) {
                refundStacks(player, taken);
                throw exception;
            }
        }

        MultiblockOperationalSavedData.StationState next = applyRepairStep(level, stationId, origin);
        if (next == null) {
            return false;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.multiblock.repaired",
                stationDisplay(entry), stateDisplay(next.state()), next.hp(), next.maxHp(), 0), true);
        return true;
    }

    public static boolean dismantle(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.unknown", unknownStationLabel()), false);
            return false;
        }
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        boolean transitioned = state.state() != MultiblockOperationalSavedData.OpState.DISABLED || state.hp() > 0;
        // Partial refund based on remaining integrity.
        int refund = Math.max(0, (int) Math.floor(repairCostShards(state) * state.efficiency()));
        MultiblockOperationalSavedData data = MultiblockOperationalSavedData.get(level);
        data.upsert(new MultiblockOperationalSavedData.StationState(
                state.dimensionId(), state.stationId(), state.packedOrigin(),
                MultiblockOperationalSavedData.OpState.DISABLED, 0, state.maxHp()));
        MultiblockStationService.markDirty(level, origin);
        if (transitioned) {
            emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.DISSIPATE,
                    stationMotif(stationId), 48);
        }
        if (refund > 0) {
            InventoryDeliveryService.giveOrEnqueue(player,
                    new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), refund),
                    "multiblock_dismantle:" + stationId);
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.multiblock.dismantled",
                stationDisplay(entry), refund), true);
        return true;
    }

    /**
     * Overhaul a formed station to intact using structure-specific materials (when resolvable)
     * plus a shard surcharge. Materials are reserved first; any failure refunds them.
     */

    /**
     * Commission a formed shell into operational service.
     * Reserves structure materials + shard surcharge for unresolved components, then commits INTACT.
     */
    public static boolean form(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.unknown", unknownStationLabel()), false);
            return false;
        }
        if (!MultiblockStationService.isStationFormed(level, stationId, origin)
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.form_need_shell", stationDisplay(entry)), true);
            return false;
        }
        MultiblockOperationalSavedData data = MultiblockOperationalSavedData.get(level);
        String dim = level.dimension().location().toString();
        Optional<MultiblockOperationalSavedData.StationState> existing = data.find(dim, stationId, origin);
        if (existing.isPresent()) {
            MultiblockOperationalSavedData.StationState state = existing.get();
            if (state.state() == MultiblockOperationalSavedData.OpState.INTACT && state.hp() >= state.maxHp()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.already_commissioned", stationDisplay(entry)), true);
                return false;
            }
            // Damaged stations must overhaul rather than re-form.
            if (state.state() != MultiblockOperationalSavedData.OpState.DISABLED
                    || state.hp() > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.form_use_overhaul", stationDisplay(entry)), true);
                return false;
            }
        }

        List<Item> materials = MultiblockMaterialCatalog.resolveItems(stationId);
        int unresolvedTax = MultiblockMaterialCatalog.unresolvedShardTax(stationId);
        int shardCost = Math.max(8, materials.isEmpty() ? 12 : 8) + unresolvedTax;
        if (!player.getAbilities().instabuild) {
            List<ItemStack> reserved = tryReserveMaterials(player, materials, 1);
            if (reserved == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.form_need_materials",
                        stationDisplay(entry), Math.max(1, materials.size())), true);
                return false;
            }
            List<ItemStack> shards = tryReserveShards(player, shardCost);
            if (shards == null) {
                refundStacks(player, reserved);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.form_need_shards", shardCost), true);
                return false;
            }
            try {
                MultiblockOperationalSavedData.StationState next = forceIntact(level, stationId, origin);
                if (next == null) {
                    refundStacks(player, reserved);
                    refundStacks(player, shards);
                    return false;
                }
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.formed_ok",
                        stationDisplay(entry), reserved.size(), shardCost), true);
                return true;
            } catch (RuntimeException exception) {
                refundStacks(player, reserved);
                refundStacks(player, shards);
                throw exception;
            }
        }

        MultiblockOperationalSavedData.StationState next = forceIntact(level, stationId, origin);
        if (next == null) {
            return false;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.multiblock.formed_ok",
                stationDisplay(entry), materials.size(), 0), true);
        return true;
    }

    public static boolean overhaul(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.unknown", unknownStationLabel()), false);
            return false;
        }
        if (!MultiblockStationService.isStationFormed(level, stationId, origin)
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.repair_need_form", stationDisplay(entry)), true);
            return false;
        }
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (state.state() == MultiblockOperationalSavedData.OpState.INTACT && state.hp() >= state.maxHp()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock.already_intact", stationDisplay(entry)), true);
            return false;
        }

        List<Item> materials = MultiblockMaterialCatalog.resolveItems(stationId);
        int unresolvedTax = MultiblockMaterialCatalog.unresolvedShardTax(stationId);
        // Prefer concrete materials when resolvable; tax only unresolved components.
        int shardCost = Math.max(repairCostShards(state) * 2, 12) + unresolvedTax;
        if (!player.getAbilities().instabuild) {
            List<ItemStack> reserved = tryReserveMaterials(player, materials, 1);
            if (reserved == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.overhaul_need_materials",
                        stationDisplay(entry), Math.max(1, materials.size())), true);
                return false;
            }
            List<ItemStack> shards = tryReserveShards(player, shardCost);
            if (shards == null) {
                refundStacks(player, reserved);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.repair_need_shards", shardCost), true);
                return false;
            }
            try {
                MultiblockOperationalSavedData.StationState next = forceIntact(level, stationId, origin);
                if (next == null) {
                    refundStacks(player, reserved);
                    refundStacks(player, shards);
                    return false;
                }
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock.overhauled",
                        stationDisplay(entry), reserved.size(), shardCost), true);
                return true;
            } catch (RuntimeException exception) {
                refundStacks(player, reserved);
                refundStacks(player, shards);
                throw exception;
            }
        }

        MultiblockOperationalSavedData.StationState next = forceIntact(level, stationId, origin);
        if (next == null) {
            return false;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.multiblock.overhauled",
                stationDisplay(entry), materials.size(), 0), true);
        return true;
    }

    private static MultiblockOperationalSavedData.StationState forceIntact(
            ServerLevel level, String stationId, BlockPos origin) {
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (state == null) {
            return null;
        }
        MultiblockOperationalSavedData.StationState next = new MultiblockOperationalSavedData.StationState(
                state.dimensionId(), state.stationId(), state.packedOrigin(),
                MultiblockOperationalSavedData.OpState.INTACT, state.maxHp(), state.maxHp());
        MultiblockOperationalSavedData.get(level).upsert(next);
        MultiblockStationService.markDirty(level, origin);
        TechniqueVfxPacket.Motif motif = stationMotif(stationId);
        emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.FORMATION, motif, 64);
        emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.CAST, motif, 34);
        return next;
    }

    /** Reserve one of each material. Null if short. Empty list if no resolvable materials. */
    private static List<ItemStack> tryReserveMaterials(ServerPlayer player, List<Item> materials, int each) {
        if (player == null) {
            return null;
        }
        if (materials == null || materials.isEmpty() || each <= 0) {
            return List.of();
        }
        // Pre-check counts without mutation.
        for (Item item : materials) {
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            int have = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(item)) {
                    have += stack.getCount();
                }
            }
            if (have < each) {
                return null;
            }
        }
        List<ItemStack> taken = new ArrayList<>();
        for (Item item : materials) {
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            int remaining = each;
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(item)) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                ItemStack copy = stack.copy();
                copy.setCount(take);
                taken.add(copy);
                stack.shrink(take);
                remaining -= take;
            }
            if (remaining > 0) {
                refundStacks(player, taken);
                return null;
            }
        }
        return taken;
    }


    public static MultiblockOperationalSavedData.StationState applyDamage(
            ServerLevel level, String stationId, BlockPos origin, int amount, boolean announce) {
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (state == null || amount <= 0) {
            return state;
        }
        int hp = Math.max(0, state.hp() - amount);
        MultiblockOperationalSavedData.OpState nextState = stateFromHp(hp, state.maxHp());
        boolean disabledNow = nextState == MultiblockOperationalSavedData.OpState.DISABLED
                && state.state() != MultiblockOperationalSavedData.OpState.DISABLED;
        MultiblockOperationalSavedData.StationState next = new MultiblockOperationalSavedData.StationState(
                state.dimensionId(), state.stationId(), state.packedOrigin(), nextState, hp, state.maxHp());
        MultiblockOperationalSavedData.get(level).upsert(next);
        MultiblockStationService.markDirty(level, origin);
        emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.IMPACT,
                stationMotif(stationId), disabledNow ? 52 : 30);
        if (disabledNow) {
            emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.DISSIPATE,
                    stationMotif(stationId), 58);
        }
        return next;
    }

    private static MultiblockOperationalSavedData.StationState applyRepairStep(
            ServerLevel level, String stationId, BlockPos origin) {
        MultiblockOperationalSavedData.StationState state = ensureState(level, stationId, origin);
        if (state == null) {
            return null;
        }
        int heal = Math.max(1, state.maxHp() / 4);
        int hp = Math.min(state.maxHp(), state.hp() + heal);
        MultiblockOperationalSavedData.OpState nextState = stateFromHp(hp, state.maxHp());
        MultiblockOperationalSavedData.StationState next = new MultiblockOperationalSavedData.StationState(
                state.dimensionId(), state.stationId(), state.packedOrigin(), nextState, hp, state.maxHp());
        MultiblockOperationalSavedData.get(level).upsert(next);
        MultiblockStationService.markDirty(level, origin);
        emitStationVfx(level, stationId, origin, TechniqueVfxPacket.Kind.STATUS,
                TechniqueVfxPacket.Motif.CLEANSE, 30);
        return next;
    }

    private static void emitStationVfx(ServerLevel level, String stationId, BlockPos origin,
                                       TechniqueVfxPacket.Kind kind, TechniqueVfxPacket.Motif motif,
                                       int intensity) {
        if (level == null || origin == null || kind == null) {
            return;
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(stationId).orElse(null);
        String semantic = stationId == null ? "" : stationId;
        if (entry != null) {
            semantic = semantic + " " + entry.type() + " " + entry.display();
        }
        TechniqueVfxPalette.Family family = stationFamily(semantic);
        double radius = entry == null ? 2.0D : Math.max(1.5D, Math.min(10.0D, entry.radius()));
        Vec3 center = Vec3.atCenterOf(origin).add(0.0D, 0.08D, 0.0D);
        Vec3 end = kind == TechniqueVfxPacket.Kind.CAST
                ? center.add(0.0D, Math.max(1.2D, Math.min(4.0D, radius * 0.6D)), 0.0D)
                : center;
        long seed = origin.asLong()
                ^ ((long) semantic.hashCode() << 17)
                ^ ((long) kind.ordinal() << 52)
                ^ level.getGameTime();
        TechniqueVfxPacket.send(level, kind, family,
                motif == null ? TechniqueVfxPacket.Motif.DOMAIN : motif,
                center, end, radius, intensity, seed);
    }

    static TechniqueVfxPacket.Motif stationMotif(String stationId) {
        String key = stationId == null ? "" : stationId.trim().toLowerCase(Locale.ROOT);
        if (containsAny(key, "gate", "rift", "teleport", "ferry", "portal", "cycle")) {
            return TechniqueVfxPacket.Motif.TELEPORT;
        }
        if (containsAny(key, "array", "formation")) {
            return TechniqueVfxPacket.Motif.FORMATION;
        }
        if (containsAny(key, "altar", "seal", "suppress")) {
            return TechniqueVfxPacket.Motif.SEAL;
        }
        if (containsAny(key, "furnace", "forge", "alchemy", "refine", "craft")) {
            return TechniqueVfxPacket.Motif.CHANNEL;
        }
        if (containsAny(key, "shield", "ward", "defense")) {
            return TechniqueVfxPacket.Motif.SHIELD;
        }
        if (containsAny(key, "garden", "planter", "herb", "spring")) {
            return TechniqueVfxPacket.Motif.HEAL;
        }
        return TechniqueVfxPacket.Motif.DOMAIN;
    }

    static TechniqueVfxPalette.Family stationFamily(String semantic) {
        String key = semantic == null ? "" : semantic.trim().toLowerCase(Locale.ROOT);
        TechniqueVfxPalette.Family family = TechniqueVfxPalette.familyOf(key);
        if (family != TechniqueVfxPalette.Family.NEUTRAL) {
            return family;
        }
        if (containsAny(key, "furnace", "forge", "alchemy", "refine")) {
            return TechniqueVfxPalette.Family.FIRE;
        }
        if (containsAny(key, "garden", "planter", "herb", "pill")) {
            return TechniqueVfxPalette.Family.WOOD;
        }
        if (containsAny(key, "gate", "rift", "teleport", "ferry", "portal", "cycle")) {
            return TechniqueVfxPalette.Family.VOID;
        }
        return TechniqueVfxPalette.Family.NEUTRAL;
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static MultiblockOperationalSavedData.OpState stateFromHp(int hp, int maxHp) {
        if (hp <= 0) {
            return MultiblockOperationalSavedData.OpState.DISABLED;
        }
        double ratio = hp / (double) Math.max(1, maxHp);
        if (ratio >= 0.90D) {
            return MultiblockOperationalSavedData.OpState.INTACT;
        }
        if (ratio >= 0.40D) {
            return MultiblockOperationalSavedData.OpState.DAMAGED;
        }
        return MultiblockOperationalSavedData.OpState.CRITICAL;
    }

    static int repairCostShards(MultiblockOperationalSavedData.StationState state) {
        if (state == null) {
            return 8;
        }
        return switch (state.state()) {
            case INTACT -> 4;
            case DAMAGED -> 8;
            case CRITICAL -> 16;
            case DISABLED -> 24;
        };
    }

    /** Returns taken stacks on success, null if inventory short. */
    private static List<ItemStack> tryReserveShards(ServerPlayer player, int count) {
        if (count <= 0) {
            return List.of();
        }
        Item shard = ModItems.SPIRIT_STONE_SHARD.get();
        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(shard)) {
                available += stack.getCount();
            }
        }
        if (available < count) {
            return null;
        }
        List<ItemStack> taken = new ArrayList<>();
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            ItemStack copy = stack.copy();
            copy.setCount(take);
            taken.add(copy);
            stack.shrink(take);
            remaining -= take;
        }
        return remaining == 0 ? taken : null;
    }

    private static void refundStacks(ServerPlayer player, List<ItemStack> stacks) {
        if (player == null || stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            InventoryDeliveryService.giveOrEnqueue(player, stack, "multiblock_repair_refund");
        }
    }


    /**
     * Ensure a formed shell is commissioned. If already operational, returns true.
     * Otherwise attempts form() (material reserve → forceIntact) and returns its result.
     */
    public static boolean ensureCommissioned(ServerPlayer player, String stationId, BlockPos origin) {
        if (player == null || !(player.level() instanceof ServerLevel level) || origin == null) {
            return false;
        }
        if (!MultiblockStationService.isStationFormed(level, stationId, origin)
                && !player.getAbilities().instabuild) {
            return false;
        }
        if (efficiencyAt(level, stationId, origin) > 0.0D) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return forceIntact(level, stationId, origin) != null;
        }
        return form(player, stationId, origin);
    }

    /**
     * Scan near the player for formed-but-uncommissioned stations and attempt form once.
     * Returns true if any station became operational.
     */
    public static boolean tryCommissionNearby(ServerPlayer player, String... stationIds) {
        if (player == null || player.level() == null || stationIds == null || stationIds.length == 0) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        BlockPos origin = player.blockPosition();
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    for (String stationId : stationIds) {
                        if (!MultiblockStationService.isStationFormed(player.level(), stationId, pos)) {
                            continue;
                        }
                        if (efficiencyAt(player.level(), stationId, pos) > 0.0D) {
                            return true;
                        }
                        if (form(player, stationId, pos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Nearby formed+operational station efficiency for soft craft gates. */
    public static double bestNearbyEfficiency(ServerPlayer player, String... stationIds) {
        if (player == null || player.level() == null || stationIds == null || stationIds.length == 0) {
            return 0.0D;
        }
        if (player.getAbilities().instabuild) {
            return 1.0D;
        }
        BlockPos origin = player.blockPosition();
        double best = 0.0D;
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    for (String stationId : stationIds) {
                        if (!MultiblockStationService.isStationFormed(player.level(), stationId, pos)) {
                            continue;
                        }
                        best = Math.max(best, efficiencyAt(player.level(), stationId, pos));
                    }
                }
            }
        }
        return best;
    }

    public static String normalizeStationId(String stationId) {
        return stationId == null ? "" : stationId.trim().toLowerCase(Locale.ROOT);
    }
}
