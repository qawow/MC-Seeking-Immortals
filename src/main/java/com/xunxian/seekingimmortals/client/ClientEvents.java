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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    public static final KeyMapping OPEN_TECHNIQUE_EDIT_KEY = new KeyMapping(
            "key.seeking_immortals.open_technique_edit",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_CULTIVATION_STATS_KEY = new KeyMapping(
            "key.seeking_immortals.open_cultivation_stats",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_QUEST_TRACKER_KEY = new KeyMapping(
            "key.seeking_immortals.open_quest_tracker",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_LORE_COMPENDIUM_KEY = new KeyMapping(
            "key.seeking_immortals.open_lore_compendium",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_BESTIARY_KEY = new KeyMapping(
            "key.seeking_immortals.open_bestiary",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.seeking_immortals");
    public static final KeyMapping OPEN_CHRONICLE_KEY = new KeyMapping(
            "key.seeking_immortals.open_chronicle",
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
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.xunxian.seekingimmortals.client.ui.UiThemeConfig.load();
            MenuScreens.register(ModMenus.ALCHEMY_FURNACE.get(), AlchemyFurnaceScreen::new);
            MenuScreens.register(ModMenus.STORAGE_BRACELET.get(), StorageBraceletScreenMenu::new);
            MenuScreens.register(ModMenus.MARKET_HALL.get(), MarketHallScreen::new);
            MenuScreens.register(ModMenus.AUCTION_HALL.get(), AuctionHallScreen::new);
            MenuScreens.register(ModMenus.SECT_HALL.get(), SectHallScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TECHNIQUE_EDIT_KEY);
        event.register(OPEN_CULTIVATION_STATS_KEY);
        event.register(OPEN_QUEST_TRACKER_KEY);
        event.register(OPEN_LORE_COMPENDIUM_KEY);
        event.register(OPEN_BESTIARY_KEY);
        event.register(OPEN_CHRONICLE_KEY);
        event.register(BREAKTHROUGH_KEY);
        for (KeyMapping keyMapping : RELEASE_TECHNIQUE_KEYS) {
            event.register(keyMapping);
        }
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cultivation_health", CultivationHealthOverlay::renderOverlay);
        event.registerAboveAll("technique_skill_bar", TechniqueSkillBarOverlay::renderOverlay);
        event.registerAboveAll("cultivation_hud", CultivationHudOverlay::renderOverlay);
        event.registerAboveAll("authored_visual_overlay", ClientVisualOverlayRuntime::render);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSHION_SEAT.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_PROJECTILE.get(), SwordProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATION_FIREBALL.get(), CultivationFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.SECT_STEWARD.get(), CultivatorNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.MARKET_TRADER.get(), CultivatorNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_STONE_BANKER.get(), CultivatorNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.QUEST_NPC.get(), CultivatorNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATION_BEAST.get(), CultivationBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.SUMMONED_SERVITOR.get(), SummonedServitorRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_BOAT.get(), SpiritBoatRenderer::new);
    }

    @Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
    public static final class ForgeClientEvents {
        private ForgeClientEvents() {}

        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())
                    && minecraft.gui instanceof ForgeGui forgeGui) {
                event.setCanceled(true);
                AuthoredStatusOverlay.render(forgeGui, event.getGuiGraphics(), event.getPartialTick(),
                        minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
                return;
            }
            if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                    && minecraft.gui instanceof ForgeGui forgeGui
                    && CultivationHealthOverlay.shouldReplaceVanillaPlayerHealth(forgeGui)) {
                event.setCanceled(true);
            }
        }

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
            while (BREAKTHROUGH_KEY.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new AttemptBreakthroughPacket());
            }
            while (OPEN_CULTIVATION_STATS_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    minecraft.setScreen(new CultivationStatsScreen(player, false));
                }
            }
            while (OPEN_QUEST_TRACKER_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    minecraft.setScreen(new QuestTrackerScreen());
                    com.xunxian.seekingimmortals.network.ModNetwork.CHANNEL.sendToServer(
                            new com.xunxian.seekingimmortals.network.QuestTrackerActionPacket("sync"));
                }
            }
            while (OPEN_LORE_COMPENDIUM_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    ModNetwork.CHANNEL.sendToServer(
                            new com.xunxian.seekingimmortals.network.LoreScreenActionPacket("compendium"));
                }
            }
            while (OPEN_BESTIARY_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    ModNetwork.CHANNEL.sendToServer(
                            new com.xunxian.seekingimmortals.network.LoreScreenActionPacket("bestiary"));
                }
            }
            while (OPEN_CHRONICLE_KEY.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    ModNetwork.CHANNEL.sendToServer(
                            new com.xunxian.seekingimmortals.network.LoreScreenActionPacket("chronicle"));
                }
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
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ClientVisualEngine.tick();
                ClientVisualOverlayRuntime.tick();
                LodestoneTechniqueVfx.tickProjectiles();
                MultiblockProjectionRenderer.tick();
            }
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            MultiblockProjectionRenderer.onMouseScroll(event);
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            MultiblockProjectionRenderer.onRightClickBlock(event);
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
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (!event.getLevel().isClientSide()) {
                return;
            }
            if (event.getEntity() == Minecraft.getInstance().player) {
                resetClientSyncState();
            }
            LodestoneTechniqueVfx.trackProjectile(event.getEntity());
        }

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                LodestoneTechniqueVfx.untrackProjectile(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                ClientVisualEngine.render(event);
                LodestoneTechniqueVfx.renderWorldGeometry(event);
                MultiblockProjectionRenderer.render(event);
            }
        }

        private static void resetClientSyncState() {
            ClientCultivationData.reset();
            ClientSkillData.reset();
            ClientTechniqueData.reset();
            ClientMethodData.reset();
            ClientMethodLayoutData.reset();
            ClientSectData.reset();
            ClientShopData.reset();
            ClientWorldpackData.reset();
            ClientQuestTrackerData.reset();
            ClientAuctionLadderData.reset();
            ClientLoreData.reset();
            ClientVisualEngine.reset();
            LodestoneTechniqueVfx.reset();
            MultiblockProjectionRenderer.reset();
        }

        private static void drainTechniqueKeyClicks() {
            OPEN_TECHNIQUE_EDIT_KEY.consumeClick();
            OPEN_CULTIVATION_STATS_KEY.consumeClick();
            OPEN_QUEST_TRACKER_KEY.consumeClick();
            OPEN_LORE_COMPENDIUM_KEY.consumeClick();
            OPEN_BESTIARY_KEY.consumeClick();
            OPEN_CHRONICLE_KEY.consumeClick();
            BREAKTHROUGH_KEY.consumeClick();
            for (KeyMapping keyMapping : RELEASE_TECHNIQUE_KEYS) {
                keyMapping.consumeClick();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen().getClass() != InventoryScreen.class) return;
            InventoryScreen inventoryScreen = (InventoryScreen) event.getScreen();
            InventoryEntryLayout layout = inventoryEntryLayout(
                    inventoryScreen.getGuiLeft(), inventoryScreen.getGuiTop(),
                    inventoryScreen.width, inventoryScreen.height);
            event.addListener(ImmortalButton.secondary(layout.x(), layout.y(), layout.width(), layout.height(),
                    Component.translatable("screen.seeking_immortals.cultivation_stats.tab"), button -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.setScreen(new CultivationStatsScreen(minecraft.player, true));
                        }
                    }));
        }
    }

    static InventoryEntryLayout inventoryEntryLayout(int screenWidth, int screenHeight) {
        int guiLeft = Math.max(0, (screenWidth - 176) / 2);
        int guiTop = Math.max(0, (screenHeight - 166) / 2);
        return inventoryEntryLayout(guiLeft, guiTop, screenWidth, screenHeight);
    }

    static InventoryEntryLayout inventoryEntryLayout(int guiLeft, int guiTop,
                                                     int screenWidth, int screenHeight) {
        int margin = 2;
        int buttonWidth = Math.min(42, Math.max(1, screenWidth - margin * 2));
        int buttonHeight = Math.min(18, Math.max(1, screenHeight - margin * 2));
        int maxX = Math.max(margin, screenWidth - buttonWidth - margin);
        int maxY = Math.max(margin, screenHeight - buttonHeight - margin);
        int x = Math.max(margin, Math.min(guiLeft, maxX));
        int y = Math.max(margin, Math.min(guiTop - buttonHeight - 2, maxY));
        return new InventoryEntryLayout(x, y, buttonWidth, buttonHeight);
    }

    record InventoryEntryLayout(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }
}
