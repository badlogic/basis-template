package io.marioslab.basis.template.parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * Segments the source into segments enclosed in "{{" and "}}" and segments not enclosed in these tags.
 */
public class TemplateSegmentParser {
	/** Segments the source into tokens that are enclosed in "{{" "}}" and tokens which are not enclosed. */
	public List<TemplateSegment> parse (String source) {
		List<TemplateSegment> tokens = new ArrayList<TemplateSegment>();
		if (source.length() == 0) return tokens;
		CharacterStream stream = new CharacterStream(source);
		stream.startSpan();

		while (stream.hasMore()) {
			if (stream.match("{{", false)) {
				if (!stream.isSpanEmpty()) tokens.add(new TemplateSegment(stream.endSpan()));
				stream.startSpan();
				while (!stream.match("}}", true)) {
					if (!stream.hasMore()) error("Did not find closing }}.", stream.endSpan());
					stream.next();
				}
				tokens.add(new TemplateSegment(stream.endSpan()));
				stream.startSpan();
			} else {
				stream.next();
			}
		}
		if (!stream.isSpanEmpty()) tokens.add(new TemplateSegment(stream.endSpan()));
		return tokens;
	}

	private void error (String message, Span location) {
		// TODO generate line numbers and nice error message.
		throw new RuntimeException("Error: " + message);
	}

	/** A template part is either a segment starting with "{{" and ending with "}}", or an arbitrary sequence of characters. */
	public static class TemplateSegment {
		private final Span span;

		public TemplateSegment (Span span) {
			this.span = span;
		}

		public Span getSpan () {
			return span;
		}

		public String getText () {
			return span.getText();
		}
	}
}
