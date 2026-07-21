package com.xunxian.seekingimmortals.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 云笈墨卷 B5: zh/en lang parity for the new hud/status/cultivation_stats namespaces. */
class LangParityTest {
    private static Set<String> keys(String lang) throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "assets", "seeking_immortals", "lang", lang + ".json")))
                .getAsJsonObject();
        return new HashSet<>(root.keySet());
    }

    @Test
    void hudAndStatusNamespacesExistInBothLanguages() throws Exception {
        Set<String> zh = keys("zh_cn");
        Set<String> en = keys("en_us");
        String[] bilingualPrefixes = {
                "hud.seeking_immortals.cultivation.",
                "hud.seeking_immortals.health.",
                "status.seeking_immortals.affliction.",
                "screen.seeking_immortals.cultivation_stats.label.",
                "screen.seeking_immortals.technique_edit.",
                "screen.seeking_immortals.display."
        };
        for (String prefix : bilingualPrefixes) {
            long zhCount = zh.stream().filter(k -> k.startsWith(prefix)).count();
            long enCount = en.stream().filter(k -> k.startsWith(prefix)).count();
            assertTrue(zhCount > 0, prefix + " must exist in zh_cn");
            assertTrue(zhCount == enCount,
                    prefix + " must be bilingual: zh=" + zhCount + " en=" + enCount);
            for (String key : zh.stream().filter(k -> k.startsWith(prefix)).toList()) {
                assertTrue(en.contains(key), key + " missing in en_us");
            }
            for (String key : en.stream().filter(k -> k.startsWith(prefix)).toList()) {
                assertTrue(zh.contains(key), key + " missing in zh_cn");
            }
        }
        // Every zh key in the new namespaces must exist in en (and vice versa).
        for (String key : zh) {
            if (key.startsWith("hud.seeking_immortals.") || key.startsWith("status.seeking_immortals.")) {
                assertTrue(en.contains(key), key + " missing in en_us");
            }
        }
        for (String key : en) {
            if (key.startsWith("hud.seeking_immortals.") || key.startsWith("status.seeking_immortals.")) {
                assertTrue(zh.contains(key), key + " missing in zh_cn");
            }
        }
    }

    @Test
    void hudOverlaysAndStatsScreenHaveNoHardcodedCjk() throws Exception {
        for (String path : new String[]{
                "CultivationStatsScreen.java", "CultivationHudOverlay.java",
                "CultivationHealthOverlay.java", "TechniqueEditScreen.java",
                "MethodTreeScreen.java"}) {
            String source = Files.readString(Path.of(
                    "src", "main", "java", "com", "xunxian", "seekingimmortals", "client", path));
            // Strip comments before scanning for CJK string literals.
            String noComments = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"[^\"\\\\]*[\\u4e00-\\u9fff][^\"\\\\]*\"").matcher(noComments);
            StringBuilder found = new StringBuilder();
            while (matcher.find()) {
                found.append(matcher.group()).append(' ');
            }
            assertTrue(found.isEmpty(), path + " still hardcodes CJK literals: " + found);
        }
    }
}
