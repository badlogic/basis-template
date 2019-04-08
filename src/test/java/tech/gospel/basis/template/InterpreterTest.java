
package tech.gospel.basis.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import org.junit.Ignore;
import org.junit.Test;

import tech.gospel.basis.template.TemplateLoader.MapTemplateLoader;

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
		private IntFunction<Integer> func = v -> v + 1;

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
		loader.set("hello",
			"Hello {{null}}, {{true}}, {{1234}}, {{12.34}}, {{123b}}, {{123s}}, {{123l}}, {{123f}}, {{123d}}, {{123.0d}}, {{'a'}}, {{'\\n'}}, {{\"world\"}}, {{\"\\\"\\n\\r\\t\\\\\"}}");

		Template template = loader.load("hello");
		String result = template.render(new TemplateContext());

		assertEquals("Hello , true, 1234, 12.34, 123, 123, 123, 123.0, 123.0, 123.0, a, \n, world, \"\n\r\t\\", result);
	}

	@Test
	public void testMapLiteral () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "{{ map = { test: \"123\", test2: true, test3: { test4: 123 } } }}");
		Template template = loader.load("hello");
		TemplateContext context = new TemplateContext();
		template.render(context);
		Map<String, Object> map = (Map<String, Object>)context.get("map");
		assertEquals(3, map.size());
		assertEquals("123", map.get("test"));
		assertEquals(true, map.get("test2"));
		assertTrue(map.get("test3") instanceof Map);
		assertEquals(123, ((Map<String, Object>)map.get("test3")).get("test4"));
	}

	@Test
	public void testMapMemberAccess () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "{{ map = { test: \"123\", test2: true, test3: { test4: 123 } }; value = map.test3.test4 }}");
		Template template = loader.load("hello");
		TemplateContext context = new TemplateContext();
		template.render(context);
		assertEquals(123, context.get("value"));
	}

	@Test
	public void testListLiteral () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "{{ list = [ 123, true, null, \"test\", [ 1234 ]] }}");
		Template template = loader.load("hello");
		TemplateContext context = new TemplateContext();
		template.render(context);
		List<Object> list = (List<Object>)context.get("list");
		assertEquals(5, list.size());
		assertEquals(123, list.get(0));
		assertEquals(true, list.get(1));
		assertEquals(null, list.get(2));
		assertEquals("test", list.get(3));
		assertTrue(list.get(4) instanceof List);
		assertEquals(1234, ((List<Object>)list.get(4)).get(0));
	}

	@Test
	public void testVariableAccess () {
		MapTemplateLoader loader = new MapTemplateLoader();
		loader.set("hello", "{{boolean}}, {{integer}}, {{float}}, {{string}}, {{object}}");
		Template template = loader.load("hello");
		TemplateContext context = new TemplateContext().set("boolean", false).set("integer", 12345).set("float", 123.45).set("string", "hello").set("object",
			new MyObject());
		String result = template.render(context);
		assertEquals("false, 12345, 123.45, hello, My Object", result);
	}

	@Test
	public void testArrayAccess () {
		MapTemplateLoader loader = new MapTemplateLoader().set("hello",
			"{{boolean[0]}}, {{char[0]}}, {{short[0]}}, {{int[0]}}, {{long[0]}}, {{float[0]}}, {{double[0]}}, {{string[0]}}");
		Template template = loader.load("hello");
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
		Template template = loader.load("hello");
		TemplateContext context = new TemplateContext();
		context.set("multi", new String[][] {new String[] {"hello"}});
		String result = template.render(context);
		assertEquals("hello", result);
	}

	@Test
	public void testListAccess () {
		TemplateLoader loader = new MapTemplateLoader().set("hello", "{{list[0]}}");
		Template template = loader.load("hello");
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
		Template template = loader.load("hello");
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
		Template template = loader.load("hello");
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("123456789123.456Test", result);
	}

	@Test
	public void testMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.getField2()}}");
		Template template = loader.load("hello");
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("456", result);
	}

	@Test
	public void testOverloadedMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.add(1, 2)}}");
		Template template = loader.load("hello");
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("3", result);

		loader.set("hello2", "{{object.add(\"Hello \", \"world\")}}");
		template = loader.load("hello2");
		result = template.render(context);
		assertEquals("Hello world", result);
	}

	@Test
	public void testParameterCoercian () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{object.add(1b, 2b)}}");
		Template template = loader.load("hello");
		context.set("object", new MyObject());
		String result = template.render(context);
		assertEquals("3", result);
	}

	@Test
	public void testArrayIndexCoercion () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{array[1b]}}");
		Template template = loader.load("hello");
		context.set("array", new int[] {1, 3, 4});
		String result = template.render(context);
		assertEquals("3", result);
	}

	@Test
	public void testStaticMethodCall () {
		MapTemplateLoader loader = new MapTemplateLoader();

		loader.set("hello", "{{Math.abs(123) \" \" Math.abs(1.23f)}}");
		Template template = loader.load("hello");
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
		Template template = loader.load("hello");
		context.set("abs", (IntFunction<Integer>)Math::abs);
		String result = template.render(context);
		assertEquals("123", result);

		loader.set("hello2", "{{array[0](123)}}");
		template = loader.load("hello2");
		context.set("array", new IntFunction[] {Math::abs, Math::signum});
		result = template.render(context);
		assertEquals("123", result);

		loader.set("hello3", "{{object.func(123)}}");
		template = loader.load("hello3");
		context.set("object", new MyObject());
		result = template.render(context);
		assertEquals("124", result);
	}

	@Test
	public void testUnaryOperators () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{+(1)}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("1", result);

		loader.set("hello", "{{-(1)}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("-1", result);

		loader.set("hello", "{{!true}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("false", result);
	}

	@Test
	public void testAddition () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{1 + 1}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("2", result);

		loader.set("hello", "{{1.0 + 1}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("2.0", result);

		loader.set("hello", "{{1l + 1b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("2", result);

		loader.set("hello", "{{\"hello\" + 1}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("hello1", result);

		loader.set("hello", "{{\"hello\" + \" world\"}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("hello world", result);
	}

	@Test
	public void testSubtraction () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{1 - 1}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("0", result);

		loader.set("hello", "{{1.0 - 1}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("0.0", result);

		loader.set("hello", "{{1l - 1b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("0", result);
	}

	@Test
	public void testMultiplication () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 * 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("6", result);

		loader.set("hello", "{{3.0 * 2}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("6.0", result);

		loader.set("hello", "{{2l * 3b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("6", result);
	}

	@Test
	public void testDivision () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 / 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + 2 / 3, result);

		loader.set("hello", "{{3.0 / 2}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + 3.0 / 2, result);

		loader.set("hello", "{{2l / 3b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + 2l / (byte)3, result);
	}

	@Test
	public void testModulo () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 % 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + 2 % 3, result);

		loader.set("hello", "{{3.0 % 2}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + 3.0 % 2, result);

		loader.set("hello", "{{2l % 3b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + 2l % (byte)3, result);
	}

	@Test
	public void testLess () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 < 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 < 3), result);

		loader.set("hello", "{{3.0f < 3}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3.0f < 3), result);

		loader.set("hello", "{{2l < 3b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (2l < (byte)3), result);
	}

	@Test
	public void testLessEqual () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 <= 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 < 3), result);

		loader.set("hello", "{{3.0f <= 3}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3.0f <= 3), result);

		loader.set("hello", "{{2l <= 3b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (2l <= (byte)3), result);
	}

	@Test
	public void testGreater () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 > 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 > 3), result);

		loader.set("hello", "{{4.0f > 3}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (4.0f > 3), result);

		loader.set("hello", "{{3l > 2b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3l > (byte)2), result);
	}

	@Test
	public void testGreaterEqual () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 >= 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 >= 3), result);

		loader.set("hello", "{{4.0f >= 3}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (4.0f >= 3), result);

		loader.set("hello", "{{3l >= 2b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3l >= (byte)2), result);
	}

	@Test
	public void testEqual () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 == 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 == 3), result);

		loader.set("hello", "{{3.0f == 3.0}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3.0f == 3.0), result);

		loader.set("hello", "{{2l == 2b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (2l == (byte)3), result);

		loader.set("hello", "{{\"hello\" == \"hello\"}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + ("hello".equals("hello")), result);
	}

	@Test
	public void testNotEqual () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{2 != 3}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (2 != 3), result);

		loader.set("hello", "{{3.0f != 3.0}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (3.0f != 3.0), result);

		loader.set("hello", "{{2l != 2b}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (2l != (byte)3), result);

		loader.set("hello", "{{\"hello\" != \"hello\"}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + !("hello".equals("hello")), result);
	}

	@Test
	public void testAnd () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{true && false}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (true && false), result);

		loader.set("hello", "{{true && true}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (true && true), result);
	}

	@Test
	public void testOr () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{true || false}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (true || false), result);

		loader.set("hello", "{{false || true}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (false || true), result);
	}

	@Test
	public void testXor () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{true ^ false}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (true ^ false), result);
	}

	@Test
	public void testAssignment () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{a = 123}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("", result);
		assertEquals(123, context.get("a"));
	}

	@Test
	public void testTernaryOperator () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{true ? 1 : 2}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("" + (true ? 1 : 2), result);

		loader.set("hello", "{{false ? 1 : 2}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("" + (false ? 1 : 2), result);
	}

	@Test
	public void testFor () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{ for v in map }}value: {{v}}\n{{end}}");
		Template template = loader.load("hello");
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("a", 1);
		map.put("b", 2);
		map.put("c", 3);
		context.set("map", map);
		String result = template.render(context);
		assertEquals("value: 1\nvalue: 2\nvalue: 3\n", result);

		loader.set("hello", "{{ for k, v in map }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("a: 1\nb: 2\nc: 3\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new boolean[] {true, false, true});
		result = template.render(context);
		assertEquals("true\nfalse\ntrue\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new boolean[] {true, false, true});
		result = template.render(context);
		assertEquals("0: true\n1: false\n2: true\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new char[] {'x', 'y', 'z'});
		result = template.render(context);
		assertEquals("x\ny\nz\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new char[] {'x', 'y', 'z'});
		result = template.render(context);
		assertEquals("0: x\n1: y\n2: z\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new byte[] {10, 11, 12});
		result = template.render(context);
		assertEquals("10\n11\n12\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new byte[] {10, 11, 12});
		result = template.render(context);
		assertEquals("0: 10\n1: 11\n2: 12\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new short[] {10, 11, 12});
		result = template.render(context);
		assertEquals("10\n11\n12\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new short[] {10, 11, 12});
		result = template.render(context);
		assertEquals("0: 10\n1: 11\n2: 12\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new int[] {10, 11, 12});
		result = template.render(context);
		assertEquals("10\n11\n12\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new int[] {10, 11, 12});
		result = template.render(context);
		assertEquals("0: 10\n1: 11\n2: 12\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new long[] {10, 11, 12});
		result = template.render(context);
		assertEquals("10\n11\n12\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new long[] {10, 11, 12});
		result = template.render(context);
		assertEquals("0: 10\n1: 11\n2: 12\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new float[] {1.2f, 2.3f, 3.4f});
		result = template.render(context);
		assertEquals("1.2\n2.3\n3.4\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new float[] {1.2f, 2.3f, 3.4f});
		result = template.render(context);
		assertEquals("0: 1.2\n1: 2.3\n2: 3.4\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new double[] {1.2, 2.3, 3.4});
		result = template.render(context);
		assertEquals("1.2\n2.3\n3.4\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new String[] {"aa", "bb", "cc"});
		result = template.render(context);
		assertEquals("aa\nbb\ncc\n", result);

		loader.set("hello", "{{ for k, v in array }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("array", new String[] {"aa", "bb", "cc"});
		result = template.render(context);
		assertEquals("0: aa\n1: bb\n2: cc\n", result);

		loader.set("hello", "{{ for v in array }}{{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("list", Arrays.asList("aa", "bb", "cc"));
		result = template.render(context);
		assertEquals("aa\nbb\ncc\n", result);

		loader.set("hello", "{{ for k, v in list }}{{k}}: {{v}}\n{{end}}");
		template = loader.load("hello");
		context.set("list", Arrays.asList("aa", "bb", "cc"));
		result = template.render(context);
		assertEquals("0: aa\n1: bb\n2: cc\n", result);
	}

	@Test
	public void testIf () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{if true}}test{{end}}");
		Template template = loader.load("hello");
		Map<String, Integer> map = new HashMap<String, Integer>();
		String result = template.render(context);
		assertEquals("test", result);

		loader.set("hello", "{{if false}}test{{else}}test2{{end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("test2", result);

		loader.set("hello", "{{if false}}test{{elseif false}}test2{{elseif true}}test3{{else}}test4{{end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("test3", result);

		loader.set("hello", "{{if false}}test{{elseif false}}test2{{elseif false}}test3{{else}}test4{{end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("test4", result);

		loader.set("hello", "{{if false}}test{{elseif false}}test2{{elseif true}}{{if false}}test3{{else}}test4{{end}}{{else}}test5{{end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("test4", result);
	}

	@Test(expected = Error.TemplateException.class)
	public void testWhile () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{while (iter.hasNext()) iter.next() end}}");
		Template template = loader.load("hello");
		context.set("iter", Arrays.asList("aa", "bb", "cc").iterator());
		String result = template.render(context);
		assertEquals("aabbcc", result);

		loader.set("hello", "{{ i = 10; while (i >= 0) i = i - 1; i; end}}");
		template = loader.load("hello");
		result = template.render(context);
		assertEquals("9876543210-1", result);
	}

	@Test
	public void testContinue () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{ for num in array if num == 2 continue else num end}}\n{{end}}");
		Template template = loader.load("hello");
		context.set("array", new int[] {0, 1, 2, 3, 4});
		String result = template.render(context);
		assertEquals("0\n1\n3\n4\n", result);
	}

	@Test
	public void testBreak () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{ for num in array if num == 2 break else num end}}\n{{end}}");
		Template template = loader.load("hello");
		context.set("array", new int[] {0, 1, 2, 3, 4});
		String result = template.render(context);
		assertEquals("0\n1\n", result);
	}

	@Test(expected = Error.TemplateException.class)
	public void testInclude () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{include \"hello2\"}}");
		loader.set("hello2", "{{i}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("0123456789", result);
	}

	@Test
	public void testMacros () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{macro helloWorld(num, text) num \":\" toUpper(text) end macro toUpper(text) text.toUpperCase() end helloWorld(1, \"test\")}}");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("1:TEST", result);
	}

	@Test
	public void testReturn () {
		MapTemplateLoader loader = new MapTemplateLoader();
		TemplateContext context = new TemplateContext();

		loader.set("hello", "{{ return; }} un-reachable");
		Template template = loader.load("hello");
		String result = template.render(context);
		assertEquals("", result);

		loader.set("hello", "{{ return 123; }} un-reachable");
		template = loader.load("hello");
		Object retVal = template.evaluate(context);
		assertEquals(123, retVal);

		loader.set("hello", "{{ macro test () return 123 end return test() }} un-reachable");
		template = loader.load("hello");
		retVal = template.evaluate(context);
		assertEquals(123, retVal);
	}
}
