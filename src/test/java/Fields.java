import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateContext;
import tech.gospel.basis.template.TemplateLoader;
import tech.gospel.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class Fields {
	public static class MyObject {
		public static String STATIC_FIELD = "I'm static";
		private int privateField = 123;
		boolean packagePrivateField = true;
		protected float protectedField = 123.456f;
		public String publicField = "ello";
	}

	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/fields.bt");
		TemplateContext context = new TemplateContext();
		context.set("myObject", new MyObject());
		context.set("myClass", MyObject.class);
		System.out.println(template.render(context));
	}
}
