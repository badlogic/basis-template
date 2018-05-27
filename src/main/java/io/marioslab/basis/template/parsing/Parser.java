
package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.List;

import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation;
import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.ExpressionNode;
import io.marioslab.basis.template.parsing.Ast.MemberAccess;
import io.marioslab.basis.template.parsing.Ast.FunctionCall;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.MethodCall;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.NumberLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.TernaryOperation;
import io.marioslab.basis.template.parsing.Ast.TextNode;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;

public class Parser {

	public Template parse (String source) {
		List<Node> nodes = new ArrayList<Node>();
		TokenStream tokens = new TokenStream(new Tokenizer().tokenize(source));

		while (tokens.hasMore()) {
			parseStatement(tokens, nodes);
		}
		return new Template(nodes);
	}

	private void parseStatement (TokenStream tokens, List<Node> nodes) {
		if (tokens.match(TokenType.TextBlock, false))
			nodes.add(new TextNode(tokens.consume().getSpan()));
		else if (tokens.match("if", false))
			parseIfStatement(tokens, nodes);
		else if (tokens.match("for", false))
			parseForStatement(tokens, nodes);
		else
			nodes.add(parseExpression(tokens));
	}

	private void parseIfStatement (TokenStream stream, List<Node> nodes) {
		stream.expect("if");
	}

	private void parseForStatement (TokenStream stream, List<Node> nodes) {
		stream.expect("for");
	}

	private ExpressionNode parseExpression (TokenStream stream) {
		return parseTernaryOperator(stream);
	}

	private ExpressionNode parseTernaryOperator (TokenStream stream) {
		ExpressionNode condition = parseBinaryOperator(stream, 0);
		if (stream.match(TokenType.Questionmark, true)) {
			ExpressionNode trueExpression = parseTernaryOperator(stream);
			stream.expect(TokenType.Colon);
			ExpressionNode falseExpression = parseTernaryOperator(stream);
			return new TernaryOperation(condition, trueExpression, falseExpression);
		} else {
			return condition;
		}
	}

	TokenType[][] binaryOperatorPrecedence = new TokenType[][] {new TokenType[] {TokenType.Or, TokenType.And},
		new TokenType[] {TokenType.Equal, TokenType.NotEqual}, new TokenType[] {TokenType.Less, TokenType.LessEqual, TokenType.Greater, TokenType.GreaterEqual},
		new TokenType[] {TokenType.Plus, TokenType.Minus}, new TokenType[] {TokenType.ForwardSlash, TokenType.Asterisk, TokenType.Percentage}};

	private ExpressionNode parseBinaryOperator (TokenStream stream, int level) {
		int nextLevel = level + 1;
		ExpressionNode left = nextLevel == binaryOperatorPrecedence.length ? parseUnaryOperator(stream) : parseBinaryOperator(stream, nextLevel);

		TokenType[] operators = binaryOperatorPrecedence[level];
		while (stream.match(false, operators)) {
			Token operator = stream.consume();
			ExpressionNode right = nextLevel == binaryOperatorPrecedence.length ? parseUnaryOperator(stream) : parseBinaryOperator(stream, nextLevel);
			left = new BinaryOperation(left, operator, right);
		}

		return left;
	}

	TokenType[] unaryOperators = new TokenType[] {TokenType.Not, TokenType.Plus, TokenType.Minus};

	private ExpressionNode parseUnaryOperator (TokenStream stream) {
		if (stream.match(false, unaryOperators)) {
			return new UnaryOperation(stream.consume(), parseUnaryOperator(stream));
		} else {
			if (stream.match(TokenType.LeftParantheses, true)) {
				ExpressionNode expression = parseExpression(stream);
				stream.expect(TokenType.LeftParantheses);
				return expression;
			} else {
				return parseAccessOrCallOrLiteral(stream);
			}
		}
	}

	private ExpressionNode parseAccessOrCallOrLiteral (TokenStream stream) {
		if (stream.match(TokenType.Identifier, false)) {
			return parseAccessOrCall(stream);
		} else if (stream.match(TokenType.StringLiteral, false)) {
			return new StringLiteral(stream.expect(TokenType.StringLiteral).getSpan());
		} else if (stream.match(TokenType.NumberLiteral, false)) {
			return new NumberLiteral(stream.expect(TokenType.NumberLiteral).getSpan());
		} else if (stream.match(TokenType.BooleanLiteral, false)) {
			return new BooleanLiteral(stream.expect(TokenType.BooleanLiteral).getSpan());
		} else {
			Error.error("Expected a variable/field/map/array access, function or method call, or literal.", stream);
			return null; // not reached
		}
	}

	private ExpressionNode parseAccessOrCall (TokenStream stream) {
		Span identifier = stream.expect(TokenType.Identifier).getSpan();
		ExpressionNode result = new VariableAccess(identifier);

		while (stream.match(false, TokenType.LeftParantheses, TokenType.LeftBracket, TokenType.Period)) {

			// function or method call
			if (stream.match(TokenType.LeftParantheses, false)) {
				List<ExpressionNode> arguments = parseArguments(stream);
				Span closingSpan = stream.expect(TokenType.RightParantheses).getSpan();
				if (result instanceof VariableAccess || result instanceof MapOrArrayAccess)
					result = new FunctionCall(result, arguments, closingSpan);
				else if (result instanceof MemberAccess) {
					result = new MethodCall((MemberAccess)result, arguments, closingSpan);
				} else {
					Error.error("Expected a variable or field/method access", stream);
				}
			}

			// map or array access
			else if (stream.match(TokenType.LeftBracket, true)) {
				ExpressionNode keyOrIndex = parseExpression(stream);
				Span closingSpan = stream.expect(TokenType.RightBracket).getSpan();
				result = new MapOrArrayAccess(result, keyOrIndex, closingSpan);
			}

			// field or method access
			else if (stream.match(TokenType.Period, true)) {
				identifier = stream.expect(TokenType.Identifier).getSpan();
				result = new MemberAccess(result, identifier);
			}
		}

		return result;
	}

	/** Does not consume the closing parentheses. **/
	private List<ExpressionNode> parseArguments (TokenStream stream) {
		stream.expect(TokenType.LeftParantheses);
		List<ExpressionNode> arguments = new ArrayList<ExpressionNode>();
		while (stream.hasMore() && !stream.match(TokenType.RightParantheses, false)) {
			arguments.add(parseExpression(stream));
			if (!stream.match(TokenType.RightParantheses, false)) stream.expect(TokenType.Comma);
		}
		return arguments;
	}
}
