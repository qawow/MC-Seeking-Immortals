package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
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
import java.util.Locale;

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
        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.quality." + quality.designId())
                .withStyle(style -> style.withColor(quality.getColor())));
        PillEffectCatalog.findByPillId(catalogId).ifPresent(entry -> {
            if (entry.effect() != null && !entry.effect().isBlank()) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.effect",
                        entry.effect()).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (entry.realmMin() != null && !entry.realmMin().isBlank()) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.min_realm",
                        entry.realmMin().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BLUE));
            }
        });
    }
}
