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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.TransformOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public abstract class TestTransformerBase {

	// TODO: This is currently used for input data and for output data.
	//       Output data should be written to a build location, which is
	//       much safer.

	private static final String	DATA_DIR = "src/test/data";

	public String getDataDir() {
		return DATA_DIR;
	}

	private String currentDirectory = ".";

	public String getCurrentDir() {
		return currentDirectory;
	}

	//

	@BeforeEach
	public void setUp() {
		currentDirectory = System.getProperty("user.dir");
		System.out.println("setUp: Current directory is: [" + currentDirectory + "]");
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

		System.out.println("Transformer arguments [ " + argCount + " ]");
		for (int argNo = 0; argNo < argCount; argNo++) {
			System.out.println("  [ " + argNo + " ]: [ " + args[argNo] + " ]");
		}

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
		verifyLog("property option logging", expectedLogFragments, sysOutByteInput);
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

	//

	/**
	 * Verify log text using an array of text fragments.
	 *
	 * If verification fails, display the generated validation failure
	 * messages, and throw an error.
	 *
	 * If verification succeeds, display a success message.
	 *
	 * Verification is performed by {@link #verifyLog(String[], InputStream)}.
	 *
	 * @param description A description to display with verification results.
	 * @param expectedFragments Text fragments which are expected in the
	 *     captured log text.
	 * @param logStream A stream containing the captured log text.
	 *
	 * @throws IOException Thrown in case of a failure to read the log stream.
	 *
	 * @throws AssertionFailedError Thrown if verification fails.
	 */
	public void verifyLog(String description, String[] expectedFragments, InputStream logStream)
		throws IOException, AssertionFailedError {

		List<String> errors = verifyLog(expectedFragments, logStream); // throws IOException
		processErrors(description, errors); // throws AssertionFailedError
	}

	/**
	 * Verify log text using an array of text fragments.
	 *
	 * Currently, each fragment must occur exactly once.
	 *
	 * Generate an error message for each missing text fragment, and for
	 * each text fragment which is detected more than once.
	 *
	 * @param expectedFragments The text fragments which are expected to be present
	 *     in the log stream
	 * @param logStream A stream containing captured log output.
	 *
	 * @return The list of error obtained whilst searching the log text for
	 *     the text fragments.
	 *
	 * @throws IOException Thrown in case of an error reading the log stream.
	 */
	public List<String> verifyLog(String[] expectedFragments, InputStream logStream) throws IOException {
		List<String> errors = new ArrayList<String>();

		BufferedReader logReader = new BufferedReader( new InputStreamReader(logStream) );

		int expectedMatches =  expectedFragments.length;
		boolean[] matches = new boolean[expectedMatches];

		String nextLine;
		while ( (nextLine = logReader.readLine()) != null ) { // throws IOException
			// System.out.println("Log [ " + nextLine + " ]");

			for ( int fragmentNo = 0; fragmentNo < expectedMatches; fragmentNo++ ) {
				String fragment = expectedFragments[fragmentNo];
				if ( !nextLine.contains(fragment) ) {
					continue;
				}

				if ( matches[fragmentNo] ) {
					errors.add("Extra occurrence of log text [ " + fragment + " ]");
				} else {
					System.out.println("Match on log fragment [ " + fragment + " ]");
					matches[fragmentNo] = true;
				}
			}
		}

		for ( int fragmentNo = 0; fragmentNo < expectedMatches; fragmentNo++ ) {
			if ( !matches[fragmentNo] ) {
				errors.add("Missing occurrence of log text [ " + expectedFragments[fragmentNo] + " ]");
			}
		}

		return errors;
	}

	/**
	 * Examine a collection of errors, and fail with an assertion error
	 * if any errors are present.  Display the errors to <code>System.out</code>
	 * before throwing the error.
	 *
	 * @param description A description to display with a failure message if
	 *     errors are detected, and with a success message if no errors are
	 *     present.
	 * @param errors The errors which are to be examined.
	 *
	 * @throws AssertionFailedError Thrown if any errors are present.
	 */
	public void processErrors(String description, List<String> errors)
		throws AssertionFailedError {

		if ( errors.isEmpty() ) {
			System.out.println("Correct " + description);

		} else {
			description = "Incorrect " + description;
			System.out.println(description);
			for ( String error : errors ) {
				System.out.println(error);
			}
			fail(description);
		}
	}
}
