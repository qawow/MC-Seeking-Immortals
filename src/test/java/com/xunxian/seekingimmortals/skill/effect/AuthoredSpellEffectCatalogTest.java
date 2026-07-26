package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.spell.AuthoredSpellEffect;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredSpellEffectCatalogTest {
    @Test
    void completeCatalogIsTypedUniqueAndRuntimeReachable() {
        AuthoredSpellEffectCatalog.Snapshot catalog = AuthoredSpellEffectCatalog.builtin();

        assertTrue(catalog.valid(), catalog.invalidRows().toString());
        assertEquals(747, catalog.counts().corpus());
        assertTrue(catalog.counts().novel() >= 1308, "novel=" + catalog.counts().novel());
        assertEquals(catalog.counts().corpus() + catalog.counts().novel(), catalog.counts().total());
        assertEquals(catalog.counts().total(), catalog.profiles().size());
        assertEquals(catalog.counts().total(), catalog.counts().uniqueVisualSignatures());
        assertEquals(catalog.profiles().size(), catalog.profiles().values().stream()
                .map(AuthoredSpellEffectCatalog.Profile::visualSignature).collect(java.util.stream.Collectors.toSet()).size());
        assertTrue(TechniqueDataManager.builtinTechniques().keySet().containsAll(catalog.profiles().keySet()));
    }

    @Test
    void representativeNovelEffectsKeepAuthoredSilhouetteAndFunction() {
        AuthoredSpellEffectCatalog.Profile ghostClaw = AuthoredSpellEffectCatalog.find("technique_1301").orElseThrow();
        assertEquals("novel", ghostClaw.namespace());
        assertEquals(TechniqueVfxPalette.Family.FIRE, ghostClaw.family());
        assertEquals(TechniqueVfxPacket.Motif.GHOST, ghostClaw.motif());
        assertEquals("giant_claw", ghostClaw.shape());
        assertEquals("soul_attack", ghostClaw.functional().type());
        assertEquals("area", ghostClaw.functional().target());
        assertTrue(ghostClaw.frames().size() >= 5);

        AuthoredSpellEffectCatalog.Profile runeField = AuthoredSpellEffectCatalog.find("technique_1310").orElseThrow();
        assertEquals(TechniqueVfxPalette.Family.THUNDER, runeField.family());
        assertEquals(TechniqueVfxPacket.Motif.FORMATION, runeField.motif());
        assertEquals("rune_orbit", runeField.shape());
        assertEquals("field", runeField.functional().type());
        assertTrue(runeField.functional().damageBase() > 0.0D);
    }

    @Test
    void resolverNeverFallsBackToLegacyGenericTemplatesForCatalogRows() {
        for (String id : new HashSet<>(AuthoredSpellEffectCatalog.profiles().keySet())) {
            TechniqueDataManager.TechniqueEntry technique = TechniqueDataManager.builtinTechniques().get(id);
            assertInstanceOf(AuthoredSpellEffect.class, AbstractTechniqueEffectResolver.resolve(technique), id);
        }
    }
}
