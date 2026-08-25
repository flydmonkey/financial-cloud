package com.jinbooks.service.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.history.HistoryLogin;
import com.jinbooks.repository.history.HistoryLoginMapper;
import com.jinbooks.service.history.HistoryLoginService;
import com.jinbooks.context.WebContext;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class HistoryLoginService  extends ServiceImpl<HistoryLoginMapper,HistoryLogin>{

	private final HistoryLoginMapper historyLoginMapper;

	public HistoryLoginMapper getMapper() {
		return historyLoginMapper;
	}
    public void insertHistory(HistoryLogin historyLogin) {
        historyLogin.setId(WebContext.genId());
        //Thread insert HistoryLogin
        new Thread(new HistoryLoginRunnable(this,historyLogin)).start();
    }

	public class HistoryLoginRunnable implements Runnable{

		HistoryLoginService service;

		HistoryLogin historyLogin;

		public HistoryLoginRunnable(HistoryLoginService historyLoginService, HistoryLogin historyLogin) {
			super();
			this.service = historyLoginService;
			this.historyLogin = historyLogin;
		}
	    public void run() {
			log.debug(" historyLogin {}" , historyLogin);
			historyLogin.setOperateTime(new Date());
			service.save(historyLogin);
		}
	}

}
