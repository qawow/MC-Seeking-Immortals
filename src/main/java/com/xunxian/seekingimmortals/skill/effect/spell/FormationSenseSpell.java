package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FormationSenseSpell extends SpellEffect {
    public FormationSenseSpell() {
        super(10, 200, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return false;
        int range = Math.min(32, Math.max(12, cultivation.getDivSense()));
        int yRange = Math.min(10, range / 2);
        int matches = 0;
        int maxMatches = 48;
        BlockPos origin = player.blockPosition();
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("illusion");
        vfx.castAt(level, player);
        vfx.scanAt(level, player.position(), Math.min(16.0D, range), 72);

        outer:
        for (int x = -range; x <= range; x++) {
            for (int y = -yRange; y <= yRange; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x * x + y * y + z * z > range * range) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!isFormationLike(state)) continue;
                    level.sendParticles(ParticleTypes.WITCH,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            2, 0.3D, 0.35D, 0.3D, 0.01D);
                    if (++matches >= maxMatches) break outer;
                }
            }
        }

        player.displayClientMessage(Component.translatable("message.seeking_immortals.spell.formation_sense", matches), true);
        return true;
    }

    private static boolean isFormationLike(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) return false;
        if (state.is(ModBlocks.SPIRIT_GATHERING_ARRAY.get()) || state.is(ModBlocks.LING_GEN_IDENTIFICATION_SLAB.get())) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return path.contains("array")
                || path.contains("formation")
                || path.contains("spirit_gathering")
                || path.contains("ling_gen")
                || path.contains("alchemy_furnace");
    }
}
