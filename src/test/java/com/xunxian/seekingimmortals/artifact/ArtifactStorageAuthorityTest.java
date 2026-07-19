package com.xunxian.seekingimmortals.artifact;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactStorageAuthorityTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path STORAGE_SOURCE = JAVA_ROOT.resolve(
            Path.of("artifact", "ArtifactStorageService.java"));
    private static final Path MENU_SOURCE = JAVA_ROOT.resolve(
            Path.of("menu", "StorageBraceletMenu.java"));

    @Test
    void forbiddenContainerTruthTableRejectsEveryNestedStorageKind() {
        for (int mask = 0; mask < 16; mask++) {
            boolean storageArtifact = (mask & 1) != 0;
            boolean shulker = (mask & 2) != 0;
            boolean bundle = (mask & 4) != 0;
            boolean itemHandler = (mask & 8) != 0;
            boolean expected = storageArtifact || shulker || bundle || itemHandler;

            assertEquals(expected, ArtifactStorageService.isForbiddenContainer(
                            storageArtifact, shulker, bundle, itemHandler),
                    "storageArtifact=" + storageArtifact
                            + ", shulker=" + shulker
                            + ", bundle=" + bundle
                            + ", itemHandler=" + itemHandler);
        }
    }

    @Test
    void storageRejectsOnlyOverflowingPositiveCapacities() {
        assertTrue(ArtifactStorageService.isStorageCountValid(0, 9));
        assertTrue(ArtifactStorageService.isStorageCountValid(9, 9));
        assertFalse(ArtifactStorageService.isStorageCountValid(10, 9));
        assertFalse(ArtifactStorageService.isStorageCountValid(1, 0));
    }

    @Test
    void menuKeepsAuthorityBoundToTheOpeningBracelet() throws Exception {
        String source = Files.readString(MENU_SOURCE);
        String removed = compact(methodSource(source, "public void removed("));
        assertNoCurrentHandLookup(removed, "removed");
        assertTrue(removed.contains("ArtifactStorageService.writeHandler(boundBracelet,handler)"),
                "removed must flush the handler to the bracelet captured at menu creation");

        String quickMove = compact(methodSource(source, "public ItemStack quickMoveStack("));
        assertNoCurrentHandLookup(quickMove, "quickMoveStack");
        int failedMoveReturn = quickMove.lastIndexOf("returnItemStack.EMPTY;");
        int quickMoveWrite = quickMove.indexOf(
                "ArtifactStorageService.writeHandler(boundBracelet,handler)");
        int successfulReturn = quickMove.lastIndexOf("returnresult;");
        assertTrue(quickMoveWrite > failedMoveReturn && successfulReturn > quickMoveWrite,
                "a successful quick move must flush to boundBracelet before returning");

        String stillValid = compact(methodSource(source, "public boolean stillValid("));
        assertTrue(stillValid.contains("boundBracelet"),
                "stillValid must retain the exact bracelet used to open the menu");
        assertTrue(Pattern.compile("(?:this\\.)?boundBracelet==|==(?:this\\.)?boundBracelet")
                        .matcher(stillValid).find(),
                "stillValid must compare boundBracelet by object identity");

        String clicked = compact(methodSource(source, "public void clicked("));
        int swapCheck = clicked.indexOf("ClickType.SWAP");
        int blockedReturn = clicked.indexOf("return;", swapCheck);
        int delegatedClick = clicked.indexOf("super.clicked(");
        assertTrue(swapCheck >= 0, "clicked must inspect SWAP clicks");
        assertTrue(clicked.contains("hand==InteractionHand.MAIN_HAND")
                        && clicked.contains("hand==InteractionHand.OFF_HAND"),
                "the SWAP rejection must identify the bound hand");
        assertTrue(blockedReturn > swapCheck && delegatedClick > blockedReturn,
                "the bound-hand SWAP must not reach the delegated click");
    }

    @Test
    void handlerValidatesInsertsAndPersistsMutationsToBoundBracelet() throws Exception {
        String source = Files.readString(STORAGE_SOURCE);
        String itemValid = compact(methodSource(source, "public boolean isItemValid("));
        assertTrue(itemValid.contains("canStore(stack)"),
                "handler insertion validation must delegate to canStore");

        String contentsChanged = compact(methodSource(source, "protected void onContentsChanged("));
        assertTrue(Pattern.compile("(?:ArtifactStorageService\\.)?writeHandler\\("
                        + "(?:this\\.)?(?:bracelet|boundBracelet),this\\)")
                        .matcher(contentsChanged).find(),
                "handler mutations must write back to the bracelet captured at creation");
    }

    @Test
    void storageUseRequiresOwnershipAuthorityAndPositiveIntegrity() throws Exception {
        String source = Files.readString(STORAGE_SOURCE);
        String use = compact(methodSource(source, "public static boolean use("));
        String ownershipGate = "if(!ArtifactOwnershipService.canActivate("
                + "player,braceletStack,artifact.id()))";
        String integrityRead = "ArtifactActivationService.getIntegrity(braceletStack,artifact)";
        int ownershipCheck = use.indexOf(ownershipGate);
        int integrityCheck = use.indexOf(integrityRead);
        int openScreen = use.indexOf("NetworkHooks.openScreen(");
        int ownershipDenial = use.indexOf("returnfalse;", ownershipCheck);
        int integrityDenial = use.indexOf("returnfalse;", integrityCheck);

        assertTrue(ownershipCheck >= 0,
                "storage use must deny activation through ArtifactOwnershipService");
        assertTrue(integrityCheck >= 0 && hasZeroGate(use, integrityRead),
                "storage use must reject a bracelet with zero integrity");
        assertTrue(openScreen > ownershipCheck && openScreen > integrityCheck,
                "ownership and integrity gates must run before opening storage");
        assertTrue(ownershipDenial > ownershipCheck && ownershipDenial < openScreen,
                "failed ownership must return before opening storage");
        assertTrue(integrityDenial > integrityCheck && integrityDenial < openScreen,
                "zero integrity must return before opening storage");
    }

    private static boolean hasZeroGate(String source, String integrityRead) {
        if (source.contains(integrityRead + "<=0")
                || source.contains(integrityRead + "==0")) {
            return true;
        }
        String localGate = "(?:int|var)([A-Za-z_$][A-Za-z0-9_$]*)="
                + Pattern.quote(integrityRead)
                + ";if\\(\\1(?:<=|==)0\\)";
        return Pattern.compile(localGate).matcher(source).find();
    }

    private static void assertNoCurrentHandLookup(String method, String methodName) {
        assertFalse(method.contains("getItemInHand("),
                methodName + " must not resolve a replacement stack from the current hand");
        assertFalse(method.contains("getMainHandItem("),
                methodName + " must not write to the current main-hand stack");
        assertFalse(method.contains("getOffhandItem("),
                methodName + " must not write to the current off-hand stack");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        assertTrue(openingBrace >= 0, "missing method body: " + declaration);

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
