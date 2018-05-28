
package io.marioslab.basis.template;

import java.util.List;

import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.Node;

public class Template {
	private final List<Node> nodes;
	private final List<Macro> macros;

	public Template (List<Node> nodes, List<Macro> macros) {
		this.nodes = nodes;
		this.macros = macros;
	}

	List<Node> getNodes () {
		return nodes;
	}

	List<Macro> getMacros () {
		return macros;
	}
}
