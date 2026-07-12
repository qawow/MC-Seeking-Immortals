package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArtifactStorageService {
    public static final String STORAGE_ITEMS_TAG = "SeekingImmortalsArtifactStoredItems";

    private ArtifactStorageService() {}

    public static boolean supports(String artifactId) {
        return ArtifactDataService.builtin()
                .findArtifact(artifactId)
                .map(ArtifactStorageService::storageSlots)
                .orElse(0) > 0;
    }

    public static int storageSlots(String artifactId) {
        return ArtifactDataService.builtin()
                .findArtifact(artifactId)
                .map(ArtifactStorageService::storageSlots)
                .orElse(0);
    }

    public static int storageSlots(ArtifactDataService.ArtifactDefinition artifact) {
        if (!"storage".equalsIgnoreCase(artifact.type())) {
            return 0;
        }
        String effect = artifact.effect().toLowerCase(Locale.ROOT);
        String prefix = "extra_slots_";
        if (!effect.startsWith(prefix)) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(effect.substring(prefix.length())));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public static int countStored(ItemStack braceletStack) {
        return storedItems(braceletStack).size();
    }

    public static void appendStorageTooltip(ItemStack stack, ArtifactDataService.ArtifactDefinition artifact,
                                            List<Component> tooltip) {
        int slots = storageSlots(artifact);
        if (slots <= 0) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.storage",
                countStored(stack), slots).withStyle(ChatFormatting.BLUE));
    }

    public static boolean use(ServerPlayer player, ItemStack braceletStack, InteractionHand hand,
                              ArtifactDataService.ArtifactDefinition artifact, PlayerCultivation cultivation) {
        int slots = storageSlots(artifact);
        if (slots <= 0) {
            return false;
        }
        Realm minRealm = realmFromDesignId(artifact.realmMin());
        if (cultivation.getRealm().ordinal() < minRealm.ordinal()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.realm_too_low", minRealm.getDisplayName()), true);
            return false;
        }

        // Wave54: open real MenuType storage GUI.
        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.seeking_immortals.storage_bracelet.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                return new com.xunxian.seekingimmortals.menu.StorageBraceletMenu(id, inv, hand, braceletStack, slots);
            }
        }, buf -> {
            buf.writeEnum(hand);
            buf.writeVarInt(slots);
        });
        return true;
    }

    private static boolean depositFromOtherHand(ServerPlayer player, ItemStack braceletStack, InteractionHand hand,
                                                int slots) {
        ItemStack target = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_no_item"), true);
            return false;
        }
        if (!canStore(target)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_no_nested"), true);
            return false;
        }

        List<ItemStack> items = storedItems(braceletStack);
        if (items.size() >= slots) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_full", slots), true);
            return false;
        }

        ItemStack stored = target.copy();
        items.add(stored);
        writeItems(braceletStack, items);
        target.setCount(0);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.storage_deposited", stored.getHoverName(),
                items.size(), slots), true);
        return true;
    }

    private static boolean withdrawLatest(ServerPlayer player, ItemStack braceletStack, int slots) {
        List<ItemStack> items = storedItems(braceletStack);
        if (items.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_empty"), true);
            return false;
        }

        ItemStack restored = items.remove(items.size() - 1);
        writeItems(braceletStack, items);
        ItemStack toGive = restored.copy();
        player.getInventory().add(toGive);
        if (!toGive.isEmpty()) {
            player.drop(toGive, false);
        }
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.storage_retrieved", restored.getHoverName(),
                items.size(), slots), true);
        return true;
    }

    
    public static boolean supportsStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArtifactCatalogItem item)) {
            return false;
        }
        return supports(item.artifactId());
    }

    public static ItemStackHandler createHandler(ItemStack bracelet, int slots) {
        ItemStackHandler handler = new ItemStackHandler(Math.max(1, slots));
        java.util.List<ItemStack> stored = storedItems(bracelet);
        for (int i = 0; i < Math.min(slots, stored.size()); i++) {
            handler.setStackInSlot(i, stored.get(i).copy());
        }
        return handler;
    }

    public static void writeHandler(ItemStack bracelet, ItemStackHandler handler) {
        if (bracelet == null || handler == null) {
            return;
        }
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        writeItems(bracelet, items);
    }

    private static boolean canStore(ItemStack stack) {
        if (stack.getItem() instanceof ArtifactCatalogItem artifactItem) {
            return !supports(artifactItem.artifactId());
        }
        return true;
    }

    private static List<ItemStack> storedItems(ItemStack braceletStack) {
        CompoundTag tag = braceletStack.getTag();
        if (tag == null || !tag.contains(STORAGE_ITEMS_TAG, Tag.TAG_LIST)) {
            return new ArrayList<>();
        }
        ListTag list = tag.getList(STORAGE_ITEMS_TAG, Tag.TAG_COMPOUND);
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            ItemStack stack = ItemStack.of(list.getCompound(index));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    private static void writeItems(ItemStack braceletStack, List<ItemStack> items) {
        if (items.isEmpty()) {
            CompoundTag tag = braceletStack.getTag();
            if (tag != null) {
                tag.remove(STORAGE_ITEMS_TAG);
            }
            return;
        }
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                list.add(stack.save(new CompoundTag()));
            }
        }
        braceletStack.getOrCreateTag().put(STORAGE_ITEMS_TAG, list);
    }

    private static Realm realmFromDesignId(String id) {
        if (id == null || id.isBlank()) {
            return Realm.MORTAL;
        }
        return switch (id.toUpperCase(Locale.ROOT)) {
            case "QI_REFINING" -> Realm.QI_REFINING;
            case "FOUNDATION", "FOUNDATION_ESTABLISHMENT" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "CORE_FORMATION" -> Realm.CORE_FORMATION;
            case "NASCENT_SOUL" -> Realm.NASCENT_SOUL;
            case "SOUL_TRANSFORMATION" -> Realm.SOUL_TRANSFORMATION;
            case "VOID_REFINEMENT" -> Realm.VOID_REFINEMENT;
            case "UNITY" -> Realm.UNITY;
            case "GREAT_VEHICLE", "MAHAYANA" -> Realm.MAHAYANA;
            case "TRIBULATION" -> Realm.TRIBULATION;
            case "TRUE_IMMORTAL" -> Realm.TRUE_IMMORTAL;
            default -> Realm.MORTAL;
        };
    }
}
