
package tech.gospel.basis.template.parsing;

import tech.gospel.basis.template.TemplateLoader.Source;

/** A span within a source string denoted by start and end index, with the latter being exclusive. */
public class Span {
	/** the source string this span refers to **/
	private final Source source;

	/** start index in source string, starting at 0 **/
	private int start;

	/** end index in source string, exclusive, starting at 0 **/
	private int end;

	/** Cached String instance to reduce pressure on GC **/
	private final String cachedText;

	public Span (Source source) {
		this(source, 0, source.getContent().length());
	}

	public Span (Source source, int start, int end) {
		if (start > end) throw new IllegalArgumentException("Start must be <= end.");
		if (start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start > source.getContent().length() - 1) 
			throw new IndexOutOfBoundsException("Start outside of string.");
		if (end > source.getContent().length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = source;
		this.start = start;
		this.end = end;
		this.cachedText = source.getContent().substring(start, end);
	}

	public Span (Span start, Span end) {
		if (!start.source.equals(end.source)) throw new IllegalArgumentException("The two spans do not reference the same source.");
		if (start.start > end.end) throw new IllegalArgumentException("Start must be <= end.");
		if (start.start < 0) throw new IndexOutOfBoundsException("Start must be >= 0.");
		if (start.start > start.source.getContent().length() - 1) throw new IndexOutOfBoundsException("Start outside of string.");
		if (end.end > start.source.getContent().length()) throw new IndexOutOfBoundsException("End outside of string.");

		this.source = start.source;
		this.start = start.start;
		this.end = end.end;
		this.cachedText = source.getContent().substring(this.start, this.end);
	}

	/** Returns the text referenced by this span **/
	public String getText () {
		return cachedText;
	}

	/** Returns the index of the first character of this span. **/
	public int getStart () {
		return start;
	}

	/** Returns the index of the last character of this span plus 1. **/
	public int getEnd () {
		return end;
	}

	/** Returns the source string this span references. **/
	public Source getSource () {
		return source;
	}

	@Override
	public String toString () {
		return "Span [text=" + getText() + ", start=" + start + ", end=" + end + "]";
	}

	/** Returns the line this span is on. Does not return a correct result for spans across multiple lines. **/
	public Line getLine () {
		int lineStart = start;
		while (true) {
			if (lineStart < 0) break;
			char c = source.getContent().charAt(lineStart);
			if (c == '\n') {
				lineStart = lineStart + 1;
				break;
			}
			lineStart--;
		}
		if (lineStart < 0) lineStart = 0;

		int lineEnd = end;
		while (true) {
			if (lineEnd > source.getContent().length() - 1) break;
			char c = source.getContent().charAt(lineEnd);
			if (c == '\n') {
				break;
			}
			lineEnd++;
		}

		int lineNumber = 0;
		int idx = lineStart;
		while (idx > 0) {
			char c = source.getContent().charAt(idx);
			if (c == '\n') {
				lineNumber++;
			}
			idx--;
		}
		lineNumber++;

		return new Line(source, lineStart, lineEnd, lineNumber);
	}

	/** A line within a Source **/
	public static class Line {
		private final Source source;
		private final int start;
		private final int end;
		private final int lineNumber;

		public Line (Source source, int start, int end, int lineNumber) {
			this.source = source;
			this.start = start;
			this.end = end;
			this.lineNumber = lineNumber;
		}

		public Source getSource () {
			return source;
		}

		public int getStart () {
			return start;
		}

		public int getEnd () {
			return end;
		}

		public int getLineNumber () {
			return lineNumber;
		}

		public String getText () {
			return source.getContent().substring(start, end);
		}
	}
}
