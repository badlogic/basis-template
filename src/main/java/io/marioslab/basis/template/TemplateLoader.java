
package io.marioslab.basis.template;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public interface TemplateLoader {
	public String load (String path);

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

	public static class ResourceTemplateLoader implements TemplateLoader {
		@Override
		public String load (String path) {
			try {
				return StreamUtils.readString(TemplateLoader.class.getResourceAsStream(path));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	public static class FileTemplateLoader implements TemplateLoader {
		private final File baseDirectory;

		public FileTemplateLoader (File baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		@Override
		public String load (String path) {
			try {
				return StreamUtils.readString(new FileInputStream(new File(baseDirectory, path)));
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't load template '" + path + "'.", t);
			}
		}
	}

	public static class MapTemplateLoader implements TemplateLoader {
		private final Map<String, String> templates = new HashMap<String, String>();

		public MapTemplateLoader set (String path, String template) {
			templates.put(path, template);
			return this;
		}

		@Override
		public String load (String path) {
			String template = templates.get(path);
			if (template == null) throw new RuntimeException("Couldn't load template '" + path + "'.");
			return template;
		}
	}
}
