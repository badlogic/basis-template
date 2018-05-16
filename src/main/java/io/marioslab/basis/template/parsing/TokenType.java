
package io.marioslab.basis.template.parsing;

import java.util.Arrays;
import java.util.Comparator;

public enum TokenType {
	// @off
	Period("."),
	Comma(","),
	Semicolon(";"),
	Plus("+"),
	Minus("-"),
	Asterisk("*"),
	Division("/"),
	LeftParantheses("("),
	RightParantheses(")"),
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
	DoubleQuote("\""),
	NumberLiteral,
	StringLiteral,
	Identifier;
	// @on

	private static TokenType[] values;

	static {
		values = TokenType.values();
		Arrays.sort(values, new Comparator<TokenType>() {
			@Override
			public int compare (TokenType o1, TokenType o2) {
				if (o1.literal == null && o2.literal == null) return 0;
				if (o1.literal == null && o2.literal != null) return 1;
				if (o1.literal != null && o2.literal == null) return -1;
				return o2.literal.length() - o1.literal.length();
			}
		});
	}

	final String literal;

	TokenType () {
		this.literal = null;
	}

	TokenType (String literal) {
		this.literal = literal;
	}

	public String getLiteral () {
		return literal;
	}

	public static TokenType[] getSortedValues () {
		return values;
	}
}
