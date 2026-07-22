package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbQuestOptionalCompatTest {
    @Test
    void commonEntrypointOnlyReferencesFtbFreeFacade() throws Exception {
        String entrypoint = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/SeekingImmortalsMod.java"));
        String facade = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/quest/FtbQuestCompatBootstrap.java"));

        assertTrue(entrypoint.contains("FtbQuestCompatBootstrap.registerIfPresent()"));
        assertFalse(entrypoint.contains("import com.xunxian.seekingimmortals.quest.FtbCustomTaskHooks"));
        assertFalse(entrypoint.contains("FtbCustomTaskHooks.register()"));
        assertFalse(facade.contains("import dev.ftb."),
                "Optional compatibility facade must not expose FTB types in its constant pool");
        assertFalse(facade.contains("FtbCustomTaskHooks.class"),
                "Optional implementation must be resolved by name only after the mod-presence gate");
    }

    @Test
    void optionalFacadeCanBeLoadedWithoutInitializingFtbHooks() {
        assertDoesNotThrow(() -> Class.forName(
                "com.xunxian.seekingimmortals.quest.FtbQuestCompatBootstrap",
                false,
                FtbQuestOptionalCompatTest.class.getClassLoader()));
    }

    @Test
    void directFtbApiImportsStayInsideReflectivelyLoadedHooks() throws Exception {
        Path sourceRoot = Path.of("src/main/java/com/xunxian/seekingimmortals");
        Set<Path> directFtbSources = new HashSet<>();
        try (var paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.toString().endsWith(".java"))
                    .toList()) {
                if (Files.readString(path).contains("import dev.ftb.")) {
                    directFtbSources.add(sourceRoot.relativize(path));
                }
            }
        }

        assertTrue(directFtbSources.equals(Set.of(Path.of("quest/FtbCustomTaskHooks.java"))),
                "Only the reflectively loaded hook implementation may link FTB API types: " + directFtbSources);
    }
}
