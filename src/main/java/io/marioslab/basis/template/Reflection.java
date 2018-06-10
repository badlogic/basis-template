
package io.marioslab.basis.template;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Used by {@link AstInterpreter} to access fields and methods of objects. This is a singleton class used by all AstInterpreter
 * instances. Replace the default implementation via {@link #setInstance(Reflection)}. THe implementation must be thread-safe. */
public abstract class Reflection {
	private static Reflection instance = new JavaReflection();

	/** Sets the Reflection instance to be used by all Template interpreters **/
	public synchronized static void setInstance (Reflection reflection) {
		instance = reflection;
	}

	/** Returns the Reflection instance used to fetch field and call methods **/
	public synchronized static Reflection getInstance () {
		return instance;
	}

	/** Returns an opaque handle to a field with the given name or null if the field could not be found **/
	public abstract Object getField (Object obj, String name);

	/** Returns an opaque handle to the method with the given name best matching the signature implied by the given arguments, or
	 * null if the method could not be found. If obj is an instance of Class, the matching static method is returned. If the name
	 * is null and the object is a {@link FunctionalInterface}, the first declared method on the object is called. **/
	public abstract Object getMethod (Object obj, String name, Object... arguments);

	/** Returns the value of the field **/
	public abstract Object getFieldValue (Object obj, Object field);

	/** Calls the method on the object with the given arguments **/
	public abstract Object callMethod (Object obj, Object method, Object... arguments);

	public static class JavaReflection extends Reflection {
		private final Map<Class, Map<String, Field>> fieldCache = new ConcurrentHashMap<Class, Map<String, Field>>();
		private final Map<Class, Map<MethodSignature, Method>> methodCache = new ConcurrentHashMap<Class, Map<MethodSignature, Method>>();
		private final Map<Class, Map<MethodSignature, Method>> functionCache = new ConcurrentHashMap<Class, Map<MethodSignature, Method>>();

		@Override
		public Object getField (Object obj, String name) {
			Class cls = obj.getClass();
			Map<String, Field> fields = fieldCache.get(cls);
			if (fields == null) {
				fields = new ConcurrentHashMap<String, Field>();
				fieldCache.put(cls, fields);
			}

			Field field = fields.get(name);
			if (field == null) {
				try {
					field = cls.getDeclaredField(name);
					field.setAccessible(true);
					fields.put(name, field);
				} catch (Throwable t) {
					// fall through, try super classes
				}

				if (field == null) {
					Class parentClass = cls.getSuperclass();
					while (parentClass != Object.class && parentClass != null) {
						try {
							field = parentClass.getDeclaredField(name);
							field.setAccessible(true);
							fields.put(name, field);
						} catch (NoSuchFieldException e) {
							// fall through
						}
						parentClass = parentClass.getSuperclass();
					}
				}
			}

			return field;
		}

		@Override
		public Object getFieldValue (Object obj, Object field) {
			Field javaField = (Field)field;
			try {
				return javaField.get(obj);
			} catch (Throwable e) {
				throw new RuntimeException(
					"Couldn't get value of field '" + javaField.getName() + "' from object of type '" + obj.getClass().getSimpleName() + "'");
			}
		}

		@Override
		public Object getMethod (Object obj, String name, Object... arguments) {
			Class cls = obj instanceof Class ? (Class)obj : obj.getClass();
			Map<MethodSignature, Method> methods = methodCache.get(cls);
			if (methods == null) {
				methods = new ConcurrentHashMap<MethodSignature, Method>();
				methodCache.put(cls, methods);
			}

			Class[][] parameterTypes = new Class[2][arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				parameterTypes[0][i] = arguments[i] == null ? Object.class : arguments[i].getClass();
			}
			for (int i = 0; i < arguments.length; i++) {
				Class argType = arguments[i] == null ? Object.class : arguments[i].getClass();
				if (argType == Boolean.class)
					argType = boolean.class;
				else if (argType == Byte.class)
					argType = byte.class;
				else if (argType == Character.class)
					argType = char.class;
				else if (argType == Short.class)
					argType = short.class;
				else if (argType == Integer.class)
					argType = int.class;
				else if (argType == Long.class)
					argType = long.class;
				else if (argType == Float.class)
					argType = float.class;
				else if (argType == Double.class) argType = double.class;
				parameterTypes[1][i] = argType;
			}

			MethodSignature[] signatures = new MethodSignature[] {new MethodSignature(name, parameterTypes[0]), new MethodSignature(name, parameterTypes[1])};

			Method method = methods.get(signatures[0]);
			if (method == null) method = methods.get(signatures[1]);

			if (method == null) {
				for (int i = 0; i < 2; i++) {
					try {
						if (name == null)
							method = cls.getDeclaredMethods()[0];
						else
							method = cls.getDeclaredMethod(name, parameterTypes[i]);
						method.setAccessible(true);
						methods.put(signatures[i], method);
						break;
					} catch (Throwable e) {
						// fall through
					}
				}

				if (method == null) {
					Class parentClass = cls.getSuperclass();
					while (parentClass != Object.class && parentClass != null) {
						for (int i = 0; i < 2; i++) {
							try {
								if (name == null)
									method = parentClass.getDeclaredMethods()[0];
								else
									method = parentClass.getDeclaredMethod(name, parameterTypes[i]);
								method.setAccessible(true);
								methods.put(signatures[i], method);
								break;
							} catch (Throwable e) {
								// fall through
							}
						}
						parentClass = parentClass.getSuperclass();
					}
				}
			}

			return method;
		}

		@Override
		public Object callMethod (Object obj, Object method, Object... arguments) {
			Method javaMethod = (Method)method;
			try {
				return javaMethod.invoke(obj, arguments);
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't call method '" + javaMethod.getName() + "' with arguments '" + Arrays.toString(arguments)
					+ "' on object of type '" + obj.getClass().getSimpleName() + "'.", t);
			}
		}

		private static class MethodSignature {
			private final String name;
			private final Class[] parameters;
			private final int hashCode;

			public MethodSignature (String name, Class[] parameters) {
				this.name = name;
				this.parameters = parameters;
				final int prime = 31;
				int hash = 1;
				hash = prime * hash + ((name == null) ? 0 : name.hashCode());
				hash = prime * hash + Arrays.hashCode(parameters);
				hashCode = hash;
			}

			@Override
			public int hashCode () {
				return hashCode;
			}

			@Override
			public boolean equals (Object obj) {
				if (this == obj) return true;
				if (obj == null) return false;
				if (getClass() != obj.getClass()) return false;
				MethodSignature other = (MethodSignature)obj;
				if (name == null) {
					if (other.name != null) return false;
				} else if (!name.equals(other.name)) return false;
				if (!Arrays.equals(parameters, other.parameters)) return false;
				return true;
			}
		}
	}
}
