
package io.marioslab.basis.template.parsing;

/** A span within a source string denoted by start and end index, with the latter being exclusive. */
public class Span {
	/** the source string this span refers to **/
	private final String source;

	/** start index in source string, starting at 0 **/
	private int start;

	/** end index in source string, exclusive, starting at 0 **/
	private int end;

	public Span (String source) {
		this(source, 0, source.length());
	}

	public Span (String source, int start, int end) {
		if (start > end) throw new IllegalArgumentException("Start must be <= end.");
		if (start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start > source.length() - 1) throw new IndexOutOfBoundsException("Start outside of string.");
		if (end > source.length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = source;
		this.start = start;
		this.end = end;
	}

	public Span (Span start, Span end) {
		if (!start.source.equals(end.source)) throw new IllegalArgumentException("The two spans do not reference the same source.");
		if (start.start > end.end) throw new IllegalArgumentException("Start must be <= end.");
		if (start.start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start.start > start.source.length() - 1) throw new IndexOutOfBoundsException("Start outside of string.");
		if (end.end > start.source.length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = start.source;
		this.start = start.start;
		this.end = end.end;
	}

	/** @return the text referenced by this span **/
	public String getText () {
		return source.substring(start, end);
	}

	/** @return the index of the first character of this span. **/
	public int getStart () {
		return start;
	}

	/** @return the index of the last character of this span plus 1. **/
	public int getEnd () {
		return end;
	}

	/** @return the source string this span references. **/
	public String getSource () {
		return source;
	}

	@Override
	public String toString () {
		return "Span [text=" + getText() + ", start=" + start + ", end=" + end + "]";
	}

}
