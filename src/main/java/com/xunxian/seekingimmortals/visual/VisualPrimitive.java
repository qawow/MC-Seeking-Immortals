package com.xunxian.seekingimmortals.visual;

import java.util.Locale;

/** Finite renderer primitives emitted by the authored visual compiler. */
public enum VisualPrimitive {
    AURA_BURST, SINGLE_PROJECTILE, GIANT_CLAW, GIANT_HAND, FIST_BARRAGE,
    SWORD_RAIN, PROJECTILE_SWARM, FALLING_BARRAGE, CLOUD_VORTEX, RUNE_ORBIT,
    ARRAY_RINGS, CHAIN_NET, CHAIN_LINKS, SPIRIT_AVATAR, SUMMON_GATE,
    SERPENT_DRAGON, EYE_GAZE, SOUND_WAVE, LOTUS_MANDALA, MOUNTAIN_METEOR,
    MIRROR_DISC, MIST_VEIL, FLAME_BIRD, BEAST_PHANTOM, SPATIAL_RIFT,
    ICE_PRISON, BLOOD_SEA, TREE_AVATAR, SCRIPTURE_GLYPH, MAGNETIC_FIELD,
    LIGHTNING_STORM, WHEEL_DISC, SPEAR_SPIKE, WING_FAN, INSECT_SWARM,
    TIDAL_WAVE, ORB_PROJECTILE, GROUND_FIELD, SPHERE_FIELD, SEAL_CAGE, BARRIER_PLANE,
    BODY_AURA, BODY_SHELL, AFTERIMAGE_PATH, LAYERED_AFTERIMAGES,
    BEAM_LANCE, CHANNEL_STREAM, BLADE_ARC, IMPACT_ARCS, RISING_MOTES,
    CLEANSING_RING, BURNING_TALISMAN,
    // semantic_layers_v3 figure silhouettes (layer-scoped dispatch only).
    CAULDRON_VESSEL, BELL_CHIME, GOURD_VESSEL, LIGHT_CURTAIN,
    HALO_RING, BANNER_STREAMER, SEAL_STAMP, BRIDGE_ARC,
    // 0.2.200 deeper figure silhouettes
    FLYING_SWORD, FORMATION_BANNER, PAGODA_TOWER, BLOOD_THREAD, JADE_SLIP, FIRE_PLUME;

    public static VisualPrimitive parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank visual primitive");
        }
        return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
