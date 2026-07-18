package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * M13 realm/dimension flight policy layered on {@link FlyingAuthority}.
 * Grants/revokes the policy source when dimension or realm no longer permits free flight.
 */
public final class FlyingAuthorityPolicy {
    public static final String SOURCE_POLICY = "dimension_policy";

    enum DimensionFlightRule {
        MORTAL,
        YIN,
        DEMON_RIFT,
        SECRET_REALM,
        SPIRIT_REALM,
        DENY
    }

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
        return switch (classifyDimension(dimensionId)) {
            case YIN -> CultivationHelper.get(player).map(c -> c.isGhostPath()).orElse(false)
                    && ProgressionGateApi.meetsRealm(player, "FOUNDATION");
            case DEMON_RIFT -> ProgressionGateApi.meetsRealm(player, "NASCENT_SOUL");
            case SECRET_REALM -> ProgressionGateApi.meetsRealm(player, "SOUL_TRANSFORMATION");
            case SPIRIT_REALM -> ProgressionGateApi.meetsRealm(player, "FOUNDATION");
            case MORTAL -> ProgressionGateApi.meetsRealm(player, "FOUNDATION")
                    || CultivationHelper.get(player)
                    .map(c -> allowsMortalCultivation(c.getRealm(), c.getStage()))
                    .orElse(false);
            case DENY -> false;
        };
    }

    static DimensionFlightRule classifyDimension(String dimensionId) {
        String dim = DimensionRegistryService.toMinecraftDimensionId(dimensionId);
        if (DimensionRegistryService.OVERWORLD.equals(dim)) {
            return DimensionFlightRule.MORTAL;
        }
        if (DimensionRegistryService.YIN_MING_POCKET.equals(dim)
                || DimensionRegistryService.NETHER_RIVER_POCKET.equals(dim)) {
            return DimensionFlightRule.YIN;
        }
        if (DimensionRegistryService.DEMON_RIFT.equals(dim)) {
            return DimensionFlightRule.DEMON_RIFT;
        }
        if (DimensionRegistryService.TIANYUAN.equals(dim)
                || DimensionRegistryService.SPIRIT_FENGYUAN.equals(dim)
                || DimensionRegistryService.IMMORTAL_REALM.equals(dim)) {
            return DimensionFlightRule.SPIRIT_REALM;
        }
        ResourceLocation location = ResourceLocation.tryParse(dim);
        if (location != null && SeekingImmortalsMod.MODID.equals(location.getNamespace())
                && (location.getPath().startsWith("secret_realm_")
                || DimensionRegistryService.ASURA_REALM.equals(dim))) {
            return DimensionFlightRule.SECRET_REALM;
        }
        return DimensionFlightRule.DENY;
    }

    static boolean allowsMortalCultivation(Realm realm, RealmStage stage) {
        return realm == Realm.QI_REFINING && stage != null
                && stage.ordinal() >= RealmStage.LAYER_10.ordinal();
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
