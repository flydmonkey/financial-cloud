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
 

package com.jinbooks.service.book;

import java.util.List;

import org.dromara.hutool.core.tree.MapTree;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinbooks.common.Message;
import com.jinbooks.domain.book.BookSubject;
import com.jinbooks.dto.book.BookChangeDto;
import com.jinbooks.dto.book.SubjectChangeDto;
import com.jinbooks.dto.book.SubjectPageDto;
import com.jinbooks.dto.common.ListIdsDto;

public interface BookSubjectService extends IService<BookSubject> {
	
	List<MapTree<String>> tree(String bookId);
	
    Message<Page<BookSubject>> pageList(SubjectPageDto dto);

    Message<String> save(SubjectChangeDto dto);

    Message<String> update(SubjectChangeDto dto);

    Message<String> delete(ListIdsDto dto);

    Message<String> reorgDisplayName(String bookId);
    
    BookSubject selectSubject(String bookId , String subjectCode);
    
    List<BookSubject> selectSubjectAndChild(String bookId , String subjectCode);
    
    BookSubject getById(String bookId, String id);
    
    boolean deleteByBookIds(List<String> bookIds);
    
    boolean initBookSubject(BookChangeDto dto);
}
