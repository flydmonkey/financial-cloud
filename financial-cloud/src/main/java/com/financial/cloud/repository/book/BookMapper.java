package com.financial.cloud.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.book.BookPageDto;
import com.financial.cloud.dto.book.BookVo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    @Select("SELECT id FROM book WHERE id = #{bookId} AND deleted = 'n' FOR UPDATE")
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    String lockActiveBookId(@Param("bookId") String bookId);

    Page<Book> pageList(Page page, @Param("Dto") BookPageDto dto);

    List<BookVo> listBooks(@Param("userId") String userId);
}
