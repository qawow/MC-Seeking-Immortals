package com.xunxian.seekingimmortals.visual;

import com.xunxian.seekingimmortals.network.VisualEventPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Server-side constructors for bounded cosmetic lifecycle packets. */
public final class VisualEventDispatcher {
    private VisualEventDispatcher() {}

    public static void event(ServerLevel level, String domain, String profileId, String trigger,
                             Vec3 start, Vec3 end, double scale, int intensity,
                             long seed, int priority) {
        if (level == null || start == null) {
            return;
        }
        Vec3 target = end == null ? start : end;
        VisualEventPacket.send(level, packet(domain, profileId, VisualEventPacket.Lifecycle.EVENT,
                trigger, VisualEventPacket.AnchorType.WORLD, -1, BlockPos.ZERO.asLong(),
                start, target, "", 0, 0, scale, intensity, seed, priority));
    }

    public static void entity(ServerLevel level, String domain, String profileId,
                              VisualEventPacket.Lifecycle lifecycle, String trigger,
                              Entity entity, String instanceKey, int durationTicks, int ageTicks,
                              double scale, int intensity, long seed, int priority) {
        if (level == null || entity == null) {
            return;
        }
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        VisualEventPacket.send(level, packet(domain, profileId, lifecycle, trigger,
                VisualEventPacket.AnchorType.ENTITY, entity.getId(), entity.blockPosition().asLong(),
                center, center, instanceKey, durationTicks, ageTicks,
                scale, intensity, seed, priority));
    }

    public static void block(ServerLevel level, String domain, String profileId,
                             VisualEventPacket.Lifecycle lifecycle, String trigger,
                             BlockPos pos, String instanceKey, int durationTicks, int ageTicks,
                             double scale, int intensity, long seed, int priority) {
        if (level == null || pos == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        VisualEventPacket.send(level, packet(domain, profileId, lifecycle, trigger,
                VisualEventPacket.AnchorType.BLOCK, -1, pos.asLong(), center, center,
                instanceKey, durationTicks, ageTicks, scale, intensity, seed, priority));
    }

    public static String entityKey(String domain, Entity entity, String profileId) {
        if (entity == null) {
            return "";
        }
        return token(domain) + ":" + entity.getUUID() + ":" + token(profileId);
    }

    public static String blockKey(String domain, ServerLevel level, BlockPos pos, String profileId) {
        if (level == null || pos == null) {
            return "";
        }
        return token(domain) + ":" + level.dimension().location() + ":"
                + Long.toUnsignedString(pos.asLong(), 36) + ":" + token(profileId);
    }

    private static VisualEventPacket packet(String domain, String profileId,
                                            VisualEventPacket.Lifecycle lifecycle, String trigger,
                                            VisualEventPacket.AnchorType anchorType, int entityId,
                                            long blockPos, Vec3 start, Vec3 target,
                                            String instanceKey, int durationTicks, int ageTicks,
                                            double scale, int intensity, long seed, int priority) {
        String safeDomain = token(domain);
        String safeId = profileId == null ? "" : profileId.trim();
        ResourceLocation profile;
        // Accept both the preferred raw id (domain + id) and a qualified id
        // from synchronized entity data.  Older callers occasionally pass the
        // latter; replacing ':' with '_' would make the client miss the
        // authored profile entirely.
        if (safeId.indexOf(':') >= 0) {
            profile = ResourceLocation.tryParse(VisualDomain.normalizeKey(safeId));
            if (profile != null) {
                safeDomain = token(profile.getNamespace());
                safeId = token(profile.getPath());
            } else {
                safeId = token(safeId);
            }
        } else {
            safeId = token(safeId);
            profile = ResourceLocation.tryBuild(safeDomain, safeId);
        }
        if (profile == null) {
            profile = ResourceLocation.tryBuild(safeDomain, safeId);
        }
        if (profile == null) {
            profile = new ResourceLocation("generic", "generic");
        }
        return new VisualEventPacket(safeDomain, profile, lifecycle, trigger, anchorType,
                entityId, blockPos, start.x, start.y, start.z,
                target.x, target.y, target.z, instanceKey, durationTicks, ageTicks,
                (float) scale, intensity, seed, priority);
    }

    private static String token(String value) {
        if (value == null || value.isBlank()) {
            return "generic";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_').replace(':', '_');
    }
}
