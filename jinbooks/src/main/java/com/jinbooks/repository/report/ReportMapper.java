/**
 *
 */
package com.jinbooks.repository.report;

import com.jinbooks.repository.report.ReportMapper;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.dto.report.DashBoardReqDto;
import com.jinbooks.dto.report.DashBoardRepVo;

/**
 * @author Crystal.sea
 *
 */
public  interface ReportMapper extends BaseMapper<DashBoardReqDto> {
	
	public Integer analysisDay(DashBoardReqDto dbReqDto);
	
	public Integer analysisNewUsers(DashBoardReqDto dbReqDto);
	
	public Integer analysisOnlineUsers(DashBoardReqDto dbReqDto);
	
	public Integer analysisActiveUsers(DashBoardReqDto dbReqDto);
	
	public List<DashBoardRepVo> analysisDayHour(DashBoardReqDto dbReqDto);
	
	public List<DashBoardRepVo> analysisMonth(DashBoardReqDto dbReqDto);

	public List<DashBoardRepVo> analysisProvince(DashBoardReqDto dbReqDto);

	
	public List<DashBoardRepVo> analysisBrowser(DashBoardReqDto dbReqDto);
	
	public List<DashBoardRepVo> analysisApp(DashBoardReqDto dbReqDto);
	
	
}
