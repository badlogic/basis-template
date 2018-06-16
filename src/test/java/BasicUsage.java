import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader;
import io.marioslab.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class BasicUsage {
	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/helloworld.bt");
		TemplateContext context = new TemplateContext();
		context.set("name", "Hotzenplotz");
		System.out.println(template.render(context));
	}
}
