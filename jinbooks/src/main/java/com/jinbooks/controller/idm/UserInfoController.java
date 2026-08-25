package com.jinbooks.controller.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jinbooks.service.auth.FileStorageService;
import com.jinbooks.service.history.HistorySystemLogsService;
import com.jinbooks.service.security.ConfigPasswordPolicyService;
import com.jinbooks.service.idm.UserInfoExcelService;
import com.jinbooks.service.idm.UserInfoService;
import com.jinbooks.common.Message;
import com.jinbooks.dto.auth.ChangePassword;
import com.jinbooks.common.ExcelImport;
import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.SignedPrincipal;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.session.Session;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.constants.ConstsAct;
import com.jinbooks.constants.ConstsActResult;
import com.jinbooks.constants.ConstsEntryType;
import com.jinbooks.constants.ConstsPasswordSetType;
import com.jinbooks.domain.security.ConfigPasswordPolicy;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.idm.UserInfoPageDto;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import com.jinbooks.context.WebContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Crystal.Sea
 *
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value = { "/api/users" })
public class UserInfoController {

	private final UserInfoService userInfoService;

	private final UserInfoExcelService userInfoExcelService;

	private final FileStorageService fileStorageService;

	private final LoginService loginService;

	private final HistorySystemLogsService historySystemLogsService;

	private final ConfigPasswordPolicyService configPasswordPolicyService;

	private final SessionManager sessionManager;

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<UserInfo>> fetch(UserInfoPageDto dto, @CurrentUser UserInfo currentUser) {
		log.debug("fetch {}",dto);
		dto.setBookId(currentUser.getBookId());
		return userInfoService.fetchPageResults(dto);
	}

	@GetMapping(value={"/query"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> query(UserInfoPageDto dto, @CurrentUser UserInfo currentUser) {
		log.debug("-query  : {}" , dto);

		LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
		if (ObjectUtils.isNotEmpty(userInfoService.list(wrapper))) {
			 return new Message<>(Message.SUCCESS);
		} else {
			 return new Message<>(Message.FAIL);
		}
	}

	/**
	 * è·åç»å½ç¨æ·ä¿¡æ¯
	 * éè¦tokenå¤´ï¼è·åå½åtokenå¯¹åºçç¨æ·å¯¹è±?
	 * @return
	 */
	@GetMapping(value = { "/currentUser" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> currentUser(@CurrentUser UserInfo currentUser) {
		if (Objects.isNull(currentUser)) {
			return new Message<>(Message.FAIL);
		}
		UserInfo userInfo = userInfoService.getById(currentUser.getId());
		userInfo.clearSensitive();
		return new Message<>(userInfo);
	}
	/**
	 * è·åç»å½ç¨æ·ä¿¡æ¯
	 * éè¦tokenå¤´ï¼è·åå½åtokenå¯¹åºçç¨æ·å¯¹è±?
	 * @return
	 */
	@GetMapping(value = { "/switchBook/{bookId}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> switchBook(@PathVariable("bookId") String bookId,
			@CurrentUser UserInfo currentUser,
			jakarta.servlet.http.HttpServletRequest request) {
		if (Objects.isNull(currentUser)|| StringUtils.isBlank(bookId)) {
			return new Message<>(Message.FAIL);
		}
		currentUser.setBookId(bookId);
		SignedPrincipal principal  = AuthorizationUtils.getPrincipal();
		if (principal == null) {
			return new Message<>(Message.FAIL);
		}
		principal.setBookId(bookId);
		principal.setUserInfo(currentUser);
		UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                		principal,
                        null,
                        loginService.grantAuthority(currentUser)
                );
		AuthorizationUtils.setAuthentication(request, authenticationToken);
		Session session = sessionManager.get(currentUser.getSessionId());
		if (session != null) {
			session.setAuthentication(authenticationToken);
			sessionManager.create(session.getId(), session);
		}

		if (ObjectUtils.isNotEmpty(userInfoService.switchBook(currentUser))) {
			 return new Message<>(Message.SUCCESS);
		} else {
			 return new Message<>(Message.FAIL);
		}
	}


	@GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> get(@PathVariable("id") String id) {
		UserInfo userInfo=userInfoService.getById(id);
		userInfo.clearSensitive();
		return new Message<>(userInfo);
	}

	@GetMapping(value = { "/getByUsername/{username}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> getByUsername(@PathVariable("username") String username) {
		UserInfo userInfo=userInfoService.findByUsername(username);
		userInfo.clearSensitive();
		return new Message<>(userInfo);
	}

	@PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> insert(@Validated(value = AddGroup.class) @RequestBody UserInfo userInfo,@CurrentUser UserInfo currentUser) {
		log.debug("-Add  : {}" , userInfo);
		userInfo.setId(WebContext.genId());
		userInfo.setBookId(currentUser.getBookId());
		userInfo.setCreatedBy(currentUser.getId());
		userInfo.setCreatedDate(new Date());
		if (userInfoService.saveOneUser(userInfo)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					userInfo,
					ConstsAct.CREATE,
					ConstsActResult.SUCCESS,
					currentUser);
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> update(@Validated(value = EditGroup.class) @RequestBody  UserInfo userInfo, @CurrentUser UserInfo currentUser) {
		log.debug("-update  : {}" , userInfo);

		userInfo.setBookId(currentUser.getBookId());

		if (userInfoService.updateOneUser(userInfo)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					userInfo,
					ConstsAct.UPDATE,
					ConstsActResult.SUCCESS,
					currentUser);
		    return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		log.debug("-delete  ids : {} " , ids);

		if (userInfoService.removeByIds(ids)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					ids,
					ConstsAct.DELETE,
					ConstsActResult.SUCCESS,
					currentUser);
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

    @GetMapping(value = "/randomPassword", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<String> randomPassword() {
        return new Message<>(userInfoService.randomPassword());
    }


	@PutMapping(value="/changePassword", produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> changePassword(
			@Validated(value = EditGroup.class)
			@RequestBody ChangePassword changePassword,
			@CurrentUser UserInfo currentUser) {
		log.debug("UserId {}",changePassword.getUserId());
		changePassword.setPasswordSetType(ConstsPasswordSetType.PASSWORD_NORMAL);
		if(userInfoService.changePassword(changePassword,true)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					changePassword,
					ConstsAct.CHANGE_PASSWORD,
					ConstsActResult.SUCCESS,
					currentUser);
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@GetMapping(value = { "/updateStatus" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<UserInfo> updateStatus(@ModelAttribute UserInfo userInfo,@CurrentUser UserInfo currentUser) {
		log.debug("updateStatus {}",userInfo);
		UserInfo loadUserInfo = userInfoService.getById(userInfo.getId());
		userInfo.setBookId(currentUser.getBookId());
		userInfo.setUsername(loadUserInfo.getUsername());
		userInfo.setDisplayName(loadUserInfo.getDisplayName());
		if(userInfoService.updateStatus(userInfo)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					userInfo,
					ConstsAct.statusActon.get(userInfo.getStatus()),
					ConstsActResult.SUCCESS,
					currentUser);
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@PutMapping(value = { "/updatePassword" })
	public Message<ChangePassword> changePasswod(
			@RequestBody ChangePassword changePassword,
			@CurrentUser UserInfo currentUser) {
		if(!currentUser.getId().equals(changePassword.getUserId())){
			return null;
		}
		changePassword.setUsername(currentUser.getUsername());
		changePassword.setPasswordSetType(ConstsPasswordSetType.PASSWORD_NORMAL);
		if(userInfoService.updatePassword(changePassword)) {
			historySystemLogsService.log(
					ConstsEntryType.USERINFO,
					changePassword,
					ConstsAct.CHANGE_PASSWORD,
					ConstsActResult.SUCCESS,
					currentUser);
			return new Message<>(Message.SUCCESS);
		} else {
			String message = (String) WebContext.getAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT);
			log.info("-message: {}",message);
			return new Message<>(Message.ERROR,message);
		}
	}

    @RequestMapping(value = "/api/import")
    public Message<UserInfo> importUsers(
    		@ModelAttribute("excelImportFile")ExcelImport excelImportFile,
    		@CurrentUser UserInfo currentUser)  {
    	userInfoExcelService.importFromExcel(excelImportFile,currentUser);
        return new Message<>(Message.FAIL);

    }


	@GetMapping(value = "/export/{type}")
	public void exportOrganizations(@ModelAttribute UserInfo userInfo,
									@PathVariable("type") String type,
									HttpServletResponse response,
									@CurrentUser UserInfo currentUser)  {
		userInfo.setBookId(currentUser.getBookId());
		userInfoExcelService.exportToExcel(type,userInfo,response);
	}

	@InitBinder
	public void binder(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
		    @Override
			public void setAsText(String value) {
		        	if(StringUtils.isEmpty(value)){
		        		setValue(null);
		        	}else{
		        		setValue(value);
		        	}
		    }

		});
		 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        dateFormat.setLenient(false);
	        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}

	@GetMapping(value={"/passwordpolicy"})
	public Message<ConfigPasswordPolicy> passwordpolicy(@CurrentUser UserInfo currentUser){
		ConfigPasswordPolicy passwordPolicy = configPasswordPolicyService.getPasswordPolicy();
		//æå»ºå¯ç å¼ºåº¦è¯´æ
		configPasswordPolicyService.buildTipMessage(passwordPolicy);
		return new Message<>(passwordPolicy);
	}
}
