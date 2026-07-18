package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmEntryAuthorityTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void clientAndNormalCommandsCannotCallDirectRealmEntry() throws Exception {
        String gameplay = Files.readString(JAVA_ROOT.resolve(Path.of("worldpack", "WorldpackGameplayService.java")));
        String actionCase = gameplay.substring(gameplay.indexOf("case ACTION_ENTER"),
                gameplay.indexOf("case ACTION_RETURN"));
        assertFalse(actionCase.contains("enterSecretRealm("));
        assertTrue(actionCase.contains("gate_required"));

        String screen = Files.readString(JAVA_ROOT.resolve(Path.of("client", "WorldpackScreen.java")));
        assertFalse(screen.contains("WorldpackGameplayService.ACTION_ENTER, realm.id()"));

        String commands = Files.readString(JAVA_ROOT.resolve(Path.of("command", "SeekingImmortalsCommand.java")));
        assertTrue(commands.contains("Commands.literal(\"enter\").requires(source -> source.hasPermission(2))"));
        assertTrue(commands.contains("Commands.literal(\"travel\").requires(source -> source.hasPermission(2))"));
    }

    @Test
    void teleportAndGateCostsUseCommitAwarePaths() throws Exception {
        String dimensions = Files.readString(JAVA_ROOT.resolve(Path.of("worldpack", "SecretRealmDimensionService.java")));
        assertTrue(dimensions.contains("return player.serverLevel() == target"));

        String gameplay = Files.readString(JAVA_ROOT.resolve(Path.of("worldpack", "WorldpackGameplayService.java")));
        assertTrue(gameplay.contains("enter_teleport_failed"));
        assertTrue(gameplay.contains("return_teleport_failed"));
        assertTrue(gameplay.contains("reservation.refund(player)"));
        assertTrue(gameplay.contains("TravelCostReservation travelCosts = reserveTravelCosts"));
        assertTrue(gameplay.contains("travelCosts.refund(player)"));
        assertTrue(gameplay.contains("boolean dedicatedDimension = SecretRealmDimensionService.hasDedicatedDimension"));
        assertTrue(gameplay.contains("dedicatedDimension\n                    ? SecretRealmDimensionService.teleportInto"));

        String sync = gameplay.substring(gameplay.indexOf("public static void sync(ServerPlayer"),
                gameplay.indexOf("public static void syncSnapshot"));
        assertFalse(sync.contains("prepareSavedData"));

        String bound = gameplay.substring(gameplay.indexOf("public static boolean enterBoundRealmOr"),
                gameplay.indexOf("public static boolean setAnchor"));
        assertTrue(bound.contains("return enterSecretRealm(player, bound.get().id())"));
        assertFalse(bound.contains("travel(player, def.regionId()"));

        String requires = Files.readString(JAVA_ROOT.resolve(Path.of("worldpack", "SpatialNodeRequiresService.java")));
        assertTrue(requires.contains("public static Reservation reserveByType"));
        assertTrue(requires.contains("InventoryReservation.consume(player, costs)"));
        assertTrue(requires.contains("if (refunded)"));

        String inventory = Files.readString(JAVA_ROOT.resolve(Path.of("worldpack", "InventoryReservation.java")));
        assertTrue(inventory.contains("consumed.add(copyForReservation(stack, take))"));
        assertTrue(inventory.contains("ItemStack remainder = consumedStack.copy()"));
    }

    @Test
    void servitorGoalRegistrationIsConstructorSafe() throws Exception {
        String servitor = Files.readString(JAVA_ROOT.resolve(Path.of("entity", "SummonedServitorEntity.java")));
        String goals = servitor.substring(servitor.indexOf("protected void registerGoals()"),
                servitor.indexOf("public void tick()"));
        assertTrue(goals.contains("archetype == null ? Archetype.GENERIC : archetype"));
        assertFalse(goals.contains("switch (archetype)"));
    }
}
