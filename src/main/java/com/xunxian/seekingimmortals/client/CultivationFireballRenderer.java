package com.xunxian.seekingimmortals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class CultivationFireballRenderer extends EntityRenderer<CultivationFireballEntity> {
    public CultivationFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CultivationFireballEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        CultivationFireballEntity.SpellElement element = entity.getElement();
        float age = entity.tickCount + partialTick;
        float pulse = 0.9F + 0.1F * Mth.sin(age * 0.45F);
        float scale = element.visualScale * pulse;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 12.0F));

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        drawDiamond(consumer, matrix, 0.42F * scale, element.outerRed, element.outerGreen, element.outerBlue, 145);
        drawDiamond(consumer, matrix, 0.24F * scale, element.coreRed, element.coreGreen, element.coreBlue, 235);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        drawDiamond(consumer, poseStack.last().pose(), 0.30F * scale, element.outerRed, element.outerGreen, element.outerBlue, 120);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void drawDiamond(VertexConsumer consumer, Matrix4f matrix, float radius,
                                    float red, float green, float blue, int alpha) {
        int r = Math.round(red * 255.0F);
        int g = Math.round(green * 255.0F);
        int b = Math.round(blue * 255.0F);
        consumer.vertex(matrix, 0.0F, radius, 0.0F).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, radius * 0.72F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, 0.0F, -radius, 0.0F).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, -radius * 0.72F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationFireballEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
