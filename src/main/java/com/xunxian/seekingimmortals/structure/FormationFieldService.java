package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.block.CatalogFormationCoreBlock;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.sect.SectMissionGenerator;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timed formation field aura while the ring multiblock remains intact.
 * Wave49: persists active fields via FormationFieldSavedData (survive restart).
 * M07: catalog-driven radius/params + public getActiveFieldEffects for M09/M14/M06.
 */
public final class FormationFieldService {
    private static final int DEFAULT_DURATION_TICKS = 20 * 90;
    private static final int TICK_INTERVAL = 20;
    private static final int RING_CHECK_INTERVAL = 20;
    private static final Map<FieldKey, ActiveField> ACTIVE = new ConcurrentHashMap<>();

    private FormationFieldService() {}

    public enum FieldKind {
        SPIRIT_GATHER(2, true),
        DEFENSE(2, false),
        KILL_SWORD(2, false),
        SEAL_DEMON(2, false),
        ILLUSION_MAZE(2, true),
        CATALOG_GENERIC(2, false);

        private final int radius;
        private final boolean spiritGatheringRing;

        FieldKind(int radius, boolean spiritGatheringRing) {
            this.radius = radius;
            this.spiritGatheringRing = spiritGatheringRing;
        }

        public int radius() {
            return radius;
        }

        public boolean usesSpiritGatheringRing() {
            return spiritGatheringRing;
        }
    }

    /**
     * Stable public field effect view for combat / secret-realm / region-event consumers.
     */
    public record FieldEffect(
            String formationId,
            FieldKind kind,
            BlockPos corePos,
            int radius,
            int remainingTicks,
            int auraBonus,
            String effect,
            boolean freeField
    ) {
        public boolean contains(BlockPos pos) {
            if (pos == null || corePos == null) {
                return false;
            }
            int r = Math.max(1, radius);
            int dx = Math.abs(pos.getX() - corePos.getX());
            int dz = Math.abs(pos.getZ() - corePos.getZ());
            int dy = Math.abs(pos.getY() - corePos.getY());
            return dx <= r && dz <= r && dy <= Math.max(2, r);
        }
    }

    public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind) {
        return activate(level, corePos, kind, null);
    }

    /** Wave490: optional deployer for life-skill FORMATION practice + duration scale. */
    public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind, ServerPlayer deployer) {
        return activate(level, corePos, kind, deployer, null);
    }

    /** M07: optional formation catalog id for parameterized radius/effect metadata. */
    public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind, ServerPlayer deployer, String formationId) {
        if (level == null || corePos == null || kind == null) {
            return false;
        }
        FormationFieldCatalog.FieldParams params = resolveParams(formationId, kind);
        String coreBlockId = coreBlockId(level, corePos);
        if (!ringIntact(level, corePos, kind, params.radius(), coreBlockId)) {
            return false;
        }
        int duration = scaledDuration(deployer, params.durationTicks() > 0 ? params.durationTicks() : DEFAULT_DURATION_TICKS);
        FieldKey key = FieldKey.of(level, corePos);
        ActiveField field = new ActiveField(
                level.dimension().location().toString(),
                corePos.immutable(),
                kind,
                duration,
                params.id(),
                params.radius(),
                params.auraBonus(),
                params.effect(),
                coreBlockId);
        ACTIVE.put(key, field);
        persistField(level, field);
        grantFormationPractice(deployer, true);
        return true;
    }

    /**
     * Handheld formation deploy: free field around player without requiring a ring core.
     */
    public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, int durationTicks) {
        return activateFreeField(level, center, kind, durationTicks, null, null);
    }

    public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, int durationTicks, ServerPlayer deployer) {
        return activateFreeField(level, center, kind, durationTicks, deployer, null);
    }

    public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, int durationTicks, ServerPlayer deployer, String formationId) {
        if (level == null || center == null || kind == null) {
            return false;
        }
        FormationFieldCatalog.FieldParams params = resolveParams(formationId, kind);
        int duration = scaledDuration(deployer, Math.max(20 * 20, durationTicks > 0 ? durationTicks : params.durationTicks()));
        FieldKey key = FieldKey.of(level, center);
        ActiveField field = new ActiveField(
                level.dimension().location().toString(),
                center.immutable(),
                kind,
                duration,
                params.id(),
                params.radius(),
                params.auraBonus(),
                params.effect(),
                "");
        field.freeField = true;
        ACTIVE.put(key, field);
        persistField(level, field);
        grantFormationPractice(deployer, false);
        return true;
    }

    /**
     * M07 stable API: active field effects covering {@code pos} in the given level.
     * Server-authoritative; clients should only receive display sync from consumers.
     */
    public static List<FieldEffect> getActiveFieldEffects(Level level, BlockPos pos) {
        if (level == null || pos == null || ACTIVE.isEmpty()) {
            return List.of();
        }
        String dim = level.dimension().location().toString();
        List<FieldEffect> out = new ArrayList<>();
        for (ActiveField field : ACTIVE.values()) {
            if (!dim.equals(field.dimensionId)) {
                continue;
            }
            FieldEffect effect = field.asEffect();
            if (effect.contains(pos)) {
                out.add(effect);
            }
        }
        return List.copyOf(out);
    }

    /** Convenience overload used by brief signature {@code getActiveFieldEffects(pos)} when level is known. */
    public static List<FieldEffect> getActiveFieldEffects(ServerLevel level, BlockPos pos) {
        return getActiveFieldEffects((Level) level, pos);
    }

    public static int spiritGatherAuraBonus(Level level, BlockPos pos) {
        int bonus = 0;
        for (FieldEffect effect : getActiveFieldEffects(level, pos)) {
            if (effect.kind() == FieldKind.SPIRIT_GATHER) {
                bonus += Math.max(0, effect.auraBonus());
            }
        }
        return bonus;
    }

    private static FormationFieldCatalog.FieldParams resolveParams(String formationId, FieldKind kind) {
        if (formationId != null && !formationId.isBlank()) {
            var found = FormationFieldCatalog.builtin().find(formationId);
            if (found.isPresent()) {
                return found.get();
            }
        }
        // Legacy callers do not provide an id. Keep their enum radius/ring
        // contract instead of silently selecting the first catalog entry.
        return new FormationFieldCatalog.FieldParams(
                kind.name().toLowerCase(Locale.ROOT),
                kind.name(),
                kind,
                kind.radius(),
                DEFAULT_DURATION_TICKS,
                kind == FieldKind.SPIRIT_GATHER ? 50 : 0,
                "",
                "",
                kind.usesSpiritGatheringRing());
    }

    private static int scaledDuration(ServerPlayer deployer, int baseTicks) {
        if (deployer == null) {
            return Math.max(1, baseTicks);
        }
        int lv = com.xunxian.seekingimmortals.skill.LifeSkillService.level(
                deployer, com.xunxian.seekingimmortals.skill.SkillType.FORMATION);
        // +5% duration per level, cap +40%.
        double scale = 1.0D + Math.min(0.40D, lv * 0.05D);
        return Math.max(1, (int) Math.round(baseTicks * scale));
    }

    private static void grantFormationPractice(ServerPlayer deployer, boolean coreDeploy) {
        if (deployer == null) {
            return;
        }
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(
                deployer,
                com.xunxian.seekingimmortals.skill.SkillType.FORMATION,
                coreDeploy ? 20 : 14,
                coreDeploy ? 8 : 5);
        // Special skill sense gains light practice when formations are read/deployed.
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(
                deployer,
                com.xunxian.seekingimmortals.skill.SkillType.FORMATION_SENSE,
                8,
                3);
        SectMissionGenerator.onFormationDeployed(deployer);
    }

    public static void loadFromSavedData(ServerLevel level) {
        if (level == null) {
            return;
        }
        String dim = level.dimension().location().toString();
        for (FormationFieldSavedData.StoredField stored : FormationFieldSavedData.get(level).fields()) {
            if (!dim.equals(stored.dimensionId())) {
                continue;
            }
            FieldKind kind;
            try {
                kind = FieldKind.valueOf(stored.kind());
            } catch (RuntimeException ignored) {
                kind = FieldKind.CATALOG_GENERIC;
            }
            String formationId = stored.formationId() == null || stored.formationId().isBlank()
                    ? kind.name().toLowerCase(Locale.ROOT)
                    : stored.formationId();
            FormationFieldCatalog.FieldParams params = resolveParams(formationId, kind);
            int radius = stored.radius() > 0 ? stored.radius() : params.radius();
            String coreBlockId = stored.coreBlockId() == null || stored.coreBlockId().isBlank()
                    ? coreBlockId(level, stored.corePos())
                    : stored.coreBlockId();
            if (!stored.freeField() && !ringIntact(level, stored.corePos(), kind, radius, coreBlockId)) {
                FormationFieldSavedData.get(level).remove(dim, stored.corePos());
                continue;
            }
            FieldKey key = new FieldKey(dim, stored.corePos().asLong());
            ActiveField field = new ActiveField(
                    dim,
                    stored.corePos(),
                    kind,
                    Math.max(1, stored.remainingTicks()),
                    formationId,
                    radius,
                    stored.auraBonus() >= 0 ? stored.auraBonus() : params.auraBonus(),
                    stored.effect() == null || stored.effect().isBlank() ? params.effect() : stored.effect(),
                    coreBlockId);
            field.freeField = stored.freeField();
            ACTIVE.put(key, field);
        }
    }

    public static void serverTick(ServerLevel level) {
        if (level == null || ACTIVE.isEmpty()) {
            return;
        }
        String dim = level.dimension().location().toString();
        Iterator<Map.Entry<FieldKey, ActiveField>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FieldKey, ActiveField> entry = it.next();
            ActiveField field = entry.getValue();
            if (!dim.equals(field.dimensionId)) {
                continue;
            }
            field.remainingTicks -= 1;
            // M07: large/any ring integrity checked on interval (dirty-friendly), not every tick.
            boolean ringBroken = false;
            if (!field.freeField && field.remainingTicks % RING_CHECK_INTERVAL == 0) {
                ringBroken = !ringIntact(level, field.corePos, field.kind, field.radius, field.coreBlockId);
            }
            if (field.remainingTicks <= 0 || ringBroken) {
                it.remove();
                FormationFieldSavedData.get(level).remove(field.dimensionId, field.corePos);
                continue;
            }
            if (field.remainingTicks % TICK_INTERVAL == 0) {
                applyFieldPulse(level, field);
                persistField(level, field);
            }
        }
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    private static void persistField(ServerLevel level, ActiveField field) {
        FormationFieldSavedData.get(level).upsert(
                field.dimensionId,
                field.corePos,
                field.kind.name(),
                field.remainingTicks,
                field.formationId,
                field.radius,
                field.auraBonus,
                field.effect,
                field.freeField,
                field.coreBlockId);
    }

    private static boolean ringIntact(Level level, BlockPos corePos, FieldKind kind) {
        return ringIntact(level, corePos, kind, kind.radius());
    }

    private static boolean ringIntact(Level level, BlockPos corePos, FieldKind kind, int radius) {
        return ringIntact(level, corePos, kind, radius, coreBlockId(level, corePos));
    }

    private static boolean ringIntact(Level level, BlockPos corePos, FieldKind kind, int radius,
                                      String expectedCoreBlockId) {
        Block coreBlock = level.getBlockState(corePos).getBlock();
        if (!isFormationCoreBlock(coreBlock)
                || expectedCoreBlockId == null
                || expectedCoreBlockId.isBlank()
                || !expectedCoreBlockId.equals(coreBlockId(level, corePos))) {
            return false;
        }
        if (coreBlock == ModBlocks.KILL_SWORD_FORMATION_CORE.get()) {
            return ArrayHubStructure.validateKillHub(level, corePos).complete();
        }
        if (coreBlock == ModBlocks.ILLUSION_MAZE_FORMATION_CORE.get()) {
            return ArrayHubStructure.validateIllusionHub(level, corePos).complete();
        }
        if (coreBlock == ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get()) {
            boolean standard = SpiritGatheringFormationStructure.validate(
                    level, corePos, ModBlocks.SPIRIT_GATHERING_ARRAY.get()).complete();
            boolean advanced = AdvancedSpiritGatheringArrayStructure.validate(
                    level,
                    corePos,
                    ModBlocks.SPIRIT_ORE.get(),
                    ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get()).complete();
            return standard || advanced;
        }
        Block ringBlock = kind.usesSpiritGatheringRing()
                ? ModBlocks.SPIRIT_GATHERING_ARRAY.get()
                : ModBlocks.SPIRIT_ORE.get();
        int r = Math.max(1, radius);
        return RingFormationStructure.validate(level, corePos, ringBlock, r).complete();
    }

    private static boolean isFormationCoreBlock(Block block) {
        return block == ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get()
                || block == ModBlocks.DEFENSE_FORMATION_CORE.get()
                || block == ModBlocks.SEAL_DEMON_FORMATION_CORE.get()
                || block == ModBlocks.KILL_SWORD_FORMATION_CORE.get()
                || block == ModBlocks.ILLUSION_MAZE_FORMATION_CORE.get()
                || block instanceof CatalogFormationCoreBlock;
    }

    private static String coreBlockId(Level level, BlockPos corePos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(corePos).getBlock()).toString();
    }

    private static void applyFieldPulse(ServerLevel level, ActiveField field) {
        double radius = field.radius + 1.5D;
        AABB box = new AABB(field.corePos).inflate(radius, 2.0D, radius);
        switch (field.kind) {
            case SPIRIT_GATHER -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0, true, true, true));
                }
                level.sendParticles(ParticleTypes.END_ROD,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        8, 0.6D, 0.3D, 0.6D, 0.01D);
            }
            case DEFENSE -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true, true));
                }
                level.sendParticles(ParticleTypes.CRIT,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        6, 0.5D, 0.2D, 0.5D, 0.02D);
            }
            case KILL_SWORD -> {
                for (Monster monster : level.getEntitiesOfClass(Monster.class, box)) {
                    monster.hurt(level.damageSources().magic(), 2.0F);
                    monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
                }
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, true, true));
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        4, 0.5D, 0.2D, 0.5D, 0.0D);
            }
            case SEAL_DEMON -> {
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
                    if (living instanceof ServerPlayer) {
                        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true, true));
                        continue;
                    }
                    if (living instanceof Monster) {
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true, true));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, true, true));
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, true, true));
                    }
                }
                level.sendParticles(ParticleTypes.SOUL,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        6, 0.5D, 0.25D, 0.5D, 0.01D);
            }
            case ILLUSION_MAZE -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, true, true, true));
                }
                for (Monster monster : level.getEntitiesOfClass(Monster.class, box)) {
                    monster.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, true, true));
                    monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
                }
                level.sendParticles(ParticleTypes.CLOUD,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        8, 0.7D, 0.3D, 0.7D, 0.01D);
            }
            case CATALOG_GENERIC -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, true, true));
                }
            }
        }
        emitFormationVfx(level, field);
    }

    private static void emitFormationVfx(ServerLevel level, ActiveField field) {
        String semantic = field.formationId + " " + field.effect;
        TechniqueVfxPalette.Family family = TechniqueVfxPalette.familyOf(semantic);
        if (family == TechniqueVfxPalette.Family.NEUTRAL) {
            family = switch (field.kind) {
                case SPIRIT_GATHER -> TechniqueVfxPalette.Family.SOUL;
                case DEFENSE -> TechniqueVfxPalette.Family.EARTH;
                case KILL_SWORD -> TechniqueVfxPalette.Family.METAL;
                case SEAL_DEMON -> TechniqueVfxPalette.Family.BLOOD;
                case ILLUSION_MAZE -> TechniqueVfxPalette.Family.ILLUSION;
                case CATALOG_GENERIC -> TechniqueVfxPalette.Family.NEUTRAL;
            };
        }
        Vec3 center = Vec3.atCenterOf(field.corePos).add(0.0D, -0.32D, 0.0D);
        long seed = field.corePos.asLong()
                ^ ((long) field.formationId.hashCode() << 19)
                ^ level.getGameTime() / TICK_INTERVAL;
        TechniqueVfxPacket.send(
                level,
                TechniqueVfxPacket.Kind.FORMATION,
                family,
                center,
                center,
                field.radius,
                Math.min(72, 28 + field.radius * 2),
                seed);
    }

    private record FieldKey(String dimensionId, long packedPos) {
        static FieldKey of(ServerLevel level, BlockPos pos) {
            return new FieldKey(level.dimension().location().toString(), pos.asLong());
        }
    }

    private static final class ActiveField {
        private final String dimensionId;
        private final BlockPos corePos;
        private final FieldKind kind;
        private int remainingTicks;
        private boolean freeField;
        private final String formationId;
        private final int radius;
        private final int auraBonus;
        private final String effect;
        private final String coreBlockId;

        private ActiveField(String dimensionId, BlockPos corePos, FieldKind kind, int remainingTicks,
                            String formationId, int radius, int auraBonus, String effect, String coreBlockId) {
            this.dimensionId = dimensionId;
            this.corePos = corePos;
            this.kind = kind;
            this.remainingTicks = remainingTicks;
            this.formationId = formationId == null ? kind.name().toLowerCase(Locale.ROOT) : formationId;
            this.radius = Math.max(1, radius);
            this.auraBonus = Math.max(0, auraBonus);
            this.effect = effect == null ? "" : effect;
            this.coreBlockId = coreBlockId == null ? "" : coreBlockId;
        }

        private FieldEffect asEffect() {
            return new FieldEffect(formationId, kind, corePos, radius, remainingTicks, auraBonus, effect, freeField);
        }
    }
}
