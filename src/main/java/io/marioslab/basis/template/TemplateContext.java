
package io.marioslab.basis.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.marioslab.basis.template.interpreter.AstInterpreter;

/**
 * <p>
 * A template context stores mappings from variable names to user provided variable values. A {@link Template} is given a context
 * for rendering to resolve variable values it references in template expressions.
 * </p>
 *
 * <p>
 * Internally, a template context is a stack of these mappings, similar to scopes in a programming language, and used as such by
 * the {@link AstInterpreter}.
 * </p>
 */
public class TemplateContext {
	public final List<Map<String, Object>> scopes = new ArrayList<Map<String, Object>>();

	/** Keeps track of previously allocated, unused scopes. New scopes are first tried to be retrieved from this pool to avoid
	 * generating garbage. **/
	public final List<Map<String, Object>> freeScopes = new ArrayList<Map<String, Object>>();

	public TemplateContext () {
		push();
	}

	/** Sets the value of the variable with the given name. */
	public TemplateContext set (String name, Object value) {
		scopes.get(scopes.size() - 1).put(name, value);
		return this;
	}

	/** Internal. Returns the value of the variable with the given name, walking the scope stack from top to bottom, similar to how
	 * scopes in programming languages are searched for variables. */
	public Object get (String name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			Map<String, Object> ctx = scopes.get(i);
			Object value = ctx.get(name);
			if (value != null) return value;
		}
		return null;
	}

	/** Internal. Pushes a new "scope" onto the stack. **/
	public void push () {
		Map<String, Object> newScope = freeScopes.size() > 0 ? freeScopes.remove(freeScopes.size() - 1) : new HashMap<String, Object>();
		scopes.add(newScope);
	}

	/** Internal. Pops the top of the "scope" stack. **/
	public void pop () {
		Map<String, Object> oldScope = scopes.remove(scopes.size() - 1);
		oldScope.clear();
		freeScopes.add(oldScope);
	}
}
