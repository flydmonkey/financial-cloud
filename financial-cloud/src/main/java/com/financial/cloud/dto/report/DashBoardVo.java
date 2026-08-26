package com.financial.cloud.dto.report;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashBoardVo {
	String bookId;
	
	int dayCount;
	
	int newUsers;
	
	int onlineUsers;
	
	int activeUsers;
	
	List <DashBoardRepVo> reportMonth;
	
	List <DashBoardRepVo> reportDayHour;
	
	List <DashBoardRepVo> reportBrowser;
	
	List <DashBoardRepVo> reportApp;
	
	List <DashBoardRepVo> reportProvince;
}
