package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.item.pill.PillQuality;
import com.xunxian.seekingimmortals.item.pill.PillType;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.visual.VisualEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Emits authored pill/consumable feedback after a server-side use has succeeded. */
public final class ConsumableVfxOrchestrator {
    private static final Map<String, String> CATALOG_PILL_VISUAL_ALIASES = Map.ofEntries(
            Map.entry("spirit_gathering_pill", "juling_pill"),
            Map.entry("fire_origin_pill", "huoyuan_pill"),
            Map.entry("marrow_cleansing_pill", "bone_marrow_pill"),
            Map.entry("essence_condensing_pill", "condensation_pill"),
            Map.entry("clear_void_pill", "calm_spirit_pill"),
            Map.entry("appearance_fixing_pill", "dingyan_pill"),
            Map.entry("longevity_pill", "longevity_fruit_pill"),
            Map.entry("return_yang_true_water", "huiyang_true_water"),
            Map.entry("marrow_extracting_pill", "marrow_extract_pill"),
            Map.entry("soul_breaking_pill", "soul_break_pill"));

    private ConsumableVfxOrchestrator() {}

    public static void emitPill(ServerPlayer player, String pillId, PillQuality quality) {
        if (player == null) {
            return;
        }
        AuthoredConsumableVfxCatalog.findPill(visualPillId(pillId))
                .ifPresent(profile -> emit(player, "pill", profile, qualityScale(quality)));
    }

    /** Resolves runtime-only catalog ids to the closest authored visual profile. */
    public static String visualPillId(String pillId) {
        if (pillId == null) {
            return "";
        }
        String key = pillId.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        return CATALOG_PILL_VISUAL_ALIASES.getOrDefault(key, key);
    }

    public static void emitLegacyPill(ServerPlayer player, PillType type, PillQuality quality) {
        String id = legacyPillId(type);
        if (!id.isBlank()) {
            emitPill(player, id, quality);
        }
    }

    public static void emitConsumable(ServerPlayer player, String catalogId, String effect) {
        if (player == null || isStorageAction(effect)) {
            return;
        }
        AuthoredConsumableVfxCatalog.findConsumable(catalogId)
                .ifPresent(profile -> emit(player, "consumable", profile, 1.0D));
    }

    public static String legacyPillId(PillType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case REJUVENATION -> "spirit_recovery_pill";
            case FOUNDATION_BUILDING -> "foundation_pill";
            case HEALING -> "huichun_pill";
            case CLEAR_SPIRIT_POWDER -> "antidote_pill";
            case FASTING -> "bigu_pill";
            case CALMING -> "qingxin_pill";
            case YELLOW_DRAGON -> "huanglong_pill";
            case GOLDEN_MARROW -> "bone_marrow_pill";
            case HARMONIZING_QI -> "heqi_pill";
            case SPIRIT_GATHERING -> "juling_pill";
            case QI_REFINING_POWDER -> "spirit_condense_minor";
            case TRUE_ESSENCE -> "solid_essence_pill";
            case DUST_DESCENDING -> "jiangchen_pill";
            case GOLDEN_CORE -> "golden_core_pill";
            case NASCENT_SOUL -> "nascent_soul_pill";
            case ESSENCE_NOURISHING -> "yanghun_pill";
            case BONE_MENDING -> "marrow_repair_pill";
            case SOUL_RETURN -> "ninghun_dan";
            case AURA_CONCEALMENT -> "demon_suppress_pill";
            case BEAUTY_PRESERVING -> "longevity_fruit_pill";
            case APPEARANCE_FIXING -> "dingyan_pill";
            case LONGEVITY -> "longevity_fruit_pill";
            case LONGEVITY_ETERNAL -> "longevity_fruit_pill";
            case SPIRIT_STABILIZING -> "ningshen_pill";
            case HEAVEN_MENDING -> "bu_tian_pill";
        };
    }

    private static void emit(ServerPlayer player,
                             String domain,
                             AuthoredConsumableVfxCatalog.Profile profile,
                             double scale) {
        if (profile == null || profile.storageLike()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, Math.min(1.15D, player.getBbHeight() * 0.58D), 0.0D);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            look = look.normalize();
        }
        double radius = Math.max(0.35D, Math.min(4.0D, profile.radius() * scale));
        int intensity = Math.max(8, Math.min(48, (int) Math.round(profile.intensity() * scale)));
        long seed = player.blockPosition().asLong() ^ ((long) profile.id().hashCode() << 17)
                ^ level.getGameTime();

        TechniqueVfxPacket.Kind kind = profile.vfxKind();
        Vec3 castStart = eye.add(look.scale(0.35D));
        Vec3 castEnd = castStart.add(look.scale(kind == TechniqueVfxPacket.Kind.CAST ? 1.55D : 0.8D));
        VisualEventDispatcher.event(level, domain, profile.id(), "CAST", castStart, castEnd,
                Math.max(0.35D, radius * 0.42D), Math.max(8, intensity / 2), seed, 1);

        if (kind == TechniqueVfxPacket.Kind.CAST) {
            return;
        }
        Vec3 semanticEnd = switch (kind) {
            case PATH, BEAM, CONE -> center.add(look.scale(Math.max(1.4D, radius * 1.8D)));
            case AURA, STATUS, DISSIPATE -> center.add(0.0D, player.getBbHeight(), 0.0D);
            case BURST, IMPACT, FORMATION, SCAN, CAST -> center;
        };
        VisualEventDispatcher.event(level, domain, profile.id(), kind.name(), center, semanticEnd,
                radius, intensity, seed ^ 0x9E3779B97F4A7C15L,
                profile.telegraphed() ? 2 : 1);
    }

    private static double qualityScale(PillQuality quality) {
        return quality == null ? PillQuality.LOW.getEffectMultiplier() : quality.getEffectMultiplier();
    }

    private static boolean isStorageAction(String effect) {
        if (effect == null) {
            return false;
        }
        String action = effect.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        return action.startsWith("portable_storage_") || action.startsWith("extra_inventory_slots_");
    }
}
