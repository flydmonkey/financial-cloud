package com.financial.cloud.controller.config;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.constants.common.ConstsAct;
import com.financial.cloud.constants.common.ConstsActResult;
import com.financial.cloud.constants.common.ConstsEntryType;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.config.InstitutionsPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.config.InstitutionsService;

import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/config/institutions"})
public class InstitutionsController {

	private final InstitutionsService institutionsService;

	private final HistorySystemLogsService historySystemLogsService;

	@GetMapping(value={"/getCurrent"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> getCurrent(@CurrentUser UserInfo currentUser){
		Institutions inst = institutionsService.getById(currentUser.getBookId());
		return new Message<>(Message.SUCCESS,inst);
	}

	@PutMapping(value={"/updateCurrent"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> updateCurrent(
			@RequestBody  Institutions inst,
			@CurrentUser UserInfo currentUser,
			BindingResult result) {
		log.debug("update {} ",inst);
		if(institutionsService.updateById(inst)) {
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<Institutions>> fetch(InstitutionsPageDto dto,
											 @CurrentUser UserInfo currentUser) {
		log.debug("fetch {}" , dto);
		LambdaQueryWrapper<Institutions> wrapper = new LambdaQueryWrapper<>();
		return new Message<>(Message.SUCCESS, institutionsService.page(dto.build(), wrapper));
	}

	@GetMapping(value={"/query"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<List<Institutions>> query(@ModelAttribute Institutions inst,@CurrentUser UserInfo currentUser) {
		log.debug("-query  {}" , inst);
		LambdaQueryWrapper<Institutions> wrapper = new LambdaQueryWrapper<>();
		List<Institutions>  instsList = institutionsService.list(wrapper);
		if (instsList != null) {
			 return new Message<>(Message.SUCCESS,instsList);
		} else {
			 return new Message<>(Message.FAIL);
		}
	}

	@GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> get(@PathVariable("id") String id) {
		Institutions inst=institutionsService.getById(id);
		return new Message<>(inst);
	}

	@PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> insert(@RequestBody Institutions inst,@CurrentUser UserInfo currentUser) {
		log.debug("-Add  : {}" , inst);
		if (institutionsService.save(inst)) {
			historySystemLogsService.log(
					ConstsEntryType.POST,
					inst,
					ConstsAct.CREATE,
					ConstsActResult.SUCCESS,
					currentUser);
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> update(@RequestBody  Institutions inst,@CurrentUser UserInfo currentUser) {
		log.debug("-update  : {}" , inst);
		if (institutionsService.updateById(inst)) {
			historySystemLogsService.log(
					ConstsEntryType.POST,
					inst,
					ConstsAct.UPDATE,
					ConstsActResult.SUCCESS,
					currentUser);
		    return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		log.debug("-delete  ids : {} " , ids);
		if (institutionsService.removeByIds(ids)) {
			historySystemLogsService.log(
					ConstsEntryType.POST,
					ids,
					ConstsAct.DELETE,
					ConstsActResult.SUCCESS,
					currentUser);
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}
}
