
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Parser.Macros;

public class Template {
	private final List<Node> nodes;
	private final Macros macros;
	private final List<Include> includes;

	Template (List<Node> nodes, Macros macros, List<Include> includes) {
		this.nodes = nodes;
		this.macros = macros;
		this.includes = includes;

		for (Macro macro : macros.values())
			macro.setTemplate(this);
	}

	List<Node> getNodes () {
		return nodes;
	}

	Macros getMacros () {
		return macros;
	}

	public List<Include> getIncludes () {
		return includes;
	}

	public String render (TemplateContext context) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		render(context, out);
		try {
			out.close();
			return new String(out.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void render (TemplateContext context, OutputStream out) {
		new AstInterpreter().interpret(this, context, out);
	}
}
