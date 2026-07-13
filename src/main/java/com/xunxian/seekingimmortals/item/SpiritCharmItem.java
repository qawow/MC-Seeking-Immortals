package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Wave481: full Curios charm integration via ICurioItem.
 * Equipped in curios:charm; right-click equip; periodic spiritual recovery.
 * Recovery was previously only a ModEvents inventory scan — now authoritative on the item.
 */
public class SpiritCharmItem extends Item implements ICurioItem {
    private static final int TICK_INTERVAL = 20;

    public SpiritCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        // Only tick once per second to avoid flooding sync/recovery.
        if (player.tickCount % TICK_INTERVAL != 0) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            int gain = Math.max(1, (int) Math.round(cultivation.getCultivationSpeedMultiplier()));
            int before = cultivation.getSpiritualPower();
            int max = cultivation.getMaxSpiritualPower();
            if (before >= max) {
                return;
            }
            cultivation.addSpiritualPower(gain);
        });
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 1.0F);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player)) {
            return false;
        }
        String id = slotContext.identifier();
        return id == null || id.isBlank() || "charm".equals(id) || "artifact".equals(id);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_charm.curios"));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_charm.recovery"));
    }
}
