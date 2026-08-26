package com.financial.cloud.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.NamingCase;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.enums.error.CommonErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.util.SqlUtil;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PageQuery implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1082122094613059399L;

	public static final String SEPARATOR = ",";

    private Integer pageSize;

    private Integer pageNumber;

    private String orderByColumn;

    private String isAsc;

    public static final int DEFAULT_PAGE_NUM = 1;

    public static final int DEFAULT_PAGE_SIZE = 20;
    // Reference-data screens intentionally request full datasets in one page.
    public static final int MAX_PAGE_SIZE = 100000;

    public <T> Page<T> build() {
        Integer pageNum = ObjectUtils.defaultIfNull(getPageNumber(), DEFAULT_PAGE_NUM);
        Integer pageSize = ObjectUtils.defaultIfNull(getPageSize(), DEFAULT_PAGE_SIZE);
        if (pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        Page<T> page = new Page<>(pageNum, pageSize);
        List<OrderItem> orderItems = buildOrderItem();
        if (CollUtil.isNotEmpty(orderItems)) {
            page.addOrder(orderItems);
        }
        return page;
    }

    private List<OrderItem> buildOrderItem() {
        if (StringUtils.isBlank(orderByColumn) || StringUtils.isBlank(isAsc)) {
            return null;
        }
        String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
        orderBy = NamingCase.toUnderlineCase(orderBy);

        isAsc = StringUtils.replaceEach(isAsc, new String[]{"ascending", "descending"}, new String[]{"asc", "desc"});

        String[] orderByArr = orderBy.split(SEPARATOR);
        String[] isAscArr = isAsc.split(SEPARATOR);
        if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
            throw new ServiceException(CommonErrorCode.SORT_PARAM_INVALID);
        }

        List<OrderItem> list = new ArrayList<>();
        for (int i = 0; i < orderByArr.length; i++) {
            String orderByStr = orderByArr[i];
            String isAscStr = isAscArr.length == 1 ? isAscArr[0] : isAscArr[i];
            if ("asc".equals(isAscStr)) {
                list.add(OrderItem.asc(orderByStr));
            } else if ("desc".equals(isAscStr)) {
                list.add(OrderItem.desc(orderByStr));
            } else {
                throw new ServiceException(CommonErrorCode.SORT_PARAM_INVALID);
            }
        }
        return list;
    }
}
