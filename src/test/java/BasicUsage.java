import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateContext;
import tech.gospel.basis.template.TemplateLoader;
import tech.gospel.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class BasicUsage {
	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/helloworld.bt");
		TemplateContext context = new TemplateContext();
		context.set("name", "Hotzenplotz");
		System.out.println(template.render(context));
	}
}
