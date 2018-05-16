package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.marioslab.basis.template.parsing.Span;
import io.marioslab.basis.template.parsing.TemplateParser;
import io.marioslab.basis.template.parsing.TemplateParser.TextNode;
import io.marioslab.basis.template.parsing.Token;
import io.marioslab.basis.template.parsing.TokenType;

public class TemplateParserTest {
	@Test
	public void testEmptySource () {
		Template template = new TemplateParser().parse("");
		assertEquals("Empty source produced non-empty template", 0, template.getNodes().size());
	}

	@Test
	public void testTextNodeOnly () {
		Template template = new TemplateParser().parse("This is a text node");
		assertEquals("Expected a single text node", 1, template.getNodes().size());
		assertEquals("Expected a single text node", TextNode.class, template.getNodes().get(0).getClass());
		assertEquals("This is a text node", template.getNodes().get(0).getSpan().getText());
	}

	@Test
	public void testTokenizer () {
		List<Token> tokens = TemplateParser.tokenize(new Span("{{ . + - * / ( ) [ ] < > <= >= == = && || ! 1 123 123. 123.432 \"this is a string literal with a \\\" quote \" _id var_234 $id , ; }}"));
		assertEquals("Expected 23 tokens", 28, tokens.size());
		assertEquals(TokenType.Period, tokens.get(0).getType());
		assertEquals(TokenType.Plus, tokens.get(1).getType());
		assertEquals(TokenType.Minus, tokens.get(2).getType());
		assertEquals(TokenType.Asterisk, tokens.get(3).getType());
		assertEquals(TokenType.Division, tokens.get(4).getType());
		assertEquals(TokenType.LeftParantheses, tokens.get(5).getType());
		assertEquals(TokenType.RightParantheses, tokens.get(6).getType());
		assertEquals(TokenType.LeftBracket, tokens.get(7).getType());
		assertEquals(TokenType.RightBracket, tokens.get(8).getType());
		assertEquals(TokenType.Less, tokens.get(9).getType());
		assertEquals(TokenType.Greater, tokens.get(10).getType());
		assertEquals(TokenType.LessEqual, tokens.get(11).getType());
		assertEquals(TokenType.GreaterEqual, tokens.get(12).getType());
		assertEquals(TokenType.Equal, tokens.get(13).getType());
		assertEquals(TokenType.Assignment, tokens.get(14).getType());
		assertEquals(TokenType.And, tokens.get(15).getType());
		assertEquals(TokenType.Or, tokens.get(16).getType());
		assertEquals(TokenType.Not, tokens.get(17).getType());
		assertEquals(TokenType.NumberLiteral, tokens.get(18).getType());
		assertEquals("1", tokens.get(18).getText());
		assertEquals(TokenType.NumberLiteral, tokens.get(19).getType());
		assertEquals("123", tokens.get(19).getText());
		assertEquals(TokenType.NumberLiteral, tokens.get(20).getType());
		assertEquals("123.", tokens.get(20).getText());
		assertEquals(TokenType.NumberLiteral, tokens.get(21).getType());
		assertEquals("123.432", tokens.get(21).getText());
		assertEquals(TokenType.StringLiteral, tokens.get(22).getType());
		assertEquals("\"this is a string literal with a \\\" quote \"", tokens.get(22).getText());
		assertEquals(TokenType.Identifier, tokens.get(23).getType());
		assertEquals("_id", tokens.get(23).getText());
		assertEquals(TokenType.Identifier, tokens.get(24).getType());
		assertEquals("var_234", tokens.get(24).getText());
		assertEquals(TokenType.Identifier, tokens.get(25).getType());
		assertEquals("$id", tokens.get(25).getText());
		assertEquals(TokenType.Comma, tokens.get(26).getType());
		assertEquals(TokenType.Semicolon, tokens.get(27).getType());
	}

	@Test
	public void testEmptyNode () {
		assertEquals("Expected 0 nodes", 0, new TemplateParser().parse("{{ }}").getNodes().size());
	}
}
