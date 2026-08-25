package com.jinbooks.service.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.history.HistoryLogin;
import com.jinbooks.repository.history.HistoryLoginMapper;
import com.jinbooks.context.WebContext;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class HistoryLoginService  extends ServiceImpl<HistoryLoginMapper,HistoryLogin>{

	private final HistoryLoginMapper historyLoginMapper;

	@Qualifier("historyTaskExecutor")
	private final TaskExecutor historyTaskExecutor;

	public HistoryLoginMapper getMapper() {
		return historyLoginMapper;
	}

    public void insertHistory(HistoryLogin historyLogin) {
        historyLogin.setId(WebContext.genId());
        historyTaskExecutor.execute(() -> {
			log.debug(" historyLogin {}" , historyLogin);
			historyLogin.setOperateTime(new Date());
			save(historyLogin);
		});
    }

}

