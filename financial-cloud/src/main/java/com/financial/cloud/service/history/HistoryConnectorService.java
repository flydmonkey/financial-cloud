package com.financial.cloud.service.history;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.history.HistoryConnector;
import com.financial.cloud.repository.history.HistoryConnectorMapper;
import com.financial.cloud.service.history.HistoryConnectorService;

@RequiredArgsConstructor
@Repository
public class HistoryConnectorService  extends ServiceImpl<HistoryConnectorMapper,HistoryConnector> {

	private final HistoryConnectorMapper historyConnectorMapper;

	public HistoryConnectorMapper getMapper() {
		return historyConnectorMapper;
	}
}
