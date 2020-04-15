/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package transformer.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
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
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ibm.ws.jakarta.transformer.JakartaTransformer;

import transformer.test.data.Sample_InjectAPI_Jakarta;
import transformer.test.data.Sample_InjectAPI_Javax;
import transformer.test.util.CaptureLoggerImpl;
import transformer.test.util.ClassData;

public class TestTransformClass extends CaptureTest {
	//

	@Test
	public void testJavaxAsJavax_inject() {
		System.out.println("test null conversion javax class load");
		testLoad( JAVAX_CLASS_NAME, getClassLoader_null() );
	}

	@Test
	public void testJakartaAsJakarta_inject() {
		System.out.println("test null conversion jakarta class load");
		testLoad( JAKARTA_CLASS_NAME, getClassLoader_null() );
	}

	@Test
	public void testJavaxAsJakarta_inject() {
		System.out.println("test javax to jakarta class load");
		Class<?> testClass = testLoad( JAVAX_CLASS_NAME, getClassLoader_toJakarta() );
		ClassData testData = new ClassData(testClass);
		testData.log( new PrintWriter(System.out, true) ); // autoflush
	}

	@Test
	public void testJakartaAsJavax_inject() {
		System.out.println("test jakarta to javax class load");
		Class<?> testClass = testLoad( JAKARTA_CLASS_NAME, getClassLoader_toJavax() );
		ClassData testData = new ClassData(testClass);
		testData.log( new PrintWriter(System.out, true) ); // autoflush
	}

	//

	public static final String JAVAX_CLASS_NAME = Sample_InjectAPI_Javax.class.getName();
	public static final String JAKARTA_CLASS_NAME = Sample_InjectAPI_Jakarta.class.getName();

	protected Set<String> includes;
	
	public Set<String> getIncludes() {
		if ( includes == null ) {
			includes = new HashSet<String>();
			includes.add( ClassActionImpl.classNameToBinaryTypeName(JAVAX_CLASS_NAME) );
			includes.add( ClassActionImpl.classNameToBinaryTypeName(JAKARTA_CLASS_NAME) );
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public static final String JAVAX_INJECT_PACKAGE_NAME = "javax.inject";
	public static final String JAKARTA_INJECT_PACKAGE_NAME = "jakarta.inject";
	
	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(
				ClassActionImpl.classNameToBinaryTypeName(JAVAX_INJECT_PACKAGE_NAME),
				ClassActionImpl.classNameToBinaryTypeName(JAKARTA_INJECT_PACKAGE_NAME) );
		}
		return packageRenames;
	}

	public ClassLoader getClassLoader_null() {
		return getClass().getClassLoader();
	}

	//

	public JarActionImpl jakartaJarAction;
	public JarActionImpl javaxJarAction;

	public JarActionImpl getJakartaJarAction() {
		if ( jakartaJarAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			jakartaJarAction = new JarActionImpl(
				useLogger, false, false,
				createBuffer(),
				createSelectionRule( useLogger, getIncludes(), getExcludes() ),
				createSignatureRule( useLogger, getPackageRenames(), null, null, null ) );
		}

		return jakartaJarAction;
	}

	public JarActionImpl getJavaxJarAction() {
		if ( javaxJarAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> invertedRenames =
				TransformProperties.invert( getPackageRenames() );

			javaxJarAction = new JarActionImpl(
				useLogger, false, false,
				createBuffer(),
				createSelectionRule( useLogger, getIncludes(), getExcludes() ),
				createSignatureRule( useLogger, invertedRenames, null, null, null ) );
		}

		return javaxJarAction;
	}

	public ClassLoader getClassLoader_toJakarta() {
		JarActionImpl jarAction = getJakartaJarAction();
		ClassActionImpl classAction = jarAction.addUsing( ClassActionImpl::new );
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing( ServiceLoaderConfigActionImpl::new );

		return new TransformClassLoader(
			getClass().getClassLoader(),
			jarAction, classAction, configAction );
	}

	public ClassLoader getClassLoader_toJavax() {
		JarActionImpl jarAction = getJavaxJarAction();
		ClassActionImpl classAction = jarAction.addUsing( ClassActionImpl::new );
		ServiceLoaderConfigActionImpl configAction = jarAction.addUsing( ServiceLoaderConfigActionImpl::new );

		return new TransformClassLoader(
			getClass().getClassLoader(),
			jarAction, classAction, configAction );
	}

	public Class<?> testLoad(String className, ClassLoader classLoader) {
		System.out.println("Loading [ " + className + " ] using [ " + classLoader + " ]");

		@SuppressWarnings("unused")
		Class<?> objectClass;
		try {
			objectClass = classLoader.loadClass( java.lang.Object.class.getName() );
		} catch ( Throwable th ) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + java.lang.Object.class.getName() + " ]: " + th);
			return null;
		}

		Class<?> testClass;
		try {
			testClass = classLoader.loadClass(className);
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + e);
			return null;
		} catch ( Throwable th ) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + th);
			return null;
		}

		System.out.println("Loaded [ " + className + " ]: " + testClass);
		return testClass;
	}

	public static InputStream getResourceStream(String resourceName) throws IOException {
		InputStream inputStream = TestUtils.getResourceStream(resourceName);
		if ( inputStream == null ) {
			throw new IOException("Resource not found [ " + resourceName + " ]");
		}
		return inputStream;
	}

	public static final String TEST_DATA_RESOURCE_NAME = "transformer/test/data";
	public static final String ANNOTATED_SERVLET_RESOURCE_NAME = "AnnotatedServlet.class";

	public static final String TRANSFORMER_RESOURCE_NAME = "com/ibm/ws/jakarta/transformer";

	public static Map<String, String> getStandardRenames() throws IOException {
		String transformerResourceName = JakartaTransformer.class.getPackage().getName().replace('.', '/');
		String renamesResourceName = transformerResourceName + '/' + JakartaTransformer.DEFAULT_RENAMES_REFERENCE;

		InputStream renamesInputStream = getResourceStream(renamesResourceName); // throws IOException
		Reader renamesReader = new InputStreamReader(renamesInputStream);

		Properties renameProperties = new Properties();
		renameProperties.load(renamesReader); // throws IOException

		Map<String, String> renames = new HashMap<String, String>( renameProperties.size() );
		for ( Map.Entry<Object, Object> renameEntry : renameProperties.entrySet() ) {
			String initialPackageName = (String) renameEntry.getKey(); 
			String finalPackageName = (String) renameEntry.getValue();
			renames.put(initialPackageName, finalPackageName);
		}

		return renames;
	}

	public ClassActionImpl createStandardClassAction() throws IOException {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(
			useLogger, false, false,
			createBuffer(),
			createSelectionRule( useLogger, Collections.emptySet(), Collections.emptySet() ),
			createSignatureRule( useLogger, getStandardRenames(), null, null, null ) );
		// 'getStandardRenames' throws IOException
	}

	@Test
	public void testAnnotatedServlet() throws TransformException, IOException {
		consumeCapturedEvents();

		ClassActionImpl classAction = createStandardClassAction(); // throws IOException

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + ANNOTATED_SERVLET_RESOURCE_NAME;
		InputStream inputStream = getResourceStream(resourceName); // throws IOException

		InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws TransformException
		display( classAction.getLastActiveChanges() );

		OutputStream outputStream = new FileOutputStream("build" + '/' + ANNOTATED_SERVLET_RESOURCE_NAME); // throws FileNotFoundException
		try {
			FileUtils.transfer(outputStreamData.stream, outputStream); // throws IOException
		} finally {
			outputStream.close(); // throws IOException
		}
	}

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
		DIRECT_STRINGS = new HashMap<String, String>();
		DIRECT_STRINGS.put("DIRECT_1", "DIRECT_X"); // String value update // 2 hits
		DIRECT_STRINGS.put("DIRECT21", "DIRECT21"); // Variable name update // 2 hits

		DIRECT_STRINGS.put("Sample value 1", "Sample value 2"); // String constant reference update // 1 hit
		DIRECT_STRINGS.put("Sample", "Official"); // String constant reference update (not found) // 0 hits
		DIRECT_STRINGS.put("value", "product"); // String constant reference update (not found) // 0 hits
	}

	public static final Map<String, String> getDirectStrings() {
		return DIRECT_STRINGS;
	}

	public ClassActionImpl createDirectClassAction() {
		CaptureLoggerImpl useLogger = getCaptureLogger();

		return new ClassActionImpl(
			useLogger, false, false,
			createBuffer(),
			createSelectionRule( useLogger, Collections.emptySet(), Collections.emptySet() ),
			createSignatureRule( useLogger, Collections.emptyMap(), null, null, getDirectStrings() ) );
	}

	public static final String DIRECT_STRINGS_RESOURCE_NAME = "Sample_DirectStrings.class";

	@Test
	public void testDirectStrings() throws TransformException, IOException {
		consumeCapturedEvents();

		ClassActionImpl classAction = createDirectClassAction();

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + DIRECT_STRINGS_RESOURCE_NAME;
		InputStream inputStream = getResourceStream(resourceName); // throws IOException

		@SuppressWarnings("unused")
		InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws TransformException
		display( classAction.getLastActiveChanges() );

		int expectedChanges = 5;
		int actualChanges = classAction.getLastActiveChanges().getModifiedConstants(); 
		Assertions.assertEquals(
			expectedChanges, actualChanges, 
			"Incorrect count of constant changes");
	}

	public static final boolean IS_EXACT = false;

	public static class ClassRelocation {
		public final String inputPath;
		public final String inputName;
		
		public final String outputName;
		public final String outputPath;

		public final boolean isApproximate;

		public ClassRelocation(
			String inputPath, String inputName,
			String outputName, String outputPath,
			boolean isApproximate) {

			this.inputPath = inputPath;
			this.inputName = inputName;

			this.outputName = outputName;
			this.outputPath = outputPath;

			this.isApproximate = isApproximate;
		}
	}

	public static ClassRelocation[] RELOCATION_CASES = new ClassRelocation[] {
		new ClassRelocation(
			"com/ibm/test/Sample.class", "com.ibm.test.Sample",
			 "com.ibm.prod.Sample", "com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation(
			"WEB-INF/classes/com/ibm/test/Sample.class", "com.ibm.test.Sample",
		    "com.ibm.prod.Sample", "WEB-INF/classes/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation(
			"META-INF/versions/9/com/ibm/test/Sample.class", "com.ibm.test.Sample",
			"com.ibm.prod.Sample", "META-INF/versions/9/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation(
			"META-INF/versions/com/ibm/test/Sample.class", "com.ibm.test.Sample",
			"com.ibm.prod.Sample", "META-INF/versions/com/ibm/prod/Sample.class", IS_EXACT),
		new ClassRelocation(
			"sample/com/ibm/test/Sample.class", "com.ibm.test.Sample",
			"com.ibm.prod.Sample", "sample/com/ibm/prod/Sample.class", IS_EXACT),

//		new ClassRelocation(
//			"com/ibm/broken/Sample.class", "com.ibm.test.Sample",
//			"com.ibm.prod.Sample", "com/ibm/prod/Sample.class", !IS_EXACT),
//		new ClassRelocation(
//			"WEB-INF/classes/com/ibm/broken/Sample.class", "com.ibm.test.Sample",
//			"com.ibm.prod.Sample", "WEB-INF/classes/com/ibm/prod/Sample.class", !IS_EXACT),
//		new ClassRelocation(
//			"META-INF/versions/9/com/ibm/broken/Sample.class", "com.ibm.test.Sample",
//			"com.ibm.prod.Sample", "META-INF/versions/9/com/ibm/prod/Sample.class", !IS_EXACT),
		
	};

	public static final String APPROXIMATE_TEXT = "Approximate relocation of class";

	@Test
	public void testClassRelocation() {
		for ( ClassRelocation relocationCase : RELOCATION_CASES ) {
			String outputPath = ClassActionImpl.relocateClass(
				getCaptureLogger(),
				relocationCase.inputPath, relocationCase.inputName, 
				relocationCase.outputName);

			List<? extends CaptureLoggerImpl.LogEvent> capturedEvents =
				consumeCapturedEvents();

			System.out.printf("Relocation [ %s ] as [ %s ]\n" +
			                  "        to [ %s ] as [ %s ]\n",
			                  relocationCase.inputPath, relocationCase.inputName,
			                  relocationCase.outputName, outputPath);

			boolean capturedApproximate = false;
			for ( CaptureLoggerImpl.LogEvent event : capturedEvents ) {
				System.out.printf("Captured Event [ %s ]\n", event);
				if ( event.message.contains(APPROXIMATE_TEXT) ) {
					capturedApproximate = true;
				}
			}

			Assertions.assertEquals(
				relocationCase.outputPath, outputPath,
				"Incorrect output path");

			Assertions.assertEquals(
				capturedApproximate, relocationCase.isApproximate,
				"Approximate error not logged");
		}
	}
}
