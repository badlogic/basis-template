
package io.marioslab.basis.template;

import java.util.List;

import io.marioslab.basis.template.parsing.Ast.Node;

public class Template {
	private final List<Node> nodes;

	public Template (List<Node> nodes) {
		this.nodes = nodes;
	}

	List<Node> getNodes () {
		return nodes;
	}
}
