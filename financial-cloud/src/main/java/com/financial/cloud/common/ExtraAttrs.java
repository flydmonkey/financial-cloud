package com.financial.cloud.common;


import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import com.financial.cloud.util.JsonUtils;

import lombok.Data;

@Slf4j
@Data
public class ExtraAttrs {

	List <ExtraAttr> attrs ;

	/**
	 *
	 */
	public ExtraAttrs() {
		super();
	}

	/**
	 *
	 */
	public ExtraAttrs(String arrayJsonString) {
		this.deserialize(arrayJsonString);
	}

	public void put(String attr,String value) {
		if(attrs == null){
			attrs = new ArrayList<>();
		}
		this.attrs.add(new ExtraAttr(attr,value));
	}

	public void put(String attr,String type,String value) {
		if(attrs == null){
			attrs = new ArrayList<>();
		}
		this.attrs.add(new ExtraAttr(attr,type,value));
	}

	public String get(String attr) {
		String value = null;
		if(CollectionUtils.isNotEmpty(attrs)){
			for(ExtraAttr extraAttr :attrs){
				if(extraAttr.getAttr().equals(attr)){
					value = extraAttr.getValue();
				}
			}
		}
		return value;
	}

	public Map<String,ExtraAttr > toHashMap(){
		HashMap<String,ExtraAttr > extraAttrsHashMap = new HashMap<>();
		for(ExtraAttr extraAttr : attrs){
			extraAttrsHashMap.put(extraAttr.getAttr(), extraAttr);
		}
		log.debug("extraAttrs HashMap {}" , extraAttrsHashMap);
		return extraAttrsHashMap;
	}

	public Properties toProperties(){
		Properties properties=new Properties();
		for(ExtraAttr extraAttr :attrs){
			properties.put(extraAttr.getAttr(), extraAttr.getValue());
		}
		log.debug("extraAttrs Properties {}" ,properties);
		return properties;
	}
	
	public String serialize(){
		String jsonString = JsonUtils.toString(attrs);
		log.debug("jsonString {}" , jsonString);
		return jsonString;
	}
	
	public void deserialize(String arrayJsonString) {
		String extraAttrsJsonString = "{\"attrs\":" + arrayJsonString + "}";
		log.debug("Extra Attrs Json String {}" , extraAttrsJsonString);
		ExtraAttrs jsonAttrs = JsonUtils.stringToObject(extraAttrsJsonString, ExtraAttrs.class);
		this.attrs = jsonAttrs.getAttrs();
	}

}
