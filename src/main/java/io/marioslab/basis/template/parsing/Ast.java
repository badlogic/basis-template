
package io.marioslab.basis.template.parsing;

import java.util.List;
import java.util.Map;

public abstract class Ast {
	public static class Node {
		private final Span span;

		public Node (Span span) {
			this.span = span;
		}

		public Span getSpan () {
			return span;
		}

		@Override
		public String toString () {
			return span.getText();
		}
	}

	public static class Text extends Node {
		public Text (Span text) {
			super(text);
		}
	}

	public abstract static class Expression extends Node {
		public Expression (Span span) {
			super(span);
		}
	}

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

	public static class BinaryOperation extends Expression {

		public static enum BinaryOperator {
			Addition, Subtraction, Multiplication, Division, Modulo, Equal, NotEqual, Less, LessEqual, Greater, GreaterEqual, And, Or;

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

	public static class StringLiteral extends Expression {
		public StringLiteral (Span literal) {
			super(literal);
		}

		public String getRawValue () {
			return getSpan().getText();
		}
	}

	public static class NumberLiteral extends Expression {
		private final double value;

		public NumberLiteral (Span literal) {
			super(literal);
			this.value = Double.parseDouble(literal.getText());
		}

		public double getValue () {
			return value;
		}
	}

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

	public static class VariableAccess extends Expression {
		public VariableAccess (Span name) {
			super(name);
		}

		public Span getVariableName () {
			return getSpan();
		}
	}

	public static class MapOrArrayAccess extends Expression {
		private final Expression mapOrArray;
		private final Expression keyOrIndex;

		public MapOrArrayAccess (Span span, Expression mapOrArray, Expression keyOrIndex) {
			super(span);
			this.mapOrArray = mapOrArray;
			this.keyOrIndex = keyOrIndex;
		}

		public Expression getMapOrArray () {
			return mapOrArray;
		}

		public Expression getKeyOrIndex () {
			return keyOrIndex;
		}
	}

	public static class MemberAccess extends Expression {
		private final Expression object;
		private final Span name;

		public MemberAccess (Expression object, Span name) {
			super(name);
			this.object = object;
			this.name = name;
		}

		public Expression getObject () {
			return object;
		}

		public Span getName () {
			return name;
		}
	}

	public static class FunctionCall extends Expression {
		private final Expression function;
		private final List<Expression> arguments;

		public FunctionCall (Span span, Expression function, List<Expression> arguments) {
			super(span);
			this.function = function;
			this.arguments = arguments;
		}

		public Expression getFunction () {
			return function;
		}

		public List<Expression> getArguments () {
			return arguments;
		}
	}

	public static class MethodCall extends Expression {
		private final MemberAccess method;
		private final List<Expression> arguments;

		public MethodCall (Span span, MemberAccess method, List<Expression> arguments) {
			super(span);
			this.method = method;
			this.arguments = arguments;
		}

		public Expression getObject () {
			return method.getObject();
		}

		public MemberAccess getMethod () {
			return method;
		}

		public List<Expression> getArguments () {
			return arguments;
		}
	}

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

		/** May return null **/
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

	public static class Macro extends Node {
		private final Span name;
		private final List<Span> argumentNames;
		private final List<Node> body;

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
	}

	public static class Include extends Node {
		private final Span path;
		private final Map<Span, Expression> context;

		public Include (Span span, Span path, Map<Span, Expression> context) {
			super(span);
			this.path = path;
			this.context = context;
		}

		public Span getPath () {
			return path;
		}

		public Map<Span, Expression> getContext () {
			return context;
		}
	}
}
