/**
 *
 */
package com.jinbooks.common;

import lombok.Data;

/**
 * @author Administrator
 *
 */
@Data
public class ExtraAttr {

	String attr;
	String name;
	String type;
	String value;

	public ExtraAttr() {
		super();
	}

	/**
	 * @param attr
	 * @param value
	 */
	public ExtraAttr(String attr, String value) {
		super();
		this.attr = attr;
		this.value = value;
	}

	/**
	 * @param attr
	 * @param type
	 * @param value
	 */
	public ExtraAttr(String attr, String type, String value) {
		super();
		this.attr = attr;
		this.type = type;
		this.value = value;
	}

	/**
	 * @param attr
	 * @param name
	 * @param type
	 * @param value
	 */
	public ExtraAttr(String attr,String name, String type, String value) {
		super();
		this.attr = attr;
		this.name = name;
		this.type = type;
		this.value = value;
	}
}
