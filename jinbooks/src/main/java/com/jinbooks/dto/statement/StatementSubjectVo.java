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
 

package com.jinbooks.dto.statement;

import com.jinbooks.domain.book.BookSubject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 简介说明: 统计报表中的科目对象
 *
 * @author wuyan
 * {@code @date} 2025/03/18 13:49:13
 * {@code @version} 1.0
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class StatementSubjectVo extends BookSubject {
	

    /**
	 * 
	 */
	private static final long serialVersionUID = -3175152469296124678L;

	/**
     * 借方金额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

}
