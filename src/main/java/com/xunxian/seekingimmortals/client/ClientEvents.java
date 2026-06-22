package com.xunxian.seekingimmortals.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.AttemptBreakthroughPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.ReleaseTechniquePacket;
import com.xunxian.seekingimmortals.network.SetMeditatingPacket;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    public static final KeyMapping MEDITATE_KEY = new KeyMapping(
            "key.seeking_immortals.meditate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_TECHNIQUE_EDIT_KEY = new KeyMapping(
            "key.seeking_immortals.open_technique_edit",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping BREAKTHROUGH_KEY = new KeyMapping(
            "key.seeking_immortals.breakthrough",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping[] RELEASE_TECHNIQUE_KEYS = new KeyMapping[] {
            releaseTechniqueKey(1),
            releaseTechniqueKey(2),
            releaseTechniqueKey(3),
            releaseTechniqueKey(4),
            releaseTechniqueKey(5),
            releaseTechniqueKey(6),
            releaseTechniqueKey(7)
    };

    private ClientEvents() {}

    private static KeyMapping releaseTechniqueKey(int slot) {
        return new KeyMapping(
                "key.seeking_immortals.release_technique_" + slot,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                "key.categories.seeking_immortals");
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MEDITATE_KEY);
        event.register(OPEN_TECHNIQUE_EDIT_KEY);
        event.register(BREAKTHROUGH_KEY);
        for (KeyMapping keyMapping : RELEASE_TECHNIQUE_KEYS) {
            event.register(keyMapping);
        }
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("technique_skill_bar", TechniqueSkillBarOverlay::renderOverlay);
        event.registerAboveAll("breathing_hud", BreathingHudOverlay::renderOverlay);
        event.registerAboveAll("cultivation_hud", CultivationHudOverlay::renderOverlay);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSHION_SEAT.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(), EmptyEntityRenderer::new);
    }

    @Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
    public static final class ForgeClientEvents {
        private ForgeClientEvents() {}

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            boolean effectiveMeditating = ClientCultivationData.effectiveMeditating();
            if (player != null && effectiveMeditating && (player.input.up || player.input.down || player.input.left || player.input.right || player.input.jumping || player.input.shiftKeyDown)) {
                ClientCultivationData.setPendingMeditating(false);
                ModNetwork.CHANNEL.sendToServer(new SetMeditatingPacket(false));
            }
            if (minecraft.screen != null) {
                drainTechniqueKeyClicks();
                return;
            }
            while (MEDITATE_KEY.consumeClick()) {
                boolean targetMeditating = !ClientCultivationData.effectiveMeditating();
                ClientCultivationData.setPendingMeditating(targetMeditating);
                ModNetwork.CHANNEL.sendToServer(new SetMeditatingPacket(targetMeditating));
            }
            while (BREAKTHROUGH_KEY.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new AttemptBreakthroughPacket());
            }
            for (int i = 0; i < RELEASE_TECHNIQUE_KEYS.length; i++) {
                while (RELEASE_TECHNIQUE_KEYS[i].consumeClick()) {
                    if (ClientTechniqueData.isSynced() && !ClientTechniqueData.getTechniqueInSlot(i).isBlank()) {
                        ModNetwork.CHANNEL.sendToServer(new ReleaseTechniquePacket(i));
                    } else if (player != null) {
                        player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.empty_slot", i + 1), true);
                    }
                }
            }
            while (OPEN_TECHNIQUE_EDIT_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    minecraft.setScreen(new TechniqueEditScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            resetClientSyncState();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            resetClientSyncState();
        }

        @SubscribeEvent
        public static void onClientRespawn(ClientPlayerNetworkEvent.Clone event) {
            resetClientSyncState();
        }

        @SubscribeEvent
        public static void onLocalPlayerJoinLevel(EntityJoinLevelEvent event) {
            if (event.getEntity() == Minecraft.getInstance().player) {
                resetClientSyncState();
            }
        }

        private static void resetClientSyncState() {
            ClientCultivationData.reset();
            ClientTechniqueData.reset();
        }

        private static void drainTechniqueKeyClicks() {
            MEDITATE_KEY.consumeClick();
            OPEN_TECHNIQUE_EDIT_KEY.consumeClick();
            BREAKTHROUGH_KEY.consumeClick();
            for (KeyMapping keyMapping : RELEASE_TECHNIQUE_KEYS) {
                keyMapping.consumeClick();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen().getClass() != InventoryScreen.class) return;
            int x = event.getScreen().width / 2 - 88;
            int y = event.getScreen().height / 2 - 104;
            event.addListener(Button.builder(Component.translatable("screen.seeking_immortals.cultivation_stats.tab"), button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null) {
                        minecraft.setScreen(new CultivationStatsScreen(minecraft.player, true));
                    }
                })
                    .bounds(x, y, 42, 18)
                    .build());
        }
    }
}
