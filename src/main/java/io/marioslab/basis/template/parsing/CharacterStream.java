
package io.marioslab.basis.template.parsing;

import io.marioslab.basis.template.TemplateLoader.Source;

/** Wraps a source string and handles traversing the contained characters. Manages a current {@link Span} via the
 * {@link #startSpan()} and {@link #endSpan()} methods. */
public class CharacterStream {
	private final Source source;
	private int index = 0;
	private final int end;

	private int spanStart = 0;

	public CharacterStream (Source source) {
		this(source, 0, source.getContent().length());
	}

	public CharacterStream (Source source, int start, int end) {
		if (start > end) throw new IllegalArgumentException("Start must be <= end.");
		if (start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start > source.getContent().length() - 1) throw new IndexOutOfBoundsException("Start outside of string.");
		if (end > source.getContent().length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = source;
		this.index = start;
		this.end = end;
	}

	/** @return whether there are more characters in the stream **/
	public boolean hasMore () {
		return index < end;
	}

	/** @return the next character without advancing the stream **/
	public char peek () {
		if (!hasMore()) throw new RuntimeException("No more characters in stream.");
		return source.getContent().charAt(index++);
	}

	/** @return the next character and advance the stream **/
	public char consume () {
		if (!hasMore()) throw new RuntimeException("No more characters in stream.");
		return source.getContent().charAt(index++);
	}

	/** Matches the given needle with the next characters. Returns true if the needle is matched, false otherwise. If there's a
	 * match and consume is true, the stream is advanced by the needle's length. */
	public boolean match (String needle, boolean consume) {
		int needleLength = needle.length();
		for (int i = 0, j = index; i < needleLength; i++, j++) {
			if (index >= end) return false;
			if (needle.charAt(i) != source.getContent().charAt(j)) return false;
		}
		if (consume) index += needleLength;
		return true;
	}

	public boolean matchDigit (boolean consume) {
		if (index >= end) return false;
		char c = source.getContent().charAt(index);
		if (Character.isDigit(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public boolean matchIdentifierStart (boolean consume) {
		if (index >= end) return false;
		char c = source.getContent().charAt(index);
		if (Character.isJavaIdentifierStart(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public boolean matchIdentifierPart (boolean consume) {
		if (index >= end) return false;
		char c = source.getContent().charAt(index);
		if (Character.isJavaIdentifierPart(c)) {
			if (consume) index++;
			return true;
		}
		return false;
	}

	public void skipWhiteSpace () {
		while (true) {
			if (index >= end) return;
			char c = source.getContent().charAt(index);
			if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
				index++;
				continue;
			} else {
				break;
			}
		}
	}

	public void startSpan () {
		spanStart = index;
	}

	public Span endSpan () {
		return new Span(source, spanStart, index);
	}

	public boolean isSpanEmpty () {
		return spanStart == this.index;
	}

	public int getIndex () {
		return index;
	}
}
