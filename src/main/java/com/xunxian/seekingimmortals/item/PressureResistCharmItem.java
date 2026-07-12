package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.pill.CatalogPillItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PressureResistCharmItem extends Item {
    private static final Realm MIN_REALM = Realm.VOID_REFINEMENT;
    private static final int DURATION_TICKS = 12000;

    public PressureResistCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean activated = CultivationHelper.get(serverPlayer)
                    .map(cultivation -> activate(serverPlayer, stack, cultivation.getRealm()))
                    .orElse(false);
            return activated ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    private boolean activate(ServerPlayer player, ItemStack stack, Realm realm) {
        if (realm.ordinal() < MIN_REALM.ordinal()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.pressure_resist_charm.realm_too_low",
                    MIN_REALM.getDisplayName()), true);
            return false;
        }

        CompoundTag data = player.getPersistentData();
        int currentTicks = data.getInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY);
        if (currentTicks >= DURATION_TICKS) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.pressure_resist_charm.already_active"), true);
            return false;
        }

        data.putInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY, DURATION_TICKS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.pressure_resist_charm.success"), true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.pressure_resist_charm")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_pill.min_realm",
                MIN_REALM.getDisplayName()).withStyle(ChatFormatting.BLUE));
    }
}
