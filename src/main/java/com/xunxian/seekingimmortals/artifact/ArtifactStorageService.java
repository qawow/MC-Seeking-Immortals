package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.item.PortableStorageItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
        if (!ArtifactOwnershipService.canActivate(player, braceletStack, artifact.id())) {
            return false;
        }
        Realm minRealm = realmFromDesignId(artifact.realmMin());
        if (minRealm == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.unknown_realm", artifact.realmMin()), true);
            return false;
        }
        if (cultivation.getRealm().ordinal() < minRealm.ordinal()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.realm_too_low", minRealm.getDisplayName()), true);
            return false;
        }
        if (!player.getAbilities().instabuild
                && ArtifactActivationService.getIntegrity(braceletStack, artifact) <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.integrity_broken", braceletStack.getHoverName()), true);
            return false;
        }
        if (!isStorageCountValid(braceletStack, slots)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_overflow", countStored(braceletStack), slots), true);
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

    public static boolean usePortableStorage(ServerPlayer player, ItemStack storageStack, InteractionHand hand) {
        if (player == null || storageStack == null || storageStack.isEmpty()
                || !(storageStack.getItem() instanceof PortableStorageItem portable)) {
            return false;
        }
        int slots = Math.max(1, Math.min(27, portable.portableStorageSlots()));
        Realm required = portableStorageRealm(portable.portableStorageRealmMin());
        if (required == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.unknown_realm",
                    portable.portableStorageRealmMin()), true);
            return false;
        }
        if (CultivationHelper.get(player)
                .map(cultivation -> cultivation.getRealm().ordinal() < required.ordinal())
                .orElse(!Realm.MORTAL.equals(required))) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.realm_too_low", required.getDisplayName()), true);
            return false;
        }
        if (!isStorageCountValid(storageStack, slots)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.storage_overflow", countStored(storageStack), slots), true);
            return false;
        }
        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.seeking_immortals.storage_bracelet.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, Player ignored) {
                return new com.xunxian.seekingimmortals.menu.StorageBraceletMenu(
                        id, inv, hand, storageStack, portable.portableStorageSlots());
            }
        }, buf -> {
            buf.writeEnum(hand);
            buf.writeVarInt(portable.portableStorageSlots());
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
        InventoryDeliveryService.giveOrDrop(player, restored);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.storage_retrieved", restored.getHoverName(),
                items.size(), slots), true);
        return true;
    }

    

    /**
     * Continuous menu authorization: same instance, support, owner/claim, realm, positive integrity.
     * Used by StorageBraceletMenu.stillValid and mutation paths.
     */
    public static boolean isContinuouslyAuthorized(ServerPlayer player, ItemStack bracelet) {
        if (player == null || bracelet == null || bracelet.isEmpty() || !supportsStack(bracelet)) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (bracelet.getItem() instanceof ArtifactCatalogItem artifactItem) {
            String artifactId = artifactItem.artifactId();
            if (!ArtifactOwnershipService.canActivate(player, bracelet, artifactId)) {
                return false;
            }
            return ArtifactDataService.builtin().findArtifact(artifactId).map(def -> {
                Realm minRealm = realmFromDesignId(def.realmMin());
                if (minRealm != null) {
                    PlayerCultivation cultivation = CultivationHelper.get(player).orElse(null);
                    if (cultivation == null || cultivation.getRealm().ordinal() < minRealm.ordinal()) {
                        return false;
                    }
                }
                return ArtifactActivationService.getIntegrity(bracelet, def) > 0;
            }).orElse(false);
        }
        // Portable storage pouches: no owner/integrity gate beyond support.
        return true;
    }

    public static boolean supportsStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof PortableStorageItem portable) {
            return portable.portableStorageSlots() > 0;
        }
        return stack.getItem() instanceof ArtifactCatalogItem item && supports(item.artifactId());
    }

    public static int storageSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof PortableStorageItem portable) {
            return Math.max(0, Math.min(27, portable.portableStorageSlots()));
        }
        if (stack.getItem() instanceof ArtifactCatalogItem artifact) {
            return storageSlots(artifact.artifactId());
        }
        return 0;
    }

    static boolean isStorageCountValid(int storedCount, int slots) {
        return storedCount >= 0 && slots > 0 && storedCount <= slots;
    }

    private static boolean isStorageCountValid(ItemStack stack, int slots) {
        return isStorageCountValid(countStored(stack), slots);
    }

    public static ItemStackHandler createHandler(ItemStack bracelet, int slots) {
        return new BraceletItemHandler(bracelet, slots);
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
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        boolean storageArtifact = supportsStack(stack);
        boolean shulker = stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
        boolean bundle = stack.is(Items.BUNDLE);
        boolean itemHandler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        return !isForbiddenContainer(storageArtifact, shulker, bundle, itemHandler);
    }

    static boolean isForbiddenContainer(boolean storageArtifact, boolean shulker,
                                        boolean bundle, boolean itemHandler) {
        return storageArtifact || shulker || bundle || itemHandler;
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

    private static final class BraceletItemHandler extends ItemStackHandler {
        private final ItemStack bracelet;
        private boolean loading = true;

        private BraceletItemHandler(ItemStack bracelet, int slots) {
            super(Math.max(1, slots));
            this.bracelet = bracelet;
            List<ItemStack> stored = storedItems(bracelet);
            for (int i = 0; i < Math.min(getSlots(), stored.size()); i++) {
                super.setStackInSlot(i, stored.get(i).copy());
            }
            loading = false;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canStore(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!loading) {
                writeHandler(bracelet, this);
            }
        }
    }

    private static Realm realmFromDesignId(String id) {
        if (id == null || id.isBlank()) {
            return Realm.MORTAL;
        }
        return Realm.fromDesignId(id);
    }

    private static Realm portableStorageRealm(String id) {
        if (id == null || id.isBlank()) {
            return Realm.MORTAL;
        }
        return Realm.fromDesignId(id);
    }
}
