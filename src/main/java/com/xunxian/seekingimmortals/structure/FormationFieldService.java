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
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
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
    /** Cosmetic target fan-out is deliberately bounded per server pulse. */
    private static final int MAX_PULSE_TARGET_VFX = 8;
    /** Shared custom-packet budget for every formation lifecycle in one dimension tick. */
    private static final int MAX_VFX_PACKETS_PER_DIMENSION_TICK = 48;
    /** Eight ticks of burst absorption without allowing an unbounded visual backlog. */
    private static final int MAX_PENDING_VFX_PER_DIMENSION = MAX_VFX_PACKETS_PER_DIMENSION_TICK * 8;
    private static final Map<FieldKey, ActiveField> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> PULSE_VFX_CURSOR = new ConcurrentHashMap<>();
    private static final Map<String, VfxBudgetState> VFX_BUDGETS = new ConcurrentHashMap<>();

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
        ActiveField replaced = ACTIVE.put(key, field);
        if (replaced != null) {
            // A new field at the same core supersedes the old visual state.
            emitDissipateVfx(level, replaced, false);
        }
        persistField(level, field);
        grantFormationPractice(deployer, true);
        emitActivationVfx(level, field);
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
        ActiveField replaced = ACTIVE.put(key, field);
        if (replaced != null) {
            emitDissipateVfx(level, replaced, false);
        }
        persistField(level, field);
        grantFormationPractice(deployer, false);
        emitActivationVfx(level, field);
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
            emitStatusVfx(level, field);
        }
    }

    public static void serverTick(ServerLevel level) {
        if (level == null) {
            return;
        }
        flushPendingVfx(level);
        if (ACTIVE.isEmpty()) {
            return;
        }
        String dim = level.dimension().location().toString();
        List<PulseVisual> pulseVisuals = new ArrayList<>();
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
                emitDissipateVfx(level, field, ringBroken);
                it.remove();
                FormationFieldSavedData.get(level).remove(field.dimensionId, field.corePos);
                continue;
            }
            if (field.remainingTicks % TICK_INTERVAL == 0) {
                pulseVisuals.add(applyFieldPulse(level, field));
                persistField(level, field);
            }
        }
        emitPulseVisuals(level, dim, pulseVisuals);
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static void clearAll() {
        ACTIVE.clear();
        PULSE_VFX_CURSOR.clear();
        VFX_BUDGETS.clear();
    }

    public static void unload(ServerLevel level) {
        if (level == null) {
            return;
        }
        String dimensionId = level.dimension().location().toString();
        ACTIVE.keySet().removeIf(key -> dimensionId.equals(key.dimensionId));
        PULSE_VFX_CURSOR.remove(dimensionId);
        VFX_BUDGETS.remove(dimensionId);
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

    private static PulseVisual applyFieldPulse(ServerLevel level, ActiveField field) {
        double radius = effectRadiusFor(field.radius);
        AABB box = AABB.ofSize(Vec3.atCenterOf(field.corePos), radius * 2.0D, 5.0D, radius * 2.0D);
        BoundedPulseTargetSampler targets = new BoundedPulseTargetSampler(
                MAX_PULSE_TARGET_VFX,
                vfxSeed(level, field, TechniqueVfxPacket.Kind.STATUS) ^ 0x6a09e667f3bcc909L);
        switch (field.kind) {
            case SPIRIT_GATHER -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    boolean changed = player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, true, true));
                    changed |= player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0, true, true, true));
                    targets.consider(player, TechniqueVfxPacket.Kind.STATUS, changed);
                }
                level.sendParticles(ParticleTypes.END_ROD,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        8, 0.6D, 0.3D, 0.6D, 0.01D);
            }
            case DEFENSE -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    boolean changed = player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true, true));
                    targets.consider(player, TechniqueVfxPacket.Kind.STATUS, changed);
                }
                level.sendParticles(ParticleTypes.CRIT,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        6, 0.5D, 0.2D, 0.5D, 0.02D);
            }
            case KILL_SWORD -> {
                for (Monster monster : level.getEntitiesOfClass(Monster.class, box)) {
                    boolean changed = monster.hurt(level.damageSources().magic(), 2.0F);
                    changed |= monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
                    targets.consider(monster, TechniqueVfxPacket.Kind.IMPACT, changed);
                }
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    boolean changed = player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, true, true));
                    targets.consider(player, TechniqueVfxPacket.Kind.STATUS, changed);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        4, 0.5D, 0.2D, 0.5D, 0.0D);
            }
            case SEAL_DEMON -> {
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
                    if (living instanceof ServerPlayer) {
                        boolean changed = living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true, true));
                        targets.consider(living, TechniqueVfxPacket.Kind.STATUS, changed);
                        continue;
                    }
                    if (living instanceof Monster) {
                        boolean changed = living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true, true));
                        changed |= living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, true, true));
                        changed |= living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, true, true));
                        targets.consider(living, TechniqueVfxPacket.Kind.IMPACT, changed);
                    }
                }
                level.sendParticles(ParticleTypes.SOUL,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        6, 0.5D, 0.25D, 0.5D, 0.01D);
            }
            case ILLUSION_MAZE -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    boolean changed = player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, true, true));
                    changed |= player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, true, true, true));
                    targets.consider(player, TechniqueVfxPacket.Kind.STATUS, changed);
                }
                for (Monster monster : level.getEntitiesOfClass(Monster.class, box)) {
                    boolean changed = monster.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, true, true));
                    changed |= monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, true, true));
                    targets.consider(monster, TechniqueVfxPacket.Kind.IMPACT, changed);
                }
                level.sendParticles(ParticleTypes.CLOUD,
                        field.corePos.getX() + 0.5D, field.corePos.getY() + 1.0D, field.corePos.getZ() + 0.5D,
                        8, 0.7D, 0.3D, 0.7D, 0.01D);
            }
            case CATALOG_GENERIC -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    boolean changed = player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, true, true));
                    targets.consider(player, TechniqueVfxPacket.Kind.STATUS, changed);
                }
            }
        }
        return new PulseVisual(field, targets.snapshot());
    }

    private static void emitPulseVisuals(ServerLevel level, String dimensionId,
                                         List<PulseVisual> visuals) {
        if (visuals.isEmpty()) {
            return;
        }
        int size = visuals.size();
        int start = Math.floorMod(PULSE_VFX_CURSOR.getOrDefault(dimensionId, 0), size);
        int remaining = MAX_VFX_PACKETS_PER_DIMENSION_TICK;
        int served = 0;
        for (int offset = 0; offset < size && remaining > 0; offset++) {
            PulseVisual visual = visuals.get((start + offset) % size);
            int remainingFields = size - offset;
            int fairShare = Math.max(1, remaining / Math.max(1, remainingFields));
            int quota = Math.min(1 + MAX_PULSE_TARGET_VFX, fairShare);
            emitFormationVfx(level, visual.field());
            remaining--;
            int targetQuota = Math.min(visual.targets().size(), Math.max(0, quota - 1));
            for (int i = 0; i < targetQuota && remaining > 0; i++) {
                PulseTarget target = visual.targets().get(i);
                emitPulseTargetVfx(level, visual.field(), target.entity(), target.kind());
                remaining--;
            }
            served++;
        }
        PULSE_VFX_CURSOR.put(dimensionId, nextPulseCursor(start, served, size));
    }

    static int nextPulseCursor(int start, int served, int size) {
        if (size <= 0) {
            return 0;
        }
        int advance = served >= size ? 1 : Math.max(1, served);
        return Math.floorMod(start + advance, size);
    }

    private static void emitPulseTargetVfx(ServerLevel level, ActiveField field,
                                           LivingEntity target, TechniqueVfxPacket.Kind kind) {
        Vec3 position = target.position().add(0.0D, Math.max(0.15D, target.getBbHeight() * 0.48D), 0.0D);
        submitVfx(level, new VfxEmission(
                kind,
                familyFor(field),
                motifFor(field.formationId, field.effect, field.kind),
                position,
                position,
                Math.max(0.35D, Math.min(1.8D, target.getBbWidth() * 0.8D)),
                kind == TechniqueVfxPacket.Kind.IMPACT ? 24 : 18,
                vfxSeed(level, field, kind) ^ target.getId() * 0x9e3779b9L), false);
    }

    private record PulseTarget(LivingEntity entity, TechniqueVfxPacket.Kind kind) {}

    private record PulseVisual(ActiveField field, List<PulseTarget> targets) {}

    /** Equal-probability reservoir sampling keeps target intent storage capped at the visual fan-out. */
    private static final class BoundedPulseTargetSampler {
        private final int capacity;
        private final RandomSource random;
        private final List<PulseTarget> samples;
        private int eligibleCount;

        private BoundedPulseTargetSampler(int capacity, long seed) {
            this.capacity = Math.max(0, capacity);
            this.random = RandomSource.create(seed);
            this.samples = new ArrayList<>(this.capacity);
        }

        private void consider(LivingEntity entity, TechniqueVfxPacket.Kind kind, boolean stateChanged) {
            if (!stateChanged || entity == null || kind == null || capacity == 0) {
                return;
            }
            for (PulseTarget sample : samples) {
                if (sample.entity().getId() == entity.getId()) {
                    return;
                }
            }
            eligibleCount++;
            PulseTarget candidate = new PulseTarget(entity, kind);
            if (samples.size() < capacity) {
                samples.add(candidate);
                return;
            }
            int slot = random.nextInt(eligibleCount);
            if (slot < capacity) {
                samples.set(slot, candidate);
            }
        }

        private List<PulseTarget> snapshot() {
            return List.copyOf(samples);
        }
    }

    private static void submitVfx(ServerLevel level, VfxEmission emission, boolean lifecycle) {
        if (level == null || emission == null) {
            return;
        }
        String dimensionId = level.dimension().location().toString();
        VfxBudgetState budget = VFX_BUDGETS.computeIfAbsent(dimensionId, ignored -> new VfxBudgetState());
        List<VfxEmission> ready = new ArrayList<>(MAX_VFX_PACKETS_PER_DIMENSION_TICK);
        synchronized (budget) {
            beginVfxTick(level, budget);
            drainPendingVfx(budget, ready);
            if (budget.sentThisTick < MAX_VFX_PACKETS_PER_DIMENSION_TICK) {
                budget.sentThisTick++;
                ready.add(emission);
            } else {
                budget.offer(emission, lifecycle);
            }
        }
        sendReadyVfx(level, ready);
    }

    private static void flushPendingVfx(ServerLevel level) {
        String dimensionId = level.dimension().location().toString();
        VfxBudgetState budget = VFX_BUDGETS.get(dimensionId);
        if (budget == null) {
            return;
        }
        List<VfxEmission> ready = new ArrayList<>(MAX_VFX_PACKETS_PER_DIMENSION_TICK);
        synchronized (budget) {
            beginVfxTick(level, budget);
            drainPendingVfx(budget, ready);
        }
        sendReadyVfx(level, ready);
    }

    private static void beginVfxTick(ServerLevel level, VfxBudgetState budget) {
        long gameTime = level.getGameTime();
        if (budget.gameTime != gameTime) {
            budget.gameTime = gameTime;
            budget.sentThisTick = 0;
        }
    }

    private static void drainPendingVfx(VfxBudgetState budget, List<VfxEmission> ready) {
        while (budget.sentThisTick < MAX_VFX_PACKETS_PER_DIMENSION_TICK) {
            VfxEmission emission = budget.poll();
            if (emission == null) {
                return;
            }
            budget.sentThisTick++;
            ready.add(emission);
        }
    }

    private static void sendReadyVfx(ServerLevel level, List<VfxEmission> ready) {
        for (VfxEmission emission : ready) {
            TechniqueVfxPacket.send(
                    level,
                    emission.kind(),
                    emission.family(),
                    emission.motif(),
                    emission.start(),
                    emission.end(),
                    emission.radius(),
                    emission.intensity(),
                    emission.seed());
        }
    }

    private static void emitActivationVfx(ServerLevel level, ActiveField field) {
        TechniqueVfxPalette.Family family = familyFor(field);
        TechniqueVfxPacket.Motif motif = motifFor(field.formationId, field.effect, field.kind);
        Vec3 center = vfxCenter(field);
        double effectRadius = effectRadiusFor(field.radius);
        long seed = vfxSeed(level, field, TechniqueVfxPacket.Kind.FORMATION);
        submitVfx(level, new VfxEmission(
                TechniqueVfxPacket.Kind.FORMATION,
                family,
                motif,
                center,
                center,
                effectRadius,
                Math.min(96, 72 + field.radius * 4),
                seed), true);
        Vec3 castStart = center.add(0.0D, 0.12D, 0.0D);
        Vec3 castEnd = center.add(0.0D, 1.25D + effectRadius * 0.18D, 0.0D);
        submitVfx(level, new VfxEmission(
                TechniqueVfxPacket.Kind.CAST,
                family,
                motif,
                castStart,
                castEnd,
                Math.max(0.65D, effectRadius * 0.35D),
                Math.min(72, 44 + field.radius * 3),
                seed ^ 0x4f1bbcdcL), true);
    }

    private static void emitStatusVfx(ServerLevel level, ActiveField field) {
        emitFormationVfx(level, field, TechniqueVfxPacket.Kind.STATUS,
                motifFor(field.formationId, field.effect, field.kind), Math.min(24, 8 + field.radius * 2), true);
    }

    private static void emitDissipateVfx(ServerLevel level, ActiveField field, boolean ringBroken) {
        emitFormationVfx(level, field, TechniqueVfxPacket.Kind.DISSIPATE,
                motifFor(field.formationId, field.effect, field.kind),
                ringBroken ? Math.min(96, 76 + field.radius * 4) : Math.min(30, 20 + field.radius * 2), true);
    }

    private static void emitFormationVfx(ServerLevel level, ActiveField field,
                                         TechniqueVfxPacket.Kind kind,
                                         TechniqueVfxPacket.Motif motif,
                                         int intensity,
                                         boolean lifecycle) {
        TechniqueVfxPalette.Family family = familyFor(field);
        Vec3 center = vfxCenter(field);
        long seed = vfxSeed(level, field, kind);
        submitVfx(level, new VfxEmission(
                kind,
                family,
                motif,
                center,
                center,
                effectRadiusFor(field.radius),
                intensity,
                seed), lifecycle);
    }

    private static void emitFormationVfx(ServerLevel level, ActiveField field) {
        emitFormationVfx(level, field, TechniqueVfxPacket.Kind.FORMATION,
                motifFor(field.formationId, field.effect, field.kind), Math.min(72, 28 + field.radius * 2), false);
    }

    private static TechniqueVfxPalette.Family familyFor(ActiveField field) {
        return familyFor(field.formationId, field.effect, field.kind);
    }

    static TechniqueVfxPalette.Family familyFor(String formationId, String effect, FieldKind kind) {
        String id = normalizeSemanticId(formationId);
        TechniqueVfxPalette.Family explicit = switch (id) {
            case "teleport_array", "teleport_array_long_range", "juling_gu_chuan",
                    "xinggong_teleport", "miaoyin_teleport", "jiezi_boundary",
                    "wuxing_yankong", "guwu_shachang", "xutian_neijin" -> TechniqueVfxPalette.Family.VOID;
            case "huangsha_zhen", "five_elements_mountain", "sect_mountain_guard",
                    "defense_wall", "barrier_sect_protection" -> TechniqueVfxPalette.Family.EARTH;
            case "thunder_tribulation_array", "lei_zhen_double", "huju_zhen_generic" ->
                    TechniqueVfxPalette.Family.THUNDER;
            case "jin_guang_fang", "vajra_prison_array", "beijiyuan_guang", "dayan_fenshen_monitor" ->
                    TechniqueVfxPalette.Family.LIGHT;
            case "xue_luo_zhao", "blood_forbidden_gate", "seal_demon_array",
                    "demon_seal_pillar_array", "kunwu_seals" -> TechniqueVfxPalette.Family.BLOOD;
            case "chunli_jianzhen", "xian_teng_nurture", "chongqun_yuzhen" -> TechniqueVfxPalette.Family.WOOD;
            case "kill_sword", "sword_array_bagua", "geng_jian_zhen", "qingpan_jianzhen",
                    "juling_kill_combo" -> TechniqueVfxPalette.Family.METAL;
            case "nine_dragon_flame_barrier", "liandao_huoyan" -> TechniqueVfxPalette.Family.FIRE;
            case "mulan_wind_ride_array" -> TechniqueVfxPalette.Family.WIND;
            case "wubianhai_fengyin" -> TechniqueVfxPalette.Family.WATER;
            case "illusion_maze", "illusion_maze_array", "inverted_five_elements_array",
                    "wuxing_huanying", "diandao_wuxing", "huanmiao_tianxiang" ->
                    TechniqueVfxPalette.Family.ILLUSION;
            case "yin_yang_ku_ban", "yin_ming_natural" -> TechniqueVfxPalette.Family.DARK;
            case "spirit_gather", "spirit_gathering_minor", "spirit_gathering_array", "juling_zhen",
                    "juling_island_guard", "ju_ling_zhu_boost", "yangsheng_muyu",
                    "gui_luo_fan_zhen", "mulan_totem_field" -> TechniqueVfxPalette.Family.SOUL;
            case "liangyi_weichen" -> TechniqueVfxPalette.Family.VOID;
            default -> TechniqueVfxPalette.Family.NEUTRAL;
        };
        if (explicit != TechniqueVfxPalette.Family.NEUTRAL) {
            return explicit;
        }
        TechniqueVfxPalette.Family semanticFamily = TechniqueVfxPalette.familyOf(effect);
        if (semanticFamily != TechniqueVfxPalette.Family.NEUTRAL) {
            return semanticFamily;
        }
        return switch (kind == null ? FieldKind.CATALOG_GENERIC : kind) {
            case SPIRIT_GATHER -> TechniqueVfxPalette.Family.SOUL;
            case DEFENSE -> TechniqueVfxPalette.Family.EARTH;
            case KILL_SWORD -> TechniqueVfxPalette.Family.METAL;
            case SEAL_DEMON -> TechniqueVfxPalette.Family.BLOOD;
            case ILLUSION_MAZE -> TechniqueVfxPalette.Family.ILLUSION;
            case CATALOG_GENERIC -> TechniqueVfxPalette.Family.NEUTRAL;
        };
    }

    static TechniqueVfxPacket.Motif motifFor(String formationId, String effect, FieldKind kind) {
        String id = normalizeSemanticId(formationId);
        TechniqueVfxPacket.Motif explicit = switch (id) {
            case "teleport_array", "teleport_array_long_range", "juling_gu_chuan",
                    "xinggong_teleport", "miaoyin_teleport", "blood_forbidden_gate" ->
                    TechniqueVfxPacket.Motif.TELEPORT;
            case "kill_sword", "sword_array_bagua", "geng_jian_zhen", "chunli_jianzhen",
                    "qingpan_jianzhen", "juling_kill_combo", "beijiyuan_guang" ->
                    TechniqueVfxPacket.Motif.BLADE;
            case "defense_wall", "barrier_sect_protection", "nine_dragon_flame_barrier",
                    "jin_guang_fang", "xue_luo_zhao", "juling_island_guard", "sect_mountain_guard",
                    "huju_zhen_generic" -> TechniqueVfxPacket.Motif.SHIELD;
            case "seal_demon_array", "demon_seal_pillar_array", "vajra_prison_array",
                    "yin_yang_ku_ban", "kunwu_seals", "wubianhai_fengyin" ->
                    TechniqueVfxPacket.Motif.SEAL;
            case "illusion_maze", "illusion_maze_array", "inverted_five_elements_array",
                    "wuxing_huanying", "diandao_wuxing", "huangsha_zhen", "huanmiao_tianxiang" ->
                    TechniqueVfxPacket.Motif.ILLUSION;
            case "thunder_tribulation_array", "lei_zhen_double" -> TechniqueVfxPacket.Motif.RAIN;
            case "spirit_gather", "spirit_gathering_minor", "spirit_gathering_array", "juling_zhen",
                    "ju_ling_zhu_boost", "yangsheng_muyu", "xian_teng_nurture" ->
                    TechniqueVfxPacket.Motif.HEAL;
            case "chongqun_yuzhen", "gui_luo_fan_zhen", "mulan_totem_field" ->
                    TechniqueVfxPacket.Motif.SUMMON;
            case "liandao_huoyan", "dayan_fenshen_monitor" -> TechniqueVfxPacket.Motif.CHANNEL;
            case "five_elements_mountain", "liangyi_weichen" -> TechniqueVfxPacket.Motif.DAO;
            case "jiezi_boundary", "wuxing_yankong", "guwu_shachang", "xutian_neijin",
                    "yin_ming_natural" -> TechniqueVfxPacket.Motif.DOMAIN;
            default -> TechniqueVfxPacket.Motif.GENERIC;
        };
        if (explicit != TechniqueVfxPacket.Motif.GENERIC) {
            return explicit;
        }
        String semantic = (effect == null ? "" : effect).toLowerCase(Locale.ROOT);
        if (containsSemantic(semantic, "teleport", "travel", "传送", "跨域")) return TechniqueVfxPacket.Motif.TELEPORT;
        if (containsSemantic(semantic, "sword", "blade", "剑", "绞杀")) return TechniqueVfxPacket.Motif.BLADE;
        if (containsSemantic(semantic, "barrier", "defense", "护", "抵挡", "隔绝")) return TechniqueVfxPacket.Motif.SHIELD;
        if (containsSemantic(semantic, "seal", "prison", "封", "镇压", "困锁")) return TechniqueVfxPacket.Motif.SEAL;
        if (containsSemantic(semantic, "illusion", "confuse", "hide", "迷", "幻")) return TechniqueVfxPacket.Motif.ILLUSION;
        if (containsSemantic(semantic, "heal", "recover", "cultivation", "恢复", "修炼", "养")) return TechniqueVfxPacket.Motif.HEAL;
        return switch (kind == null ? FieldKind.CATALOG_GENERIC : kind) {
            case SPIRIT_GATHER -> TechniqueVfxPacket.Motif.HEAL;
            case DEFENSE -> TechniqueVfxPacket.Motif.SHIELD;
            case KILL_SWORD -> TechniqueVfxPacket.Motif.BLADE;
            case SEAL_DEMON -> TechniqueVfxPacket.Motif.SEAL;
            case ILLUSION_MAZE -> TechniqueVfxPacket.Motif.ILLUSION;
            case CATALOG_GENERIC -> TechniqueVfxPacket.Motif.FORMATION;
        };
    }

    private static String normalizeSemanticId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsSemantic(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    static double effectRadiusFor(int gameplayRadius) {
        // The pulse AABB is centered on the core block and extends two blocks beyond the catalog radius.
        return Math.max(1, gameplayRadius) + 2.0D;
    }

    private static Vec3 vfxCenter(ActiveField field) {
        return Vec3.atCenterOf(field.corePos).add(0.0D, -0.32D, 0.0D);
    }

    private static long vfxSeed(ServerLevel level, ActiveField field, TechniqueVfxPacket.Kind kind) {
        return field.corePos.asLong()
                ^ ((long) field.formationId.hashCode() << 19)
                ^ (level.getGameTime() / TICK_INTERVAL)
                ^ ((long) kind.ordinal() << 52);
    }

    private record FieldKey(String dimensionId, long packedPos) {
        static FieldKey of(ServerLevel level, BlockPos pos) {
            return new FieldKey(level.dimension().location().toString(), pos.asLong());
        }
    }

    private record VfxEmission(
            TechniqueVfxPacket.Kind kind,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            Vec3 start,
            Vec3 end,
            double radius,
            int intensity,
            long seed
    ) {}

    private static final class VfxBudgetState {
        private final ArrayDeque<VfxEmission> lifecyclePending = new ArrayDeque<>();
        private final ArrayDeque<VfxEmission> pulsePending = new ArrayDeque<>();
        private long gameTime = Long.MIN_VALUE;
        private int sentThisTick;

        private void offer(VfxEmission emission, boolean lifecycle) {
            if (pendingCount() >= MAX_PENDING_VFX_PER_DIMENSION) {
                if (!lifecycle || pulsePending.pollFirst() == null) {
                    return;
                }
            }
            (lifecycle ? lifecyclePending : pulsePending).addLast(emission);
        }

        private VfxEmission poll() {
            VfxEmission lifecycle = lifecyclePending.pollFirst();
            return lifecycle != null ? lifecycle : pulsePending.pollFirst();
        }

        private int pendingCount() {
            return lifecyclePending.size() + pulsePending.size();
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
