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
 

package com.jinbooks.service.voucher;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.voucher.VoucherTemplate;
import com.jinbooks.dto.voucher.VoucherTemplateChangeDto;
import com.jinbooks.dto.voucher.VoucherTemplatePageDto;

public interface VoucherTemplateService extends IService<VoucherTemplate> {
	
	Message<VoucherTemplate> get(String id);
	
    Message<Page<VoucherTemplate>> pageList(VoucherTemplatePageDto dto);
    
    Message<String> save(VoucherTemplateChangeDto dto);

    Message<String> update(VoucherTemplateChangeDto dto);

    Message<String> delete(ListIdsDto dto);

    boolean deleteByBookIds(List<String> bookIds);
    
    boolean insertBookTemplate(String bookId,String standardId);
}
