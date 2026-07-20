package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.Realm;
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
 * Executable bulk talisman carrier. Role/id keywords map to a small set of server-side casts.
 * Technique CAST_* paths still reserve via {@link com.xunxian.seekingimmortals.skill.TalismanConsumePolicy}.
 */
public class CatalogTalismanItem extends BaseMaterialItem {
    private final String catalogId;
    private final String grade;
    private final String role;

    public CatalogTalismanItem(Properties properties,
                               MaterialCategory category,
                               MaterialRarity rarity,
                               String description,
                               String catalogId,
                               String grade,
                               String role) {
        super(properties, category, rarity, description);
        this.catalogId = catalogId == null ? "" : catalogId;
        this.grade = grade == null ? "" : grade;
        this.role = role == null ? "" : role;
    }

    public String catalogId() {
        return catalogId;
    }

    public String grade() {
        return grade;
    }

    public String role() {
        return role;
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

        // 使用 ItemUsageGateService 进行境界门禁检查
        Realm minRealm = parseMinRealmFromGrade(grade);
        if (minRealm != null) {
            ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(minRealm);
            ItemUsageGateService.GateResult gateCheck = ItemUsageGateService.canUse(serverPlayer, requirement);
            if (!gateCheck.allowed()) {
                serverPlayer.displayClientMessage(gateCheck.message(), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        boolean ok = CatalogTalismanService.cast(serverPlayer, catalogId, role);
        if (!ok) {
            return InteractionResultHolder.fail(stack);
        }
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (grade != null && !grade.isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman_grade." + grade.toLowerCase(Locale.ROOT))
                    .withStyle(ChatFormatting.GOLD));
        }

        // 使用 ItemUsageGateService 显示境界要求
        Realm minRealm = parseMinRealmFromGrade(grade);
        if (minRealm != null) {
            ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(minRealm);
            ItemUsageGateService.appendRequirementTooltip(stack, tooltip, requirement);
        }

        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_talisman.use")
                .withStyle(ChatFormatting.GREEN));
        String mode = CatalogTalismanService.modeKey(catalogId, role);
        if (!mode.isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_talisman.mode." + mode)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 根据符箓品级解析最低境界要求。
     */
    private Realm parseMinRealmFromGrade(String grade) {
        if (grade == null || grade.isBlank()) {
            return Realm.QI_REFINING;
        }
        String normalized = grade.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "low", "下品", "一阶" -> Realm.QI_REFINING;
            case "middle", "中品", "二阶" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "high", "上品", "三阶" -> Realm.CORE_FORMATION;
            case "top", "极品", "四阶" -> Realm.NASCENT_SOUL;
            case "supreme", "仙品", "五阶" -> Realm.SOUL_TRANSFORMATION;
            default -> Realm.QI_REFINING;
        };
    }
}
