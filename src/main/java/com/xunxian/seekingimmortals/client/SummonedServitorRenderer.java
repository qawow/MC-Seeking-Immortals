package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Wave56: real GeckoLib GeoEntityRenderer for summoned servitors.
 */
public class SummonedServitorRenderer extends GeoEntityRenderer<SummonedServitorEntity> {
    public SummonedServitorRenderer(EntityRendererProvider.Context context) {
        super(context, new SummonedServitorGeoModel());
        this.shadowRadius = 0.55F;
    }

    @Override
    public ResourceLocation getTextureLocation(SummonedServitorEntity animatable) {
        return SummonedServitorGeoModel.textureFor(animatable);
    }

    public static final class SummonedServitorGeoModel extends GeoModel<SummonedServitorEntity> {
        private static final ResourceLocation MODEL =
                new ResourceLocation(SeekingImmortalsMod.MODID, "geo/summoned_servitor.geo.json");
        private static final ResourceLocation ANIM =
                new ResourceLocation(SeekingImmortalsMod.MODID, "animations/summoned_servitor.animation.json");
        private static final ResourceLocation TEX_GENERIC =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/summoned_servitor.png");
        private static final ResourceLocation TEX_BEAST =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/summoned_servitor_beast.png");
        private static final ResourceLocation TEX_PUPPET =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/summoned_servitor_puppet.png");
        private static final ResourceLocation TEX_GHOST =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/summoned_servitor_ghost.png");

        static ResourceLocation textureFor(SummonedServitorEntity entity) {
            if (entity == null) {
                return TEX_GENERIC;
            }
            return switch (entity.getArchetype()) {
                case BEAST -> TEX_BEAST;
                case PUPPET -> TEX_PUPPET;
                case GHOST -> TEX_GHOST;
                default -> TEX_GENERIC;
            };
        }

        @Override
        public ResourceLocation getModelResource(SummonedServitorEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(SummonedServitorEntity animatable) {
            return textureFor(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(SummonedServitorEntity animatable) {
            return ANIM;
        }
    }
}
