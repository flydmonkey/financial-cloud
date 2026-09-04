package com.financial.cloud.service.config;

import com.financial.cloud.domain.config.ConfigInsuranceFund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InsuranceFundDefaultsTest {

    @Test
    void createForBook_usesNationalMinimumRatesAndBase2500() {
        ConfigInsuranceFund cfg = InsuranceFundDefaults.createForBook("book-1");
        assertNotNull(cfg);
        assertEquals("book-1", cfg.getBookId());
        assertEquals(0, new BigDecimal("2500.00").compareTo(cfg.getPayBase()));

        assertEquals(0, new BigDecimal("16.0").compareTo(cfg.getEndowmentBusiness()));
        assertEquals(0, new BigDecimal("8.0").compareTo(cfg.getEndowmentPersonal()));

        assertEquals(0, new BigDecimal("6.0").compareTo(cfg.getMedicalBusiness()));
        assertEquals(0, new BigDecimal("2.0").compareTo(cfg.getMedicalPersonal()));

        assertEquals(0, new BigDecimal("0.0").compareTo(cfg.getMaternityBusiness()));
        assertEquals(0, new BigDecimal("0.0").compareTo(cfg.getMaternityPersonal()));

        assertEquals(0, new BigDecimal("0.3").compareTo(cfg.getUnemploymentBusiness()));
        assertEquals(0, new BigDecimal("0.2").compareTo(cfg.getUnemploymentPersonal()));

        assertEquals(0, new BigDecimal("0.2").compareTo(cfg.getEmploymentInjuryBusiness()));
        assertEquals(0, new BigDecimal("0.0").compareTo(cfg.getEmploymentInjuryPersonal()));

        assertEquals(0, new BigDecimal("5.0").compareTo(cfg.getProvidentFundSupBusiness()));
        assertEquals(0, new BigDecimal("5.0").compareTo(cfg.getProvidentFundSupPersonal()));

        assertEquals(0, BigDecimal.ZERO.compareTo(cfg.getSeriousMedicalBusiness()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cfg.getSeriousMedicalPersonal()));
    }
}
