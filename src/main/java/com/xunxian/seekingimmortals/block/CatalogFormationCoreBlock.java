package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.structure.RingFormationStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared placeable formation core for remaining formation_catalog entries.
 * Ring uses Spirit Ore or Spirit Gathering Array depending on kind.
 */
public class CatalogFormationCoreBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    private final FormationKind kind;

    public CatalogFormationCoreBlock(Properties properties, FormationKind kind) {
        super(properties);
        this.kind = kind;
    }

    public FormationKind kind() {
        return kind;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable(kind.infoKey()), false);
            return InteractionResult.CONSUME;
        }

        Block ringBlock = kind.usesSpiritGatheringRing()
                ? ModBlocks.SPIRIT_GATHERING_ARRAY.get()
                : ModBlocks.SPIRIT_ORE.get();
        RingFormationStructure.CheckResult check = RingFormationStructure.validate(level, pos, ringBlock, kind.radius());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(kind.incompleteKey(), check.missingRing()), false);
            return InteractionResult.CONSUME;
        }

        for (MobEffectInstance effect : kind.effects()) {
            serverPlayer.addEffect(effect);
        }
        FormationFieldService.FieldKind fieldKind = kind.usesSpiritGatheringRing()
                ? FormationFieldService.FieldKind.SPIRIT_GATHER
                : FormationFieldService.FieldKind.CATALOG_GENERIC;
        FormationFieldService.activate(serverPlayer.serverLevel(), pos, fieldKind, serverPlayer);
        ServerLevel serverLevel = serverPlayer.serverLevel();
        serverLevel.sendParticles(kind.particle(), pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                36, 0.75D, 0.45D, 0.75D, 0.03D);
        serverLevel.playSound(null, pos, kind.sound(), SoundSource.BLOCKS, 0.8F, kind.soundPitch());
        player.displayClientMessage(Component.translatable(kind.activatedKey()), true);
        return InteractionResult.CONSUME;
    }

    public enum FormationKind {
        FIVE_ELEMENTS_MOUNTAIN(
                2, false,
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 1),
                new MobEffectInstance(MobEffects.ABSORPTION, 180, 0),
                ParticleTypes.POOF, SoundEvents.STONE_PLACE, 0.85F),
        NINE_DRAGON_FLAME_BARRIER(
                2, false,
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0),
                ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE, 1.1F),
        INVERTED_FIVE_ELEMENTS(
                2, true,
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0),
                ParticleTypes.REVERSE_PORTAL, SoundEvents.ENDERMAN_TELEPORT, 0.9F),
        VAJRA_PRISON(
                2, false,
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1),
                new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 0),
                ParticleTypes.CRIT, SoundEvents.ANVIL_LAND, 1.0F),
        MULAN_WIND_RIDE(
                2, true,
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 1),
                new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0),
                ParticleTypes.CLOUD, SoundEvents.ELYTRA_FLYING, 1.2F),
        BARRIER_SECT_PROTECTION(
                3, false,
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, 1),
                new MobEffectInstance(MobEffects.ABSORPTION, 220, 1),
                ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, 1.0F),
        SPIRIT_GATHERING_MINOR(
                1, true,
                new MobEffectInstance(MobEffects.REGENERATION, 160, 0),
                new MobEffectInstance(MobEffects.ABSORPTION, 120, 0),
                ParticleTypes.HAPPY_VILLAGER, SoundEvents.AMETHYST_BLOCK_CHIME, 1.15F),
        DEMON_SEAL_PILLAR(
                2, false,
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1),
                new MobEffectInstance(MobEffects.GLOWING, 200, 0),
                ParticleTypes.SOUL, SoundEvents.WARDEN_HEARTBEAT, 0.85F),
        SWORD_ARRAY_BAGUA(
                2, false,
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 1),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0),
                ParticleTypes.CRIT, SoundEvents.TRIDENT_RETURN, 1.05F),
        THUNDER_TRIBULATION_ARRAY(
                2, false,
                new MobEffectInstance(MobEffects.ABSORPTION, 240, 1),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0),
                ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F);

        private final int radius;
        private final boolean spiritGatheringRing;
        private final MobEffectInstance primary;
        private final MobEffectInstance secondary;
        private final ParticleOptions particle;
        private final SoundEvent sound;
        private final float soundPitch;

        FormationKind(int radius, boolean spiritGatheringRing,
                      MobEffectInstance primary, MobEffectInstance secondary,
                      ParticleOptions particle, SoundEvent sound, float soundPitch) {
            this.radius = radius;
            this.spiritGatheringRing = spiritGatheringRing;
            this.primary = primary;
            this.secondary = secondary;
            this.particle = particle;
            this.sound = sound;
            this.soundPitch = soundPitch;
        }

        public int radius() {
            return radius;
        }

        public boolean usesSpiritGatheringRing() {
            return spiritGatheringRing;
        }

        public MobEffectInstance[] effects() {
            return new MobEffectInstance[] {primary, secondary};
        }

        public ParticleOptions particle() {
            return particle;
        }

        public SoundEvent sound() {
            return sound;
        }

        public float soundPitch() {
            return soundPitch;
        }

        public String id() {
            return name().toLowerCase();
        }

        public String infoKey() {
            return "message.seeking_immortals." + id() + "_formation_core.info";
        }

        public String incompleteKey() {
            return "message.seeking_immortals." + id() + "_formation_core.incomplete";
        }

        public String activatedKey() {
            return "message.seeking_immortals." + id() + "_formation_core.activated";
        }
    }
}
