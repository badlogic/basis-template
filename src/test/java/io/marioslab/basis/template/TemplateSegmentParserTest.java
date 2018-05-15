
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import io.marioslab.basis.template.parsing.TemplateSegmentParser;
import io.marioslab.basis.template.parsing.TemplateSegmentParser.TemplateSegment;

public class TemplateSegmentParserTest {
	@Test
	public void testEmptysegment () {
		List<TemplateSegment> segments = new TemplateSegmentParser().parse("");
		assertEquals("Segments are not empty", 0, segments.size());
	}

	@Test
	public void testNoTemplateTemplateSegment () {
		List<TemplateSegment> segment = new TemplateSegmentParser().parse("this is a test");
		assertEquals("Expected one non-template token", 1, segment.size());
		assertEquals("Expected one non-template token", "this is a test", segment.get(0).getText());
	}

	@Test
	public void testOnlyTemplateTemplateSegment () {
		List<TemplateSegment> segment = new TemplateSegmentParser().parse("{{ this is a test }}");
		assertEquals("Expected one template token", 1, segment.size());
		assertEquals("Expected one template token", "{{ this is a test }}", segment.get(0).getText());
	}

	@Test
	public void testMixedToknes () {
		List<TemplateSegment> segment = new TemplateSegmentParser()
			.parse("{{ this is a test }} and a different segment {{and another test}} fin.");
		assertEquals("Expected one template token", 4, segment.size());
		assertEquals("{{ this is a test }}", segment.get(0).getText());
		assertEquals(" and a different segment ", segment.get(1).getText());
		assertEquals("{{and another test}}", segment.get(2).getText());
		assertEquals(" fin.", segment.get(3).getText());
	}

	@Test
	public void testMissingClosingTag () {
		try {
			new TemplateSegmentParser().parse("{{ this lacks a closing tag");
			fail("Missing closing tag not detected");
		} catch (RuntimeException t) {
			// expected.
		}
	}
}
