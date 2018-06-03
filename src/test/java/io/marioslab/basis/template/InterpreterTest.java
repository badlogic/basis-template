
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.Test;

import io.marioslab.basis.template.TemplateLoader.MapTemplateLoader;

public class InterpreterTest {
	class OtherObject {
		float a = 123.456f;
	}

	class MyObject {
		int field1 = 123;
		int field2 = 456;
		private int field3 = 789;
		private OtherObject other = new OtherObject();
		private String text = "Test";
		private IntFunction<Integer> func = (IntFunction<Integer>)Math::abs;

		public int getField2 () {
			return field2;
		}

		public int add (int a, int b) {
			return a + b;
		}

		public String add (String a, String b) {
			return a + b;
		}

		@Override
		public String toString () {
			return "My Object";
		}
	}

	@Test
	public void testLiterals () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "Hello {{null}}, {{true}}, {{1234}}, {{12.34}}, {{\"world\"}}");
		Template template = Template.load("hello", loader);
		String result = template.render(new TemplateContext());
		assertEquals("Hello , true, 1234, 12.34, world", result);
	}

	@Test
	public void testVariableAccess () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "{{boolean}}, {{integer}}, {{float}}, {{string}}, {{object}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext().set("boolean", false).set("integer", 12345).set("float", 123.45).set("string", "hello").set("object",
			new MyObject());
		String result = template.render(context);
		assertEquals("false, 12345, 123.45, hello, My Object", result);
	}

	@Test
	public void testArrayAccess () {
		MapTemplateLoader loader = new MapTemplateLoader().set("hello",
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
		TemplateLoader loader = new MapTemplateLoader().set("hello", "{{multi[0][0]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		context.set("multi", new String[][] {new String[] {"hello"}});
		String result = template.render(context);
		assertEquals("hello", result);
	}

	@Test
	public void testListAccess () {
		TemplateLoader loader = new MapTemplateLoader().set("hello", "{{list[0]}}");
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
		MapTemplateLoader loader = new MapTemplateLoader();

		loader.set("hello", "{{map[\"key\"]}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		Map<String, String> map = new HashMap<String, String>();
		map.put("key", "hello");
		context.set("map", map);
		String result = template.render(context);
		assertEquals("hello", result);
	}

	@Test
	public void testMemberAccess () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.field1 object.field2 object.field3 object.other.a object.text}}");
		Template template = Template.load("hello", loader);
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("123456789123.456Test", result);
	}

	@Test
	public void testMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.getField2()}}");
		Template template = Template.load("hello", loader);
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("456", result);
	}

	@Test
	public void testOverloadedMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.add(1, 2)}}");
		Template template = Template.load("hello", loader);
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("3", result);

		loader.set("hello2", "{{object.add(\"Hello \", \"world\")}}");
		template = Template.load("hello2", loader);
		result = template.render(context);
		assertEquals("Hello world", result);
	}

	@Test
	public void testStaticMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();

		loader.set("hello", "{{Math.abs(123) \" \" Math.abs(1.23)}}");
		Template template = Template.load("hello", loader);
		TemplateContext context = new TemplateContext();
		context.set("Math", Math.class);
		String result = template.render(context);
		assertEquals("123 1.23", result);
	}

	@Test
	public void testFunctionCall () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{abs(123)}}");
		Template template = Template.load("hello", loader);
		context.set("abs", (IntFunction)Math::abs);
		String result = template.render(context);
		assertEquals("123", result);

		loader.set("hello2", "{{array[0](123)}}");
		template = Template.load("hello2", loader);
		context.set("array", new IntFunction[] {Math::abs});
		result = template.render(context);
		assertEquals("123", result);

		loader.set("hello3", "{{object.func(123)}}");
		template = Template.load("hello3", loader);
		context.set("object", new MyObject());
		result = template.render(context);
		assertEquals("123", result);
	}

	@Test
	public void testUnaryOperators () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{+(1)}}");
		Template template = Template.load("hello", loader);
		String result = template.render(context);
		assertEquals("1", result);

		loader.set("hello", "{{-(1)}}");
		template = Template.load("hello", loader);
		result = template.render(context);
		assertEquals("-1", result);

		loader.set("hello", "{{!true}}");
		template = Template.load("hello", loader);
		result = template.render(context);
		assertEquals("false", result);
	}
}
