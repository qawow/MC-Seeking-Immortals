package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CatalogConsumableItem extends BaseMaterialItem implements PortableStorageItem {
    private final String catalogId;
    private final String realmMin;
    private final String effect;
    private final int storageSlots;

    public CatalogConsumableItem(net.minecraft.world.item.Item.Properties properties,
                                 MaterialCategory category,
                                 MaterialRarity rarity,
                                 String description,
                                 BulkItemClassifier.ConsumableDefinition definition) {
        super(properties, category, rarity, description);
        this.catalogId = definition.id();
        this.realmMin = definition.realmMin() == null ? "" : definition.realmMin();
        this.effect = definition.effect() == null ? "" : definition.effect();
        this.storageSlots = Math.max(0, definition.storageSlots());
    }

    public String catalogId() {
        return catalogId;
    }

    public String effect() {
        return effect;
    }

    public String realmMin() {
        return realmMin;
    }

    @Override
    public int portableStorageSlots() {
        return storageSlots;
    }

    @Override
    public String portableStorageRealmMin() {
        return realmMin;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        CatalogConsumableService.UseResult result = CatalogConsumableService.use(
                serverPlayer, stack, hand, catalogId, effect, realmMin);
        if (result != CatalogConsumableService.UseResult.SUCCESS) {
            return InteractionResultHolder.fail(stack);
        }
        if (CatalogConsumableService.shouldConsumeOnSuccess(effect, storageSlots)
                && !serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (effect != null && !effect.isBlank()) {
            String effectKey = "tooltip.seeking_immortals.catalog_consumable.effect." + effect;
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_consumable.effect",
                    PlayerDisplayText.translatedOr(effectKey,
                            "tooltip.seeking_immortals.catalog_consumable.effect.unlisted"))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (realmMin != null && !realmMin.isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_consumable.min_realm",
                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(realmMin))
                    .withStyle(ChatFormatting.BLUE));
        }
        if (storageSlots > 0) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.storage",
                    com.xunxian.seekingimmortals.artifact.ArtifactStorageService.countStored(stack), storageSlots)
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
