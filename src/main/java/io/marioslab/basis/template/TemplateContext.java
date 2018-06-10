
package io.marioslab.basis.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateContext {
	public final List<Map<String, Object>> scopes = new ArrayList<Map<String, Object>>();
	public final List<Map<String, Object>> freeScopes = new ArrayList<Map<String, Object>>();

	public TemplateContext () {
		push();
	}

	public TemplateContext set (String name, Object value) {
		scopes.get(scopes.size() - 1).put(name, value);
		return this;
	}

	Object get (String name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			Map<String, Object> ctx = scopes.get(i);
			Object value = ctx.get(name);
			if (value != null) return value;
		}
		return null;
	}

	void push () {
		Map<String, Object> newScope = freeScopes.size() > 0 ? freeScopes.remove(freeScopes.size() - 1) : new HashMap<String, Object>();
		scopes.add(newScope);
	}

	void pop () {
		Map<String, Object> oldScope = scopes.remove(scopes.size() - 1);
		oldScope.clear();
		freeScopes.add(oldScope);
	}
}
