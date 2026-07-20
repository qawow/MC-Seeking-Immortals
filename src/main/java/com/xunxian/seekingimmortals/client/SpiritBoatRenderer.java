package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.SpiritBoatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Visible GeckoLib renderer for spirit boat / cloud vehicles.
 */
public class SpiritBoatRenderer extends GeoEntityRenderer<SpiritBoatEntity> {
    public SpiritBoatRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiritBoatGeoModel());
        this.shadowRadius = 0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritBoatEntity animatable) {
        return SpiritBoatGeoModel.textureFor(animatable);
    }

    public static final class SpiritBoatGeoModel extends GeoModel<SpiritBoatEntity> {
        private static final ResourceLocation MODEL =
                new ResourceLocation(SeekingImmortalsMod.MODID, "geo/spirit_boat.geo.json");
        private static final ResourceLocation ANIM =
                new ResourceLocation(SeekingImmortalsMod.MODID, "animations/spirit_boat.animation.json");
        private static final ResourceLocation TEX =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/spirit_boat.png");
        private static final ResourceLocation TEX_CLOUD =
                new ResourceLocation(SeekingImmortalsMod.MODID, "textures/entity/spirit_boat_cloud.png");

        static ResourceLocation textureFor(SpiritBoatEntity entity) {
            if (entity != null && entity.vehicleId() != null) {
                String id = entity.vehicleId().toLowerCase();
                if (id.contains("cloud") || id.contains("sedan")) {
                    return TEX_CLOUD;
                }
            }
            return TEX;
        }

        @Override
        public ResourceLocation getModelResource(SpiritBoatEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(SpiritBoatEntity animatable) {
            return textureFor(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(SpiritBoatEntity animatable) {
            return ANIM;
        }
    }
}
