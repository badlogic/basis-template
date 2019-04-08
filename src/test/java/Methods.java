import java.util.Iterator;
import java.util.function.BiFunction;

import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateContext;
import tech.gospel.basis.template.TemplateLoader;
import tech.gospel.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class Methods {
	public static class MyObject {
		private int add (int a, int b) {
			return a + b;
		}

		protected float add (float a, float b) {
			return a + b;
		}

		public static String staticMethod () {
			return "Hello";
		}
	}

	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/methods.bt");
		TemplateContext context = new TemplateContext();
		context.set("myObject", new MyObject());
		context.set("myClass", MyObject.class);

		context.set("range", (BiFunction<Integer, Integer, Iterator<Integer>>) (from, to) -> {
			return new Iterator<Integer>() {
				int idx = from;
				public boolean hasNext () { return idx <= to; }
				public Integer next () { return idx++; }
			};
		});

		System.out.println(template.render(context));
	}
}
