package com.jinbooks.service.history;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.history.HistoryConnector;
import com.jinbooks.repository.history.HistoryConnectorMapper;
import com.jinbooks.service.history.HistoryConnectorService;

@RequiredArgsConstructor
@Repository
public class HistoryConnectorService  extends ServiceImpl<HistoryConnectorMapper,HistoryConnector> {

	private final HistoryConnectorMapper historyConnectorMapper;

	public HistoryConnectorMapper getMapper() {
		return historyConnectorMapper;
	}
}
