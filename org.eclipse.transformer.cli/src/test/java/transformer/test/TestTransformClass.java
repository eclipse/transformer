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

package transformer.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.ClassChangesImpl;
import org.eclipse.transformer.action.impl.JarActionImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.jakarta.JakartaTransformer;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import aQute.bnd.classfile.AnnotationInfo;
import aQute.bnd.classfile.AnnotationsAttribute;
import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ElementValueInfo;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.MethodInfo;
import aQute.lib.io.ByteBufferDataInput;
import transformer.test.data.Sample_InjectAPI_Jakarta;
import transformer.test.data.Sample_InjectAPI_Javax;
import transformer.test.data.Sample_Repeat_Target;
import transformer.test.util.CaptureLoggerImpl;
import transformer.test.util.ClassData;

public class TestTransformClass extends CaptureTest {
	// Rename used by the injection sample.

	public static final String	JAVAX_INJECT_PACKAGE_NAME			= "javax.inject";
	public static final String	JAKARTA_INJECT_PACKAGE_NAME			= "jakarta.inject";

	// Rename used by the repeat annotation sample.

	public static final String	JAVAX_REPEAT_PACKAGE_NAME			= "transformer.test.data.javax.repeat";
	public static final String	JAKARTA_REPEAT_PACKAGE_NAME			= "transformer.test.data.jakarta.repeat";

	// Rename used by the annotated servlet.

	public static final String	JAVAX_ANNO_PACKAGE_NAME				= "javax.annotation";
	public static final String	JAVAX_SERVLET_PACKAGE_NAME			= "javax.servlet";
	public static final String	JAVAX_SERVLET_ANNO_PACKAGE_NAME		= "javax.servlet.annotation";

	public static final String	JAKARTA_ANNO_PACKAGE_NAME			= "jakarta.annotation";
	public static final String	JAKARTA_SERVLET_PACKAGE_NAME		= "jakarta.servlet";
	public static final String	JAKARTA_SERVLET_ANNO_PACKAGE_NAME	= "jakarta.servlet.annotation";

	// These test classes are build within the project:

	public static final String	INJECT_JAVAX_CLASS_NAME				= Sample_InjectAPI_Javax.class.getName();
	public static final String	INJECT_JAKARTA_CLASS_NAME			= Sample_InjectAPI_Jakarta.class.getName();

	public static final String	REPEAT_TARGET_CLASS_NAME			= Sample_Repeat_Target.class.getName();
	public static final String	REPEAT_TARGET_RESOURCE_NAME			= "Sample_Repeat_Target.class";

	// The annotated servlet and the mixed servlet classes are provided from
	// open-liberty.

	public static final String	TEST_DATA_RESOURCE_NAME				= "transformer/test/data";

	// Tests disabled, since "AnnotatedServlet" and "BasicEnvPrimMixServlet" are
	// from open-liberty.

	// "AnnotatedServlet" has these runtime visible annotations:
	//
	// #46 = Utf8 RuntimeVisibleAnnotations
	// #47 = Utf8 Ljavax/servlet/annotation/WebServlet;

	// com.ibm.ws.sample.sci.AnnotatedServlet extends
	// javax.servlet.http.HttpServlet
	// public static final String ANNOTATED_SERVLET_SIMPLE_CLASS_NAME =
	// "AnnotatedServlet.class";

	// "BasicEnvPrimMixServlet" has these runtime visible annotations:
	//
	// #103 = Utf8 RuntimeVisibleAnnotations
	// #104 = Utf8 Ljavax/annotation/Resource;
	// #129 = Utf8 Lorg/junit/Test;
	// #151 = Utf8 Ljavax/annotation/Resources;
	// #171 = Utf8 Ljavax/servlet/annotation/WebServlet;

	// public class com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet
	// extends componenttest.app.FATServlet
	// public static final String MIXED_SERVLET_SIMPLE_CLASS_NAME =
	// "BasicEnvPrimMixServlet.class";

	//

	@Test
	public void testJavaxAsJavax_load_inject() {
		System.out.println("Test javax null transformation on the injection sample");
		testLoad(INJECT_JAVAX_CLASS_NAME, getClassLoader_null());
	}

	@Test
	public void testJakartaAsJakarta_load_inject() {
		System.out.println("Test jakarta null transformation on the injection sample");
		testLoad(INJECT_JAKARTA_CLASS_NAME, getClassLoader_null());
	}

	@Test
	public void testJavaxAsJakarta_load_inject() {
		System.out.println("Test javax to jakarta transformation on the injection sample");
		Class<?> testClass = testLoad(INJECT_JAVAX_CLASS_NAME, getClassLoader_toJakarta());
		ClassData testData = new ClassData(testClass);
		testData.log(new PrintWriter(System.out, true)); // autoflush
	}

	@Test
	public void testJakartaAsJavax_inject() {
		System.out.println("Test jakarta to javax transformation on the injection sample");
		Class<?> testClass = testLoad(INJECT_JAKARTA_CLASS_NAME, getClassLoader_toJavax());
		ClassData testData = new ClassData(testClass);
		testData.log(new PrintWriter(System.out, true)); // autoflush
	}

	@Test
	public void testJavaxAsJakarta_load_repeat() {
		System.out.println("Test javax to jakarta transformation on the repeat annotation sample");
		Class<?> testClass = testLoad(REPEAT_TARGET_CLASS_NAME, getClassLoader_toJakarta());

		Annotation[] testAnnos = testClass.getAnnotations();
		for (Annotation testAnno : testAnnos) {
			display("Class anno [ %s ]", testAnno);
		}

		for (Method method : testClass.getMethods()) {
			display("Method [ %s ]", method);
			for (Annotation anno : method.getAnnotations()) {
				display("  Method anno [ %s ]", anno);
			}
		}

		for (Field field : testClass.getFields()) {
			display("Field [ %s ]", field);
			for (Annotation anno : field.getAnnotations()) {
				display("  Method anno [ %s ]", anno);
			}
		}

		ClassData testData = new ClassData(testClass);
		testData.log(new PrintWriter(System.out, true)); // autoflush
	}

	//

	protected Set<String> includes;

	public Set<String> getIncludes() {
		if (includes == null) {
			includes = new HashSet<>();
			includes.add(ClassActionImpl.classNameToResourceName(INJECT_JAVAX_CLASS_NAME));
			includes.add(ClassActionImpl.classNameToResourceName(INJECT_JAKARTA_CLASS_NAME));

			includes.add(ClassActionImpl.classNameToResourceName(REPEAT_TARGET_CLASS_NAME));

			// includes.add( TEST_DATA_RESOURCE_NAME + '/' +
			// ANNOTATED_SERVLET_SIMPLE_CLASS_NAME);
			// includes.add( TEST_DATA_RESOURCE_NAME + '/' +
			// MIXED_SERVLET_SIMPLE_CLASS_NAME);
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> toJakartaRenames;

	public Map<String, String> getToJakartaRenames() {
		if (toJakartaRenames == null) {
			toJakartaRenames = new HashMap<>();
			toJakartaRenames.put(JAVAX_INJECT_PACKAGE_NAME, JAKARTA_INJECT_PACKAGE_NAME);

			toJakartaRenames.put(JAVAX_REPEAT_PACKAGE_NAME, JAKARTA_REPEAT_PACKAGE_NAME);

			toJakartaRenames.put(JAVAX_ANNO_PACKAGE_NAME, JAKARTA_ANNO_PACKAGE_NAME);
			toJakartaRenames.put(JAVAX_SERVLET_PACKAGE_NAME, JAKARTA_SERVLET_PACKAGE_NAME);
			toJakartaRenames.put(JAVAX_SERVLET_ANNO_PACKAGE_NAME, JAKARTA_SERVLET_ANNO_PACKAGE_NAME);
		}
		return toJakartaRenames;
	}

	protected Map<String, String> toJakartaPrefixes;

	public Map<String, String> getToJakartaPrefixes() {
		if (toJakartaPrefixes == null) {
			Map<String, String> useRenames = getToJakartaRenames();
			toJakartaPrefixes = new HashMap<>(useRenames.size());

			for (Map.Entry<String, String> renameEntry : useRenames.entrySet()) {
				String initialName = renameEntry.getKey();
				String finalName = renameEntry.getValue();

				String initialPrefix = 'L' + initialName.replace('.', '/');
				String finalPrefix = 'L' + finalName.replace('.', '/');

				toJakartaPrefixes.put(initialPrefix, finalPrefix);
			}
		}
		return toJakartaPrefixes;
	}

	public ClassLoader getClassLoader_null() {
		return getClass().getClassLoader();
	}

	//

	public JarActionImpl	toJakartaJarAction;
	public JarActionImpl	toJavaxJarAction;

	public JarActionImpl getJavaxToJakartaJarAction() {
		if (toJakartaJarAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			toJakartaJarAction = new JarActionImpl(useLogger, false, false, createBuffer(),
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getToJakartaRenames(), null, null, null, Collections.emptyMap()));
		}

		return toJakartaJarAction;
	}

	public JarActionImpl getJakartaToJavaxJarAction() {
		if (toJavaxJarAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> toJavaxRenames = TransformProperties.invert(getToJakartaRenames());

			toJavaxJarAction = new JarActionImpl(useLogger, false, false, createBuffer(),
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, toJavaxRenames, null, null, null, Collections.emptyMap()));
		}

		return toJavaxJarAction;
	}

	public ClassLoader getClassLoader_toJakarta() {
		JarActionImpl jarAction = getJavaxToJakartaJarAction();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public ClassLoader getClassLoader_toJavax() {
		JarActionImpl jarAction = getJakartaToJavaxJarAction();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public Class<?> testLoad(String className, ClassLoader classLoader) {
		System.out.println("Loading [ " + className + " ] using [ " + classLoader + " ]");

		@SuppressWarnings("unused")
		Class<?> objectClass;
		try {
			objectClass = classLoader.loadClass(java.lang.Object.class.getName());
		} catch (Throwable th) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + java.lang.Object.class.getName() + " ]: " + th);
			return null;
		}

		Class<?> testClass;
		try {
			testClass = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + e);
			return null;
		} catch (Throwable th) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + th);
			return null;
		}

		System.out.println("Loaded [ " + className + " ]: " + testClass);
		return testClass;
	}

	public static InputStream getResourceStream(String resourceName) throws IOException {
		InputStream inputStream = TestUtils.getResourceStream(resourceName);
		if (inputStream == null) {
			throw new IOException("Resource not found [ " + resourceName + " ]");
		}
		return inputStream;
	}

	//

	public static Map<String, String> loadRenames(String resourceRef) throws IOException {
		InputStream renamesInputStream = getResourceStream(resourceRef);
		// throws IOException

		Reader renamesReader = new InputStreamReader(renamesInputStream);

		Properties renameProperties = new Properties();
		renameProperties.load(renamesReader); // throws IOException

		Map<String, String> renames = new HashMap<>(renameProperties.size());
		for (Map.Entry<Object, Object> renameEntry : renameProperties.entrySet()) {
			String initialPackageName = (String) renameEntry.getKey();
			String finalPackageName = (String) renameEntry.getValue();
			renames.put(initialPackageName, finalPackageName);
		}

		return renames;
	}

	//

	public ClassActionImpl createToJakartaClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(useLogger, false, false, createBuffer(),
			createSelectionRule(useLogger, Collections.emptySet(), Collections.emptySet()),
			createSignatureRule(useLogger, getToJakartaRenames(), null, null, null, Collections.emptyMap()));
	}

	protected void toJakartaRewrite(String simpleClassName) throws TransformException, IOException {
		consumeCapturedEvents();

		display("Transform to Jakarta [ %s ] ...", simpleClassName);

		Map<String, String> packageRenames = getToJakartaRenames();
		display(packageRenames);

		Map<String, String> packagePrefixes = getToJakartaPrefixes();
		display(packagePrefixes);

		ClassActionImpl classAction = createToJakartaClassAction(); // throws
																	// IOException

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + simpleClassName;
		display("Reading class [ %s ]", resourceName);
		InputStream inputStream = getResourceStream(resourceName); // throws
																	// IOException

		ByteArrayOutputStream capturedInput = new ByteArrayOutputStream();
		FileUtils.transfer(inputStream, capturedInput); // throws IOException
		byte[] inputBytes = capturedInput.toByteArray();
		display("Input class size [ %s ]", inputBytes.length);
		ClassFile inputClass = parse(inputBytes); // throws IOException
		display(inputClass);

		display("Transforming class [ %s ]", resourceName);
		ByteArrayInputStream internalInputStream = new ByteArrayInputStream(inputBytes);
		InputStreamData outputStreamData = classAction.apply(resourceName, internalInputStream); // throws
																									// TransformException
		display(classAction.getLastActiveChanges());

		ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
		FileUtils.transfer(outputStreamData.stream, capturedOutput); // throws
																		// IOException
		byte[] outputBytes = capturedOutput.toByteArray();
		display("Output class size [ %s ]", outputBytes.length);
		ClassFile outputClass = parse(outputBytes); // throws IOException
		display(outputClass);

		validateAnnotations(packageRenames, packagePrefixes, inputClass, outputClass);

		File outputFile = new File("build" + '/' + simpleClassName);
		display("Writing transformed class [ %s ]", outputFile.getAbsolutePath());
		try (OutputStream outputStream = new FileOutputStream(outputFile)) { // throws
																				// FileNotFoundException
			capturedOutput.writeTo(outputStream); // throws IOException
		}

		display("Transform to Jakarta [ %s ] ... done", simpleClassName);
	}

	protected ClassFile parse(byte[] classBytes) throws IOException {
		DataInput inputClassData = ByteBufferDataInput.wrap(classBytes);
		return ClassFile.parseClassFile(inputClassData); // throws IOException
	}

	protected void display(String msg, Object... parms) {
		if (parms.length == 0) {
			System.out.println(msg);
		} else {
			System.out.println(String.format(msg, parms));
		}
	}

	protected void display(ClassFile classFile) throws IOException {
		display("Class [ %s ] ", classFile.this_class);
		display("  Super [ %s ]", classFile.super_class);
		if (classFile.interfaces != null) {
			display("  Interfaces [ %s ]", classFile.interfaces.length);
			for (String interfaceName : classFile.interfaces) {
				display("    [ %s ]", interfaceName);
			}
		}
		display(classFile.attributes);

		if (classFile.fields != null) {
			display("  Fields [ %s ]", classFile.fields.length);
			for (FieldInfo field : classFile.fields) {
				display("    Field [ %s ] [ %s ]", field.name, field.descriptor);
				display(field.attributes);
			}
		}
		if (classFile.methods != null) {
			display("  Methods [ %s ]", classFile.methods.length);
			for (MethodInfo method : classFile.methods) {
				display("    Method [ %s ] [ %s ]", method.name, method.descriptor);
				display(method.attributes);
			}
		}
	}

	private void display(Attribute[] attributes) {
		if ((attributes == null) || (attributes.length == 0)) {
			return;
		}

		display("    Attributes [ %s ]", attributes.length);

		for (Attribute attribute : attributes) {
			display("      [ %s ] [ %s ]", attribute.getClass(), attribute.name());
			if (attribute instanceof AnnotationsAttribute) {
				display((AnnotationsAttribute) attribute);
			}
		}
	}

	private void display(AnnotationsAttribute attribute) {
		display("    Annotations [ %s ]", attribute.annotations.length);
		int numAnno = attribute.annotations.length;
		for (int annoNo = 0; annoNo < numAnno; annoNo++) {
			AnnotationInfo annoInfo = attribute.annotations[annoNo];
			display("      Annotation [ %s ]", annoInfo.type);

			int numValues = annoInfo.values.length;
			for (int valueNo = 0; valueNo < numValues; valueNo++) {
				ElementValueInfo valueInfo = annoInfo.values[valueNo];
				display("        [ %s ] [ %s ]", valueInfo.name, valueInfo.value);
			}
		}
	}

	private void display(Map<String, String> packageRenames) {
		display("Package renames [ %s ]", packageRenames.size());
		for (Map.Entry<String, String> renameEntry : packageRenames.entrySet()) {
			display("  [ %s ] -> [ %s ]", renameEntry.getKey(), renameEntry.getValue());
		}
	}

	protected void validateAnnotations(Map<String, String> packageRenames, Map<String, String> packagePrefixes,
		ClassFile inputClass, ClassFile outputClass) {

		display("Validating package renames on [ %s ] ...", inputClass.this_class);
		display("  Package renames [ %s ]", packageRenames.size());

		validateAnnotations(packageRenames, packagePrefixes, inputClass.attributes, outputClass.attributes);

		display("  Fields [ %s ]", inputClass.fields.length, outputClass.fields.length);

		int numFields = inputClass.fields.length;
		for (int fieldNo = 0; fieldNo < numFields; fieldNo++) {
			FieldInfo inputField = inputClass.fields[fieldNo];
			FieldInfo outputField = outputClass.fields[fieldNo];
			display("    [ %s ] [ %s ]", inputField.name, outputField.name);
			validateAnnotations(packageRenames, packagePrefixes, inputField.attributes, outputField.attributes);
		}

		display("  Methods[ %s ]", inputClass.methods.length, outputClass.methods.length);

		int numMethods = inputClass.methods.length;

		for (int methodNo = 0; methodNo < numMethods; methodNo++) {
			MethodInfo inputMethod = inputClass.methods[methodNo];
			MethodInfo outputMethod = outputClass.methods[methodNo];
			display("    [ %s ] [ %s ]", inputMethod.name, outputMethod.name);
			validateAnnotations(packageRenames, packagePrefixes, inputMethod.attributes, outputMethod.attributes);
		}

		System.out.println("Validating package renames on [ " + inputClass.this_class + " ] ... done");
	}

	public void validateAnnotations(Map<String, String> packageRenames, Map<String, String> packagePrefixes,
		Attribute[] inputAttributes, Attribute[] outputAttributes) {

		display("    Attributes [ %s ] [ %s ]", inputAttributes.length, outputAttributes.length);

		for (int attrNo = 0; attrNo < inputAttributes.length; attrNo++) {
			Attribute inputAttr = inputAttributes[attrNo];
			Attribute outputAttr = outputAttributes[attrNo];

			display("    [ %s ] [ %s ]", inputAttr.getClass(), outputAttr.getClass());

			if (inputAttr instanceof AnnotationsAttribute) {
				validateAnnotation(packageRenames, packagePrefixes, (AnnotationsAttribute) inputAttr,
					(AnnotationsAttribute) outputAttr);
			}
		}
	}

	public void validateAnnotation(Map<String, String> packageRenames, Map<String, String> packagePrefixes,
		AnnotationsAttribute inputAttributes, AnnotationsAttribute outputAttributes) {

		display("      Annotations [ %s ] [ %s ]", inputAttributes.annotations.length,
			outputAttributes.annotations.length);

		int numAnno = inputAttributes.annotations.length;
		for (int annoNo = 0; annoNo < numAnno; annoNo++) {
			AnnotationInfo inputAnno = inputAttributes.annotations[annoNo];
			AnnotationInfo outputAnno = outputAttributes.annotations[annoNo];

			display("        [ %s ] [ %s ]", inputAnno.type, outputAnno.type);

			verifyRename(packageRenames, packagePrefixes, inputAnno.type, outputAnno.type);

			validateAnnotationValues(packageRenames, packagePrefixes, inputAnno.values, outputAnno.values);
		}
	}

	public void verifyRename(Map<String, String> packageRenames, Map<String, String> packagePrefixes, String inputType,
		String outputType) {

		for (Map.Entry<String, String> prefixEntry : packagePrefixes.entrySet()) {
			String initialPrefix = prefixEntry.getKey();
			if (!inputType.startsWith(initialPrefix)) {
				continue;
			}
			String finalPrefix = prefixEntry.getValue();

			if (!outputType.startsWith(finalPrefix)) {
				Assertions.fail("Input type [ " + inputType + " ] matches [ " + initialPrefix + " ]; "
					+ "output type [ " + outputType + " ] does not match [ " + finalPrefix + " ]");
			} else {
				display("Type [ %s ] -> [ %s ]; ([ %s ] -> [ %s ])", inputType, outputType, initialPrefix, finalPrefix);
			}
		}
	}

	public void validateAnnotationValues(Map<String, String> packageRenames, Map<String, String> packagePrefixes,
		ElementValueInfo[] inputValues, ElementValueInfo[] outputValues) {

		display("        Values [ %s ] [ %s ]", inputValues.length, outputValues.length);

		int numValues = inputValues.length;
		for (int valueNo = 0; valueNo < numValues; valueNo++) {
			ElementValueInfo inputValue = inputValues[valueNo];
			ElementValueInfo outputValue = outputValues[valueNo];

			display("          [ %s ] [ %s ]", inputValue.value, outputValue.value);
		}
	}

	// @Test
	// public void testAnnotatedServlet() throws TransformException, IOException
	// {
	// toJakartaRewrite(ANNOTATED_SERVLET_SIMPLE_CLASS_NAME); // throws
	// TransformException, IOException
	// }

	// @Test
	// public void testMixedServlet() throws TransformException, IOException {
	// toJakartaRewrite(MIXED_SERVLET_SIMPLE_CLASS_NAME); // throws
	// TransformException, IOException
	// }

	//

	public void display(ClassChangesImpl classChanges) {
		System.out.println("Input class [ " + classChanges.getInputClassName() + " ]");
		System.out.println("Output class [ " + classChanges.getOutputClassName() + " ]");

		System.out.println("Input super class [ " + classChanges.getInputSuperName() + " ]");
		System.out.println("Output super class [ " + classChanges.getOutputSuperName() + " ]");

		System.out.println("Modified interfaces [ " + classChanges.getModifiedInterfaces() + " ]");
		System.out.println("Modified fields [ " + classChanges.getModifiedFields() + " ]");
		System.out.println("Modified methods [ " + classChanges.getModifiedMethods() + " ]");
		System.out.println("Modified constants [ " + classChanges.getModifiedConstants() + " ]");
		System.out.println("Modified attributes [ " + classChanges.getModifiedAttributes() + " ]");
	}

	//

	public static final Map<String, String> DIRECT_STRINGS;
	static {
		DIRECT_STRINGS = new HashMap<>();
		DIRECT_STRINGS.put("DIRECT_1", "DIRECT_X"); // String value update // 2
													// hits
		DIRECT_STRINGS.put("DIRECT21", "DIRECT21"); // Variable name update // 2
													// hits

		DIRECT_STRINGS.put("Sample value 1", "Sample value 2"); // String
																// constant
																// reference
																// update // 1
																// hit
		DIRECT_STRINGS.put("Sample", "Official"); // String constant reference
													// update (not found) // 0
													// hits
		DIRECT_STRINGS.put("value", "product"); // String constant reference
												// update (not found) // 0 hits
	}

	public static final String PER_CLASS_RESOURCE_NAME = "Sample_PerClassConstant.class";
	public static final Map<String, Map<String, String>> PER_CLASS_CONSTANT_MASTER;

	static {
		PER_CLASS_CONSTANT_MASTER = new HashMap();
		Map<String, String> mapping = new HashMap<>();
		mapping.put("javax.servlet", "jakarta.servlet");
		PER_CLASS_CONSTANT_MASTER.put(TEST_DATA_RESOURCE_NAME + '/' + PER_CLASS_RESOURCE_NAME, mapping);
	}

	public static final Map<String, String> getDirectStrings() {
		return DIRECT_STRINGS;
	}

	public ClassActionImpl createDirectClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(useLogger, false, false, createBuffer(),
			createSelectionRule(useLogger, Collections.emptySet(), Collections.emptySet()),
			createSignatureRule(useLogger, Collections.emptyMap(), null, null, getDirectStrings(), Collections.emptyMap()));
	}

	public ClassActionImpl createPerClassConstantClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(useLogger, false, false, createBuffer(),
				createSelectionRule(useLogger, Collections.emptySet(), Collections.emptySet()),
				createSignatureRule(useLogger, Collections.emptyMap(), null, null, null, PER_CLASS_CONSTANT_MASTER));
	}

	public static final String DIRECT_STRINGS_RESOURCE_NAME = "Sample_DirectStrings.class";

	@Test
	public void testDirectStrings() throws TransformException, IOException {
		consumeCapturedEvents();

		ClassActionImpl classAction = createDirectClassAction();

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + DIRECT_STRINGS_RESOURCE_NAME;
		InputStream inputStream = getResourceStream(resourceName); // throws
																	// IOException

		@SuppressWarnings("unused")
		InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws
																							// TransformException
		display(classAction.getLastActiveChanges());

		int expectedChanges = 5;
		int actualChanges = classAction.getLastActiveChanges()
			.getModifiedConstants();
		Assertions.assertEquals(expectedChanges, actualChanges, "Incorrect count of constant changes");
	}

	@Test
	public void testPerClassConstant() throws TransformException, IOException {
		consumeCapturedEvents();

		ClassActionImpl classAction = createPerClassConstantClassAction();

		{
			String resourceName = TEST_DATA_RESOURCE_NAME + '/' + PER_CLASS_RESOURCE_NAME;
			InputStream inputStream = getResourceStream(resourceName); // throws IOException

			@SuppressWarnings("unused")
			InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws TransformException
			display(classAction.getLastActiveChanges());

			// 2 to pass although should be 1. Both UTF8 and ConstantString are counted.
			int expectedChanges = 2;
			int actualChanges = classAction.getLastActiveChanges()
					.getModifiedConstants();
			Assertions.assertEquals(expectedChanges, actualChanges, "Incorrect count of constant changes");
		}

		{
			String resourceName = TEST_DATA_RESOURCE_NAME + '/' + DIRECT_STRINGS_RESOURCE_NAME;
			InputStream inputStream = getResourceStream(resourceName); // throws IOException

			@SuppressWarnings("unused")
			InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws TransformException
			display(classAction.getLastActiveChanges());

			int expectedChanges = 0;
			int actualChanges = classAction.getLastActiveChanges()
					.getModifiedConstants();
			Assertions.assertEquals(expectedChanges, actualChanges, "Incorrect count of constant changes");
		}

	}

	public static final boolean IS_EXACT = false;

	public static class ClassRelocation {
		public final String		inputPath;
		public final String		inputName;

		public final String		outputName;
		public final String		outputPath;

		public final boolean	isApproximate;

		public ClassRelocation(String inputPath, String inputName, String outputName, String outputPath,
			boolean isApproximate) {

			this.inputPath = inputPath;
			this.inputName = inputName;

			this.outputName = outputName;
			this.outputPath = outputPath;

			this.isApproximate = isApproximate;
		}
	}

	public static ClassRelocation[]	RELOCATION_CASES	= new ClassRelocation[] {
		new ClassRelocation("com/ibm/test/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation("WEB-INF/classes/com/ibm/test/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"WEB-INF/classes/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation("META-INF/versions/9/com/ibm/test/Sample.class", "com.ibm.test.Sample",
			"com.ibm.prod.Sample", "META-INF/versions/9/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation("META-INF/versions/com/ibm/test/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"META-INF/versions/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation("sample/com/ibm/test/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"sample/com/ibm/prod/Sample.class", IS_EXACT),

		// new ClassRelocation(
		// "com/ibm/broken/Sample.class", "com.ibm.test.Sample",
		// "com.ibm.prod.Sample", "com/ibm/prod/Sample.class", !IS_EXACT),
		// new ClassRelocation(
		// "WEB-INF/classes/com/ibm/broken/Sample.class", "com.ibm.test.Sample",
		// "com.ibm.prod.Sample", "WEB-INF/classes/com/ibm/prod/Sample.class",
		// !IS_EXACT),
		// new ClassRelocation(
		// "META-INF/versions/9/com/ibm/broken/Sample.class",
		// "com.ibm.test.Sample",
		// "com.ibm.prod.Sample",
		// "META-INF/versions/9/com/ibm/prod/Sample.class", !IS_EXACT),

	};

	public static final String		APPROXIMATE_TEXT	= "Approximate relocation of class";

	@Test
	public void testClassRelocation() {
		for (ClassRelocation relocationCase : RELOCATION_CASES) {
			String outputPath = ClassActionImpl.relocateClass(getCaptureLogger(), relocationCase.inputPath,
				relocationCase.inputName, relocationCase.outputName);

			List<? extends CaptureLoggerImpl.LogEvent> capturedEvents = consumeCapturedEvents();

			System.out.printf("Relocation [ %s ] as [ %s ]\n" + "        to [ %s ] as [ %s ]\n",
				relocationCase.inputPath, relocationCase.inputName, relocationCase.outputName, outputPath);

			boolean capturedApproximate = false;
			for (CaptureLoggerImpl.LogEvent event : capturedEvents) {
				System.out.printf("Captured Event [ %s ]\n", event);
				if (event.message.contains(APPROXIMATE_TEXT)) {
					capturedApproximate = true;
				}
			}

			Assertions.assertEquals(relocationCase.outputPath, outputPath, "Incorrect output path");

			Assertions.assertEquals(capturedApproximate, relocationCase.isApproximate, "Approximate error not logged");
		}
	}

	//

	public static Map<String, String> getStandardRenames() throws IOException {
		String transformerResourceName = JakartaTransformer.class.getPackage()
			.getName()
			.replace('.', '/');

		String renamesResourceName = transformerResourceName + '/' + JakartaTransformer.DEFAULT_RENAMES_REFERENCE;

		return loadRenames(renamesResourceName);
	}

	public ClassActionImpl createStandardClassAction() throws IOException {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(useLogger, false, false, createBuffer(),
			createSelectionRule(useLogger, Collections.emptySet(), Collections.emptySet()),
			createSignatureRule(useLogger, getStandardRenames(), null, null, null, Collections.emptyMap()));
		// 'getStandardRenames' throws IOException
	}

}
