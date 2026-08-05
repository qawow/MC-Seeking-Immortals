package com.xunxian.seekingimmortals.artifact;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M-B: natal binding is an <em>instance</em> relationship, not an artifact-id relationship.
 *
 * <p>Before this batch every benefit test was {@code artifact.id().equals(boundId(player))}, so a
 * second copy of the same artifact id inherited the full natal discount — cooldown cut, spiritual
 * cost cut, integrity cut and growth — without ever being bound. Binding also happened silently as
 * a side effect of claiming ownership, so the first artifact a player claimed became their one
 * natal artifact whether they meant it or not.</p>
 */
class NatalInstanceBindingTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void claimingOwnershipNoLongerSilentlyBindsANatalArtifact() throws Exception {
        String ownership = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactOwnershipService.java"));
        String claim = compact(methodSource(ownership, "public static boolean claim("));

        assertFalse(claim.contains("NatalBindingService.bind("),
                "claiming must only record ownership; binding is a deliberate two-hand ritual");
        // Ownership itself must still be written, and still refuse someone else's artifact.
        assertTrue(claim.contains("tag.putUUID(OWNER_UUID_TAG,player.getUUID())"));
        assertTrue(claim.contains("claim.owned_by_other"));
    }

    @Test
    void bindingRecordsInstanceIdentityAndSchemaVersion() throws Exception {
        String natal = Files.readString(JAVA_ROOT.resolve("artifact/NatalBindingService.java"));

        // The natal root is now (artifact id + instance uuid + schema version + growth).
        assertTrue(natal.contains("KEY_INSTANCE") && natal.contains("KEY_SCHEMA"),
                "the natal root must record the bound instance and its schema version");
        assertEquals(2, NatalBindingService.SCHEMA_VERSION,
                "schema 1 was id-only; instance binding is schema 2");
        assertTrue(natal.contains("STACK_INSTANCE"),
                "the target stack must carry the same instance id");
        assertTrue(natal.contains("UUID.randomUUID()"),
                "each binding mints a fresh instance id");
    }

    @Test
    void everyBenefitSiteResolvesTheExactInstanceNotJustTheArtifactId() throws Exception {
        String activation = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactActivationService.java"));
        String activeSkill = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactActiveSkillService.java"));
        String refinement = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactRefinementService.java"));

        for (String source : new String[]{activation, activeSkill}) {
            assertFalse(compact(source).contains(".id().equals(NatalBindingService.boundId(player))"),
                    "an id comparison lets a second copy of the same artifact inherit natal benefits");
        }

        // Cost/cooldown/growth all key off the held instance.
        String compactActivation = compact(activation);
        assertTrue(compactActivation.contains("NatalBindingService.isBoundInstance(player,stack)"),
                "activation growth must require the bound instance");
        assertTrue(compactActivation.contains("effectiveIntegrityCost(player,stack,artifact,info)")
                        && compactActivation.contains("effectiveSpiritualCost(player,stack,artifact,info)")
                        && compactActivation.contains("effectiveCooldown(player,stack,artifact,info)"),
                "the three discounts must receive the stack they are discounting");
        assertTrue(compact(activeSkill).contains("NatalBindingService.isBoundInstance(player,stack)"));

        // Refinement mints a brand-new stack, so id equality there was the worst case: forging a
        // fresh copy grew the natal instance sitting in the player's bag.
        assertTrue(compact(refinement).contains("NatalBindingService.holdsBoundInstanceOf(player,recipe.artifactId())"),
                "refinement growth must require the player to actually carry the bound instance");
    }

    @Test
    void instanceResolutionFailsClosedOnCopiedOrForeignStacks() throws Exception {
        String natal = Files.readString(JAVA_ROOT.resolve("artifact/NatalBindingService.java"));
        String resolve = compact(methodSource(natal, "public static boolean isBoundInstance("));

        // A duplicated NBT stack carries the same instance id, so ownership must also match.
        assertTrue(resolve.contains("ArtifactOwnershipService.ownerUuid(stack)"),
                "a copied instance tag must not work in someone else's hands");
        // Blank/missing instance ids must never match a blank root value.
        assertTrue(resolve.contains("isBlank()"),
                "a blank instance id must never compare equal");
        assertTrue(resolve.contains("artifactId()"),
                "the artifact id must still agree with the bound id");
    }

    @Test
    void twoHandBindingIsAtomicAndGatedOnRealmAndOwnership() throws Exception {
        String natal = Files.readString(JAVA_ROOT.resolve("artifact/NatalBindingService.java"));
        String bind = compact(methodSource(natal, "public static boolean bindWithEmbryo("));

        // Authored gate: 结丹后择一飞剑为本命 (natal_sword_embryo realm_min CORE_FORMATION).
        assertTrue(bind.contains("Realm.CORE_FORMATION"),
                "binding must keep the authored core-formation threshold");
        assertTrue(bind.contains("EMBRYO_ITEM_ID") || bind.contains("natal_sword_embryo"),
                "the embryo is the ritual component");
        // The embryo may only be consumed once every rejection path has already returned.
        int ownerCheck = bind.indexOf("ownerUuid(");
        int alreadyBound = bind.indexOf("KEY_INSTANCE");
        int consume = bind.indexOf("embryo.shrink(1)");
        assertTrue(ownerCheck >= 0 && consume > ownerCheck,
                "ownership must be verified before the embryo is spent");
        assertTrue(alreadyBound >= 0 && consume > alreadyBound,
                "an existing natal binding must be rejected before the embryo is spent");
        assertFalse(bind.contains("instabuild") && bind.indexOf("instabuild") > consume,
                "creative checks must not run after the consume");
    }

    @Test
    void legacyIdOnlyBindingsAreMigratedOnlyWhenUnambiguous() throws Exception {
        String natal = Files.readString(JAVA_ROOT.resolve("artifact/NatalBindingService.java"));

        assertTrue(natal.contains("isLegacyBinding("),
                "old saves stored ArtifactId/Growth with no instance; that state must be detectable");
        String migrate = compact(methodSource(natal, "public static MigrationResult migrateLegacyBinding("));

        // One matching artifact in the inventory: adopt it. Several: refuse and ask for the
        // embryo, because picking one for the player could silently pick the wrong sword.
        assertTrue(migrate.contains("candidates.size()==1") || migrate.contains("candidates.size()!=1"),
                "migration must branch on candidate uniqueness");
        assertTrue(natal.contains("AMBIGUOUS"),
                "multiple candidates must report an ambiguous outcome, not guess");
        assertTrue(migrate.contains("KEY_SCHEMA"),
                "a migrated binding must be stamped with the new schema version");

        // Growth carries over: migration must not silently reset a long-grown natal artifact.
        assertTrue(migrate.contains("KEY_GROWTH"),
                "existing growth must survive migration");
    }

    @Test
    void adminDiagnosticIsPermissionGatedAndReadOnlyByDefault() throws Exception {
        String command = Files.readString(JAVA_ROOT.resolve("command/SeekingImmortalsCommand.java"));
        assertTrue(command.contains("natalDiagnose("),
                "operators need a way to inspect a stuck legacy binding");
        String registration = compact(command);
        assertTrue(registration.contains("Commands.literal(\"diagnose\").requires(source->source.hasPermission(2))"),
                "the diagnostic must be operator-only");
        // bind/grow were already operator-only; that must not regress.
        assertTrue(registration.contains("Commands.literal(\"bind\").requires(source->source.hasPermission(2))"));
        assertTrue(registration.contains("Commands.literal(\"grow\").requires(source->source.hasPermission(2))"));
    }

    @Test
    void tooltipDescribesTheRealTwoHandRitualAndNotASneakClick() throws Exception {
        String item = Files.readString(JAVA_ROOT.resolve("item/ArtifactCatalogItem.java"));
        assertTrue(item.contains("natal.bind_hint_embryo"),
                "the hint must describe the embryo ritual, not the old sneak-to-bind claim");

        // A bound stack shows its own state; an unbound copy of the same id must not.
        assertTrue(item.contains("NatalBindingService.STACK_INSTANCE"),
                "the bound marker must be read from the instance tag");

        // hasActivation must stay a data question, never a natal-slot activation claim.
        String activation = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactActivationService.java"));
        assertTrue(activation.contains("case \"natal_slot\" -> true"),
                "natal_slot must remain a deferred activation type");
        assertFalse(ArtifactActivationService.hasActivation("natal_sword_embryo"),
                "the embryo is a binding component, not an activatable treasure");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int closingBrace = matchingDelimiter(source, openingBrace);
        return source.substring(start, closingBrace + 1);
    }

    private static int matchingDelimiter(String source, int openingBrace) {
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("unterminated method body");
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
