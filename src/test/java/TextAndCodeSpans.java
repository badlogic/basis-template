import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader;
import io.marioslab.basis.template.TemplateLoader.ClasspathTemplateLoader;

public class TextAndCodeSpans {

	public static class License {
		public final String productName;
		public final String[] activationCodes;

		public License (String productName, String[] activationCodes) {
			this.productName = productName;
			this.activationCodes = activationCodes;
		}
	}

	public static void main (String[] args) {
		TemplateLoader loader = new ClasspathTemplateLoader();
		Template template = loader.load("/textandcodespans.bt");
		TemplateContext context = new TemplateContext();
		context.set("customer", "Mr. Hotzenplotz");
		context.set("license", new License("Hotzenplotz", new String[] {"3ba34234bcffe", "5bbe77f879000", "dd3ee54324bf3"}));
		System.out.println(template.render(context));
	}
}
