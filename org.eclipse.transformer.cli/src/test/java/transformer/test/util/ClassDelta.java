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

public class ClassDelta implements Delta {
	public static final String CLASS_NAME = ClassDelta.class.getSimpleName();

	public ClassDelta(ClassData finalClass, ClassData initialClass) {
		this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "("
			+ finalClass.getSimpleClassName() + "," + initialClass.getSimpleClassName() + ")";
		this.finalSimpleClassName = finalClass.getSimpleClassName();
		this.initialSimpleClassName = initialClass.getSimpleClassName();

		this.finalClassName = finalClass.getClassName();
		this.initialClassName = initialClass.getClassName();
		this.changedClassName = (!this.initialClassName.equals(this.finalClassName));

		this.finalSuperclassName = finalClass.getSuperclassName();
		this.initialSuperclassName = initialClass.getSuperclassName();
		this.changedSuperclassName = Delta.strCmp(this.finalClassName, this.initialSuperclassName);

		this.interfaceNameChanges = SetDeltaImpl.stringDelta("interface");
		this.interfaceNameChanges.subtract(finalClass.getInterfaceNames(), initialClass.getInterfaceNames());

		this.classAnnoChanges = SetDeltaImpl.stringDelta("annotation");
		this.classAnnoChanges.subtract(finalClass.getClassAnnotationNames(), initialClass.getClassAnnotationNames());

		this.fieldChanges = SetDeltaImpl.stringDelta("field");
		this.fieldChanges.subtract(finalClass.getFieldNames(), initialClass.getFieldNames());
		this.fieldAnnoChanges = BiDiMapDeltaImpl.stringDelta("field", "annotation");
		this.fieldAnnoChanges.subtract(finalClass.getFieldAnnotationNames(), initialClass.getFieldAnnotationNames());

		this.methodChanges = SetDeltaImpl.stringDelta("method");
		this.methodChanges.subtract(finalClass.getMethodDescriptions(), initialClass.getMethodDescriptions());
		this.methodAnnoChanges = BiDiMapDeltaImpl.stringDelta("method", "annotation");
		this.methodAnnoChanges.subtract(finalClass.getMethodAnnotationNames(), initialClass.getMethodAnnotationNames());

		this.initChanges = SetDeltaImpl.stringDelta("init");
		this.initChanges.subtract(finalClass.getInitDescriptions(), initialClass.getInitDescriptions());
		this.initAnnoChanges = BiDiMapDeltaImpl.stringDelta("init", "annotation");
		this.initAnnoChanges.subtract(finalClass.getInitAnnotationNames(), initialClass.getInitAnnotationNames());

		this.staticMethodChanges = SetDeltaImpl.stringDelta("static method");
		this.staticMethodChanges.subtract(finalClass.getStaticMethodDescriptions(),
			initialClass.getStaticMethodDescriptions());
		this.staticMethodAnnoChanges = BiDiMapDeltaImpl.stringDelta("static method", "annotation");
		this.staticMethodAnnoChanges.subtract(finalClass.getStaticMethodAnnotationNames(),
			initialClass.getStaticMethodAnnotationNames());
	}

	//

	private final String hashText;

	@Override
	public String getHashText() {
		return hashText;
	}

	private final String	finalSimpleClassName;
	private final String	initialSimpleClassName;

	public String getFinalSimpleClassName() {
		return finalSimpleClassName;
	}

	public String getInitialSimpleClassName() {
		return initialSimpleClassName;
	}

	//

	private final String							finalClassName;
	private final String							initialClassName;
	private final boolean							changedClassName;

	private final String							initialSuperclassName;
	private final String							finalSuperclassName;
	private final boolean							changedSuperclassName;

	private final SetDeltaImpl<String>				interfaceNameChanges;

	//

	private final SetDeltaImpl<String>				classAnnoChanges;

	private final SetDeltaImpl<String>				fieldChanges;
	private final BiDiMapDeltaImpl<String, String>	fieldAnnoChanges;

	private final SetDeltaImpl<String>				methodChanges;
	private final BiDiMapDeltaImpl<String, String>	methodAnnoChanges;

	private final SetDeltaImpl<String>				initChanges;
	private final BiDiMapDeltaImpl<String, String>	initAnnoChanges;

	private final SetDeltaImpl<String>				staticMethodChanges;
	private final BiDiMapDeltaImpl<String, String>	staticMethodAnnoChanges;

	//

	public boolean nullClassNameChange() {
		return !changedClassName;
	}

	public boolean nullSuperclassNameChange() {
		return !changedSuperclassName;
	}

	public boolean nullInterfaceNameChanges() {
		return interfaceNameChanges.isNull();
	}

	public boolean nullFieldChanges() {
		return fieldChanges.isNull();
	}

	public boolean nullFieldAnnotationChanges() {
		return fieldAnnoChanges.isNull();
	}

	public boolean nullMethodChanges() {
		return methodChanges.isNull();
	}

	public boolean nullMethodAnnotationChanges() {
		return methodAnnoChanges.isNull();
	}

	public boolean nullInitChanges() {
		return initChanges.isNull();
	}

	public boolean nullInitAnnotationChanges() {
		return initAnnoChanges.isNull();
	}

	public boolean nullStaticMethodChanges() {
		return staticMethodChanges.isNull();
	}

	public boolean nullStaticMethodAnnotationChanges() {
		return staticMethodAnnoChanges.isNull();
	}

	@Override
	public boolean isNull() {
		return (!changedClassName && !changedSuperclassName && interfaceNameChanges.isNull() && fieldChanges.isNull()
			&& fieldAnnoChanges.isNull() && methodChanges.isNull() && methodAnnoChanges.isNull() && initChanges.isNull()
			&& initAnnoChanges.isNull() && staticMethodChanges.isNull() && staticMethodAnnoChanges.isNull());
	}

	private static final String logPrefix = CLASS_NAME + ": " + "log" + ": ";

	@Override
	public void log(PrintWriter writer) {
		if (isNull()) {
			writer.println(logPrefix + "Class Delta: Unchanged [ " + finalClassName + " ]: " + getHashText());
			return;
		}

		writer.println(logPrefix + "Class Delta: BEGIN [ " + finalClassName + " ]: " + getHashText());

		if (changedClassName) {
			writer.println(logPrefix + "Final class name [ " + finalClassName + " ]");
			writer.println(logPrefix + "Initial class name [ " + initialClassName + " ]");
		} else {
			writer.println(logPrefix + "Unchanged class name [ " + finalClassName + " ]");
		}

		if (changedSuperclassName) {
			writer.println(logPrefix + "Final super class name [ " + finalSuperclassName + " ]");
			writer.println(logPrefix + "Initial super class name [ " + initialSuperclassName + " ]");
		} else {
			writer.println(logPrefix + "Unchanged super class name [ " + finalSuperclassName + " ]");
		}

		writer.println(logPrefix + "Interface names:");
		interfaceNameChanges.log(writer);

		writer.println(logPrefix + "Fields:");
		fieldChanges.log(writer);
		writer.println(logPrefix + "Field annotations:");
		fieldAnnoChanges.log(writer);

		writer.println(logPrefix + "Methods:");
		methodChanges.log(writer);
		writer.println(logPrefix + "Method annotations:");
		methodAnnoChanges.log(writer);

		writer.println(logPrefix + "Initializers:");
		initChanges.log(writer);
		writer.println(logPrefix + "Initializer annotations:");
		initAnnoChanges.log(writer);

		writer.println(logPrefix + "Static methods:");
		staticMethodChanges.log(writer);
		writer.println(logPrefix + "Static method annotations:");
		staticMethodAnnoChanges.log(writer);

		writer.println(logPrefix + "Class Delta: END [ " + finalClassName + " ]: " + getHashText());
	}
}
