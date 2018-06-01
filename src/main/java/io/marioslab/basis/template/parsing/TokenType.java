
package io.marioslab.basis.template.parsing;

import java.util.Arrays;
import java.util.Comparator;

public enum TokenType {
	// @off
	TextBlock("a text block"),
	Period(".", "."),
	Comma(",", ","),
	Semicolon(";", ";"),
	Colon(":", ":"),
	Plus("+", "+"),
	Minus("-", "-"),
	Asterisk("*", "*"),
	ForwardSlash("/", "/"),
	Percentage("%", "%"),
	LeftParantheses("(", ")"),
	RightParantheses(")", ")"),
	LeftBracket("[", "["),
	RightBracket("]", "]"),
	LeftCurly("{", "{"),
	RightCurly("}"), // special treatment!
	Less("<", "<"),
	Greater(">", ">"),
	LessEqual("<=", "<="),
	GreaterEqual(">=", ">="),
	Equal("==", "=="),
	NotEqual("!=", "!="),
	Assignment("=", "="),
	And("&&", "&&"),
	Or("||", "||"),
	Not("!", "!"),
	Questionmark("?", "?"),
	DoubleQuote("\"", "\""),
	BooleanLiteral("true or false"),
	FloatLiteral("a floating point number"),
	IntegerLiteral("an integer number"),
	StringLiteral("a string"),
	NullLiteral("null"),
	Identifier("an identifier");
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
	final String error;

	TokenType (String error) {
		this.literal = null;
		this.error = error;
	}

	TokenType (String literal, String error) {
		this.literal = literal;
		this.error = error;
	}

	public String getLiteral () {
		return literal;
	}

	public static TokenType[] getSortedValues () {
		return values;
	}
}
