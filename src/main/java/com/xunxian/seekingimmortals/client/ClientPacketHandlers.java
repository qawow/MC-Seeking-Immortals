package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.OpenAlchemyStatusPacket;
import com.xunxian.seekingimmortals.network.OpenAuctionScreenPacket;
import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import com.xunxian.seekingimmortals.network.OpenRefinePlanPacket;
import com.xunxian.seekingimmortals.network.OpenStoragePreviewPacket;
import com.xunxian.seekingimmortals.network.SyncLoreUnlockPacket;
import com.xunxian.seekingimmortals.network.SyncQuestTrackerPacket;
import com.xunxian.seekingimmortals.network.SyncSectDataPacket;
import com.xunxian.seekingimmortals.network.SyncShopDataPacket;
import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Client-only packet side effects. Kept out of network/* so dedicated servers can load
 * packet classes without resolving Screen/Minecraft during CONSTRUCT.
 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void handleSyncSect(SyncSectDataPacket packet) {
        ClientSectData.set(packet);
        // Wave490: sect opens via NetworkHooks SectHallMenu; legacy SectScreen only if no hall open.
        if (packet.openScreen()) {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof SectHallScreen) && !(mc.screen instanceof SectScreen)) {
                mc.setScreen(new SectScreen());
            }
        }
    }

    public static void handleSyncShop(SyncShopDataPacket packet) {
        ClientShopData.set(packet);
        // Wave490: market opens via NetworkHooks MarketHallMenu; keep legacy ShopScreen only as soft fallback.
        if (packet.openScreen()) {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof MarketHallScreen) && !(mc.screen instanceof ShopScreen)) {
                mc.setScreen(new ShopScreen());
            }
        }
    }

    public static void handleSyncWorldpack(SyncWorldpackDataPacket packet) {
        ClientWorldpackData.set(packet);
        if (packet.openScreen()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof WorldpackScreen screen) {
                screen.refreshFromSync();
            } else {
                minecraft.setScreen(new WorldpackScreen());
            }
        }
    }

    public static void handleSyncQuestTracker(SyncQuestTrackerPacket packet) {
        ClientQuestTrackerData.set(packet);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuestTrackerScreen tracker) {
            tracker.refreshWidgets();
        }
    }

    public static void handleSyncLore(SyncLoreUnlockPacket packet) {
        ClientLoreData.set(
                packet.bestiaryUnlocked(),
                packet.chronicleDiscovered(),
                packet.timelinePhases());
        String open = packet.openScreen() == null ? "" : packet.openScreen().trim().toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        if (open.isBlank()) {
            if (mc.screen instanceof BestiaryScreen bestiary) {
                bestiary.refreshFromSync();
            } else if (mc.screen instanceof ChronicleScreen chronicle) {
                chronicle.refreshFromSync();
            } else if (mc.screen instanceof LoreCompendiumScreen lore) {
                lore.refreshFromSync();
            }
            return;
        }

        boolean mayReplace = mc.screen == null
                || mc.screen instanceof BestiaryScreen
                || mc.screen instanceof ChronicleScreen
                || mc.screen instanceof LoreCompendiumScreen;
        switch (open) {
            case "bestiary" -> {
                if (mc.screen instanceof BestiaryScreen bestiary) {
                    bestiary.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new BestiaryScreen());
                }
            }
            case "chronicle" -> {
                if (mc.screen instanceof ChronicleScreen chronicle) {
                    chronicle.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new ChronicleScreen());
                }
            }
            case "compendium", "hub", "lore" -> {
                if (mc.screen instanceof LoreCompendiumScreen lore
                        && lore.isShowing(LoreCompendiumScreen.Tab.HUB)) {
                    lore.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new LoreCompendiumScreen());
                }
            }
            case "glossary" -> {
                if (mc.screen instanceof LoreCompendiumScreen lore
                        && lore.isShowing(LoreCompendiumScreen.Tab.GLOSSARY)) {
                    lore.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new LoreCompendiumScreen(LoreCompendiumScreen.Tab.GLOSSARY));
                }
            }
            case "numeric" -> {
                if (mc.screen instanceof LoreCompendiumScreen lore
                        && lore.isShowing(LoreCompendiumScreen.Tab.NUMERIC)) {
                    lore.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new LoreCompendiumScreen(LoreCompendiumScreen.Tab.NUMERIC));
                }
            }
            case "visual" -> {
                if (mc.screen instanceof LoreCompendiumScreen lore
                        && lore.isShowing(LoreCompendiumScreen.Tab.VISUAL)) {
                    lore.refreshFromSync();
                } else if (mayReplace) {
                    mc.setScreen(new LoreCompendiumScreen(LoreCompendiumScreen.Tab.VISUAL));
                }
            }
            default -> {
            }
        }
    }

    public static void handleOpenAuction(OpenAuctionScreenPacket packet) {
        // Wave490: prefer productized hall; legacy AuctionScreen only if no container open.
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AuctionHallScreen) && !(mc.screen instanceof AuctionScreen)) {
            mc.setScreen(new AuctionScreen());
        }
    }

    public static void handleOpenDialogue(OpenDialogueScreenPacket packet) {
        Minecraft.getInstance().setScreen(new DialogueScreen(packet));
    }

    public static void handleOpenAlchemyStatus(OpenAlchemyStatusPacket packet) {
        Minecraft.getInstance().setScreen(new AlchemyStatusScreen(
                packet.skillLevel(), packet.skillExp(), packet.message()));
    }

    public static void handleOpenStoragePreview(OpenStoragePreviewPacket packet) {
        Minecraft.getInstance().setScreen(new StorageBraceletScreen(packet.lines()));
    }

    public static void handleOpenRefinePlan(OpenRefinePlanPacket packet) {
        Minecraft.getInstance().setScreen(new RefinementPlanScreen(packet.lines()));
    }
}
