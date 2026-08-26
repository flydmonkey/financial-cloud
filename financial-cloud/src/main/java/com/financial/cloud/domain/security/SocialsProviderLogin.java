package com.financial.cloud.domain.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
public class SocialsProviderLogin implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2672107566766342357L;
	
	List<SocialsProvider> providers = new ArrayList<>();
	
	String qrScan = null;

	public SocialsProviderLogin(List<SocialsProvider> socialSignOnProviders) {
		super();
		this.providers = socialSignOnProviders;
	}
}
