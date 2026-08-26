package com.financial.cloud.authn.handler;

import java.util.Date;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.support.AuthorizationUtils;

@Component
public class PersistFieldAutoFillHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		this.fillStrategy(metaObject , "createdDate", new Date());
		this.fillStrategy(metaObject , "modifiedDate", new Date());
		this.fillStrategy(metaObject, "deleted", "n");
		try {
			SignedPrincipal principal = getPrincipal();
			if(principal != null) {
				this.fillStrategy(metaObject , "createdBy", principal.getUserId());
				this.fillStrategy(metaObject , "modifiedBy", principal.getUserId());
			}
		} catch (Exception e) {
			this.fillStrategy(metaObject , "createdBy", "0");
			this.fillStrategy(metaObject , "modifiedBy", "0");
		}
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		try {
			SignedPrincipal principal = getPrincipal();
			if(principal != null) {
				this.fillStrategy(metaObject , "modifiedBy", principal.getUserId());
			}
			this.setFieldValByName("modifiedDate", new Date(), metaObject);
		} catch (Exception e) {
			this.setFieldValByName("modifiedDate", new Date(), metaObject);
			this.fillStrategy(metaObject , "modifiedBy", "0");
		}

	}

	/**
	 * 获取principal , 忽略异常情况
	 * @return
	 */
	SignedPrincipal getPrincipal() {
		SignedPrincipal principal = null;
		try {
			principal = AuthorizationUtils.getPrincipal();
		}catch(Exception e) {
			//
		}
		return principal;
	}

}
