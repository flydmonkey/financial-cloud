package com.financial.cloud.controller.standard;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.standard.StandardStatementIncome;
import com.financial.cloud.service.standard.StandardStatementIncomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/standardstatementincome/")
@Slf4j
@RequiredArgsConstructor
public class StandardStatementIncomeController {
	
    private final StandardStatementIncomeService standardStatementIncomeService;

    @GetMapping(value = {"/get"})
    public Message<StandardStatementIncome> get(StandardStatementIncome dto) {
        return standardStatementIncomeService.get(dto);
    }

    @GetMapping(value = {"/fetch"})
    public Message<List<StandardStatementIncome>> fetch(StandardStatementIncome dto,@CurrentUser UserInfo userInfo) {
        return standardStatementIncomeService.list(dto);
    }

    @PostMapping(value = {"/save"})
    public Message<StandardStatementIncome> save(@Validated @RequestBody StandardStatementIncome dto,
                                                           @CurrentUser UserInfo userInfo) {
        return standardStatementIncomeService.save(dto);
    }

    @DeleteMapping(value = {"/delete/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return standardStatementIncomeService.delete(id);
    }

}
