
package io.marioslab.basis.template;

import io.marioslab.basis.template.TemplateLoader.Source;
import io.marioslab.basis.template.parsing.Span;
import io.marioslab.basis.template.parsing.TokenStream;
import io.marioslab.basis.template.parsing.Span.Line;

/** All errors reported by the library go through the static functions of this class. */
public class Error {

	/**
	 * <p>
	 * Create an error message based on the provided message and stream, highlighting the line on which the error happened. If the
	 * stream has more tokens, the next token will be highlighted. Otherwise the end of the source of the stream will be
	 * highlighted.
	 * </p>
	 *
	 * <p>
	 * Throws a {@link RuntimeException}
	 * </p>
	 */
	public static void error (String message, TokenStream stream) {
		if (stream.hasMore())
			error(message, stream.consume().getSpan());
		else {
			Source source = stream.getSource();
			if (source == null)
				error(message, new Span(new Source("unknown", " "), 0, 1));
			else
				error(message, new Span(source, source.getContent().length() - 1, source.getContent().length()));
		}
	}

	/**
	 * <p>
	 * Create an error message based on the provided message and location, highlighting the location in the line on which the error
	 * happened.
	 * </p>
	 * *
	 * <p>
	 * Throws a {@link RuntimeException}
	 * </p>
	 **/
	public static void error (String message, Span location) {

		Line line = location.getLine();
		message = "Error (" + location.getSource().getPath() + ":" + line.getLineNumber() + "): " + message + "\n\n";
		message += line.getText();
		message += "\n";

		int errorStart = location.getStart() - line.getStart();
		int errorEnd = errorStart + location.getText().length() - 1;
		for (int i = 0, n = line.getText().length(); i < n; i++) {
			boolean useTab = line.getText().charAt(i) == '\t';
			message += i >= errorStart && i <= errorEnd ? "^" : useTab ? "\t" : " ";
		}

		throw new RuntimeException(message);
	}
}
