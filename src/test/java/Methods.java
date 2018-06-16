import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader;
import io.marioslab.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class Methods {
	public static class MyObject {
		private int add (int a, int b) { return a + b; }
		protected float add (float a, float b) { return a + b; }
		public static String staticMethod () { return "Hello"; }
	}

	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/methods.bt");
		TemplateContext context = new TemplateContext();
		context.set("myObject", new MyObject());
		context.set("myClass", MyObject.class);
		System.out.println(template.render(context));
	}
}
