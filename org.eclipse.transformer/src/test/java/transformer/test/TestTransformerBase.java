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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.ResultCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.helpers.SubstituteLoggerFactory;

public abstract class TestTransformerBase {

	private static final String	STATIC_CONTENT_DIR = "src/test/data";
	private static final String	DYNAMIC_CONTENT_DIR = "target/test/data";

	public String getStaticContentDir() {
		return STATIC_CONTENT_DIR;
	}

	public String getDynamicContentDir() {
		return DYNAMIC_CONTENT_DIR;
	}

	private String currentDirectory = ".";

	public String getCurrentDir() {
		return currentDirectory;
	}

	//

	private Properties prior;

	private String		name;

	@BeforeEach
	public void setUp(TestInfo testInfo) {
		name = testInfo.getTestClass()
			.get()
			.getName() + "."
			+ testInfo.getTestMethod()
				.get()
				.getName();
		prior = new Properties();
		prior.putAll(System.getProperties());
		currentDirectory = System.getProperty("user.dir");

		String staticDir = getStaticContentDir();
		String dynamicDir = getDynamicContentDir();

		System.out.println("setUp: Current directory is: [" + currentDirectory + "]");

		if ( staticDir != null ) {
			System.out.println("setUp: Static content directory is: [" + staticDir + "]");
		}
		if ( dynamicDir != null ) {
			System.out.println("setUp: Dynamic content directory is: [" + dynamicDir + "]");
		}

		if ( staticDir != null ) {
			TestUtils.verifyDirectory(staticDir, !TestUtils.DO_CREATE, "static content");
		}
		if ( dynamicDir != null ) {
			TestUtils.verifyDirectory(dynamicDir, TestUtils.DO_CREATE, "dynamic content");
		}
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	protected String getName() {
		return name;
	}

	public void runTransformer(String inputFileName, String outputFileName, Map<AppOption, List<String>> options)
		throws Exception, AssertionFailedError {
		runTransformer(inputFileName, outputFileName, options, null);
	}

	public void runTransformer(String inputFileName, String outputFileName, Map<AppOption, List<String>> options,
		boolean doLog) throws Exception, AssertionFailedError {
		runTransformer(inputFileName, outputFileName, options, null, doLog);
	}

	public static final boolean DO_LOG = true;

	public void runTransformer(String inputFileName, String outputFileName, Map<AppOption, List<String>> options,
		List<String> expectedLogFragments) throws Exception, AssertionFailedError {

		runTransformer(inputFileName, outputFileName, options, expectedLogFragments, !DO_LOG);
	}

	/**
	 * Run the transformer using java interfaces. Compose transformer options
	 * using the supplied parameters. Verify that the output file was created
	 * and that expected log fragments are present in the transformer output
	 * log. Answer the raw bytes of the captured transformer log.
	 *
	 * @param inputFileName The file to be transformed.
	 * @param outputFileName The transformation output file.
	 * @param options Additional transformation parameters.
	 * @param expectedLogFragments Text which is expected to be present in the
	 *            transformer log.
	 * @param doLog Control parameter: Tell if normal logging output is to be
	 *            used. Has no effect if the expected log fragments is not
	 *            empty.
	 * @throws Exception Thrown in case of a transformation error.
	 * @throws AssertionFailedError Thrown by an assertion failure. This is a
	 *             test failure, as opposed to a processing error.
	 */
	@SuppressWarnings("null")
	public void runTransformer(
		String inputFileName, String outputFileName, Map<AppOption, List<String>> options,
		List<String> expectedLogFragments, boolean doLog)
		throws Exception, AssertionFailedError {

		TransformOptions transformOptions = new TransformOptions() {
			@Override
			public String getOutputFileName() {
				return outputFileName;
			}
			@Override
			public String getInputFileName() {
				return inputFileName;
			}
			@Override
			public List<String> getOptionValues(AppOption option) {
				return options.get(option);
			}
		};

		boolean haveFragments = ((expectedLogFragments != null) && !expectedLogFragments.isEmpty());

		SubstituteLoggerFactory loggerFactory;
		Transformer transformer;

		if (!haveFragments && doLog) {
			loggerFactory = null;
			transformer = new Transformer(transformOptions);
		} else {
			loggerFactory = new SubstituteLoggerFactory();
			Logger logger = loggerFactory.getLogger(getName());
			transformer = new Transformer(logger, transformOptions);
		}

		ResultCode rc = transformer.run();
		assertThat(rc).isEqualTo(ResultCode.SUCCESS_RC);
		assertThat(new File(outputFileName)).exists();

		if (haveFragments) {
			TestUtils.verifyLog("property option logging", expectedLogFragments, loggerFactory.getEventQueue());
		}
	}

	//

	/**
	 * Standard log fragment for multi-property logging.
	 *
	 * @param key The key that was updated.
	 * @param value1 The initial value.
	 * @param value2 The final, updated, value.
	 *
	 * @return Update replacement log fragment text.
	 */
	public static String logReplacementFragment(String key, String value1, String value2) {
		return "key [ " + key + " ] replaces value [ " + value1 + " ] with [ " + value2 + " ]";
	}
}
