package com.xunxian.seekingimmortals.block.entity;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeService;
import com.xunxian.seekingimmortals.block.AlchemyFurnaceBlock;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyTieredItem;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.structure.AlchemyFurnaceShellStructure;
import com.xunxian.seekingimmortals.structure.SectEarthFireRoomMultiblock;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class AlchemyFurnaceBlockEntity extends BlockEntity {
    private static final int EARTH_FIRE_TIER = 4;

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
    private final ItemStackHandler items = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ContainerData data = new SimpleContainerData(2) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progressTicks;
                case 1 -> totalTicks;
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
            return 2;
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
        if (furnace.progressTicks <= 0) return;
        // H12: 守卫上提 —— 非服务端维度不修改进度状态，防止静默丢失配方
        if (!(level instanceof ServerLevel serverLevel)) return;
        furnace.progressTicks--;
        furnace.setChanged();
        if (furnace.progressTicks <= 0) {
            furnace.finishCraft(serverLevel);
        }
    }

    public void interact(ServerPlayer player, ItemStack held) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!storedOutput.isEmpty()) {
            giveOutput(player);
            return;
        }
        if (progressTicks > 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.progress",
                    getRecipeName(), totalTicks - progressTicks, totalTicks), true);
            return;
        }
        if (tryInstallComponent(serverLevel, player, held)) {
            return;
        }
        findRecipeForHeldItem(held).ifPresentOrElse(recipe -> startRecipe(serverLevel, player, recipe),
                () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.idle",
                        getFurnaceTier(), lidTier, fireTier, describeFormula()), false));
    }

    public void dropStoredContents() {
        if (level == null || level.isClientSide) return;
        dropItemStack(storedOutput);
        dropItemStack(installedTieredComponentStack(AlchemyTieredItem.ComponentType.LID, lidTier));
        dropItemStack(installedTieredComponentStack(AlchemyTieredItem.ComponentType.FIRE, fireTier));
        dropItemStack(installedFormulaStack());

        storedOutput = ItemStack.EMPTY;
        lidTier = 0;
        fireTier = 0;
        knownFormulaId = "";
        formulaSource = AlchemyFormulaSource.PAPER;
        reset();
    }

    private void startRecipe(ServerLevel serverLevel, ServerPlayer player, AlchemyRecipe recipe) {
        int furnaceTier = getFurnaceTier();
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
        // Wave38: high-tier furnaces require MultiblockPattern shell (G1 cardinal ring / G3 full ring).
        if (furnaceTier >= 3 && !AlchemyFurnaceShellStructure.isComplete(serverLevel, worldPosition, furnaceTier)) {
            int missing = AlchemyFurnaceShellStructure.missingOffsets(serverLevel, worldPosition, furnaceTier).size();
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_furnace.shell_incomplete", furnaceTier, missing), true);
            return;
        }
        // Wave53: alchemy skill level gate unlocks higher furnace-tier recipes.
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
                recipe.displayName(), recipe.manaCost(), (int)Math.round(successRate * 100.0D)), false);
        serverLevel.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4F, 0.8F);
        setChanged();
    }

    private boolean tryInstallComponent(ServerLevel serverLevel, ServerPlayer player, ItemStack held) {
        if (held.isEmpty()) return false;
        if (held.getItem() instanceof AlchemyTieredItem tieredItem) {
            if (tieredItem.componentType() == AlchemyTieredItem.ComponentType.LID) {
                if (lidTier == tieredItem.tier()) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.lid_already_installed", lidTier), true);
                    return true;
                }
                returnInstalledItem(player, installedTieredComponentStack(AlchemyTieredItem.ComponentType.LID, lidTier));
                lidTier = tieredItem.tier();
                shrinkInstalledItem(player, held);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.lid_installed", lidTier), false);
            } else {
                if (fireTier == tieredItem.tier()) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.fire_already_installed", fireTier), true);
                    return true;
                }
                if (!canInstallFire(serverLevel, player, held, tieredItem)) return true;
                returnInstalledItem(player, installedTieredComponentStack(AlchemyTieredItem.ComponentType.FIRE, fireTier));
                fireTier = tieredItem.tier();
                shrinkInstalledItem(player, held);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.fire_installed", fireTier), false);
            }
            setChanged();
            return true;
        }
        if (held.getItem() instanceof AlchemyFormulaItem formulaItem) {
            if (formulaItem.recipeId().equals(knownFormulaId) && formulaItem.source() == formulaSource) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.formula_already_installed",
                        Component.translatable("alchemy_recipe.seeking_immortals." + knownFormulaId),
                        Component.translatable("alchemy_formula_source.seeking_immortals." + formulaSource.id())), true);
                return true;
            }
            returnInstalledItem(player, installedFormulaStack());
            knownFormulaId = formulaItem.recipeId();
            formulaSource = formulaItem.source();
            shrinkInstalledItem(player, held);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.formula_installed",
                    Component.translatable("alchemy_recipe.seeking_immortals." + knownFormulaId),
                    Component.translatable("alchemy_formula_source.seeking_immortals." + formulaSource.id())), false);
            setChanged();
            return true;
        }
        return false;
    }

    private void finishCraft(ServerLevel serverLevel) {
        AlchemyRecipe recipe = AlchemyRecipe.findById(recipeId).orElse(null);
        if (recipe == null) {
            reset();
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
            storedOutput = new ItemStack(rollOutputQuality(serverLevel, recipe, roll), recipe.outputCount());
            grantAlchemyExperience(serverLevel);
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.2F);
        } else {
            storedOutput = new ItemStack(ModItems.WASTE_PILL.get());
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
        lidTier = 0;
        DamageSource source = serverLevel.damageSources().explosion(null, null);
        serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(2.5D))
                .forEach(target -> target.hurt(source, 4.0F));
        serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.6F, 1.3F);
        setChanged();
    }

    private void giveOutput(ServerPlayer player) {
        ItemStack result = storedOutput.copy();
        storedOutput = ItemStack.EMPTY;
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
        if (knownFormulaId.isBlank()) return "-";
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
        if (stack.isEmpty()) return;
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

    private int getFurnaceTier() {
        if (getBlockState().getBlock() instanceof AlchemyFurnaceBlock furnaceBlock) {
            return furnaceBlock.tier();
        }
        return 1;
    }

    /** 成丹时按成功余量与炼丹等级决定品质（M15）。realRoll 为 finishCraft 实际成功掷骰值（∈[explosionChance, successThreshold)）。 */
    private net.minecraft.world.item.Item rollOutputQuality(ServerLevel serverLevel, AlchemyRecipe recipe, double realRoll) {
        if (!recipe.isQualityVariable()) return recipe.output();
        // 余量：realRoll 越接近成功阈值(successThreshold=explosionChance+successRate)越接近失败边界，按设计该情况品质分越高。
        double successThreshold = explosionChance + successRate;
        double margin = successThreshold <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, (successThreshold - realRoll) / successRate));
        // normalized ∈ [0,1)：realRoll 越接近失败边界 → margin 越小 → normalized 越大（越接近失败边界分越高）。
        double normalized = 1.0D - margin;
        ServerPlayer crafter = craftingPlayerId == null ? null : serverLevel.getServer().getPlayerList().getPlayer(craftingPlayerId);
        double alchemyBonus = crafter == null ? 0.0D : AlchemyRecipeService.getAlchemySkillBonus(crafter);
        double qualityScore = normalized * 0.8D + alchemyBonus;
        com.xunxian.seekingimmortals.item.pill.PillQuality quality;
        if (qualityScore >= 0.95D) quality = com.xunxian.seekingimmortals.item.pill.PillQuality.SUPREME;
        else if (qualityScore >= 0.75D) quality = com.xunxian.seekingimmortals.item.pill.PillQuality.HIGH;
        else if (qualityScore >= 0.50D) quality = com.xunxian.seekingimmortals.item.pill.PillQuality.MEDIUM;
        else quality = com.xunxian.seekingimmortals.item.pill.PillQuality.LOW;
        return recipe.outputForQuality(quality);
    }

    /** 成丹时为开炉玩家结算炼丹术经验（H13：提供解锁/升级路径）。 */
    private void grantAlchemyExperience(ServerLevel serverLevel) {
        if (craftingPlayerId == null) return;
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(craftingPlayerId);
        if (player == null) return;
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!cultivation.hasAlchemy()) {
                cultivation.unlockSkill(SkillType.ALCHEMY);
            }
            cultivation.addSkillExperience(SkillType.ALCHEMY, 25);
            com.xunxian.seekingimmortals.network.SyncCultivationDataPacket.send(player, cultivation);
        });
    }

    private void reset() {
        recipeId = "";
        storedOutput = ItemStack.EMPTY;
        progressTicks = 0;
        totalTicks = 0;
        successRate = 0.0D;
        explosionChance = 0.0D;
        craftingPlayerId = null;
        setChanged();
    }

    private void discardStoredContents() {
        lidTier = 0;
        fireTier = 0;
        knownFormulaId = "";
        formulaSource = AlchemyFormulaSource.PAPER;
        reset();
    }

    private void dropItemStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stack.copy());
    }

    private ItemStack installedTieredComponentStack(AlchemyTieredItem.ComponentType componentType, int tier) {
        if (tier <= 0) return ItemStack.EMPTY;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof AlchemyTieredItem tieredItem
                    && tieredItem.componentType() == componentType
                    && tieredItem.tier() == tier) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack installedFormulaStack() {
        if (knownFormulaId.isBlank()) return ItemStack.EMPTY;
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
        if (craftingPlayerId != null) tag.putUUID("CraftingPlayerId", craftingPlayerId);
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
    }
}
