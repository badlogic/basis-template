
package tech.gospel.basis.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import tech.gospel.basis.template.TemplateLoader.Source;
import tech.gospel.basis.template.parsing.Token;
import tech.gospel.basis.template.parsing.TokenType;
import tech.gospel.basis.template.parsing.Tokenizer;

public class TokenizerTest {
	@Test
	public void testEmptysegment () {
		List<Token> tokens = new Tokenizer().tokenize(new Source("test", ""));
		assertEquals("Tokens are not empty", 0, tokens.size());
	}

	@Test
	public void testNoTemplateTemplateSegment () {
		List<Token> tokens = new Tokenizer().tokenize(new Source("test", "this is a test"));
		assertEquals("Expected one text block token", 1, tokens.size());
		assertEquals("Expected one text block token", "this is a test", tokens.get(0).getText());
	}

	@Test
	public void testOnlyTemplateTemplateSegment () {
		List<Token> tokens = new Tokenizer().tokenize(new Source("test", "{{ identifier }}"));
		assertEquals("Expected one identifier token", 1, tokens.size());
		assertEquals("Expected one identifier token", "identifier", tokens.get(0).getText());
	}

	@Test
	public void testMixedTokens () {
		List<Token> tokens = new Tokenizer().tokenize(new Source("test", "{{ identifier }} and a different segment {{identifier2}} fin."));
		assertEquals("Expected one template token", 4, tokens.size());
		assertEquals("identifier", tokens.get(0).getText());
		assertEquals(" and a different segment ", tokens.get(1).getText());
		assertEquals("identifier2", tokens.get(2).getText());
		assertEquals(" fin.", tokens.get(3).getText());
	}

	@Test
	public void testMissingClosingTag () {
		try {
			new Tokenizer().tokenize(new Source("test", "{{ this lacks a closing tag"));
			fail("Missing closing tag not detected");
		} catch (RuntimeException t) {
			// expected.
		}
	}

	@Test
	public void testTokenizer () {
		List<Token> tokens = new Tokenizer().tokenize(new Source("test",
			"{{ . + - * / ( ) [ ] < > <= >= == = && || ! 1 123 123. 123.432 \"this is a string literal with a \\\" quote \" _id var_234 $id , ; 1b 1s 1l 1f 1.0d 'c' '\\n'}}"));
		assertEquals("Expected 35 tokens", 35, tokens.size());
		Assert.assertEquals(TokenType.Period, tokens.get(0).getType());
		assertEquals(TokenType.Plus, tokens.get(1).getType());
		assertEquals(TokenType.Minus, tokens.get(2).getType());
		assertEquals(TokenType.Asterisk, tokens.get(3).getType());
		assertEquals(TokenType.ForwardSlash, tokens.get(4).getType());
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
		assertEquals(TokenType.IntegerLiteral, tokens.get(18).getType());
		assertEquals("1", tokens.get(18).getText());
		assertEquals(TokenType.IntegerLiteral, tokens.get(19).getType());
		assertEquals("123", tokens.get(19).getText());
		assertEquals(TokenType.FloatLiteral, tokens.get(20).getType());
		assertEquals("123.", tokens.get(20).getText());
		assertEquals(TokenType.FloatLiteral, tokens.get(21).getType());
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
		assertEquals(TokenType.ByteLiteral, tokens.get(28).getType());
		assertEquals(TokenType.ShortLiteral, tokens.get(29).getType());
		assertEquals(TokenType.LongLiteral, tokens.get(30).getType());
		assertEquals(TokenType.FloatLiteral, tokens.get(31).getType());
		assertEquals(TokenType.DoubleLiteral, tokens.get(32).getType());
	}
}
