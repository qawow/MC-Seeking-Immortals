package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Persistent beast contract slots (Wave49 Phase21 depth).
 * Uses player persistent data (no protocol churn). Summon reuses SummonHonestMvpService.
 */
public final class BeastContractService {
    private static final String ROOT = "seeking_immortals_beast_contracts";
    private static final int MAX_SLOTS = 3;

    private BeastContractService() {}

    public record Contract(String id, int affinity, int growth) {}

    public static List<Contract> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<Contract> list = new ArrayList<>();
        for (String key : root.getAllKeys()) {
            CompoundTag entry = root.getCompound(key);
            list.add(new Contract(key, entry.getInt("Affinity"), entry.getInt("Growth")));
        }
        return list;
    }

    public static boolean contract(ServerPlayer player, String beastId) {
        String id = normalize(beastId);
        if (id.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.unknown"), false);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.already", id), false);
            return false;
        }
        if (root.getAllKeys().size() >= MAX_SLOTS) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.full", MAX_SLOTS), false);
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putInt("Affinity", 1);
        entry.putInt("Growth", 0);
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.contracted", id), true);
        return true;
    }

    public static boolean feed(ServerPlayer player, String beastId) {
        String id = normalize(beastId);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (!root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing", id), false);
            return false;
        }
        if (!player.getAbilities().instabuild) {
            if (!consumeOne(player, ModItems.BEAST_CORE.get().getDefaultInstance())
                    && !consumeOne(player, ModItems.SPIRIT_STONE_SHARD.get().getDefaultInstance())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.feed_missing"), false);
                return false;
            }
        }
        CompoundTag entry = root.getCompound(id).copy();
        entry.putInt("Affinity", Math.min(100, entry.getInt("Affinity") + 5));
        entry.putInt("Growth", Math.min(20, entry.getInt("Growth") + 1));
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.fed",
                id, entry.getInt("Affinity"), entry.getInt("Growth")), true);
        return true;
    }

    public static boolean summon(ServerPlayer player, String beastId) {
        String id = normalize(beastId);
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        if (!root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing", id), false);
            return false;
        }
        CompoundTag entry = root.getCompound(id);
        int affinity = entry.getInt("Affinity");
        int growth = entry.getInt("Growth");
        double health = 24.0D + affinity * 0.4D + growth * 2.0D;
        double damage = 4.0D + affinity * 0.05D + growth * 0.4D;
        int life = 20 * (25 + growth * 2);
        boolean ok = SummonHonestMvpService.spawnConfigured(player, "beast_" + id, life, health, damage);
        if (ok) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.summoned",
                    id, affinity, growth), true);
        }
        return ok;
    }

    public static Map<String, String> snapshotLines(ServerPlayer player) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Contract contract : list(player)) {
            map.put(contract.id(), "affinity=" + contract.affinity() + ",growth=" + contract.growth());
        }
        return map;
    }

    private static boolean consumeOne(ServerPlayer player, ItemStack sample) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(sample.getItem())) {
                stack.shrink(1);
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
