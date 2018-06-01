
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.ParserResult;

public class Template {
	private final List<Node> nodes;
	private final List<Macro> macros;
	private final TemplateLoader loader;

	Template (List<Node> nodes, List<Macro> macros, TemplateLoader loader) {
		this.nodes = nodes;
		this.macros = macros;
		this.loader = loader;
	}

	List<Node> getNodes () {
		return nodes;
	}

	List<Macro> getMacros () {
		return macros;
	}

	TemplateLoader getLoader () {
		return loader;
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

	public static Template load (String path, TemplateLoader loader) {
		ParserResult result = new Parser().parse(loader.load(path));
		return new Template(result.getNodes(), result.getMacros(), loader);
	}
}
