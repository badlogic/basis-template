
package io.marioslab.basis.template;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import io.marioslab.basis.template.parsing.Ast.BinaryOperation;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation.BinaryOperator;
import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.ByteLiteral;
import io.marioslab.basis.template.parsing.Ast.CharacterLiteral;
import io.marioslab.basis.template.parsing.Ast.DoubleLiteral;
import io.marioslab.basis.template.parsing.Ast.Expression;
import io.marioslab.basis.template.parsing.Ast.FloatLiteral;
import io.marioslab.basis.template.parsing.Ast.FunctionCall;
import io.marioslab.basis.template.parsing.Ast.IntegerLiteral;
import io.marioslab.basis.template.parsing.Ast.LongLiteral;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.MemberAccess;
import io.marioslab.basis.template.parsing.Ast.MethodCall;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.NullLiteral;
import io.marioslab.basis.template.parsing.Ast.ShortLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.Text;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation.UnaryOperator;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;

public class AstInterpreter {
	public void interpret (Template template, TemplateContext context, OutputStream out) {
		try {
			Writer writer = new OutputStreamWriter(out);
			for (int i = 0, n = template.getNodes().size(); i < n; i++) {
				Node node = template.getNodes().get(i);
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
		// TODO wrap node interpretation blocks into try/catch and rethrow with location info.
		if (node instanceof Text) {
			out.write(((Text)node).getSpan().getText());
			return null;

		} else if (node instanceof BooleanLiteral) {
			return ((BooleanLiteral)node).getValue();

		} else if (node instanceof DoubleLiteral) {
			return ((DoubleLiteral)node).getValue();

		} else if (node instanceof FloatLiteral) {
			return ((FloatLiteral)node).getValue();

		} else if (node instanceof ByteLiteral) {
			return ((ByteLiteral)node).getValue();

		} else if (node instanceof ShortLiteral) {
			return ((ShortLiteral)node).getValue();

		} else if (node instanceof IntegerLiteral) {
			return ((IntegerLiteral)node).getValue();

		} else if (node instanceof LongLiteral) {
			return ((LongLiteral)node).getValue();

		} else if (node instanceof CharacterLiteral) {
			return ((CharacterLiteral)node).getValue();

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
				if (mapOrArray instanceof int[])
					return ((int[])mapOrArray)[index];
				else if (mapOrArray instanceof float[])
					return ((float[])mapOrArray)[index];
				else if (mapOrArray instanceof double[])
					return ((double[])mapOrArray)[index];
				else if (mapOrArray instanceof boolean[])
					return ((boolean[])mapOrArray)[index];
				else if (mapOrArray instanceof char[])
					return ((char[])mapOrArray)[index];
				else if (mapOrArray instanceof short[])
					return ((short[])mapOrArray)[index];
				else if (mapOrArray instanceof long[])
					return ((long[])mapOrArray)[index];
				else
					return ((Object[])mapOrArray)[index];
			}

		} else if (node instanceof MemberAccess) {
			MemberAccess memberAccess = (MemberAccess)node;
			Object object = interpretNode(memberAccess.getObject(), template, context, out);
			if (object == null) Error.error("Couldn't find object in context.", memberAccess.getSpan());
			Object field = Reflection.getInstance().getField(object, memberAccess.getName().getText());
			if (field == null)
				Error.error("Couldn't find field '" + memberAccess.getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
					memberAccess.getSpan());
			return Reflection.getInstance().getFieldValue(object, field);

		} else if (node instanceof MethodCall) {
			MethodCall methodCall = (MethodCall)node;
			Object object = interpretNode(methodCall.getObject(), template, context, out);
			if (object == null) Error.error("Couldn't find object in context.", methodCall.getSpan());
			Object[] args = new Object[methodCall.getArguments().size()];
			List<Expression> arguments = methodCall.getArguments();
			for (int i = 0, n = args.length; i < n; i++) {
				Expression expr = arguments.get(i);
				args[i] = interpretNode(expr, template, context, out);
			}
			Object method = Reflection.getInstance().getMethod(object, methodCall.getMethod().getName().getText(), args);
			if (method == null) {
				Object field = Reflection.getInstance().getField(object, methodCall.getMethod().getName().getText());
				if (field == null) Error.error(
					"Couldn't find method '" + methodCall.getMethod().getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
					methodCall.getSpan());
				Object function = Reflection.getInstance().getFieldValue(object, field);
				method = Reflection.getInstance().getMethod(function, null, args);
				if (method == null) Error.error("Couldn't find function in field '" + methodCall.getMethod().getName().getText() + "' for object of type '"
					+ object.getClass().getSimpleName() + "'.", node.getSpan());
				return Reflection.getInstance().callMethod(function, method, args);
			}
			return Reflection.getInstance().callMethod(object, method, args);

		} else if (node instanceof FunctionCall) {
			// TODO calls to macros

			FunctionCall call = (FunctionCall)node;
			Object function = interpretNode(call.getFunction(), template, context, out);
			if (function == null) Error.error("Couldn't find function.", node.getSpan());
			Object[] args = new Object[call.getArguments().size()];
			List<Expression> arguments = call.getArguments();
			for (int i = 0, n = args.length; i < n; i++) {
				Expression expr = arguments.get(i);
				args[i] = interpretNode(expr, template, context, out);
			}
			Object method = Reflection.getInstance().getMethod(function, null, args);
			if (method == null) Error.error("Couldn't find function.", node.getSpan());
			return Reflection.getInstance().callMethod(function, method, args);

		} else if (node instanceof UnaryOperation) {
			UnaryOperation op = (UnaryOperation)node;
			Object operand = interpretNode(op.getOperand(), template, context, out);

			if (op.getOperator() == UnaryOperator.Negate) {
				if (operand instanceof Integer)
					return -(Integer)operand;
				else if (operand instanceof Float)
					return -(Float)operand;
				else if (operand instanceof Double)
					return -(Double)operand;
				else if (operand instanceof Byte)
					return -(Byte)operand;
				else if (operand instanceof Short)
					return -(Short)operand;
				else if (operand instanceof Long)
					return -(Long)operand;
				else {
					Error.error("Operand of operator '" + op.getOperator().name() + "' must be a number, got " + operand, op.getSpan());
					return null; // never reached
				}
			} else if (op.getOperator() == UnaryOperator.Not) {
				if (!(operand instanceof Boolean)) Error.error("Operand of operator '" + op.getOperator().name() + "' must be a boolean", op.getSpan());
				return !(Boolean)operand;
			} else {
				return operand;
			}

		} else if (node instanceof BinaryOperation) {
			BinaryOperation op = (BinaryOperation)node;
			Object left = interpretNode(op.getLeftOperand(), template, context, out);
			Object right = interpretNode(op.getRightOperand(), template, context, out);

			if (op.getOperator() == BinaryOperator.Addition) {
				if (!(left instanceof Number || left instanceof String))
					Error.error("Left operand must be a number or String, got " + left + ".", op.getLeftOperand().getSpan());
				if (!(right instanceof Number || right instanceof String))
					Error.error("Right operand must be a number or String, got " + right + ".", op.getRightOperand().getSpan());

				if (left instanceof String || right instanceof String) {
					return left.toString() + right.toString();
				} else if (left instanceof Double || right instanceof Double) {
					return ((Number)left).doubleValue() + ((Number)right).doubleValue();
				} else if (left instanceof Float || right instanceof Float) {
					return ((Number)left).floatValue() + ((Number)right).floatValue();
				} else if (left instanceof Long || right instanceof Long) {
					return ((Number)left).longValue() + ((Number)right).longValue();
				} else if (left instanceof Integer || right instanceof Integer) {
					return ((Number)left).intValue() + ((Number)right).intValue();
				} else if (left instanceof Short || right instanceof Short) {
					return ((Number)left).shortValue() + ((Number)right).shortValue();
				} else if (left instanceof Byte || right instanceof Byte) {
					return ((Number)left).byteValue() + ((Number)right).byteValue();
				} else {
					Error.error("Operands for addition operator must be numbers or strings, got " + left + ", " + right + ".", node.getSpan());
					return null; // never reached
				}
			}
			if (op.getOperator() == BinaryOperator.Subtraction) {
				if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
				if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

				if (left instanceof Double || right instanceof Double) {
					return ((Number)left).doubleValue() - ((Number)right).doubleValue();
				} else if (left instanceof Float || right instanceof Float) {
					return ((Number)left).floatValue() - ((Number)right).floatValue();
				} else if (left instanceof Long || right instanceof Long) {
					return ((Number)left).longValue() - ((Number)right).longValue();
				} else if (left instanceof Integer || right instanceof Integer) {
					return ((Number)left).intValue() - ((Number)right).intValue();
				} else if (left instanceof Short || right instanceof Short) {
					return ((Number)left).shortValue() - ((Number)right).shortValue();
				} else if (left instanceof Byte || right instanceof Byte) {
					return ((Number)left).byteValue() - ((Number)right).byteValue();
				} else {
					Error.error("Operands for addition operator must be numbers" + left + ", " + right + ".", node.getSpan());
					return null; // never reached
				}
			} else {
				Error.error("Binary operator " + op.getOperator().name() + " not implemented", node.getSpan());
				return null;
			}
		} else {
			Error.error("Interpretation of node " + node.getClass().getSimpleName() + " not implemented.", node.getSpan());
			return null; // never reached
		}
	}
}
