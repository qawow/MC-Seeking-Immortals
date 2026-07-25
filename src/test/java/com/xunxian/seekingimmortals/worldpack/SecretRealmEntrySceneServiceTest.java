package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmEntrySceneServiceTest {
    @Test
    void dedicatedRealmsResolveToExplicitArrivalProfiles() {
        assertEquals(SecretRealmEntrySceneService.Profile.BLOOD_GROVE,
                SecretRealmEntrySceneService.profileFor("blood_forbidden"));
        assertEquals(SecretRealmEntrySceneService.Profile.FROST_TEMPLE,
                SecretRealmEntrySceneService.profileFor("kunwu_mountain"));
        assertEquals(SecretRealmEntrySceneService.Profile.PUPPET_TOWER,
                SecretRealmEntrySceneService.profileFor("thousand_bamboo_puppet_tower"));
        assertEquals(SecretRealmEntrySceneService.Profile.CATACOMB,
                SecretRealmEntrySceneService.profileFor("yin_mountain_catacomb"));
        assertEquals(SecretRealmEntrySceneService.Profile.CATACOMB,
                SecretRealmEntrySceneService.profileFor("yinyang_ku"));
        assertTrue(SecretRealmEntrySceneService.profileCount() >= 24);
    }
}
