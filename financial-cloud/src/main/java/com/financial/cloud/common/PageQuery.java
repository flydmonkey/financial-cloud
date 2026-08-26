package com.financial.cloud.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.NamingCase;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.util.SqlUtil;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/14 15:12
 */

@Data
public class PageQuery implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1082122094613059399L;

	public static final String SEPARATOR = ",";

    /**
     * åé¡µå¤§å°
     */
    private Integer pageSize;

    /**
     * å½åé¡µæ°
     */
    private Integer pageNumber;

    /**
     * æåºå?
     */
    private String orderByColumn;

    /**
     * æåºçæ¹ådescæèasc
     */
    private String isAsc;

    /**
     * å½åè®°å½èµ·å§ç´¢å¼ é»è®¤å?
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * æ¯é¡µæ¾ç¤ºè®°å½æ?é»è®¤å?
     */
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

    /**
     * æå»ºæåº
     *
     * æ¯æçç¨æ³å¦ä¸?
     * {isAsc:"asc",orderByColumn:"id"} order by id asc
     * {isAsc:"asc",orderByColumn:"id,createTime"} order by id asc,create_time asc
     * {isAsc:"desc",orderByColumn:"id,createTime"} order by id desc,create_time desc
     * {isAsc:"asc,desc",orderByColumn:"id,createTime"} order by id asc,create_time desc
     */
    private List<OrderItem> buildOrderItem() {
        if (StringUtils.isBlank(orderByColumn) || StringUtils.isBlank(isAsc)) {
            return null;
        }
        String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
        orderBy = NamingCase.toUnderlineCase(orderBy);

        // å¼å®¹åç«¯æåºç±»å
        isAsc = StringUtils.replaceEach(isAsc, new String[]{"ascending", "descending"}, new String[]{"asc", "desc"});

        String[] orderByArr = orderBy.split(SEPARATOR);
        String[] isAscArr = isAsc.split(SEPARATOR);
        if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
            throw new ServiceException("æåºåæ°æè¯¯");
        }

        List<OrderItem> list = new ArrayList<>();
        // æ¯ä¸ªå­æ®µåèªæåº
        for (int i = 0; i < orderByArr.length; i++) {
            String orderByStr = orderByArr[i];
            String isAscStr = isAscArr.length == 1 ? isAscArr[0] : isAscArr[i];
            if ("asc".equals(isAscStr)) {
                list.add(OrderItem.asc(orderByStr));
            } else if ("desc".equals(isAscStr)) {
                list.add(OrderItem.desc(orderByStr));
            } else {
                throw new ServiceException("æåºåæ°æè¯¯");
            }
        }
        return list;
    }
}
