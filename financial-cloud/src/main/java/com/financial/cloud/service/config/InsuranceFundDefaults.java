package com.financial.cloud.service.config;

import com.financial.cloud.domain.config.ConfigInsuranceFund;

import java.math.BigDecimal;

/**
 * National minimum-rate defaults for social insurance / housing fund (SMB starter).
 * Maternity is merged into medical (rates 0); personal injury rate is 0.
 */
public final class InsuranceFundDefaults {

    public static final BigDecimal PAY_BASE = new BigDecimal("2500.00");

    private InsuranceFundDefaults() {
    }

    public static ConfigInsuranceFund createForBook(String bookId) {
        ConfigInsuranceFund cfg = new ConfigInsuranceFund();
        cfg.setBookId(bookId);
        cfg.setPayBase(PAY_BASE);

        // 养老保险
        cfg.setEndowmentBusiness(new BigDecimal("16.0"));
        cfg.setEndowmentPersonal(new BigDecimal("8.0"));

        // 医疗保险（含生育）
        cfg.setMedicalBusiness(new BigDecimal("6.0"));
        cfg.setMedicalPersonal(new BigDecimal("2.0"));

        // 生育已并入医保：单独项置 0
        cfg.setMaternityBusiness(new BigDecimal("0.0"));
        cfg.setMaternityPersonal(new BigDecimal("0.0"));

        // 失业保险
        cfg.setUnemploymentBusiness(new BigDecimal("0.3"));
        cfg.setUnemploymentPersonal(new BigDecimal("0.2"));

        // 工伤保险（个人不缴）
        cfg.setEmploymentInjuryBusiness(new BigDecimal("0.2"));
        cfg.setEmploymentInjuryPersonal(new BigDecimal("0.0"));

        // 住房公积金（单位=个人，最低 5%）
        cfg.setProvidentFundSupBusiness(new BigDecimal("5.0"));
        cfg.setProvidentFundSupPersonal(new BigDecimal("5.0"));

        // 大病医疗（定额，默认无）
        cfg.setSeriousMedicalBusiness(BigDecimal.ZERO);
        cfg.setSeriousMedicalPersonal(BigDecimal.ZERO);

        return cfg;
    }
}
