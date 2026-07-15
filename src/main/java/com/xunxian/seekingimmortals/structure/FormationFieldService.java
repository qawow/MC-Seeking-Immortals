package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timed formation field aura while the ring multiblock remains intact.
 * Wave49: persists active fields via FormationFieldSavedData (survive restart).
 */
public final class FormationFieldService {
    private static final int DEFAULT_DURATION_TICKS = 20 * 90;
    private static final int TICK_INTERVAL = 20;
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

    public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind) {
        return activate(level, corePos, kind, null);
    }

    /** Wave490: optional deployer for life-skill FORMATION practice + duration scale. */
    public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind, ServerPlayer deployer) {
        if (level == null || corePos == null || kind == null) {
            return false;
        }
        if (!ringIntact(level, corePos, kind)) {
            return false;
        }
        int duration = scaledDuration(deployer, DEFAULT_DURATION_TICKS);
        FieldKey key = FieldKey.of(level, corePos);
        ActiveField field = new ActiveField(level.dimension().location().toString(), corePos.immutable(), kind, duration);
        ACTIVE.put(key, field);
        persistField(level, field);
        grantFormationPractice(deployer, true);
        return true;
    }

    /**
     * Handheld formation deploy: free field around player without requiring a ring core.
     */
    public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, int durationTicks) {
        return activateFreeField(level, center, kind, durationTicks, null);
    }

    public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, int durationTicks, ServerPlayer deployer) {
        if (level == null || center == null || kind == null) {
            return false;
        }
        int duration = scaledDuration(deployer, Math.max(20 * 20, durationTicks));
        FieldKey key = FieldKey.of(level, center);
        ActiveField field = new ActiveField(level.dimension().location().toString(), center.immutable(), kind, duration);
        field.freeField = true;
        ACTIVE.put(key, field);
        persistField(level, field);
        grantFormationPractice(deployer, false);
        return true;
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
            FieldKey key = new FieldKey(dim, stored.corePos().asLong());
            ActiveField field = new ActiveField(dim, stored.corePos(), kind, Math.max(1, stored.remainingTicks()));
            field.freeField = !ringIntact(level, stored.corePos(), kind);
            ACTIVE.put(key, field);
        }
    }

    public static void serverTick(ServerLevel level) {
        if (level == null || ACTIVE.isEmpty()) {
            return;
        }
        String dim = level.dimension().location().toString();
        boolean dirty = false;
        Iterator<Map.Entry<FieldKey, ActiveField>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FieldKey, ActiveField> entry = it.next();
            ActiveField field = entry.getValue();
            if (!dim.equals(field.dimensionId)) {
                continue;
            }
            field.remainingTicks -= 1;
            if (field.remainingTicks <= 0 || (!field.freeField && !ringIntact(level, field.corePos, field.kind))) {
                it.remove();
                FormationFieldSavedData.get(level).remove(field.dimensionId, field.corePos);
                dirty = true;
                continue;
            }
            if (field.remainingTicks % TICK_INTERVAL == 0) {
                applyFieldPulse(level, field);
                persistField(level, field);
                dirty = true;
            }
        }
        if (dirty) {
            // no-op: SavedData marks dirty per upsert/remove
        }
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    private static void persistField(ServerLevel level, ActiveField field) {
        FormationFieldSavedData.get(level).upsert(field.dimensionId, field.corePos, field.kind.name(), field.remainingTicks);
    }

    private static boolean ringIntact(Level level, BlockPos corePos, FieldKind kind) {
        Block ringBlock = kind.usesSpiritGatheringRing()
                ? ModBlocks.SPIRIT_GATHERING_ARRAY.get()
                : ModBlocks.SPIRIT_ORE.get();
        return RingFormationStructure.validate(level, corePos, ringBlock, kind.radius()).complete();
    }

    private static void applyFieldPulse(ServerLevel level, ActiveField field) {
        double radius = field.kind.radius() + 1.5D;
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

        private ActiveField(String dimensionId, BlockPos corePos, FieldKind kind, int remainingTicks) {
            this.dimensionId = dimensionId;
            this.corePos = corePos;
            this.kind = kind;
            this.remainingTicks = remainingTicks;
        }
    }
}
