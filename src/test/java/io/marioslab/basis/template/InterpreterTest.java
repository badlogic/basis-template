
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class InterpreterTest {
	class MyObject {
		int field1;
		int field2;
		private int field3;
		private String text = "Test";

		public int getField2 () {
			return field2;
		}

		@Override
		public String toString () {
			return "My Object";
		}
	}

	@Test
	public void testLiterals () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello", "Hello {{null}}, {{true}}, {{1234}}, {{12.34}}, {{\"world\"}}");
		Template template = Template.load("hello", loader);
		String result = template.render(new TemplateContext());
		assertEquals("Hello , true, 1234, 12.34, world", result);
	}

	@Test
	public void testVariableAccess () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello", "{{boolean}}, {{integer}}, {{float}}, {{string}}, {{object}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext().set("boolean", false).set("integer", 12345).set("float", 123.45).set("string", "hello").set("object",
			new MyObject());
		String result = template.render(context);
		assertEquals("false, 12345, 123.45, hello, My Object", result);
	}

	@Test
	public void testArrayAccess () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello",
			"{{boolean[0]}}, {{char[0]}}, {{short[0]}}, {{int[0]}}, {{long[0]}}, {{float[0]}}, {{double[0]}}, {{string[0]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		context.set("boolean", new boolean[] {true});
		context.set("char", new char[] {'a'});
		context.set("short", new short[] {(short)123});
		context.set("int", new int[] {456});
		context.set("long", new long[] {789});
		context.set("float", new float[] {1.23f});
		context.set("double", new double[] {4.56});
		context.set("string", new String[] {"hello"});
		String result = template.render(context);
		assertEquals("true, a, 123, 456, 789, 1.23, 4.56, hello", result);
	}

	@Test
	public void testMultiArrayAccess () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello", "{{multi[0][0]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		context.set("multi", new String[][] {new String[] {"hello"}});
		String result = template.render(context);
		assertEquals("hello", result);
	}

	@Test
	public void testListAccess () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello", "{{list[0]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		List<String> list = new ArrayList<String>();
		list.add("hello");
		context.set("list", list);
		String result = template.render(context);
		assertEquals("hello", result);
	}

	@Test
	public void testMapAccess () {
		TemplateLoader loader = new TemplateLoader.MapTemplateLoader().set("hello", "{{map[\"key\"]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		Map<String, String> map = new HashMap<String, String>();
		map.put("key", "hello");
		context.set("map", map);
		String result = template.render(context);
		assertEquals("hello", result);
	}
}
