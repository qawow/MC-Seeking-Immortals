package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.quest.QuestProgress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SectContributionTokenServiceTest {
    @Test
    void redeemsOnePointOnlyForKnownCurrentSect() {
        QuestProgress unaligned = new QuestProgress();
        assertEquals(SectContributionTokenService.RedemptionResult.NO_SECT,
                SectContributionTokenService.redeem(unaligned));
        assertEquals(0, unaligned.getContribution());

        QuestProgress member = new QuestProgress();
        member.setSect("huangfeng_valley", "outer_disciple");
        assertEquals(SectContributionTokenService.RedemptionResult.SUCCESS,
                SectContributionTokenService.redeem(member));
        assertEquals(SectContributionTokenService.CONTRIBUTION_PER_TOKEN, member.getContribution());
    }

    @Test
    void cappedContributionDoesNotConsumeTokenValue() {
        QuestProgress member = new QuestProgress();
        member.setSect("qinglan_sect", "inner_disciple");
        member.addContribution(Integer.MAX_VALUE);

        assertEquals(SectContributionTokenService.RedemptionResult.CAPPED,
                SectContributionTokenService.redeem(member));
        assertEquals(Integer.MAX_VALUE, member.getContribution());
    }
}
