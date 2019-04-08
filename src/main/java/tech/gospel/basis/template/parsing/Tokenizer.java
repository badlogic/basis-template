
package tech.gospel.basis.template.parsing;

import java.util.ArrayList;
import java.util.List;

import tech.gospel.basis.template.Error;
import tech.gospel.basis.template.TemplateLoader.Source;

public class Tokenizer {

	/** Tokenizes the source into tokens with a {@link TokenType}. Text blocks not enclosed in {{ }} are returned as a single token
	 * of type {@link TokenType.TextBlock}. {{ and }} are not returned as individual tokens. See {@link TokenType} for the list of
	 * tokens this tokenizer understands. */
	public List<Token> tokenize (Source source) {
		List<Token> tokens = new ArrayList<Token>();
		if (source.getContent().length() == 0) return tokens;
		CharacterStream stream = new CharacterStream(source);
		stream.startSpan();

		// TODO: this will fall on its face if we have something like {{ "}}" }}.
		while (stream.hasMore()) {
			if (stream.match("{{", false)) {
				if (!stream.isSpanEmpty()) tokens.add(new Token(TokenType.TextBlock, stream.endSpan()));
				stream.startSpan();
				while (!stream.match("}}", true)) {
					if (!stream.hasMore()) Error.error("Did not find closing }}.", stream.endSpan());
					stream.consume();
				}
				tokens.addAll(tokenizeCodeSpan(stream.endSpan()));
				stream.startSpan();
			} else {
				stream.consume();
			}
		}
		if (!stream.isSpanEmpty()) tokens.add(new Token(TokenType.TextBlock, stream.endSpan()));
		return tokens;
	}

	private static List<Token> tokenizeCodeSpan (Span span) {
		Source source = span.getSource();
		CharacterStream stream = new CharacterStream(source, span.getStart(), span.getEnd());
		List<Token> tokens = new ArrayList<Token>();

		// match opening tag and throw it away
		if (!stream.match("{{", true)) Error.error("Expected {{", new Span(source, stream.getPosition(), stream.getPosition() + 1));

		outer:
		while (stream.hasMore()) {
			// skip whitespace
			stream.skipWhiteSpace();

			// Number literal, both integers and floats. Number literals may be suffixed by a type identifier.
			if (stream.matchDigit(false)) {
				TokenType type = TokenType.IntegerLiteral;
				stream.startSpan();
				while (stream.matchDigit(true))
					;
				if (stream.match(TokenType.Period.getLiteral(), true)) {
					type = TokenType.FloatLiteral;
					while (stream.matchDigit(true))
						;
				}
				if (stream.match("b", true)) {
					if (type == TokenType.FloatLiteral) Error.error("Byte literal can not have a decimal point.", stream.endSpan());
					type = TokenType.ByteLiteral;
				} else if (stream.match("s", true)) {
					if (type == TokenType.FloatLiteral) Error.error("Short literal can not have a decimal point.", stream.endSpan());
					type = TokenType.ShortLiteral;
				} else if (stream.match("l", true)) {
					if (type == TokenType.FloatLiteral) Error.error("Long literal can not have a decimal point.", stream.endSpan());
					type = TokenType.LongLiteral;
				} else if (stream.match("f", true)) {
					type = TokenType.FloatLiteral;
				} else if (stream.match("d", true)) {
					type = TokenType.DoubleLiteral;
				}
				Span numberSpan = stream.endSpan();
				tokens.add(new Token(type, numberSpan));
				continue;
			}

			// Character literal
			if (stream.match("'", false)) {
				stream.startSpan();
				stream.consume();
				// Note: escape sequences are parsed in CharacterLiteral
				stream.match("\\", true);
				stream.consume();
				if (!stream.match("'", true)) Error.error("Expected closing ' for character literal.", stream.endSpan());
				Span literalSpan = stream.endSpan();
				tokens.add(new Token(TokenType.CharacterLiteral, literalSpan));
				continue;
			}

			// String literal
			if (stream.match(TokenType.DoubleQuote.getLiteral(), true)) {
				stream.startSpan();
				boolean matchedEndQuote = false;
				while (stream.hasMore()) {
					// Note: escape sequences like \n are parsed in StringLiteral
					if (stream.match("\\", true)) {
						stream.consume();
					}
					if (stream.match(TokenType.DoubleQuote.getLiteral(), true)) {
						matchedEndQuote = true;
						break;
					}
					stream.consume();
				}
				if (!matchedEndQuote) Error.error("String literal is not closed by double quote", stream.endSpan());
				Span stringSpan = stream.endSpan();
				stringSpan = new Span(stringSpan.getSource(), stringSpan.getStart() - 1, stringSpan.getEnd());
				tokens.add(new Token(TokenType.StringLiteral, stringSpan));
				continue;
			}

			// Identifier, keyword, boolean literal, or null literal
			if (stream.matchIdentifierStart(true)) {
				stream.startSpan();
				while (stream.matchIdentifierPart(true))
					;
				Span identifierSpan = stream.endSpan();
				identifierSpan = new Span(identifierSpan.getSource(), identifierSpan.getStart() - 1, identifierSpan.getEnd());

				if (identifierSpan.getText().equals("true") || identifierSpan.getText().equals("false")) {
					tokens.add(new Token(TokenType.BooleanLiteral, identifierSpan));
				} else if (identifierSpan.getText().equals("null")) {
					tokens.add(new Token(TokenType.NullLiteral, identifierSpan));
				} else {
					tokens.add(new Token(TokenType.Identifier, identifierSpan));
				}
				continue;
			}

			// Simple tokens
			for (TokenType t : TokenType.getSortedValues()) {
				if (t.getLiteral() != null) {
					if (stream.match(t.getLiteral(), true)) {
						tokens.add(new Token(t, new Span(source, stream.getPosition() - t.getLiteral().length(), stream.getPosition())));
						continue outer;
					}
				}
			}

			// match closing tag
			if (stream.match("}}", false)) break;

			// single right curly for object literals
			if (stream.match("}", false)) {
				stream.startSpan();
				stream.match("}", true);
				tokens.add(new Token(TokenType.RightCurly, stream.endSpan()));
				continue;
			}

			Error.error("Unknown token", new Span(source, stream.getPosition(), stream.getPosition() + 1));
		}

		// code spans must end with }}
		if (!stream.match("}}", true)) Error.error("Expected }}", new Span(source, stream.getPosition(), stream.getPosition() + 1));
		return tokens;
	}
}
