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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.ActionSelectorImpl;
import org.eclipse.transformer.action.impl.PropertiesActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.util.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transformer.test.util.CaptureLoggerImpl;

public class TestTransformServiceConfig extends CaptureTest {
	public static final String	COMPLEX_RESOURCE_PATH	= "complex.properties";
	private Properties prior;

	@BeforeEach
	public void setUp() {
		prior = new Properties();
		prior.putAll(System.getProperties());
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	public SelectionRuleImpl createSelectionRule(CaptureLoggerImpl useLogger, Map<String, String> useIncludes,
												 Map<String, String> useExcludes) {

		return new SelectionRuleImpl(useLogger, useIncludes, useExcludes);
	}

	public SignatureRuleImpl createSignatureRule(CaptureLoggerImpl useLogger, Map<String, String> usePackageRenames,
		Map<String, String> usePackageVersions, Map<String, BundleData> bundleData, Map<String, String> directStrings) {

		return new SignatureRuleImpl(useLogger, usePackageRenames, usePackageVersions, null, bundleData, null,
			directStrings, Collections.emptyMap());
	}

	//

	public static final String		TEST_DATA_PATH						= "serviceconfig";

	public static final String		JAVAX_OTHER_READER_SERVICE_PATH		= TEST_DATA_PATH + "/"
		+ "META-INF/services/javax.other.Reader";
	public static final String[]	JAVAX_OTHER_READER_LINES			= {
		"javax.other.ReaderImpl"
	};
	public static final String		JAVAX_SAMPLE_READER_SERVICE_PATH	= TEST_DATA_PATH + "/"
		+ "META-INF/services/javax.sample.Reader";
	public static final String[]	JAVAX_SAMPLE_READER_LINES			= {
		"javax.sample.ReaderImpl"
	};
	public static final String		JAVAX_SAMPLE_WRITER_SERVICE_PATH	= TEST_DATA_PATH + "/"
		+ "META-INF/services/javax.sample.Writer";
	public static final String[]	JAVAX_SAMPLE_WRITER_LINES			= {
		"javax.sample.WriterImpl"
	};

	public static final String		JAKARTA_OTHER_READER_SERVICE_PATH	= TEST_DATA_PATH + "/"
		+ "META-INF/services/jakarta.other.Reader";
	public static final String[]	JAKARTA_OTHER_READER_LINES			= {
		"jakarta.other.ReaderImpl"
	};
	public static final String		JAKARTA_SAMPLE_READER_SERVICE_PATH	= TEST_DATA_PATH + "/"
		+ "META-INF/services/jakarta.sample.Reader";
	public static final String[]	JAKARTA_SAMPLE_READER_LINES			= {
		"jakarta.sample.ReaderImpl"
	};
	public static final String		JAKARTA_SAMPLE_WRITER_SERVICE_PATH	= TEST_DATA_PATH + "/"
		+ "META-INF/services/jakarta.sample.Writer";
	public static final String[]	JAKARTA_SAMPLE_WRITER_LINES			= {
		"jakarta.sample.WriterImpl"
	};

	public static final String		JAVAX_SAMPLE						= "javax.sample";
	public static final String		JAKARTA_SAMPLE						= "jakarta.sample";

	public static final String		JAVAX_SERVLET						= "javax.servlet";
	public static final String		JAVAX_SERVLET_ANNOTATION			= "javax.servlet.annotation";
	public static final String		JAVAX_SERVLET_DESCRIPTOR			= "javax.servlet.descriptor";
	public static final String		JAVAX_SERVLET_HTTP					= "javax.servlet.http";
	public static final String		JAVAX_SERVLET_RESOURCES				= "javax.servlet.resources";

	public static final String		JAKARTA_SERVLET_VERSION				= "[2.6, 6.0)";
	public static final String		JAKARTA_SERVLET_ANNOTATION_VERSION	= "[2.6, 6.0)";
	public static final String		JAKARTA_SERVLET_DESCRIPTOR_VERSION	= "[2.6, 6.0)";
	public static final String		JAKARTA_SERVLET_HTTP_VERSION		= "[2.6, 6.0)";
	public static final String		JAKARTA_SERVLET_RESOURCES_VERSION	= "[2.6, 6.0)";

	protected Map<String, String>			includes;

	public Map<String, String> getIncludes() {
		if (includes == null) {
			includes = new HashMap<>();
			includes.put(JAVAX_SAMPLE_READER_SERVICE_PATH, FileUtils.DEFAULT_CHARSET.name());
			includes.put(JAVAX_SAMPLE_WRITER_SERVICE_PATH, FileUtils.DEFAULT_CHARSET.name());
		}

		return includes;
	}

	public Map<String, String> getExcludes() {
		return Collections.emptyMap();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if (packageRenames == null) {
			packageRenames = new HashMap<>();
			packageRenames.put(JAVAX_SAMPLE, JAKARTA_SAMPLE);
		}
		return packageRenames;
	}

	public ServiceLoaderConfigActionImpl	jakartaServiceAction;
	public ServiceLoaderConfigActionImpl	javaxServiceAction;
	public ZipActionImpl					jarJavaxServiceAction;

	public ServiceLoaderConfigActionImpl getJakartaServiceAction() {
		if (jakartaServiceAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getPackageRenames(), null, null, null));

			jakartaServiceAction = new ServiceLoaderConfigActionImpl(context);
		}
		return jakartaServiceAction;
	}

	public ServiceLoaderConfigActionImpl getJavaxServiceAction() {
		if (javaxServiceAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> invertedRenames = TransformProperties.invert(getPackageRenames());

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, invertedRenames, null, null, null));

			javaxServiceAction = new ServiceLoaderConfigActionImpl(context);
		}
		return javaxServiceAction;
	}

	public ZipActionImpl getJarJavaxServiceAction() {
		if (jarJavaxServiceAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> invertedRenames = TransformProperties.invert(getPackageRenames());

			ActionSelectorImpl actionSelector = new ActionSelectorImpl();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, Collections.emptyMap(), getExcludes()),
				createSignatureRule(useLogger, invertedRenames, null, null, null));

			jarJavaxServiceAction = new ZipActionImpl(context, ActionType.JAR);
			jarJavaxServiceAction.addUsing(PropertiesActionImpl::new);
			jarJavaxServiceAction.addUsing(ServiceLoaderConfigActionImpl::new);
		}

		return jarJavaxServiceAction;
	}

	@Test
	public void testJakartaTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl jakartaAction = getJakartaServiceAction();

		verifyTransform(jakartaAction, JAVAX_OTHER_READER_SERVICE_PATH, JAVAX_OTHER_READER_LINES); // Not
																									// transformed
		verifyTransform(jakartaAction, JAVAX_SAMPLE_READER_SERVICE_PATH, JAKARTA_SAMPLE_READER_LINES); // Transformed
		verifyTransform(jakartaAction, JAVAX_SAMPLE_READER_SERVICE_PATH, JAKARTA_SAMPLE_READER_LINES); // Transformed
	}

	@Test
	public void testJavaxTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl javaxAction = getJavaxServiceAction();

		verifyTransform(javaxAction, JAKARTA_OTHER_READER_SERVICE_PATH, JAKARTA_OTHER_READER_LINES); // Not
																										// transformed
		verifyTransform(javaxAction, JAKARTA_SAMPLE_READER_SERVICE_PATH, JAVAX_SAMPLE_READER_LINES); // Transformed
		verifyTransform(javaxAction, JAKARTA_SAMPLE_READER_SERVICE_PATH, JAVAX_SAMPLE_READER_LINES); // Transformed
	}

	/**
	 * Ensure that the inputlength parameter in ServiceLoaderConfigActionImpl is
	 * used. When processing using a ContainerAction, the data passed in may
	 * "leak" other data from other files. The resulting transformed service
	 * file is corrupt.
	 *
	 * @throws Exception Thrown in case of a IO failure or a transformation
	 *             failure.
	 */
	@Test
	public void testInputLength() throws Exception {
		final String inputName = "sample.jar";
		final File inputJarFile = File.createTempFile("sample", ".jar");
		inputJarFile.deleteOnExit();

		final String outputName = "sample_output.jar";
		final File outputJarFile = File.createTempFile("sample_output", ".jar");
		outputJarFile.delete();
		outputJarFile.deleteOnExit();

		final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class);
		javaArchive.add(new ClassLoaderAsset(COMPLEX_RESOURCE_PATH), "complex.properties");
		javaArchive.add(new ClassLoaderAsset(JAKARTA_SAMPLE_READER_SERVICE_PATH), "META-INF/services/jakarta.sample.Reader");
		javaArchive.as(ZipExporter.class).exportTo(inputJarFile, true);

		final ZipActionImpl useJarJavaxServiceAction = getJarJavaxServiceAction();
		useJarJavaxServiceAction.apply(inputName, inputJarFile, outputName, outputJarFile);

		final String[] expectedLines = new String[] { "# Sample reader", "", "javax.sample.ReaderImpl" };

		Assertions.assertTrue(outputJarFile.exists());

		boolean found = false;
		final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(outputJarFile));
		ZipEntry inputEntry;
		while ((inputEntry = zipInputStream.getNextEntry()) != null) {
			final String inputEntryName = inputEntry.getName();
			@SuppressWarnings("unused")
			final long inputEntryLength = inputEntry.getSize();

			if ("META-INF/services/javax.sample.Reader".equals(inputEntryName)) {
				found = true;
				final List<String> lines = TestUtils.loadLines(zipInputStream);
				TestUtils.verify(inputName, expectedLines, lines);
			}
		}

		Assertions.assertTrue(found);
	}

	protected void verifyTransform(ServiceLoaderConfigActionImpl action, String inputName, String[] expectedLines)
		throws IOException, TransformException {

		ByteData inputData;
		try (InputStream inputStream = TestUtils.getResourceStream(inputName)) {
			inputData = action.collect(inputName, inputStream);
		}

		List<String> inputLines = TestUtils.loadLines(inputData.stream());

		ByteData transformedData = action.apply(inputData);

		List<String> transformedLines = TestUtils.loadLines(transformedData.stream());
		TestUtils.filter(transformedLines);
		TestUtils.verify(inputName, expectedLines, transformedLines);
	}

}
