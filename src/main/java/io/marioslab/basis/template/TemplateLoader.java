
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Parser.ParserResult;
import io.marioslab.basis.template.parsing.Span;

public interface TemplateLoader {
	public Template load (String path);

	public static class StreamUtils {
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

	public abstract class CachingTemplateLoader implements TemplateLoader {
		Map<String, Template> templates = new ConcurrentHashMap<String, Template>();

		@Override
		public Template load (String path) {
			if (templates.containsKey(path)) return templates.get(path);
			try {
				String source = loadSource(path);
				Template template = compileTemplate(source);
				templates.put(path, template);
				return template;
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}

		protected Template compileTemplate (String source) {
			ParserResult result = new Parser().parse(source);

			// resolve includes and macros
			for (Include include : result.getIncludes()) {
				String includePath = include.getPath().getText();
				Template template = load(includePath.substring(1, includePath.length() - 1));
				include.setTemplate(template);
			}

			return new Template(result.getNodes(), result.getMacros(), result.getIncludes());
		}

		protected abstract String loadSource (String path);
	}

	public static class ResourceTemplateLoader extends CachingTemplateLoader {
		@Override
		protected String loadSource (String path) {
			try {
				return StreamUtils.readString(TemplateLoader.class.getResourceAsStream(path));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	public static class FileTemplateLoader extends CachingTemplateLoader {
		private final File baseDirectory;

		public FileTemplateLoader (File baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		@Override
		protected String loadSource (String path) {
			try {
				return StreamUtils.readString(new FileInputStream(new File(baseDirectory, path)));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	public static class MapTemplateLoader extends CachingTemplateLoader {
		private final Map<String, String> templates = new HashMap<String, String>();

		public MapTemplateLoader set (String path, String template) {
			super.templates.remove(path);
			templates.put(path, template);
			return this;
		}

		@Override
		protected String loadSource (String path) {
			String template = templates.get(path);
			if (template == null) throw new RuntimeException("Couldn't load template '" + path + "'.");
			return template;
		}
	}
}
