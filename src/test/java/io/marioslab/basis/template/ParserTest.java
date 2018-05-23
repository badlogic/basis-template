
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.TextNode;

public class ParserTest {
	@Test
	public void testEmptySource () {
		Template template = new Parser().parse("");
		assertEquals("Empty source produced non-empty template", 0, template.getNodes().size());
	}

	@Test
	public void testTextNodeOnly () {
		Template template = new Parser().parse("This is a text node");
		assertEquals("Expected a single text node", 1, template.getNodes().size());
		assertEquals("Expected a single text node", TextNode.class, template.getNodes().get(0).getClass());
		assertEquals("This is a text node", template.getNodes().get(0).getSpan().getText());
	}

	@Test
	public void testEmptyNode () {
		assertEquals("Expected 0 nodes", 0, new Parser().parse("{{ }}").getNodes().size());
	}
}
