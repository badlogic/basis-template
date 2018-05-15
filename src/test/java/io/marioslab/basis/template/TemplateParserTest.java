package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.marioslab.basis.template.parsing.TemplateParser;
import io.marioslab.basis.template.parsing.TemplateParser.Node;
import io.marioslab.basis.template.parsing.TemplateParser.TextNode;

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
	public void testExpressionNode () {
		List<Node> nodes = new TemplateParser().parse("{{ 1 }}").getNodes();
		assertEquals("Expected one node", 1, nodes.size());
	}
}
