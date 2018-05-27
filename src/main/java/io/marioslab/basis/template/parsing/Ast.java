
package io.marioslab.basis.template.parsing;

import java.util.List;

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

	public static class TextNode extends Node {
		public TextNode (Span span) {
			super(span);
		}
	}

	public abstract static class ExpressionNode extends Node {
		public ExpressionNode (Span span) {
			super(span);
		}
	}

	public static class UnaryOperation extends ExpressionNode {

		public static enum UnaryOperator {
			Not, Negative, Positive;

			public static UnaryOperator getOperator (Token op) {
				if (op.getType() == TokenType.Not) return UnaryOperator.Not;
				if (op.getType() == TokenType.Plus) return UnaryOperator.Positive;
				if (op.getType() == TokenType.Minus) return UnaryOperator.Negative;
				Error.error("Unknown unary operator " + op + ".", op.getSpan());
				return null; // not reached
			}
		}

		private final UnaryOperator operator;
		private final ExpressionNode operand;

		public UnaryOperation (Token operator, ExpressionNode operand) {
			super(operator.getSpan());
			this.operator = UnaryOperator.getOperator(operator);
			this.operand = operand;
		}

		public UnaryOperator getOperator () {
			return operator;
		}

		public ExpressionNode getOperand () {
			return operand;
		}
	}

	public static class BinaryOperation extends ExpressionNode {

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
				if (op.getType() == TokenType.Greater) return BinaryOperator.GreaterEqual;
				if (op.getType() == TokenType.And) return BinaryOperator.And;
				if (op.getType() == TokenType.Or) return BinaryOperator.Or;
				Error.error("Unknown binary operator " + op + ".", op.getSpan());
				return null; // not reached
			}
		}

		private final ExpressionNode leftOperand;
		private final BinaryOperator operator;
		private final ExpressionNode rightOperand;

		public BinaryOperation (ExpressionNode leftOperand, Token operator, ExpressionNode rightOperand) {
			super(operator.getSpan());
			this.leftOperand = leftOperand;
			this.operator = BinaryOperator.getOperator(operator);
			this.rightOperand = rightOperand;
		}

		public ExpressionNode getLeftOperand () {
			return leftOperand;
		}

		public BinaryOperator getOperator () {
			return operator;
		}

		public ExpressionNode getRightOperand () {
			return rightOperand;
		}
	}

	public static class TernaryOperation extends ExpressionNode {
		private final ExpressionNode condition;
		private final ExpressionNode trueExpression;
		private final ExpressionNode falseExpression;

		public TernaryOperation (ExpressionNode condition, ExpressionNode trueExpression, ExpressionNode falseExpression) {
			super(new Span(condition.getSpan(), falseExpression.getSpan()));
			this.condition = condition;
			this.trueExpression = trueExpression;
			this.falseExpression = falseExpression;
		}

		public ExpressionNode getCondition () {
			return condition;
		}

		public ExpressionNode getTrueExpression () {
			return trueExpression;
		}

		public ExpressionNode getFalseExpression () {
			return falseExpression;
		}
	}

	public static class StringLiteral extends ExpressionNode {
		public StringLiteral (Span span) {
			super(span);
		}

		public String getRawValue () {
			return getSpan().getText();
		}
	}

	public static class NumberLiteral extends ExpressionNode {
		private final double value;

		public NumberLiteral (Span span) {
			super(span);
			this.value = Double.parseDouble(span.getText());
		}

		public double getValue () {
			return value;
		}
	}

	public static class BooleanLiteral extends ExpressionNode {
		private final boolean value;

		public BooleanLiteral (Span span) {
			super(span);
			this.value = Boolean.parseBoolean(span.getText());
		}

		public boolean getValue () {
			return value;
		}
	}

	public static class VariableAccess extends ExpressionNode {
		public VariableAccess (Span span) {
			super(span);
		}

		public Span getVariableName () {
			return getSpan();
		}
	}

	public static class MapOrArrayAccess extends ExpressionNode {
		private final ExpressionNode mapOrArray;
		private final ExpressionNode keyOrIndex;

		public MapOrArrayAccess (ExpressionNode mapOrArray, ExpressionNode keyOrIndex, Span closingBracket) {
			super(new Span(mapOrArray.getSpan(), closingBracket));
			this.mapOrArray = mapOrArray;
			this.keyOrIndex = keyOrIndex;
		}

		public ExpressionNode getMapOrArray () {
			return mapOrArray;
		}

		public ExpressionNode getKeyOrIndex () {
			return keyOrIndex;
		}
	}

	public static class MemberAccess extends ExpressionNode {
		private final ExpressionNode object;
		private final Span name;

		public MemberAccess (ExpressionNode object, Span name) {
			super(name);
			this.object = object;
			this.name = name;
		}

		public ExpressionNode getObject () {
			return object;
		}

		public Span getName () {
			return name;
		}
	}

	public static class FunctionCall extends ExpressionNode {
		private final ExpressionNode function;
		private final List<ExpressionNode> arguments;

		public FunctionCall (ExpressionNode function, List<ExpressionNode> arguments, Span closingParanthesis) {
			super(new Span(function.getSpan(), closingParanthesis));
			this.function = function;
			this.arguments = arguments;
		}

		public ExpressionNode getFunction () {
			return function;
		}

		public List<ExpressionNode> getArguments () {
			return arguments;
		}
	}

	public static class MethodCall extends ExpressionNode {
		private final MemberAccess method;
		private final List<ExpressionNode> arguments;

		public MethodCall (MemberAccess method, List<ExpressionNode> arguments, Span closingParanthesis) {
			super(new Span(method.getSpan(), closingParanthesis));
			this.method = method;
			this.arguments = arguments;
		}

		public ExpressionNode getObject () {
			return method.getObject();
		}

		public MemberAccess getMethod () {
			return method;
		}

		public List<ExpressionNode> getArguments () {
			return arguments;
		}
	}
}
