package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket.Kind;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket.Motif;
import com.xunxian.seekingimmortals.network.VisualEventPacket;
import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import com.xunxian.seekingimmortals.visual.VisualEventDispatcher;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.List;
import java.util.Set;

/** Plans and emits the shared two-stage visual language for successful technique casts. */
public final class TechniqueVfxOrchestrator {
    private static final Set<String> SELF_TYPES = Set.of(
            "buff_self", "buff", "shield", "transform", "heal", "heal_spirit", "cleanse");
    private static final Set<String> MOVEMENT_TYPES = Set.of(
            "movement", "dash", "escape", "teleport_short");

    private TechniqueVfxOrchestrator() {}

    public record VisualPlan(
            TechniqueVfxPalette.Family family,
            Motif motif,
            Kind kind,
            TechniqueVfxPacket.ParticleStyle particleStyle,
            TechniqueVfxPacket.TrailStyle trailStyle,
            boolean telegraphed,
            double range,
            double radius,
            int intensity
    ) {
        public VisualPlan {
            family = family == null ? TechniqueVfxPalette.Family.NEUTRAL : family;
            motif = motif == null ? Motif.GENERIC : motif;
            kind = kind == null || kind == Kind.CAST ? Kind.BURST : kind;
            particleStyle = particleStyle == null ? TechniqueVfxPacket.ParticleStyle.DEFAULT : particleStyle;
            trailStyle = trailStyle == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trailStyle;
            range = clampFinite(range, 0.0D, 48.0D, 0.0D);
            radius = clampFinite(radius, 0.35D, 12.0D, 1.0D);
            intensity = Math.max(8, Math.min(48, intensity));
        }

        public int castIntensity() {
            return Math.max(6, Math.min(24, (int) Math.round(intensity * 0.52D)));
        }
    }

    /** Optional authored skin layered over an existing technique geometry plan. */
    public record VisualOverride(
            TechniqueVfxPalette.Family family,
            Motif motif,
            TechniqueVfxPacket.ParticleStyle particleStyle,
            TechniqueVfxPacket.TrailStyle trailStyle,
            boolean telegraphed
    ) {
        public VisualOverride {
            family = family == null ? TechniqueVfxPalette.Family.NEUTRAL : family;
            motif = motif == null ? Motif.GENERIC : motif;
            particleStyle = particleStyle == null
                    ? TechniqueVfxPacket.ParticleStyle.DEFAULT : particleStyle;
            trailStyle = trailStyle == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trailStyle;
        }
    }

    public static VisualPlan plan(TechniqueDataManager.TechniqueEntry technique,
                                  SkillType skillType,
                                  boolean secondary) {
        if (technique == null) {
            return plan("", "", "", "", Set.of(), "", "", "", "",
                    skillType == null ? "" : skillType.name(), secondary);
        }
        return plan(
                technique.id(),
                technique.effectType(),
                technique.effectElement(),
                technique.effectKey(),
                technique.tags(),
                technique.target(),
                technique.range(),
                technique.attribute(),
                join(technique.source(), technique.name()),
                skillType == null ? "" : skillType.name(),
                secondary);
    }

    public static VisualPlan plan(String id,
                                  String effectType,
                                  String effectElement,
                                  String effectKey,
                                  Set<String> tags,
                                  String target,
                                  String range,
                                  String attribute,
                                  String source,
                                  String skillTypeName,
                                  boolean secondary) {
        String type = normalize(effectType);
        String safeTarget = normalize(target);
        String skillName = normalizeSkillName(skillTypeName);
        String tagText = tags == null ? "" : String.join(" ", tags);
        String blob = join(id, type, effectElement, effectKey, tagText, safeTarget, range,
                attribute, source, skillName);

        Motif motif = motifForSkillName(skillName);
        boolean lockedSkillMotif = motif != null;
        if (!lockedSkillMotif) {
            motif = inferMotif(type, blob);
        }
        TechniqueVfxPalette.Family family = inferFamily(effectElement, attribute, tagText, blob, motif);
        Kind kind = inferKind(type, safeTarget, blob, motif);
        double plannedRange = resolveRange(range, type, safeTarget);
        double radius = resolveRadius(type, safeTarget, motif);
        int intensity = resolveIntensity(type, kind, secondary);
        VisualPlan fallback = new VisualPlan(family, motif, kind,
                TechniqueVfxPacket.ParticleStyle.DEFAULT,
                TechniqueVfxPacket.TrailStyle.DEFAULT,
                false,
                plannedRange, radius, intensity);
        AuthoredSpellEffectCatalog.Profile authored = AuthoredSpellEffectCatalog.find(id).orElse(null);
        if (authored != null) {
            return applyAuthoredSpellProfile(authored, secondary);
        }
        return AuthoredTechniqueVfxCatalog.find(id)
                .map(profile -> applyAuthoredProfile(
                        fallback, profile, type, safeTarget, blob, secondary, lockedSkillMotif))
                .orElse(fallback);
    }

    private static VisualPlan applyAuthoredSpellProfile(AuthoredSpellEffectCatalog.Profile profile,
                                                        boolean secondary) {
        AuthoredSpellEffectCatalog.Functional functional = profile.functional();
        Kind kind = inferKind(functional.type(), functional.target(),
                join(profile.id(), profile.shape(), profile.source()), profile.motif());
        if (kind == Kind.CAST) {
            kind = Kind.BURST;
        }
        int intensity = secondary
                ? Math.max(8, (int) Math.round(profile.intensity() * 0.72D))
                : profile.intensity();
        return new VisualPlan(
                profile.family(), profile.motif(), kind, profile.particle(), profile.trail(),
                profile.telegraphed(), functional.range(), functional.radius(), intensity);
    }

    private static VisualPlan applyAuthoredProfile(VisualPlan fallback,
                                                   AuthoredTechniqueVfxCatalog.Profile profile,
                                                   String effectType,
                                                   String target,
                                                   String blob,
                                                   boolean secondary,
                                                   boolean lockedSkillMotif) {
        Motif motif = authoredMotif(profile, fallback.motif(), lockedSkillMotif);
        TechniqueVfxPalette.Family family = fallback.family();
        if (family == TechniqueVfxPalette.Family.NEUTRAL) {
            TechniqueVfxPalette.Family authoredFamily = TechniqueVfxPalette.familyOf(profile.element());
            if (authoredFamily != TechniqueVfxPalette.Family.NEUTRAL) {
                family = authoredFamily;
            }
        }
        Kind kind = fallback.kind();
        if (!profile.effectType().isBlank()) {
            kind = inferKind(profile.effectType(), target,
                    join(blob, profile.shape(), profile.school()), motif);
            if (kind == Kind.CAST) {
                kind = fallback.kind();
            }
        }
        String authoredType = profile.effectType().isBlank() ? effectType : profile.effectType();
        double radius = resolveRadius(authoredType, target, motif);
        int intensity = resolveIntensity(authoredType, kind, secondary);
        return new VisualPlan(family, motif, kind, profile.particle(), profile.trail(), profile.telegraphed(),
                fallback.range(), radius, intensity);
    }

    private static Motif authoredMotif(AuthoredTechniqueVfxCatalog.Profile profile,
                                       Motif fallback,
                                       boolean lockedSkillMotif) {
        if (lockedSkillMotif) {
            return fallback;
        }
        // The v122 shape language is the primary silhouette contract. A trail is an
        // additional ribbon style and must not erase a stronger authored geometry.
        switch (profile.shape()) {
            case "冲击环" -> { return Motif.MARTIAL; }
            case "地网" -> { return Motif.FORMATION; }
            case "丝连" -> { return Motif.CHAIN; }
            case "绿金粒" -> { return Motif.HEAL; }
            case "纸焚轨迹" -> { return Motif.TALISMAN; }
            case "薄线" -> { return Motif.BLADE; }
            case "重影", "残影" -> { return Motif.ILLUSION; }
            case "半透", "湿暗" -> { return Motif.GHOST; }
            case "风骨混合" -> { return Motif.TELEPORT; }
            case "弹/扇/柱随元素" -> { return Motif.PROJECTILE; }
            default -> { }
        }
        TechniqueVfxPacket.TrailStyle trail = profile.trail();
        if (trail == TechniqueVfxPacket.TrailStyle.SWORD_THIN) return Motif.BLADE;
        if (trail == TechniqueVfxPacket.TrailStyle.TALISMAN_ASH) return Motif.TALISMAN;
        if (trail == TechniqueVfxPacket.TrailStyle.MOVEMENT_WIND) return Motif.TELEPORT;
        if (trail == TechniqueVfxPacket.TrailStyle.HEAVY_WEAPON) return Motif.MARTIAL;
        if (trail == TechniqueVfxPacket.TrailStyle.SOUL_AFTERIMAGE) return Motif.ILLUSION;
        return fallback;
    }

    public static void emitSuccessfulCast(ServerPlayer player,
                                          TechniqueDataManager.TechniqueEntry technique,
                                          SkillType skillType,
                                          Vec3 beforeCast,
                                          boolean secondary) {
        emitSuccessfulCast(player, technique, skillType, beforeCast, List.of(), secondary);
    }

    public static void emitSuccessfulCast(ServerPlayer player,
                                          TechniqueDataManager.TechniqueEntry technique,
                                          SkillType skillType,
                                          Vec3 beforeCast,
                                          List<TechniqueVfxPacket> capturedIntents,
                                          boolean secondary) {
        emitSuccessfulCast(player, technique, skillType, beforeCast, capturedIntents, secondary, null);
    }

    public static void emitSuccessfulCast(ServerPlayer player,
                                          TechniqueDataManager.TechniqueEntry technique,
                                          SkillType skillType,
                                          Vec3 beforeCast,
                                          List<TechniqueVfxPacket> capturedIntents,
                                          boolean secondary,
                                          VisualOverride visualOverride) {
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        VisualPlan plan = applyOverride(plan(technique, skillType, secondary), visualOverride);
        Vec3 after = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 look = normalized(player.getLookAngle());
        double eyeOffset = Math.max(0.5D, eye.y - after.y);
        boolean moved = finite(beforeCast) && beforeCast.distanceToSqr(after) > 0.01D;
        TechniqueVfxPacket capturedCast = applyOverride(
                selectCaptured(capturedIntents, Kind.CAST), visualOverride);
        TechniqueVfxPacket capturedSemantic = applyOverride(
                selectSemantic(capturedIntents, plan.kind()), visualOverride);

        boolean movementTechnique = isMovementTechnique(technique);
        Vec3 castStart = capturedCast == null
                ? (movementTechnique && moved
                        ? beforeCast.add(0.0D, eyeOffset, 0.0D)
                        : eye)
                : start(capturedCast);
        Vec3 castEnd = capturedCast == null ? castStart.add(look.scale(1.1D)) : end(capturedCast);
        double castRadius = capturedCast == null
                ? Math.max(0.48D, Math.min(1.25D, plan.radius() * 0.34D))
                : capturedCast.radius();
        long seed = seed(level, player, technique, skillType, secondary);
        if (useUnifiedProfile(technique)) {
            VisualEventDispatcher.event(level, "technique", technique.id(), "CAST",
                    castStart, castEnd, castRadius, plan.castIntensity(), seed,
                    plan.telegraphed() ? 2 : 1);
        } else {
            TechniqueVfxPacket.send(
                    level,
                    Kind.CAST,
                    family(plan, capturedCast),
                    motif(plan, capturedCast),
                    particleStyle(plan, capturedCast),
                    trailStyle(plan, capturedCast),
                    telegraphed(plan, capturedCast),
                    castStart,
                    castEnd,
                    castRadius,
                    plan.castIntensity(),
                    seed);
        }

        Geometry geometry = geometry(player, technique, plan, beforeCast, moved, eyeOffset, look);
        Kind semanticKind = capturedSemantic == null ? plan.kind() : capturedSemantic.kind();
        Vec3 semanticStart = capturedSemantic == null ? geometry.start() : start(capturedSemantic);
        Vec3 semanticEnd = capturedSemantic == null ? geometry.end() : end(capturedSemantic);
        double semanticRadius = capturedSemantic == null ? plan.radius() : capturedSemantic.radius();
        if (useUnifiedProfile(technique)) {
            VisualEventDispatcher.event(level, "technique", technique.id(), semanticKind.name(),
                    semanticStart, semanticEnd, semanticRadius, plan.intensity(),
                    seed ^ 0x6A09E667F3BCC909L, plan.telegraphed() ? 2 : 1);
        } else {
            TechniqueVfxPacket.send(
                    level,
                    semanticKind,
                    family(plan, capturedSemantic),
                    motif(plan, capturedSemantic),
                    particleStyle(plan, capturedSemantic),
                    trailStyle(plan, capturedSemantic),
                    telegraphed(plan, capturedSemantic),
                    semanticStart,
                    semanticEnd,
                    semanticRadius,
                    plan.intensity(),
                    seed ^ 0x6A09E667F3BCC909L);
        }
    }

    private static boolean useUnifiedProfile(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null || technique.id() == null || technique.id().isBlank()) {
            return false;
        }
        return AuthoredVisualCatalog.resolve("technique:" + technique.id()).isPresent();
    }

    private static VisualPlan applyOverride(VisualPlan plan, VisualOverride override) {
        if (override == null) {
            return plan;
        }
        return new VisualPlan(
                override.family() == TechniqueVfxPalette.Family.NEUTRAL ? plan.family() : override.family(),
                override.motif() == Motif.GENERIC ? plan.motif() : override.motif(),
                plan.kind(),
                override.particleStyle() == TechniqueVfxPacket.ParticleStyle.DEFAULT
                        ? plan.particleStyle() : override.particleStyle(),
                override.trailStyle() == TechniqueVfxPacket.TrailStyle.DEFAULT
                        ? plan.trailStyle() : override.trailStyle(),
                plan.telegraphed() || override.telegraphed(),
                plan.range(), plan.radius(), plan.intensity());
    }

    private static TechniqueVfxPacket applyOverride(TechniqueVfxPacket packet, VisualOverride override) {
        if (packet == null || override == null) {
            return packet;
        }
        return new TechniqueVfxPacket(
                packet.kind(),
                override.family() == TechniqueVfxPalette.Family.NEUTRAL ? packet.family() : override.family(),
                override.motif() == Motif.GENERIC ? packet.motif() : override.motif(),
                override.particleStyle() == TechniqueVfxPacket.ParticleStyle.DEFAULT
                        ? packet.particleStyle() : override.particleStyle(),
                override.trailStyle() == TechniqueVfxPacket.TrailStyle.DEFAULT
                        ? packet.trailStyle() : override.trailStyle(),
                packet.telegraphed() || override.telegraphed(),
                packet.x(), packet.y(), packet.z(), packet.endX(), packet.endY(), packet.endZ(),
                packet.radius(), packet.intensity(), packet.seed());
    }

    private static TechniqueVfxPacket selectCaptured(List<TechniqueVfxPacket> captured, Kind kind) {
        if (captured == null || captured.isEmpty()) {
            return null;
        }
        for (int i = captured.size() - 1; i >= 0; i--) {
            TechniqueVfxPacket packet = captured.get(i);
            if (packet != null && packet.kind() == kind) {
                return packet;
            }
        }
        return null;
    }

    static TechniqueVfxPacket selectSemantic(List<TechniqueVfxPacket> captured, Kind preferred) {
        TechniqueVfxPacket exact = selectCaptured(captured, preferred);
        if (exact != null && exact.kind() != Kind.CAST) {
            return exact;
        }
        if (captured == null) {
            return null;
        }
        for (int i = captured.size() - 1; i >= 0; i--) {
            TechniqueVfxPacket packet = captured.get(i);
            if (packet != null && packet.kind() != Kind.CAST) {
                return packet;
            }
        }
        return null;
    }

    private static TechniqueVfxPalette.Family family(VisualPlan plan, TechniqueVfxPacket captured) {
        if (captured != null && captured.family() != TechniqueVfxPalette.Family.NEUTRAL) {
            return captured.family();
        }
        return plan.family();
    }

    static Motif motif(VisualPlan plan, TechniqueVfxPacket captured) {
        if (captured != null && captured.motif() != Motif.GENERIC) {
            return captured.motif();
        }
        return plan.motif();
    }

    private static TechniqueVfxPacket.ParticleStyle particleStyle(VisualPlan plan,
                                                                  TechniqueVfxPacket captured) {
        if (captured != null && captured.particleStyle() != TechniqueVfxPacket.ParticleStyle.DEFAULT) {
            return captured.particleStyle();
        }
        return plan.particleStyle();
    }

    private static TechniqueVfxPacket.TrailStyle trailStyle(VisualPlan plan,
                                                            TechniqueVfxPacket captured) {
        if (captured != null && captured.trailStyle() != TechniqueVfxPacket.TrailStyle.DEFAULT) {
            return captured.trailStyle();
        }
        return plan.trailStyle();
    }

    private static boolean telegraphed(VisualPlan plan, TechniqueVfxPacket captured) {
        return (captured != null && captured.telegraphed()) || plan.telegraphed();
    }

    private static Vec3 start(TechniqueVfxPacket packet) {
        return new Vec3(packet.x(), packet.y(), packet.z());
    }

    private static Vec3 end(TechniqueVfxPacket packet) {
        return new Vec3(packet.endX(), packet.endY(), packet.endZ());
    }

    private static Geometry geometry(ServerPlayer player,
                                     TechniqueDataManager.TechniqueEntry technique,
                                     VisualPlan plan,
                                     Vec3 beforeCast,
                                     boolean moved,
                                     double eyeOffset,
                                     Vec3 look) {
        Vec3 after = player.position();
        Vec3 eye = player.getEyePosition();
        if (isMovementTechnique(technique) && moved) {
            return new Geometry(
                    beforeCast.add(0.0D, eyeOffset, 0.0D),
                    after.add(0.0D, eyeOffset, 0.0D));
        }

        double distance = Math.max(1.5D, plan.range());
        Vec3 aimed = eye.add(look.scale(distance));
        boolean selfCentered = selfCentered(technique, plan);
        return switch (plan.kind()) {
            case PATH, BEAM, CONE -> new Geometry(eye, aimed);
            case AURA, SCAN, STATUS, DISSIPATE -> {
                Vec3 center = after.add(0.0D, 0.16D, 0.0D);
                yield new Geometry(center, center);
            }
            case FORMATION -> {
                Vec3 center = selfCentered
                        ? after.add(0.0D, 0.12D, 0.0D)
                        : new Vec3(aimed.x, after.y + 0.12D, aimed.z);
                yield new Geometry(center, center);
            }
            case IMPACT, BURST -> {
                Vec3 center = selfCentered ? after.add(0.0D, 0.75D, 0.0D) : aimed;
                yield new Geometry(center, center);
            }
            case CAST -> new Geometry(eye, eye.add(look));
        };
    }

    private static boolean selfCentered(TechniqueDataManager.TechniqueEntry technique, VisualPlan plan) {
        String type = technique == null ? "" : normalize(technique.effectType());
        String target = technique == null ? "" : normalize(technique.target());
        return plan.range() <= 0.05D
                || "self".equals(target)
                || "caster".equals(target)
                || "ally".equals(target)
                || "allies".equals(target)
                || SELF_TYPES.contains(type)
                || plan.motif() == Motif.SHIELD
                || plan.motif() == Motif.HEAL
                || plan.motif() == Motif.CLEANSE;
    }

    private static boolean isMovementTechnique(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return false;
        }
        String type = normalize(technique.effectType());
        return MOVEMENT_TYPES.contains(type);
    }

    private static TechniqueVfxPalette.Family inferFamily(String effectElement,
                                                            String attribute,
                                                            String tagText,
                                                            String blob,
                                                            Motif motif) {
        TechniqueVfxPalette.Family family = TechniqueVfxPalette.familyOf(
                join(effectElement, attribute, tagText));
        if (family == TechniqueVfxPalette.Family.NEUTRAL) {
            family = TechniqueVfxPalette.familyOf(blob);
        }
        if (family != TechniqueVfxPalette.Family.NEUTRAL) {
            return family;
        }
        return switch (motif) {
            case BUDDHIST, CONFUCIAN, CLEANSE, SEAL -> TechniqueVfxPalette.Family.LIGHT;
            case DAO -> TechniqueVfxPalette.Family.THUNDER;
            case GHOST, SUMMON, CHANNEL -> TechniqueVfxPalette.Family.SOUL;
            case ILLUSION -> TechniqueVfxPalette.Family.ILLUSION;
            case BLADE, CHAIN -> TechniqueVfxPalette.Family.METAL;
            case SHIELD, WALL -> TechniqueVfxPalette.Family.EARTH;
            case TELEPORT -> TechniqueVfxPalette.Family.VOID;
            case HEAL -> TechniqueVfxPalette.Family.WOOD;
            case MARTIAL -> TechniqueVfxPalette.Family.EARTH;
            default -> TechniqueVfxPalette.Family.NEUTRAL;
        };
    }

    private static Motif motifForSkillName(String skillName) {
        if (skillName.isBlank()) {
            return null;
        }
        return switch (skillName) {
            case "BUDDHA_LIGHT", "DEMON_SUBDUE_PALM", "VAJRA_PALM", "DAJIN_BUDDHIST_VAJRA" -> Motif.BUDDHIST;
            case "SARIRA_SHIELD" -> Motif.SHIELD;
            case "ZEN_PULSE" -> Motif.DOMAIN;

            case "RIGHTEOUS_QI", "SCROLL_STRIKE", "CONFUCIAN_RIGHTEOUS_QI" -> Motif.CONFUCIAN;
            case "WORD_SUPPRESS" -> Motif.SEAL;
            case "INK_SEA" -> Motif.DOMAIN;

            case "FIVE_THUNDER" -> Motif.DAO;
            case "PURE_YANG_SWORD" -> Motif.BLADE;
            case "TAOIST_SEAL", "BAGUA_SEAL" -> Motif.SEAL;
            case "CLOUD_WALK" -> Motif.TELEPORT;
            case "IMMORTAL_ROPE" -> Motif.CHAIN;
            case "DAO_NATURE_BREATH" -> Motif.HEAL;

            case "BLOOD_SHADOW_ESCAPE" -> Motif.TELEPORT;
            case "SKY_SUPPORTING_DEMONIC_SKILL", "MYSTIC_SOUL_BONE_CONDENSING_ART", "BLOOD_LUO_BARRIER" -> Motif.SHIELD;
            case "MYSTIC_SOUL_GHOST_FIRE", "YIN_DEMON_SLASH", "UNDERWORLD_FLAME" -> Motif.GHOST;
            case "SOUL_DEVOURING_CLOUD" -> Motif.DOMAIN;
            case "YIN_SOUL_CHAIN" -> Motif.CHAIN;
            case "CORPSE_ARMOR" -> Motif.SHIELD;

            case "SENSE_SCAN", "DIVINE_SENSE_SCAN", "MIND_READ", "SENSE_PRESSURE", "SENSE_NEEDLE",
                    "SOUL_ATTACK_WAVE", "SOUL_CRY_SHOCK" -> Motif.CHANNEL;
            case "SENSE_DOMAIN" -> Motif.DOMAIN;
            case "SENSE_LOCK", "DIVINE_SENSE_LOCK" -> Motif.CHAIN;

            case "MIRROR_PHANTOM", "HUNDRED_ILLUSION", "MIND_CONFUSION", "VEIL_OF_MOON",
                    "INVISIBILITY_BASIC", "ILLUSION_MIST", "INVERSE_STAR_VEIL",
                    "YANYUE_MOON_ILLUSION", "WANHU_NINE_ILLUSION" -> Motif.ILLUSION;
            case "VOID_STEP" -> Motif.TELEPORT;
            case "DREAM_SNARE" -> Motif.CHAIN;
            case "CLONE_IMAGE" -> Motif.SUMMON;
            case "YANYUE_PHANTOM_ARRAY" -> Motif.FORMATION;

            case "SMALL_SWORD_ARRAY", "ILLUSION_FORMATION", "SPIRIT_GATHER_ARRAY", "THUNDER_TRAP_ARRAY",
                    "SEAL_ARRAY", "KILL_SWORD_FORMATION", "DEFENSE_FORMATION", "SEA_LOCK_ARRAY",
                    "STAR_PALACE_PATROL_BEACON", "FORMATION_TRAP_BASIC", "STAR_PALACE_SEAL",
                    "KUNWU_SEAL_STRIKE", "STAR_PALACE_TIDAL_LOCK" -> Motif.FORMATION;

            case "QINGYUAN_SWORD_RAY", "FLYING_SWORD_STRIKE", "GREEN_BAMBOO_SWORD_QI",
                    "BLOOD_SWORD_SLASH", "INVISIBLE_SWORD", "DUAL_SWORD_DANCE" -> Motif.BLADE;
            case "SWORD_SHIELD" -> Motif.SHIELD;
            case "SWORD_ESCAPE" -> Motif.TELEPORT;
            case "THOUSAND_SWORD_ARRAY" -> Motif.RAIN;
            case "SWORD_MERGE" -> Motif.CHANNEL;
            case "SWORD_DOMAIN" -> Motif.DOMAIN;

            case "PRIMORDIAL_MAGNET", "PRIMORDIAL_MAGNET_SPHERE", "FIVE_ELEMENT_FUSION",
                    "FIVE_ELEMENT_FUSION_BURST" -> Motif.DOMAIN;
            case "FLAME_SERPENT_STORM", "TRUE_FIRE_HEAVEN", "LIEYAN_TRUE_FIRE_SECRET" -> Motif.RAIN;
            case "EARTH_MOUNTAIN_PRESS", "XUANTIAN_ICE_PRISON" -> Motif.WALL;
            case "LIFE_FIRE" -> Motif.CHANNEL;
            default -> null;
        };
    }

    private static Motif inferMotif(String type, String blob) {
        Motif typed = switch (type) {
            case "projectile", "beam", "cone" -> Motif.PROJECTILE;
            case "melee", "strike" -> Motif.BLADE;
            case "shield" -> Motif.SHIELD;
            case "domain", "field", "buff_zone" -> Motif.DOMAIN;
            case "movement", "dash", "escape", "teleport_short" -> Motif.TELEPORT;
            case "summon", "summon_field" -> Motif.SUMMON;
            case "wall" -> Motif.WALL;
            case "chain" -> Motif.CHAIN;
            case "scan", "scout", "inspect", "command", "craft_gate" -> Motif.CHANNEL;
            case "heal", "heal_spirit" -> Motif.HEAL;
            case "cleanse" -> Motif.CLEANSE;
            case "talisman_consume" -> Motif.TALISMAN;
            case "trap", "control", "aoe_control" -> containsAny(blob, "seal", "suppress") ? Motif.SEAL : Motif.FORMATION;
            case "aoe", "aoe_dot", "ultimate", "secret_art" -> containsAny(blob, "rain", "storm", "barrage", "volley")
                    ? Motif.RAIN : Motif.DOMAIN;
            default -> null;
        };
        if (typed != null) {
            return typed;
        }
        if (containsAny(blob, "talisman")) return Motif.TALISMAN;
        if (containsAny(blob, "teleport", "escape", "void_step", "dash")) return Motif.TELEPORT;
        if (containsAny(blob, "summon", "avatar", "clone", "puppet_call")) return Motif.SUMMON;
        if (containsAny(blob, "wall", "prison", "cage")) return Motif.WALL;
        if (containsAny(blob, "shield", "armor", "barrier", "guard")) return Motif.SHIELD;
        if (containsAny(blob, "domain", "world", "field")) return Motif.DOMAIN;
        if (containsAny(blob, "chain", "rope", "bind", "snare")) return Motif.CHAIN;
        if (containsAny(blob, "rain", "storm", "barrage", "volley")) return Motif.RAIN;
        if (containsAny(blob, "cleanse", "purify", "detox")) return Motif.CLEANSE;
        if (containsAny(blob, "heal", "recovery", "regeneration")) return Motif.HEAL;
        if (containsAny(blob, "seal", "suppress", "lock")) return Motif.SEAL;
        if (containsAny(blob, "sword", "blade", "slash", "strike")) return Motif.BLADE;
        if (containsAny(blob, "scan", "sense", "channel", "mind_read")) return Motif.CHANNEL;
        if (containsAny(blob, "formation", "array", "trap")) return Motif.FORMATION;
        if (containsAny(blob, "buddha", "vajra", "sarira")) return Motif.BUDDHIST;
        if (containsAny(blob, "confucian", "righteous_qi", "ink_sea")) return Motif.CONFUCIAN;
        if (containsAny(blob, "taoist", "bagua", "five_thunder", "dao_")) return Motif.DAO;
        if (containsAny(blob, "ghost", "underworld", "xuan_yin")) return Motif.GHOST;
        if (containsAny(blob, "illusion", "mirage", "phantom", "dream")) return Motif.ILLUSION;
        return Motif.GENERIC;
    }

    private static Kind inferKind(String type, String target, String blob, Motif motif) {
        if (MOVEMENT_TYPES.contains(type)) return Kind.PATH;
        if ("beam".equals(type)) return Kind.BEAM;
        if ("cone".equals(type)) return Kind.CONE;
        if ("projectile".equals(type)) return Kind.PATH;
        if (motif == Motif.TELEPORT || motif == Motif.CHAIN || motif == Motif.BLADE) return Kind.PATH;
        if (motif == Motif.SHIELD || motif == Motif.HEAL || motif == Motif.CLEANSE) return Kind.AURA;
        if (motif == Motif.WALL || motif == Motif.FORMATION || motif == Motif.DOMAIN) return Kind.FORMATION;
        if (motif == Motif.RAIN || motif == Motif.SUMMON) return Kind.BURST;
        if (motif == Motif.CHANNEL) {
            if (containsAny(blob, "beam", "wave", "ray", "ghost_fire", "life_fire")) return Kind.BEAM;
            return containsAny(type, "scan", "scout", "inspect") ? Kind.SCAN : Kind.AURA;
        }
        if (motif == Motif.SEAL) {
            return "single".equals(target) ? Kind.IMPACT : Kind.FORMATION;
        }
        if ("scan".equals(type) || "scout".equals(type) || "inspect".equals(type)) return Kind.SCAN;
        if (SELF_TYPES.contains(type) || "self".equals(target)) return Kind.AURA;
        return switch (type) {
            case "chain" -> Kind.PATH;
            case "aoe", "aoe_dot", "debuff", "dot", "drain", "soul_attack", "melee", "strike" -> Kind.IMPACT;
            case "aoe_control", "field", "domain", "trap", "control", "wall", "buff_zone" -> Kind.FORMATION;
            case "summon", "summon_field", "ultimate", "secret_art", "talisman_consume" -> Kind.BURST;
            default -> containsAny(blob, "beam", "ray") ? Kind.BEAM : Kind.BURST;
        };
    }

    private static double resolveRange(String authoredRange, String type, String target) {
        String normalized = normalize(authoredRange);
        if (!normalized.isBlank()) {
            try {
                return Math.max(0.0D, Math.min(48.0D, Double.parseDouble(normalized)));
            } catch (NumberFormatException ignored) {
                return switch (normalized) {
                    case "long" -> 32.0D;
                    case "medium" -> 20.0D;
                    case "short" -> 10.0D;
                    case "dash" -> 9.0D;
                    case "touch", "melee" -> 4.0D;
                    default -> defaultRange(type, target);
                };
            }
        }
        return defaultRange(type, target);
    }

    private static double defaultRange(String type, String target) {
        if (SELF_TYPES.contains(type) || "self".equals(target)) return 0.0D;
        if (MOVEMENT_TYPES.contains(type) || "melee".equals(type) || "strike".equals(type)) return 9.0D;
        if ("ultimate".equals(type) || "secret_art".equals(type) || "battlefield".equals(target)) return 24.0D;
        if (containsAny(type, "wall", "buff_zone", "command", "craft_gate", "talisman_consume")) return 16.0D;
        return "area".equals(target) ? 22.0D : 18.0D;
    }

    private static double resolveRadius(String type, String target, Motif motif) {
        double radius;
        if (containsAny(type, "chain", "aoe", "aoe_dot", "aoe_control", "field", "domain", "trap", "control")) {
            radius = "area".equals(target) ? 5.0D : 3.5D;
        } else if (containsAny(type, "ultimate", "secret_art", "buff_zone")) {
            radius = "battlefield".equals(target) || "area".equals(target) ? 6.0D : 4.5D;
        } else if ("wall".equals(type)) {
            radius = 3.0D;
        } else if (containsAny(type, "talisman_consume", "command", "craft_gate")) {
            radius = 3.5D;
        } else if ("summon_field".equals(type)) {
            radius = 4.0D;
        } else {
            radius = "area".equals(target) ? 4.0D : 2.0D;
        }
        radius = switch (motif) {
            case DOMAIN, FORMATION, RAIN -> Math.max(4.0D, radius);
            case WALL -> Math.max(3.0D, radius);
            case SHIELD, HEAL, CLEANSE -> Math.max(2.2D, radius);
            case PROJECTILE, BLADE, CHAIN, TELEPORT -> Math.min(1.6D, radius);
            default -> radius;
        };
        return radius;
    }

    private static int resolveIntensity(String type, Kind kind, boolean secondary) {
        int intensity = switch (kind) {
            case PATH, AURA -> 22;
            case SCAN -> 26;
            case BEAM, CONE, IMPACT -> 28;
            case FORMATION -> 32;
            case BURST -> 30;
            case STATUS, DISSIPATE -> 18;
            case CAST -> 16;
        };
        if ("ultimate".equals(type) || "secret_art".equals(type)) {
            intensity += 8;
        }
        if (secondary) {
            intensity = Math.max(8, (int) Math.round(intensity * 0.58D));
        }
        return intensity;
    }

    private static long seed(ServerLevel level,
                             ServerPlayer player,
                             TechniqueDataManager.TechniqueEntry technique,
                             SkillType skillType,
                             boolean secondary) {
        String id = technique == null ? "" : technique.id();
        String skillName = skillType == null ? "" : skillType.name();
        return level.getGameTime() * 31L
                ^ player.getUUID().getMostSignificantBits()
                ^ ((long) id.hashCode() << 21)
                ^ ((long) skillName.hashCode() << 43)
                ^ (secondary ? 0xBB67AE8584CAA73BL : 0L);
    }

    private static boolean finite(Vec3 value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static Vec3 normalized(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
    }

    private static boolean containsAny(String value, String... tokens) {
        String safe = value == null ? "" : value;
        for (String token : tokens) {
            if (safe.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String... values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(normalized);
        }
        return out.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalizeSkillName(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private record Geometry(Vec3 start, Vec3 end) {}
}
