
package io.marioslab.basis.template;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.FloatLiteral;
import io.marioslab.basis.template.parsing.Ast.IntegerLiteral;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.NullLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.Text;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;

public class AstInterpreter {
	public void interpret (Template template, TemplateContext context, OutputStream out) {
		try {
			Writer writer = new OutputStreamWriter(out);
			for (Node node : template.getNodes()) {
				Object value = interpretNode(node, template, context, writer);
				if (value != null) {
					writer.write(String.valueOf(value));
				}
			}
			writer.flush();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private Object interpretNode (Node node, Template template, TemplateContext context, Writer out) throws IOException {
		if (node instanceof Text) {
			out.write(((Text)node).getSpan().getText());
			return null;

		} else if (node instanceof BooleanLiteral) {
			return ((BooleanLiteral)node).getValue();

		} else if (node instanceof FloatLiteral) {
			return ((FloatLiteral)node).getValue();

		} else if (node instanceof IntegerLiteral) {
			return ((IntegerLiteral)node).getValue();

		} else if (node instanceof StringLiteral) {
			return ((StringLiteral)node).getValue();

		} else if (node instanceof NullLiteral) {
			return null;

		} else if (node instanceof VariableAccess) {
			VariableAccess varAccess = (VariableAccess)node;
			Object value = context.get(varAccess.getVariableName().getText());
			if (value == null) Error.error("Couldn't find variable '" + varAccess.getVariableName().getText() + "' in context.", varAccess.getSpan());
			return value;

		} else if (node instanceof MapOrArrayAccess) {
			MapOrArrayAccess mapAccess = (MapOrArrayAccess)node;
			Object mapOrArray = interpretNode(mapAccess.getMapOrArray(), template, context, out);
			if (mapOrArray == null) Error.error("Couldn't find map or array in context.", mapAccess.getSpan());
			Object keyOrIndex = interpretNode(mapAccess.getKeyOrIndex(), template, context, out);
			if (keyOrIndex == null) Error.error("Couldn't evaluate key or index.", mapAccess.getKeyOrIndex().getSpan());

			if (mapOrArray instanceof Map) {
				return ((Map)mapOrArray).get(keyOrIndex);
			} else if (mapOrArray instanceof List) {
				if (!(keyOrIndex instanceof Number)) {
					Error.error("List index must be an integer, but was " + keyOrIndex.getClass().getSimpleName(), mapAccess.getKeyOrIndex().getSpan());
				}
				int index = ((Number)keyOrIndex).intValue();
				return ((List)mapOrArray).get(index);
			} else {
				if (!(keyOrIndex instanceof Number)) {
					Error.error("Array index must be an integer, but was " + keyOrIndex.getClass().getSimpleName(), mapAccess.getKeyOrIndex().getSpan());
				}
				int index = ((Number)keyOrIndex).intValue();
				if (mapOrArray instanceof boolean[])
					return ((boolean[])mapOrArray)[index];
				else if (mapOrArray instanceof char[])
					return ((char[])mapOrArray)[index];
				else if (mapOrArray instanceof short[])
					return ((short[])mapOrArray)[index];
				else if (mapOrArray instanceof int[])
					return ((int[])mapOrArray)[index];
				else if (mapOrArray instanceof long[])
					return ((long[])mapOrArray)[index];
				else if (mapOrArray instanceof float[])
					return ((float[])mapOrArray)[index];
				else if (mapOrArray instanceof double[])
					return ((double[])mapOrArray)[index];
				else
					return ((Object[])mapOrArray)[index];
			}
		} else {
			Error.error("Interpretation of node " + node.getClass().getSimpleName() + " not implemented.", node.getSpan());
			return null; // never reached
		}
	}
}
