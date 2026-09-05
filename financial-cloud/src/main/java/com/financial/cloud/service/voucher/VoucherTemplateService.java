package com.financial.cloud.service.voucher;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.domain.voucher.VoucherTemplateItem;
import com.financial.cloud.dto.voucher.VoucherTemplateChangeDto;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.standard.StandardSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.voucher.VoucherTemplateMapper;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.book.MonthEndCloseRules;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class VoucherTemplateService extends ServiceImpl<VoucherTemplateMapper, VoucherTemplate>{

    private final IdentifierGenerator identifierGenerator;

    private final VoucherTemplateMapper voucherTemplateMapper;
    
    private final VoucherTemplateItemMapper voucherTemplateItemMapper;

    private final BookSubjectMapper bookSubjectMapper;

    private final StandardSubjectMapper bookStandardSubjectMapper;

    private final BookSubjectService bookSubjectService;
    public Message<Page<VoucherTemplate>> pageList(VoucherTemplatePageDto dto) {
        if (StringUtils.isNotBlank(dto.getRelatedId())) {
            rematchSalaryAccrualParentSubjects(dto.getRelatedId());
        }
        Page<VoucherTemplate> page = voucherTemplateMapper.pageList(dto.build(), dto);

        return new Message<>(Message.SUCCESS, page);
    }
    @Transactional
    public Message<String> save(VoucherTemplateChangeDto dto) {
    	log.debug("dto {}",dto);
    	VoucherTemplate voucherTemplate = voucherTemplateMapper.selectById(dto.getId());
    	if(voucherTemplate == null) {
	        dto.setId(identifierGenerator.nextId(dto).toString());
	        saveItems(dto);
	        //新增
	        voucherTemplate = new VoucherTemplate();
	        BeanUtil.copyProperties(dto, voucherTemplate);
	        boolean saveResult = super.save(voucherTemplate);
	
	        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    	}else {
    		return this.update(dto);
    	}
    }
    @Transactional
    public Message<String> update(VoucherTemplateChangeDto dto) {
    	saveItems(dto);
        //更新
    	VoucherTemplate voucherTemplate = new VoucherTemplate();
        BeanUtil.copyProperties(dto, voucherTemplate);
        boolean result = super.updateById(voucherTemplate);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }

    /**
     * @Description: 插入关联和科目
     * @Param: [dto]
     * @return: void
     * @Author: xZen
     * @Date: 2025/1/2 15:01
     */
    private void saveItems(VoucherTemplateChangeDto dto) {
    	LambdaQueryWrapper<VoucherTemplateItem>templateWrapper = new LambdaQueryWrapper<>();
    	templateWrapper.eq(VoucherTemplateItem::getTemplateId, dto.getId());
        List<VoucherTemplateItem> templateItems = voucherTemplateItemMapper.selectList(templateWrapper);

        if (CollectionUtils.isNotEmpty(dto.getItems())) {
        	List<VoucherTemplateItem> newTemplateItems = new ArrayList<>();
        	List<VoucherTemplateItem> updateTemplateItems = new ArrayList<>();
        	List<String> deleteTemplateItems = new ArrayList<>();
        	for(VoucherTemplateItem item : dto.getItems()) {
        		if(StringUtils.isBlank(item.getId())) {
        			item.setRelatedId(dto.getRelatedId());
        			item.setTemplateId(dto.getId());
        			newTemplateItems.add(item);
        		}else {
        			for(VoucherTemplateItem loadItem : templateItems) {
        				if(item.getId().equals(loadItem.getId())) {
        					updateTemplateItems.add(item);
        				}
        			}
        		}
        	}
        	
        	for(VoucherTemplateItem loadItem : templateItems) {
        		boolean isNotExsits = true;
        		for(VoucherTemplateItem item : dto.getItems()) {
					if(loadItem.getId().equals(item.getId())) {
						isNotExsits = false;
					}
        		}
        		if(isNotExsits) {
        			deleteTemplateItems.add(loadItem.getId());
        		}
			}
        	Db.saveBatch(newTemplateItems);
        	Db.saveOrUpdateBatch(updateTemplateItems);
        	voucherTemplateItemMapper.deleteByIds(deleteTemplateItems);
        	
        }else {
        	//没有传入数据
        	voucherTemplateItemMapper.delete(templateWrapper);
        }
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();

        LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VoucherTemplate::getId, listIds);
        boolean result = voucherTemplateMapper.delete(wrapper) > 0;

        //删除关联科目
        LambdaQueryWrapper<VoucherTemplateItem> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.in(VoucherTemplateItem::getTemplateId, listIds);
        voucherTemplateItemMapper.delete(itemQueryWrapper);
        
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
	public Message<VoucherTemplate> get(String id) {
    	 VoucherTemplate voucherTemplate = voucherTemplateMapper.selectById(id);
    	 if (voucherTemplate == null) {
    		 return Message.ok(null);
    	 }
    	 // 账套内计提工资模板：父级科目改写为可入账末级，避免规则页只显示编码
    	 rematchSalaryAccrualParentSubjects(voucherTemplate.getRelatedId());
    	 voucherTemplate = voucherTemplateMapper.selectById(id);

    	 LambdaQueryWrapper<VoucherTemplateItem> lqw = Wrappers.lambdaQuery();
         lqw.eq(VoucherTemplateItem::getRelatedId, voucherTemplate.getRelatedId());
         lqw.eq(VoucherTemplateItem::getTemplateId, voucherTemplate.getId());
         
         lqw.orderByAsc(VoucherTemplateItem::getDirection,VoucherTemplateItem::getSubjectCode);
         List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(lqw);

         voucherTemplate.setItems(items);
		return Message.ok(voucherTemplate);
	}
	public boolean deleteByBookIds(List<String> bookIds) {
		LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VoucherTemplate::getRelatedId, bookIds);
        voucherTemplateMapper.delete(wrapper);
        
        LambdaQueryWrapper<VoucherTemplateItem> lqwItem = new LambdaQueryWrapper<>();
        lqwItem.in(VoucherTemplateItem::getRelatedId, bookIds);
        voucherTemplateItemMapper.delete(lqwItem);
		return true;
	}
	public boolean insertBookTemplate(String bookId, String standardId) {
    	LambdaQueryWrapper<VoucherTemplate>templateWrapper = new LambdaQueryWrapper<>();
    	templateWrapper.eq(VoucherTemplate::getRelatedId, standardId);
        List<VoucherTemplate> templates = voucherTemplateMapper.selectList(templateWrapper);
        List<VoucherTemplate> bookTemplates = new ArrayList<>();
        List<VoucherTemplateItem> newItems = new ArrayList<>();
        for(VoucherTemplate template: templates) {
        	// 已退役模板：销售成本并入 qm_jz_cbfy；折旧走固定资产模块
        	if (MonthEndCloseRules.isRetiredTemplateCode(template.getCode())) {
        		continue;
        	}
        	LambdaQueryWrapper<VoucherTemplateItem> itemLqw = Wrappers.lambdaQuery();
            itemLqw.eq(VoucherTemplateItem::getRelatedId, standardId);
            itemLqw.eq(VoucherTemplateItem::getTemplateId, template.getId());
            List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(itemLqw);

            template.setId(identifierGenerator.nextId(standardId).toString());
            template.setRelatedId(bookId);
            for(VoucherTemplateItem item : items) {
            	item.setId(identifierGenerator.nextId(standardId).toString());
            	item.setRelatedId(template.getRelatedId());
            	item.setTemplateId(template.getId());
            }
            // 标准模板若无损益结转/计提分录，按所选会计制度生成默认分录
            if (items.isEmpty() && MonthEndCloseRules.isAutoSeedTemplateCode(template.getCode())) {
            	items = buildDefaultTemplateItems(bookId, template.getId(), template.getCode(), standardId);
            }
            newItems.addAll(items);
            bookTemplates.add(template);
        }


        if (CollectionUtils.isNotEmpty(newItems)) {
        	voucherTemplateItemMapper.insert(newItems);
        }
        if (CollectionUtils.isNotEmpty(bookTemplates)) {
        	voucherTemplateMapper.insert(bookTemplates);
        }
        // 再兜底一次：空分录的必做结转 + 计提模板
        ensureDefaultTemplateItems(bookId, standardId);
		return true;
	}

	/**
	 * Ensure book-level P&amp;L carry and accrual templates have default lines for {@code standardId}.
	 * Idempotent: skips templates that already have items.
	 */
	public void ensureDefaultTemplateItems(String bookId, String standardId) {
		if (StringUtils.isBlank(bookId) || StringUtils.isBlank(standardId)) {
			return;
		}
		List<String> codes = new ArrayList<>();
		codes.add(MonthEndCloseRules.CODE_CARRY_INCOME);
		codes.add(MonthEndCloseRules.CODE_CARRY_COST);
		codes.addAll(MonthEndCloseRules.accrualTemplateCodes());
		LambdaQueryWrapper<VoucherTemplate> tw = Wrappers.lambdaQuery();
		tw.eq(VoucherTemplate::getRelatedId, bookId);
		tw.in(VoucherTemplate::getCode, codes);
		List<VoucherTemplate> templates = voucherTemplateMapper.selectList(tw);
		List<VoucherTemplateItem> toInsert = new ArrayList<>();
		for (VoucherTemplate template : templates) {
			Long count = voucherTemplateItemMapper.selectCount(Wrappers.<VoucherTemplateItem>lambdaQuery()
					.eq(VoucherTemplateItem::getTemplateId, template.getId())
					.eq(VoucherTemplateItem::getRelatedId, bookId));
			if (count != null && count > 0) {
				continue;
			}
			toInsert.addAll(buildDefaultTemplateItems(bookId, template.getId(), template.getCode(), standardId));
		}
		if (CollectionUtils.isNotEmpty(toInsert)) {
			Db.saveBatch(toInsert);
		}
		rematchSalaryAccrualParentSubjects(bookId);
		ensureSalaryPaymentTemplates(bookId, standardId);
	}

	/**
	 * Ensure book has {@code zf_gz} (wage payment) template header + default lines.
	 * Idempotent for existing books that never received a payment template from the standard catalog.
	 */
	public void ensureSalaryPaymentTemplates(String bookId, String standardId) {
		if (StringUtils.isBlank(bookId) || StringUtils.isBlank(standardId)) {
			return;
		}
		for (String code : MonthEndCloseRules.salaryPaymentTemplateCodes()) {
			VoucherTemplate template = voucherTemplateMapper.selectOne(Wrappers.<VoucherTemplate>lambdaQuery()
					.eq(VoucherTemplate::getRelatedId, bookId)
					.eq(VoucherTemplate::getCode, code)
					.eq(VoucherTemplate::getDeleted, "n")
					.last("limit 1"));
			if (template == null) {
				template = VoucherTemplate.builder()
						.id(identifierGenerator.nextId(bookId).toString())
						.relatedId(bookId)
						.code(code)
						.name("发放工资")
						.category(2)
						.voucherType(0)
						.voucherDate(0)
						.wordHead("记")
						.remark("发放{yyyy}年{mm}月{name}工资")
						.sortIndex(110)
						.status("1")
						.build();
				voucherTemplateMapper.insert(template);
			}
			Long count = voucherTemplateItemMapper.selectCount(Wrappers.<VoucherTemplateItem>lambdaQuery()
					.eq(VoucherTemplateItem::getTemplateId, template.getId())
					.eq(VoucherTemplateItem::getRelatedId, bookId));
			if (count == null || count == 0) {
				List<VoucherTemplateItem> items =
						buildDefaultTemplateItems(bookId, template.getId(), code, standardId);
				for (VoucherTemplateItem item : items) {
					rematchItemSubjectToLeaf(bookId, item);
				}
				if (CollectionUtils.isNotEmpty(items)) {
					Db.saveBatch(items);
				}
			}
		}
		rematchSalaryPaymentParentSubjects(bookId);
	}

	/**
	 * Existing books may still store parent codes (5602/2211) on jt_gz.
	 * Rewrite to preferred postable leaves when available so month-end UI matches generation.
	 */
	public void rematchSalaryAccrualParentSubjects(String bookId) {
		rematchTemplateSubjectsToLeaves(bookId, List.of(MonthEndCloseRules.CODE_ACCRUE_SALARY));
	}

	/** Rematch {@code zf_gz} payable/bank parents to postable leaves when available. */
	public void rematchSalaryPaymentParentSubjects(String bookId) {
		rematchTemplateSubjectsToLeaves(bookId, List.copyOf(MonthEndCloseRules.salaryPaymentTemplateCodes()));
	}

	private void rematchTemplateSubjectsToLeaves(String bookId, List<String> codes) {
		if (StringUtils.isBlank(bookId) || CollectionUtils.isEmpty(codes)) {
			return;
		}
		List<VoucherTemplate> templates = voucherTemplateMapper.selectList(Wrappers.<VoucherTemplate>lambdaQuery()
				.eq(VoucherTemplate::getRelatedId, bookId)
				.in(VoucherTemplate::getCode, codes));
		if (CollectionUtils.isEmpty(templates)) {
			return;
		}
		for (VoucherTemplate template : templates) {
			List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(Wrappers.<VoucherTemplateItem>lambdaQuery()
					.eq(VoucherTemplateItem::getTemplateId, template.getId())
					.eq(VoucherTemplateItem::getRelatedId, bookId));
			for (VoucherTemplateItem item : items) {
				if (rematchItemSubjectToLeaf(bookId, item)) {
					voucherTemplateItemMapper.updateById(item);
				}
			}
		}
	}

	private boolean rematchItemSubjectToLeaf(String bookId, VoucherTemplateItem item) {
		if (item == null || StringUtils.isBlank(item.getSubjectCode())) {
			return false;
		}
		BookSubject leaf = bookSubjectService.resolvePostableSubject(bookId, item.getSubjectCode());
		if (leaf != null && !leaf.getCode().equals(item.getSubjectCode())) {
			item.setSubjectCode(leaf.getCode());
			return true;
		}
		return false;
	}

	/** @deprecated use {@link #ensureDefaultTemplateItems(String, String)} */
	@Deprecated
	public void ensurePnlCarryTemplateItems(String bookId, String standardId) {
		ensureDefaultTemplateItems(bookId, standardId);
	}

	List<VoucherTemplateItem> buildDefaultTemplateItems(String bookId, String templateId,
			String templateCode, String standardId) {
		List<MonthEndCloseRules.CarryTemplateItemSpec> specs =
				MonthEndCloseRules.defaultCarryTemplateItems(templateCode, standardId);
		List<VoucherTemplateItem> items = new ArrayList<>(specs.size());
		for (MonthEndCloseRules.CarryTemplateItemSpec spec : specs) {
			VoucherTemplateItem item = VoucherTemplateItem.builder()
					.id(identifierGenerator.nextId(templateId).toString())
					.relatedId(bookId)
					.templateId(templateId)
					.summary(spec.summary())
					.subjectCode(spec.subjectCode())
					.direction(spec.direction())
					.build();
			items.add(item);
		}
		return items;
	}

}
