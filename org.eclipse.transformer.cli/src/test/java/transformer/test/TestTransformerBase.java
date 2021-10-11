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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public abstract class TestTransformerBase {

	// TODO: This is currently used for input data and for output data.
	//       Output data should be written to a build location, which is
	//       much safer.

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

	@BeforeEach
	public void setUp() {
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

	/** Control parameter: Enable debug logging. */
	public static final boolean DO_DEBUG_LOGGING = true;

	/**
	 * Run the transformer using java interfaces.
	 *
	 * Compose transformer options using the supplied parameters.
	 *
	 * Verify that the output file was created and that expected
	 * log fragments are present in the transformer output log.
	 *
	 * Answer the raw bytes of the captured transformer log.
	 *
	 * @param inputFileName The file to be transformed.
	 * @param outputFileName The transformation output file.
	 * @param parms Additional transformation parameters.
	 * @param enableDebugLogging Control parameter telling if debug
	 *     logging is to be enabled.
	 * @param expectedLogFragments Text which is expected to be present
	 *     in the transformer log.
	 *
	 * @return The raw bytes of the transformer log.
	 *
	 * @throws Exception Thrown in case of a transformation error.
	 *
	 * @throws AssertionFailedError Thrown by an assertion failure.  This
	 *     is a test failure, as opposed to a processing error.
	 */
	@Test
	public byte[] runTransformer(
		String inputFileName, String outputFileName,
		String[] parms,
		boolean enableDebugLogging,
		String[] expectedLogFragments) throws Exception, AssertionFailedError {

		ByteArrayOutputStream sysOutByteOutput = new ByteArrayOutputStream();

		PrintStream sysOutOutput = new PrintStream(sysOutByteOutput);

		int argCount = 3 + parms.length + ( enableDebugLogging ? 2 : 0 );

		String[] args = new String[argCount];
		args[0] = inputFileName;
		args[1] = outputFileName;
		args[2] = "-o"; // Overwrite

		for ( int parmNo = 0; parmNo < parms.length; parmNo++ ) {
			args[3 + parmNo] = parms[parmNo];
		}

		if ( enableDebugLogging ) {
			args[argCount - 2] = "-ll";
			args[argCount - 1] = "debug";
		}

		// System.out.println("Transformer arguments [ " + argCount + " ]");
		// for (int argNo = 0; argNo < argCount; argNo++) {
		// System.out.println(" [ " + argNo + " ]: [ " + args[argNo] + " ]");
		// }

		Transformer t = new Transformer(sysOutOutput, sysOutOutput);

		t.setArgs(args);
		t.setParsedArgs();

		TransformOptions options = t.createTransformOptions();
		options.setLogging();

		assertTrue( options.setInput(), "options.setInput() failed" );
		assertEquals( inputFileName, options.getInputFileName(),
			"input file name is not correct [ " + options.getInputFileName() + " ]" );

		assertTrue( options.setOutput(), "options.setOutput() failed" );
		assertEquals( outputFileName, options.getOutputFileName(),
			"output file name is not correct [ " + options.getOutputFileName() + " ]" );

		assertTrue(options.setRules(), "options.setRules() failed");
		assertTrue(options.acceptAction(), "options.acceptAction() failed");

		options.transform();

		sysOutOutput.close();

		byte[] sysOutBytes = sysOutByteOutput.toByteArray();
		ByteArrayInputStream sysOutByteInput = new ByteArrayInputStream(sysOutBytes, 0, sysOutBytes.length);
		TestUtils.verifyLog("property option logging", expectedLogFragments, sysOutByteInput);
		// throws IOException, AssertionFailedError

		assertTrue( (new File(outputFileName)).exists(), "output file not created" );

		return sysOutBytes;
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
