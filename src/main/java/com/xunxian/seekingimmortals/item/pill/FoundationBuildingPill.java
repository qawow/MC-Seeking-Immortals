package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import net.minecraft.server.level.ServerPlayer;

public class FoundationBuildingPill extends BasePillItem {
    public FoundationBuildingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.FOUNDATION_BUILDING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        return player.getCapability(CultivationProvider.CULTIVATION).map(cultivation -> {
            if (cultivation.getRealm() != Realm.QI_REFINING) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("只有炼气期才能服用筑基丹！"),
                    true
                );
                return false;
            }

            if (cultivation.isBreakthroughAssisted()) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("当前已有突破辅助效果！"),
                    true
                );
                return false;
            }

            cultivation.setBreakthroughPillBonus(getQuality().getBreakthroughBonus());
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "服用" + getQuality().getDisplayName() + "筑基丹，下次突破成功率 +" +
                    getQuality().getBreakthroughBonusPercent() + "%"
                ), true
            );
            return true;
        }).orElse(false);
    }
}
