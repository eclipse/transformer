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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Test;

import transformer.test.util.CaptureLoggerImpl;

public class TestTransformServiceConfig extends CaptureTest {

	public SelectionRuleImpl createSelectionRule(
		CaptureLoggerImpl useLogger,
		Set<String> useIncludes,
		Set<String> useExcludes) {

		return new SelectionRuleImpl( useLogger, useIncludes, useExcludes );
	}

	public SignatureRuleImpl createSignatureRule(
		CaptureLoggerImpl useLogger,
		Map<String, String> usePackageRenames,
		Map<String, String> usePackageVersions,
		Map<String, BundleData> bundleData,
		Map<String, String> directStrings) {

		return new SignatureRuleImpl(
			useLogger,
			usePackageRenames, usePackageVersions,
			bundleData,
			null,
			directStrings );
	}

	//

	public static final String JAVAX_OTHER_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.other.Reader";
	public static final String[] JAVAX_OTHER_READER_LINES = { "javax.other.ReaderImpl" };
	public static final String JAVAX_SAMPLE_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.sample.Reader";
	public static final String[] JAVAX_SAMPLE_READER_LINES = { "javax.sample.ReaderImpl" };	
	public static final String JAVAX_SAMPLE_WRITER_SERVICE_PATH = "transformer/test/data/META-INF/services/javax.sample.Writer";
	public static final String[] JAVAX_SAMPLE_WRITER_LINES = { "javax.sample.WriterImpl" };	
	
	public static final String JAKARTA_OTHER_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.other.Reader";
	public static final String[] JAKARTA_OTHER_READER_LINES = { "jakarta.other.ReaderImpl" };
	public static final String JAKARTA_SAMPLE_READER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.sample.Reader";
	public static final String[] JAKARTA_SAMPLE_READER_LINES = { "jakarta.sample.ReaderImpl" };	
	public static final String JAKARTA_SAMPLE_WRITER_SERVICE_PATH = "transformer/test/data/META-INF/services/jakarta.sample.Writer";
	public static final String[] JAKARTA_SAMPLE_WRITER_LINES = { "jakarta.sample.WriterImpl" };	

	public static final String JAVAX_SAMPLE = "javax.sample";
	public static final String JAKARTA_SAMPLE = "jakarta.sample";
	
	public static final String JAVAX_SERVLET = "javax.servlet";
	public static final String JAVAX_SERVLET_ANNOTATION = "javax.servlet.annotation";
	public static final String JAVAX_SERVLET_DESCRIPTOR = "javax.servlet.descriptor";
	public static final String JAVAX_SERVLET_HTTP = "javax.servlet.http";
	public static final String JAVAX_SERVLET_RESOURCES = "javax.servlet.resources";	

	public static final String JAKARTA_SERVLET_VERSION = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_ANNOTATION_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_DESCRIPTOR_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_HTTP_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_RESOURCES_VERSION  = "[2.6, 6.0)";

	protected Set<String> includes;
	
	public Set<String> getIncludes() {
		if ( includes == null ) {
			includes = new HashSet<String>();
			includes.add(JAVAX_SAMPLE_READER_SERVICE_PATH);
			includes.add(JAVAX_SAMPLE_WRITER_SERVICE_PATH);
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(JAVAX_SAMPLE, JAKARTA_SAMPLE);
		}
		return packageRenames;
	}
	
	public ServiceLoaderConfigActionImpl jakartaServiceAction;
	public ServiceLoaderConfigActionImpl javaxServiceAction;

	public ServiceLoaderConfigActionImpl getJakartaServiceAction() {
		if ( jakartaServiceAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			jakartaServiceAction = new ServiceLoaderConfigActionImpl(
				useLogger, false, false,
				createBuffer(),
				createSelectionRule( useLogger, getIncludes(), getExcludes() ),
				createSignatureRule( useLogger, getPackageRenames(), null, null, null ) );
		}
		return jakartaServiceAction;
	}

	public ServiceLoaderConfigActionImpl getJavaxServiceAction() {
		if ( javaxServiceAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			Map<String, String> invertedRenames = TransformProperties.invert( getPackageRenames() );

			javaxServiceAction = new ServiceLoaderConfigActionImpl(
				useLogger, false, false,
				createBuffer(),
				createSelectionRule( useLogger, getIncludes(), getExcludes() ),
				createSignatureRule( useLogger, invertedRenames, null, null, null ) );
		}
		return javaxServiceAction;
	}

	@Test
	public void testJakartaTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl jakartaAction = getJakartaServiceAction();

		verifyTransform(
			jakartaAction,
			JAVAX_OTHER_READER_SERVICE_PATH,
			JAVAX_OTHER_READER_LINES); // Not transformed 
		verifyTransform(
			jakartaAction,
			JAVAX_SAMPLE_READER_SERVICE_PATH,
			JAKARTA_SAMPLE_READER_LINES); // Transformed
		verifyTransform(
			jakartaAction,
			JAVAX_SAMPLE_READER_SERVICE_PATH,
			JAKARTA_SAMPLE_READER_LINES); // Transformed 
	}

	@Test
	public void testJavaxTransform() throws IOException, TransformException {
		ServiceLoaderConfigActionImpl javaxAction = getJavaxServiceAction();

		verifyTransform(
			javaxAction,
			JAKARTA_OTHER_READER_SERVICE_PATH,
			JAKARTA_OTHER_READER_LINES); // Not transformed
		verifyTransform(
			javaxAction,
			JAKARTA_SAMPLE_READER_SERVICE_PATH,
			JAVAX_SAMPLE_READER_LINES); // Transformed
		verifyTransform(
			javaxAction,
			JAKARTA_SAMPLE_READER_SERVICE_PATH,
			JAVAX_SAMPLE_READER_LINES); // Transformed
	}

	protected void verifyTransform(
		ServiceLoaderConfigActionImpl action,
		String inputName,
		String[] expectedLines) throws IOException, TransformException {

		InputStream inputStream = TestUtils.getResourceStream(inputName);

		InputStreamData transformedData;
		try {
			transformedData = action.apply(inputName, inputStream);
		} finally {
			inputStream.close();
		}

		List<String> transformedLines = TestUtils.loadLines(transformedData.stream);
		TestUtils.filter(transformedLines);
		TestUtils.verify(inputName, expectedLines, transformedLines);
	}
	
}
