package com.financial.cloud.repository.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.book.BookPageDto;
import com.financial.cloud.dto.book.BookVo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:12
 */

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    Page<Book> pageList(Page page, @Param("Dto") BookPageDto dto);

    List<BookVo> listBooks(@Param("userId") String userId);
}
