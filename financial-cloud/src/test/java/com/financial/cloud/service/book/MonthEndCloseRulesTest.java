package com.financial.cloud.service.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthEndCloseRulesTest {

    @Test
    void smallBusinessUsesXiaorenSubjectRoots() {
        assertTrue(MonthEndCloseRules.costCarryRootsForStandard("1").contains("5401"));
        assertTrue(MonthEndCloseRules.costCarryRootsForStandard("1").contains("5602"));
        assertTrue(MonthEndCloseRules.incomeCarryRootsForStandard("1").contains("5001"));
        assertEquals("3103", MonthEndCloseRules.yearProfitSubjectForStandard("1"));
        assertEquals("3104.02", MonthEndCloseRules.undistributedProfitSubjectForStandard("1"));
    }

    @Test
    void enterpriseSystemUsesQiyeKuaijiZhiduRoots() {
        assertTrue(MonthEndCloseRules.costCarryRootsForStandard("2").contains("5501"));
        assertTrue(MonthEndCloseRules.costCarryRootsForStandard("2").contains("5405"));
        assertTrue(MonthEndCloseRules.incomeCarryRootsForStandard("2").contains("5101"));
        assertEquals("3131", MonthEndCloseRules.yearProfitSubjectForStandard("2"));
        assertEquals("3141.15", MonthEndCloseRules.undistributedProfitSubjectForStandard("2"));
    }

    @Test
    void defaultIncomeCarryTemplateItemsFollowSmallBusiness() {
        var items = MonthEndCloseRules.defaultIncomeCarryTemplateItems("1");
        assertEquals(5, items.size());
        assertEquals("5001", items.get(0).subjectCode());
        assertEquals(1, items.get(0).direction());
        assertEquals("3103", items.get(items.size() - 1).subjectCode());
        assertEquals(2, items.get(items.size() - 1).direction());
    }

    @Test
    void defaultCostCarryTemplateItemsFollowEnterpriseSystem() {
        var items = MonthEndCloseRules.defaultCostCarryTemplateItems("2");
        assertEquals(7, items.size());
        assertEquals("5401", items.get(0).subjectCode());
        assertEquals(2, items.get(0).direction());
        assertEquals("3131", items.get(items.size() - 1).subjectCode());
        assertEquals(1, items.get(items.size() - 1).direction());
    }

    @Test
    void defaultCarryTemplateItemsByCode() {
        assertEquals(5, MonthEndCloseRules.defaultCarryTemplateItems("qm_jz_sr", "1").size());
        assertEquals(7, MonthEndCloseRules.defaultCarryTemplateItems("qm_jz_cbfy", "1").size());
        assertTrue(MonthEndCloseRules.defaultCarryTemplateItems("qm_jz_bnlr", "1").isEmpty());
    }

    @Test
    void defaultAccrualTemplateItemsFollowSmallBusiness() {
        assertAccrualPair("jt_gz", "1", "5602", "2211", "计提工资");
        assertAccrualPair("jt_sds", "1", "5801", "2221.05", "计提所得税");
        assertAccrualPair("jt_fjs", "1", "5403", "2221", "计提附加税");
        assertTrue(MonthEndCloseRules.defaultCarryTemplateItems("jt_zj", "1").isEmpty());
        assertTrue(MonthEndCloseRules.isRetiredTemplateCode("jt_zj"));
    }

    @Test
    void defaultAccrualTemplateItemsFollowEnterpriseSystem() {
        assertAccrualPair("jt_gz", "2", "5502", "2151", "计提工资");
        assertAccrualPair("jt_sds", "2", "5701", "2171.06", "计提所得税");
        assertAccrualPair("jt_fjs", "2", "5402", "2171", "计提附加税");
    }

    private static void assertAccrualPair(String templateCode, String standardId,
            String debit, String credit, String summary) {
        var items = MonthEndCloseRules.defaultCarryTemplateItems(templateCode, standardId);
        assertEquals(2, items.size());
        assertEquals(debit, items.get(0).subjectCode());
        assertEquals(1, items.get(0).direction());
        assertEquals(summary, items.get(0).summary());
        assertEquals(credit, items.get(1).subjectCode());
        assertEquals(2, items.get(1).direction());
        assertEquals(summary, items.get(1).summary());
    }
}
