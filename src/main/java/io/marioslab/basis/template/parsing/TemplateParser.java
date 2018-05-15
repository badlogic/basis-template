package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

			if (segment.getText().startsWith("{{")) {
				parseStatement(segment, segments, nodes);
			} else {
				nodes.add(new TextNode(segment.getSpan()));
			}
		}
		return new Template(nodes);
	}

	private void parseStatement(TemplateSegment segment, Iterator<TemplateSegment> segments, List<Node> nodes) {
		List<Token> tokens = tokenize(segment.getSpan());
	}

	private List<Token> tokenize (Span span) {
		String source = span.source;
		CharacterStream stream = new CharacterStream(source, span.start, span.end);
		List<Token> tokens = new ArrayList<Token>();

		if (!stream.match("{{", true)) error("Expected {{", new Span(source, stream.getIndex(), stream.getIndex() + 1));

		while (stream.hasMore()) {
			// skip whitespace
			stream.skipWhiteSpace();

			// Simple tokens
			for (TokenType t: TokenType.getValues()) {
				if (t.literal != null) {
					if (stream.match(t.literal, true)) {
						tokens.add(new Token(TokenType.Period, new Span(source, stream.getIndex() - t.literal.length(), stream.getIndex())));
						continue;
					}
				}
			}

			// Number literal

			error("Unknown token", new Span(source, stream.getIndex(), stream.getIndex() + 1));
		}

		if (!stream.match("}}", true)) error("Expected }}", new Span(source, stream.getIndex(), stream.getIndex() + 1));
		return tokens;
	}

	private void error (String message, Span location) {
		// TODO generate line numbers and nice error message.
		throw new RuntimeException("Error: " + message);
	}

	private static enum TokenType {
		Identifier,
		Period("."),
		Plus("+"),
		Minus("-"),
		Asterisk("*"),
		Division("/"),
		LeftParanthese("("),
		RightParanthese(")"),
		LeftBracket("["),
		RightBracket("]"),
		Less("<"),
		Greater(">"),
		LessEqual("<="),
		GreaterEqual(">="),
		Equal("=="),
		Assignment("="),
		And("&&"),
		Or("||"),
		Not("!"),
		NumberLiteral,
		StringLiteral;

		private static TokenType[] values;

		static {
			values = TokenType.values();
			Arrays.sort(values, new Comparator<TokenType>() {
				@Override
				public int compare (TokenType o1, TokenType o2) {
					if (o1.literal == null && o2.literal == null) {
						return 0;
					}

					if (o1.literal == null && o2.literal != null) {
						return 1;
					}

					if (o1.literal != null && o2.literal == null) {
						return -1;
					}

					return o2.literal.length() - o1.literal.length();
				}
			});
		}

		private final String literal;

		TokenType () {
			this.literal = null;
		}

		TokenType (String literal) {
			this.literal = literal;
		}

		public String getLiteral () {
			return literal;
		}

		public static TokenType[] getValues () {
			return values;
		}
	}

	private static class Token {
		private final TokenType type;
		private final Span span;

		public Token (TokenType type, Span span) {
			this.type = type;
			this.span = span;
		}

		public TokenType getType () {
			return type;
		}

		public Span getSpan () {
			return span;
		}
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
}
