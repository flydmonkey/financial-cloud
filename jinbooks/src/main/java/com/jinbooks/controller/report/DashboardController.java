package com.jinbooks.controller.report;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.report.DashBoardReqDto;
import com.jinbooks.dto.report.DashBoardVo;
import com.jinbooks.service.report.ReportService;

/**
 * Index
 * @author Crystal.Sea
 *
 */
@RequiredArgsConstructor
@Slf4j
@RestController
public class DashboardController {

	private final ReportService reportService;

	@GetMapping(value={"/api/dashboard"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<DashBoardVo> dashboard(@CurrentUser UserInfo currentUser) {

		log.debug("DashboardController /dashboard.");
		
		DashBoardReqDto dbReqDto = new DashBoardReqDto();
		dbReqDto.setBookId(currentUser.getBookId());
		
		DashBoardVo dbv = new DashBoardVo();
		dbv.setBookId(currentUser.getBookId());
		
		dbv.setDayCount(reportService.analysisDay(dbReqDto));
		dbv.setNewUsers(reportService.analysisNewUsers(dbReqDto));
		
		dbv.setOnlineUsers(reportService.analysisOnlineUsers(dbReqDto));
		dbv.setActiveUsers(reportService.analysisActiveUsers(dbReqDto));
		
		dbv.setReportMonth(reportService.analysisMonth(dbReqDto));
		dbv.setReportDayHour(reportService.analysisDayHour(dbReqDto));
		
		dbv.setReportBrowser(reportService.analysisBrowser(dbReqDto));

		dbv.setReportProvince(reportService.analysisProvince(dbReqDto));
		return new Message<>(dbv);
	}
}
