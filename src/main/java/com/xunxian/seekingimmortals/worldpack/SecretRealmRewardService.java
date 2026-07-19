package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class SecretRealmRewardService {
    private static final String BINDING_TAG = "seeking_immortals_secret_realm_reward";
    private static final String REWARDS_TAG = "Rewards";
    private static final String SEALED_TAG = "Sealed";
    private static final String CLAIMED_TAG = "Claimed";

    private SecretRealmRewardService() {}

    public enum ClaimResult {
        NOT_BOUND,
        DENIED,
        SEALED,
        EMPTY,
        CLAIMED
    }

    public static void initializeChest(ChestBlockEntity chest, ServerPlayer owner,
                                       SecretRealmProgressSavedData.Session session,
                                       String realmId, String encounterId,
                                       boolean sealed, List<ItemStack> rewards) {
        if (chest == null || owner == null || session == null) {
            return;
        }
        chest.clearContent();
        CompoundTag binding = new CompoundTag();
        SecretRealmSessionService.bindEncounter(binding, owner, session, realmId, encounterId);
        binding.putBoolean(SEALED_TAG, sealed);
        binding.putBoolean(CLAIMED_TAG, false);
        writeRewards(binding, rewards);
        chest.getPersistentData().put(BINDING_TAG, binding);
        chest.setChanged();
    }

    public static boolean isBoundReward(BlockEntity blockEntity) {
        return blockEntity != null
                && blockEntity.getPersistentData().contains(BINDING_TAG, Tag.TAG_COMPOUND);
    }

    public static boolean matches(ChestBlockEntity chest, ServerPlayer player,
                                  String realmId, String encounterId) {
        if (!isBoundReward(chest)) {
            return false;
        }
        CompoundTag binding = binding(chest);
        return SecretRealmSessionService.matchesEncounter(player, binding)
                && SecretRealmSessionService.boundRealmId(binding).equals(normalize(realmId))
                && binding.getString(SecretRealmSessionService.ENCOUNTER_ID).equals(normalize(encounterId));
    }

    public static boolean unlock(ChestBlockEntity chest, ServerPlayer player,
                                 String realmId, String encounterId, List<ItemStack> rewards) {
        if (!matches(chest, player, realmId, encounterId)) {
            return false;
        }
        CompoundTag binding = binding(chest).copy();
        if (binding.getBoolean(CLAIMED_TAG)) {
            return false;
        }
        binding.putBoolean(SEALED_TAG, false);
        writeRewards(binding, rewards);
        chest.getPersistentData().put(BINDING_TAG, binding);
        chest.setChanged();
        return true;
    }

    public static ClaimResult claim(ServerPlayer player, BlockEntity blockEntity) {
        if (!isBoundReward(blockEntity) || !(blockEntity instanceof ChestBlockEntity chest)) {
            return ClaimResult.NOT_BOUND;
        }
        CompoundTag binding = binding(chest).copy();
        if (!SecretRealmSessionService.matchesEncounter(player, binding)) {
            return ClaimResult.DENIED;
        }
        if (binding.getBoolean(SEALED_TAG)) {
            return ClaimResult.SEALED;
        }
        if (binding.getBoolean(CLAIMED_TAG)) {
            return ClaimResult.EMPTY;
        }
        List<ItemStack> rewards = readRewards(binding);
        if (rewards.isEmpty()) {
            return ClaimResult.EMPTY;
        }
        binding.putBoolean(CLAIMED_TAG, true);
        binding.remove(REWARDS_TAG);
        chest.getPersistentData().put(BINDING_TAG, binding);
        chest.setChanged();
        rewards.forEach(stack -> InventoryDeliveryService.giveOrEnqueue(player, stack, "secret_realm_reward"));
        if (chest.getLevel() != null) {
            chest.getLevel().removeBlock(chest.getBlockPos(), false);
        }
        return ClaimResult.CLAIMED;
    }

    private static CompoundTag binding(ChestBlockEntity chest) {
        return chest.getPersistentData().getCompound(BINDING_TAG);
    }

    private static void writeRewards(CompoundTag binding, List<ItemStack> rewards) {
        ListTag list = new ListTag();
        if (rewards != null) {
            for (ItemStack stack : rewards) {
                if (stack != null && !stack.isEmpty()) {
                    list.add(stack.save(new CompoundTag()));
                }
            }
        }
        binding.put(REWARDS_TAG, list);
    }

    private static List<ItemStack> readRewards(CompoundTag binding) {
        List<ItemStack> rewards = new ArrayList<>();
        ListTag list = binding.getList(REWARDS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                rewards.add(stack);
            }
        }
        return List.copyOf(rewards);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
