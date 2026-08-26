package com.financial.cloud.authn.session;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSessions  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2525429081773580767L;

	LinkedBlockingQueue<String> sessionIdQueue = new LinkedBlockingQueue<>(); 
	
	LinkedBlockingQueue<String> mobileSessionIdQueue = new LinkedBlockingQueue<>();
	
	public LinkedBlockingQueue<String> getSessionIdQueue() {
		return sessionIdQueue;
	}

	public void setSessionIdQueue(LinkedBlockingQueue<String> sessionIdQueue) {
		this.sessionIdQueue = sessionIdQueue;
	}

	public LinkedBlockingQueue<String> getMobileSessionIdQueue() {
		return mobileSessionIdQueue;
	}

	public void setMobileSessionIdQueue(LinkedBlockingQueue<String> mobileSessionIdQueue) {
		this.mobileSessionIdQueue = mobileSessionIdQueue;
	}

}
