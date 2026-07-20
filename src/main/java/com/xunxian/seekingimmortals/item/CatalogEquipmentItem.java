package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.FlightVehicleService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * Bulk equipment carriers: vehicles board, puppets summon, furnace shells place.
 */
public class CatalogEquipmentItem extends BaseMaterialItem {
    private final String catalogId;

    public CatalogEquipmentItem(Properties properties,
                                MaterialCategory category,
                                MaterialRarity rarity,
                                String description,
                                String catalogId) {
        super(properties, category, rarity, description);
        this.catalogId = catalogId == null ? "" : catalogId;
    }

    public String catalogId() {
        return catalogId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        CatalogEquipmentService.Result result = CatalogEquipmentService.use(serverPlayer, stack, catalogId, null);
        return result.holder(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel)) {
            return InteractionResult.FAIL;
        }
        BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
        CatalogEquipmentService.Result result =
                CatalogEquipmentService.use(serverPlayer, stack, catalogId, placeAt);
        return result.interaction();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String mode = CatalogEquipmentService.modeKey(catalogId);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_equipment.use")
                .withStyle(ChatFormatting.GREEN));
        if (!mode.isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_equipment.mode." + mode)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
