
package io.marioslab.basis.template;

import io.marioslab.basis.template.parsing.Span;
import io.marioslab.basis.template.parsing.TokenStream;
import io.marioslab.basis.template.parsing.Span.Line;

public class Error {
	public static void error (String message, TokenStream stream) {
		if (stream.hasMore())
			error(message, stream.consume().getSpan());
		else {
			String source = stream.getSource();
			if (source == null)
				error(message, new Span(" ", 0, 1));
			else
				error(message, new Span(source, source.length() - 1, source.length()));
		}
	}

	public static void error (String message, Span location) {

		Line line = location.getLine();
		message = "Error (line:" + line.getLineNumber() + "): " + message + "\n\n";
		message += line.getText();
		message += "\n";

		int errorStart = location.getStart() - line.getStart();
		int errorEnd = errorStart + location.getText().length() - 1;
		for (int i = 0, n = line.getText().length(); i < n; i++) {
			message += i >= errorStart && i <= errorEnd ? "^" : " ";
		}

		throw new RuntimeException(message);
	}
}
