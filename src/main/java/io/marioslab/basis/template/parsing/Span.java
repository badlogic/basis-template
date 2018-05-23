
package io.marioslab.basis.template.parsing;

/** A span within a source string denoted by start and end index, with the latter being exclusive. */
public class Span {
	/** the source string this span refers to **/
	public final String source;

	/** start index in source string, starting at 0 **/
	public int start;

	/** end index in source string, exclusive, starting at 0 **/
	public int end;

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

	/** @return the text referenced by this span **/
	public String getText () {
		return source.substring(start, end);
	}
}
