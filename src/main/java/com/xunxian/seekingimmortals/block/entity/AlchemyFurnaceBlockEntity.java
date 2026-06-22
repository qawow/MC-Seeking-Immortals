package com.xunxian.seekingimmortals.block.entity;

import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeService;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlchemyFurnaceBlockEntity extends BlockEntity {
    private String recipeId = "";
    private ItemStack storedOutput = ItemStack.EMPTY;
    private int progressTicks;
    private int totalTicks;
    private double successRate;
    private double explosionChance;

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
        AlchemyRecipe.findByHeldIngredient(held).ifPresentOrElse(recipe -> startRecipe(serverLevel, player, recipe),
                () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.idle"), false));
    }

    public void dropStoredOutput() {
        if (level == null || storedOutput.isEmpty()) return;
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 0.5D, worldPosition.getZ(), storedOutput.copy());
        storedOutput = ItemStack.EMPTY;
    }

    private void startRecipe(ServerLevel serverLevel, ServerPlayer player, AlchemyRecipe recipe) {
        if (!AlchemyRecipeService.consumeInputs(player, recipe)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.missing",
                    recipe.displayName(), AlchemyRecipeService.missingSummary(player, recipe)), true);
            return;
        }
        recipeId = recipe.id();
        totalTicks = recipe.cookTicks();
        progressTicks = totalTicks;
        successRate = AlchemyRecipeService.successRate(serverLevel, player, recipe);
        explosionChance = AlchemyRecipeService.explosionChance(player, recipe);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.alchemy_furnace.started",
                recipe.displayName(), recipe.manaCost(), (int)Math.round(successRate * 100.0D)), false);
        serverLevel.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4F, 0.8F);
        setChanged();
    }

    private void finishCraft(ServerLevel serverLevel) {
        AlchemyRecipe recipe = AlchemyRecipe.findById(recipeId).orElse(null);
        if (recipe == null) {
            reset();
            return;
        }
        double roll = serverLevel.random.nextDouble();
        if (roll < explosionChance) {
            reset();
            serverLevel.destroyBlock(worldPosition, false);
            serverLevel.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    1.0F, Level.ExplosionInteraction.BLOCK);
            return;
        }
        if (roll < explosionChance + successRate) {
            storedOutput = new ItemStack(recipe.output(), recipe.outputCount());
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

    private void reset() {
        recipeId = "";
        storedOutput = ItemStack.EMPTY;
        progressTicks = 0;
        totalTicks = 0;
        successRate = 0.0D;
        explosionChance = 0.0D;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("RecipeId", recipeId);
        tag.put("StoredOutput", storedOutput.save(new CompoundTag()));
        tag.putInt("ProgressTicks", progressTicks);
        tag.putInt("TotalTicks", totalTicks);
        tag.putDouble("SuccessRate", successRate);
        tag.putDouble("ExplosionChance", explosionChance);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        recipeId = tag.getString("RecipeId");
        storedOutput = tag.contains("StoredOutput") ? ItemStack.of(tag.getCompound("StoredOutput")) : ItemStack.EMPTY;
        progressTicks = tag.getInt("ProgressTicks");
        totalTicks = tag.getInt("TotalTicks");
        successRate = tag.getDouble("SuccessRate");
        explosionChance = tag.getDouble("ExplosionChance");
    }
}
