
package io.marioslab.basis.template.parsing;

import java.util.List;

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

	/** Returns the next token and consumes it. **/
	public Token next () {
		if (!hasMore()) throw new RuntimeException("No more tokens in stream.");
		return tokens.get(index++);
	}

	/** Matches and optionally consumes the next token in case of a match, or throws an error in case the token did not match. */
	public void matchOrError (TokenType type, boolean consume) {
		boolean result = match(type, consume);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			Parser.error("Expected token of type " + type + ", got " + (token != null ? '"' + token.getText() + '"' : "end of source."), span);
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

	/** Matches and optionally consumes the next token in case of a match, or throws an error in case the token did not match. */
	public void matchOrError (String text, boolean consume) {
		boolean result = match(text, consume);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			Parser.error("Expected token of \"" + text + "\", got " + (token != null ? '"' + token.getText() + '"' : "end of source."), span);
		}
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
}
