
package tech.gospel.basis.template.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.gospel.basis.template.Error;
import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateLoader.Source;
import tech.gospel.basis.template.parsing.Ast.BinaryOperation;
import tech.gospel.basis.template.parsing.Ast.BooleanLiteral;
import tech.gospel.basis.template.parsing.Ast.Break;
import tech.gospel.basis.template.parsing.Ast.ByteLiteral;
import tech.gospel.basis.template.parsing.Ast.CharacterLiteral;
import tech.gospel.basis.template.parsing.Ast.Continue;
import tech.gospel.basis.template.parsing.Ast.DoubleLiteral;
import tech.gospel.basis.template.parsing.Ast.Expression;
import tech.gospel.basis.template.parsing.Ast.FloatLiteral;
import tech.gospel.basis.template.parsing.Ast.ForStatement;
import tech.gospel.basis.template.parsing.Ast.FunctionCall;
import tech.gospel.basis.template.parsing.Ast.IfStatement;
import tech.gospel.basis.template.parsing.Ast.Include;
import tech.gospel.basis.template.parsing.Ast.IncludeRaw;
import tech.gospel.basis.template.parsing.Ast.IntegerLiteral;
import tech.gospel.basis.template.parsing.Ast.ListLiteral;
import tech.gospel.basis.template.parsing.Ast.LongLiteral;
import tech.gospel.basis.template.parsing.Ast.Macro;
import tech.gospel.basis.template.parsing.Ast.MapLiteral;
import tech.gospel.basis.template.parsing.Ast.MapOrArrayAccess;
import tech.gospel.basis.template.parsing.Ast.MemberAccess;
import tech.gospel.basis.template.parsing.Ast.MethodCall;
import tech.gospel.basis.template.parsing.Ast.Node;
import tech.gospel.basis.template.parsing.Ast.NullLiteral;
import tech.gospel.basis.template.parsing.Ast.Return;
import tech.gospel.basis.template.parsing.Ast.ShortLiteral;
import tech.gospel.basis.template.parsing.Ast.StringLiteral;
import tech.gospel.basis.template.parsing.Ast.TernaryOperation;
import tech.gospel.basis.template.parsing.Ast.Text;
import tech.gospel.basis.template.parsing.Ast.UnaryOperation;
import tech.gospel.basis.template.parsing.Ast.VariableAccess;
import tech.gospel.basis.template.parsing.Ast.WhileStatement;

/** Parses a {@link Source} into a {@link Template}. The implementation is a simple recursive descent parser with a lookahead of
 * 1. **/
public class Parser {

	/** Parses a {@link Source} into a {@link Template}. **/
	public ParserResult parse (Source source) {
		List<Node> nodes = new ArrayList<Node>();
		Macros macros = new Macros();
		List<Include> includes = new ArrayList<Include>();
		List<IncludeRaw> rawIncludes = new ArrayList<IncludeRaw>();
		TokenStream stream = new TokenStream(new Tokenizer().tokenize(source));

		while (stream.hasMore()) {
			nodes.add(parseStatement(stream, true, macros, includes, rawIncludes));
		}
		return new ParserResult(nodes, macros, includes, rawIncludes);
	}

	/** Parse a statement, which may either be a text block, if statement, for statement, while statement, macro definition,
	 * include statement or an expression. **/
	private Node parseStatement (TokenStream tokens, boolean allowMacros, Macros macros, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Node result = null;

		if (tokens.match(TokenType.TextBlock, false))
			result = new Text(tokens.consume().getSpan());
		else if (tokens.match("if", false))
			result = parseIfStatement(tokens, includes, rawIncludes);
		else if (tokens.match("for", false))
			result = parseForStatement(tokens, includes, rawIncludes);
		else if (tokens.match("while", false))
			result = parseWhileStatement(tokens, includes, rawIncludes);
		else if (tokens.match("continue", false))
			result = new Continue(tokens.consume().getSpan());
		else if (tokens.match("break", false))
			result = new Break(tokens.consume().getSpan());
		else if (tokens.match("macro", false)) {
			if (!allowMacros) {
				Error.error("Macros can only be defined at the top level of a template.", tokens.consume().getSpan());
				result = null; // never reached
			} else {
				Macro macro = parseMacro(tokens, includes, rawIncludes);
				macros.put(macro.getName().getText(), macro);
				result = macro; //
			}
		} else if (tokens.match("include", false)) {
			result = parseInclude(tokens, includes, rawIncludes);
		} else if (tokens.match("return", false)) {
			result = parseReturn(tokens);
		} else
			result = parseExpression(tokens);

		// consume semi-colons as statement delimiters
		while (tokens.match(";", true))
			;

		return result;
	}

	private IfStatement parseIfStatement (TokenStream stream, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Span openingIf = stream.expect("if").getSpan();

		Expression condition = parseExpression(stream);

		List<Node> trueBlock = new ArrayList<Node>();
		while (stream.hasMore() && !stream.match(false, "elseif", "else", "end")) {
			trueBlock.add(parseStatement(stream, false, null, includes, rawIncludes));
		}

		List<IfStatement> elseIfs = new ArrayList<IfStatement>();
		while (stream.hasMore() && stream.match(false, "elseif")) {
			Span elseIfOpening = stream.expect("elseif").getSpan();

			Expression elseIfCondition = parseExpression(stream);

			List<Node> elseIfBlock = new ArrayList<Node>();
			while (stream.hasMore() && !stream.match(false, "elseif", "else", "end")) {
				elseIfBlock.add(parseStatement(stream, false, null, includes, rawIncludes));
			}

			Span elseIfSpan = new Span(elseIfOpening, elseIfBlock.size() > 0 ? elseIfBlock.get(elseIfBlock.size() - 1).getSpan() : elseIfOpening);
			elseIfs.add(new IfStatement(elseIfSpan, elseIfCondition, elseIfBlock, new ArrayList<IfStatement>(), new ArrayList<Node>()));
		}

		List<Node> falseBlock = new ArrayList<Node>();
		if (stream.match("else", true)) {
			while (stream.hasMore() && !stream.match(false, "end")) {
				falseBlock.add(parseStatement(stream, false, null, includes, rawIncludes));
			}
		}

		Span closingEnd = stream.expect("end").getSpan();

		return new IfStatement(new Span(openingIf, closingEnd), condition, trueBlock, elseIfs, falseBlock);
	}

	private ForStatement parseForStatement (TokenStream stream, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Span openingFor = stream.expect("for").getSpan();

		Span index = null;
		Span value = stream.expect(TokenType.Identifier).getSpan();

		if (stream.match(TokenType.Comma, true)) {
			index = value;
			value = stream.expect(TokenType.Identifier).getSpan();
		}

		stream.expect("in");

		Expression mapOrArray = parseExpression(stream);

		List<Node> body = new ArrayList<Node>();
		while (stream.hasMore() && !stream.match(false, "end")) {
			body.add(parseStatement(stream, false, null, includes, rawIncludes));
		}

		Span closingEnd = stream.expect("end").getSpan();

		return new ForStatement(new Span(openingFor, closingEnd), index != null ? index : null, value, mapOrArray, body);
	}

	private WhileStatement parseWhileStatement (TokenStream stream, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Span openingWhile = stream.expect("while").getSpan();

		Expression condition = parseExpression(stream);

		List<Node> body = new ArrayList<Node>();
		while (stream.hasMore() && !stream.match(false, "end")) {
			body.add(parseStatement(stream, false, null, includes, rawIncludes));
		}

		Span closingEnd = stream.expect("end").getSpan();

		return new WhileStatement(new Span(openingWhile, closingEnd), condition, body);
	}

	private Macro parseMacro (TokenStream stream, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Span openingWhile = stream.expect("macro").getSpan();

		Span name = stream.expect(TokenType.Identifier).getSpan();

		List<Span> argumentNames = parseArgumentNames(stream);

		stream.expect(TokenType.RightParantheses);

		List<Node> body = new ArrayList<Node>();
		while (stream.hasMore() && !stream.match(false, "end")) {
			body.add(parseStatement(stream, false, null, includes, rawIncludes));
		}

		Span closingEnd = stream.expect("end").getSpan();

		return new Macro(new Span(openingWhile, closingEnd), name, argumentNames, body);
	}

	/** Does not consume the closing parentheses. **/
	private List<Span> parseArgumentNames (TokenStream stream) {
		stream.expect(TokenType.LeftParantheses);
		List<Span> arguments = new ArrayList<Span>();
		while (stream.hasMore() && !stream.match(TokenType.RightParantheses, false)) {
			arguments.add(stream.expect(TokenType.Identifier).getSpan());
			if (!stream.match(TokenType.RightParantheses, false)) stream.expect(TokenType.Comma);
		}
		return arguments;
	}

	private Node parseInclude (TokenStream stream, List<Include> includes, List<IncludeRaw> rawIncludes) {
		Span openingInclude = stream.expect("include").getSpan();
		if (stream.match("raw", true)) {
			Span path = stream.expect(TokenType.StringLiteral).getSpan();
			IncludeRaw rawInclude = new IncludeRaw(new Span(openingInclude, path), path);
			rawIncludes.add(rawInclude);
			return rawInclude;
		}

		Span path = stream.expect(TokenType.StringLiteral).getSpan();
		Span closing = path;

		Include include = null;
		if (stream.match("with", true)) {
			Map<Span, Expression> context = parseMap(stream);
			closing = stream.expect(TokenType.RightParantheses).getSpan();
			include = new Include(new Span(openingInclude, closing), path, context, false, null);
		} else if (stream.match("as", true)) {
			Span alias = stream.expect(TokenType.Identifier).getSpan();
			closing = alias;
			include = new Include(new Span(openingInclude, closing), path, null, true, alias);
		} else {
			include = new Include(new Span(openingInclude, closing), path, new HashMap<Span, Expression>(), false, null);
		}
		includes.add(include);
		return include;
	}

	/** Does not consume the closing parentheses. **/
	private Map<Span, Expression> parseMap (TokenStream stream) {
		stream.expect(TokenType.LeftParantheses);
		Map<Span, Expression> map = new HashMap<Span, Expression>();
		while (stream.hasMore() && !stream.match(TokenType.RightParantheses, false)) {
			Span key = stream.expect(TokenType.Identifier).getSpan();
			stream.expect(TokenType.Colon);
			map.put(key, parseExpression(stream));
			if (!stream.match(TokenType.RightParantheses, false)) stream.expect(TokenType.Comma);
		}
		return map;
	}

	private Expression parseExpression (TokenStream stream) {
		return parseTernaryOperator(stream);
	}

	private Expression parseTernaryOperator (TokenStream stream) {
		Expression condition = parseBinaryOperator(stream, 0);
		if (stream.match(TokenType.Questionmark, true)) {
			Expression trueExpression = parseTernaryOperator(stream);
			stream.expect(TokenType.Colon);
			Expression falseExpression = parseTernaryOperator(stream);
			return new TernaryOperation(condition, trueExpression, falseExpression);
		} else {
			return condition;
		}
	}

	TokenType[][] binaryOperatorPrecedence = new TokenType[][] {new TokenType[] {TokenType.Assignment},
		new TokenType[] {TokenType.Or, TokenType.And, TokenType.Xor}, new TokenType[] {TokenType.Equal, TokenType.NotEqual},
		new TokenType[] {TokenType.Less, TokenType.LessEqual, TokenType.Greater, TokenType.GreaterEqual}, new TokenType[] {TokenType.Plus, TokenType.Minus},
		new TokenType[] {TokenType.ForwardSlash, TokenType.Asterisk, TokenType.Percentage}};

	private Expression parseBinaryOperator (TokenStream stream, int level) {
		int nextLevel = level + 1;
		Expression left = nextLevel == binaryOperatorPrecedence.length ? parseUnaryOperator(stream) : parseBinaryOperator(stream, nextLevel);

		TokenType[] operators = binaryOperatorPrecedence[level];
		while (stream.hasMore() && stream.match(false, operators)) {
			Token operator = stream.consume();
			Expression right = nextLevel == binaryOperatorPrecedence.length ? parseUnaryOperator(stream) : parseBinaryOperator(stream, nextLevel);
			left = new BinaryOperation(left, operator, right);
		}

		return left;
	}

	TokenType[] unaryOperators = new TokenType[] {TokenType.Not, TokenType.Plus, TokenType.Minus};

	private Expression parseUnaryOperator (TokenStream stream) {
		if (stream.match(false, unaryOperators)) {
			return new UnaryOperation(stream.consume(), parseUnaryOperator(stream));
		} else {
			if (stream.match(TokenType.LeftParantheses, true)) {
				Expression expression = parseExpression(stream);
				stream.expect(TokenType.RightParantheses);
				return expression;
			} else {
				return parseAccessOrCallOrLiteral(stream);
			}
		}
	}

	private Expression parseAccessOrCallOrLiteral (TokenStream stream) {
		if (stream.match(TokenType.Identifier, false)) {
			return parseAccessOrCall(stream);
		} else if (stream.match(TokenType.LeftCurly, false)) {
			return parseMapLiteral(stream);
		} else if (stream.match(TokenType.LeftBracket, false)) {
			return parseListLiteral(stream);
		} else if (stream.match(TokenType.StringLiteral, false)) {
			return new StringLiteral(stream.expect(TokenType.StringLiteral).getSpan());
		} else if (stream.match(TokenType.BooleanLiteral, false)) {
			return new BooleanLiteral(stream.expect(TokenType.BooleanLiteral).getSpan());
		} else if (stream.match(TokenType.DoubleLiteral, false)) {
			return new DoubleLiteral(stream.expect(TokenType.DoubleLiteral).getSpan());
		} else if (stream.match(TokenType.FloatLiteral, false)) {
			return new FloatLiteral(stream.expect(TokenType.FloatLiteral).getSpan());
		} else if (stream.match(TokenType.ByteLiteral, false)) {
			return new ByteLiteral(stream.expect(TokenType.ByteLiteral).getSpan());
		} else if (stream.match(TokenType.ShortLiteral, false)) {
			return new ShortLiteral(stream.expect(TokenType.ShortLiteral).getSpan());
		} else if (stream.match(TokenType.IntegerLiteral, false)) {
			return new IntegerLiteral(stream.expect(TokenType.IntegerLiteral).getSpan());
		} else if (stream.match(TokenType.LongLiteral, false)) {
			return new LongLiteral(stream.expect(TokenType.LongLiteral).getSpan());
		} else if (stream.match(TokenType.CharacterLiteral, false)) {
			return new CharacterLiteral(stream.expect(TokenType.CharacterLiteral).getSpan());
		} else if (stream.match(TokenType.NullLiteral, false)) {
			return new NullLiteral(stream.expect(TokenType.NullLiteral).getSpan());
		} else {
			Error.error("Expected a variable, field, map, array, function or method call, or literal.", stream);
			return null; // not reached
		}
	}

	private Expression parseMapLiteral (TokenStream stream) {
		Span openCurly = stream.expect(TokenType.LeftCurly).getSpan();

		List<Span> keys = new ArrayList<>();
		List<Expression> values = new ArrayList<>();
		while (stream.hasMore() && !stream.match(TokenType.RightCurly, false)) {
			keys.add(stream.expect(TokenType.Identifier).getSpan());
			stream.expect(":");
			values.add(parseExpression(stream));
			if (!stream.match(TokenType.RightCurly, false)) stream.expect(TokenType.Comma);
		}

		Span closeCurly = stream.expect(TokenType.RightCurly).getSpan();
		return new MapLiteral(new Span(openCurly, closeCurly), keys, values);
	}

	private Expression parseListLiteral (TokenStream stream) {
		Span openBracket = stream.expect(TokenType.LeftBracket).getSpan();

		List<Expression> values = new ArrayList<>();
		while (stream.hasMore() && !stream.match(TokenType.RightBracket, false)) {
			values.add(parseExpression(stream));
			if (!stream.match(TokenType.RightBracket, false)) stream.expect(TokenType.Comma);
		}

		Span closeBracket = stream.expect(TokenType.RightBracket).getSpan();
		return new ListLiteral(new Span(openBracket, closeBracket), values);
	}

	private Expression parseAccessOrCall (TokenStream stream) {
		Span identifier = stream.expect(TokenType.Identifier).getSpan();
		Expression result = new VariableAccess(identifier);

		while (stream.hasMore() && stream.match(false, TokenType.LeftParantheses, TokenType.LeftBracket, TokenType.Period)) {

			// function or method call
			if (stream.match(TokenType.LeftParantheses, false)) {
				List<Expression> arguments = parseArguments(stream);
				Span closingSpan = stream.expect(TokenType.RightParantheses).getSpan();
				if (result instanceof VariableAccess || result instanceof MapOrArrayAccess)
					result = new FunctionCall(new Span(result.getSpan(), closingSpan), result, arguments);
				else if (result instanceof MemberAccess) {
					result = new MethodCall(new Span(result.getSpan(), closingSpan), (MemberAccess)result, arguments);
				} else {
					Error.error("Expected a variable, field or method.", stream);
				}
			}

			// map or array access
			else if (stream.match(TokenType.LeftBracket, true)) {
				Expression keyOrIndex = parseExpression(stream);
				Span closingSpan = stream.expect(TokenType.RightBracket).getSpan();
				result = new MapOrArrayAccess(new Span(result.getSpan(), closingSpan), result, keyOrIndex);
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
	private List<Expression> parseArguments (TokenStream stream) {
		stream.expect(TokenType.LeftParantheses);
		List<Expression> arguments = new ArrayList<Expression>();
		while (stream.hasMore() && !stream.match(TokenType.RightParantheses, false)) {
			arguments.add(parseExpression(stream));
			if (!stream.match(TokenType.RightParantheses, false)) stream.expect(TokenType.Comma);
		}
		return arguments;
	}

	private Node parseReturn (TokenStream tokens) {
		Span returnSpan = tokens.expect("return").getSpan();
		if (tokens.match(";", false)) return new Return(returnSpan, null);
		Expression returnValue = parseExpression(tokens);
		return new Return(new Span(returnSpan, returnValue.getSpan()), returnValue);
	}

	@SuppressWarnings("serial")
	public static class Macros extends HashMap<String, Macro> {
	}

	public static class ParserResult {
		private final List<Node> nodes;
		private final Macros macros;
		private final List<Include> includes;
		private final List<IncludeRaw> rawIncludes;

		public ParserResult (List<Node> nodes, Macros macros, List<Include> includes, List<IncludeRaw> rawIncludes) {
			this.nodes = nodes;
			this.macros = macros;
			this.includes = includes;
			this.rawIncludes = rawIncludes;
		}

		public List<Node> getNodes () {
			return nodes;
		}

		public Macros getMacros () {
			return macros;
		}

		public List<Include> getIncludes () {
			return includes;
		}

		public List<IncludeRaw> getRawIncludes () {
			return rawIncludes;
		}
	}
}
