
package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.List;
import static io.marioslab.basis.template.parsing.Parser.error;

public class Tokenizer {

	/** Tokenizes the source into tokens. Text blocks not enclosed in {{ }} are returned as a single token of type
	 * {@link TokenType.TextBlock}. {{ and }} are not returned as individual tokens. See {@link TokenType} for the list of tokens
	 * this tokenizer understands. */
	public List<Token> tokenize (String source) {
		List<Token> tokens = new ArrayList<Token>();
		if (source.length() == 0) return tokens;
		CharacterStream stream = new CharacterStream(source);
		stream.startSpan();

		while (stream.hasMore()) {
			if (stream.match("{{", false)) {
				if (!stream.isSpanEmpty()) tokens.add(new Token(TokenType.TextBlock, stream.endSpan()));
				stream.startSpan();
				while (!stream.match("}}", true)) {
					if (!stream.hasMore()) error("Did not find closing }}.", stream.endSpan());
					stream.next();
				}
				tokens.addAll(tokenizeCodeSpan(stream.endSpan()));
				stream.startSpan();
			} else {
				stream.next();
			}
		}
		if (!stream.isSpanEmpty()) tokens.add(new Token(TokenType.TextBlock, stream.endSpan()));
		return tokens;
	}

	private static List<Token> tokenizeCodeSpan (Span span) {
		String source = span.source;
		CharacterStream stream = new CharacterStream(source, span.start, span.end);
		List<Token> tokens = new ArrayList<Token>();

		// match opening tag and throw it away
		if (!stream.match("{{", true)) error("Expected {{", new Span(source, stream.getIndex(), stream.getIndex() + 1));

		outer:
		while (stream.hasMore()) {
			// skip whitespace
			stream.skipWhiteSpace();

			// Number literal
			if (stream.matchDigit(false)) {
				stream.startSpan();
				while (stream.matchDigit(true))
					;
				if (stream.match(TokenType.Period.getLiteral(), true)) while (stream.matchDigit(true))
					;
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
				while (stream.matchIdentifierPart(true))
					;
				Span identifierSpan = stream.endSpan();
				identifierSpan.start--;
				tokens.add(new Token(TokenType.Identifier, identifierSpan));
				continue;
			}

			// Simple tokens
			for (TokenType t : TokenType.getSortedValues()) {
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
}
