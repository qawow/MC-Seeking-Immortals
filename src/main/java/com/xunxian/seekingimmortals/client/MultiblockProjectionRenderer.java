package com.xunxian.seekingimmortals.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.structure.MultiblockProjectionCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Local Botania-style construction projection. It never sends placement or structure data to a server.
 */
@OnlyIn(Dist.CLIENT)
public final class MultiblockProjectionRenderer {
    private static final ResourceLocation BLUEPRINT_ITEM =
            new ResourceLocation(SeekingImmortalsMod.MODID, "structure_blueprint_table");
    private static final double MAX_VIEW_DISTANCE_SQR = 64.0D * 64.0D;
    private static final int MAX_RENDERED_CELLS = 240;
    private static final double OUTLINE_EPSILON = 0.003D;
    private static final RenderType GHOST_RENDER_TYPE = ForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET.get();

    @Nullable
    private static MultiblockProjectionCatalog.Projection visibleProjection;
    @Nullable
    private static BlockPos visibleOrigin;
    @Nullable
    private static MultiblockProjectionCatalog.Projection pinnedProjection;
    @Nullable
    private static BlockPos pinnedOrigin;
    @Nullable
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> pinnedDimension;
    private static int layerCursor;
    private static long lastToggleTick = Long.MIN_VALUE;
    @Nullable
    private static BlockPos lastToggleOrigin;

    private MultiblockProjectionRenderer() {}

    /** Re-evaluate the held-item preview once per client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            reset();
            return;
        }
        if (pinnedProjection != null && !level.dimension().equals(pinnedDimension)) {
            pinnedProjection = null;
            pinnedOrigin = null;
            pinnedDimension = null;
            layerCursor = 0;
        }

        Optional<Preview> held = heldPreview(minecraft, level);
        if (held.isPresent()) {
            setVisible(held.get().projection(), held.get().origin());
        } else if (pinnedProjection != null && pinnedOrigin != null) {
            setVisible(pinnedProjection, pinnedOrigin);
        } else {
            visibleProjection = null;
            visibleOrigin = null;
        }
    }

    /** Toggle a pinned projection and consume the client-side block interaction. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide()
                || !(event.getEntity().level() instanceof ClientLevel level)) {
            return;
        }
        ItemStack held = event.getEntity().getItemInHand(event.getHand());
        if (!isBlueprint(held)) {
            return;
        }
        String controllerId = blockId(level.getBlockState(event.getPos()).getBlock());
        Optional<MultiblockProjectionCatalog.Projection> projection =
                MultiblockProjectionCatalog.find(controllerId);
        if (projection.isEmpty()) {
            return;
        }
        togglePinned(level, event.getPos().immutable(), projection.get());
        // Controller use handlers commonly consume any held item before Item#useOn can run.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Shift-scroll cycles all layers and then each individual relative-height layer. */
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isShiftKeyDown()
                || !hasVisibleProjection()) {
            return;
        }
        cycleLayer(event.getScrollDelta());
        event.setCanceled(true);
        minecraft.player.displayClientMessage(layerMessage(), true);
    }

    public static boolean hasVisibleProjection() {
        return visibleProjection != null && visibleOrigin != null;
    }

    public static void cycleLayer(double delta) {
        if (!hasVisibleProjection()) {
            return;
        }
        int layerCount = visibleProjection.layers().size() + 1;
        int direction = delta >= 0.0D ? 1 : -1;
        layerCursor = Math.floorMod(layerCursor + direction, layerCount);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !hasVisibleProjection()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        BlockPos origin = visibleOrigin;
        MultiblockProjectionCatalog.Projection projection = visibleProjection;
        if (origin == null || projection == null) {
            return;
        }
        Vec3Camera camera = new Vec3Camera(event.getCamera().getPosition().x,
                event.getCamera().getPosition().y, event.getCamera().getPosition().z);
        double distanceSqr = origin.distToCenterSqr(event.getCamera().getPosition());
        if (distanceSqr > MAX_VIEW_DISTANCE_SQR) {
            return;
        }

        Integer selectedLayer = layerCursor == 0
                ? null
                : projection.layers().get(Math.min(layerCursor - 1, projection.layers().size() - 1));
        List<MultiblockProjectionCatalog.Cell> cells = projection.cellsForLayer(selectedLayer);
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        List<GhostEntry> ghosts = new ArrayList<>();
        List<OutlineEntry> outlines = new ArrayList<>();
        int rendered = 0;
        boolean ghostBatchEnded = false;
        try {
            for (MultiblockProjectionCatalog.Cell cell : cells) {
                if (rendered >= MAX_RENDERED_CELLS) {
                    break;
                }
                BlockPos worldPos = origin.offset(cell.offset());
                if (!level.hasChunkAt(worldPos)) {
                    continue;
                }
                BlockState actual = level.getBlockState(worldPos);
                boolean matches = cell.matches(blockId(actual.getBlock()), actual.isAir());
                if (cell.airRequired()) {
                    if (!matches) {
                        outlines.add(new OutlineEntry(worldPos, 0.95F, 0.18F, 0.16F, 0.92F));
                        rendered++;
                    }
                } else if (matches) {
                    outlines.add(new OutlineEntry(worldPos, 0.20F, 0.95F, 0.36F, 0.92F));
                    rendered++;
                } else if (actual.isAir()) {
                    ghosts.add(new GhostEntry(cell.displayBlockId(), worldPos));
                    rendered++;
                } else {
                    outlines.add(new OutlineEntry(worldPos, 0.95F, 0.18F, 0.16F, 0.94F));
                    rendered++;
                }
            }
            for (GhostEntry ghost : ghosts) {
                renderGhost(dispatcher, poseStack, buffers, ghost.displayBlockId(), ghost.worldPos(), camera);
            }
            buffers.endBatch(GHOST_RENDER_TYPE);
            ghostBatchEnded = true;
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            for (OutlineEntry outline : outlines) {
                renderOutline(poseStack, lines, outline.worldPos(), camera,
                        outline.red(), outline.green(), outline.blue(), outline.alpha());
            }
        } finally {
            if (!ghostBatchEnded) {
                buffers.endBatch(GHOST_RENDER_TYPE);
            }
            buffers.endBatch(RenderType.lines());
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    public static void reset() {
        visibleProjection = null;
        visibleOrigin = null;
        pinnedProjection = null;
        pinnedOrigin = null;
        pinnedDimension = null;
        layerCursor = 0;
        lastToggleTick = Long.MIN_VALUE;
        lastToggleOrigin = null;
    }

    private static Optional<Preview> heldPreview(Minecraft minecraft, ClientLevel level) {
        if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK
                || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return Optional.empty();
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            String controllerId = blockId(blockItem.getBlock());
            Optional<MultiblockProjectionCatalog.Projection> projection =
                    MultiblockProjectionCatalog.find(controllerId);
            if (projection.isEmpty()) {
                continue;
            }
            BlockPlaceContext context = new BlockPlaceContext(minecraft.player, hand, stack, hit);
            return Optional.of(new Preview(projection.get(), context.getClickedPos().immutable()));
        }
        return Optional.empty();
    }

    private static void setVisible(MultiblockProjectionCatalog.Projection projection, BlockPos origin) {
        if (visibleProjection == null || visibleOrigin == null
                || !visibleProjection.controllerId().equals(projection.controllerId())
                || !visibleOrigin.equals(origin)) {
            layerCursor = 0;
        }
        visibleProjection = projection;
        visibleOrigin = origin.immutable();
    }

    private static void togglePinned(ClientLevel level, BlockPos origin,
                                     MultiblockProjectionCatalog.Projection projection) {
        long gameTime = level.getGameTime();
        if (gameTime == lastToggleTick && origin.equals(lastToggleOrigin)) {
            return;
        }
        lastToggleTick = gameTime;
        lastToggleOrigin = origin;
        if (pinnedProjection != null && pinnedOrigin != null
                && pinnedOrigin.equals(origin)
                && pinnedProjection.controllerId().equals(projection.controllerId())) {
            pinnedProjection = null;
            pinnedOrigin = null;
            pinnedDimension = null;
            visibleProjection = null;
            visibleOrigin = null;
            layerCursor = 0;
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.multiblock_projection.cleared"), true);
            }
            return;
        }
        pinnedProjection = projection;
        pinnedOrigin = origin;
        pinnedDimension = level.dimension();
        layerCursor = 0;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.multiblock_projection.pinned",
                    Component.translatable(projection.displayKey())), true);
        }
    }

    private static boolean isBlueprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return BLUEPRINT_ITEM.equals(id);
    }

    private static boolean renderGhost(BlockRenderDispatcher dispatcher, PoseStack poseStack,
                                       MultiBufferSource.BufferSource buffers, String displayBlockId,
                                       BlockPos worldPos, Vec3Camera camera) {
        ResourceLocation id = ResourceLocation.tryParse(displayBlockId);
        if (id == null) {
            return false;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            return false;
        }
        BlockState ghost = block.defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(worldPos.getX() - camera.x, worldPos.getY() - camera.y,
                worldPos.getZ() - camera.z);
        MultiBufferSource tinted = renderType -> new TintingVertexConsumer(
                buffers.getBuffer(GHOST_RENDER_TYPE), 70, 220, 235, 112);
        dispatcher.renderSingleBlock(ghost, poseStack, tinted, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ModelData.EMPTY, GHOST_RENDER_TYPE);
        poseStack.popPose();
        return true;
    }

    private static void renderOutline(PoseStack poseStack, VertexConsumer consumer,
                                      BlockPos worldPos, Vec3Camera camera,
                                      float red, float green, float blue, float alpha) {
        double x = worldPos.getX() - camera.x;
        double y = worldPos.getY() - camera.y;
        double z = worldPos.getZ() - camera.z;
        LevelRenderer.renderLineBox(poseStack, consumer,
                x - OUTLINE_EPSILON, y - OUTLINE_EPSILON, z - OUTLINE_EPSILON,
                x + 1.0D + OUTLINE_EPSILON, y + 1.0D + OUTLINE_EPSILON,
                z + 1.0D + OUTLINE_EPSILON, red, green, blue, alpha);
    }

    private static String blockId(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            id = BuiltInRegistries.BLOCK.getKey(block);
        }
        return id == null ? "" : id.toString();
    }

    private static Component layerMessage() {
        if (visibleProjection == null || layerCursor == 0) {
            return Component.translatable("message.seeking_immortals.multiblock_projection.layer_all");
        }
        int layer = visibleProjection.layers().get(Math.min(layerCursor - 1, visibleProjection.layers().size() - 1));
        return Component.translatable("message.seeking_immortals.multiblock_projection.layer_single",
                layerCursor, visibleProjection.layers().size(), layer);
    }

    private record Preview(MultiblockProjectionCatalog.Projection projection, BlockPos origin) {}

    private record GhostEntry(String displayBlockId, BlockPos worldPos) {}

    private record OutlineEntry(BlockPos worldPos, float red, float green, float blue, float alpha) {}

    private record Vec3Camera(double x, double y, double z) {}

    private static final class TintingVertexConsumer extends VertexConsumerWrapper {
        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;

        private TintingVertexConsumer(VertexConsumer parent, int red, int green, int blue, int alpha) {
            super(parent);
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return super.color(scale(red, this.red), scale(green, this.green),
                    scale(blue, this.blue), scale(alpha, this.alpha));
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            super.defaultColor(scale(red, this.red), scale(green, this.green),
                    scale(blue, this.blue), scale(alpha, this.alpha));
        }

        private static int scale(int value, int tint) {
            return Math.max(0, Math.min(255, value * tint / 255));
        }
    }
}
