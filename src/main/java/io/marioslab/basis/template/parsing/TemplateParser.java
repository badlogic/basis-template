package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.parsing.TemplateSegmentParser.TemplateSegment;

public class TemplateParser {

	public Template parse (String source) {
		Iterator<TemplateSegment> segments = new TemplateSegmentParser().parse(source).iterator();
		List<Node> nodes = new ArrayList<Node>();

		while (segments.hasNext()) {
			TemplateSegment segment = segments.next();

			if (segment.getText().startsWith("{{"))
				parseStatement(segment, segments, nodes);
			else
				nodes.add(new TextNode(segment.getSpan()));
		}
		return new Template(nodes);
	}

	private void parseStatement(TemplateSegment segment, Iterator<TemplateSegment> segments, List<Node> nodes) {
		List<Token> tokens = tokenize(segment.getSpan());
		if (tokens.size() == 0) return;

		TokenStream stream = new TokenStream(tokens);
		if (stream.match("if", false))
			parseIfStatement(stream, segments, nodes);
		else if (stream.match("for", false))
			parseForStatement(stream, segments, nodes);
		else
			nodes.add(parseExpression(stream));
	}

	private void parseIfStatement(TokenStream stream, Iterator<TemplateSegment> segments, List<Node> nodes) {
		stream.matchOrError("if", true);

	}

	private void parseForStatement(TokenStream stream, Iterator<TemplateSegment> segments, List<Node> nodes) {
		stream.matchOrError("for", true);
	}

	private ExpressionNode parseExpression(TokenStream stream) {
		return null;
	}

	public static List<Token> tokenize (Span span) {
		String source = span.source;
		CharacterStream stream = new CharacterStream(source, span.start, span.end);
		List<Token> tokens = new ArrayList<Token>();

		// match opening tag
		if (!stream.match("{{", true)) error("Expected {{", new Span(source, stream.getIndex(), stream.getIndex() + 1));

		outer:
		while (stream.hasMore()) {
			// skip whitespace
			stream.skipWhiteSpace();

			// Number literal
			if (stream.matchDigit(false)) {
				stream.startSpan();
				while (stream.matchDigit(true));
				if (stream.match(TokenType.Period.getLiteral(), true))
					while (stream.matchDigit(true));
				Span numberSpan = stream.endSpan();
				tokens.add(new Token(TokenType.NumberLiteral, numberSpan));
				continue;
			}

			// String literal
			if (stream.match(TokenType.DoubleQuote.literal, true)) {
				stream.startSpan();
				boolean matchedEndQuote = false;
				while (stream.hasMore()) {
					if (stream.match("\\\"", true)) continue;
					if (stream.match(TokenType.DoubleQuote.literal, true)) {
						matchedEndQuote = true;
						break;
					}
					stream.next();
				}
				if (!matchedEndQuote) error("String literal is not closed by double quote", stream.endSpan());
				Span stringSpan = stream.endSpan();
				stringSpan.start--;
				tokens.add(new Token(TokenType.StringLiteral, stringSpan));
				continue;
			}

			// Identifiers
			if (stream.matchIdentifierStart(true)) {
				stream.startSpan();
				while (stream.matchIdentifierPart(true));
				Span identifierSpan = stream.endSpan();
				identifierSpan.start--;
				tokens.add(new Token(TokenType.Identifier, identifierSpan));
				continue;
			}

			// Simple tokens
			for (TokenType t: TokenType.getSortedValues()) {
				if (t.literal != null) {
					if (stream.match(t.literal, true)) {
						tokens.add(new Token(t, new Span(source, stream.getIndex() - t.literal.length(), stream.getIndex())));
						continue outer;
					}
				}
			}

			// match closing tag
			if (stream.match("}}", false)) break;

			error("Unknown token", new Span(source, stream.getIndex(), stream.getIndex() + 1));
		}

		// just another sanity check
		if (!stream.match("}}", true)) error("Expected }}", new Span(source, stream.getIndex(), stream.getIndex() + 1));
		return tokens;
	}

	public static void error (String message, Span location) {
		// TODO generate line numbers and nice error message.
		throw new RuntimeException("Error: " + message + ", " + location.getText());
	}

	public static class Node {
		private final Span span;

		public Node (Span span) {
			this.span = span;
		}

		public Span getSpan () {
			return span;
		}
	}

	public static class TextNode extends Node {
		public TextNode (Span span) {
			super(span);
		}
	}

	public static class ExpressionNode extends Node {
		public ExpressionNode (Span span) {
			super(span);
		}
	}
}
