
package io.marioslab.basis.template.parsing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.marioslab.basis.template.Error;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.Source;
import io.marioslab.basis.template.interpreter.AstInterpreter;
import io.marioslab.basis.template.interpreter.Reflection;
import io.marioslab.basis.template.parsing.Parser.Macros;

/** Templates are parsed into an abstract syntax tree (AST) nodes by a Parser. This class contains all AST node types. */
public abstract class Ast {

	/** Base class for all AST nodes. A node minimally stores the {@link Span} that references its location in the
	 * {@link Source}. **/
	public abstract static class Node {
		private final Span span;

		public Node (Span span) {
			this.span = span;
		}

		/** Returns the {@link Span} referencing this node's location in the {@link Source}. **/
		public Span getSpan () {
			return span;
		}

		@Override
		public String toString () {
			return span.getText();
		}

		public abstract Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException;
	}

	/** A text node represents an "un-templated" span in the source that should be emitted verbatim. **/
	public static class Text extends Node {
		private final byte[] bytes;

		public Text (Span text) {
			super(text);
			try {
				bytes = text.getText().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Couldn't convert text to UTF-8 string.", e);
			}
		}

		/** Returns the UTF-8 representatino of this text node. **/
		public byte[] getBytes () {
			return bytes;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			out.write(getBytes());
			return null;
		}
	}

	/** All expressions are subclasses of this node type. Expressions are separated into unary operations (!, -), binary operations
	 * (+, -, *, /, etc.) and ternary operations (?:). */
	public abstract static class Expression extends Node {
		public Expression (Span span) {
			super(span);
		}
	}

	/** An unary operation node represents a logical or numerical negation. **/
	public static class UnaryOperation extends Expression {

		public static enum UnaryOperator {
			Not, Negate, Positive;

			public static UnaryOperator getOperator (Token op) {
				if (op.getType() == TokenType.Not) return UnaryOperator.Not;
				if (op.getType() == TokenType.Plus) return UnaryOperator.Positive;
				if (op.getType() == TokenType.Minus) return UnaryOperator.Negate;
				Error.error("Unknown unary operator " + op + ".", op.getSpan());
				return null; // not reached
			}
		}

		private final UnaryOperator operator;
		private final Expression operand;

		public UnaryOperation (Token operator, Expression operand) {
			super(operator.getSpan());
			this.operator = UnaryOperator.getOperator(operator);
			this.operand = operand;
		}

		public UnaryOperator getOperator () {
			return operator;
		}

		public Expression getOperand () {
			return operand;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object operand = getOperand().evaluate(template, context, out);

			if (getOperator() == UnaryOperator.Negate) {
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
					Error.error("Operand of operator '" + getOperator().name() + "' must be a number, got " + operand, getSpan());
					return null; // never reached
				}
			} else if (getOperator() == UnaryOperator.Not) {
				if (!(operand instanceof Boolean)) Error.error("Operand of operator '" + getOperator().name() + "' must be a boolean", getSpan());
				return !(Boolean)operand;
			} else {
				return operand;
			}
		}
	}

	/** A binary operation represents arithmetic operators, like addition or division, comparison operators, like less than or
	 * equals, logical operators, like and, or an assignment. **/
	public static class BinaryOperation extends Expression {

		public static enum BinaryOperator {
			Addition, Subtraction, Multiplication, Division, Modulo, Equal, NotEqual, Less, LessEqual, Greater, GreaterEqual, And, Or, Xor, Assignment;

			public static BinaryOperator getOperator (Token op) {
				if (op.getType() == TokenType.Plus) return BinaryOperator.Addition;
				if (op.getType() == TokenType.Minus) return BinaryOperator.Subtraction;
				if (op.getType() == TokenType.Asterisk) return BinaryOperator.Multiplication;
				if (op.getType() == TokenType.ForwardSlash) return BinaryOperator.Division;
				if (op.getType() == TokenType.Percentage) return BinaryOperator.Modulo;
				if (op.getType() == TokenType.Equal) return BinaryOperator.Equal;
				if (op.getType() == TokenType.NotEqual) return BinaryOperator.NotEqual;
				if (op.getType() == TokenType.Less) return BinaryOperator.Less;
				if (op.getType() == TokenType.LessEqual) return BinaryOperator.LessEqual;
				if (op.getType() == TokenType.Greater) return BinaryOperator.Greater;
				if (op.getType() == TokenType.GreaterEqual) return BinaryOperator.GreaterEqual;
				if (op.getType() == TokenType.And) return BinaryOperator.And;
				if (op.getType() == TokenType.Or) return BinaryOperator.Or;
				if (op.getType() == TokenType.Xor) return BinaryOperator.Xor;
				if (op.getType() == TokenType.Assignment) return BinaryOperator.Assignment;
				Error.error("Unknown binary operator " + op + ".", op.getSpan());
				return null; // not reached
			}
		}

		private final Expression leftOperand;
		private final BinaryOperator operator;
		private final Expression rightOperand;

		public BinaryOperation (Expression leftOperand, Token operator, Expression rightOperand) {
			super(operator.getSpan());
			this.leftOperand = leftOperand;
			this.operator = BinaryOperator.getOperator(operator);
			this.rightOperand = rightOperand;
		}

		public Expression getLeftOperand () {
			return leftOperand;
		}

		public BinaryOperator getOperator () {
			return operator;
		}

		public Expression getRightOperand () {
			return rightOperand;
		}

		private Object evaluateAddition (Object left, Object right) {
			if (left instanceof String || right instanceof String) return left.toString() + right.toString();
			if (left instanceof Double || right instanceof Double) return ((Number)left).doubleValue() + ((Number)right).doubleValue();
			if (left instanceof Float || right instanceof Float) return ((Number)left).floatValue() + ((Number)right).floatValue();
			if (left instanceof Long || right instanceof Long) return ((Number)left).longValue() + ((Number)right).longValue();
			if (left instanceof Integer || right instanceof Integer) return ((Number)left).intValue() + ((Number)right).intValue();
			if (left instanceof Short || right instanceof Short) return ((Number)left).shortValue() + ((Number)right).shortValue();
			if (left instanceof Byte || right instanceof Byte) return ((Number)left).byteValue() + ((Number)right).byteValue();

			Error.error("Operands for addition operator must be numbers or strings, got " + left + ", " + right + ".", getSpan());
			return null; // never reached
		}

		private Object evaluateSubtraction (Object left, Object right) {
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
				Error.error("Operands for subtraction operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateMultiplication (Object left, Object right) {
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
				Error.error("Operands for multiplication operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateDivision (Object left, Object right) {
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
				Error.error("Operands for division operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateModulo (Object left, Object right) {
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
				Error.error("Operands for modulo operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private boolean evaluateLess (Object left, Object right) {
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
				Error.error("Operands for less operator must be numbers" + left + ", " + right + ".", getSpan());
				return false; // never reached
			}
		}

		private Object evaluateLessEqual (Object left, Object right) {
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
				Error.error("Operands for less/equal operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateGreater (Object left, Object right) {
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
				Error.error("Operands for greater operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateGreaterEqual (Object left, Object right) {
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
				Error.error("Operands for greater/equal operator must be numbers" + left + ", " + right + ".", getSpan());
				return null; // never reached
			}
		}

		private Object evaluateAnd (Object left, Template template, TemplateContext context, OutputStream out) throws IOException {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", getLeftOperand().getSpan());
			if (!(Boolean)left) return false;
			Object right = getRightOperand().evaluate(template, context, out);
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", getRightOperand().getSpan());
			return (Boolean)left && (Boolean)right;
		}

		private Object evaluateOr (Object left, Template template, TemplateContext context, OutputStream out) throws IOException {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", getLeftOperand().getSpan());
			if ((Boolean)left) return true;
			Object right = getRightOperand().evaluate(template, context, out);
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", getRightOperand().getSpan());
			return (Boolean)left || (Boolean)right;
		}

		private Object evaluateXor (Object left, Object right) {
			if (!(left instanceof Boolean)) Error.error("Left operand must be a boolean, got " + left + ".", getLeftOperand().getSpan());
			if (!(right instanceof Boolean)) Error.error("Right operand must be a boolean, got " + right + ".", getRightOperand().getSpan());
			return (Boolean)left ^ (Boolean)right;
		}

		private Object evaluateEqual (Object left, Object right) {
			return left.equals(right);
		}

		private Object evaluateNotEqual (Object left, Object right) {
			return !left.equals(right);
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			if (getOperator() == BinaryOperator.Assignment) {
				if (!(getLeftOperand() instanceof VariableAccess)) Error.error("Can only assign to top-level variables in context.", getLeftOperand().getSpan());
				Object value = getRightOperand().evaluate(template, context, out);
				context.set(((VariableAccess)getLeftOperand()).getVariableName().getText(), value);
				return null;
			}

			Object left = getLeftOperand().evaluate(template, context, out);
			Object right = getOperator() == BinaryOperator.And || getOperator() == BinaryOperator.Or ? null : getRightOperand().evaluate(template, context, out);

			switch (getOperator()) {
			case Addition:
				return evaluateAddition(left, right);
			case Subtraction:
				return evaluateSubtraction(left, right);
			case Multiplication:
				return evaluateMultiplication(left, right);
			case Division:
				return evaluateDivision(left, right);
			case Modulo:
				return evaluateModulo(left, right);
			case Less:
				return evaluateLess(left, right);
			case LessEqual:
				return evaluateLessEqual(left, right);
			case Greater:
				return evaluateGreater(left, right);
			case GreaterEqual:
				return evaluateGreaterEqual(left, right);
			case Equal:
				return evaluateEqual(left, right);
			case NotEqual:
				return evaluateNotEqual(left, right);
			case And:
				return evaluateAnd(left, template, context, out);
			case Or:
				return evaluateOr(left, template, context, out);
			case Xor:
				return evaluateXor(left, right);
			default:
				Error.error("Binary operator " + getOperator().name() + " not implemented", getSpan());
				return null;
			}
		}
	}

	/** A ternary operation is an abbreviated if/then/else operation, and equivalent to the the ternary operator in Java. **/
	public static class TernaryOperation extends Expression {
		private final Expression condition;
		private final Expression trueExpression;
		private final Expression falseExpression;

		public TernaryOperation (Expression condition, Expression trueExpression, Expression falseExpression) {
			super(new Span(condition.getSpan(), falseExpression.getSpan()));
			this.condition = condition;
			this.trueExpression = trueExpression;
			this.falseExpression = falseExpression;
		}

		public Expression getCondition () {
			return condition;
		}

		public Expression getTrueExpression () {
			return trueExpression;
		}

		public Expression getFalseExpression () {
			return falseExpression;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object condition = getCondition().evaluate(template, context, out);
			if (!(condition instanceof Boolean)) Error.error("Condition of ternary operator must be a boolean, got " + condition + ".", getSpan());
			return ((Boolean)condition) ? getTrueExpression().evaluate(template, context, out) : getFalseExpression().evaluate(template, context, out);
		}
	}

	/** A null literal, with the single value <code>null</code> **/
	public static class NullLiteral extends Expression {
		public NullLiteral (Span span) {
			super(span);
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return null;
		}
	}

	/** A boolean literal, with the values <code>true</code> and <code>false</code> **/
	public static class BooleanLiteral extends Expression {
		private final Boolean value;

		public BooleanLiteral (Span literal) {
			super(literal);
			this.value = Boolean.parseBoolean(literal.getText());
		}

		public Boolean getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A double precision floating point literal. Must be marked with the <code>d</code> suffix, e.g. "1.0d". **/
	public static class DoubleLiteral extends Expression {
		private final Double value;

		public DoubleLiteral (Span literal) {
			super(literal);
			this.value = Double.parseDouble(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public Double getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A single precision floating point literla. May be optionally marked with the <code>f</code> suffix, e.g. "1.0f". **/
	public static class FloatLiteral extends Expression {
		private final Float value;

		public FloatLiteral (Span literal) {
			super(literal);
			String text = literal.getText();
			if (text.charAt(text.length() - 1) == 'f') text = text.substring(0, text.length() - 1);
			this.value = Float.parseFloat(text);
		}

		public Float getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A byte literal. Must be marked with the <code>b</code> suffix, e.g. "123b". **/
	public static class ByteLiteral extends Expression {
		private final Byte value;

		public ByteLiteral (Span literal) {
			super(literal);
			this.value = Byte.parseByte(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public Byte getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A short literal. Must be marked with the <code>s</code> suffix, e.g. "123s". **/
	public static class ShortLiteral extends Expression {
		private final Short value;

		public ShortLiteral (Span literal) {
			super(literal);
			this.value = Short.parseShort(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public Short getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** An integer literal. **/
	public static class IntegerLiteral extends Expression {
		private final Integer value;

		public IntegerLiteral (Span literal) {
			super(literal);
			this.value = Integer.parseInt(literal.getText());
		}

		public Integer getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A long integer literal. Must be marked with the <code>l</code> suffix, e.g. "123l". **/
	public static class LongLiteral extends Expression {
		private final Long value;

		public LongLiteral (Span literal) {
			super(literal);
			this.value = Long.parseLong(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public Long getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A character literal, enclosed in single quotes. Supports escape sequences \n, \r,\t, \' and \\. **/
	public static class CharacterLiteral extends Expression {
		private final Character value;

		public CharacterLiteral (Span literal) {
			super(literal);

			String text = literal.getText();
			if (text.length() > 3) {
				if (text.charAt(2) == 'n')
					value = '\n';
				else if (text.charAt(2) == 'r')
					value = '\r';
				else if (text.charAt(2) == 't')
					value = '\t';
				else if (text.charAt(2) == '\\')
					value = '\\';
				else if (text.charAt(2) == '\'')
					value = '\'';
				else {
					Error.error("Unknown escape sequence '" + literal.getText() + "'.", literal);
					value = 0; // never reached
				}
			} else {
				this.value = literal.getText().charAt(1);
			}
		}

		public Character getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** A string literal, enclosed in double quotes. Does not support escape sequences. **/
	public static class StringLiteral extends Expression {
		private final String value;

		public StringLiteral (Span literal) {
			super(literal);
			String text = getSpan().getText();
			value = text.substring(1, text.length() - 1);
		}

		/** Returns the literal without quotes **/
		public String getValue () {
			return value;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return value;
		}
	}

	/** Represents a top-level variable access by name. E.g. in the expression "a + 1", <code>a</code> would be encoded as a
	 * VariableAccess node. Variables can be both read (in expressions) and written to (in assignments). Variable values are looked
	 * up and written to a {@link TemplateContext}. **/
	public static class VariableAccess extends Expression {
		public VariableAccess (Span name) {
			super(name);
		}

		public Span getVariableName () {
			return getSpan();
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object value = context.get(getSpan().getText());
			if (value == null) Error.error("Couldn't find variable '" + getSpan().getText() + "' in context.", getSpan());
			return value;
		}
	}

	/** Represents a map or array element access of the form <code>mapOrArray[keyOrIndex]</code>. Maps and arrays may only be read
	 * from. **/
	public static class MapOrArrayAccess extends Expression {
		private final Expression mapOrArray;
		private final Expression keyOrIndex;

		public MapOrArrayAccess (Span span, Expression mapOrArray, Expression keyOrIndex) {
			super(span);
			this.mapOrArray = mapOrArray;
			this.keyOrIndex = keyOrIndex;
		}

		/** Returns an expression that must evaluate to a map or array. **/
		public Expression getMapOrArray () {
			return mapOrArray;
		}

		/** Returns an expression that is used as the key or index to fetch a map or array element. **/
		public Expression getKeyOrIndex () {
			return keyOrIndex;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object mapOrArray = getMapOrArray().evaluate(template, context, out);
			if (mapOrArray == null) Error.error("Couldn't find map or array in context.", getSpan());
			Object keyOrIndex = getKeyOrIndex().evaluate(template, context, out);
			if (keyOrIndex == null) Error.error("Couldn't evaluate key or index.", getKeyOrIndex().getSpan());

			if (mapOrArray instanceof Map) {
				return ((Map)mapOrArray).get(keyOrIndex);
			} else if (mapOrArray instanceof List) {
				if (!(keyOrIndex instanceof Number)) {
					Error.error("List index must be an integer, but was " + keyOrIndex.getClass().getSimpleName(), getKeyOrIndex().getSpan());
				}
				int index = ((Number)keyOrIndex).intValue();
				return ((List)mapOrArray).get(index);
			} else {
				if (!(keyOrIndex instanceof Number)) {
					Error.error("Array index must be an integer, but was " + keyOrIndex.getClass().getSimpleName(), getKeyOrIndex().getSpan());
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
	}

	/** Represents an access of a member (field or method) of the form <code>object.member</code>. Members may only be read
	 * from. **/
	public static class MemberAccess extends Expression {
		private final Expression object;
		private final Span name;
		private Object cachedMember;

		public MemberAccess (Expression object, Span name) {
			super(name);
			this.object = object;
			this.name = name;
		}

		/** Returns the object on which to access the member. **/
		public Expression getObject () {
			return object;
		}

		/** The name of the member. **/
		public Span getName () {
			return name;
		}

		/** Returns the cached member descriptor as returned by {@link Reflection#getField(Object, String)} or
		 * {@link Reflection#getMethod(Object, String, Object...)}. See {@link #setCachedMember(Object)}. **/
		public Object getCachedMember () {
			return cachedMember;
		}

		/** Sets the member descriptor as returned by {@link Reflection#getField(Object, String)} or
		 * {@link Reflection#getMethod(Object, String, Object...)} for faster member lookups. Called by {@link AstInterpreter} the
		 * first time this node is evaluated. Subsequent evaluations can use the cached descriptor, avoiding a costly reflective
		 * lookup. **/
		public void setCachedMember (Object cachedMember) {
			this.cachedMember = cachedMember;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object object = getObject().evaluate(template, context, out);
			if (object == null) Error.error("Couldn't find object in context.", getSpan());
			Object field = getCachedMember();
			if (field != null) {
				try {
					return Reflection.getInstance().getFieldValue(object, field);
				} catch (Throwable t) {
					// fall through
				}
			}

			field = Reflection.getInstance().getField(object, getName().getText());
			if (field == null)
				Error.error("Couldn't find field '" + getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.", getSpan());
			setCachedMember(field);
			return Reflection.getInstance().getFieldValue(object, field);
		}
	}

	/** Represents a call to a top-level function. A function may either be a {@link FunctionalInterface} stored in a
	 * {@link TemplateContext}, or a {@link Macro} defined in a template. */
	public static class FunctionCall extends Expression {
		private final Expression function;
		private final List<Expression> arguments;
		private Object cachedFunction;
		private final ThreadLocal<Object[]> cachedArguments;

		public FunctionCall (Span span, Expression function, List<Expression> arguments) {
			super(span);
			this.function = function;
			this.arguments = arguments;
			this.cachedArguments = new ThreadLocal<Object[]>();
		}

		/** Return the expression that must evaluate to a {@link FunctionalInterface} or a {@link Macro}. **/
		public Expression getFunction () {
			return function;
		}

		/** Returns the list of expressions to be passed to the function as arguments. **/
		public List<Expression> getArguments () {
			return arguments;
		}

		/** Returns the cached "function" descriptor as returned by {@link Reflection#getMethod(Object, String, Object...)} or the
		 * {@link Macro}. See {@link #setCachedFunction(Object)}. **/
		public Object getCachedFunction () {
			return cachedFunction;
		}

		/** Sets the "function" descriptor as returned by {@link Reflection#getMethod(Object, String, Object...)} for faster
		 * lookups, or the {@link Macro} to be called. Called by {@link AstInterpreter} the first time this node is evaluated.
		 * Subsequent evaluations can use the cached descriptor, avoiding a costly reflective lookup. **/
		public void setCachedFunction (Object cachedFunction) {
			this.cachedFunction = cachedFunction;
		}

		/** Returns a scratch buffer to store arguments in when calling the function in {@link AstInterpreter}. Avoids generating
		 * garbage. **/
		public Object[] getCachedArguments () {
			Object[] args = cachedArguments.get();
			if (args == null) {
				args = new Object[arguments.size()];
				cachedArguments.set(args);
			}
			return args;
		}

		/** Must be invoked when this node is done evaluating so we don't leak memory **/
		public void clearCachedArguments () {
			Object[] args = getCachedArguments();
			for (int i = 0; i < args.length; i++)
				args[i] = null;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			try {
				Object[] argumentValues = getCachedArguments();
				List<Expression> arguments = getArguments();
				for (int i = 0, n = argumentValues.length; i < n; i++) {
					Expression expr = arguments.get(i);
					argumentValues[i] = expr.evaluate(template, context, out);
				}

				// This is a special case to handle template level macros. If a call to a macro is
				// made, evaluating the function expression will result in an exception, as the
				// function name can't be found in the context. Instead we need to manually check
				// if the function expression is a VariableAccess and if so, if it can be found
				// in the context.
				Object function = null;
				if (getFunction() instanceof VariableAccess) {
					VariableAccess varAccess = (VariableAccess)getFunction();
					function = context.get(varAccess.getVariableName().getText());
				} else {
					function = getFunction().evaluate(template, context, out);
				}

				if (function != null) {
					Object method = getCachedFunction();
					if (method != null) {
						try {
							return Reflection.getInstance().callMethod(function, method, argumentValues);
						} catch (Throwable t) {
							// fall through
						}
					}
					method = Reflection.getInstance().getMethod(function, null, argumentValues);
					if (method == null) Error.error("Couldn't find function.", getSpan());
					setCachedFunction(method);
					return Reflection.getInstance().callMethod(function, method, argumentValues);
				} else {
					// Check if this is a call to a macro defined in this template
					if (getFunction() instanceof VariableAccess) {
						String functionName = ((VariableAccess)getFunction()).getVariableName().getText();
						Macros macros = template.getMacros();
						Macro macro = macros.get(functionName);
						if (macro != null) {
							if (macro.getArgumentNames().size() != arguments.size())
								Error.error("Expected " + macro.getArgumentNames().size() + " arguments, got " + arguments.size(), getSpan());
							TemplateContext macroContext = macro.getMacroContext();
							for (int i = 0; i < arguments.size(); i++) {
								Object arg = argumentValues[i];
								String name = macro.getArgumentNames().get(i).getText();
								macroContext.set(name, arg);
							}
							AstInterpreter.interpretNodeList(macro.getBody(), macro.getTemplate(), macroContext, out);
							return null;
						}
					}
					Error.error("Couldn't find function.", getSpan());
					return null; // never reached
				}
			} finally {
				clearCachedArguments();
			}
		}
	}

	/** Represents a call to a method of the form <code>object.method(a, b, c)</code>. **/
	public static class MethodCall extends Expression {
		private final MemberAccess method;
		private final List<Expression> arguments;
		private Object cachedMethod;
		private final ThreadLocal<Object[]> cachedArguments;

		public MethodCall (Span span, MemberAccess method, List<Expression> arguments) {
			super(span);
			this.method = method;
			this.arguments = arguments;
			this.cachedArguments = new ThreadLocal<Object[]>();
		}

		/** Returns the object on which to call the method. **/
		public Expression getObject () {
			return method.getObject();
		}

		/** Returns the method to call. **/
		public MemberAccess getMethod () {
			return method;
		}

		/** Returns the list of expressions to be passed to the function as arguments. **/
		public List<Expression> getArguments () {
			return arguments;
		}

		/** Returns the cached member descriptor as returned by {@link Reflection#getMethod(Object, String, Object...)}. See
		 * {@link #setCachedMember(Object)}. **/
		public Object getCachedMethod () {
			return cachedMethod;
		}

		/** Sets the method descriptor as returned by {@link Reflection#getMethod(Object, String, Object...)} for faster lookups.
		 * Called by {@link AstInterpreter} the first time this node is evaluated. Subsequent evaluations can use the cached
		 * descriptor, avoiding a costly reflective lookup. **/
		public void setCachedMethod (Object cachedMethod) {
			this.cachedMethod = cachedMethod;
		}

		/** Returns a scratch buffer to store arguments in when calling the function in {@link AstInterpreter}. Avoids generating
		 * garbage. **/
		public Object[] getCachedArguments () {
			Object[] args = cachedArguments.get();
			if (args == null) {
				args = new Object[arguments.size()];
				cachedArguments.set(args);
			}
			return args;
		}

		/** Must be invoked when this node is done evaluating so we don't leak memory **/
		public void clearCachedArguments () {
			Object[] args = getCachedArguments();
			for (int i = 0; i < args.length; i++)
				args[i] = null;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			try {
				Object object = getObject().evaluate(template, context, out);
				if (object == null) Error.error("Couldn't find object in context.", getSpan());

				Object[] argumentValues = getCachedArguments();
				List<Expression> arguments = getArguments();
				for (int i = 0, n = argumentValues.length; i < n; i++) {
					Expression expr = arguments.get(i);
					argumentValues[i] = expr.evaluate(template, context, out);
				}

				// if the object we call the method on is a Macros instance, lookup the macro by name
				// and execute its node list
				if (object instanceof Macros) {
					Macros macros = (Macros)object;
					Macro macro = macros.get(getMethod().getName().getText());
					if (macro != null) {
						if (macro.getArgumentNames().size() != arguments.size())
							Error.error("Expected " + macro.getArgumentNames().size() + " arguments, got " + arguments.size(), getSpan());
						TemplateContext macroContext = macro.getMacroContext();
						for (int i = 0; i < arguments.size(); i++) {
							Object arg = argumentValues[i];
							String name = macro.getArgumentNames().get(i).getText();
							macroContext.set(name, arg);
						}
						AstInterpreter.interpretNodeList(macro.getBody(), macro.getTemplate(), macroContext, out);
						return null;
					}
				}

				// Otherwise try to find a corresponding method or field pointing to a lambda.
				Object method = getCachedMethod();
				if (method != null) {
					try {
						return Reflection.getInstance().callMethod(object, method, argumentValues);
					} catch (Throwable t) {
						// fall through
					}
				}

				method = Reflection.getInstance().getMethod(object, getMethod().getName().getText(), argumentValues);
				if (method != null) {
					// found the method on the object, call it
					setCachedMethod(method);
					return Reflection.getInstance().callMethod(object, method, argumentValues);
				} else {
					// didn't find the method on the object, try to find a field pointing to a lambda
					Object field = Reflection.getInstance().getField(object, getMethod().getName().getText());
					if (field == null)
						Error.error("Couldn't find method '" + getMethod().getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
							getSpan());
					Object function = Reflection.getInstance().getFieldValue(object, field);
					method = Reflection.getInstance().getMethod(function, null, argumentValues);
					if (method == null) Error.error(
						"Couldn't find function in field '" + getMethod().getName().getText() + "' for object of type '" + object.getClass().getSimpleName() + "'.",
						getSpan());
					return Reflection.getInstance().callMethod(function, method, argumentValues);
				}
			} finally {
				clearCachedArguments();
			}
		}
	}

	/** Represents an if statement of the form <code>if condition trueBlock elseif condition ... else falseBlock end</code>. Elseif
	 * and else blocks are optional. */
	public static class IfStatement extends Node {
		private final Expression condition;
		private final List<Node> trueBlock;
		private final List<IfStatement> elseIfs;
		private final List<Node> falseBlock;

		public IfStatement (Span span, Expression condition, List<Node> trueBlock, List<IfStatement> elseIfs, List<Node> falseBlock) {
			super(span);
			this.condition = condition;
			this.trueBlock = trueBlock;
			this.elseIfs = elseIfs;
			this.falseBlock = falseBlock;
		}

		public Expression getCondition () {
			return condition;
		}

		public List<Node> getTrueBlock () {
			return trueBlock;
		}

		public List<IfStatement> getElseIfs () {
			return elseIfs;
		}

		public List<Node> getFalseBlock () {
			return falseBlock;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object condition = getCondition().evaluate(template, context, out);
			if (!(condition instanceof Boolean)) Error.error("Expected a condition evaluating to a boolean, got " + condition, getCondition().getSpan());
			if ((Boolean)condition) {
				context.push();
				AstInterpreter.interpretNodeList(getTrueBlock(), template, context, out);
				context.pop();
				return null;
			}

			if (getElseIfs().size() > 0) {
				for (IfStatement elseIf : getElseIfs()) {
					condition = elseIf.getCondition().evaluate(template, context, out);
					if (!(condition instanceof Boolean))
						Error.error("Expected a condition evaluating to a boolean, got " + condition, elseIf.getCondition().getSpan());
					if ((Boolean)condition) {
						context.push();
						AstInterpreter.interpretNodeList(elseIf.getTrueBlock(), template, context, out);
						context.pop();
						return null;
					}
				}
			}

			context.push();
			AstInterpreter.interpretNodeList(getFalseBlock(), template, context, out);
			context.pop();
			return null;
		}
	}

	/** Represents a for statement of the form <code>for value in mapOrArray ... end</code> or
	 * <code>for keyOrIndex, value in mapOrArray ... end</code>. The later form will store the key or index of the current
	 * iteration in the specified variable. */
	public static class ForStatement extends Node {
		private final Span indexOrKeyName;
		private final Span valueName;
		private final Expression mapOrArray;
		private final List<Node> body;

		public ForStatement (Span span, Span indexOrKeyName, Span valueName, Expression mapOrArray, List<Node> body) {
			super(span);
			this.indexOrKeyName = indexOrKeyName;
			this.valueName = valueName;
			this.mapOrArray = mapOrArray;
			this.body = body;
		}

		/** Returns null if no index or key name was given **/
		public Span getIndexOrKeyName () {
			return indexOrKeyName;
		}

		public Span getValueName () {
			return valueName;
		}

		public Expression getMapOrArray () {
			return mapOrArray;
		}

		public List<Node> getBody () {
			return body;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Object mapOrArray = getMapOrArray().evaluate(template, context, out);
			if (mapOrArray == null) Error.error("Expected a map or array, got null.", getMapOrArray().getSpan());
			String valueName = getValueName().getText();

			if (mapOrArray instanceof Map) {
				Map map = (Map)mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (Object entry : map.entrySet()) {
						Entry e = (Entry)entry;
						context.set(keyName, e.getKey());
						context.set(valueName, e.getValue());
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (Object value : map.values()) {
						context.set(valueName, value);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof Iterable) {
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					Iterator iter = ((Iterable)mapOrArray).iterator();
					int i = 0;
					while (iter.hasNext()) {
						context.set(keyName, i++);
						context.set(valueName, iter.next());
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					Iterator iter = ((Iterable)mapOrArray).iterator();
					context.push();
					while (iter.hasNext()) {
						context.set(valueName, iter.next());
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof Iterator) {
				if (getIndexOrKeyName() != null) {
					Error.error("Can not do indexed/keyed for loop on an iterator.", getMapOrArray().getSpan());
				} else {
					Iterator iter = (Iterator)mapOrArray;
					context.push();
					while (iter.hasNext()) {
						context.set(valueName, iter.next());
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof int[]) {
				int[] array = (int[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof float[]) {
				float[] array = (float[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof double[]) {
				double[] array = (double[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof boolean[]) {
				boolean[] array = (boolean[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof char[]) {
				char[] array = (char[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof short[]) {
				short[] array = (short[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof byte[]) {
				byte[] array = (byte[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof long[]) {
				long[] array = (long[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else if (mapOrArray instanceof Object[]) {
				Object[] array = (Object[])mapOrArray;
				if (getIndexOrKeyName() != null) {
					context.push();
					String keyName = getIndexOrKeyName().getText();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(keyName, i);
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				} else {
					context.push();
					for (int i = 0, n = array.length; i < n; i++) {
						context.set(valueName, array[i]);
						AstInterpreter.interpretNodeList(getBody(), template, context, out);
					}
					context.pop();
				}
			} else {
				Error.error("Expected a map, an array or an iterable, got " + mapOrArray, getMapOrArray().getSpan());
			}
			return null;
		}
	}

	/** Represents a while statement of the form <code>while condition ... end</code>. **/
	public static class WhileStatement extends Node {
		private final Expression condition;
		private final List<Node> body;

		public WhileStatement (Span span, Expression condition, List<Node> body) {
			super(span);
			this.condition = condition;
			this.body = body;
		}

		public Expression getCondition () {
			return condition;
		}

		public List<Node> getBody () {
			return body;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			context.push();
			while (true) {
				Object condition = getCondition().evaluate(template, context, out);
				if (!(condition instanceof Boolean)) Error.error("Expected a condition evaluating to a boolean, got " + condition, getCondition().getSpan());
				if (!((Boolean)condition)) break;
				AstInterpreter.interpretNodeList(getBody(), template, context, out);
			}
			context.pop();
			return null;
		}
	}

	/** Represents a macro of the form macro(arg1, arg2, arg3) ... end. Macros allow specifying re-usable template blocks that can
	 * be "called" from other sections in the current template, or templates including the template. */
	public static class Macro extends Node {
		private final Span name;
		private final List<Span> argumentNames;
		private final List<Node> body;
		private final TemplateContext macroContext = new TemplateContext();
		private Template template;

		public Macro (Span span, Span name, List<Span> argumentNames, List<Node> body) {
			super(span);
			this.name = name;
			this.argumentNames = argumentNames;
			this.body = body;
		}

		public Span getName () {
			return name;
		}

		public List<Span> getArgumentNames () {
			return argumentNames;
		}

		public List<Node> getBody () {
			return body;
		}

		public TemplateContext getMacroContext () {
			return macroContext;
		}

		public void setTemplate (Template template) {
			this.template = template;
		}

		/** The template of the macro is set after the entire template has been parsed. See the Template constructor. **/
		public Template getTemplate () {
			return template;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			return null;
		}
	}

	/** Represents an include statement of the form <code>include "path"</code>, which includes the template verbatim, or
	 * <code>include "path" as alias</code>, which includes only the macros and makes them accessible under the alias, e.g.
	 * <code>alias.myMacro(a, b, c)</code>, or <code>include "path" with (key: value, key2: value)</code>, which includes the
	 * template, passing the given map as the context. **/
	public static class Include extends Node {
		private final Span path;
		private final Map<Span, Expression> context;
		private Template template;
		private final boolean macrosOnly;
		private final Span alias;

		public Include (Span span, Span path, Map<Span, Expression> context, boolean macrosOnly, Span alias) {
			super(span);
			this.path = path;
			this.context = context;
			this.macrosOnly = macrosOnly;
			this.alias = alias;
		}

		public Span getPath () {
			return path;
		}

		/** Returns null if macrosOnly is true **/
		public Map<Span, Expression> getContext () {
			return context;
		}

		public Template getTemplate () {
			return template;
		}

		public void setTemplate (Template template) {
			this.template = template;
		}

		/** Returns whether to include macros only. **/
		public boolean isMacrosOnly () {
			return macrosOnly;
		}

		/** Returns null if macrosOnly is false **/
		public Span getAlias () {
			return alias;
		}

		@Override
		public Object evaluate (Template template, TemplateContext context, OutputStream out) throws IOException {
			Template other = getTemplate();

			if (!isMacrosOnly()) {
				if (getContext().isEmpty()) {
					AstInterpreter.interpretNodeList(other.getNodes(), other, context, out);
				} else {
					TemplateContext otherContext = new TemplateContext();
					for (Span span : getContext().keySet()) {
						String key = span.getText();
						Object value = getContext().get(span).evaluate(template, context, out);
						otherContext.set(key, value);
					}
					AstInterpreter.interpretNodeList(other.getNodes(), other, otherContext, out);
				}
			} else {
				context.set(getAlias().getText(), getTemplate().getMacros());
			}
			return null;
		}
	}
}
