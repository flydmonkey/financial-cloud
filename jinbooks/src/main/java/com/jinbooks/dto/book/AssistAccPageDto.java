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
 

package com.jinbooks.dto.book;

import com.jinbooks.common.PageQuery;
import lombok.*;

/**
 * 辅助核算项目查询对象
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistAccPageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = -979219411529190936L;

	/**
     * 所属账套
     */
    private String bookId;

    /**
     * 辅助核算编码
     */
    private String assistType;
    
    /**
     * 辅助核算编码
     */
    private String assistCode;

    /**
     * 辅助核算名称
     */
    private String assistName;

    private String status;

    /**
     * no ID
     */
    private String noId;
}
