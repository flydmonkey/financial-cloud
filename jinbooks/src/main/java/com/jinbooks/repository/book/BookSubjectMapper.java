package com.jinbooks.repository.book;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.book.BookSubject;
import com.jinbooks.dto.book.SubjectPageDto;
import com.jinbooks.dto.statement.StatementSubjectVo;
import com.jinbooks.dto.statement.StatementParamsDto;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookSubjectMapper extends BaseMapper<BookSubject> {
    Page<BookSubject> pageListByBook(Page<BookSubject> page, @Param("Dto") SubjectPageDto dto);

    int deleteByBookId(@Param("bookId") String bookId);

    /**
     * 统计各科目指定期余额
     *
     * @param params 查询参数
     * @return 结果
     */
    List<StatementSubjectVo> selectPeriodBalance(@Param("params") StatementParamsDto params);

}
