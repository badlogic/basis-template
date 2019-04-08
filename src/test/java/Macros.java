import tech.gospel.basis.template.Template;
import tech.gospel.basis.template.TemplateContext;
import tech.gospel.basis.template.TemplateLoader;
import tech.gospel.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class Macros {
	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/macros.bt");
		TemplateContext context = new TemplateContext();
		System.out.println(template.render(context));
	}
}
