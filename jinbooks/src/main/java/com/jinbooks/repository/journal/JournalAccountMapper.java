package com.jinbooks.repository.journal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.journal.JournalAccount;
import com.jinbooks.dto.journal.JournalAccountPageDto;

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
     * 转结： 本期期初余额=余额
     * @param bookId
     * @return
     */
    @Update("update journal_account set opening_balance = balance where book_id = #{bookId}")
    public int checkout(@Param ("bookId") String bookId);
    

}
