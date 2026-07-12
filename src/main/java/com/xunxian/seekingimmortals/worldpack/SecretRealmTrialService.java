package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Light secret-realm trial shell: builds a temporary multiblock arena near the
 * travel anchor for P0 realms and grants a one-time rare-drop proxy reward.
 * Wave48: also spawns a lightweight guardian encounter at the core sanctum.
 * Not a full instanced dimension with custom biomes.
 */
public final class SecretRealmTrialService {
    private static final String REWARD_ROOT = "seeking_immortals_secret_realm_trial_rewards";
    private static final String ENCOUNTER_ROOT = "seeking_immortals_secret_realm_encounters";

    private SecretRealmTrialService() {}

    public static void onEnter(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (isShellTrialRealm(id)) {
            BlockPos center = player.blockPosition().above();
            int radius = shellRadiusFor(id);
            BlockPos coreCenter = center.offset(0, 0, radius + 4);
            // Wave46 layered zones: outer / mid / core.
            buildTrialShell(level, center, radius);
            buildInnerChamber(level, center.offset(radius + 4, 0, 0), Math.max(2, radius - 1));
            buildCoreSanctum(level, coreCenter, Math.max(2, radius - 1));
            placeLootChest(level, center.offset(0, 0, 2), id);
            placeLootChest(level, center.offset(radius + 4, 0, 0), id);
            placeLootChest(level, coreCenter, id);
            // Wave48: one-time guardian encounter per realm for this player.
            spawnCoreEncounter(level, player, coreCenter, id);
            BossEncounterService.spawnIfNeeded(player, "core_" + id);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, false, true, true));
            if (id.contains("void") || id.contains("diyuan") || id.contains("asura")) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 20, 0, false, true, true));
            }
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_shell_ready", id));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_multi_room", id));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_layers", id, "outer/mid/core"));
            ReputationService.add(player, "secret_realm_explorer", 2);
        }
        grantOneTimeRareDropProxy(player, id);
    }

    public static void onReturn(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    private static boolean isShellTrialRealm(String id) {
        return id.contains("blood_forbidden")
                || id.contains("fallen_demon")
                || id.contains("void_palace")
                || id.contains("nether_river")
                || id.contains("diyuan")
                || id.contains("kunwu")
                || id.contains("mist_cave")
                || id.contains("asura")
                || id.contains("jiuxian")
                || id.contains("king_");
    }

    private static int shellRadiusFor(String id) {
        if (id.contains("void") || id.contains("diyuan") || id.contains("asura")) {
            return 4;
        }
        if (id.contains("king_") || id.contains("kunwu")) {
            return 3;
        }
        return 3;
    }

    private static void buildTrialShell(ServerLevel level, BlockPos center, int radius) {
        int r = Math.max(2, Math.min(5, radius));
        // Outer ring of spirit ore + floor + corner pillars.
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.abs(x) == r || Math.abs(z) == r) {
                    setIfAir(level, center.offset(x, -1, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
                } else {
                    setIfAir(level, center.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
            }
        }
        setIfAir(level, center, ModBlocks.TELEPORT_ARRAY_PEDESTAL.get().defaultBlockState());
        int[] corners = {-r, r};
        for (int x : corners) {
            for (int z : corners) {
                setIfAir(level, center.offset(x, 0, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
                setIfAir(level, center.offset(x, 1, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
            }
        }
        // Clear standing room.
        int clear = Math.max(1, r - 1);
        for (int y = 1; y <= 3; y++) {
            for (int x = -clear; x <= clear; x++) {
                for (int z = -clear; z <= clear; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void setIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, state, 3);
        }
    }

    private static void buildInnerChamber(ServerLevel level, BlockPos center, int radius) {
        buildTrialShell(level, center, radius);
        // Mark inner room floor with spirit gathering arrays as chamber identity.
        setIfAir(level, center.offset(0, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
        setIfAir(level, center.offset(1, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
        setIfAir(level, center.offset(-1, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
    }

    private static void buildCoreSanctum(ServerLevel level, BlockPos center, int radius) {
        buildTrialShell(level, center, radius);
        // Core sanctum uses teleport pedestal + ore pillars as boss/core marker.
        setIfAir(level, center, ModBlocks.TELEPORT_ARRAY_PEDESTAL.get().defaultBlockState());
        setIfAir(level, center.offset(0, 1, 0), Blocks.AIR.defaultBlockState());
        setIfAir(level, center.offset(2, 0, 2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(-2, 0, 2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(2, 0, -2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(-2, 0, -2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
    }

    private static void placeLootChest(ServerLevel level, BlockPos pos, String realmId) {
        BlockPos chestPos = pos.above();
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            ItemStack loot = new ItemStack(proxyRewardItem(realmId), 1);
            chest.setItem(0, loot);
            chest.setItem(1, new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 4 + level.random.nextInt(5)));
            if (realmId.contains("void") || realmId.contains("palace")) {
                chest.setItem(2, new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
            }
            if (realmId.contains("demon") || realmId.contains("blood")) {
                chest.setItem(2, new ItemStack(ModItems.DEMONIC_BLOOD_CORAL.get(), 1));
            }
        }
    }

    /**
     * Wave48 encounter depth: spawn 1 guardian + 1-2 adds at the core sanctum once per realm.
     * Uses vanilla monsters as temporary combat shells (not GeckoLib bosses).
     */
    private static void spawnCoreEncounter(ServerLevel level, ServerPlayer player, BlockPos coreCenter, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(ENCOUNTER_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        EntityType<? extends Mob> guardianType = guardianTypeFor(realmId);
        EntityType<? extends Mob> addType = addTypeFor(realmId);
        Mob guardian = guardianType.create(level);
        if (guardian == null) {
            return;
        }
        BlockPos spawn = coreCenter.offset(0, 1, 0);
        guardian.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);
        scaleGuardian(guardian, realmId);
        guardian.setCustomName(Component.translatable("entity.seeking_immortals.trial_guardian.name", realmId));
        guardian.setCustomNameVisible(true);
        guardian.setPersistenceRequired();
        guardian.setTarget(player);
        level.addFreshEntity(guardian);
        int adds = realmId.contains("king") || realmId.contains("void") || realmId.contains("asura") ? 2 : 1;
        for (int i = 0; i < adds; i++) {
            Mob add = addType.create(level);
            if (add == null) {
                continue;
            }
            BlockPos addPos = coreCenter.offset((i == 0 ? 2 : -2), 1, (i == 0 ? 1 : -1));
            add.moveTo(addPos.getX() + 0.5D, addPos.getY(), addPos.getZ() + 0.5D, player.getYRot(), 0.0F);
            add.setPersistenceRequired();
            add.setTarget(player);
            level.addFreshEntity(add);
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(ENCOUNTER_ROOT, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_encounter", realmId, guardian.getName().getString(), adds));
        ReputationService.add(player, "secret_realm_explorer", 1);
    }

    private static EntityType<? extends Mob> guardianTypeFor(String realmId) {
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("ghost") || id.contains("yin") || id.contains("nether")) {
            return EntityType.WITHER_SKELETON;
        }
        if (id.contains("void") || id.contains("palace") || id.contains("asura")) {
            return EntityType.ENDERMAN;
        }
        if (id.contains("demon") || id.contains("blood")) {
            return EntityType.BLAZE;
        }
        if (id.contains("diyuan") || id.contains("kunwu") || id.contains("king")) {
            return EntityType.IRON_GOLEM;
        }
        return EntityType.VINDICATOR;
    }

    private static EntityType<? extends Mob> addTypeFor(String realmId) {
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("ghost") || id.contains("yin") || id.contains("nether")) {
            return EntityType.SKELETON;
        }
        if (id.contains("void") || id.contains("palace") || id.contains("asura")) {
            return EntityType.ENDERMITE;
        }
        if (id.contains("demon") || id.contains("blood")) {
            return EntityType.ZOMBIFIED_PIGLIN;
        }
        return EntityType.ZOMBIE;
    }

    private static void scaleGuardian(Mob guardian, String realmId) {
        double health = 40.0D;
        double damage = 6.0D;
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("void") || id.contains("asura") || id.contains("king")) {
            health = 70.0D;
            damage = 9.0D;
        } else if (id.contains("diyuan") || id.contains("demon") || id.contains("kunwu")) {
            health = 55.0D;
            damage = 7.5D;
        }
        if (guardian.getAttribute(Attributes.MAX_HEALTH) != null) {
            guardian.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            guardian.setHealth((float) health);
        }
        if (guardian.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            guardian.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (guardian.getAttribute(Attributes.ARMOR) != null) {
            guardian.getAttribute(Attributes.ARMOR).setBaseValue(6.0D);
        }
    }

    public static boolean hasEncountered(ServerPlayer player, String realmId) {
        if (player == null || realmId == null) {
            return false;
        }
        return player.getPersistentData().getCompound(ENCOUNTER_ROOT)
                .getBoolean(realmId.trim().toLowerCase(Locale.ROOT));
    }

    private static void grantOneTimeRareDropProxy(ServerPlayer player, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(REWARD_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        Item reward = proxyRewardItem(realmId);
        ItemStack stack = new ItemStack(reward, 1);
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(REWARD_ROOT, root);

        Optional<TextMaterialCatalogService.SecretRealmFlavor> flavor =
                TextMaterialCatalogService.builtin().findFlavor(realmId);
        List<String> rare = flavor.map(TextMaterialCatalogService.SecretRealmFlavor::rareDrops).orElse(List.of());
        if (!rare.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy",
                    String.join(", ", rare), stack.getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy_generic",
                    stack.getHoverName()));
        }
    }

    private static Item proxyRewardItem(String realmId) {
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("blood") || id.contains("demon")) {
            return ModItems.DEMONIC_BLOOD_CORAL.get();
        }
        if (id.contains("void") || id.contains("star")) {
            return ModItems.VOID_CRYSTAL.get();
        }
        if (id.contains("yin") || id.contains("ghost")) {
            return ModItems.YIN_STONE.get();
        }
        return ModItems.SPIRIT_STONE_SHARD.get();
    }
}
