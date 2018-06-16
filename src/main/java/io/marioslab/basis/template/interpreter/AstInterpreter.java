
package io.marioslab.basis.template.interpreter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.marioslab.basis.template.parsing.Ast.BinaryOperation;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation.BinaryOperator;
import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.ByteLiteral;
import io.marioslab.basis.template.parsing.Ast.CharacterLiteral;
import io.marioslab.basis.template.parsing.Ast.DoubleLiteral;
import io.marioslab.basis.template.parsing.Ast.Expression;
import io.marioslab.basis.template.parsing.Ast.FloatLiteral;
import io.marioslab.basis.template.parsing.Ast.ForStatement;
import io.marioslab.basis.template.parsing.Ast.FunctionCall;
import io.marioslab.basis.template.parsing.Ast.IfStatement;
import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.IntegerLiteral;
import io.marioslab.basis.template.parsing.Ast.LongLiteral;
import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.MemberAccess;
import io.marioslab.basis.template.parsing.Ast.MethodCall;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.NullLiteral;
import io.marioslab.basis.template.parsing.Ast.ShortLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.TernaryOperation;
import io.marioslab.basis.template.parsing.Ast.Text;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation.UnaryOperator;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;
import io.marioslab.basis.template.parsing.Ast.WhileStatement;
import io.marioslab.basis.template.parsing.Parser.Macros;
import io.marioslab.basis.template.Error;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.parsing.Ast;
import io.marioslab.basis.template.parsing.Span;

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

	private static void interpretNodeList (List<Node> nodes, Template template, TemplateContext context, OutputStream out) throws IOException {
		for (int i = 0, n = nodes.size(); i < n; i++) {
			Node node = nodes.get(i);
			Object value = interpretNode(node, template, context, out);
			if (value != null) {
				out.write(value.toString().getBytes("UTF-8"));
			}
		}
	}

	private static Object interpretVariableAccess (VariableAccess varAccess, Template template, TemplateContext context, OutputStream out) {
		Object value = context.get(varAccess.getVariableName().getText());
		if (value == null) Error.error("Couldn't find variable '" + varAccess.getVariableName().getText() + "' in context.", varAccess.getSpan());
		return value;
	}

	@SuppressWarnings("rawtypes")
	private static Object interpretMapOrArrayAccess (MapOrArrayAccess mapAccess, Template template, TemplateContext context, OutputStream out)
		throws IOException {
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
	}

	private static Object interpretMemberAccess (MemberAccess memberAccess, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object object = interpretNode(memberAccess.getObject(), template, context, out);
		if (object == null) Error.error("Couldn't find object in context.", memberAccess.getSpan());
		Object field = memberAccess.getCachedMember();
		if (field != null) {
			try {
				return Reflection.getInstance().getFieldValue(object, field);
			} catch (Throwable t) {
				// fall through
			}
		}

		field = Reflection.getInstance().getField(object, memberAccess.getName().getText());
		if (field == null)
			Error.error("Couldn't find field '" + memberAccess.getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
				memberAccess.getSpan());
		memberAccess.setCachedMember(field);
		return Reflection.getInstance().getFieldValue(object, field);
	}

	private static Object interpretMethodCall (MethodCall methodCall, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object object = interpretNode(methodCall.getObject(), template, context, out);
		if (object == null) Error.error("Couldn't find object in context.", methodCall.getSpan());

		Object[] argumentValues = methodCall.getCachedArguments();
		List<Expression> arguments = methodCall.getArguments();
		for (int i = 0, n = argumentValues.length; i < n; i++) {
			Expression expr = arguments.get(i);
			argumentValues[i] = interpretNode(expr, template, context, out);
		}

		// if the object we call the method on is a Macros instance, lookup the macro by name
		// and execute its node list
		if (object instanceof Macros) {
			Macros macros = (Macros)object;
			Macro macro = macros.get(methodCall.getMethod().getName().getText());
			if (macro != null) {
				if (macro.getArgumentNames().size() != arguments.size())
					Error.error("Expected " + macro.getArgumentNames().size() + " arguments, got " + arguments.size(), methodCall.getSpan());
				TemplateContext macroContext = macro.getMacroContext();
				for (int i = 0; i < arguments.size(); i++) {
					Object arg = argumentValues[i];
					String name = macro.getArgumentNames().get(i).getText();
					macroContext.set(name, arg);
				}
				interpretNodeList(macro.getBody(), macro.getTemplate(), macroContext, out);
				return null;
			}
		}

		// Otherwise try to find a corresponding method or field pointing to a lambda.
		Object method = methodCall.getCachedMethod();
		if (method != null) {
			try {
				return Reflection.getInstance().callMethod(object, method, argumentValues);
			} catch (Throwable t) {
				// fall through
			}
		}

		method = Reflection.getInstance().getMethod(object, methodCall.getMethod().getName().getText(), argumentValues);
		if (method != null) {
			// found the method on the object, call it
			methodCall.setCachedMethod(method);
			return Reflection.getInstance().callMethod(object, method, argumentValues);
		} else {
			// didn't find the method on the object, try to find a field pointing to a lambda
			Object field = Reflection.getInstance().getField(object, methodCall.getMethod().getName().getText());
			if (field == null) Error.error(
				"Couldn't find method '" + methodCall.getMethod().getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
				methodCall.getSpan());
			Object function = Reflection.getInstance().getFieldValue(object, field);
			method = Reflection.getInstance().getMethod(function, null, argumentValues);
			if (method == null) Error.error("Couldn't find function in field '" + methodCall.getMethod().getName().getText() + "' for object of type '"
				+ object.getClass().getSimpleName() + "'.", methodCall.getSpan());
			return Reflection.getInstance().callMethod(function, method, argumentValues);
		}
	}

	private static Object interpretFunctionCall (FunctionCall call, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object[] argumentValues = call.getCachedArguments();
		List<Expression> arguments = call.getArguments();
		for (int i = 0, n = argumentValues.length; i < n; i++) {
			Expression expr = arguments.get(i);
			argumentValues[i] = interpretNode(expr, template, context, out);
		}

		// This is a special case to handle template level macros. If a call to a macro is
		// made, evaluating the function expression will result in an exception, as the
		// function name can't be found in the context. Instead we need to manually check
		// if the function expression is a VariableAccess and if so, if it can be found
		// in the context.
		Object function = null;
		if (call.getFunction() instanceof VariableAccess) {
			VariableAccess varAccess = (VariableAccess)call.getFunction();
			function = context.get(varAccess.getVariableName().getText());
		} else {
			function = interpretNode(call.getFunction(), template, context, out);
		}

		if (function != null) {
			Object method = call.getCachedFunction();
			if (method != null) {
				try {
					return Reflection.getInstance().callMethod(function, method, argumentValues);
				} catch (Throwable t) {
					// fall through
				}
			}
			method = Reflection.getInstance().getMethod(function, null, argumentValues);
			if (method == null) Error.error("Couldn't find function.", call.getSpan());
			call.setCachedFunction(method);
			return Reflection.getInstance().callMethod(function, method, argumentValues);
		} else {
			// Check if this is a call to a macro defined in this template
			if (call.getFunction() instanceof VariableAccess) {
				String functionName = ((VariableAccess)call.getFunction()).getVariableName().getText();
				Macros macros = template.getMacros();
				Macro macro = macros.get(functionName);
				if (macro != null) {
					if (macro.getArgumentNames().size() != arguments.size())
						Error.error("Expected " + macro.getArgumentNames().size() + " arguments, got " + arguments.size(), call.getSpan());
					TemplateContext macroContext = macro.getMacroContext();
					for (int i = 0; i < arguments.size(); i++) {
						Object arg = argumentValues[i];
						String name = macro.getArgumentNames().get(i).getText();
						macroContext.set(name, arg);
					}
					interpretNodeList(macro.getBody(), macro.getTemplate(), macroContext, out);
					return null;
				}
			}
			Error.error("Couldn't find function.", call.getSpan());
			return null; // never reached
		}
	}

	private static Object interpretUnaryOperation (UnaryOperation op, Template template, TemplateContext context, OutputStream out) throws IOException {
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
	}

	private static Object interpretBinaryOperation (BinaryOperation op, Template template, TemplateContext context, OutputStream out) throws IOException {

		if (op.getOperator() == BinaryOperator.Assignment) {
			if (!(op.getLeftOperand() instanceof VariableAccess)) Error.error("Can only assign to top-level variables in context.", op.getLeftOperand().getSpan());
			Object value = interpretNode(op.getRightOperand(), template, context, out);
			context.set(((VariableAccess)op.getLeftOperand()).getVariableName().getText(), value);
			return null;
		}

		Object left = interpretNode(op.getLeftOperand(), template, context, out);
		Object right = op.getOperator() == BinaryOperator.And || op.getOperator() == BinaryOperator.Or ? null
			: interpretNode(op.getRightOperand(), template, context, out);

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
				Error.error("Operands for addition operator must be numbers or strings, got " + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Subtraction) {
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
				Error.error("Operands for subtraction operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Multiplication) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() * ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() * ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() * ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() * ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() * ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() * ((Number)right).byteValue();
			} else {
				Error.error("Operands for multiplication operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Division) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() / ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() / ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() / ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() / ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() / ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() / ((Number)right).byteValue();
			} else {
				Error.error("Operands for division operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Modulo) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() % ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() % ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() % ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() % ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() % ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() % ((Number)right).byteValue();
			} else {
				Error.error("Operands for modulo operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Less) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() < ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() < ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() < ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() < ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() < ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() < ((Number)right).byteValue();
			} else {
				Error.error("Operands for less operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.LessEqual) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() <= ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() <= ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() <= ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() <= ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() <= ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() <= ((Number)right).byteValue();
			} else {
				Error.error("Operands for less/equal operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Greater) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() > ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() > ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() > ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() > ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() > ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() > ((Number)right).byteValue();
			} else {
				Error.error("Operands for greater operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.GreaterEqual) {
			if (!(left instanceof Number)) Error.error("Left operand must be a number, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Number)) Error.error("Right operand must be a number, got " + right + ".", op.getRightOperand().getSpan());

			if (left instanceof Double || right instanceof Double) {
				return ((Number)left).doubleValue() >= ((Number)right).doubleValue();
			} else if (left instanceof Float || right instanceof Float) {
				return ((Number)left).floatValue() >= ((Number)right).floatValue();
			} else if (left instanceof Long || right instanceof Long) {
				return ((Number)left).longValue() >= ((Number)right).longValue();
			} else if (left instanceof Integer || right instanceof Integer) {
				return ((Number)left).intValue() >= ((Number)right).intValue();
			} else if (left instanceof Short || right instanceof Short) {
				return ((Number)left).shortValue() >= ((Number)right).shortValue();
			} else if (left instanceof Byte || right instanceof Byte) {
				return ((Number)left).byteValue() >= ((Number)right).byteValue();
			} else {
				Error.error("Operands for greater/equal operator must be numbers" + left + ", " + right + ".", op.getSpan());
				return null; // never reached
			}
		} else if (op.getOperator() == BinaryOperator.Equal) {
			return left.equals(right);
		} else if (op.getOperator() == BinaryOperator.NotEqual) {
			return !left.equals(right);
		} else if (op.getOperator() == BinaryOperator.And) {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(Boolean)left) return false;
			right = interpretNode(op.getRightOperand(), template, context, out);
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", op.getRightOperand().getSpan());
			return (Boolean)left && (Boolean)right;
		} else if (op.getOperator() == BinaryOperator.Or) {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", op.getLeftOperand().getSpan());
			if ((Boolean)left) return true;
			right = interpretNode(op.getRightOperand(), template, context, out);
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", op.getRightOperand().getSpan());
			return (Boolean)left || (Boolean)right;
		} else if (op.getOperator() == BinaryOperator.Xor) {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", op.getLeftOperand().getSpan());
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", op.getRightOperand().getSpan());
			return (Boolean)left ^ (Boolean)right;
		} else {
			Error.error("Binary operator " + op.getOperator().name() + " not implemented", op.getSpan());
			return null;
		}

	}

	private static Object interpretTernaryOperation (TernaryOperation op, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object condition = interpretNode(op.getCondition(), template, context, out);
		if (!(condition instanceof Boolean)) Error.error("Condition of ternary operator must be a boolean, got " + condition + ".", op.getSpan());
		return ((Boolean)condition) ? interpretNode(op.getTrueExpression(), template, context, out)
			: interpretNode(op.getFalseExpression(), template, context, out);
	}

	@SuppressWarnings("rawtypes")
	private static Object interpretFor (ForStatement forStatement, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object mapOrArray = interpretNode(forStatement.getMapOrArray(), template, context, out);
		if (mapOrArray == null) Error.error("Expected a map or array, got null.", forStatement.getMapOrArray().getSpan());
		String valueName = forStatement.getValueName().getText();

		if (mapOrArray instanceof Map) {
			Map map = (Map)mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (Object entry : map.entrySet()) {
					Entry e = (Entry)entry;
					context.set(keyName, e.getKey());
					context.set(valueName, e.getValue());
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (Object value : map.values()) {
					context.set(valueName, value);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof Iterable) {
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				Iterator iter = ((Iterable)mapOrArray).iterator();
				int i = 0;
				while (iter.hasNext()) {
					context.set(keyName, i++);
					context.set(valueName, iter.next());
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				Iterator iter = ((Iterable)mapOrArray).iterator();
				while (iter.hasNext()) {
					context.set(valueName, iter.next());
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof int[]) {
			int[] array = (int[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof float[]) {
			float[] array = (float[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof double[]) {
			double[] array = (double[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof boolean[]) {
			boolean[] array = (boolean[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof char[]) {
			char[] array = (char[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof short[]) {
			short[] array = (short[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof byte[]) {
			byte[] array = (byte[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof long[]) {
			long[] array = (long[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else if (mapOrArray instanceof Object[]) {
			Object[] array = (Object[])mapOrArray;
			if (forStatement.getIndexOrKeyName() != null) {
				context.push();
				String keyName = forStatement.getIndexOrKeyName().getText();
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(keyName, i);
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
				context.pop();
			} else {
				for (int i = 0, n = array.length; i < n; i++) {
					context.set(valueName, array[i]);
					interpretNodeList(forStatement.getBody(), template, context, out);
				}
			}
		} else {
			Error.error("Expected a map, an array or an iterable, got " + mapOrArray, forStatement.getMapOrArray().getSpan());
		}
		return null;
	}

	private static Object interpretIf (IfStatement ifStatement, Template template, TemplateContext context, OutputStream out) throws IOException {
		Object condition = interpretNode(ifStatement.getCondition(), template, context, out);
		if (!(condition instanceof Boolean)) Error.error("Expected a condition evaluating to a boolean, got " + condition, ifStatement.getCondition().getSpan());
		if ((Boolean)condition) {
			interpretNodeList(ifStatement.getTrueBlock(), template, context, out);
			return null;
		}

		if (ifStatement.getElseIfs().size() > 0) {
			for (IfStatement elseIf : ifStatement.getElseIfs()) {
				condition = interpretNode(elseIf.getCondition(), template, context, out);
				if (!(condition instanceof Boolean)) Error.error("Expected a condition evaluating to a boolean, got " + condition, elseIf.getCondition().getSpan());
				if ((Boolean)condition) {
					interpretNodeList(elseIf.getTrueBlock(), template, context, out);
					return null;
				}
			}
		}

		interpretNodeList(ifStatement.getFalseBlock(), template, context, out);
		return null;
	}

	private static Object interpretWhile (WhileStatement whileStatement, Template template, TemplateContext context, OutputStream out) throws IOException {
		while (true) {
			Object condition = interpretNode(whileStatement.getCondition(), template, context, out);
			if (!(condition instanceof Boolean))
				Error.error("Expected a condition evaluating to a boolean, got " + condition, whileStatement.getCondition().getSpan());
			if (!((Boolean)condition)) break;
			interpretNodeList(whileStatement.getBody(), template, context, out);
		}
		return null;
	}

	private static Object interpretInclude (Include include, Template template, TemplateContext context, OutputStream out) throws IOException {
		Template other = include.getTemplate();

		if (!include.isMacrosOnly()) {
			if (include.getContext().isEmpty()) {
				interpretNodeList(other.getNodes(), other, context, out);
			} else {
				TemplateContext otherContext = new TemplateContext();
				for (Span span : include.getContext().keySet()) {
					String key = span.getText();
					Object value = interpretNode(include.getContext().get(span), template, context, out);
					otherContext.set(key, value);
				}
				interpretNodeList(other.getNodes(), other, otherContext, out);
			}
		} else {
			context.set(include.getAlias().getText(), include.getTemplate().getMacros());
		}
		return null;
	}

	private static Object interpretNode (Node node, Template template, TemplateContext context, OutputStream out) throws IOException {
		if (node.getClass() == Text.class) {
			out.write(((Text)node).getBytes());
			return null;

		} else if (node.getClass() == BooleanLiteral.class) {
			return ((BooleanLiteral)node).getValue();

		} else if (node.getClass() == DoubleLiteral.class) {
			return ((DoubleLiteral)node).getValue();

		} else if (node.getClass() == FloatLiteral.class) {
			return ((FloatLiteral)node).getValue();

		} else if (node.getClass() == ByteLiteral.class) {
			return ((ByteLiteral)node).getValue();

		} else if (node.getClass() == ShortLiteral.class) {
			return ((ShortLiteral)node).getValue();

		} else if (node.getClass() == IntegerLiteral.class) {
			return ((IntegerLiteral)node).getValue();

		} else if (node.getClass() == LongLiteral.class) {
			return ((LongLiteral)node).getValue();

		} else if (node.getClass() == CharacterLiteral.class) {
			return ((CharacterLiteral)node).getValue();

		} else if (node.getClass() == StringLiteral.class) {
			return ((StringLiteral)node).getValue();

		} else if (node.getClass() == NullLiteral.class) {
			return null;

		} else if (node.getClass() == VariableAccess.class) {
			return interpretVariableAccess((VariableAccess)node, template, context, out);

		} else if (node.getClass() == MapOrArrayAccess.class) {
			return interpretMapOrArrayAccess((MapOrArrayAccess)node, template, context, out);

		} else if (node.getClass() == MemberAccess.class) {
			return interpretMemberAccess((MemberAccess)node, template, context, out);

		} else if (node.getClass() == MethodCall.class) {
			return interpretMethodCall((MethodCall)node, template, context, out);

		} else if (node.getClass() == FunctionCall.class) {
			return interpretFunctionCall((FunctionCall)node, template, context, out);

		} else if (node.getClass() == UnaryOperation.class) {
			return interpretUnaryOperation((UnaryOperation)node, template, context, out);

		} else if (node.getClass() == BinaryOperation.class) {
			return interpretBinaryOperation((BinaryOperation)node, template, context, out);

		} else if (node.getClass() == TernaryOperation.class) {
			return interpretTernaryOperation((TernaryOperation)node, template, context, out);

		} else if (node.getClass() == ForStatement.class) {
			return interpretFor((ForStatement)node, template, context, out);

		} else if (node.getClass() == IfStatement.class) {
			return interpretIf((IfStatement)node, template, context, out);

		} else if (node.getClass() == WhileStatement.class) {
			return interpretWhile((WhileStatement)node, template, context, out);

		} else if (node.getClass() == Include.class) {
			return interpretInclude((Include)node, template, context, out);

		} else if (node.getClass() == Macro.class) {
			// Do nothing for macros
			return null;
		} else {
			Error.error("Interpretation of node " + node.getClass().getSimpleName() + " not implemented.", node.getSpan());
			return null; // never reached
		}
	}
}
