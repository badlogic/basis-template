
package tech.gospel.basis.template.interpreter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateContext;
import tech.gospel.basis.template.parsing.Ast;
import tech.gospel.basis.template.Error;

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
	public static Object interpret (Template template, TemplateContext context, OutputStream out) {
		try {
			Object result = interpretNodeList(template.getNodes(), template, context, out);
			if (result == Ast.Return.RETURN_SENTINEL) {
				return ((Ast.Return.ReturnValue)result).getValue();
			} else {
				return null;
			}
		} catch (Throwable t) {
			if (t instanceof Error.TemplateException)
				throw (Error.TemplateException)t;
			else {
				Error.error("Couldn't interpret node list due to I/O error, " + t.getMessage(), template.getNodes().get(0).getSpan());
				return null; // never reached
			}
		} finally {
			// clear out RETURN_SENTINEL as it uses a ThreadLocal and would leak memory otherwise
			Ast.Return.RETURN_SENTINEL.setValue(null);
		}
	}

	public static Object interpretNodeList (List<Ast.Node> nodes, Template template, TemplateContext context, OutputStream out) throws IOException {
		for (int i = 0, n = nodes.size(); i < n; i++) {
			Ast.Node node = nodes.get(i);
			Object value = node.evaluate(template, context, out);
			if (value != null) {
				if (value == Ast.Break.BREAK_SENTINEL || value == Ast.Continue.CONTINUE_SENTINEL || value == Ast.Return.RETURN_SENTINEL)
					return value;
				else
					out.write(value.toString().getBytes("UTF-8"));
			}
		}
		return null;
	}
}
