package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalismanConsumePolicyTest {
    @Test
    void detectsCastTalismanTechniqueIds() {
        assertTrue(TalismanConsumePolicy.requiresTalisman("cast_fire_burst_talisman", null));
        assertTrue(TalismanConsumePolicy.requiresTalisman("fire_talisman", null));
        assertTrue(TalismanConsumePolicy.requiresTalisman("mid_grade_talisman_burst", null));
        assertFalse(TalismanConsumePolicy.requiresTalisman("fireball_art", null));
        assertFalse(TalismanConsumePolicy.requiresTalisman("beast_summon", null));
    }

    @Test
    void reservationNotRequiredIsAllowedAndIdempotent() {
        TalismanConsumePolicy.Reservation reservation = TalismanConsumePolicy.Reservation.notRequired();
        assertTrue(reservation.allowed());
        assertFalse(reservation.required());
        reservation.commit(null);
        reservation.refund(null);
    }

    @Test
    void deniedReservationCannotCommit() {
        TalismanConsumePolicy.Reservation denied = TalismanConsumePolicy.Reservation.denied(null);
        assertFalse(denied.allowed());
        denied.commit(null);
        denied.refund(null);
    }
}
