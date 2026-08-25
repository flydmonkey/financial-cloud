package com.jinbooks.dto.book;

import com.jinbooks.domain.voucher.VoucherTemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SettlementCarryforwardVo extends VoucherTemplate{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4969126049394752855L;

	String voucherId;
	
	
}
