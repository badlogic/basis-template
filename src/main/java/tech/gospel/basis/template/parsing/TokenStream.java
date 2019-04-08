
package tech.gospel.basis.template.parsing;

import java.util.List;

import tech.gospel.basis.template.Error;
import tech.gospel.basis.template.TemplateLoader.Source;

/** Iterates over a list of {@link Token} instances, provides methods to match expected tokens and throw errors in case of a
 * mismatch. */
public class TokenStream {
	private final List<Token> tokens;
	private int index;
	private final int end;

	public TokenStream (List<Token> tokens) {
		this.tokens = tokens;
		this.index = 0;
		this.end = tokens.size();
	}

	/** Returns whether there are more tokens in the stream. **/
	public boolean hasMore () {
		return index < end;
	}

	/** Consumes the next token and returns it. **/
	public Token consume () {
		if (!hasMore()) throw new RuntimeException("Reached the end of the source.");
		return tokens.get(index++);
	}

	/** Checks if the next token has the give type and optionally consumes, or throws an error if the next token did not match the
	 * type. */
	public Token expect (TokenType type) {
		boolean result = match(type, true);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			if (span == null)
				Error.error("Expected '" + type.getError() + "', but reached the end of the source.", this);
			else
				Error.error("Expected '" + type.getError() + "', but got '" + token.getText() + "'", span);
			return null; // never reached
		} else {
			return tokens.get(index - 1);
		}
	}

	/** Checks if the next token matches the given text and optionally consumes, or throws an error if the next token did not match
	 * the text. */
	public Token expect (String text) {
		boolean result = match(text, true);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			if (span == null)
				Error.error("Expected '" + text + "', but reached the end of the source.", this);
			else
				Error.error("Expected '" + text + "', but got '" + token.getText() + "'", span);
			return null; // never reached
		} else {
			return tokens.get(index - 1);
		}
	}

	/** Matches and optionally consumes the next token in case of a match. Returns whether the token matched. */
	public boolean match (TokenType type, boolean consume) {
		if (index >= end) return false;
		if (tokens.get(index).getType() == type) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	/** Matches and optionally consumes the next token in case of a match. Returns whether the token matched. */
	public boolean match (String text, boolean consume) {
		if (index >= end) return false;
		if (tokens.get(index).getText().equals(text)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	/** Matches any of the token types and optionally consumes the next token in case of a match. Returns whether the token
	 * matched. */
	public boolean match (boolean consume, TokenType... types) {
		for (TokenType type : types) {
			if (match(type, consume)) return true;
		}
		return false;
	}

	/** Matches any of the token texts and optionally consumes the next token in case of a match. Returns whether the token
	 * matched. */
	public boolean match (boolean consume, String... tokenTexts) {
		for (String text : tokenTexts) {
			if (match(text, consume)) return true;
		}
		return false;
	}

	/** Returns the {@link Source} this stream wraps. */
	public Source getSource () {
		if (tokens.size() == 0) return null;
		return tokens.get(0).getSpan().getSource();
	}
}
