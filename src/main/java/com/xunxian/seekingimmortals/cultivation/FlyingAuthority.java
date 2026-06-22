package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * 单一飞行授权中心：以"来源集合 + 首次采样基线"解决御剑术与法宝飞行各自维护 previousMayfly 快照
 * 导致的 grant/revoke 双向覆盖竞态（H1）。
 *
 * <p>语义：
 * <ul>
 *   <li>{@link #grant}：来源集合为空时才采样 mayfly/flyingSpeed 基线；把来源加入集合；置 mayfly=true；
 *       若给了 speed 则覆盖 flyingSpeed。</li>
 *   <li>{@link #revoke}：从集合移除来源；集合仍非空说明另一来源仍在生效，仅同步速度到剩余来源的期望值，
 *       不动 mayfly（避免覆盖仍生效的对方）；集合为空才恢复基线 mayfly/flyingSpeed。</li>
 *   <li>{@link #clearAll}：供 respawn/换维/死亡清理（H11）一次性清空并恢复基线。</li>
 * </ul>
 *
 * <p>所有状态写入 Forge 玩家 {@link net.minecraft.world.entity.player.Player#getPersistentData()}，
 * 与既有飞行代码一致；非创造/旁观靠调用方既有 early-return 保护，故基线 always 视为"无飞行权限"。
 */
public final class FlyingAuthority {

    public static final String SOURCE_ARTIFACT = "artifact";
    public static final String SOURCE_QI_FLYING = "qi_flying";

    private static final String SOURCES_KEY = "SeekingImmortalsFlyingSources";
    private static final String BASELINE_MAYFLY_KEY = "SeekingImmortalsFlyingBaselineMayfly";
    private static final String BASELINE_SPEED_KEY = "SeekingImmortalsFlyingBaselineSpeed";

    private FlyingAuthority() {
    }

    /** 当前生效的来源数。 */
    public static int activeSourceCount(ServerPlayer player) {
        return readSources(player).size();
    }

    /**
     * 授予某来源飞行权限。
     *
     * @param speed 若 &gt; 0 则覆盖当前 flyingSpeed；&lt;= 0 表示不调整速度（保留既有 speed）。
     */
    public static void grant(ServerPlayer player, String source, float speed) {
        CompoundTag data = player.getPersistentData();
        Set<String> sources = readSources(player);
        if (sources.isEmpty()) {
            data.putBoolean(BASELINE_MAYFLY_KEY, player.getAbilities().mayfly);
            data.putFloat(BASELINE_SPEED_KEY, player.getAbilities().getFlyingSpeed());
        }
        sources.add(source);
        writeSources(data, sources);

        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
        }
        if (speed > 0.0F && Math.abs(player.getAbilities().getFlyingSpeed() - speed) > 0.0001F) {
            player.getAbilities().setFlyingSpeed(speed);
        }
        player.onUpdateAbilities();
    }

    /**
     * 撤销某来源飞行权限，并按需恢复基线或切到剩余来源期望速度。
     *
     * @param fallbackSpeed 集合非空时把 speed 切回此值（剩余来源的期望速度）；&lt;= 0 不调整。
     */
    public static void revoke(ServerPlayer player, String source, String reasonKey, float fallbackSpeed) {
        CompoundTag data = player.getPersistentData();
        Set<String> sources = readSources(player);
        if (!sources.remove(source)) {
            return;
        }
        writeSources(data, sources);

        if (sources.isEmpty()) {
            boolean baselineMayfly = data.getBoolean(BASELINE_MAYFLY_KEY);
            float baselineSpeed = data.contains(BASELINE_SPEED_KEY, Tag.TAG_FLOAT)
                    ? data.getFloat(BASELINE_SPEED_KEY) : 0.05F;
            data.remove(BASELINE_MAYFLY_KEY);
            data.remove(BASELINE_SPEED_KEY);
            player.getAbilities().mayfly = baselineMayfly;
            if (!baselineMayfly) {
                player.getAbilities().flying = false;
            }
            player.getAbilities().setFlyingSpeed(baselineSpeed);
        } else if (fallbackSpeed > 0.0F) {
            player.getAbilities().setFlyingSpeed(fallbackSpeed);
        }
        player.onUpdateAbilities();
        if (reasonKey != null) {
            player.displayClientMessage(Component.translatable(reasonKey), true);
        }
    }

    /** 一次性清空所有来源并恢复基线（respawn/换维/死亡，H11）。不发送消息。 */
    public static void clearAll(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SOURCES_KEY, Tag.TAG_LIST)) {
            return;
        }
        boolean baselineMayfly = data.getBoolean(BASELINE_MAYFLY_KEY);
        float baselineSpeed = data.contains(BASELINE_SPEED_KEY, Tag.TAG_FLOAT)
                ? data.getFloat(BASELINE_SPEED_KEY) : 0.05F;
        data.remove(SOURCES_KEY);
        data.remove(BASELINE_MAYFLY_KEY);
        data.remove(BASELINE_SPEED_KEY);
        player.getAbilities().mayfly = baselineMayfly;
        if (!baselineMayfly) {
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(baselineSpeed);
        player.onUpdateAbilities();
    }

    private static Set<String> readSources(ServerPlayer player) {
        Set<String> sources = new HashSet<>();
        CompoundTag data = player.getPersistentData();
        if (data.contains(SOURCES_KEY, Tag.TAG_LIST)) {
            for (Tag tag : data.getList(SOURCES_KEY, Tag.TAG_STRING)) {
                if (tag instanceof StringTag stringTag) {
                    sources.add(stringTag.getAsString());
                }
            }
        }
        return sources;
    }

    private static void writeSources(CompoundTag data, Set<String> sources) {
        if (sources.isEmpty()) {
            data.remove(SOURCES_KEY);
        } else {
            ListTag list = new ListTag();
            for (String source : sources) {
                list.add(StringTag.valueOf(source));
            }
            data.put(SOURCES_KEY, list);
        }
    }
}
