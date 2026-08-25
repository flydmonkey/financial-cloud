package com.jinbooks.service.voucher;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.voucher.VoucherTemplate;
import com.jinbooks.domain.voucher.VoucherTemplateItem;
import com.jinbooks.dto.voucher.VoucherTemplateChangeDto;
import com.jinbooks.dto.voucher.VoucherTemplatePageDto;
import com.jinbooks.repository.book.BookSubjectMapper;
import com.jinbooks.repository.standard.StandardSubjectMapper;
import com.jinbooks.repository.voucher.VoucherTemplateItemMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.jinbooks.repository.voucher.VoucherTemplateMapper;
import com.jinbooks.service.voucher.VoucherTemplateService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:15
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class VoucherTemplateService extends ServiceImpl<VoucherTemplateMapper, VoucherTemplate>{

    private final IdentifierGenerator identifierGenerator;

    private final VoucherTemplateMapper voucherTemplateMapper;
    
    private final VoucherTemplateItemMapper voucherTemplateItemMapper;

    private final BookSubjectMapper bookSubjectMapper;

    private final StandardSubjectMapper bookStandardSubjectMapper;
    public Message<Page<VoucherTemplate>> pageList(VoucherTemplatePageDto dto) {
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
    	 
    	 LambdaQueryWrapper<VoucherTemplateItem> lqw = Wrappers.lambdaQuery();
         lqw.eq(VoucherTemplateItem::getRelatedId, voucherTemplate.getRelatedId());
         lqw.eq(VoucherTemplateItem::getTemplateId, voucherTemplate.getId());
         
         lqw.orderByAsc(VoucherTemplateItem::getDirection,VoucherTemplateItem::getSubjectCode);
         List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(lqw);

         if(voucherTemplate != null) {
        	 voucherTemplate.setItems(items);
         }
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
        List<VoucherTemplateItem> newItems = new ArrayList<>();
        for(VoucherTemplate template: templates) {
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
            newItems.addAll(items);
        }


        voucherTemplateItemMapper.insert(newItems);
        voucherTemplateMapper.insert(templates);
		return true;
	}

}
