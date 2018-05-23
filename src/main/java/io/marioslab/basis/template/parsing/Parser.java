
package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.List;

import io.marioslab.basis.template.Template;

public class Parser {

	public Template parse (String source) {
		List<Node> nodes = new ArrayList<Node>();
		TokenStream tokens = new TokenStream(new Tokenizer().tokenize(source));

		while (tokens.hasMore()) {
			parseStatement(tokens, nodes);
		}
		return new Template(nodes);
	}

	private void parseStatement (TokenStream tokens, List<Node> nodes) {
		if (tokens.match(TokenType.TextBlock, false))
			nodes.add(new TextNode(tokens.next().getSpan()));
		else if (tokens.match("if", false))
			parseIfStatement(tokens, nodes);
		else if (tokens.match("for", false))
			parseForStatement(tokens, nodes);
		else
			nodes.add(parseExpression(tokens));
	}

	private void parseIfStatement (TokenStream stream, List<Node> nodes) {
		stream.matchOrError("if", true);

	}

	private void parseForStatement (TokenStream stream, List<Node> nodes) {
		stream.matchOrError("for", true);
	}

	private ExpressionNode parseExpression (TokenStream stream) {
		return null;
	}

	public static void error (String message, Span location) {
		// TODO generate line numbers and nice error message.
		throw new RuntimeException("Error: " + message + ", " + location.getText());
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

	public static class ExpressionNode extends Node {
		public ExpressionNode (Span span) {
			super(span);
		}
	}
}
