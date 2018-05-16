package io.marioslab.basis.template.parsing;

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

	@Override
	public String toString () {
		return "Token [type=" + type + ", span=" + span + "]";
	}

	public String getText () {
		return span.getText();
	}

	public boolean match(TokenType type) {
		return this.type == type;
	}

	public boolean matches(TokenType type, String text) {
		return this.type == type && span.getText() == text;
	}
}