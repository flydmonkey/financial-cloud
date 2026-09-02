package com.financial.cloud.repository.journal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.dto.journal.JournalAccountPageDto;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface JournalAccountMapper extends BaseMapper<JournalAccount> {
    Page<JournalAccount> pageList(Page page, @Param("dto") JournalAccountPageDto dto);

    /**
     * 收入
     * @param accId
     * @param income
     * @return
     */
    @Update("update journal_account set balance = balance + #{income} where id = #{accId}")
    public int income(@Param ("accId") String accId,@Param ("income") BigDecimal income);
    
    /**
     * 支出
     * @param accId
     * @param expenditure
     * @return
     */
    @Update("update journal_account set balance = balance - #{expenditure} where id = #{accId}")
    public int expenditure(@Param ("accId") String accId,@Param ("expenditure") BigDecimal expenditure);
    
    /**
     * 结账：先备份 opening 到 prev_opening_balance，再 opening = balance。
     * IFNULL 保证新结账路径下 prev 非 null，便于反结账识别「无快照」的历史数据。
     */
    @Update("update journal_account set prev_opening_balance = IFNULL(opening_balance, 0), opening_balance = balance where book_id = #{bookId}")
    public int checkout(@Param ("bookId") String bookId);

    /**
     * 反结账：用结账前快照恢复 opening。
     */
    @Update("update journal_account set opening_balance = prev_opening_balance where book_id = #{bookId}")
    public int restoreOpeningFromPrev(@Param ("bookId") String bookId);

}
