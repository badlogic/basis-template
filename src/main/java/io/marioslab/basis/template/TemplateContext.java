
package io.marioslab.basis.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateContext {
	public final List<Map<String, Object>> symbols = new ArrayList<Map<String, Object>>();

	public TemplateContext () {
		push(new HashMap<String, Object>());
	}

	public TemplateContext set (String name, Object value) {
		symbols.get(symbols.size() - 1).put(name, value);
		return this;
	}

	Object get (String name) {
		for (int i = symbols.size() - 1; i >= 0; i--) {
			Map<String, Object> ctx = symbols.get(i);
			Object value = ctx.get(name);
			if (value != null) return value;
		}
		return null;
	}

	void push (Map<String, Object> scope) {
		symbols.add(scope);
	}

	Map<String, Object> getScope () {
		return symbols.get(symbols.size() - 1);
	}

	void pop () {
		symbols.remove(symbols.size() - 1);
	}
}
