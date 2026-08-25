package com.jinbooks.service.report;


import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.dto.report.DashBoardReqDto;
import com.jinbooks.dto.report.DashBoardRepVo;
import com.jinbooks.repository.report.ReportMapper;
import com.jinbooks.service.report.ReportService;

@RequiredArgsConstructor
@Repository
public class ReportService  extends ServiceImpl<ReportMapper,DashBoardReqDto>{

	private final ReportMapper reportMapper;

	public ReportMapper getMapper() {
		return reportMapper;
	}


	public Integer analysisDay(DashBoardReqDto dbReqDto) {
		return getMapper().analysisDay(dbReqDto);
	}
	
	public Integer analysisNewUsers(DashBoardReqDto dbReqDto) {
		return getMapper().analysisNewUsers(dbReqDto);
	}
	
	public Integer analysisOnlineUsers(DashBoardReqDto dbReqDto) {
		return getMapper().analysisOnlineUsers(dbReqDto);
	}
	
	public Integer analysisActiveUsers(DashBoardReqDto dbReqDto) {
		return getMapper().analysisActiveUsers(dbReqDto);
	}
	
	public List<DashBoardRepVo> analysisDayHour(DashBoardReqDto dbReqDto){
		return getMapper().analysisDayHour(dbReqDto);
	}
	
	public List<DashBoardRepVo> analysisMonth(DashBoardReqDto dbReqDto){
		return getMapper().analysisMonth(dbReqDto);
	}
	public List<DashBoardRepVo> analysisProvince(DashBoardReqDto dbReqDto){
		List<DashBoardRepVo> listDbRepVo = getMapper().analysisProvince(dbReqDto);
		
		for(DashBoardRepVo dbRepVo : listDbRepVo) {
			String name = dbRepVo.getReportName();
			if (name.endsWith("省")
					|| name.endsWith("市")
					|| name.endsWith("特别行政区")
					|| name.endsWith("自治区")) {
				name = name.replace("省","")
						.replace("市","")
						.replace("特别行政区","")
						.replace("自治区","");
				dbRepVo.setName(name);
			}
		}
		return listDbRepVo;
	}

	public List<DashBoardRepVo> analysisBrowser(DashBoardReqDto dbReqDto){
		return getMapper().analysisBrowser(dbReqDto);
	}

}
