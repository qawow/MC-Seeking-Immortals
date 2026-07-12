package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import org.jetbrains.annotations.Nullable;

final class SpiritStonePassiveBonus {
    private SpiritStonePassiveBonus() {}

    static boolean matchesAttribute(@Nullable SpiritualRootAttribute stoneAttribute, @Nullable SpiritualRootAttribute requiredAttribute) {
        if (!isFiveElement(requiredAttribute)) return true;
        return stoneAttribute != null && stoneAttribute == requiredAttribute;
    }

    private static boolean isFiveElement(@Nullable SpiritualRootAttribute attribute) {
        return attribute == SpiritualRootAttribute.METAL
                || attribute == SpiritualRootAttribute.WOOD
                || attribute == SpiritualRootAttribute.WATER
                || attribute == SpiritualRootAttribute.FIRE
                || attribute == SpiritualRootAttribute.EARTH;
    }
}
