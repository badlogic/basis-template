package io.marioslab.basis.template.parsing;

import java.util.List;

public class TokenStream {
	private final List<Token> tokens;
	private int index;
	private final int end;

	public TokenStream(List<Token> tokens) {
		this.tokens = tokens;
		this.index = 0;
		this.end = tokens.size();
	}

	public boolean hasMore () {
		return index < end;
	}

	public Token next () {
		if (!hasMore()) throw new RuntimeException("No more tokens in stream.");
		return tokens.get(index++);
	}

	public boolean matchOrError(TokenType type, boolean consume) {
		boolean result = match(type, consume);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			TemplateParser.error("Expected token of type " + type + ", got " + (token != null ? '"' + token.getText() + '"': "end of source."), span);
		}
		return result;
	}

	public boolean match(TokenType type, boolean consume) {
		if (index >= end) return false;
		if (tokens.get(index).getType() == type) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public boolean matchOrError(String text, boolean consume) {
		boolean result = match(text, consume);
		if (!result) {
			Token token = index < tokens.size() ? tokens.get(index) : null;
			Span span = token != null ? token.getSpan() : null;
			TemplateParser.error("Expected token of \"" + text + "\", got " + (token != null ? '"' + token.getText() + '"': "end of source."), span);
		}
		return result;
	}

	public boolean match(String text, boolean consume) {
		if (index >= end) return false;
		if (tokens.get(index).getText().equals(text)) {
			if (consume) index++;
			return true;
		}
		return false;
	}
}
