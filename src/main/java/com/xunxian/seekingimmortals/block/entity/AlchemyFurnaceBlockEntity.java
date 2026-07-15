package com.xunxian.seekingimmortals.block.entity;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeService;
import com.xunxian.seekingimmortals.block.AlchemyFurnaceBlock;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.block.AlchemyLidBlock;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyTieredItem;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.structure.AlchemyFurnaceShellStructure;
import com.xunxian.seekingimmortals.structure.SectEarthFireRoomMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Wave499/500:
 * - GUI slots authority for formula/fire
 * - Lid is a placed multiblock block above the furnace (not a GUI item)
 * - FORMED is a blockstate driven by shell structure
 * Ingredient consumption still uses player inventory (AlchemyRecipeService).
 */
public class AlchemyFurnaceBlockEntity extends BlockEntity {
    public static final int SLOT_FORMULA = 0;
    public static final int SLOT_INGREDIENT = 1;
    /** Legacy slot index kept for save/layout compatibility; no longer accepts items. */
    public static final int SLOT_LID = 2;
    public static final int SLOT_FIRE = 3;
    public static final int SLOT_OUTPUT = 4;

    private static final int EARTH_FIRE_TIER = 4;
    private static final int FORM_REFRESH_INTERVAL = 20;

    private String recipeId = "";
    private String knownFormulaId = "";
    private AlchemyFormulaSource formulaSource = AlchemyFormulaSource.PAPER;
    private int lidTier;
    private int fireTier;
    private ItemStack storedOutput = ItemStack.EMPTY;
    private int progressTicks;
    private int totalTicks;
    private double successRate;
    private double explosionChance;
    private UUID craftingPlayerId;
    private int formRefreshCooldown;
    private boolean migratedLegacyComponents;

    private final ItemStackHandler items = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_FORMULA || slot == SLOT_FIRE) {
                syncComponentsFromSlots();
            }
            if (slot == SLOT_LID && !getStackInSlot(SLOT_LID).isEmpty()) {
                // Wave500: lid is a world block; eject accidental slot inserts.
                // Cannot call level-sensitive helpers here safely; clear and leave to interact path.
                setStackInSlot(SLOT_LID, ItemStack.EMPTY);
            }
            if (slot == SLOT_OUTPUT && !getStackInSlot(SLOT_OUTPUT).isEmpty()) {
                storedOutput = getStackInSlot(SLOT_OUTPUT).copy();
            }
            setChanged();
        }
    };

    private final ContainerData data = new SimpleContainerData(4) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progressTicks;
                case 1 -> totalTicks;
                case 2 -> isFormed() ? 1 : 0;
                case 3 -> hasEarthFireRoomNearby() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progressTicks = value;
                case 1 -> totalTicks = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ItemStackHandler getItemHandler() {
        return items;
    }

    public ContainerData getContainerData() {
        return data;
    }

    public AlchemyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMY_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity furnace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        furnace.formRefreshCooldown--;
        if (furnace.formRefreshCooldown <= 0) {
            furnace.formRefreshCooldown = FORM_REFRESH_INTERVAL;
            furnace.refreshFormedState(serverLevel, false);
        }
        if (furnace.progressTicks <= 0) {
            return;
        }
        // Wave498/499: if the multiblock shell breaks mid-cook, abort and waste the batch.
        if (!AlchemyFurnaceShellStructure.isComplete(serverLevel, pos, furnace.getFurnaceTier())) {
            furnace.refreshFormedState(serverLevel, false);
            furnace.abortForBrokenShell(serverLevel);
            return;
        }
        furnace.progressTicks--;
        furnace.setChanged();
        if (furnace.progressTicks <= 0) {
            furnace.finishCraft(serverLevel);
        }
    }

    public void interact(ServerPlayer player, ItemStack held) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        refreshFormedState(serverLevel, false);
        syncComponentsFromSlots();

        if (!storedOutput.isEmpty() || !items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            giveOutput(player);
            return;
        }
        if (progressTicks > 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.progress",
                    getRecipeName(), totalTicks - progressTicks, totalTicks), true);
            return;
        }
        // Wave499: components go into GUI authority slots.
        if (tryInsertComponentToSlot(serverLevel, player, held)) {
            return;
        }
        findRecipeForHeldItem(held).ifPresentOrElse(recipe -> startRecipe(serverLevel, player, recipe),
                () -> {
                    int tier = getFurnaceTier();
                    int required = AlchemyFurnaceShellStructure.requiredCount(tier);
                    int present = AlchemyFurnaceShellStructure.presentCount(serverLevel, worldPosition, tier);
                    boolean shellOk = isFormed() && present >= required;
                    boolean room = hasEarthFireRoom(serverLevel);
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.idle",
                            tier, lidTier, fireTier, describeFormula(),
                            present, required,
                            shellOk
                                    ? Component.translatable("message.seeking_immortals.alchemy_furnace.shell_ok")
                                    : Component.translatable("message.seeking_immortals.alchemy_furnace.shell_bad"),
                            room
                                    ? Component.translatable("message.seeking_immortals.alchemy_furnace.room_ok")
                                    : Component.translatable("message.seeking_immortals.alchemy_furnace.room_bad")), false);
                });
    }

    public void refreshFormedState(ServerLevel serverLevel, boolean forceParticles) {
        if (serverLevel == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AlchemyFurnaceBlock)) {
            return;
        }
        boolean complete = AlchemyFurnaceShellStructure.isComplete(serverLevel, worldPosition, getFurnaceTier());
        boolean wasFormed = state.getValue(AlchemyFurnaceBlock.FORMED);
        if (complete == wasFormed && !forceParticles) {
            return;
        }
        // Preserve BE while flipping formed flag.
        serverLevel.setBlock(worldPosition, state.setValue(AlchemyFurnaceBlock.FORMED, complete), 3);
        if (complete && !wasFormed) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D,
                    18, 0.45D, 0.35D, 0.45D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D, worldPosition.getZ() + 0.5D,
                    8, 0.35D, 0.2D, 0.35D, 0.0D);
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.15F);
        } else if (!complete && wasFormed) {
            serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45F, 0.9F);
        }
        setChanged();
    }

    public boolean isFormed() {
        BlockState state = getBlockState();
        return state.hasProperty(AlchemyFurnaceBlock.FORMED) && state.getValue(AlchemyFurnaceBlock.FORMED);
    }

    private void abortForBrokenShell(ServerLevel serverLevel) {
        ServerPlayer crafter = craftingPlayerId == null
                ? null
                : serverLevel.getServer().getPlayerList().getPlayer(craftingPlayerId);
        if (crafter != null) {
            crafter.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.shell_broken"), false);
        }
        ItemStack waste = new ItemStack(ModItems.WASTE_PILL.get());
        storedOutput = waste.copy();
        items.setStackInSlot(SLOT_OUTPUT, waste);
        progressTicks = 0;
        totalTicks = 0;
        successRate = 0.0D;
        explosionChance = 0.0D;
        craftingPlayerId = null;
        recipeId = "";
        serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.7F);
        setChanged();
    }

    public void dropStoredContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Wave499: drop GUI handler stacks as authority inventory.
        for (int i = 0; i < items.getSlots(); i++) {
            dropItemStack(items.getStackInSlot(i));
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
        // storedOutput may duplicate SLOT_OUTPUT; only drop if output slot already empty path failed.
        if (!storedOutput.isEmpty() && items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            // already dropped above when present in slot; clear residual
        }
        storedOutput = ItemStack.EMPTY;
        lidTier = 0;
        fireTier = 0;
        knownFormulaId = "";
        formulaSource = AlchemyFormulaSource.PAPER;
        resetCookState();
    }

    private void startRecipe(ServerLevel serverLevel, ServerPlayer player, AlchemyRecipe recipe) {
        syncComponentsFromSlots();
        int furnaceTier = getFurnaceTier();
        if (!isFormed() || !AlchemyFurnaceShellStructure.isComplete(serverLevel, worldPosition, furnaceTier)) {
            int missing = AlchemyFurnaceShellStructure.missingOffsets(serverLevel, worldPosition, furnaceTier).size();
            int required = AlchemyFurnaceShellStructure.requiredCount(furnaceTier);
            int present = Math.max(0, required - missing);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.shell_incomplete",
                    furnaceTier, present, required, missing), true);
            return;
        }
        if (!hasInstalledFormula(recipe)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.no_formula", recipe.displayName()), true);
            return;
        }
        if (lidTier <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.no_lid"), true);
            return;
        }
        if (fireTier <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.no_fire"), true);
            return;
        }
        if (fireTier > furnaceTier) {
            explodeFurnace(serverLevel, player);
            return;
        }
        if (fireTier > lidTier) {
            blowLid(serverLevel, player);
            return;
        }
        boolean hasEarthFireRoom = hasEarthFireRoom(serverLevel);
        if (recipe.requiresEarthFireRoom()) {
            if (fireTier != EARTH_FIRE_TIER || !hasEarthFireRoom) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.recipe_needs_earth_fire_room",
                        recipe.displayName(), Component.translatable("item.seeking_immortals.earth_fire")), true);
                return;
            }
        } else if (fireTier == EARTH_FIRE_TIER && !hasEarthFireRoom) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.fire_needs_earth_room",
                    Component.translatable("item.seeking_immortals.earth_fire")), true);
            return;
        }
        int requiredSkill = Math.max(1, recipe.requiredFurnaceTier() * 2 - 1);
        int alchemyLevel = CultivationHelper.get(player)
                .map(c -> {
                    var skill = c.getSkill(SkillType.ALCHEMY);
                    return skill == null ? 0 : skill.getLevel();
                }).orElse(0);
        if (alchemyLevel < requiredSkill && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.skill_too_low",
                    recipe.displayName(), requiredSkill, alchemyLevel), true);
            return;
        }
        if (furnaceTier < recipe.requiredFurnaceTier() || fireTier < recipe.idealFireTier() || !AlchemyRecipeService.hasRealmControl(player, recipe)) {
            if (AlchemyRecipeService.consumeHalfInputs(player, recipe)) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.half_waste",
                        recipe.displayName(), furnaceTier, recipe.requiredFurnaceTier(), fireTier, recipe.idealFireTier(), recipe.minControlRealm().getDisplayName()), false);
                serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.7F);
            } else {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.missing",
                        recipe.displayName(), AlchemyRecipeService.missingSummary(player, recipe)), true);
            }
            setChanged();
            return;
        }
        if (!AlchemyRecipeService.consumeInputs(player, recipe)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.missing",
                    recipe.displayName(), AlchemyRecipeService.missingSummary(player, recipe)), true);
            return;
        }
        recipeId = recipe.id();
        totalTicks = recipe.cookTicks();
        progressTicks = totalTicks;
        successRate = AlchemyRecipeService.successRate(serverLevel, player, recipe, furnaceTier, fireTier, formulaSource);
        explosionChance = AlchemyRecipeService.explosionChance(player, recipe, furnaceTier, lidTier, fireTier, formulaSource);
        craftingPlayerId = player.getUUID();
        player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.started",
                recipe.displayName(), recipe.manaCost(), (int) Math.round(successRate * 100.0D)), false);
        serverLevel.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4F, 0.8F);
        setChanged();
    }

    private boolean tryInsertComponentToSlot(ServerLevel serverLevel, ServerPlayer player, ItemStack held) {
        if (held.isEmpty()) {
            return false;
        }
        // Wave500: lids are placeable blocks on top of the furnace controller.
        if (held.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AlchemyLidBlock lidBlock) {
            BlockPos lidPos = worldPosition.above();
            BlockState existing = serverLevel.getBlockState(lidPos);
            if (existing.getBlock() instanceof AlchemyLidBlock existingLid && existingLid.tier() == lidBlock.tier()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.alchemy_furnace.lid_already_installed", lidBlock.tier()), true);
                return true;
            }
            if (!existing.isAir() && !(existing.getBlock() instanceof AlchemyLidBlock) && !existing.canBeReplaced()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.alchemy_furnace.lid_blocked"), true);
                return true;
            }
            if (existing.getBlock() instanceof AlchemyLidBlock) {
                net.minecraft.world.level.block.Block.dropResources(existing, serverLevel, lidPos);
                serverLevel.removeBlock(lidPos, false);
            }
            serverLevel.setBlock(lidPos, lidBlock.defaultBlockState(), 3);
            shrinkInstalledItem(player, held);
            syncComponentsFromSlots();
            refreshFormedState(serverLevel, true);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.lid_installed", lidBlock.tier()), false);
            setChanged();
            return true;
        }
        if (held.getItem() instanceof AlchemyTieredItem tieredItem
                && tieredItem.componentType() == AlchemyTieredItem.ComponentType.FIRE) {
            if (!canInstallFire(serverLevel, player, held, tieredItem)) {
                return true;
            }
            ItemStack existing = items.getStackInSlot(SLOT_FIRE);
            if (!existing.isEmpty()
                    && existing.getItem() instanceof AlchemyTieredItem existingTiered
                    && existingTiered.componentType() == AlchemyTieredItem.ComponentType.FIRE
                    && existingTiered.tier() == tieredItem.tier()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.alchemy_furnace.fire_already_installed",
                        tieredItem.tier()), true);
                return true;
            }
            if (!existing.isEmpty()) {
                returnInstalledItem(player, existing);
            }
            ItemStack one = held.copy();
            one.setCount(1);
            items.setStackInSlot(SLOT_FIRE, one);
            shrinkInstalledItem(player, held);
            syncComponentsFromSlots();
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.fire_installed",
                    tieredItem.tier()), false);
            setChanged();
            return true;
        }
        if (held.getItem() instanceof AlchemyFormulaItem formulaItem) {
            ItemStack existing = items.getStackInSlot(SLOT_FORMULA);
            if (!existing.isEmpty()
                    && existing.getItem() instanceof AlchemyFormulaItem existingFormula
                    && existingFormula.recipeId().equals(formulaItem.recipeId())
                    && existingFormula.source() == formulaItem.source()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.formula_already_installed",
                        Component.translatable("alchemy_recipe.seeking_immortals." + formulaItem.recipeId()),
                        Component.translatable("alchemy_formula_source.seeking_immortals." + formulaItem.source().id())), true);
                return true;
            }
            if (!existing.isEmpty()) {
                returnInstalledItem(player, existing);
            }
            ItemStack one = held.copy();
            one.setCount(1);
            items.setStackInSlot(SLOT_FORMULA, one);
            shrinkInstalledItem(player, held);
            syncComponentsFromSlots();
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.formula_installed",
                    Component.translatable("alchemy_recipe.seeking_immortals." + formulaItem.recipeId()),
                    Component.translatable("alchemy_formula_source.seeking_immortals." + formulaItem.source().id())), false);
            setChanged();
            return true;
        }
        return false;
    }

    public void syncComponentsFromSlots() {
        ItemStack formula = items.getStackInSlot(SLOT_FORMULA);
        if (formula.getItem() instanceof AlchemyFormulaItem formulaItem) {
            knownFormulaId = formulaItem.recipeId();
            formulaSource = formulaItem.source();
        } else {
            knownFormulaId = "";
            formulaSource = AlchemyFormulaSource.PAPER;
        }
        // Wave500: lid tier comes from the placed lid block above the furnace.
        if (level != null) {
            lidTier = AlchemyFurnaceShellStructure.lidTierAt(level, worldPosition).orElse(0);
        } else {
            lidTier = 0;
        }
        // Clear legacy lid slot residue so it cannot shadow world lid authority.
        if (!items.getStackInSlot(SLOT_LID).isEmpty()) {
            items.setStackInSlot(SLOT_LID, ItemStack.EMPTY);
        }
        ItemStack fire = items.getStackInSlot(SLOT_FIRE);
        if (fire.getItem() instanceof AlchemyTieredItem tiered
                && tiered.componentType() == AlchemyTieredItem.ComponentType.FIRE) {
            fireTier = tiered.tier();
        } else {
            fireTier = 0;
        }
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        storedOutput = out.isEmpty() ? ItemStack.EMPTY : out.copy();
    }

    private void finishCraft(ServerLevel serverLevel) {
        AlchemyRecipe recipe = AlchemyRecipe.findById(recipeId).orElse(null);
        if (recipe == null) {
            resetCookState();
            return;
        }
        double roll = serverLevel.random.nextDouble();
        if (roll < explosionChance) {
            discardStoredContents();
            serverLevel.destroyBlock(worldPosition, false);
            serverLevel.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    1.0F, Level.ExplosionInteraction.BLOCK);
            return;
        }
        if (roll < explosionChance + successRate) {
            ItemStack result = new ItemStack(rollOutputQuality(serverLevel, recipe, roll), recipe.outputCount());
            storedOutput = result.copy();
            items.setStackInSlot(SLOT_OUTPUT, result);
            grantAlchemyExperience(serverLevel);
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.2F);
        } else {
            ItemStack waste = new ItemStack(ModItems.WASTE_PILL.get());
            storedOutput = waste.copy();
            items.setStackInSlot(SLOT_OUTPUT, waste);
            serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
        progressTicks = 0;
        totalTicks = 0;
        successRate = 0.0D;
        explosionChance = 0.0D;
        setChanged();
    }

    private void explodeFurnace(ServerLevel serverLevel, ServerPlayer player) {
        int furnaceTier = getFurnaceTier();
        player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.explode_furnace", fireTier, furnaceTier), false);
        discardStoredContents();
        serverLevel.destroyBlock(worldPosition, false);
        serverLevel.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                1.4F, Level.ExplosionInteraction.BLOCK);
    }

    private void blowLid(ServerLevel serverLevel, ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.explode_lid", fireTier, lidTier), false);
        BlockPos lidPos = worldPosition.above();
        BlockState lidState = serverLevel.getBlockState(lidPos);
        if (lidState.getBlock() instanceof AlchemyLidBlock) {
            serverLevel.destroyBlock(lidPos, false);
        }
        items.setStackInSlot(SLOT_LID, ItemStack.EMPTY);
        lidTier = 0;
        refreshFormedState(serverLevel, false);
        DamageSource source = serverLevel.damageSources().explosion(null, null);
        serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(2.5D))
                .forEach(target -> target.hurt(source, 4.0F));
        serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.6F, 1.3F);
        setChanged();
    }

    private void giveOutput(ServerPlayer player) {
        ItemStack result = items.getStackInSlot(SLOT_OUTPUT);
        if (result.isEmpty()) {
            result = storedOutput.copy();
        } else {
            result = result.copy();
        }
        items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        storedOutput = ItemStack.EMPTY;
        if (result.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.collected", result.getHoverName(), result.getCount()), false);
        setChanged();
    }

    private String getRecipeName() {
        return AlchemyRecipe.findById(recipeId)
                .map(recipe -> recipe.displayName().getString())
                .orElse(recipeId);
    }

    private boolean hasInstalledFormula(AlchemyRecipe recipe) {
        return !knownFormulaId.isBlank() && knownFormulaId.equals(recipe.id());
    }

    private java.util.Optional<AlchemyRecipe> findRecipeForHeldItem(ItemStack held) {
        if (knownFormulaId.isBlank()) {
            return AlchemyRecipe.findByHeldIngredient(held);
        }
        return AlchemyRecipe.findById(knownFormulaId)
                .filter(recipe -> recipe.acceptsHeldIngredient(held));
    }

    private String describeFormula() {
        if (knownFormulaId.isBlank()) {
            return "-";
        }
        return AlchemyRecipe.findById(knownFormulaId)
                .map(recipe -> recipe.displayName().getString())
                .orElse(knownFormulaId);
    }

    private void shrinkInstalledItem(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void returnInstalledItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack returned = stack.copy();
        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }
    }

    private boolean canInstallFire(ServerLevel serverLevel, ServerPlayer player, ItemStack held, AlchemyTieredItem fireItem) {
        boolean realmOk = fireItem.minRealm().ordinal() == 0 || CultivationHelper.get(player)
                .map(cultivation -> cultivation.getRealm().ordinal() >= fireItem.minRealm().ordinal())
                .orElse(false);
        if (!realmOk) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.fire_realm_too_low",
                    held.getHoverName(), fireItem.minRealm().getDisplayName()), true);
            return false;
        }
        if (fireItem.requiresEarthFireRoom() && !hasEarthFireRoom(serverLevel)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.fire_needs_earth_room",
                    held.getHoverName()), true);
            return false;
        }
        return true;
    }

    private boolean hasEarthFireRoom(ServerLevel serverLevel) {
        return SectEarthFireRoomMultiblock.hasCompleteRoom(serverLevel, worldPosition);
    }

    private boolean hasEarthFireRoomNearby() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return hasEarthFireRoom(serverLevel);
    }

    private int getFurnaceTier() {
        if (getBlockState().getBlock() instanceof AlchemyFurnaceBlock furnaceBlock) {
            return furnaceBlock.tier();
        }
        return 1;
    }

    private Item rollOutputQuality(ServerLevel serverLevel, AlchemyRecipe recipe, double realRoll) {
        if (!recipe.isQualityVariable()) {
            return recipe.output();
        }
        double successThreshold = explosionChance + successRate;
        double margin = successThreshold <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, (successThreshold - realRoll) / successRate));
        double normalized = 1.0D - margin;
        ServerPlayer crafter = craftingPlayerId == null ? null : serverLevel.getServer().getPlayerList().getPlayer(craftingPlayerId);
        double alchemyBonus = crafter == null ? 0.0D : AlchemyRecipeService.getAlchemySkillBonus(crafter);
        double qualityScore = normalized * 0.8D + alchemyBonus;
        com.xunxian.seekingimmortals.item.pill.PillQuality quality;
        if (qualityScore >= 0.95D) {
            quality = com.xunxian.seekingimmortals.item.pill.PillQuality.SUPREME;
        } else if (qualityScore >= 0.75D) {
            quality = com.xunxian.seekingimmortals.item.pill.PillQuality.HIGH;
        } else if (qualityScore >= 0.50D) {
            quality = com.xunxian.seekingimmortals.item.pill.PillQuality.MEDIUM;
        } else {
            quality = com.xunxian.seekingimmortals.item.pill.PillQuality.LOW;
        }
        return recipe.outputForQuality(quality);
    }

    private void grantAlchemyExperience(ServerLevel serverLevel) {
        if (craftingPlayerId == null) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(craftingPlayerId);
        if (player == null) {
            return;
        }
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player, SkillType.ALCHEMY, 25, 10);
    }

    private void resetCookState() {
        recipeId = "";
        progressTicks = 0;
        totalTicks = 0;
        successRate = 0.0D;
        explosionChance = 0.0D;
        craftingPlayerId = null;
        setChanged();
    }

    private void discardStoredContents() {
        for (int i = 0; i < items.getSlots(); i++) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
        lidTier = 0;
        fireTier = 0;
        knownFormulaId = "";
        formulaSource = AlchemyFormulaSource.PAPER;
        storedOutput = ItemStack.EMPTY;
        resetCookState();
    }

    private void dropItemStack(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return;
        }
        Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stack.copy());
    }

    /** One-time migration: scalar-only installs become slot stacks (fire/formula/output only). */
    private void migrateLegacyComponentsIntoSlots() {
        if (migratedLegacyComponents) {
            return;
        }
        migratedLegacyComponents = true;
        // Lid is world structure now; drop any legacy lid slot item for the player world to place manually.
        if (!items.getStackInSlot(SLOT_LID).isEmpty()) {
            dropItemStack(items.getStackInSlot(SLOT_LID));
            items.setStackInSlot(SLOT_LID, ItemStack.EMPTY);
        }
        if (items.getStackInSlot(SLOT_FIRE).isEmpty() && fireTier > 0) {
            ItemStack stack = installedFireStack(fireTier);
            if (!stack.isEmpty()) {
                items.setStackInSlot(SLOT_FIRE, stack);
            }
        }
        if (items.getStackInSlot(SLOT_FORMULA).isEmpty() && !knownFormulaId.isBlank()) {
            ItemStack stack = installedFormulaStack();
            if (!stack.isEmpty()) {
                items.setStackInSlot(SLOT_FORMULA, stack);
            }
        }
        if (items.getStackInSlot(SLOT_OUTPUT).isEmpty() && !storedOutput.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, storedOutput.copy());
        }
        syncComponentsFromSlots();
    }

    private ItemStack installedFireStack(int tier) {
        if (tier <= 0) {
            return ItemStack.EMPTY;
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof AlchemyTieredItem tieredItem
                    && tieredItem.componentType() == AlchemyTieredItem.ComponentType.FIRE
                    && tieredItem.tier() == tier) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack installedFormulaStack() {
        if (knownFormulaId.isBlank()) {
            return ItemStack.EMPTY;
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof AlchemyFormulaItem formulaItem
                    && formulaItem.recipeId().equals(knownFormulaId)
                    && formulaItem.source() == formulaSource) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        syncComponentsFromSlots();
        tag.put("Items", items.serializeNBT());
        tag.putString("RecipeId", recipeId);
        tag.putString("KnownFormulaId", knownFormulaId);
        tag.putString("FormulaSource", formulaSource.id());
        tag.putInt("LidTier", lidTier);
        tag.putInt("FireTier", fireTier);
        tag.put("StoredOutput", storedOutput.save(new CompoundTag()));
        tag.putInt("ProgressTicks", progressTicks);
        tag.putInt("TotalTicks", totalTicks);
        tag.putDouble("SuccessRate", successRate);
        tag.putDouble("ExplosionChance", explosionChance);
        tag.putBoolean("MigratedLegacyComponents", migratedLegacyComponents);
        if (craftingPlayerId != null) {
            tag.putUUID("CraftingPlayerId", craftingPlayerId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            items.deserializeNBT(tag.getCompound("Items"));
        }
        recipeId = tag.getString("RecipeId");
        knownFormulaId = tag.getString("KnownFormulaId");
        formulaSource = tag.contains("FormulaSource") ? AlchemyFormulaSource.byId(tag.getString("FormulaSource")) : AlchemyFormulaSource.PAPER;
        lidTier = Math.max(0, tag.getInt("LidTier"));
        fireTier = Math.max(0, tag.getInt("FireTier"));
        storedOutput = tag.contains("StoredOutput") ? ItemStack.of(tag.getCompound("StoredOutput")) : ItemStack.EMPTY;
        progressTicks = tag.getInt("ProgressTicks");
        totalTicks = tag.getInt("TotalTicks");
        successRate = tag.getDouble("SuccessRate");
        explosionChance = tag.getDouble("ExplosionChance");
        craftingPlayerId = tag.hasUUID("CraftingPlayerId") ? tag.getUUID("CraftingPlayerId") : null;
        migratedLegacyComponents = tag.getBoolean("MigratedLegacyComponents");
        migrateLegacyComponentsIntoSlots();
        syncComponentsFromSlots();
    }
}
