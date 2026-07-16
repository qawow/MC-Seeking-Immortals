package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Bulk catalog carrier with optional talisman grade metadata (M03).
 * Consume policy for talismans remains owned by M14 {@code TalismanConsumePolicy}.
 */
public class CatalogCarrierItem extends BaseMaterialItem {
    private final String catalogId;
    private final String grade;

    public CatalogCarrierItem(Properties properties,
                              MaterialCategory category,
                              MaterialRarity rarity,
                              String description,
                              String catalogId,
                              String grade) {
        super(properties, category, rarity, description);
        this.catalogId = catalogId == null ? "" : catalogId;
        this.grade = grade == null ? "" : grade;
    }

    public String catalogId() {
        return catalogId;
    }

    public String grade() {
        return grade;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String effectiveGrade = grade;
        if ((effectiveGrade == null || effectiveGrade.isBlank()) && catalogId != null && !catalogId.isBlank()) {
            effectiveGrade = ItemCatalogService.findMeta(catalogId).map(ItemCatalogService.CarrierMeta::grade).orElse("");
        }
        if (effectiveGrade != null && !effectiveGrade.isBlank()) {
            String key = "tooltip.seeking_immortals.talisman_grade." + effectiveGrade.toLowerCase(Locale.ROOT);
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        }
    }
}
