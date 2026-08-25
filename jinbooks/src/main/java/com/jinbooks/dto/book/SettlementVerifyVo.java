package com.jinbooks.dto.book;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class SettlementVerifyVo {
	int    id;
	
	String item;
	
	boolean result;
	
	boolean warning;
}
