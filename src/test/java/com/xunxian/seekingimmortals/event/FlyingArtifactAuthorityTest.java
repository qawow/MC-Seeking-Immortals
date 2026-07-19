package com.xunxian.seekingimmortals.event;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlyingArtifactAuthorityTest {
    @Test
    void flightRequiresOwnerAndIntegrity() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "event", "ModEvents.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("isAuthorizedFlyingArtifact"));
        assertTrue(compact.contains("ArtifactOwnershipService.isUsableBy"),
                "flight path must silently re-check ownership");
        assertTrue(compact.contains("getIntegrity"),
                "flight path must require positive integrity");
        assertTrue(compact.contains("findFirstCurio(stack->isAuthorizedFlyingArtifact"),
                "hasFlyingArtifact must filter through authorized helper");
        String auth = methodBody(source, "private static boolean isAuthorizedFlyingArtifact");
        assertFalse(auth.contains("canActivate("),
                "tick-path ownership check must stay silent (no canActivate messages)");
        assertTrue(auth.contains("isUsableBy"),
                "silent ownership check must still reject foreign owners");
        assertTrue(auth.contains("requiresClaim") || auth.contains("getIntegrity"),
                "high-tier unclaimed or broken flying curios must fail closed");
    }

    private static String methodBody(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing " + declaration);
        int opening = source.indexOf('{', start);
        int depth = 0;
        for (int i = opening; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return source.substring(start, i + 1).replaceAll("\\s+", "");
            }
        }
        throw new AssertionError("unterminated " + declaration);
    }
}
