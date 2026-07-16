package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.server.level.ServerPlayer;

/**
 * M13 realm/dimension flight policy layered on {@link FlyingAuthority}.
 * Grants/revokes the policy source when dimension or realm no longer permits free flight.
 */
public final class FlyingAuthorityPolicy {
    public static final String SOURCE_POLICY = "dimension_policy";

    private FlyingAuthorityPolicy() {}

    public static boolean allowsFreeFlight(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return true;
        }
        String dim = player.level().dimension().location().toString();
        return allowsFreeFlight(player, dim);
    }

    public static boolean allowsFreeFlight(ServerPlayer player, String dimensionId) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return true;
        }
        String dim = DimensionRegistryService.toMinecraftDimensionId(dimensionId);
        // yin pockets: no free flight unless foundation+ ghost path
        if (YinUnderworldClusterService.isYinDimension(dim)) {
            boolean ghost = CultivationHelper.get(player).map(c -> c.isGhostPath()).orElse(false);
            return ghost && ProgressionGateApi.meetsRealm(player, "FOUNDATION");
        }
        // demon rift: nascent soul+
        if (dim.endsWith("demon_rift")) {
            return ProgressionGateApi.meetsRealm(player, "NASCENT_SOUL");
        }
        // secret realms: often no_fly — deny free policy flight
        if (dim.contains("secret_realm")) {
            return ProgressionGateApi.meetsRealm(player, "SOUL_TRANSFORMATION")
                    || ProgressionGateApi.meetsRealm(player, "DEITY_TRANSFORMATION");
        }
        // spirit realm hubs: foundation+ may fly with policy source
        if (dim.endsWith("tianyuan") || dim.endsWith("spirit_fengyuan") || dim.endsWith("immortal_realm")) {
            return ProgressionGateApi.meetsRealm(player, "FOUNDATION");
        }
        // mortal overworld: qi refining late / foundation
        return ProgressionGateApi.meetsRealm(player, "FOUNDATION")
                || CultivationHelper.get(player).map(c -> c.getRealm().ordinal() >= Realm.QI_REFINING.ordinal()
                && c.getRealm() == Realm.QI_REFINING).orElse(false);
    }

    public static boolean allowsVehicle(ServerPlayer player, String vehicleId) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        String dim = player.level().dimension().location().toString();
        if (YinUnderworldClusterService.isYinDimension(dim) && (vehicleId == null || !vehicleId.contains("ghost"))) {
            // only ghost-capable vehicles in yin cluster unless creative
            return CultivationHelper.get(player).map(c -> c.isGhostPath()).orElse(false);
        }
        if (dim.contains("secret_realm") && vehicleId != null && vehicleId.contains("cloud")) {
            return ProgressionGateApi.meetsRealm(player, "CORE_FORMATION");
        }
        return true;
    }

    public static void refresh(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (allowsFreeFlight(player)) {
            // policy source is a soft mayfly grant at baseline speed; other sources may override speed
            FlyingAuthority.grant(player, SOURCE_POLICY, 0.0F);
        } else {
            FlyingAuthority.revoke(player, SOURCE_POLICY, null, 0.0F);
        }
    }

    public static void onDimensionChanged(ServerPlayer player, String newDimensionId) {
        if (player == null) {
            return;
        }
        // clear transient grants that should not cross realms, then re-apply policy
        FlyingAuthority.clearAll(player);
        if (allowsFreeFlight(player, newDimensionId)) {
            FlyingAuthority.grant(player, SOURCE_POLICY, 0.0F);
        }
    }
}
