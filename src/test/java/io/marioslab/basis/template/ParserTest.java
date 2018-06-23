
package io.marioslab.basis.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import io.marioslab.basis.template.TemplateLoader.Source;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation.BinaryOperator;
import io.marioslab.basis.template.parsing.Ast.BooleanLiteral;
import io.marioslab.basis.template.parsing.Ast.ByteLiteral;
import io.marioslab.basis.template.parsing.Ast.CharacterLiteral;
import io.marioslab.basis.template.parsing.Ast.DoubleLiteral;
import io.marioslab.basis.template.parsing.Ast.Expression;
import io.marioslab.basis.template.parsing.Ast.FloatLiteral;
import io.marioslab.basis.template.parsing.Ast.ForStatement;
import io.marioslab.basis.template.parsing.Ast.FunctionCall;
import io.marioslab.basis.template.parsing.Ast.IfStatement;
import io.marioslab.basis.template.parsing.Ast.Include;
import io.marioslab.basis.template.parsing.Ast.IntegerLiteral;
import io.marioslab.basis.template.parsing.Ast.LongLiteral;
import io.marioslab.basis.template.parsing.Ast.Macro;
import io.marioslab.basis.template.parsing.Ast.MapOrArrayAccess;
import io.marioslab.basis.template.parsing.Ast.MemberAccess;
import io.marioslab.basis.template.parsing.Ast.MethodCall;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.ShortLiteral;
import io.marioslab.basis.template.parsing.Ast.StringLiteral;
import io.marioslab.basis.template.parsing.Ast.TernaryOperation;
import io.marioslab.basis.template.parsing.Ast.Text;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation;
import io.marioslab.basis.template.parsing.Ast.UnaryOperation.UnaryOperator;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;
import io.marioslab.basis.template.parsing.Ast.WhileStatement;
import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.Macros;
import io.marioslab.basis.template.parsing.Parser.ParserResult;
import io.marioslab.basis.template.parsing.Span;

public class ParserTest {
	@Test
	public void testEmptySource () {
		ParserResult template = new Parser().parse(new Source("test", ""));
		assertEquals("Empty source produced non-empty template", 0, template.getNodes().size());
	}

	@Test
	public void testTextNodeOnly () {
		ParserResult template = new Parser().parse(new Source("test", "This is a text node"));
		assertEquals("Expected a single text node", 1, template.getNodes().size());
		assertEquals("Expected a single text node", Text.class, template.getNodes().get(0).getClass());
		assertEquals("This is a text node", template.getNodes().get(0).getSpan().getText());
	}

	@Test
	public void testEmptyNode () {
		assertEquals("Expected 0 nodes", 0, new Parser().parse(new Source("test", "{{ }}")).getNodes().size());
	}

	@Test
	public void testEscapedCurly () {
		assertEquals("{{ }}", new String(((Text)new Parser().parse(new Source("test", "\\{\\{ \\}\\}")).getNodes().get(0)).getBytes()));
	}

	@Test
	public void testBooleanLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{true}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected BooleanLiteral node", BooleanLiteral.class, template.getNodes().get(0).getClass());
		BooleanLiteral literal = (BooleanLiteral)template.getNodes().get(0);
		assertEquals("Expected true literal", true, literal.getValue());
	}

	@Test
	public void testFloatLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123.0}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", FloatLiteral.class, template.getNodes().get(0).getClass());
		FloatLiteral literal = (FloatLiteral)template.getNodes().get(0);
		assertEquals("Expected 123.0 literal", 123.0, literal.getValue(), 0);
	}

	@Test
	public void testDoubleLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123.0d}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected DoubleLiteral node", DoubleLiteral.class, template.getNodes().get(0).getClass());
		DoubleLiteral literal = (DoubleLiteral)template.getNodes().get(0);
		assertEquals("Expected 123.0 literal", 123.0, literal.getValue(), 0);
	}

	@Test
	public void testByteLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123b}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", ByteLiteral.class, template.getNodes().get(0).getClass());
		ByteLiteral literal = (ByteLiteral)template.getNodes().get(0);
		assertEquals("Expected 123 literal", 123, literal.getValue(), 0);
	}

	@Test
	public void testShortLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123s}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", ShortLiteral.class, template.getNodes().get(0).getClass());
		ShortLiteral literal = (ShortLiteral)template.getNodes().get(0);
		assertEquals("Expected 123 literal", 123, literal.getValue(), 0);
	}

	@Test
	public void testIntegerLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123456}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", IntegerLiteral.class, template.getNodes().get(0).getClass());
		IntegerLiteral literal = (IntegerLiteral)template.getNodes().get(0);
		assertEquals("Expected 123456 literal", 123456, literal.getValue(), 0);
	}

	@Test
	public void testCharacterLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{'a' '\\n'}}"));
		assertEquals("Expected 2 node", 2, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", CharacterLiteral.class, template.getNodes().get(0).getClass());
		CharacterLiteral literal = (CharacterLiteral)template.getNodes().get(0);
		assertEquals("Expected a literal", 'a', literal.getValue(), 0);
		literal = (CharacterLiteral)template.getNodes().get(1);
		assertEquals("Expected a literal", '\n', literal.getValue(), 0);
	}

	@Test
	public void testLongLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{123456l}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected NumberLiteral node", LongLiteral.class, template.getNodes().get(0).getClass());
		LongLiteral literal = (LongLiteral)template.getNodes().get(0);
		assertEquals("Expected 123456 literal", 123456l, literal.getValue(), 0);
	}

	@Test
	public void testStringLiteralNode () {
		ParserResult template = new Parser().parse(new Source("test", "{{\"this is a test\"}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected StringLiteral node", StringLiteral.class, template.getNodes().get(0).getClass());
		StringLiteral literal = (StringLiteral)template.getNodes().get(0);
		assertEquals("Expected \"this is a test\" literal", "this is a test", literal.getValue());
	}

	@Test
	public void testVariableAccess () {
		ParserResult template = new Parser().parse(new Source("test", "{{a}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected VariableAccess node", VariableAccess.class, template.getNodes().get(0).getClass());
		assertEquals("Expected variable name to be a", "a", ((VariableAccess)template.getNodes().get(0)).getVariableName().getText());
	}

	@Test
	public void testFunctionCall () {
		ParserResult template = new Parser().parse(new Source("test", "{{foo()}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected FunctionCall node", FunctionCall.class, template.getNodes().get(0).getClass());
		FunctionCall call = (FunctionCall)template.getNodes().get(0);
		assertEquals("Expected function name to be foo", "foo", ((VariableAccess)call.getFunction()).getVariableName().getText());
		assertEquals("Expected 0 arguments", 0, call.getArguments().size());
	}

	@Test
	public void testFunctionCallWithArguments () {
		ParserResult template = new Parser().parse(new Source("test", "{{foo(a, b, c)}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected FunctionCall node", FunctionCall.class, template.getNodes().get(0).getClass());
		FunctionCall call = (FunctionCall)template.getNodes().get(0);
		assertEquals("Expected function name to be foo", "foo", ((VariableAccess)call.getFunction()).getVariableName().getText());
		assertEquals("Expected 3 arguments", 3, call.getArguments().size());
	}

	@Test
	public void testMapAccess () {
		ParserResult template = new Parser().parse(new Source("test", "{{foo[1]}}"));
		assertEquals("Expected 1 node", 1, template.getNodes().size());
		assertEquals("Expected MapOrArrayAccess node", MapOrArrayAccess.class, template.getNodes().get(0).getClass());
		MapOrArrayAccess mapAccess = (MapOrArrayAccess)template.getNodes().get(0);
		assertEquals(VariableAccess.class, mapAccess.getMapOrArray().getClass());
		VariableAccess map = (VariableAccess)mapAccess.getMapOrArray();
		assertEquals("foo", map.getVariableName().getText());
		IntegerLiteral key = (IntegerLiteral)mapAccess.getKeyOrIndex();
		assertEquals(1, key.getValue(), 0);
	}

	@Test
	public void testMemberAccess () {
		ParserResult template = new Parser().parse(new Source("test", "{{foo.field}}"));
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
		ParserResult template = new Parser().parse(new Source("test", "{{foo.method()}}"));
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
		ParserResult template = new Parser().parse(new Source("test", "{{foo.method(a, b, c)}}"));
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
		ParserResult template = new Parser().parse(new Source("test", "{{foo.bar[0]()[0]().method(a, b, c).field}}"));
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
		assertEquals("foo", variableAccess.getVariableName().getText());
	}

	@Test
	public void testTernaryOperator () {
		ParserResult template = new Parser().parse(new Source("test", "{{ \"hello\" ? 123 : true }}"));
		List<Node> nodes = template.getNodes();
		assertEquals(1, nodes.size());
		assertEquals(TernaryOperation.class, nodes.get(0).getClass());
		TernaryOperation ternary = (TernaryOperation)nodes.get(0);
		assertEquals(StringLiteral.class, ternary.getCondition().getClass());
		assertEquals(IntegerLiteral.class, ternary.getTrueExpression().getClass());
		assertEquals(BooleanLiteral.class, ternary.getFalseExpression().getClass());
	}

	@Test
	public void testBinaryMathOperators () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ 1 + 2 * 3 / 4 % 5 }}")).getNodes();
		BinaryOperation add = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Addition, add.getOperator());
		assertEquals(IntegerLiteral.class, add.getLeftOperand().getClass());
		BinaryOperation mod = (BinaryOperation)add.getRightOperand();
		assertEquals(BinaryOperator.Modulo, mod.getOperator());
		assertEquals(IntegerLiteral.class, mod.getRightOperand().getClass());
		BinaryOperation div = (BinaryOperation)mod.getLeftOperand();
		assertEquals(BinaryOperator.Division, div.getOperator());
		BinaryOperation mul = (BinaryOperation)div.getLeftOperand();
		assertEquals(BinaryOperator.Multiplication, mul.getOperator());
		assertEquals(IntegerLiteral.class, mul.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, mul.getRightOperand().getClass());
	}

	@Test
	public void testComparisonOperators () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ 1 < 2 }}")).getNodes();
		BinaryOperation less = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Less, less.getOperator());
		assertEquals(IntegerLiteral.class, less.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, less.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ 1 > 2 }}")).getNodes();
		BinaryOperation greater = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Greater, greater.getOperator());
		assertEquals(IntegerLiteral.class, greater.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, greater.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ 1 >= 2 }}")).getNodes();
		BinaryOperation greaterEqual = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.GreaterEqual, greaterEqual.getOperator());
		assertEquals(IntegerLiteral.class, greaterEqual.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, greaterEqual.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ 1 <= 2 }}")).getNodes();
		BinaryOperation lessEqual = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.LessEqual, lessEqual.getOperator());
		assertEquals(IntegerLiteral.class, lessEqual.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, lessEqual.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ 1 == 2 }}")).getNodes();
		BinaryOperation equal = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Equal, equal.getOperator());
		assertEquals(IntegerLiteral.class, equal.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, equal.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ 1 != 2 }}")).getNodes();
		BinaryOperation notEqual = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.NotEqual, notEqual.getOperator());
		assertEquals(IntegerLiteral.class, notEqual.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, notEqual.getRightOperand().getClass());
	}

	@Test
	public void testLogicalOperators () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ true && false }}")).getNodes();
		BinaryOperation and = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.And, and.getOperator());
		assertEquals(BooleanLiteral.class, and.getLeftOperand().getClass());
		assertEquals(BooleanLiteral.class, and.getRightOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ true || false }}")).getNodes();
		BinaryOperation or = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Or, or.getOperator());
		assertEquals(BooleanLiteral.class, or.getLeftOperand().getClass());
		assertEquals(BooleanLiteral.class, or.getRightOperand().getClass());
	}

	@Test
	public void testUnaryOperators () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ !true }}")).getNodes();
		UnaryOperation and = (UnaryOperation)nodes.get(0);
		assertEquals(UnaryOperator.Not, and.getOperator());
		assertEquals(BooleanLiteral.class, and.getOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ -1 }}")).getNodes();
		UnaryOperation negate = (UnaryOperation)nodes.get(0);
		assertEquals(UnaryOperator.Negate, negate.getOperator());
		assertEquals(IntegerLiteral.class, negate.getOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{ +1 }}")).getNodes();
		UnaryOperation positive = (UnaryOperation)nodes.get(0);
		assertEquals(UnaryOperator.Positive, positive.getOperator());
		assertEquals(IntegerLiteral.class, negate.getOperand().getClass());

		nodes = new Parser().parse(new Source("test", "{{  1 + -2 }}")).getNodes();
		BinaryOperation add = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Addition, add.getOperator());
		assertEquals(IntegerLiteral.class, add.getLeftOperand().getClass());
		negate = (UnaryOperation)add.getRightOperand();
		assertEquals(UnaryOperator.Negate, negate.getOperator());
		assertEquals(IntegerLiteral.class, negate.getOperand().getClass());
	}

	@Test
	public void testAssignment () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ a = 123 }}")).getNodes();
		BinaryOperation and = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Assignment, and.getOperator());
		assertEquals(VariableAccess.class, and.getLeftOperand().getClass());
		assertEquals(IntegerLiteral.class, and.getRightOperand().getClass());
	}

	@Test
	public void testOperatorPrecedence () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ a = 2 + 3 * -4 < !(true || 2 >= foo.bar[0](a, \"test\", 123, true)) }}")).getNodes();
		BinaryOperation assignment = (BinaryOperation)nodes.get(0);
		assertEquals(BinaryOperator.Assignment, assignment.getOperator());
		BinaryOperation less = (BinaryOperation)assignment.getRightOperand();
		assertEquals(BinaryOperator.Less, less.getOperator());
		BinaryOperation add = (BinaryOperation)less.getLeftOperand();
		assertEquals(BinaryOperator.Addition, add.getOperator());
		assertEquals(IntegerLiteral.class, add.getLeftOperand().getClass());
		BinaryOperation mul = (BinaryOperation)add.getRightOperand();
		assertEquals(BinaryOperator.Multiplication, mul.getOperator());
		assertEquals(IntegerLiteral.class, mul.getLeftOperand().getClass());
		UnaryOperation negate = (UnaryOperation)mul.getRightOperand();
		assertEquals(UnaryOperator.Negate, negate.getOperator());
		assertEquals(IntegerLiteral.class, negate.getOperand().getClass());

		UnaryOperation not = (UnaryOperation)less.getRightOperand();
		assertEquals(UnaryOperator.Not, not.getOperator());
		BinaryOperation or = (BinaryOperation)not.getOperand();
		assertEquals(BooleanLiteral.class, or.getLeftOperand().getClass());
		BinaryOperation greaterEqual = (BinaryOperation)or.getRightOperand();
		assertEquals(BinaryOperator.GreaterEqual, greaterEqual.getOperator());
		assertEquals(IntegerLiteral.class, greaterEqual.getLeftOperand().getClass());

		FunctionCall call = (FunctionCall)greaterEqual.getRightOperand();
		assertEquals(4, call.getArguments().size());
		assertEquals(VariableAccess.class, call.getArguments().get(0).getClass());
		assertEquals(StringLiteral.class, call.getArguments().get(1).getClass());
		assertEquals(IntegerLiteral.class, call.getArguments().get(2).getClass());
		assertEquals(BooleanLiteral.class, call.getArguments().get(3).getClass());
		MapOrArrayAccess mapAccess = (MapOrArrayAccess)call.getFunction();
		assertEquals(IntegerLiteral.class, mapAccess.getKeyOrIndex().getClass());
		MemberAccess memberAccess = (MemberAccess)mapAccess.getMapOrArray();
		assertEquals("bar", memberAccess.getName().getText());
		VariableAccess variableAccess = (VariableAccess)memberAccess.getObject();
		assertEquals("foo", variableAccess.getVariableName().getText());
	}

	@Test
	public void testIfStatement () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ if true }} true body {{ end }}")).getNodes();
		assertEquals(1, nodes.size());
		IfStatement ifStmt = (IfStatement)nodes.get(0);
		assertEquals(BooleanLiteral.class, ifStmt.getCondition().getClass());
		assertEquals(1, ifStmt.getTrueBlock().size());
		assertEquals(Text.class, ifStmt.getTrueBlock().get(0).getClass());
		assertEquals(0, ifStmt.getElseIfs().size());
		assertEquals(0, ifStmt.getFalseBlock().size());

		nodes = new Parser().parse(new Source("test", "{{ if true }} true body {{ else }} false body {{ end }}")).getNodes();
		assertEquals(1, nodes.size());
		ifStmt = (IfStatement)nodes.get(0);
		assertEquals(BooleanLiteral.class, ifStmt.getCondition().getClass());
		assertEquals(1, ifStmt.getTrueBlock().size());
		assertEquals(Text.class, ifStmt.getTrueBlock().get(0).getClass());
		assertEquals(0, ifStmt.getElseIfs().size());
		assertEquals(1, ifStmt.getFalseBlock().size());
		assertEquals(Text.class, ifStmt.getFalseBlock().get(0).getClass());

		nodes = new Parser()
			.parse(new Source("test", "{{ if true }} true {{ if false }} innerTrue {{ else }} innerFalse {{end }} body {{ else }} false body {{ end }}"))
			.getNodes();
		assertEquals(1, nodes.size());
		ifStmt = (IfStatement)nodes.get(0);
		assertEquals(BooleanLiteral.class, ifStmt.getCondition().getClass());
		assertEquals(3, ifStmt.getTrueBlock().size());
		assertEquals(Text.class, ifStmt.getTrueBlock().get(0).getClass());
		assertEquals(Text.class, ifStmt.getTrueBlock().get(2).getClass());
		IfStatement innerIf = (IfStatement)ifStmt.getTrueBlock().get(1);
		assertEquals(1, innerIf.getTrueBlock().size());
		assertEquals(Text.class, innerIf.getTrueBlock().get(0).getClass());
		assertEquals(1, innerIf.getFalseBlock().size());
		assertEquals(Text.class, innerIf.getFalseBlock().get(0).getClass());

		assertEquals(1, ifStmt.getFalseBlock().size());
		assertEquals(Text.class, ifStmt.getFalseBlock().get(0).getClass());

		nodes = new Parser().parse(new Source("test", "{{ if true }} one {{ elseif false }} two {{ elseif false }} three {{ else }} four {{ end }}")).getNodes();
		assertEquals(1, nodes.size());
		ifStmt = (IfStatement)nodes.get(0);
		assertEquals(2, ifStmt.getElseIfs().size());
		assertEquals(1, ifStmt.getElseIfs().get(0).getTrueBlock().size());
		assertEquals(0, ifStmt.getElseIfs().get(0).getElseIfs().size());
		assertEquals(0, ifStmt.getElseIfs().get(0).getFalseBlock().size());
		assertEquals(1, ifStmt.getElseIfs().get(1).getTrueBlock().size());
		assertEquals(0, ifStmt.getElseIfs().get(1).getElseIfs().size());
		assertEquals(0, ifStmt.getElseIfs().get(1).getFalseBlock().size());
	}

	@Test
	public void testForStatement () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ for x in y }} true body {{expr}} {{ end }}")).getNodes();
		assertEquals(1, nodes.size());
		ForStatement forStmt = (ForStatement)nodes.get(0);
		assertNull(forStmt.getIndexOrKeyName());
		assertEquals("x", forStmt.getValueName().getText());
		assertEquals(VariableAccess.class, forStmt.getMapOrArray().getClass());
		assertEquals(3, forStmt.getBody().size());
		assertEquals(Text.class, forStmt.getBody().get(0).getClass());
		assertEquals(VariableAccess.class, forStmt.getBody().get(1).getClass());
		assertEquals(Text.class, forStmt.getBody().get(2).getClass());
	}

	@Test
	public void testWhileStatement () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ while true }} true body {{expr}} {{ end }}")).getNodes();
		assertEquals(1, nodes.size());
		WhileStatement whileStmt = (WhileStatement)nodes.get(0);
		assertEquals(BooleanLiteral.class, whileStmt.getCondition().getClass());
		assertEquals(3, whileStmt.getBody().size());
		assertEquals(Text.class, whileStmt.getBody().get(0).getClass());
		assertEquals(VariableAccess.class, whileStmt.getBody().get(1).getClass());
		assertEquals(Text.class, whileStmt.getBody().get(2).getClass());
	}

	@Test
	public void testMacro () {
		ParserResult template = new Parser().parse(new Source("test", "{{ macro myMacro(a, b, c) }} true body {{expr}} {{ end }}"));
		List<Node> nodes = template.getNodes();
		Macros macros = template.getMacros();

		assertEquals(1, nodes.size());
		Macro macro = (Macro)nodes.get(0);
		assertEquals(3, macro.getArgumentNames().size());
		assertEquals("a", macro.getArgumentNames().get(0).getText());
		assertEquals("b", macro.getArgumentNames().get(1).getText());
		assertEquals("c", macro.getArgumentNames().get(2).getText());

		assertEquals(3, macro.getBody().size());
		assertEquals(Text.class, macro.getBody().get(0).getClass());
		assertEquals(VariableAccess.class, macro.getBody().get(1).getClass());
		assertEquals(Text.class, macro.getBody().get(2).getClass());

		assertEquals(1, macros.size());
		macro = macros.get("myMacro");
		assertEquals(3, macro.getArgumentNames().size());
		assertEquals("a", macro.getArgumentNames().get(0).getText());
		assertEquals("b", macro.getArgumentNames().get(1).getText());
		assertEquals("c", macro.getArgumentNames().get(2).getText());

		assertEquals(3, macro.getBody().size());
		assertEquals(Text.class, macro.getBody().get(0).getClass());
		assertEquals(VariableAccess.class, macro.getBody().get(1).getClass());
		assertEquals(Text.class, macro.getBody().get(2).getClass());

		try {
			new Parser().parse(new Source("test", "{{if true}} {{ macro myMacro() }} test {{end}} {{end}}"));
			assertTrue("Macros are only allowed at the top level.", false);
		} catch (Throwable t) {
			// expected state
		}
	}

	@Test
	public void testInclude () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ include \"othertemplate.html\" with ( key1: 1 * 2 + 3, key2: \"test\" ) }}")).getNodes();
		assertEquals(1, nodes.size());

		Include inc = (Include)nodes.get(0);
		assertEquals("\"othertemplate.html\"", inc.getPath().getText());
		assertEquals(2, inc.getContext().size());
		Set<String> keys = new HashSet<String>();
		keys.add("key1");
		keys.add("key2");
		for (Entry<Span, Expression> entry : inc.getContext().entrySet()) {
			assertTrue(keys.size() > 0);
			if (entry.getKey().getText().equals("key1")) {
				keys.remove("key1");
				assertEquals(BinaryOperation.class, entry.getValue().getClass());
			} else if (entry.getKey().getText().equals("key2")) {
				keys.remove("key2");
				assertEquals(StringLiteral.class, entry.getValue().getClass());
			}
		}
		assertEquals(0, keys.size());
	}

	@Test
	public void testIncludeMacros () {
		List<Node> nodes = new Parser().parse(new Source("test", "{{ include \"othertemplate.html\" as math }}")).getNodes();
		assertEquals(1, nodes.size());

		Include inc = (Include)nodes.get(0);
		assertEquals("\"othertemplate.html\"", inc.getPath().getText());
		assertNull(inc.getContext());
		assertTrue(inc.isMacrosOnly());
		assertEquals("math", inc.getAlias().getText());
	}

	@Test
	public void testErrorMessage () {
		try {
			new Parser().parse(new Source("test", "\n\n\n{{   if (()  }}"));
		} catch (RuntimeException e) {
			System.out.println(e);
		}
	}

}
