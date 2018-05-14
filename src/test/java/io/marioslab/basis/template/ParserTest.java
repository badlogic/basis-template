package io.marioslab.basis.template;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

public class ParserTest {
	@Test
	public void testEmptyTokens () {
		List<Parser.Token> tokens = new Parser().tokenize("");
		assertEquals("Tokens are not empty", 0, tokens.size());
	}

	@Test
	public void testNoTemplateToken () {
		List<Parser.Token> tokens = new Parser().tokenize("this is a test");
		assertEquals("Expected one non-template token", 1, tokens.size());
		assertEquals("Expected one non-template token", "this is a test", tokens.get(0).getText());
	}

	@Test
	public void testOnlyTemplateToken () {
		List<Parser.Token> tokens = new Parser().tokenize("{{ this is a test }}");
		assertEquals("Expected one template token", 1, tokens.size());
		assertEquals("Expected one template token", "{{ this is a test }}", tokens.get(0).getText());
	}

	@Test
	public void testMixedToknes () {
		List<Parser.Token> tokens = new Parser().tokenize("{{ this is a test }} and a different segment {{and another test}} fin.");
		assertEquals("Expected one template token", 4, tokens.size());
		assertEquals("{{ this is a test }}", tokens.get(0).getText());
		assertEquals(" and a different segment ", tokens.get(1).getText());
		assertEquals("{{and another test}}", tokens.get(2).getText());
		assertEquals(" fin.", tokens.get(3).getText());
	}

	@Test
	public void testMissingClosingTag () {
		try {
			new Parser().tokenize("{{ this lacks a closing tag");
			fail("Missing closing tag not detected");
		} catch (RuntimeException t) {
			// expected.
		}
	}
}
