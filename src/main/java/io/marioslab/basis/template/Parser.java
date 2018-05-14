
package io.marioslab.basis.template;

import java.util.ArrayList;
import java.util.List;

public class Parser {

	/** A span within a source string denoted by start and end index, with the latter being exclusive. */
	public static class Span {
		/** the source string this span refers to **/
		public final String source;

		/** start index in source string, starting at 0 **/
		public int start;

		/** end index in source string, exclusive, starting at 0 **/
		public int end;

		public Span (String source) {
			this.source = source;
		}

		/** @return the text referenced by this span **/
		public String getText () {
			return source.substring(start, end);
		}
	}

	/** Wraps a source string and handles traversing the contained characters. Manages a current span via the */
	public static class CharacterStream {
		private Span span;
		private final String source;
		private int index = 0;

		public CharacterStream (String source) {
			this.source = source;
		}

		/** @return whether there are more characters in the stream **/
		public boolean hasMore () {
			return index < source.length();
		}

		/** @return the next character and advance the stream **/
		public char next () {
			if (!hasMore()) throw new RuntimeException("No more characters in stream.");
			return source.charAt(index++);
		}

		/** Matches the given needle with the next characters. Returns true if the needle is matched, false otherwise. If there's a
		 * match and consume is true, the stream is advanced by the needle's length. */
		public boolean match (String needle, boolean consume) {
			int sourceLength = source.length();
			int needleLength = needle.length();
			for (int i = 0, j = index; i < needleLength; i++, j++) {
				if (index >= sourceLength) return false;
				if (needle.charAt(i) != source.charAt(j)) return false;
			}
			if (consume) index += needleLength;
			return true;
		}

		public void startSpan () {
			span = new Span(source);
			span.start = index;
		}

		public Span endSpan () {
			Span span = this.span;
			span.end = this.index;
			this.span = null;
			return span;
		}

		public boolean isSpanEmpty () {
			return span.start == this.index;
		}
	}

	/** A token is either a segment starting with "{{" and ending with "}}", or an arbitrary sequence of characters. */
	public static class Token {
		private final Span span;

		public Token (Span span) {
			this.span = span;
		}

		public Span getSpan () {
			return span;
		}

		public String getText () {
			return span.getText();
		}
	}

	/** Tokenizes the source into tokens that are enclosed in "{{" "}}" and tokens which are not enclosed. */
	public List<Token> tokenize (String source) {
		List<Token> tokens = new ArrayList<Token>();
		if (source.length() == 0) return tokens;
		CharacterStream stream = new CharacterStream(source);
		stream.startSpan();

		while (stream.hasMore()) {
			if (stream.match("{{", false)) {
				if (!stream.isSpanEmpty()) tokens.add(new Token(stream.endSpan()));
				stream.startSpan();
				while (!stream.match("}}", true)) {
					if (!stream.hasMore()) error("Did not find closing }}.", stream.endSpan());
					stream.next();
				}
				tokens.add(new Token(stream.endSpan()));
				stream.startSpan();
			} else {
				stream.next();
			}
		}
		if (!stream.isSpanEmpty()) tokens.add(new Token(stream.endSpan()));
		return tokens;
	}

	private void error (String message, Span location) {
		// TODO generate line numbers and nice error message.
		throw new RuntimeException("Error: " + message);
	}
}
