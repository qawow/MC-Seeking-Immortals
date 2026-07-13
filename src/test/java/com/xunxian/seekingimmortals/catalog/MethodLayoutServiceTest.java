package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.network.SyncMethodLayoutPacket;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodLayoutServiceTest {
    @Test
    void onlyCatalogBackedBoundedIdsCanBePersisted() {
        String valid = TextMaterialCatalogService.builtin().methods().keySet().iterator().next();
        assertTrue(MethodLayoutService.isValidMethodId(valid));
        assertFalse(MethodLayoutService.isValidMethodId("not_a_catalog_method"));
        assertFalse(MethodLayoutService.isValidMethodId("x".repeat(SyncMethodLayoutPacket.MAX_ID_LENGTH + 1)));
    }

    @Test
    void cloneSanitizesPoisonedLayoutData() {
        String valid = TextMaterialCatalogService.builtin().methods().keySet().iterator().next();
        CompoundTag layout = new CompoundTag();
        layout.putString(valid, "12,-8");
        layout.putString("not_a_catalog_method", "1,1");
        layout.putString("x".repeat(SyncMethodLayoutPacket.MAX_ID_LENGTH + 1), "2,2");
        CompoundTag original = new CompoundTag();
        original.put(MethodLayoutService.LAYOUT_TAG, layout);

        CompoundTag clone = new CompoundTag();
        MethodLayoutService.copyLayoutData(original, clone);

        CompoundTag copied = clone.getCompound(MethodLayoutService.LAYOUT_TAG);
        assertEquals(1, copied.size());
        assertEquals("12,-8", copied.getString(valid));
    }
}
