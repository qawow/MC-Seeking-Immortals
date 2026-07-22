package com.xunxian.seekingimmortals.event;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionBeastAuthorityContractTest {
    private static final Path EVENTS = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals", "event", "ModEvents.java");
    private static final Path BEAST = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals", "entity",
            "CultivationBeastEntity.java");

    @Test
    void companionBeastsShareSecretRealmDamageAndKillAuthorityWithTheirOwner() throws Exception {
        String source = Files.readString(EVENTS);
        String resolver = methodSource(source, "private static ServerPlayer resolveCombatAuthorityPlayer(");
        String controlled = methodSource(source, "private static boolean isPlayerControlledCombatSource(");
        String hurt = methodSource(source, "public static void onLivingHurt(");
        String drops = methodSource(source, "private static void handleCommittedLivingDrops(");

        assertTrue(resolver.contains("source instanceof CultivationBeastEntity beast && beast.isCompanion()"));
        assertTrue(resolver.contains("beast.getOwnerUUID()"));
        assertTrue(controlled.contains("source instanceof CultivationBeastEntity beast && beast.isCompanion()"));
        assertTrue(hurt.contains("resolveCombatAuthorityPlayer(authoritySource)"));
        assertTrue(drops.contains("resolveCombatAuthorityPlayer(event.getSource().getEntity())"));
    }

    @Test
    void elementalDamageOverTimeRetainsCompanionOwnerKillAuthority() throws Exception {
        String beast = Files.readString(BEAST);
        String events = Files.readString(EVENTS);
        String elemental = methodSource(beast, "private void applyElementalEffect(");
        String recentOwner = methodSource(beast,
                "public static Optional<ServerPlayer> recentCompanionDamageOwner(");
        String drops = methodSource(events, "private static void handleCommittedLivingDrops(");

        assertTrue(elemental.contains("if (applied && isCompanion() && ownerUUID != null)"));
        assertTrue(elemental.contains("target.getPersistentData().putUUID(TAG_COMPANION_DAMAGE_OWNER, ownerUUID)"));
        assertTrue(elemental.contains("TAG_COMPANION_DAMAGE_EXPIRY, target.level().getGameTime() + duration + 20L"));

        assertTrue(recentOwner.contains("data.hasUUID(TAG_COMPANION_DAMAGE_OWNER)"));
        assertTrue(recentOwner.contains("data.getLong(TAG_COMPANION_DAMAGE_EXPIRY) < level.getGameTime()"));
        assertTrue(recentOwner.contains("level.getPlayerByUUID(data.getUUID(TAG_COMPANION_DAMAGE_OWNER))"));

        int directOwner = drops.indexOf("resolveCombatAuthorityPlayer(event.getSource().getEntity())");
        int missingOwnerGuard = drops.indexOf("if (combatOwner == null");
        int statusOwnerFallback = drops.indexOf(
                "CultivationBeastEntity.recentCompanionDamageOwner(event.getEntity())");
        int rewardRouting = drops.indexOf("if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob");
        assertTrue(directOwner >= 0 && missingOwnerGuard > directOwner);
        assertTrue(statusOwnerFallback > missingOwnerGuard && rewardRouting > statusOwnerFallback,
                "status damage must resolve its companion owner before kill rewards are routed");
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
}
