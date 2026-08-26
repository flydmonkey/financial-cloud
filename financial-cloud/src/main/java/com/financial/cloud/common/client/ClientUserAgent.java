package com.financial.cloud.common.client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientUserAgent {

	String platform;

	String name;
	
	String userAgentHash;

}
