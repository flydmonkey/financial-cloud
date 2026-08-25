/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

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
