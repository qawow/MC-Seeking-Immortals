package com.xunxian.seekingimmortals.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryDeliveryServiceTest {
    private static final Pattern DUPLICATING_DELIVERY = Pattern.compile(
            "getInventory\\(\\)\\.add\\([^\\n]*\\.copy\\(\\)\\)\\s*\\{?\\s*[^\\n]*drop\\([^\\n]*\\.copy\\(\\)",
            Pattern.MULTILINE);

    @Test
    void splitsOversizedDeliveriesWithoutChangingTotal() {
        assertEquals(List.of(), InventoryDeliveryService.splitCounts(0, 64));
        assertEquals(List.of(1), InventoryDeliveryService.splitCounts(1, 64));
        assertEquals(List.of(64, 64, 2), InventoryDeliveryService.splitCounts(130, 64));
        assertEquals(130, InventoryDeliveryService.splitCounts(130, 64).stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void sourceTreeHasNoCopyInsertThenFullCopyDropPattern() throws IOException {
        Path root = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(DUPLICATING_DELIVERY.matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }

    @Test
    void deliveryUsesForgeMainInventoryRemainders() throws IOException {
        Path source = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "InventoryDeliveryService.java");
        String text = Files.readString(source);
        assertFalse(text.contains("getInventory().add("));
        assertFalse(text.contains("return delivered"));
        assertFalse(text.contains("player.drop("));
        assertEquals(1, countOccurrences(text, "ItemHandlerHelper.giveItemToPlayer("));
    }

    @Test
    void outboxHelpersArePresentAndDeliveryStillUsesForgeRemainder() throws IOException {
        Path source = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "InventoryDeliveryService.java");
        String text = Files.readString(source);
        assertFalse(text.contains("return delivered"));
        assertEquals(1, countOccurrences(text, "ItemHandlerHelper.giveItemToPlayer("));
        assertEquals(true, text.contains("giveOrEnqueue"));
        assertEquals(true, text.contains("claimQueued"));
        assertEquals(true, text.contains("canFullyAccept"));
        Path outbox = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "DeliveryOutboxSavedData.java");
        String outboxText = Files.readString(outbox);
        assertEquals(true, outboxText.contains("seeking_immortals_delivery_outbox")
                || outboxText.contains("_delivery_outbox"));
        assertEquals(true, outboxText.contains("enqueue"));
        assertEquals(true, outboxText.contains("claimAll"));
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
