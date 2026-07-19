package com.xunxian.seekingimmortals.item.material;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.xunxian.seekingimmortals.item.CatalogItemDescriptionService;

import java.util.List;
import java.util.Locale;

public class BaseMaterialItem extends Item {
    private final MaterialCategory category;
    private final MaterialRarity rarity;
    private final String description;

    public BaseMaterialItem(Properties properties, MaterialCategory category, MaterialRarity rarity, String description) {
        super(properties);
        this.category = category;
        this.rarity = rarity;
        this.description = description;
    }

    public MaterialCategory getCategory() { return category; }
    public MaterialRarity getRarity() { return rarity; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Wave489: bulk appraisal/identify carriers appraise the opposite hand.
        String id = stack.getDescriptionId() == null ? "" : stack.getDescriptionId().toLowerCase(Locale.ROOT);
        if (id.contains("appraisal") || id.contains("identify")) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack target = player.getItemInHand(other);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                boolean ok = com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService
                        .appraise(serverPlayer, stack, target);
                return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        // M07: formation flag/disk place-or-activate behavior (ids from formation_items_catalog).
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var formationUse = com.xunxian.seekingimmortals.structure.FormationItemService.tryUse(serverPlayer, stack);
            if (formationUse.isPresent()) {
                return formationUse.get();
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(rarity.getDisplayName()).withStyle(style -> style.withColor(rarity.getColor())));
        tooltip.add(Component.literal(category.getDisplayName()).withStyle(ChatFormatting.GRAY));
        // Wave496: material descriptions prefer lang keys; bulk catalog descriptions are generated bilingually.
        String desc = description == null ? "" : description;
        if (!CatalogItemDescriptionService.appendCatalogDescription(stack, tooltip, desc)) {
            String path = stack.getItem().builtInRegistryHolder().key().location().getPath();
            String descKey = "tooltip.seeking_immortals.material." + path;
            Language language = Language.getInstance();
            if (language != null && language.has(descKey)) {
                tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_GRAY));
            } else if (!desc.isBlank()) {
                tooltip.add(Component.literal(desc).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        String id = stack.getDescriptionId() == null ? "" : stack.getDescriptionId().toLowerCase(Locale.ROOT);
        if (id.contains("appraisal") || id.contains("identify")) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.appraisal_tool")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraised(stack)) {
            var tag = stack.getTag();
            if (tag != null) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.appraised",
                        tag.getInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TIER),
                        com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.type(
                                tag.getString(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TYPE)),
                        tag.getInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_VALUE))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rarity == MaterialRarity.LEGENDARY
                || com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraised(stack);
    }
}
