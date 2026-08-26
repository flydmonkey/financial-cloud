package com.financial.cloud.common;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数控件节点列表 列表的元素为TreeNode
 * 
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
public class TreeAttributes {

	TreeNode rootNode;

	int nodeCount;

	List<TreeNode> nodes = new ArrayList<>();

	/**
	 * 新增节点到列表
	 * 
	 * @param treeNode
	 */
	public void addNode(TreeNode treeNode) {
		this.nodes.add(treeNode);
	}

}
