
package io.marioslab.basis.template.parsing;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import io.marioslab.basis.template.Error;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.Source;
import io.marioslab.basis.template.interpreter.AstInterpreter;
import io.marioslab.basis.template.interpreter.Reflection;

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
	}

	/** A null literal, with the single value <code>null</code> **/
	public static class NullLiteral extends Expression {
		public NullLiteral (Span span) {
			super(span);
		}
	}

	/** A boolean literal, with the values <code>true</code> and <code>false</code> **/
	public static class BooleanLiteral extends Expression {
		private final boolean value;

		public BooleanLiteral (Span literal) {
			super(literal);
			this.value = Boolean.parseBoolean(literal.getText());
		}

		public boolean getValue () {
			return value;
		}
	}

	/** A double precision floating point literal. Must be marked with the <code>d</code> suffix, e.g. "1.0d". **/
	public static class DoubleLiteral extends Expression {
		private final double value;

		public DoubleLiteral (Span literal) {
			super(literal);
			this.value = Double.parseDouble(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public double getValue () {
			return value;
		}
	}

	/** A single precision floating point literla. May be optionally marked with the <code>f</code> suffix, e.g. "1.0f". **/
	public static class FloatLiteral extends Expression {
		private final float value;

		public FloatLiteral (Span literal) {
			super(literal);
			String text = literal.getText();
			if (text.charAt(text.length() - 1) == 'f') text = text.substring(0, text.length() - 1);
			this.value = Float.parseFloat(text);
		}

		public float getValue () {
			return value;
		}
	}

	/** A byte literal. Must be marked with the <code>b</code> suffix, e.g. "123b". **/
	public static class ByteLiteral extends Expression {
		private final byte value;

		public ByteLiteral (Span literal) {
			super(literal);
			this.value = Byte.parseByte(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public byte getValue () {
			return value;
		}
	}

	/** A short literal. Must be marked with the <code>s</code> suffix, e.g. "123s". **/
	public static class ShortLiteral extends Expression {
		private final short value;

		public ShortLiteral (Span literal) {
			super(literal);
			this.value = Short.parseShort(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public short getValue () {
			return value;
		}
	}

	/** An integer literal. **/
	public static class IntegerLiteral extends Expression {
		private final int value;

		public IntegerLiteral (Span literal) {
			super(literal);
			this.value = Integer.parseInt(literal.getText());
		}

		public int getValue () {
			return value;
		}
	}

	/** A long integer literal. Must be marked with the <code>l</code> suffix, e.g. "123l". **/
	public static class LongLiteral extends Expression {
		private final long value;

		public LongLiteral (Span literal) {
			super(literal);
			this.value = Long.parseLong(literal.getText().substring(0, literal.getText().length() - 1));
		}

		public long getValue () {
			return value;
		}
	}

	/** A character literal, enclosed in single quotes. Supports escape sequences \n, \r,\t, \' and \\. **/
	public static class CharacterLiteral extends Expression {
		private final char value;

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

		public char getValue () {
			return value;
		}
	}

	/** A string literal, enclosed in double quotes. Does not support escape sequences. **/
	public static class StringLiteral extends Expression {
		private final String cachedValue;

		public StringLiteral (Span literal) {
			super(literal);
			String text = getSpan().getText();
			cachedValue = text.substring(1, text.length() - 1);
		}

		/** Returns the literal without quotes **/
		public String getValue () {
			return cachedValue;
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
	}

	/** Represents a call to a top-level function. A function may either be a {@link FunctionalInterface} stored in a
	 * {@link TemplateContext}, or a {@link Macro} defined in a template. */
	public static class FunctionCall extends Expression {
		private final Expression function;
		private final List<Expression> arguments;
		private Object cachedFunction;
		private final Object[] cachedArguments;

		public FunctionCall (Span span, Expression function, List<Expression> arguments) {
			super(span);
			this.function = function;
			this.arguments = arguments;
			this.cachedArguments = new Object[arguments.size()];
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
			return cachedArguments;
		}
	}

	/** Represents a call to a method of the form <code>object.method(a, b, c)</code>. **/
	public static class MethodCall extends Expression {
		private final MemberAccess method;
		private final List<Expression> arguments;
		private Object cachedMethod;
		private final Object[] cachedArguments;

		public MethodCall (Span span, MemberAccess method, List<Expression> arguments) {
			super(span);
			this.method = method;
			this.arguments = arguments;
			this.cachedArguments = new Object[arguments.size()];
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
			return cachedArguments;
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
	}
}
