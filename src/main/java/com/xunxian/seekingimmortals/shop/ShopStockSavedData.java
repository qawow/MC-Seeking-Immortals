package com.xunxian.seekingimmortals.shop;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 商店限量库存与刷新计时持久化（全量审查 Batch E）。
 * <p>此前库存仅存于内存 static 缓存，服务器重启/重载后全部补满。
 * 现持久化到 overworld SavedData，重启后保留剩余库存与下次刷新时间。</p>
 */
public class ShopStockSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_shop_stock";
    private final Map<String, StockRecord> stocks = new HashMap<>();

    public static ShopStockSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ShopStockSavedData::load,
                ShopStockSavedData::new,
                DATA_NAME);
    }

    public static ShopStockSavedData load(CompoundTag tag) {
        ShopStockSavedData data = new ShopStockSavedData();
        ListTag list = tag.getList("Stocks", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key = entry.getString("Key");
            if (key.isBlank()) continue;
            int remaining = Math.max(0, entry.getInt("Remaining"));
            long nextRefresh = entry.getLong("NextRefresh");
            data.stocks.put(key, new StockRecord(remaining, nextRefresh));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, StockRecord> e : stocks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Key", e.getKey());
            entry.putInt("Remaining", e.getValue().remaining());
            entry.putLong("NextRefresh", e.getValue().nextRefreshGameTime());
            list.add(entry);
        }
        tag.put("Stocks", list);
        return tag;
    }

    /** 读取库存；不存在返回 null（调用方按默认库存处理）。 */
    public StockRecord get(String key) {
        return stocks.get(key);
    }

    /** 写入库存并标记脏。 */
    public void put(String key, int remaining, long nextRefreshGameTime) {
        stocks.put(key, new StockRecord(Math.max(0, remaining), nextRefreshGameTime));
        setDirty();
    }

    public record StockRecord(int remaining, long nextRefreshGameTime) {}
}
