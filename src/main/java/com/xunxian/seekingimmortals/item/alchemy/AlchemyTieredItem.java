package com.xunxian.seekingimmortals.item.alchemy;

import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class AlchemyTieredItem extends Item {
    public enum ComponentType {
        LID,
        FIRE
    }

    private final ComponentType componentType;
    private final int tier;
    private final Realm minRealm;
    private final boolean requiresEarthFireRoom;

    public AlchemyTieredItem(Properties properties, ComponentType componentType, int tier) {
        this(properties, componentType, tier, Realm.MORTAL, false);
    }

    public AlchemyTieredItem(Properties properties, ComponentType componentType, int tier, Realm minRealm, boolean requiresEarthFireRoom) {
        super(properties);
        this.componentType = componentType;
        this.tier = Math.max(1, tier);
        this.minRealm = minRealm == null ? Realm.MORTAL : minRealm;
        this.requiresEarthFireRoom = requiresEarthFireRoom;
    }

    public ComponentType componentType() {
        return componentType;
    }

    public int tier() {
        return tier;
    }

    public Realm minRealm() {
        return minRealm;
    }

    public boolean requiresEarthFireRoom() {
        return requiresEarthFireRoom;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_component.tier", tier).withStyle(ChatFormatting.GOLD));
        if (componentType == ComponentType.FIRE && minRealm != Realm.MORTAL) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.dan_fire.min_realm", minRealm.getDisplayName()).withStyle(ChatFormatting.RED));
        }
        if (componentType == ComponentType.FIRE && requiresEarthFireRoom) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.dan_fire.earth_fire_room").withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable(componentType == ComponentType.LID
                ? "tooltip.seeking_immortals.alchemy_lid.use"
                : "tooltip.seeking_immortals.dan_fire.use").withStyle(ChatFormatting.GRAY));
    }
}
