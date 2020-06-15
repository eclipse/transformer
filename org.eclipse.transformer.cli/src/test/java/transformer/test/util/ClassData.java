/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package transformer.test.util;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClassData {
	public static final String		CLASS_NAME			= ClassData.class.getSimpleName();

	//

	private static final String[]	EMPTY_STRING_ARRAY	= new String[0];

	//

	protected final String			hashText;
	protected final String			simpleClassName;

	public String getHashText() {
		return hashText;
	}

	public String getSimpleClassName() {
		return simpleClassName;
	}

	//

	private final String					className;
	private final String					superclassName;
	private final String[]					interfaceNames;

	private final Set<String>				classAnnotationNames;

	private final Set<String>				fieldNames;
	private final BiDiMap<String, String>	fieldAnnotationNames;

	private final Set<String>				methodDescs;
	private final BiDiMap<String, String>	methodAnnotationNames;

	private final Set<String>				initDescs;
	private final BiDiMap<String, String>	initAnnotationNames;

	private final Set<String>				staticMethodDescs;
	private final BiDiMap<String, String>	staticMethodAnnotationNames;

	public String getClassName() {
		return className;
	}

	public String getSuperclassName() {
		return superclassName;
	}

	public String[] getInterfaceNames() {
		return interfaceNames;
	}

	public Set<String> getClassAnnotationNames() {
		return classAnnotationNames;
	}

	public Set<String> getFieldNames() {
		return fieldNames;
	}

	public BiDiMap<String, String> getFieldAnnotationNames() {
		return fieldAnnotationNames;
	}

	public Set<String> getMethodDescriptions() {
		return methodDescs;
	}

	public BiDiMap<String, String> getMethodAnnotationNames() {
		return methodAnnotationNames;
	}

	public Set<String> getInitDescriptions() {
		return initDescs;
	}

	public BiDiMap<String, String> getInitAnnotationNames() {
		return initAnnotationNames;
	}

	public Set<String> getStaticMethodDescriptions() {
		return staticMethodDescs;
	}

	public BiDiMap<String, String> getStaticMethodAnnotationNames() {
		return staticMethodAnnotationNames;
	}

	//

	public ClassData(Class<?> testClass) {
		this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "("
			+ testClass.getSimpleName() + ")";
		this.simpleClassName = testClass.getSimpleName();

		this.className = testClass.getName();

		Class<?> useSuperclass = testClass.getSuperclass();
		this.superclassName = ((useSuperclass == null) ? null : useSuperclass.getName());

		Class<?>[] useInterfaces = testClass.getInterfaces();
		String[] useInterfaceNames;
		int numInterfaces = useInterfaces.length;
		if (numInterfaces == 0) {
			useInterfaceNames = EMPTY_STRING_ARRAY;
		} else {
			useInterfaceNames = new String[useInterfaces.length];
			for (int interfaceNo = 0; interfaceNo < numInterfaces; interfaceNo++) {
				useInterfaceNames[interfaceNo] = useInterfaces[interfaceNo].getName();
			}
		}
		this.interfaceNames = useInterfaceNames;

		this.classAnnotationNames = getNames(testClass.getDeclaredAnnotations());

		Field[] useFields = testClass.getDeclaredFields();
		int numFields = useFields.length;
		Set<String> useFieldNames = new HashSet<>(numFields);
		BiDiMap<String, String> useFieldAnnotationNames = new BiDiMapImpl<>(String.class, "field",
			String.class, "annotation");
		for (Field field : useFields) {
			String fieldName = field.getName();
			useFieldNames.add(fieldName);
			for (Annotation anno : field.getAnnotations()) {
				useFieldAnnotationNames.record(fieldName, anno.annotationType()
					.getName());
			}
		}

		this.fieldNames = useFieldNames;
		this.fieldAnnotationNames = useFieldAnnotationNames;

		Method[] useMethods = testClass.getDeclaredMethods();
		int numStaticMethods = 0;
		int numMethods = 0;
		for (Method method : useMethods) {
			if (isStatic(method)) {
				numStaticMethods++;
			} else {
				numMethods++;
			}
		}

		Set<String> useMethodDescs = new HashSet<>(numMethods);
		Set<String> useStaticMethodDescs = new HashSet<>(numStaticMethods);

		BiDiMap<String, String> useStaticMethodAnnotationNames = new BiDiMapImpl<>(String.class,
			"static method", String.class, "annotation");
		BiDiMap<String, String> useMethodAnnotationNames = new BiDiMapImpl<>(String.class, "method",
			String.class, "annotation");

		for (Method method : useMethods) {
			String methodDesc = method.toString();
			boolean isStatic = isStatic(method);

			BiDiMap<String, String> useAnnotationNames;
			if (isStatic) {
				useStaticMethodDescs.add(methodDesc);
				useAnnotationNames = useStaticMethodAnnotationNames;
			} else {
				useMethodDescs.add(methodDesc);
				useAnnotationNames = useMethodAnnotationNames;
			}

			for (Annotation anno : method.getDeclaredAnnotations()) {
				useAnnotationNames.record(methodDesc, anno.annotationType()
					.getName());
			}
		}

		this.staticMethodDescs = useStaticMethodDescs;
		this.staticMethodAnnotationNames = useStaticMethodAnnotationNames;
		this.methodDescs = useMethodDescs;
		this.methodAnnotationNames = useMethodAnnotationNames;

		Constructor<?>[] useInits = testClass.getDeclaredConstructors();
		Set<String> useInitDescs = new HashSet<>(useInits.length);
		BiDiMap<String, String> useInitAnnotationNames = new BiDiMapImpl<>(String.class, "init",
			String.class, "annotation");

		for (Constructor<?> init : useInits) {
			String initDescription = init.toString();
			useInitDescs.add(initDescription);
			for (Annotation anno : init.getDeclaredAnnotations()) {
				useInitAnnotationNames.record(initDescription, anno.annotationType()
					.getName());
			}
		}

		this.initDescs = useInitDescs;
		this.initAnnotationNames = useInitAnnotationNames;
	}

	protected static boolean isStatic(Method method) {
		return ((method.getModifiers() & Modifier.STATIC) != 0);
	}

	protected static Set<String> getNames(Annotation[] annotations) {
		Set<String> annotationNames;
		int numAnnotations = annotations.length;
		if (numAnnotations == 0) {
			annotationNames = Collections.emptySet();
		} else {
			annotationNames = new HashSet<>(numAnnotations);
			for (Annotation classAnnotation : annotations) {
				annotationNames.add(classAnnotation.annotationType()
					.getName());
			}
		}
		return annotationNames;
	}

	//

	private static final String logPrefix = CLASS_NAME + ": " + "log" + ": ";

	public void log(PrintWriter writer) {
		writer.println(logPrefix + "Class Data: BEGIN: " + getHashText());

		writer.println(logPrefix + "Class name: " + className);
		writer.println(logPrefix + "Superclass name: " + superclassName);
		writer.println(logPrefix + "Interface names: " + interfaceNames);

		if (classAnnotationNames.isEmpty()) {
			writer.println(logPrefix + "Class annotations: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Class annotations:");
			for (String annoName : classAnnotationNames) {
				writer.println(logPrefix + "  " + annoName);
			}
		}

		if (fieldNames.isEmpty()) {
			writer.println(logPrefix + "Fields: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Fields:");
			for (String fieldName : fieldNames) {
				writer.println(logPrefix + "  " + fieldName);
			}
		}

		if (fieldAnnotationNames.isEmpty()) {
			writer.println(logPrefix + "Field annotations: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Field annotations:");
			for (String fieldName : fieldAnnotationNames.getHolders()) {
				writer.println(logPrefix + "  Field: " + fieldName);
				for (String annoName : fieldAnnotationNames.getHeld(fieldName)) {
					writer.println(logPrefix + "    " + annoName);
				}
			}
		}

		if (methodDescs.isEmpty()) {
			writer.println(logPrefix + "Methods: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Methods:");
			for (String methodDesc : methodDescs) {
				writer.println(logPrefix + "  " + methodDesc);
			}
		}

		if (methodAnnotationNames.isEmpty()) {
			writer.println(logPrefix + "Method annotations: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Method annotations:");
			for (String methodName : methodAnnotationNames.getHolders()) {
				writer.println(logPrefix + "  Method: " + methodName);
				for (String annoName : methodAnnotationNames.getHeld(methodName)) {
					writer.println(logPrefix + "    " + annoName);
				}
			}
		}

		if (initDescs.isEmpty()) {
			writer.println(logPrefix + "Inits: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Inits:");
			for (String initDesc : initDescs) {
				writer.println(logPrefix + "  " + initDesc);
			}
		}

		if (initAnnotationNames.isEmpty()) {
			writer.println(logPrefix + "Init annotations: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Init annotations:");
			for (String initName : initAnnotationNames.getHolders()) {
				writer.println(logPrefix + "  Init: " + initName);
				for (String annoName : initAnnotationNames.getHeld(initName)) {
					writer.println(logPrefix + "    " + annoName);
				}
			}
		}

		if (staticMethodDescs.isEmpty()) {
			writer.println(logPrefix + "Static methods: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Static methods:");
			for (String staticMethodDesc : staticMethodDescs) {
				writer.println(logPrefix + "  " + staticMethodDesc);
			}
		}

		if (staticMethodAnnotationNames.isEmpty()) {
			writer.println(logPrefix + "Static method annotations: ** EMPTY **");
		} else {
			writer.println(logPrefix + "Static method annotations:");
			for (String staticMethodName : staticMethodAnnotationNames.getHolders()) {
				writer.println(logPrefix + "  Static method: " + staticMethodName);
				for (String annoName : staticMethodAnnotationNames.getHeld(staticMethodName)) {
					writer.println(logPrefix + "    " + annoName);
				}
			}
		}

		writer.println(logPrefix + "Class Data: END: " + getHashText());
	}
}
