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
 

package com.jinbooks.service.statement;

import com.jinbooks.common.Message;
import com.jinbooks.domain.statement.StatementIncomeItem;
import com.jinbooks.domain.statement.StatementRules;
import com.jinbooks.domain.statement.StatementSubjectBalance;

import java.util.List;


/**
 * 财务报表配置业务接口
 */
public interface StatementIncomeConfigService {
	
    Message<StatementIncomeItem> get(String bookId , String itemCode);

    /**
     * 资产负载表配置
     */
    Message<StatementIncomeItem> save(StatementIncomeItem dto);

    /**
     * 报表规则配置
     * @param dto 配置项
     * @return 结果
     */
    Message<List<StatementRules>> saveRules(List<StatementRules> dto, String bookId ,String itemCode);

    Message<List<StatementIncomeItem>> list(String bookId);

    Message<List<StatementRules>> getRules(String bookId , String itemCode);

    Message<Boolean> delete(String id);

    Message<StatementSubjectBalance> getSubjectBalance(StatementSubjectBalance params);

    void updateRuleBalance(StatementSubjectBalance subjectBalance, StatementRules statementRules);
}
