package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.skill.effect.AuthoredSpellEffectCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredSpellRuntimeContractTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void everyMechanicsEnumIsUsedByTheFullCatalog() {
        Set<AuthoredSpellEffectCatalog.Operation> operations = AuthoredSpellEffectCatalog.profiles().values().stream()
                .map(profile -> profile.mechanics().operation()).collect(Collectors.toSet());
        Set<AuthoredSpellEffectCatalog.Delivery> deliveries = AuthoredSpellEffectCatalog.profiles().values().stream()
                .map(profile -> profile.mechanics().delivery()).collect(Collectors.toSet());

        assertEquals(EnumSet.allOf(AuthoredSpellEffectCatalog.Operation.class), operations);
        assertEquals(EnumSet.allOf(AuthoredSpellEffectCatalog.Delivery.class), deliveries);
        assertTrue(AuthoredSpellEffectCatalog.profiles().size() >= 2187);
    }

    @Test
    void executorHasExplicitBranchesForEveryOperationAndRealProjectiles() throws IOException {
        String source = compact(read("skill", "effect", "spell", "AuthoredSpellEffect.java"));

        assertTrue(source.contains("caseRESTORE,RESTORE_SPIRIT,CLEANSE->executeRecovery"));
        assertTrue(source.contains("caseMOVE->executeMovement"));
        assertTrue(source.contains("caseSUMMON->executeSummon"));
        assertTrue(source.contains("caseCOMMAND->executeCommand"));
        assertTrue(source.contains("caseDEFEND->executeDefend"));
        assertTrue(source.contains("caseDETECT->executeDetection"));
        assertTrue(source.contains("caseCONCEAL->executeConcealment"));
        assertTrue(source.contains("caseTRANSFORM->executeTransformation"));
        assertTrue(source.contains("caseTERRAIN->executeTerrain"));
        assertTrue(source.contains("caseDRAIN->executeDrain"));
        assertTrue(source.contains("caseCRAFT->executeCraftingFocus"));
        assertTrue(source.contains("caseCULTIVATE->executeCultivationSupport"));
        assertTrue(source.contains("caseCOMMUNICATE->executeCommunication"));
        assertTrue(source.contains("caseATTACK,SEAL->"));
        assertTrue(source.contains("newSwordProjectileEntity"));
        assertTrue(source.contains("newCultivationFireballEntity"));
        assertTrue(source.contains("profile.mechanics().maxTargets()"));
        assertTrue(source.contains("level.scheduleTick(pos,ModBlocks.EARTH_WALL.get(),removalTicks)"));
        assertTrue(source.contains("servitor.getOwnerUUID().filter(caster.getUUID()::equals)"));
        assertTrue(source.contains("beast.getOwnerUUID().filter(caster.getUUID()::equals)"));
        assertFalse(source.contains("AreaEffectCloud"));
    }

    @Test
    void fieldsAndProjectilesEnforceOwnershipAndPvpSafety() throws IOException {
        String fields = compact(read("skill", "effect", "spell", "AuthoredSpellFieldService.java"));
        String fireball = compact(read("entity", "CultivationFireballEntity.java"));
        String sword = compact(read("entity", "SwordProjectileEntity.java"));
        String events = compact(read("event", "ModEvents.java"));

        assertTrue(fields.contains("caster.canHarmPlayer(player)"));
        assertTrue(fields.contains("servitor.getOwnerUUID()"));
        assertTrue(fields.contains("beast.getOwnerUUID()"));
        assertTrue(fields.contains("MAX_FIELDS_PER_SERVER=128"));
        assertTrue(events.contains("AuthoredSpellFieldService.serverTick(event.getServer())"));
        assertTrue(events.contains("AuthoredSpellFieldService.clearAll()"));
        assertTrue(events.contains("AuthoredSpellFieldService.clearLevel(serverLevel)"));

        assertTrue(fireball.contains("protectedbooleancanHitEntity(Entitytarget)"));
        assertTrue(fireball.contains("caster.canHarmPlayer(playerTarget)"));
        assertTrue(fireball.contains("livingOwner.isAlliedTo(target)"));
        assertTrue(fireball.contains("servitor.getOwnerUUID().filter(caster.getUUID()::equals)"));
        assertTrue(fireball.contains("entity!=directTarget&&canDamageTarget(entity)"));
        assertTrue(sword.contains("protectedbooleancanHitEntity(Entitytarget)"));
        assertTrue(sword.contains("caster.canHarmPlayer(playerTarget)"));
        assertTrue(sword.contains("livingOwner.isAlliedTo(target)"));
        assertTrue(sword.contains("beast.getOwnerUUID().filter(caster.getUUID()::equals)"));
    }

    private static String read(String... path) throws IOException {
        return Files.readString(JAVA_ROOT.resolve(Path.of("", path)));
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
