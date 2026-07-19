package com.xunxian.seekingimmortals.combat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSourceAuthorityTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void playerSpellAndArtifactDamageRetainsCasterAttribution() throws Exception {
        Path spellRoot = JAVA_ROOT.resolve(Path.of("skill", "effect", "spell"));
        List<Path> spellFiles;
        try (var paths = Files.walk(spellRoot)) {
            spellFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        int spellHits = 0;
        for (Path path : spellFiles) {
            String source = Files.readString(path);
            assertFalse(source.contains("damageSources().magic()"), path.toString());
            int fileHits = count(source, "player.damageSources().indirectMagic(player, player)");
            spellHits += fileHits;
            if (fileHits > 0) {
                assertTrue(source.contains("canAffect(player,"), path + " must honor server PvP targeting");
            }
        }

        String spellBase = Files.readString(spellRoot.resolve("SpellEffect.java"));
        assertTrue(spellBase.contains("caster.canHarmPlayer(targetPlayer)"));

        Path activation = JAVA_ROOT.resolve(Path.of("artifact", "ArtifactActivationService.java"));
        String activationSource = Files.readString(activation);
        assertFalse(activationSource.contains("damageSources().magic()"));
        int artifactHits = count(activationSource, "player.damageSources().indirectMagic(player, player)");
        assertTrue(spellHits == 47, "expected 47 caster-aware spell damage sites, got " + spellHits);
        assertTrue(artifactHits == 9, "expected 9 caster-aware artifact damage sites, got " + artifactHits);

        String events = Files.readString(JAVA_ROOT.resolve(Path.of("event", "ModEvents.java")));
        assertFalse(events.contains("SeekingImmortalsProjectileDamage"));
    }

    @Test
    void customProjectilesPersistTheirBaseProjectileState() throws Exception {
        for (String name : List.of("CultivationFireballEntity.java", "SwordProjectileEntity.java")) {
            String source = Files.readString(JAVA_ROOT.resolve(Path.of("entity", name)));
            assertTrue(source.contains("super.readAdditionalSaveData(tag);"), name);
            assertTrue(source.contains("super.addAdditionalSaveData(tag);"), name);
            assertTrue(source.contains("boolean damaged = target.hurt"), name);
        }
    }

    private static int count(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
