package com.financial.cloud.service.history;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.history.HistorySynchronizer;
import com.financial.cloud.repository.history.HistorySynchronizerMapper;
import com.financial.cloud.service.history.HistorySynchronizerService;

@RequiredArgsConstructor
@Repository
public class HistorySynchronizerService  extends ServiceImpl<HistorySynchronizerMapper,HistorySynchronizer>{

	private final HistorySynchronizerMapper historySynchronizerMapper;

	public HistorySynchronizerMapper getMapper() {
		return historySynchronizerMapper;
	}
}
