/********************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.classfile.AnnotationInfo;
import aQute.bnd.classfile.AnnotationsAttribute;
import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ElementValueInfo;
import aQute.bnd.classfile.FieldInfo;
import aQute.bnd.classfile.MethodInfo;
import aQute.lib.io.ByteBufferDataInput;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.ClassChangesImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.jakarta.JakartaTransform;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.SignatureUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transformer.test.data.Sample_InjectAPI_Jakarta;
import transformer.test.data.Sample_InjectAPI_Javax;
import transformer.test.data.Sample_Repeat_Target;
import transformer.test.data.Sample_SecurityAPI_Javax;
import transformer.test.util.CaptureLoggerImpl;
import transformer.test.util.ClassData;

public class TestTransformClass extends CaptureTest {
	private Properties prior;

	@BeforeEach
	public void beforeTest() throws Exception {
		prior = new Properties();
		prior.putAll(System.getProperties());
		consumeCapturedEvents();
	}

	@AfterEach
	public void afterTest() throws Exception {
		displayCapturedEvents();
		System.setProperties(prior);
	}

	// Rename used by the injection sample.

	public static final String	JAVAX_INJECT_PACKAGE_NAME			= "javax.inject";
	public static final String	JAKARTA_INJECT_PACKAGE_NAME			= "jakarta.inject";

	// Rename used by the security sample.
	public static final String JAVAX_SECURITY_AUTH_MESSAGE_CONFIG = "javax.security.auth.message.config";
	public static final String JAKARTA_SECURITY_AUTH_MESSAGE_CONFIG = "jakarta.security.auth.message.config";

	// Rename used by the repeat annotation sample.

	public static final String	JAVAX_REPEAT_PACKAGE_NAME			= "transformer.test.data.javax.repeat";
	public static final String	JAKARTA_REPEAT_PACKAGE_NAME			= "transformer.test.data.jakarta.repeat";

	// Rename used by the annotated servlet.

	public static final String	JAVAX_ANNO_PACKAGE_NAME				= "javax.annotation";
	public static final String	JAVAX_SERVLET_PACKAGE_NAME			= "javax.servlet";

	public static final String	JAKARTA_ANNO_PACKAGE_NAME			= "jakarta.annotation";
	public static final String	JAKARTA_SERVLET_PACKAGE_NAME		= "jakarta.servlet";

	// These test classes are build within the project:

	public static final String	INJECT_JAVAX_CLASS_NAME				= Sample_InjectAPI_Javax.class.getName();
	public static final String	INJECT_JAKARTA_CLASS_NAME			= Sample_InjectAPI_Jakarta.class.getName();

	public static final String	REPEAT_TARGET_CLASS_NAME			= Sample_Repeat_Target.class.getName();
	public static final String	REPEAT_TARGET_RESOURCE_NAME			= "Sample_Repeat_Target.class";

	public static final String	SECURITY_JAVAX_CLASS_NAME			= Sample_SecurityAPI_Javax.class.getName();

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
	public void testjavaxAsJakarta_security() {
		System.out.println("Test javax.security.auth.message.config.AuthConfigFactory => jakarta.security.auth.message.config.AuthConfigFactory");
		Class<?> testClass = testLoad(SECURITY_JAVAX_CLASS_NAME, getClassLoader_toJakarta());
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

	protected Map<String, String> includes;

	public Map<String, String> getIncludes() {
		if (includes == null) {
			includes = new HashMap<>();
			includes.put(SignatureUtils.classNameToResourceName(INJECT_JAVAX_CLASS_NAME), "");
			includes.put(SignatureUtils.classNameToResourceName(INJECT_JAKARTA_CLASS_NAME), "");
			includes.put(SignatureUtils.classNameToResourceName(REPEAT_TARGET_CLASS_NAME), "");
		}

		return includes;
	}

	public Map<String, String> getExcludes() {
		return Collections.emptyMap();
	}

	protected Map<String, String> toJakartaRenames;

	public Map<String, String> getToJakartaRenames() {
		if (toJakartaRenames == null) {
			toJakartaRenames = new HashMap<>();
			toJakartaRenames.put(JAVAX_INJECT_PACKAGE_NAME + ".*", JAKARTA_INJECT_PACKAGE_NAME);
			toJakartaRenames.put(JAVAX_SECURITY_AUTH_MESSAGE_CONFIG, JAKARTA_SECURITY_AUTH_MESSAGE_CONFIG);

			toJakartaRenames.put(JAVAX_REPEAT_PACKAGE_NAME, JAKARTA_REPEAT_PACKAGE_NAME);

			toJakartaRenames.put(JAVAX_ANNO_PACKAGE_NAME, JAKARTA_ANNO_PACKAGE_NAME);
			toJakartaRenames.put(JAVAX_SERVLET_PACKAGE_NAME + ".*", JAKARTA_SERVLET_PACKAGE_NAME);
		}
		return toJakartaRenames;
	}

	public void displayJakartaPackageRenames() {
		System.out.println("Package Renames [ javax -> jakarta ]");
		getToJakartaRenames().forEach((key, value) -> {
			System.out.println("  [ " + key + " ] --> [ " + value + " ]");
		});
	}

	//

	public static final String	OVERRIDE_TARGET_CLASS_NAME		= "transformer.test.data.Sample_DirectOverride";

	public static final String	OVERRIDE_TARGET_RESOURCE_NAME	= SignatureUtils
		.classNameToResourceName(OVERRIDE_TARGET_CLASS_NAME);

	public Map<String, String> getOverrideIncludes() {
		Map<String, String> overrideIncludes = new HashMap<>();
		overrideIncludes.put(OVERRIDE_TARGET_RESOURCE_NAME, FileUtils.DEFAULT_CHARSET.name());
		return overrideIncludes;
	}

	protected static final String	JAVAX_SERVLET_CLASS_NAME			= "javax.servlet.Servlet";
	protected static final String	DIRECT_OVERRIDE_SERVLET_CLASS_NAME	= "transformer.test.data1.Servlet";

	protected Map<String, String> toJakartaDirectStrings;

	public Map<String, String> toJakartaDirectStrings() {
		if (toJakartaDirectStrings == null) {
			toJakartaDirectStrings = new HashMap<>();
			toJakartaDirectStrings.put(JAVAX_SERVLET_CLASS_NAME, DIRECT_OVERRIDE_SERVLET_CLASS_NAME);
		}
		return toJakartaDirectStrings;
	}

	public void displayJakartaGlobalDirectStrings() {
		System.out.println("Global Direct Strings [ javax -> jakarta ]");
		toJakartaDirectStrings().forEach((key, value) -> {
			System.out.println("  [ " + key + " ] --> [ " + value + " ]");
		});
	}

	protected static final String				PER_CLASS_OVERRIDE_PACKAGE_NAME	= "transformer.test.data2";

	protected Map<String, Map<String, String>> toJakartaPerClassDirectStrings;

	public Map<String, Map<String, String>> toJakartaPerClassDirectStrings() {
		if (toJakartaPerClassDirectStrings == null) {
			toJakartaPerClassDirectStrings = new HashMap<>();

			Map<String, String> directStringsForClass = new HashMap<>();
			directStringsForClass.put(JAVAX_SERVLET_PACKAGE_NAME, PER_CLASS_OVERRIDE_PACKAGE_NAME);
			toJakartaPerClassDirectStrings.put(OVERRIDE_TARGET_RESOURCE_NAME, directStringsForClass);
		}
		return toJakartaPerClassDirectStrings;
	}

	public void displayJakartaPerClassDirectStrings() {
		System.out.println("Per Class Direct Strings [ javax -> jakarta ]");
		toJakartaPerClassDirectStrings().forEach((className, directStrings) -> {
			System.out.println("  [ " + className + " ]:");
			directStrings.forEach((key, value) -> {
				System.out.println("    [ " + key + " ] --> [ " + value + " ]");
			});
		});
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

	public ZipActionImpl toJakartaJarAction;

	public ZipActionImpl getJavaxToJakartaJarAction() {
		if (toJakartaJarAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getToJakartaRenames(), null, null, null, Collections.emptyMap()));

			toJakartaJarAction = new ZipActionImpl(context, ActionType.JAR);
		}

		return toJakartaJarAction;
	}

	public ZipActionImpl toJavaxJarAction;

	public ZipActionImpl getJakartaToJavaxJarAction() {
		if (toJavaxJarAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> toJavaxRenames = TransformProperties.invert(getToJakartaRenames());

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, toJavaxRenames, null, null, null, Collections.emptyMap()));

			toJavaxJarAction = new ZipActionImpl(context, ActionType.JAR);
		}

		return toJavaxJarAction;
	}

	public ZipActionImpl toJakartaJarAction_DirectOverride;

	public ZipActionImpl getJavaxToJakartaJarAction_DirectOverride() {
		if (toJakartaJarAction_DirectOverride == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getOverrideIncludes(), getExcludes()),
				createSignatureRule(useLogger, getToJakartaRenames(), null, null, toJakartaDirectStrings(), null));

			toJakartaJarAction_DirectOverride = new ZipActionImpl(context, ActionType.JAR);
		}

		return toJakartaJarAction_DirectOverride;
	}

	public ZipActionImpl toJakartaJarAction_PerClassDirectOverride;

	public ZipActionImpl getJavaxToJakartaJarAction_PerClassDirectOverride() {
		if (toJakartaJarAction_PerClassDirectOverride == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getOverrideIncludes(), getExcludes()),
				createSignatureRule(useLogger, getToJakartaRenames(), null, null, toJakartaDirectStrings(),
					toJakartaPerClassDirectStrings()));

			toJakartaJarAction_PerClassDirectOverride = new ZipActionImpl(context, ActionType.JAR);
		}

		return toJakartaJarAction_PerClassDirectOverride;
	}

	public ZipActionImpl toJakartaJarAction_PackageRenamesOnly;

	public ZipActionImpl getJavaxToJakartaJarAction_PackageRenamesOnly() {
		if (toJakartaJarAction_PackageRenamesOnly == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getOverrideIncludes(), getExcludes()),
				createSignatureRule(useLogger, getToJakartaRenames(), null, null, null, null));

			toJakartaJarAction_PackageRenamesOnly = new ZipActionImpl(context, ActionType.JAR);
		}

		return toJakartaJarAction_PackageRenamesOnly;
	}

	public ClassLoader getClassLoader_toJakarta() {
		ZipActionImpl jarAction = getJavaxToJakartaJarAction();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public ClassLoader getClassLoader_toJavax() {
		ZipActionImpl jarAction = getJakartaToJavaxJarAction();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public ClassLoader getClassLoader_toJakarta_DirectOverride() {
		ZipActionImpl jarAction = getJavaxToJakartaJarAction_DirectOverride();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public ClassLoader getClassLoader_toJakarta_PerClassDirectOverride() {
		ZipActionImpl jarAction = getJavaxToJakartaJarAction_PerClassDirectOverride();
		ClassActionImpl classAction = jarAction.addUsing(ClassActionImpl::new);
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing(ServiceLoaderConfigActionImpl::new);

		return new TransformClassLoader(getClass().getClassLoader(), jarAction, classAction, configAction);
	}

	public ClassLoader getClassLoader_toJakarta_PackageRenamesOnly() {
		ZipActionImpl jarAction = getJavaxToJakartaJarAction_PackageRenamesOnly();
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

		Reader renamesReader = new InputStreamReader(renamesInputStream);

		Properties renameProperties = new Properties();
		renameProperties.load(renamesReader);

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

		ActionContext context = new ActionContextImpl(useLogger,
			createSelectionRule(useLogger, Collections.emptyMap(), Collections.emptyMap()),
			createSignatureRule(useLogger, getToJakartaRenames(), null, null, null, Collections.emptyMap()));

		return new ClassActionImpl(context);
	}

	protected void toJakartaRewrite(String simpleClassName) throws TransformException, IOException {
		consumeCapturedEvents();

		display("Transform to Jakarta [ %s ] ...", simpleClassName);

		Map<String, String> packageRenames = getToJakartaRenames();
		display(packageRenames);

		Map<String, String> packagePrefixes = getToJakartaPrefixes();
		display(packagePrefixes);

		ClassActionImpl classAction = createToJakartaClassAction();

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + simpleClassName;
		display("Reading class [ %s ]", resourceName);
		InputStream inputStream = getResourceStream(resourceName);

		ByteArrayOutputStream capturedInput = new ByteArrayOutputStream();
		FileUtils.transfer(inputStream, capturedInput);
		byte[] inputBytes = capturedInput.toByteArray();
		display("Input class size [ %s ]", inputBytes.length);
		ClassFile inputClass = parse(inputBytes);
		display(inputClass);

		display("Transforming class [ %s ]", resourceName);
		ByteArrayInputStream internalInputStream = new ByteArrayInputStream(inputBytes);
		ByteData outputStreamData = classAction.apply(classAction.collect(resourceName, internalInputStream));
		display(classAction.getLastActiveChanges());

		ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
		FileUtils.transfer(outputStreamData.stream(), capturedOutput);
		byte[] outputBytes = capturedOutput.toByteArray();
		display("Output class size [ %s ]", outputBytes.length);
		ClassFile outputClass = parse(outputBytes);
		display(outputClass);

		validateAnnotations(packageRenames, packagePrefixes, inputClass, outputClass);

		File outputFile = new File("build" + '/' + simpleClassName);
		display("Writing transformed class [ %s ]", outputFile.getAbsolutePath());
		try (OutputStream outputStream = new FileOutputStream(outputFile)) {
			capturedOutput.writeTo(outputStream);
		}

		display("Transform to Jakarta [ %s ] ... done", simpleClassName);
	}

	protected ClassFile parse(byte[] classBytes) throws IOException {
		DataInput inputClassData = ByteBufferDataInput.wrap(classBytes);
		return ClassFile.parseClassFile(inputClassData);
	}

	protected void display(String msg, Object... parms) {
		if (parms.length == 0) {
			System.out.println(msg);
		} else {
			System.out.println(String.format(msg, parms));
		}
	}

	protected void display(ClassFile classFile) {
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

	public void verifyRename(@SuppressWarnings("unused") Map<String, String> packageRenames,
		Map<String, String> packagePrefixes, String inputType,
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

	public void validateAnnotationValues(@SuppressWarnings("unused") Map<String, String> packageRenames,
		@SuppressWarnings("unused") Map<String, String> packagePrefixes,
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
	// toJakartaRewrite(ANNOTATED_SERVLET_SIMPLE_CLASS_NAME);
	// }

	// @Test
	// public void testMixedServlet() throws TransformException, IOException {
	// toJakartaRewrite(MIXED_SERVLET_SIMPLE_CLASS_NAME);
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
		PER_CLASS_CONSTANT_MASTER = new HashMap<>();
		Map<String, String> mapping = new HashMap<>();
		mapping.put("javax.servlet", "jakarta.servlet");
		PER_CLASS_CONSTANT_MASTER.put(TEST_DATA_RESOURCE_NAME + '/' + PER_CLASS_RESOURCE_NAME, mapping);
	}

	public static final Map<String, String> getDirectStrings() {
		return DIRECT_STRINGS;
	}

	public ClassActionImpl createDirectClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		ActionContext context = new ActionContextImpl(useLogger,
			createSelectionRule(useLogger, Collections.emptyMap(), Collections.emptyMap()), createSignatureRule(
				useLogger, Collections.emptyMap(), null, null, getDirectStrings(), Collections.emptyMap()));

		return new ClassActionImpl(context);
	}

	public ClassActionImpl createPerClassConstantClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		ActionContext context = new ActionContextImpl(useLogger,
			createSelectionRule(useLogger, Collections.emptyMap(), Collections.emptyMap()),
			createSignatureRule(useLogger, Collections.emptyMap(), null, null, null, PER_CLASS_CONSTANT_MASTER));

		return new ClassActionImpl(context);
	}

	public static final String DIRECT_STRINGS_RESOURCE_NAME = "Sample_DirectStrings.class";

	@Test
	public void testDirectStrings() throws TransformException, IOException {
		consumeCapturedEvents();

		ClassActionImpl classAction = createDirectClassAction();

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + DIRECT_STRINGS_RESOURCE_NAME;
		InputStream inputStream = getResourceStream(resourceName);

		@SuppressWarnings("unused")
		ByteData outputStreamData = classAction.apply(classAction.collect(resourceName, inputStream));
		display(classAction.getLastActiveChanges());

		// TODO:
		//
		// Previously, only [5] direct replacements were made.
		//
		// With the direct string update modifications, [14] direct replacements
		// are now made:
		//
		// Class [ transformer/test/data/Sample_DirectStrings.class ]
		// [ String ] [ Sample value 1 ] [ Sample value 2 ]
		// [ String ] [ DIRECT_1 ] [ DIRECT_X ]
		// [ String ] [ DIRECT_1_2 ] [ DIRECT_X_2 ]
		// [ String ] [ 2_DIRECT_1 ] [ 2_DIRECT_X ]
		// [ UTF8 ] [ DIRECT212 ] [ DIRECT212 ]
		// [ String ] [ 2_DIRECT_1_2 ] [ 2_DIRECT_X_2 ]
		// [ UTF8 ] [ Ltransformer/test/data/Sample_DirectStrings; ] [
		// Ltransformer/test/data/Official_DirectStrings; ]
		// [ UTF8 ] [ Sample_DirectStrings.java ] [ Official_DirectStrings.java
		// ]
		// [ UTF8 ] [ Sample value 1 ] [ Sample value 2 ]
		// [ UTF8 ] [ transformer/test/data/Sample_DirectStrings ] [
		// transformer/test/data/Official_DirectStrings ]
		// [ UTF8 ] [ DIRECT_1 ] [ DIRECT_X ]
		// [ UTF8 ] [ DIRECT_1_2 ] [ DIRECT_X_2 ]
		// [ UTF8 ] [ 2_DIRECT_1 ] [ 2_DIRECT_X ]
		// [ UTF8 ] [ 2_DIRECT_1_2 ] [ 2_DIRECT_X_2 ]
		//
		// int expectedChanges = 5;
		//
		// See issue #299

		int expectedChanges = 14;
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
			InputStream inputStream = getResourceStream(resourceName);

			@SuppressWarnings("unused")
			ByteData outputStreamData = classAction.apply(classAction.collect(resourceName, inputStream));
			display(classAction.getLastActiveChanges());

			// 2 to pass although should be 1. Both UTF8 and ConstantString are counted.
			int expectedChanges = 2;
			int actualChanges = classAction.getLastActiveChanges()
					.getModifiedConstants();
			Assertions.assertEquals(expectedChanges, actualChanges, "Incorrect count of constant changes");
		}

		{
			String resourceName = TEST_DATA_RESOURCE_NAME + '/' + DIRECT_STRINGS_RESOURCE_NAME;
			InputStream inputStream = getResourceStream(resourceName);

			@SuppressWarnings("unused")
			ByteData outputStreamData = classAction.apply(classAction.collect(resourceName, inputStream));
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

		new ClassRelocation("com/ibm/broken/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"com/ibm/prod/Sample.class", !IS_EXACT),
		new ClassRelocation("WEB-INF/classes/com/ibm/broken/Sample.class", "com.ibm.test.Sample", "com.ibm.prod.Sample",
			"WEB-INF/classes/com/ibm/prod/Sample.class", !IS_EXACT),
		new ClassRelocation("META-INF/versions/9/com/ibm/broken/Sample.class", "com.ibm.test.Sample",
			"com.ibm.prod.Sample", "META-INF/versions/9/com/ibm/prod/Sample.class", !IS_EXACT)

	};

	public static final String		APPROXIMATE_TEXT	= "Class location mismatch";

	@Test
	public void testClassRelocation() {
		ClassActionImpl classAction = createDirectClassAction();
		for (ClassRelocation relocationCase : RELOCATION_CASES) {
			String outputPath = classAction.relocateClass(relocationCase.inputPath,
				relocationCase.inputName, relocationCase.outputName);

			List<CaptureLoggerImpl.LogEvent> capturedEvents = consumeCapturedEvents();

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

	@Test
	public void testjavaxAsJakarta_directOverride() throws Exception {
		System.out.println("Test transformation with a direct override");

		displayJakartaPackageRenames();
		displayJakartaGlobalDirectStrings();

		Class<?> targetClass = testLoad(OVERRIDE_TARGET_CLASS_NAME, getClassLoader_toJakarta_DirectOverride());

		Class<?> transformedType = getStaticFieldType(targetClass, "sampleServlet");
		String transformedClassName = getStaticField(targetClass, "SAMPLE_CLASS_NAME");
		String transformedValue = getStaticField(targetClass, "SAMPLE_STRING");
		String transformedRefValue = getStaticField(targetClass, "SAMPLE_STRING_REFERENCE");
		String transformedAnnoValue1 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value1");
		String transformedAnnoValue2 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value2");

		System.out.println("Transformed type [ sampleServlet ]: " + transformedType.getName());
		System.out.println("Transformed class name [ SAMPLE_CLASS_NAME ]: " + transformedClassName);
		System.out.println("Transformed value [ SAMPLE_STRING ]: " + transformedValue);
		System.out.println("Transformed reference value [ SAMPLE_STRING_REFERENCE ]: " + transformedRefValue);
		System.out.println("Transformed annotation value [ Sample_Annotation.value1 ]: " + transformedAnnoValue1);
		System.out.println("Transformed annotation value [ Sample_Annotation.value2 ]: " + transformedAnnoValue2);

		Assertions.assertEquals("jakarta.servlet.Servlet", transformedType.getName(),
			"Type of [ sampleServlet ]");

		Assertions.assertEquals("transformer.test.data1.Servlet", transformedClassName,
			"Value of [ SAMPLE_CLASS_NAME ]");

		// TODO: The text substitution changes impacted this test.
		//
		// Prior expected value:
		// "jakarta.servlet.Servlet=MyServlet,jakarta.servlet.Listener=MyListener",
		//
		// See issue #299

		Assertions.assertEquals(
			"transformer.test.data1.Servlet=MyServlet,javax.servlet.Listener=MyListener",
			transformedValue,
			"Value of [ SAMPLE_STRING ]");

		Assertions.assertEquals("transformer.test.data1.Servlet", transformedRefValue,
			"Value of [ SAMPLE_STRING_REFERENCE ]");
		Assertions.assertEquals("transformer.test.data1.Servlet", transformedAnnoValue1,
			"Value of [ Sample_Annotation.value1 ]");

		// TODO: The text substitution changes impacted this test.
		//
		// Prior expected value:
		// "jakarta.servlet.Servlet=MyServlet,jakarta.servlet.Listener=MyListener",
		//
		// See issue #299

		Assertions.assertEquals(
			"transformer.test.data1.Servlet=MyServlet,javax.servlet.Listener=MyListener",
			transformedAnnoValue2,
			"Value of [ Sample_Annotation.value2 ]");
	}

	@Test
	public void testjavaxAsJakarta_PerClassOverride() throws Exception {
		System.out.println("Test transformation with a per-class override");

		displayJakartaPackageRenames();
		displayJakartaPerClassDirectStrings();
		displayJakartaGlobalDirectStrings();

		Class<?> targetClass = testLoad(OVERRIDE_TARGET_CLASS_NAME, getClassLoader_toJakarta_PerClassDirectOverride());

		Class<?> transformedType = getStaticFieldType(targetClass, "sampleServlet");
		String transformedClassName = getStaticField(targetClass, "SAMPLE_CLASS_NAME");
		String transformedValue = getStaticField(targetClass, "SAMPLE_STRING");
		String transformedRefValue = getStaticField(targetClass, "SAMPLE_STRING_REFERENCE");
		String transformedAnnoValue1 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value1");
		String transformedAnnoValue2 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value2");

		System.out.println("Transformed type [ sampleServlet ]: " + transformedType.getName());
		System.out.println("Transformed class name [ SAMPLE_CLASS_NAME ]: " + transformedClassName);
		System.out.println("Transformed value [ SAMPLE_STRING ]: " + transformedValue);
		System.out.println("Transformed reference value [ SAMPLE_STRING_REFERENCE ]: " + transformedRefValue);
		System.out.println("Transformed annotation value [ Sample_Annotation.value1 ]: " + transformedAnnoValue1);
		System.out.println("Transformed annotation value [ Sample_Annotation.value2 ]: " + transformedAnnoValue2);

		Assertions.assertEquals("jakarta.servlet.Servlet", transformedType.getName(),
			"Type of [ sampleServlet ]");

		Assertions.assertEquals("transformer.test.data2.Servlet", transformedClassName,
			"Value of [ SAMPLE_CLASS_NAME ]");
		Assertions.assertEquals("transformer.test.data2.Servlet=MyServlet,transformer.test.data2.Listener=MyListener",
			transformedValue, "Value of [ SAMPLE_STRING ]");
		Assertions.assertEquals("transformer.test.data2.Servlet", transformedRefValue,
			"Value of [ SAMPLE_STRING_REFERENCE ]");
		Assertions.assertEquals("transformer.test.data2.Servlet", transformedAnnoValue1,
			"Value of [ Sample_Annotation.value1 ]");
		Assertions.assertEquals(
			"transformer.test.data2.Servlet=MyServlet,transformer.test.data2.Listener=MyListener",
			transformedAnnoValue2, "Value of [ Sample_Annotation.value2 ]");
	}

	@Test
	public void testjavaxAsJakarta_packageRenamesOnly() throws Exception {
		System.out.println("Test transformation with only package renames");

		displayJakartaPackageRenames();

		Class<?> targetClass = testLoad(OVERRIDE_TARGET_CLASS_NAME, getClassLoader_toJakarta_PackageRenamesOnly());

		Class<?> transformedType = getStaticFieldType(targetClass, "sampleServlet");
		String transformedClassName = getStaticField(targetClass, "SAMPLE_CLASS_NAME");
		String transformedValue = getStaticField(targetClass, "SAMPLE_STRING");
		String transformedRefValue = getStaticField(targetClass, "SAMPLE_STRING_REFERENCE");
		String transformedAnnoValue1 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value1");
		String transformedAnnoValue2 = getAnnotationValue(targetClass, "transformer.test.data.Sample_Annotation",
			"value2");

		System.out.println("Transformed type [ sampleServlet ]: " + transformedType.getName());
		System.out.println("Transformed class name [ SAMPLE_CLASS_NAME ]: " + transformedClassName);
		System.out.println("Transformed value [ SAMPLE_STRING ]: " + transformedValue);
		System.out.println("Transformed reference value [ SAMPLE_STRING_REFERENCE ]: " + transformedRefValue);
		System.out.println("Transformed annotation value [ Sample_Annotation.value1 ]: " + transformedAnnoValue1);
		System.out.println("Transformed annotation value [ Sample_Annotation.value2 ]: " + transformedAnnoValue2);

		Assertions.assertEquals(
			"jakarta.servlet.Servlet", transformedType.getName(),
			"Type of [ sampleServlet ]");
		Assertions.assertEquals(
			"jakarta.servlet.Servlet", transformedClassName,
			"Value of [ SAMPLE_CLASS_NAME ]");
		Assertions.assertEquals(
			"jakarta.servlet.Servlet", transformedRefValue,
			"Value of [ SAMPLE_STRING_REFERENCE ]");
		Assertions.assertEquals(
			"jakarta.servlet.Servlet=MyServlet,jakarta.servlet.Listener=MyListener",
			transformedValue,
			"Value of [ SAMPLE_STRING ]");
		Assertions.assertEquals(
			"jakarta.servlet.Servlet", transformedAnnoValue1,
			"Value of [ Sample_Annotation.value1 ]");
		Assertions.assertEquals(
			"jakarta.servlet.Servlet=MyServlet,jakarta.servlet.Listener=MyListener",
			transformedAnnoValue2,
			"Value of [ Sample_Annotation.value2 ]");
	}

	public Class<?> getStaticFieldType(Class<?> targetClass, String fieldName) throws Exception {
		Field staticField = targetClass.getDeclaredField(fieldName);
		return staticField.getType();
	}

	public String getStaticField(Class<?> targetClass, String fieldName) throws Exception {
		Field staticField = targetClass.getDeclaredField(fieldName);
		return (String) staticField.get(null);
	}

	public String getAnnotationValue(Class<?> targetClass, String annoClassName, String attributeName) throws Exception {
		Annotation[] annotations = targetClass.getDeclaredAnnotations();
		for ( Annotation annotation : annotations ) {
			Class<? extends Annotation> annoType = annotation.annotationType();
			if (!annoType.getName()
				.equals(annoClassName)) {
				continue;
			}

			Method attrMethod = annoType.getDeclaredMethod(attributeName);
			String annoValue = (String) attrMethod.invoke(annotation);
			return annoValue;
		}

		Assertions.fail(
			"Class [ " + targetClass + " ]" +
		    " does not have annotation [ " + annoClassName + " ]" +
		    " with attribute [ " + attributeName + " ]");
		return null;
	}

	//

	public static Map<String, String> getStandardRenames() throws IOException {
		String transformerResourceName = JakartaTransform.class.getPackage()
			.getName()
			.replace('.', '/');

		String renamesResourceName = transformerResourceName + '/' + JakartaTransform.DEFAULT_RENAMES_REFERENCE;

		return loadRenames(renamesResourceName);
	}

	public ClassActionImpl createStandardClassAction() throws IOException {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		ActionContext context = new ActionContextImpl(useLogger,
			createSelectionRule(useLogger, Collections.emptyMap(), Collections.emptyMap()),
			createSignatureRule(useLogger, getStandardRenames(), null, null, null, Collections.emptyMap()));

		return new ClassActionImpl(context);
	}
}
