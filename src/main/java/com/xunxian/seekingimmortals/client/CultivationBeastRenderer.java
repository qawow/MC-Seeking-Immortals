package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Locale;

/** GeckoLib renderer selecting authored silhouettes and affinity skins from synced beast data. */
public final class CultivationBeastRenderer extends GeoEntityRenderer<CultivationBeastEntity> {
    public CultivationBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new CultivationBeastGeoModel());
        this.shadowRadius = 0.55F;
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationBeastEntity animatable) {
        return CultivationBeastGeoModel.textureFor(animatable);
    }

    static final class CultivationBeastGeoModel extends GeoModel<CultivationBeastEntity> {
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                SeekingImmortalsMod.MODID, "animations/cultivation_beast.animation.json");

        private static String bodyPlan(CultivationBeastEntity entity) {
            CultivationBeastEntity.BodyPlan plan = entity == null
                    ? CultivationBeastEntity.BodyPlan.QUADRUPED
                    : entity.getBodyPlan();
            return plan.name().toLowerCase(Locale.ROOT);
        }

        private static String element(CultivationBeastEntity entity) {
            String value = entity == null || entity.getElement() == null
                    ? "neutral"
                    : entity.getElement().trim().toLowerCase(Locale.ROOT);
            return CultivationBeastEntity.SUPPORTED_ELEMENTS.contains(value) ? value : "neutral";
        }

        private static ResourceLocation textureFor(CultivationBeastEntity entity) {
            String plan = bodyPlan(entity);
            String affinity = element(entity);
            if ("neutral".equals(affinity)) {
                return new ResourceLocation(SeekingImmortalsMod.MODID,
                        "textures/entity/cultivation_beast_" + plan + ".png");
            }
            return new ResourceLocation(SeekingImmortalsMod.MODID,
                    "textures/entity/cultivation_beast/" + plan + "_" + affinity + ".png");
        }

        @Override
        public ResourceLocation getModelResource(CultivationBeastEntity animatable) {
            return new ResourceLocation(SeekingImmortalsMod.MODID,
                    "geo/cultivation_beast_" + bodyPlan(animatable) + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(CultivationBeastEntity animatable) {
            return textureFor(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(CultivationBeastEntity animatable) {
            return ANIMATION;
        }
    }
}
