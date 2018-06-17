
package io.marioslab.basis.template.interpreter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.parsing.Ast;
import io.marioslab.basis.template.parsing.Ast.Node;

/**
 * <p>
 * Interprets a Template given a TemplateContext to lookup variable values in and writes the evaluation results to an output
 * stream. Uses the global {@link Reflection} instance as returned by {@link Reflection#getInstance()} to access members and call
 * methods.
 * </p>
 *
 * <p>
 * The interpeter traverses the AST as stored in {@link Template#getNodes()}. the interpeter has a method for each AST node type
 * (see {@link Ast} that evaluates that node. A node may return a value, to be used in the interpretation of a parent node or to
 * be written to the output stream.
 * </p>
 **/
public class AstInterpreter {
	public static void interpret (Template template, TemplateContext context, OutputStream out) {
		try {
			interpretNodeList(template.getNodes(), template, context, out);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static void interpretNodeList (List<Node> nodes, Template template, TemplateContext context, OutputStream out) throws IOException {
		for (int i = 0, n = nodes.size(); i < n; i++) {
			Node node = nodes.get(i);
			Object value = node.evaluate(template, context, out);
			if (value != null) {
				out.write(value.toString().getBytes("UTF-8"));
			}
		}
	}
}
