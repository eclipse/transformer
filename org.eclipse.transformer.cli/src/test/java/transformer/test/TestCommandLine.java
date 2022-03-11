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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.action.impl.JavaActionImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.eclipse.transformer.cli.JakartaTransformerCLI;
import org.eclipse.transformer.cli.TransformerCLI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class TestCommandLine {

	private static final String	STATIC_CONTENT_DIR			= "src/test/data/command-line";
	private static final String DYNAMIC_CONTENT_DIR          = "target/test/data/command-line";

	private String currentDirectory	= ".";
	private Properties			prior;
	private String				name;

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
		System.out.println("setUp: Current directory is: [" + currentDirectory + "]");
		System.out.println("setUp: Static content directory is: [" + STATIC_CONTENT_DIR + "]");
		System.out.println("setUp: Dynamic content directory is: [" + DYNAMIC_CONTENT_DIR + "]");

		TestUtils.verifyDirectory(STATIC_CONTENT_DIR, !TestUtils.DO_CREATE, "static content");
		TestUtils.verifyDirectory(DYNAMIC_CONTENT_DIR, TestUtils.DO_CREATE, "dynamic content");
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	@Test
	void testManifestActionAccepted() throws Exception {
		String inputFileName = STATIC_CONTENT_DIR + '/' + "MANIFEST.MF";
		String outputFileName = DYNAMIC_CONTENT_DIR + '/' + "MANIFEST.MF";
		verifyAction(ManifestActionImpl.class.getName(), inputFileName, outputFileName);
	}

	@Test
	void testJavaActionAccepted() throws Exception {
		String inputFileName = STATIC_CONTENT_DIR + '/' + "A.java";
		String outputFileName = DYNAMIC_CONTENT_DIR + '/' + "A.java";
		verifyAction(JavaActionImpl.class.getName(), inputFileName, outputFileName);
	}

	@Test
	void testSetLogLevelQuiet() throws Exception {
		TransformerCLI cli = new TransformerCLI(System.out, System.err, new String[] {
			"--logName", name, "--quiet"
		});
		Transformer transformer = new Transformer(cli.getLogger(), cli);
		Logger logger = transformer.getLogger();
		assertThat(logger.isTraceEnabled()).isFalse();
		assertThat(logger.isDebugEnabled()).isFalse();
		assertThat(logger.isInfoEnabled()).isFalse();
		assertThat(logger.isWarnEnabled()).isFalse();
		assertThat(logger.isErrorEnabled()).isTrue();
	}

	@Test
	void testSetLogLevelDefault() throws Exception {
		TransformerCLI cli = new TransformerCLI(System.out, System.err, new String[] {
			"--logName", name
		});
		Transformer transformer = new Transformer(cli.getLogger(), cli);
		Logger logger = transformer.getLogger();
		assertThat(logger.isTraceEnabled()).isFalse();
		assertThat(logger.isDebugEnabled()).isFalse();
		assertThat(logger.isInfoEnabled()).isTrue();
		assertThat(logger.isWarnEnabled()).isTrue();
		assertThat(logger.isErrorEnabled()).isTrue();
	}

	@Test
	void testSetLogLevelDebug() throws Exception {
		TransformerCLI cli = new TransformerCLI(System.out, System.err, new String[] {
			"--logName", name, "--verbose"
		});
		Transformer transformer = new Transformer(cli.getLogger(), cli);
		Logger logger = transformer.getLogger();
		assertThat(logger.isTraceEnabled()).isFalse();
		assertThat(logger.isDebugEnabled()).isTrue();
		assertThat(logger.isInfoEnabled()).isTrue();
		assertThat(logger.isWarnEnabled()).isTrue();
		assertThat(logger.isErrorEnabled()).isTrue();
	}

	@Test
	void testSetLogLevelTrace() throws Exception {
		TransformerCLI cli = new TransformerCLI(System.out, System.err, new String[] {
			"--logName", name, "--trace"
		});
		Transformer transformer = new Transformer(cli.getLogger(), cli);
		Logger logger = transformer.getLogger();
		assertThat(logger.isTraceEnabled()).isTrue();
		assertThat(logger.isDebugEnabled()).isTrue();
		assertThat(logger.isInfoEnabled()).isTrue();
		assertThat(logger.isWarnEnabled()).isTrue();
		assertThat(logger.isErrorEnabled()).isTrue();
	}

	@Test
	void testSetLogFile() throws Exception {
		ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
		ByteArrayOutputStream sysErr = new ByteArrayOutputStream();
		String logFileName = DYNAMIC_CONTENT_DIR + '/' + "log.txt";
		File logFile = new File(logFileName);
		logFile.delete();
		assertThat(logFile).doesNotExist();
		try (PrintStream out = new PrintStream(sysOut); PrintStream err = new PrintStream(sysErr)) {
			TransformerCLI cli = new TransformerCLI(out, err, new String[] {
				"--logName", name,
				"--verbose",
				"--logFile", logFileName
			});
			Transformer transformer = new Transformer(cli.getLogger(), cli);
			Logger logger = transformer.getLogger();
			assertThat(logger.isDebugEnabled()).isTrue();
			Marker consoleMarker = MarkerFactory.getMarker("console");
			Marker otherMarker = MarkerFactory.getMarker("other");
			logger.debug(consoleMarker, "Test log console marker");
			logger.error(consoleMarker, "Test error console marker");
			logger.info("Test log {}", "plain");
			logger.error(otherMarker, "Test log {} {}", "other", "marker");
			out.flush();
			err.flush();
		}

		SoftAssertions.assertSoftly(softly -> {
			/*
			 * We cannot uncomment the logFile asserts since the Simple slf4j
			 * implementation is a static config and other test classes already
			 * cause the configuration to be locked into stdout. So our
			 * configuration request to use a file does not actually cause a
			 * file to we written. Sigh!
			 */
			// softly.assertThat(logFile)
			// .content()
			// .contains("Test log console marker")
			// .contains("Test error console marker")
			// .contains("Test log plain")
			// .contains("Test log other marker");
			softly.assertThat(sysOut.toString())
				.contains("Test log console marker")
			.doesNotContain("Test error console marker")
			.doesNotContain("Test log plain")
				.doesNotContain("Test log other marker");
			softly.assertThat(sysErr.toString())
				.doesNotContain("Test log console marker")
			.contains("Test error console marker")
			.doesNotContain("Test log plain")
				.doesNotContain("Test log other marker");
		});

	}

	private void verifyAction(String actionClassName, String inputFileName, String outputFileName) throws Exception {
		TransformerCLI cli = new JakartaTransformerCLI(System.out, System.err, new String[] {
			inputFileName, outputFileName, "-o"
		});

		Transformer transformer = new Transformer(cli.getLogger(), cli);

		assertTrue(transformer.setInput(), "options.setInput() failed");
		assertEquals(inputFileName, transformer.getInputFileName(),
			"input file name is not correct [" + transformer.getInputFileName() + "]");

		assertTrue(transformer.setOutput(), "options.setOutput() failed");
		assertEquals(outputFileName, transformer.getOutputFileName(),
			"output file name is not correct [" + transformer.getOutputFileName() + "]");

		assertTrue(transformer.setRules(), "options.setRules() failed");
		assertTrue(transformer.acceptAction(), "options.acceptAction() failed");
		assertEquals(actionClassName, transformer.acceptedAction.getClass().getName());

		transformer.transform();
		assertTrue((new File(outputFileName)).exists(), "output file not created");
	}
}
