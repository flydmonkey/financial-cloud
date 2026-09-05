package com.financial.cloud.controller.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.permissions.SessionList;
import com.financial.cloud.dto.permissions.SessionListPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.permissions.SessionListService;
import com.financial.cloud.util.StrUtils;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
// DISABLED open-register-book-auth: menu hidden, code retained
//@RestController
@RequestMapping(value = { "/api/access/session" })
public class SessionController {

    private final SessionListService sessionListService;

    private final SessionManager sessionManager;

    private final HistorySystemLogsService historySystemLogsService;

    /**
     * 查询登录日志.
     *
     * @param dto
     * @return
     */
    @GetMapping(value = { "/fetch" })
    public Message<Page<SessionList>> fetch(
    		SessionListPageDto dto,
    			@CurrentUser UserInfo currentUser) {
        log.debug("history/session/fetch {}" , dto);

        LambdaQueryWrapper<SessionList> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SessionList::getOperateTime);
        return new Message<>(sessionListService.page(dto.build(), wrapper));
    }

    @DeleteMapping(value="/terminate")
    public Message<SessionList> terminate(@RequestParam("ids") String ids,@CurrentUser UserInfo currentUser) {
        log.debug(ids);
        boolean isTerminated = false;
        try {
            for(String sessionId : StrUtils.string2List(ids, ",")) {
                if (sessionId.equals(currentUser.getSessionId())) {
                    continue;//skip current session
                }
                log.trace("terminate session Id {} ",sessionId);
                sessionManager.terminate(sessionId,currentUser.getId(),currentUser.getUsername());
                sessionListService.removeById(sessionId);
            }
            isTerminated = true;
        }catch(Exception e) {
            log.debug("terminate Exception .",e);
        }

        if(isTerminated) {
        	return new Message<>(Message.SUCCESS);
        } else {
        	return new Message<>(Message.ERROR);
        }
    }
}
