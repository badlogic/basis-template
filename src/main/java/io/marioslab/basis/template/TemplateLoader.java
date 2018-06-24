
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.IncludeRaw;
import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.ParserResult;

/** A template loader loads a {@link Template} from a path, and recursively loads other templates the template may reference. See
 * {@link CachingTemplateLoader}, {@link ClasspathTemplateLoader}, {@link FileTemplateLoader} and {@link MapTemplateLoader} for
 * specific implementations. */
public interface TemplateLoader {

	/** Loads the template from the given path, recursively resolving other templates included or referenced by this template.
	 * Throws a {@link RuntimeException} if a template could not be loaded, or if the template could not be parsed due to a syntax
	 * error. In the later case, the message of the exception contains a description of the syntax error and its location. */
	public Template load (String path);

	/** A source stores a template's content and the path it was loaded from **/
	public static class Source {
		private final String path;
		private final String content;

		public Source (String path, String content) {
			this.path = path;
			this.content = content;
		}

		/** Returns the path the template was loaded from. **/
		public String getPath () {
			return path;
		}

		/** The content of the template. **/
		public String getContent () {
			return content;
		}

		@Override
		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((content == null) ? 0 : content.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		@Override
		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Source other = (Source)obj;
			if (content == null) {
				if (other.content != null) return false;
			} else if (!content.equals(other.content)) return false;
			if (path == null) {
				if (other.path != null) return false;
			} else if (!path.equals(other.path)) return false;
			return true;
		}
	}

	/** Base class for other {@link TemplateLoader} implementations that caches templates and recursively loads other templates
	 * referenced by a template via an include statement. */
	public abstract class CachingTemplateLoader implements TemplateLoader {
		Map<String, Template> templates = new ConcurrentHashMap<String, Template>();

		@Override
		public Template load (String path) {
			if (templates.containsKey(path)) return templates.get(path);
			try {
				Source source = loadSource(path);
				Template template = compileTemplate(source);
				templates.put(path, template);
				return template;
			} catch (Throwable t) {
				if (t instanceof RuntimeException)
					throw t;
				else
					throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}

		protected Template compileTemplate (Source source) {
			ParserResult result = new Parser().parse(source);

			// resolve includes and macros
			for (Include include : result.getIncludes()) {
				String includePath = include.getPath().getText();
				Template template = load(includePath.substring(1, includePath.length() - 1));
				include.setTemplate(template);
			}

			for (IncludeRaw rawInclude : result.getRawIncludes()) {
				String includePath = rawInclude.getPath().getText();
				Source content = loadSource(includePath.substring(1, includePath.length() - 1));
				try {
					rawInclude.setContent(content.content.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException("Couldn't load raw include " + includePath, e);
				}
			}

			return new Template(result.getNodes(), result.getMacros(), result.getIncludes());
		}

		protected abstract Source loadSource (String path);
	}

	/** A TemplateLoader to load templates from the classpath. **/
	public static class ClasspathTemplateLoader extends CachingTemplateLoader {
		@Override
		protected Source loadSource (String path) {
			try {
				return new Source(path, StreamUtils.readString(TemplateLoader.class.getResourceAsStream(path)));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	/** A TemplateLoader to load templates from a directory. **/
	public static class FileTemplateLoader extends CachingTemplateLoader {
		private final File baseDirectory;

		/** Construct the loader with the base directory. All paths passed to {@link #load(String)} are assumed to be relative to
		 * that directory. **/
		public FileTemplateLoader (File baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		@Override
		protected Source loadSource (String path) {
			try {
				return new Source(path, StreamUtils.readString(new FileInputStream(new File(baseDirectory, path))));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	/** A TemplateLoader to load templates from memory. Call {@link #set(String, String)} to specify a templates source and
	 * path. **/
	public static class MapTemplateLoader extends CachingTemplateLoader {
		private final Map<String, Source> templates = new HashMap<String, Source>();

		/** Set the path and content of a template to be loaded with a call to {@link #load(String)}. **/
		public MapTemplateLoader set (String path, String template) {
			super.templates.remove(path);
			templates.put(path, new Source(path, template));
			return this;
		}

		@Override
		protected Source loadSource (String path) {
			Source template = templates.get(path);
			if (template == null) throw new RuntimeException("Couldn't load template '" + path + "'.");
			return template;
		}
	}

	static class StreamUtils {
		private static String readString (InputStream in) throws IOException {
			byte[] buffer = new byte[1024 * 10];
			int read = 0;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			return new String(out.toByteArray(), "UTF-8");
		}
	}
}
