# basis-template
Basis-template is an expressive templating engine for Java and other JVM languages. It is similar in spirit to [Jtwig](https://jtwig.org).

## Motivation
Why yet another templating engine?

* Zero dependencies.
* No external compilation steps while retaining a high level of performance.
* Allow logic in templates using familiar syntax.
* Exercise in compiler construction and interpreters.
* Simple enough for others to extend and dork around with.

## Setup
As a dependency of your Maven project:

```
<dependency>
	<groupId>io.marioslab.basis</groupId>
	<artifactId>tempalte</artifactId>
	<version>1.0</version>
</dependency>
```

As a dependency of your Gradle project:
```
compile 'io.marioslab.basis:template:1.0'
```

You can also build the `.jar` file yourself, assuming you have Maven and JDK 1.8+ installed:
```
mvn clean install
```

The resulting `.jar` file will be located in the `target/` folder.

## Basic Usage
Create a new file called `helloworld.bt`with the following content:

```html
Hello {{name}}.
```

We can then load the template from the file on the classpath, set the value of the template variable `name`, and render the template to a string, which we output to the console.


```java
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
```

This yields the following result:
```
Hello Hotzenplotz.
```

This illustrates all the API surface you'll generally encounter. A quick run-down of the code:
1. To load a template, we need a `TemplateLoader`. Basis-template provides loaders for classpath resources, files, and in-memory templates.
2. To pass variable values to the template, we need a `TemplateContext`. A context can be thought of as a map from variable names (like `name`), and their values (like the String `"Hotzenplotz"`).
3. To render the template, we pass it the context. The template then evaluates all expressions contained in it, looks up variable values in the context, and finally returns a rendered string.

That's it! Let's explore the templating language, which is much more expressive than the above example lets on.

## Text and code spans
A template consists of text and code spans. Text spans are any old character sequence. Code spans are character sequences inside `{{` and `}}` which must conform to the templating language syntax. Text and code spans can be freely intermixed, e.g.:

```
Dear {{customer.getName()}},

Thank you for purchasing {{license.getProductName()}}. Find below your activation codes:

{{for index, activationCode in license.activationCodes}}
	{{(index + 1)}}. {{activationCode}}
{{end}}

Please let me know if I can help you with anything else!

Kind regards
Your friendly neighbourhood customer service employee
```

We can stuff this template into the following code:

```java
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
```

Which will yield the output:

```
Dear Mr. Hotzenplotz,

Thank you for purchasing Hotzenplotz. Find below your activation codes:

	1. 3ba34234bcffe
	2. 5bbe77f879000
	3. dd3ee54324bf3

Please let me know if I can help you with anything else!

Kind regards
Your friendly neighbourhood customer service employee

```

When you render a template, all text and code spans are evaluated by the templating engine and written out ("emitted") to a `String` or `OutputStream` in sequence. Text spans, like "Dear " or "Thank you for purchasing ", are emitted verbatim as UTF-8 strings.

Code spans are more complex. If a code span consists of an expression like `{{license.productName}}` or `{{index + 1}}`, the expression is first evaluated by the templating engine. If it yields a non-void, non-null result, the result is converted to a UTF-8 string and emitted.

Some templating language constructs, like `for` above, consist of multiple intermixed code and text spans. In the `for` case above, the `{{for ...}}` and `{{end}}` code spans do non-emitting code spans, as they do not produce a value themselves. The spans in between are text spans and expression code spans producing a value, and will therefore be emitted (as many times as the for-loop iterates).

When a code span is the only non-whitespace character sequence in a line, and the code span does not emit a value, the entire line will be omitted from the output. Otherwise, the code span is either replaced with the emitted value, or entirely removed from its containing line if it does not emit a value, starting at `{{` and ending (inclusively) in `}}`.

Let's have a look at what we can put inside code spans.

## Literals
The basis-template language supports a wide range of literals, similar to what is supported by Java. When the templating engine encounters a code span only consisting of a literal, it will convert it to a string according to the semantics of Java's `String.valueOf()`. If a literal is used in an expression evaluation, like `{{1 + 2}}` it will assume the type of the literal during evaluation.

Boolean literals take the form of `true` and `false`:

```
{{true}} fake news is {{false}}.
```

Number literals come in integer and floating point flavors:

```
This is an integer {{123}}, and this is a float {{123.456}}.
```

You can specify the type of a numeric literal with a suffix, similar to how you can suffix `long`, `float` and `double` literals in Java:

```
A byte {{123b}}.
A short {{123s}}.
An int {{123}}.
A long {{123l}}.
A float {{123f}}.
A double {{123d}}.
```

While in general you will not find a lot of need for using the suffixes, they can come in handy when calling functions and methods that require a specific type.

> **Note**: the templating engine will perform widening type coercion when evaluating arithmethic operations, such as `{{1b + 2.3}}`. In this case, the `byte` operand will first be widened to a double, to match the `double` operand.

The templating language also supports character and string literals:

```
The character {{'a'}} is included in the string {{"a-team"}}.
```

Character and string literals may contain the common escape sequences `\n`, `\r`, `\t`. The characters `\`, `'` and `"` must also be escaped in character and string literals, e.g. `{{'\\'}} {{'\''}} {{"\""}}`.

Finally, since basis-template is a JVM templating engine, we can not escape the million dollar escape of `null`, which looks like this in literal form: `{{null}}`. This may come in handy if you want to compare the return value of a method or function to `null`, or must pass a `null`.

## Operators
The templating language supports most of the Java operators. The precedence of these operators is also the same as in Java.

### Unary Operators
You can negate a number via the unary `-` operator, e.g. `{{-234}}`. To negate boolean expressions, you can use the `!` operator, e.g. `{{!true}}`.

### Arithmetic Operators
Not entirely unexpectedly, the templating engine supports the common arithmetic operators, e.g. `{{1 + 2 * 3 / 4 % 2}}`.

Arithmetic operators evaluate to the wider type of their two operands. E.g. when adding a `byte` and a `float`, the resulting value will have type `float`.

As in Java, the `+` operator may also be used to concatenate a `String` with another value. The template engine will perform automatic coercion from the non-string operand to string in this case, e.g. `{{"Lucky number " + 9}}`.

### Comparison Operators
All comparison operators you know from Java are at your disposal, e.g. `{{23 < 34}}`, `{{23 <= 34}}`, `{{23 > 34}}`, `{{23 >= 34}}`, `{{ true != false }}`, `{{23 == 34}}`.

> **Note**: The equality operator does **NOT** invoke equals on object instances. Instead, it functions like its Java equivalent.

Comparison operators evaluate to a boolean.

### Logical Operators
In addition to the unary `!` operator, you can also use `&&` and `||`. The operators are a short-curcuiting operators. If the left-hand operator of `&&` evaluates to `false`, the right-hand operand will not be evaluated. If the left-hand operand of `||` evaluates to false, the right-hand operand will not be evaluated.

Logical operators evaluate to a boolean.

### Ternary Operator
The ternary operator is a short-hand for an `if` statement and works like in Java, e.g. `{{true ? "yes" : "no"}}`.

The conditional is required to evaluate to a boolean.

> **Note**: `null` does not evaluate to boolean `false`. This is not JavaScript. User the `==` or `!=` operators with (potential) `null` values.

### Grouping expressions by parenthesis
If you need more control, or want to make precedence explicit, you can use `(` and `)` to group expressions, e.g. `{{(1 + 3) * 4}}`.

### Where are my bit-wise operators?
These are currently not implemented. You can either implement them as functions, or send a pull request to add them to the language. It's simple and a great exercise!

## Contexts & Variables
For a template to be useful, we need to be able to inject values into it. As shown previously, this is done by providing a `TemplateContext` when rendering a template.

To set a variable value, invoke the `TemplateContext#set(String, Object)` method:

```
{{a}} {{b}} {{c}}
```

```Java
TemplateContext context = new TemplateContext();
context.set("a", 123).set("b", "Test").set("c", myObject);
template.render(context);
```

You can set a variable to any Java primitive or object, and even `null`. Variable names, also known as identifiers, follow the same rules as idenifiers in Java. They may start with `_`, `$`, or `[a-zA-Z]`, and then continue on with zero or more of `_`, `$`, `[a-zA-Z]`, or `[0-9]`.

When the templating engine encounters a variable name in an expression, it looks into the context for its value. Unlike in other templating engines, if the value for that variable name is not found, a `RuntimeException` is thrown. If you really require "optional" variables, you can check for their existence by comparing the variable to `null`.

When the variable value is evaluated, it takes on whatever type the corresponding Java object has. E.g. an `int` will be treated like an integer in expression, a `Map` like a map and so on. The template engine will also perform widening type coercions for arithmetic expressions and passing arguments to methods and functions in the same way Java does.

The evaluation of primitive types is straight forward. However, the real power of basis-template comes from being able to access fields and call methods on objects.

## Accessing fields
When a context variable points to an object, you can access that object's fields like in Java (with one slight twist):

```
Basis-template can access private {{myObject.privateField}}, package private {{myObject.packagePrivateField}}, protected {{myObject.protectedField}}, and public {{myObject.publicField}} fields. It can also access static {{myClass.STATIC_FIELD}} fields.
```

```java
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
```

The twist is that basis-template will stomp over your access modifiers and allow reading private, package private and protected fields. Field access is slightly more performant in basis-template than method invocations. This little unsafe feature lets you wrangle out a tiny bit more performance of your templates.

> **Note**: Unlike other templating engines, basis-template does not resolve getter methods following the Java bean convention. Either access the field by name, or invoke the getter.

## Calling methods
Similar to fields, basis-template lets you call any method on any object.

```
{{myObject.add(1, 2)}} {{myObject.add(1f, 2f)}} {{String.format(%010d", 93)}}
```

```java
public static class MyObject {
	private int add (int a, int b) { return a + b; }
	protected float add (float a, float b) { return a + b; }
	public static String staticMethod () { return "Hello"; }
}

public static void main (String[] args) {
	TemplateLoader loader = new ClasspathTemplateLoader();
	Template template = loader.load("/methods.bt");
	TemplateContext context = new TemplateContext();
	context.set("myObject", new MyObject());
	context.set("String", String.class);
	System.out.println(template.render(context));
}
```

Again, basis-template allows you to ignore access modifiers entirely.

One point of note is the fact, that basis-template can deal with overloaded methods. For this to work, the types of the arguments passed to an overloaded method must match the method argument types (which may require an implicit widnening type coercion). In the above example `{{myObject.add(1, 2)}}` will call the `MyObject.add(int, int)` method, because the two supplied arguments are of type `int`, where as `{{myObject.add(1f, 2f)}}` will call the `MyObject.add(float, float)` method because the supplied arguments are of type `float`. A widening type coercion will be attempted in a case like `{{myObject.add(1b, 1s)}}`. This would match the `MyObject.add(int, int)` method. through widening the `byte` and `short` arguments to `int`.

## Arrays and maps
The templating language also grants you access to array elements and map entries:

```
{{myArray[2]}} {{myMap.get("key")}} {{myMap["key"]}}
```

```java
TemplateContext context = new TemplateContext();
context.set("myArray", new int[] { 1, 2, 3 });
Map<String, String> myMap = new HashMap<String, String>();
context.set("myMap", myMap);
template.render(context);
```

Array elements are accessed via `[index]` like in Java. To access map entries, you can either call `Map.get()`, or use the short hand notation `map[key]`.

Both array indices and map keys can be arbitrary expressions. Array indices must evaluate to an `ìnt`. Map keys must evalute to the key type of the map.

## Access chaining
Like in Java, you can infinitely nest member, array element and map accesses:

```
{{{myObject.aField[12]["key"].someMethod(1, 2).anotherMethod()}}
```

### Functions
Basis-template supports functions as first class citizens. For this to work, you must use a Java 8+ JVM. But how to set a variable to a Java "function"?

```
{{cos(3.14f)}}
```
```java
TemplateContext context = new TemplateContext();
context.set("cos", (DoubleFunction<Double>)Math::cos);
template.render(context);
```

The trick is to take a method reference (like `Math::cos`) and cast it to a fitting `FunctionalInterface`. The Java compiler will translate this into an anonymous class instance with one method called `apply`. When such an instance is set as the value of a variable, and the template engine encounters that variable as a name of a function to call, it is smart enough to resolve the `apply` function and reflectively call it.

With this little trick, you can build up a "standard" library of sorts to be used in all our templates, with short function names like `trim`, `abs` and so on.

> **Note**: Basis-template does not come with any built-in functions out of the box. If you happen to create a set of such functions, send a PR!

This feature also allows you to store "functions" in fields, arrays, maps or variables, essentially making "functions" first class citizens.

```
This calls Math::abs on the argument: {{array[0](123)}}
This calls Math::signum on the argument: {{array[1](-7)}}
This calls the function in the field myFunc: {{myObject.myFunc(3)}}
```

```java
class MyObject {
	IntFunction<Integer> myFunc = v -> return v + 1;
}

context.set("myObject", new MyObject());
context.set("array", new IntFunction[] {Math::abs, Math::signum});
result = template.render(context);
```

Alternatively to injecting functional interface instances via the template context, you can also create functions directly in your template. These are called macros.

## Macros
A macro consists of a name, an argument list and a macro body. They are essentially functions defined directly in your template:

```
{{macro upperCase(a)
	a.toUpperCase()
end}}

Hey, {{upperCase("it'se me, Mario")}}

{{upperCase("oh no")}}
```

The macro declaration (and definition) itself does not emit anything. However, when we call the macro as a function, it's body text and code spans will be evaluated and emitted according to the arguments provided.

Also visible in this example: you can write a full program inside a single `{{}}` block!

> **Note**: When a macro is called, it gets its own context. This context only contains the arguments. Accessing the context of the enclosing template is not possible. This might change in the future.

> **Note**: Macros can currently not return a value to be used in an expression. A macro will always return `null` semantically. This might change in the future.

## Assignments
The templating language allows a limited form of assignements. You can set the value of a context variable in your template. This can be useful when you want to store function or method call return values:

```
{{{a = 10}}} {{b = abs(-234)}}
```

If the variable name already exists in the context, its value will be replaced. If the variable name did not exist already, it is created.

Assigning new values to object fields, arrays or maps is not supported and will never be supported, as this would allow modification of Java side objects from within the template.

## Control flow
### If
### For
### While

## Includes