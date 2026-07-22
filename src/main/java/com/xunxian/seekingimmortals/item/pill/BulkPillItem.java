package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import com.xunxian.seekingimmortals.cultivation.Realm;
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

/**
 * Bulk-catalog pill carrier with data-driven consume effects (M04).
 * Falls back to material tooltip when no effect entry is registered.
 */
public class BulkPillItem extends BaseMaterialItem {
    private final PillQuality quality;
    private final String catalogId;

    public BulkPillItem(Properties properties, MaterialCategory category, MaterialRarity rarity,
                        String description, String catalogId, PillQuality quality) {
        super(properties, category, rarity, description);
        this.catalogId = catalogId == null ? "" : catalogId;
        this.quality = quality == null ? PillQuality.LOW : quality;
    }

    public PillQuality quality() {
        return quality;
    }

    public String catalogId() {
        return catalogId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean consumed = PillEffectCatalog.tryConsume(serverPlayer, stack, catalogId, quality);
            if (consumed) {
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            // No effect mapping: keep as non-consumable material (inspect only).
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String qualityKey = "tooltip.seeking_immortals.catalog_pill.quality." + quality.designId();
        tooltip.add(PlayerDisplayText.translatedOr(qualityKey,
                        "tooltip.seeking_immortals.catalog_pill.quality.unknown").copy()
                .withStyle(style -> style.withColor(quality.getColor())));
        PillEffectCatalog.findByPillId(catalogId).ifPresent(entry -> {
            if (entry.effect() != null && !entry.effect().isBlank()) {
                String effectKey = "tooltip.seeking_immortals.catalog_pill.effect." + entry.effect();
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.effect",
                        PlayerDisplayText.translatedOr(effectKey,
                                "tooltip.seeking_immortals.catalog_pill.unknown"))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (entry.realmMin() != null && !entry.realmMin().isBlank()) {
                Realm minRealm = Realm.fromDesignId(entry.realmMin());
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.min_realm",
                        minRealm == null
                                ? Component.translatable("text.seeking_immortals.unknown_realm")
                                : minRealm.getDisplayName())
                        .withStyle(ChatFormatting.BLUE));
            }
            if (entry.realmTarget() != null && !entry.realmTarget().isBlank()) {
                Realm targetRealm = Realm.fromDesignId(entry.realmTarget());
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.target_realm",
                        targetRealm == null
                                ? Component.translatable("text.seeking_immortals.unknown_realm")
                                : targetRealm.getDisplayName())
                        .withStyle(ChatFormatting.GOLD));
            }
        });
    }
}
