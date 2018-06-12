
package io.marioslab.basis.template;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	 * null if the method could not be found. The first argument in arguments  must be the receiver (and object or Class instance).
	 * If the receiver is an instance of Class, the matching static method is returned. If the name
	 * is null and the receiver is a {@link FunctionalInterface}, the first declared method on the object is called. **/
	public abstract Object getMethod (String name, List<Object> arguments);

	/** Returns the value of the field **/
	public abstract Object getFieldValue (Object obj, Object field);

	/** Calls the method on the object with the given arguments. The object is the first argument in arguments. **/
	public abstract Object callMethod (Object method, List<Object> arguments);

	public static class JavaReflection extends Reflection {
		private final Map<Class, Map<String, Field>> fieldCache = new ConcurrentHashMap<Class, Map<String, Field>>();
		private final Map<Class, Map<MethodSignature, MethodHandle>> methodCache = new ConcurrentHashMap<Class, Map<MethodSignature, MethodHandle>>();

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
		public Object getMethod (String name, List<Object> arguments) {
			Object obj = arguments.get(0);
			Class cls = obj instanceof Class ? (Class)obj : obj.getClass();
			Map<MethodSignature, MethodHandle> methods = methodCache.get(cls);
			if (methods == null) {
				methods = new ConcurrentHashMap<MethodSignature, MethodHandle>();
				methodCache.put(cls, methods);
			}

			Class[][] parameterTypes = new Class[2][arguments.size() - 1];
			for (int i = 1, n = arguments.size(); i < n; i++) {
				parameterTypes[0][i - 1] = arguments.get(i) == null ? Object.class : arguments.get(i).getClass();
			}
			for (int i = 1, n = arguments.size(); i < n; i++) {
				Class argType = arguments.get(i) == null ? Object.class : arguments.get(i).getClass();
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
				parameterTypes[1][i - 1] = argType;
			}

			MethodSignature[] signatures = new MethodSignature[] {new MethodSignature(name, parameterTypes[0]), new MethodSignature(name, parameterTypes[1])};

			// Check if we have the method cached based on signatures
			MethodHandle methodHandle = methods.get(signatures[0]);
			if (methodHandle != null) {
				return methodHandle;
			} else {
				methodHandle = methods.get(signatures[1]);
				if (methodHandle != null) return methodHandle;
			}

			// Check if we can find the method on the concrete
			// class as a declared method.
			Method method = null;
			MethodSignature signature = null;
			for (int i = 0; i < 2; i++) {
				try {
					if (name == null)
						method = cls.getDeclaredMethods()[0];
					else
						method = cls.getDeclaredMethod(name, parameterTypes[i]);
					signature = signatures[i];
					break;
				} catch (Throwable e) {
					// fall through
				}
			}

			// Check the base classes
			if (method == null) {
				Class parentClass = cls.getSuperclass();
				while (parentClass != Object.class && parentClass != null) {
					for (int i = 0; i < 2; i++) {
						try {
							if (name == null)
								method = parentClass.getDeclaredMethods()[0];
							else
								method = parentClass.getDeclaredMethod(name, parameterTypes[i]);
							signature = signatures[i];
							break;
						} catch (Throwable e) {
							// fall through
						}
					}
					parentClass = parentClass.getSuperclass();
				}
			}

			// Convert the found method via LambdaMetaFactory and cache it
			if (method != null) {
				method.setAccessible(true);
				Lookup lookup = MethodHandles.lookup();
				try {
					methodHandle = lookup.unreflect(method);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Couldn't lookup method " + method.getName());
				}
				methods.put(signature, methodHandle);
				return methodHandle;
			} else {
				return null;
			}
		}

		@Override
		public Object callMethod (Object method, List<Object> arguments) {
			MethodHandle javaMethod = (MethodHandle)method;
			try {
				if(javaMethod.type().parameterCount() > 0 && !javaMethod.type().parameterType(0).isAssignableFrom(arguments.get(0).getClass())) {
					arguments.remove(0);
				}
				return javaMethod.invokeWithArguments(arguments);
			} catch (Throwable t) {
				throw new RuntimeException("Couldn't call method '" + method.toString() + "' with arguments '" + arguments.toString()
					+ "' on object of type '" + arguments.get(0).getClass().getSimpleName() + "'.", t);
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
