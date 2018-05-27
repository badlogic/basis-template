
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.MemberAccess;
import io.marioslab.basis.template.parsing.Ast.FunctionCall;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.MethodCall;
import io.marioslab.basis.template.parsing.Ast.NumberLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.TextNode;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;
import io.marioslab.basis.template.parsing.Parser;

public class ParserTest {
	@Test
	public void testEmptySource () {
		Template template = new Parser().parse("");
		assertEquals("Empty source produced non-empty template", 0, template.getNodes().size());
	}

	@Test
	public void testTextNodeOnly () {
		Template template = new Parser().parse("This is a text node");
		assertEquals("Expected a single text node", 1, template.getNodes().size());
		assertEquals("Expected a single text node", TextNode.class, template.getNodes().get(0).getClass());
		assertEquals("This is a text node", template.getNodes().get(0).getSpan().getText());
	}

	@Test
	public void testEmptyNode () {
		assertEquals("Expected 0 nodes", 0, new Parser().parse("{{ }}").getNodes().size());
	}

	@Test
	public void testBooleanLiteralNode () {
		Template template = new Parser().parse("{{true}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected BooleanLiteral node", BooleanLiteral.class, template.getNodes().get(0).getClass());
		BooleanLiteral literal = (BooleanLiteral)template.getNodes().get(0);
		assertEquals("Expected true literal", true, literal.getValue());
	}

	@Test
	public void testNumberLiteralNode () {
		Template template = new Parser().parse("{{123.456}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", NumberLiteral.class, template.getNodes().get(0).getClass());
		NumberLiteral literal = (NumberLiteral)template.getNodes().get(0);
		assertEquals("Expected 123.56 literal", 123.456, literal.getValue(), 0);
	}

	@Test
	public void testStringLiteralNode () {
		Template template = new Parser().parse("{{\"this is a test\"}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected StringLiteral node", StringLiteral.class, template.getNodes().get(0).getClass());
		StringLiteral literal = (StringLiteral)template.getNodes().get(0);
		assertEquals("Expected \"this is a test\" literal", "\"this is a test\"", literal.getRawValue());
	}

	@Test
	public void testVariableAccess () {
		Template template = new Parser().parse("{{a}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected VariableAccess node", VariableAccess.class, template.getNodes().get(0).getClass());
		assertEquals("Expected variable name to be a", "a", ((VariableAccess)template.getNodes().get(0)).getVariableName().getText());
	}

	@Test
	public void testFunctionCall () {
		Template template = new Parser().parse("{{foo()}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected FunctionCall node", FunctionCall.class, template.getNodes().get(0).getClass());
		FunctionCall call = (FunctionCall)template.getNodes().get(0);
		assertEquals("Expected function name to be foo", "foo", ((VariableAccess)call.getFunction()).getVariableName().getText());
		assertEquals("Expected 0 arguments", 0, call.getArguments().size());
	}

	@Test
	public void testFunctionCallWithArguments () {
		Template template = new Parser().parse("{{foo(a, b, c)}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected FunctionCall node", FunctionCall.class, template.getNodes().get(0).getClass());
		FunctionCall call = (FunctionCall)template.getNodes().get(0);
		assertEquals("Expected function name to be foo", "foo", ((VariableAccess)call.getFunction()).getVariableName().getText());
		assertEquals("Expected 3 arguments", 3, call.getArguments().size());
	}

	@Test
	public void testMapAccess () {
		Template template = new Parser().parse("{{foo[1]}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected MapOrArrayAccess node", MapOrArrayAccess.class, template.getNodes().get(0).getClass());
		MapOrArrayAccess mapAccess = (MapOrArrayAccess)template.getNodes().get(0);
		assertEquals(VariableAccess.class, mapAccess.getMapOrArray().getClass());
		VariableAccess map = (VariableAccess)mapAccess.getMapOrArray();
		assertEquals("foo", map.getVariableName().getText());
		NumberLiteral key = (NumberLiteral)mapAccess.getKeyOrIndex();
		assertEquals(1, key.getValue(), 0);
	}

	@Test
	public void testMemberAccess () {
		Template template = new Parser().parse("{{foo.field}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected MemberAccess node", MemberAccess.class, template.getNodes().get(0).getClass());
		MemberAccess fieldAccess = (MemberAccess)template.getNodes().get(0);
		assertEquals(VariableAccess.class, fieldAccess.getObject().getClass());
		VariableAccess object = (VariableAccess)fieldAccess.getObject();
		assertEquals("foo", object.getVariableName().getText());
		assertEquals("field", fieldAccess.getName().getText());
	}

	@Test
	public void testMethodCall () {
		Template template = new Parser().parse("{{foo.method()}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected MethodCall node", MethodCall.class, template.getNodes().get(0).getClass());
		MethodCall methodCall = (MethodCall)template.getNodes().get(0);
		assertEquals(VariableAccess.class, methodCall.getObject().getClass());
		VariableAccess object = (VariableAccess)methodCall.getObject();
		assertEquals("foo", object.getVariableName().getText());
		assertEquals("method", methodCall.getMethod().getName().getText());
		assertEquals(0, methodCall.getArguments().size());
	}

	@Test
	public void testMethodCallWithArguments () {
		Template template = new Parser().parse("{{foo.method(a, b, c)}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected MethodCall node", MethodCall.class, template.getNodes().get(0).getClass());
		MethodCall methodCall = (MethodCall)template.getNodes().get(0);
		assertEquals(VariableAccess.class, methodCall.getObject().getClass());
		VariableAccess object = (VariableAccess)methodCall.getObject();
		assertEquals("foo", object.getVariableName().getText());
		assertEquals("method", methodCall.getMethod().getName().getText());
		assertEquals(3, methodCall.getArguments().size());
	}

	@Test
	public void testChainAccess () {
		Template template = new Parser().parse("{{foo.bar[0]()[0]().method(a, b, c).field}}");
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected  node", MemberAccess.class, template.getNodes().get(0).getClass());
		MemberAccess fieldAccess = (MemberAccess)template.getNodes().get(0);
		MethodCall methodCall = (MethodCall)fieldAccess.getObject();
		FunctionCall functionCall = (FunctionCall)methodCall.getObject();
		MapOrArrayAccess arrayAccess = (MapOrArrayAccess)functionCall.getFunction();
		FunctionCall functionCall2 = (FunctionCall)arrayAccess.getMapOrArray();
		MapOrArrayAccess arrayAccess2 = (MapOrArrayAccess)functionCall2.getFunction();
		MemberAccess fieldAccess2 = (MemberAccess)arrayAccess2.getMapOrArray();
		VariableAccess variableAccess = (VariableAccess)fieldAccess2.getObject();
	}
}
