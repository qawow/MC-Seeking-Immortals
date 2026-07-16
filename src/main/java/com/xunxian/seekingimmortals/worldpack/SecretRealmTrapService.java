package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.structure.FormationFieldService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

/**
 * M09 trap placement that consumes M07 {@link FormationFieldService} free fields.
 * Does not invent a second field system.
 */
public final class SecretRealmTrapService {
    private static final int DEFAULT_TRAP_DURATION = 20 * 90;

    private SecretRealmTrapService() {}

    public static int activateLayerTraps(ServerPlayer player, String realmId, int layerIndex) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        SecretRealmCatalogService.RealmDef realm = SecretRealmCatalogService.find(realmId).orElse(null);
        if (realm == null || realm.layers().isEmpty()) {
            return 0;
        }
        int index = Math.max(0, Math.min(layerIndex, realm.layers().size() - 1));
        SecretRealmCatalogService.LayerDef layer = realm.layers().get(index);
        return activateTraps(level, player.blockPosition(), layer.traps(), DEFAULT_TRAP_DURATION);
    }

    public static int activateAllLayerTraps(ServerPlayer player, String realmId) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        SecretRealmCatalogService.RealmDef realm = SecretRealmCatalogService.find(realmId).orElse(null);
        if (realm == null) {
            return 0;
        }
        int activated = 0;
        BlockPos base = player.blockPosition();
        List<SecretRealmCatalogService.LayerDef> layers = realm.layers();
        for (int i = 0; i < layers.size(); i++) {
            // Offset free fields so multiple traps do not collapse onto one key.
            BlockPos center = base.offset((i % 3) * 3, 0, (i / 3) * 3);
            activated += activateTraps(level, center, layers.get(i).traps(), DEFAULT_TRAP_DURATION + i * 20);
        }
        return activated;
    }

    public static int activateTraps(
            ServerLevel level,
            BlockPos center,
            List<SecretRealmCatalogService.TrapDef> traps,
            int durationTicks) {
        if (level == null || center == null || traps == null || traps.isEmpty()) {
            return 0;
        }
        int activated = 0;
        int offset = 0;
        for (SecretRealmCatalogService.TrapDef trap : traps) {
            FormationFieldService.FieldKind kind = parseKind(trap.fieldKind());
            if (kind == null) {
                continue;
            }
            BlockPos pos = center.offset((offset % 2 == 0 ? 2 : -2), 0, (offset < 2 ? 2 : -2));
            if (FormationFieldService.activateFreeField(level, pos, kind, durationTicks)) {
                activated++;
            }
            offset++;
        }
        return activated;
    }

    public static FormationFieldService.FieldKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return FormationFieldService.FieldKind.valueOf(key);
        } catch (RuntimeException ignored) {
            if (key.contains("ILLUSION") || key.contains("MAZE")) {
                return FormationFieldService.FieldKind.ILLUSION_MAZE;
            }
            if (key.contains("SEAL") || key.contains("DEMON")) {
                return FormationFieldService.FieldKind.SEAL_DEMON;
            }
            if (key.contains("KILL") || key.contains("SWORD")) {
                return FormationFieldService.FieldKind.KILL_SWORD;
            }
            if (key.contains("DEFENSE")) {
                return FormationFieldService.FieldKind.DEFENSE;
            }
            if (key.contains("GATHER") || key.contains("SPIRIT")) {
                return FormationFieldService.FieldKind.SPIRIT_GATHER;
            }
            return FormationFieldService.FieldKind.CATALOG_GENERIC;
        }
    }
}
