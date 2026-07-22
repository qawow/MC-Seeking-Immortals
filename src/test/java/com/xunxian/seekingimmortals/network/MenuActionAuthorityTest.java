package com.xunxian.seekingimmortals.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuActionAuthorityTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void accessContextBindsDimensionDistanceEntityAndNonce() throws Exception {
        String source = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("menu", "MenuAccessContext.java"))));

        assertTrue(source.contains("ResourceKey<Level>dimension"));
        assertTrue(source.contains("UUIDsourceEntityId"));
        assertTrue(source.contains("serverPlayer.serverLevel().getEntity(sourceEntityId)"));
        assertTrue(source.contains("source.distanceToSqr(anchor)<=MAX_DISTANCE_SQR"));
        assertTrue(source.contains("serverPlayer.distanceToSqr(anchor)<=MAX_DISTANCE_SQR"));
        assertTrue(source.contains("serverPlayer.distanceToSqr(source)<=MAX_DISTANCE_SQR"));
        assertTrue(source.contains("token!=0L&&token==presentedToken"));

        String atEntity = compact(methodSource(Files.readString(JAVA_ROOT.resolve(
                Path.of("menu", "MenuAccessContext.java"))), "public static MenuAccessContext atEntity("));
        assertTrue(atEntity.contains("returndenied()"));
        assertFalse(atEntity.contains("returnatPlayer("), "invalid entity contexts must fail closed");
    }

    @Test
    void mutationHandlersGateOnCurrentMenuBeforeEffects() throws Exception {
        String shop = compact(methodSource(Files.readString(JAVA_ROOT.resolve(
                Path.of("shop", "ShopService.java"))), "public static void handleClientAction("));
        assertOrdered(shop, "player.containerMenuinstanceofMarketHallMenumenu", "buyWithItemCurrency(");
        assertTrue(shop.contains("menu.authorizes(player,normalizedShop,accessToken)"));

        String auction = compact(methodSource(Files.readString(JAVA_ROOT.resolve(
                Path.of("network", "AuctionActionPacket.java"))), "public static void handle("));
        assertOrdered(auction, "player.containerMenuinstanceofAuctionHallMenumenu", "switch(action)");
        assertTrue(auction.contains("menu.authorizes(player,packet.accessToken)"));

        String sect = compact(methodSource(Files.readString(JAVA_ROOT.resolve(
                Path.of("sect", "SectContributionService.java"))), "public static void handleClientAction("));
        assertOrdered(sect, "player.containerMenuinstanceofSectHallMenumenu", "switch(normalizedAction)");
        assertTrue(sect.contains("menu.authorizes(player,accessToken)"));
        assertTrue(sect.contains("menu.authorizesSect("));
        assertOrdered(sect, "requiresFocusedSect(normalizedAction)", "switch(normalizedAction)");
        assertFalse(sect.contains("openScreen("), "menu actions must retain their original access context");
    }

    @Test
    void sectMemberActionsStayBoundToTheInteractedSteward() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(
                Path.of("sect", "SectContributionService.java")));
        String focusedActions = compact(methodSource(source, "private static boolean requiresFocusedSect("));
        assertTrue(focusedActions.contains("ACTION_DIALOGUE.equals(action)"));
        assertTrue(focusedActions.contains("ACTION_ADVANCE.equals(action)"));
        assertTrue(focusedActions.contains("ACTION_ACCEPT_MISSION.equals(action)"));
        assertTrue(focusedActions.contains("ACTION_TURN_IN_MISSION.equals(action)"));
        assertTrue(focusedActions.contains("ACTION_BUY.equals(action)"));
        assertTrue(focusedActions.contains("ACTION_DONATE_SPIRIT_GRASS.equals(action)"));

        String authorization = compact(methodSource(source,
                "public static boolean authorizeStewardInteraction("));
        String foreignSectGate = "!definition.id().equals(normalize(progress.getSectId()))";
        int foreignSectGateIndex = authorization.indexOf(foreignSectGate);
        assertTrue(foreignSectGateIndex >= 0, "missing foreign-sect steward gate");
        assertOrdered(authorization.substring(foreignSectGateIndex), foreignSectGate, "returnfalse;");
        assertTrue(authorization.contains("message.seeking_immortals.sect.other_sect"));

        String interaction = compact(methodSource(source,
                "private static boolean handleStewardInteraction("));
        assertOrdered(interaction, "!authorizeStewardInteraction(player,definition.id())", "openScreen(");
        assertTrue(interaction.contains("returntrue;"));
    }

    @Test
    void playerFacingMenusSendOnlyTheirIssuedTokenAndTarget() throws Exception {
        String market = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("client", "MarketHallScreen.java"))));
        assertTrue(market.contains("returnList.of(menu.shopId())"));
        assertTrue(market.contains("menu.accessToken()"));

        String auction = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("client", "AuctionHallScreen.java"))));
        assertTrue(auction.contains("menu.accessToken()"));

        String sect = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("client", "SectHallScreen.java"))));
        assertTrue(sect.contains("menu.accessToken()"));
        assertTrue(sect.contains("menu.focusSectId().equals(candidate.id())"));
    }

    @Test
    void npcOpenedMenusRetainTheInteractedEntity() throws Exception {
        String trader = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("entity", "MarketTraderEntity.java"))));
        assertTrue(trader.contains("startDialogue(player,npc,dialogueTreeId,this)"));
        assertTrue(trader.contains("ShopService.openMarket(player,shop,this)"));

        String rawSteward = Files.readString(JAVA_ROOT.resolve(Path.of("entity", "SectStewardEntity.java")));
        String steward = compact(rawSteward);
        assertTrue(steward.contains("startDialogue(player,npcId,treeId,this)"));
        String stewardDialogue = compact(methodSource(rawSteward,
                "public boolean openDialogue(ServerPlayer player)"));
        assertFalse(stewardDialogue.contains("authorizeStewardInteraction("),
                "named-NPC dialogue conditions must run before sect business authorization");
        assertTrue(stewardDialogue.contains("return!treeId.isBlank()&&NpcDialogueApi.startDialogue("),
                "a missing authored tree must fall back to sect business instead of a dialogue template");
        assertOrdered(stewardDialogue, "StringnpcId=namedNpcId",
                "startDialogue(player,npcId,treeId,this)");

        String dialogue = compact(Files.readString(JAVA_ROOT.resolve(
                Path.of("npc", "NpcDialogueApi.java"))));
        assertTrue(dialogue.contains("SourceEntityId"));
        assertTrue(dialogue.contains("currentSourceEntity("));
        assertTrue(dialogue.contains("distanceSqr(source.getX(),source.getY(),source.getZ(),session.anchorX()"));
        assertTrue(dialogue.contains("distanceSqr(player.getX(),player.getY(),player.getZ(),session.anchorX()"));

        String rawDialogue = Files.readString(JAVA_ROOT.resolve(Path.of("npc", "NpcDialogueApi.java")));
        String entityStart = compact(methodSource(rawDialogue,
                "public static boolean startDialogue(ServerPlayer player, String npcId, String treeId, Entity source)"));
        assertTrue(entityStart.contains("source==null||!source.isAlive()||source.level()!=player.level()"));
        assertFalse(entityStart.contains("captureAnchor(player)"),
                "an invalid explicit source must not degrade to a player anchor");

        String playerAnchor = compact(methodSource(rawDialogue,
                "private static Anchor captureAnchor(ServerPlayer player)"));
        assertTrue(playerAnchor.contains("player.getX()"));
        assertTrue(playerAnchor.contains("player.getY()"));
        assertTrue(playerAnchor.contains("player.getZ()"));
        assertFalse(playerAnchor.contains("captureAnchor(player,null)"),
                "ordinary dialogue must not dereference a missing entity source");
    }

    private static void assertOrdered(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "missing authority gate: " + first);
        assertTrue(secondIndex > firstIndex, "effect must occur after authority gate: " + second);
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("unterminated source method: " + declaration);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
