package com.jinbooks.service.history;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.history.HistorySynchronizer;
import com.jinbooks.repository.history.HistorySynchronizerMapper;
import com.jinbooks.service.history.HistorySynchronizerService;

@RequiredArgsConstructor
@Repository
public class HistorySynchronizerService  extends ServiceImpl<HistorySynchronizerMapper,HistorySynchronizer>{

	private final HistorySynchronizerMapper historySynchronizerMapper;

	public HistorySynchronizerMapper getMapper() {
		return historySynchronizerMapper;
	}
}
