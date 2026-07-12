package com.xunxian.seekingimmortals.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class MarketTraderEntity extends Villager {
    public MarketTraderEntity(EntityType<? extends MarketTraderEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
}
