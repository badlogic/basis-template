
package io.marioslab.basis.template.parsing;

/** A token produced by the {@link Tokenizer}. */
public class Token {
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

	public String getText () {
		return span.getText();
	}

	/** @return whether the token matches the type. */
	public boolean match (TokenType type) {
		return this.type == type;
	}

	/** @return whether the token matches the type and text. */
	public boolean matches (TokenType type, String text) {
		return this.type == type && span.getText() == text;
	}

	@Override
	public String toString () {
		return "Token [type=" + type + ", span=" + span + "]";
	}
}
