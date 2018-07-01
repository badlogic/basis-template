
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import io.marioslab.basis.template.interpreter.AstInterpreter;
import io.marioslab.basis.template.parsing.Ast;
import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.Macros;

/** A template is loaded by a {@link TemplateLoader} from a file marked up with the basis-template language. The template can be
 * rendered to a {@link String} or {@link OutputStream} by calling one of the <code>render()</code> methods. The
 * {@link TemplateContext} passed to the <code>render()</code> methods is used to look up variable values referenced in the
 * template. */
public class Template {
	private final List<Node> nodes;
	private final Macros macros;
	private final List<Include> includes;

	/** Internal. Created by {@link Parser}. **/
	public Template (List<Node> nodes, Macros macros, List<Include> includes) {
		this.nodes = nodes;
		this.macros = macros;
		this.includes = includes;

		for (Macro macro : macros.values())
			macro.setTemplate(this);
	}

	/** Internal. The AST nodes representing this template after parsing. See {@link Ast}. Used by {@link AstInterpreter}. **/
	public List<Node> getNodes () {
		return nodes;
	}

	/** Internal. The top-level macros defined in the template. See {@link Macro}. Used by the {@link AstInterpreter}. **/
	public Macros getMacros () {
		return macros;
	}

	/** Internal. The includes referenced in this template. A {@link TemplateLoader} is responsible for setting the template
	 * instances referenced by includes. **/
	public List<Include> getIncludes () {
		return includes;
	}

	/** Renders the template using the TemplateContext to resolve variable values referenced in the template. **/
	public String render (TemplateContext context) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(2 * 1024);
		render(context, out);
		try {
			out.close();
			return new String(out.toByteArray(), "UTF-8");
		} catch (IOException e) {
			Error.error("Couldn't render template to string, " + e.getMessage(), nodes.get(0).getSpan());
			return null; // never reached
		}
	}

	/** Renderes the template to the OutputStream as UTF-8, using the TemplateContext to resolve variable values referenced in the
	 * template. If a return statement with a return value was encountered, the method returns that return value. Otherwise null is
	 * returned. **/
	public Object render (TemplateContext context, OutputStream out) {
		return AstInterpreter.interpret(this, context, out);
	}

	/** Evaluates this template using the TemplateContext to resolve variable values referenced in the template. Returns any value
	 * returned by the template, or null. **/
	public Object evaluate (TemplateContext context) {
		return AstInterpreter.interpret(this, context, new OutputStream() {
			@Override
			public void write (int b) throws IOException {
			}
		});
	}
}
