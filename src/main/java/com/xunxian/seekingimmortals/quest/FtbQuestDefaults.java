package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FtbQuestDefaults {
    private static final String RESOURCE_ROOT = "/seeking_immortals/ftbquests/quests/";
    private static final Path TARGET_ROOT = Path.of("ftbquests", "quests");
    private static final List<SeedFile> SEED_FILES = List.of(
            new SeedFile("data.snbt"),
            new SeedFile("chapters/seeking_immortals_main.snbt"),
            new SeedFile("chapters/seeking_immortals_chaotic_sea.snbt"),
            new SeedFile("chapters/seeking_immortals_dajin_kunwu.snbt"),
            new SeedFile("chapters/seeking_immortals_fallen_demon_yin.snbt"),
            new SeedFile("chapters/seeking_immortals_mulan_demonic.snbt"),
            new SeedFile("chapters/seeking_immortals_spirit_realm_service.snbt"),
            new SeedFile("chapters/seeking_immortals_tiannan_seven_sects.snbt"),
            new SeedFile("chapters/seeking_immortals_star_palace_inverse.snbt"),
            new SeedFile("chapters/seeking_immortals_ascension_border.snbt")
    );

    private FtbQuestDefaults() {
    }

    public static void bootstrapDefaultPack() {
        Path configRoot = FMLPaths.CONFIGDIR.get().resolve(TARGET_ROOT);
        for (SeedFile file : SEED_FILES) {
            copyIfMissing(configRoot, file);
        }
    }

    static void copyIfMissing(Path configRoot, String relativePath) {
        copyIfMissing(configRoot, new SeedFile(relativePath));
    }

    private static void copyIfMissing(Path configRoot, SeedFile file) {
        Path target = configRoot.resolve(file.relativePath());
        if (Files.exists(target)) {
            return;
        }

        String resource = RESOURCE_ROOT + file.relativePath().replace('\\', '/');
        try (InputStream input = FtbQuestDefaults.class.getResourceAsStream(resource)) {
            if (input == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing bundled FTB quest default {}", resource);
                return;
            }

            Files.createDirectories(target.getParent());
            Files.copy(input, target);
            SeekingImmortalsMod.LOGGER.info("Seeded bundled FTB quest default {}", target);
        } catch (IOException exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to seed bundled FTB quest default {}", target, exception);
        }
    }

    private record SeedFile(String relativePath) {
    }
}
