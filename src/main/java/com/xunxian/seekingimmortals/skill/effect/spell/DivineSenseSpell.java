package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DivineSenseSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final DivineSenseForm form;
    private final String successKey;

    public DivineSenseSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                            DivineSenseForm form, String successKey) {
        super(cost, cooldownTicks, damage);
        this.range = range;
        this.radius = radius;
        this.form = form;
        this.successKey = successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        return switch (form) {
            case SENSE_SCAN, DIVINE_SENSE_SCAN -> castScan(player, skill);
            case MIND_READ -> castMindRead(player, skill);
            case SENSE_PRESSURE, SENSE_NEEDLE, SENSE_LOCK, DIVINE_SENSE_LOCK -> castSingleTarget(player, skill);
            case SENSE_DOMAIN, SOUL_CRY_SHOCK -> castDomain(player, skill);
            case SOUL_ATTACK_WAVE -> castSoulWave(player, skill);
        };
    }

    private boolean castScan(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.7D, 0.0D);
        List<LivingEntity> targets = findAround(level, player, center, range, Math.max(4.0D, range * 0.45D));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            form.spawnScan(level, center, Math.min(range, 10.0D), List.of());
            return false;
        }

        int duration = scaled(form == DivineSenseForm.DIVINE_SENSE_SCAN ? 150 : 95, skill, 8);
        int revealed = 0;
        for (LivingEntity target : targets.stream().limit(form == DivineSenseForm.DIVINE_SENSE_SCAN ? 24 : 12).toList()) {
            add(target, MobEffects.GLOWING, duration, 0);
            if (form == DivineSenseForm.DIVINE_SENSE_SCAN) {
                target.removeEffect(MobEffects.INVISIBILITY);
            }
            revealed++;
        }

        form.spawnScan(level, center, Math.min(range, 12.0D), targets);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, revealed), true);
        return true;
    }

    private boolean castMindRead(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(radius, 0.9D));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        add(target, MobEffects.GLOWING, scaled(90, skill, 6), 0);
        add(target, MobEffects.WEAKNESS, scaled(55, skill, 5), 0);
        form.spawnLink(level, player.getEyePosition().subtract(0.0D, 0.12D, 0.0D),
                target.position().add(0.0D, target.getBbHeight() * 0.66D, 0.0D));
        level.playSound(null, target.blockPosition(), form.sound, SoundSource.PLAYERS, 0.62F, form.pitch);
        MutableComponent result = Component.translatable(successKey, target.getDisplayName(),
                Math.round(target.getHealth()), Math.round(target.getMaxHealth()));
        if (target instanceof Player targetPlayer) {
            result.append(" ").append(readPlayerRealm(targetPlayer).component());
        }
        player.displayClientMessage(result, true);
        return true;
    }

    private static RealmReadResult readPlayerRealm(Player target) {
        if (StatusRegistry.hidesRealm(target)) {
            return realmReadResult(true, null);
        }
        return CultivationHelper.get(target)
                .map(cultivation -> realmReadResult(false, cultivation.getRealm()))
                .orElseGet(() -> realmReadResult(false, null));
    }

    static RealmReadResult realmReadResult(boolean hidden, @Nullable Realm realm) {
        if (hidden) {
            return new RealmReadResult("message.seeking_immortals.spell.realm_read.hidden", List.of());
        }
        if (realm == null) {
            return new RealmReadResult("message.seeking_immortals.spell.realm_read.unknown", List.of());
        }
        return new RealmReadResult("message.seeking_immortals.spell.realm_read.visible",
                List.of(realm.getDisplayName()));
    }

    record RealmReadResult(String messageKey, List<Object> args) {
        RealmReadResult {
            args = List.copyOf(args);
        }

        Component component() {
            return Component.translatable(messageKey, args.toArray());
        }
    }

    private boolean castSingleTarget(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(level, player, range, Math.max(radius, 0.65D));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        if (damage > 0.0D) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * form.damageMultiplier(target)));
        }
        form.applySingleEffects(player, target, skill);
        form.spawnSingle(level, player.getEyePosition(), target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D));
        level.playSound(null, target.blockPosition(), form.sound, SoundSource.PLAYERS, 0.72F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, target.getDisplayName()), true);
        return true;
    }

    private boolean castDomain(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.3D, 0.0D);
        List<LivingEntity> targets = findAround(level, player, center, radius, 2.7D);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.area.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double falloff = Math.max(0.48D, 1.0D - target.position().distanceTo(center) / (radius + 0.85D));
            if (damage > 0.0D) {
                target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * form.damageMultiplier(target)));
            }
            form.applyAreaEffects(player, target, center, skill);
            hitCount++;
        }

        form.spawnDomain(level, center, radius, targets);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.82F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private boolean castSoulWave(ServerPlayer player, CultivationSkill skill) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.001D) {
            return false;
        }

        direction = direction.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.45D));
        Vec3 maxEnd = start.add(direction.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        List<LivingEntity> targets = findBeamTargets(level, player, start, end, Math.max(1.1D, radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.target.fail"), true);
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            double along = target.position().subtract(start).dot(direction);
            double falloff = Math.max(0.52D, 1.0D - along / (range * 1.30D));
            target.hurt(player.damageSources().indirectMagic(player, player), (float)(damage * falloff * form.damageMultiplier(target)));
            form.applyAreaEffects(player, target, target.position(), skill);
            hitCount++;
        }

        form.spawnWave(level, start, end);
        level.playSound(null, player.blockPosition(), form.sound, SoundSource.PLAYERS, 0.84F, form.pitch);
        player.displayClientMessage(Component.translatable(successKey, hitCount), true);
        return true;
    }

    private LivingEntity findTarget(ServerLevel level, ServerPlayer player, double maxRange, double inflate) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return null;
        }
        Vec3 end = start.add(look.normalize().scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, traceEnd,
                new AABB(start, traceEnd).inflate(Math.max(0.7D, inflate)),
                entity -> canTarget(entity, player));
        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private List<LivingEntity> findAround(ServerLevel level, ServerPlayer player, Vec3 center, double horizontal, double vertical) {
        AABB area = new AABB(center, center).inflate(horizontal, vertical, horizontal);
        double maxDistance = horizontal + 0.85D;
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player)
                                && entity.position().distanceToSqr(center) <= maxDistance * maxDistance)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .toList();
    }

    private List<LivingEntity> findBeamTargets(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, double maxRadius) {
        Vec3 line = end.subtract(start);
        AABB area = new AABB(start, end).inflate(maxRadius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> canTarget(entity, player)
                                && distanceToSegment(entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D), start, line) <= maxRadius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private boolean canTarget(Entity entity, ServerPlayer player) {
        return canAffect(player, entity);
    }

    private double distanceToSegment(Vec3 point, Vec3 start, Vec3 line) {
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(line) / lengthSqr));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static void add(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, true));
    }

    private static int scaled(int baseTicks, CultivationSkill skill, int perLevelTicks) {
        return baseTicks + Math.max(0, skill.getLevel() - 1) * perLevelTicks;
    }

    public enum DivineSenseForm {
        SENSE_SCAN(new DustParticleOptions(new Vector3f(0.38F, 0.68F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(0.82F, 0.94F, 1.00F), 0.34F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.34F),
        SENSE_PRESSURE(new DustParticleOptions(new Vector3f(0.42F, 0.34F, 0.94F), 0.62F),
                new DustParticleOptions(new Vector3f(0.92F, 0.82F, 1.00F), 0.34F),
                SoundEvents.BEACON_DEACTIVATE, 1.56F),
        SENSE_NEEDLE(new DustParticleOptions(new Vector3f(0.72F, 0.90F, 1.00F), 0.50F),
                new DustParticleOptions(new Vector3f(1.00F, 1.00F, 1.00F), 0.30F),
                SoundEvents.AMETHYST_CLUSTER_HIT, 1.72F),
        SENSE_DOMAIN(new DustParticleOptions(new Vector3f(0.32F, 0.55F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(0.92F, 0.78F, 1.00F), 0.38F),
                SoundEvents.BEACON_AMBIENT, 1.22F),
        MIND_READ(new DustParticleOptions(new Vector3f(0.74F, 0.62F, 1.00F), 0.48F),
                new DustParticleOptions(new Vector3f(0.96F, 0.92F, 1.00F), 0.32F),
                SoundEvents.ENCHANTMENT_TABLE_USE, 1.30F),
        SENSE_LOCK(new DustParticleOptions(new Vector3f(0.22F, 0.82F, 1.00F), 0.58F),
                new DustParticleOptions(new Vector3f(1.00F, 0.94F, 0.44F), 0.36F),
                SoundEvents.BEACON_POWER_SELECT, 1.48F),
        DIVINE_SENSE_SCAN(new DustParticleOptions(new Vector3f(0.62F, 0.82F, 1.00F), 0.55F),
                new DustParticleOptions(new Vector3f(0.98F, 0.94F, 0.68F), 0.38F),
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.18F),
        DIVINE_SENSE_LOCK(new DustParticleOptions(new Vector3f(0.24F, 0.94F, 1.00F), 0.62F),
                new DustParticleOptions(new Vector3f(1.00F, 0.82F, 0.36F), 0.42F),
                SoundEvents.BEACON_POWER_SELECT, 1.28F),
        SOUL_ATTACK_WAVE(new DustParticleOptions(new Vector3f(0.48F, 0.20F, 0.86F), 0.68F),
                new DustParticleOptions(new Vector3f(0.76F, 0.98F, 1.00F), 0.36F),
                SoundEvents.SOUL_ESCAPE, 0.72F),
        SOUL_CRY_SHOCK(new DustParticleOptions(new Vector3f(0.16F, 0.88F, 0.82F), 0.66F),
                new DustParticleOptions(new Vector3f(0.76F, 0.18F, 0.92F), 0.44F),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.18F);

        private final DustParticleOptions core;
        private final DustParticleOptions edge;
        private final SoundEvent sound;
        private final float pitch;

        DivineSenseForm(DustParticleOptions core, DustParticleOptions edge, SoundEvent sound, float pitch) {
            this.core = core;
            this.edge = edge;
            this.sound = sound;
            this.pitch = pitch;
        }

        private double damageMultiplier(LivingEntity target) {
            if (this == SOUL_CRY_SHOCK && target.getMobType() == MobType.UNDEAD) {
                return 1.45D;
            }
            return 1.0D;
        }

        private void applySingleEffects(ServerPlayer player, LivingEntity target, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case SENSE_PRESSURE -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 85 + bonus * 7, 2);
                    add(target, MobEffects.WEAKNESS, 80 + bonus * 6, 0);
                    crush(target, player.position(), 0.18D);
                }
                case SENSE_NEEDLE -> {
                    add(target, MobEffects.CONFUSION, 90 + bonus * 5, 0);
                    add(target, MobEffects.GLOWING, 75 + bonus * 4, 0);
                }
                case SENSE_LOCK -> {
                    add(target, MobEffects.GLOWING, 135 + bonus * 8, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 90 + bonus * 6, 2);
                }
                case DIVINE_SENSE_LOCK -> {
                    add(target, MobEffects.GLOWING, 180 + bonus * 10, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 120 + bonus * 8, 3);
                    add(target, MobEffects.WEAKNESS, 110 + bonus * 8, 1);
                    target.removeEffect(MobEffects.INVISIBILITY);
                }
                default -> {
                }
            }
        }

        private void applyAreaEffects(ServerPlayer player, LivingEntity target, Vec3 center, CultivationSkill skill) {
            int bonus = Math.max(0, skill.getLevel() - 1);
            switch (this) {
                case SENSE_DOMAIN -> {
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 95 + bonus * 7, 2);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 6, 0);
                    add(target, MobEffects.GLOWING, 80 + bonus * 5, 0);
                    crush(target, center, 0.12D);
                }
                case SOUL_ATTACK_WAVE -> {
                    add(target, MobEffects.CONFUSION, 100 + bonus * 6, 0);
                    add(target, MobEffects.WEAKNESS, 90 + bonus * 7, 1);
                }
                case SOUL_CRY_SHOCK -> {
                    add(target, MobEffects.CONFUSION, 115 + bonus * 7, 0);
                    add(target, MobEffects.MOVEMENT_SLOWDOWN, 90 + bonus * 6, 1);
                    if (target.getMobType() == MobType.UNDEAD) {
                        add(target, MobEffects.GLOWING, 130 + bonus * 8, 0);
                    }
                    crush(target, player.position(), -0.26D);
                }
                default -> {
                }
            }
        }

        private static void crush(LivingEntity target, Vec3 center, double vertical) {
            Vec3 delta = target.position().subtract(center);
            if (delta.lengthSqr() < 0.001D) {
                return;
            }
            target.push(delta.x * 0.05D, vertical, delta.z * 0.05D);
            target.hasImpulse = true;
        }

        private void spawnScan(ServerLevel level, Vec3 center, double range, List<LivingEntity> targets) {
            for (int layer = 0; layer < 3; layer++) {
                ring(level, center.add(0.0D, 0.18D + layer * 0.38D, 0.0D),
                        range * (0.28D + layer * 0.24D), 84, 0.07D);
            }
            int links = Math.min(8, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 0.62D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.62D, 0.0D), i * 17);
            }
        }

        private void spawnSingle(ServerLevel level, Vec3 start, Vec3 end) {
            switch (this) {
                case SENSE_NEEDLE -> beam(level, start, end, 0.05D, 10.0D);
                case SENSE_LOCK, DIVINE_SENSE_LOCK -> {
                    beam(level, start, end, 0.10D, 7.0D);
                    lockRings(level, end, this == DIVINE_SENSE_LOCK ? 1.15D : 0.82D);
                }
                case SENSE_PRESSURE -> {
                    beam(level, start, end, 0.14D, 5.0D);
                    for (int i = 0; i < 4; i++) {
                        ring(level, end.add(0.0D, 1.0D - i * 0.22D, 0.0D), 1.1D - i * 0.12D, 48, 0.02D);
                    }
                }
                default -> beam(level, start, end, 0.09D, 6.0D);
            }
        }

        private void spawnLink(ServerLevel level, Vec3 start, Vec3 end) {
            arc(level, start, end, 31);
            arc(level, start.add(0.0D, -0.08D, 0.0D), end.add(0.0D, 0.10D, 0.0D), 53);
            level.sendParticles(edge, end.x, end.y, end.z, 18, 0.18D, 0.16D, 0.18D, 0.006D);
        }

        private void spawnDomain(ServerLevel level, Vec3 center, double radius, List<LivingEntity> targets) {
            for (int layer = 0; layer < 4; layer++) {
                ring(level, center.add(0.0D, 0.20D + layer * 0.38D, 0.0D),
                        radius * (1.0D - layer * 0.13D), 92 - layer * 12, 0.10D);
            }
            for (int spoke = 0; spoke < 10; spoke++) {
                double angle = Math.PI * 2.0D * spoke / 10.0D;
                for (int i = 0; i < 18; i++) {
                    double t = i / 17.0D;
                    double wave = Math.sin(t * Math.PI * 2.0D + spoke) * 0.10D;
                    level.sendParticles((i & 1) == 0 ? core : edge,
                            center.x + Math.cos(angle) * radius * t,
                            center.y + 0.35D + wave,
                            center.z + Math.sin(angle) * radius * t,
                            1, 0.018D, 0.018D, 0.018D, 0.0D);
                }
            }
            int links = Math.min(6, targets.size());
            for (int i = 0; i < links; i++) {
                LivingEntity target = targets.get(i);
                arc(level, center.add(0.0D, 0.95D, 0.0D),
                        target.position().add(0.0D, target.getBbHeight() * 0.60D, 0.0D), i * 23);
            }
        }

        private void spawnWave(ServerLevel level, Vec3 start, Vec3 end) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(12, (int)(line.length() * 4.5D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double width = 0.18D + t * 0.75D;
                for (int arm = -1; arm <= 1; arm++) {
                    Vec3 offset = side.scale(arm * width).add(up.scale(Math.sin(t * Math.PI * 5.0D + arm) * 0.10D));
                    level.sendParticles(arm == 0 ? core : edge,
                            point.x + offset.x, point.y + offset.y, point.z + offset.z,
                            1, 0.024D, 0.024D, 0.024D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 24, 0.30D, 0.22D, 0.30D, 0.012D);
        }

        private void beam(ServerLevel level, Vec3 start, Vec3 end, double coilRadius, double turns) {
            Vec3 line = end.subtract(start);
            Vec3 direction = line.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : line.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            Vec3 up = side.cross(direction).normalize();
            int steps = Math.max(8, (int)(line.length() * 6.0D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t);
                double coil = t * Math.PI * turns;
                Vec3 swirl = side.scale(Math.sin(coil) * coilRadius).add(up.scale(Math.cos(coil) * coilRadius));
                level.sendParticles(core, point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
                if ((i & 1) == 0) {
                    Vec3 edgePoint = point.add(swirl);
                    level.sendParticles(edge, edgePoint.x, edgePoint.y, edgePoint.z, 1, 0.018D, 0.018D, 0.018D, 0.0D);
                }
            }
            level.sendParticles(edge, end.x, end.y, end.z, 16, 0.12D, 0.10D, 0.12D, 0.006D);
        }

        private void lockRings(ServerLevel level, Vec3 center, double radius) {
            ring(level, center.add(0.0D, 0.18D, 0.0D), radius, 48, 0.02D);
            ring(level, center.add(0.0D, -0.18D, 0.0D), radius * 0.82D, 40, 0.02D);
        }

        private void ring(ServerLevel level, Vec3 center, double radius, int points, double wave) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles((i & 1) == 0 ? core : edge,
                        center.x + Math.cos(angle) * radius,
                        center.y + Math.sin(angle * 3.0D) * wave,
                        center.z + Math.sin(angle) * radius,
                        1, 0.018D, 0.018D, 0.018D, 0.0D);
            }
        }

        private void arc(ServerLevel level, Vec3 start, Vec3 end, int seed) {
            Vec3 path = end.subtract(start);
            int steps = Math.max(6, (int)(path.length() * 4.8D));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vec3 point = start.lerp(end, t).add(Math.sin(seed + i * 1.21D) * 0.06D,
                        Math.cos(seed * 0.35D + i * 0.87D) * 0.05D,
                        Math.cos(seed + i * 1.13D) * 0.06D);
                level.sendParticles((i & 1) == 0 ? core : edge,
                        point.x, point.y, point.z, 1, 0.014D, 0.014D, 0.014D, 0.0D);
            }
        }
    }
}
