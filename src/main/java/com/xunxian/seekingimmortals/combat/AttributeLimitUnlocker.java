package com.xunxian.seekingimmortals.combat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class AttributeLimitUnlocker {
    private static final double UNCAPPED_ATTRIBUTE_MAX = 1.0E12D;

    private AttributeLimitUnlocker() {}

    public static void unlockCombatAttributeCaps() {
        raiseMax(Attributes.MAX_HEALTH);
        raiseMax(Attributes.ATTACK_DAMAGE);
        raiseMax(Attributes.ARMOR);
        raiseMax(Attributes.ARMOR_TOUGHNESS);
        raiseMax(Attributes.KNOCKBACK_RESISTANCE);
        raiseMax(Attributes.MOVEMENT_SPEED);
    }

    private static void raiseMax(Attribute attribute) {
        if (attribute instanceof RangedAttribute rangedAttribute) {
            rangedAttribute.maxValue = UNCAPPED_ATTRIBUTE_MAX;
        }
    }
}
