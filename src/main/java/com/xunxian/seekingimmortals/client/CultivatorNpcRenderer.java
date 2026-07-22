package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivatorNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Shared GeckoLib renderer for the dedicated cultivator NPC hierarchy. */
public final class CultivatorNpcRenderer<T extends CultivatorNpcEntity> extends GeoEntityRenderer<T> {
    public CultivatorNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CultivatorNpcGeoModel<>());
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return CultivatorNpcGeoModel.textureFor(animatable);
    }

    private static final class CultivatorNpcGeoModel<T extends CultivatorNpcEntity> extends GeoModel<T> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                SeekingImmortalsMod.MODID, "geo/cultivator_npc.geo.json");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                SeekingImmortalsMod.MODID, "animations/cultivator_npc.animation.json");
        private static final ResourceLocation STEWARD = texture("steward");
        private static final ResourceLocation TRADER = texture("trader");
        private static final ResourceLocation BANKER = texture("banker");
        private static final ResourceLocation QUEST = texture("quest");

        private static ResourceLocation texture(String role) {
            return new ResourceLocation(SeekingImmortalsMod.MODID,
                    "textures/entity/cultivator_npc_" + role + ".png");
        }

        private static ResourceLocation textureFor(CultivatorNpcEntity entity) {
            if (entity == null) {
                return QUEST;
            }
            return switch (entity.getVisualRole()) {
                case STEWARD -> STEWARD;
                case TRADER -> TRADER;
                case BANKER -> BANKER;
                case QUEST -> QUEST;
            };
        }

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return textureFor(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return ANIMATION;
        }
    }
}
