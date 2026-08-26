package com.financial.cloud.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数控件的节点 使用HashMap<String,Object> attr存储节点数据.
 * 
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
public class TreeNode {
	String key;
	String code;
	String title;
	
	String codePath;
	String namePath;
	
	String parentKey;
	String parentCode;
	String parentTitle;
	
	
	boolean expanded;
	boolean isLeaf;
	
    // TreeNode
    Object attrs;

    public TreeNode(String key, String title) {
        this.key = key;
        this.title = title;
    }

}
