
package io.marioslab.basis.template.parsing;

/** Wraps a source string and handles traversing the contained characters. Manages a current {@link Span} via the
 * {@link #startSpan()} and {@link #endSpan()} methods. */
public class CharacterStream {
	private Span span;
	private final String source;
	private int index = 0;
	private final int end;

	public CharacterStream (String source) {
		this(source, 0, source.length());
	}

	public CharacterStream (String source, int start, int end) {
		if (start > end) throw new IllegalArgumentException("Start must be <= end.");
		if (start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start > source.length() - 1) throw new IndexOutOfBoundsException("Start outside of string.");
		if (end > source.length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = source;
		this.index = start;
		this.end = end;
	}

	/** @return whether there are more characters in the stream **/
	public boolean hasMore () {
		return index < end;
	}

	/** @return the next character and advance the stream **/
	public char next () {
		if (!hasMore()) throw new RuntimeException("No more characters in stream.");
		return source.charAt(index++);
	}

	/** Matches the given needle with the next characters. Returns true if the needle is matched, false otherwise. If there's a
	 * match and consume is true, the stream is advanced by the needle's length. */
	public boolean match (String needle, boolean consume) {
		int needleLength = needle.length();
		for (int i = 0, j = index; i < needleLength; i++, j++) {
			if (index >= end) return false;
			if (needle.charAt(i) != source.charAt(j)) return false;
		}
		if (consume) index += needleLength;
		return true;
	}

	public boolean matchDigit (boolean consume) {
		if (index >= end) return false;
		char c = source.charAt(index);
		if (Character.isDigit(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public boolean matchIdentifierStart (boolean consume) {
		if (index >= end) return false;
		char c = source.charAt(index);
		if (Character.isJavaIdentifierStart(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public boolean matchIdentifierPart (boolean consume) {
		if (index >= end) return false;
		char c = source.charAt(index);
		if (Character.isJavaIdentifierPart(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public void skipWhiteSpace () {
		while (true) {
			if (index >= end) return;
			char c = source.charAt(index);
			if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
				index++;
				continue;
			} else {
				break;
			}
		}
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

	public int getIndex () {
		return index;
	}
}
