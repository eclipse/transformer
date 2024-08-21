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

import static org.junit.jupiter.api.Assertions.fail;
import static transformer.test.TestUtils.newErrors;
import static transformer.test.TestUtils.unzip;
import static transformer.test.TestUtils.zip;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.io.IO;
import org.eclipse.transformer.AppOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import transformer.test.TestUtils.ErrorAccumulator;
import transformer.test.TestUtils.InputMapping;

public class TestTransformerJava extends TestTransformerBase {

	public static final String						STATIC_CONTENT_DIR	= "src/test/data/java";
	public static final String						DYNAMIC_CONTENT_DIR	= "target/test/data/java";

	protected static File							input;
	protected static String							inputPath;

	protected static File							output;
	protected static String							outputPath;

	protected static File							inputSrc;
	protected static String							inputSrcPath;

	protected static File							outputSrc;
	protected static String							outputSrcPath;

	protected static File							inputZip;
	protected static String							inputZipPath;

	protected static File							outputZip;
	protected static String							outputZipPath;

	protected static File							outputZipExpanded;
	protected static String							outputZipExpandedPath;

	protected static File							renamesFile;
	protected static String							renamesPath;

	protected static Map<AppOption, List<String>>	options;

	protected static File							expectedSrc;
	protected static String							expectedSrcPath;

	//

	protected static final String					INPUT_NESTED_PATH	= "javax/servlet/input.jar";
	protected static final String					OUTPUT_NESTED_PATH	= "jakarta/servlet/input.jar";

	protected static File							inputNestedZip;
	protected static String							inputNestedZipPath;

	protected static File							outputNestedZip;
	protected static String							outputNestedZipPath;

	protected static File							outputNestedZipExpanded;
	protected static String							outputNestedZipExpandedPath;

	protected static File							expectedNested;
	protected static String							expectedNestedPath;

	@BeforeAll
	public static void setUp() throws Exception {
		input = new File(STATIC_CONTENT_DIR);
		inputPath = input.getPath();

		output = new File(DYNAMIC_CONTENT_DIR);
		outputPath = output.getPath();

		System.out.println("Input [ " + inputPath + " ]");
		System.out.println("Output [ " + outputPath + " ]");

		inputSrc = new File(input, "input-src");
		inputSrcPath = inputSrc.getPath();

		outputSrc = new File(output, "output-src");
		outputSrcPath = outputSrc.getPath();

		System.out.println("Input Source [ " + inputSrcPath + " ]");
		System.out.println("Output Source [ " + outputSrcPath + " ]");

		inputZip = new File(output, "input.zip");
		inputZipPath = inputZip.getPath();

		outputZip = new File(output, "output.zip");
		outputZipPath = outputZip.getPath();
		outputZipExpanded = new File(output, "output.zip-expanded");
		outputZipExpandedPath = outputZipExpanded.getPath();

		System.out.println("Input Zip [ " + inputZipPath + " ]");
		System.out.println("Output Zip [ " + outputZipPath + " ]");
		System.out.println("Output Zip Expanded [ " + outputZipExpandedPath + " ]");

		renamesFile = new File(input, "rules/renames.properties");
		renamesPath = renamesFile.getPath();
		System.out.println("Renames [ " + renamesPath + " ]");

		options = new HashMap<>();
		options.put(AppOption.OVERWRITE, List.of("true"));
		options.put(AppOption.LOG_LEVEL, List.of("TRACE"));
		options.put(AppOption.RULES_RENAMES, List.of(renamesPath));

		System.out.println("Overwrite is enabled");
		System.out.println("Logging is set to debug");

		expectedSrc = new File(input, "expected-src");
		expectedSrcPath = expectedSrc.getPath();

		System.out.println("Expected Source [ " + expectedSrcPath + " ]");
		System.out.println("Expected Source [ " + expectedSrcPath + " ]");

		//

		IO.deleteContent(output);
		IO.mkdirs(output);

		zip(inputSrc, inputZip);

		//

		inputNestedZip = new File(output, "input-nested.zip");
		inputNestedZipPath = inputNestedZip.getPath();

		outputNestedZip = new File(output, "output-nested.zip");
		outputNestedZipPath = outputNestedZip.getPath();

		outputNestedZipExpanded = new File(output, "output-nested.zip-expanded");
		outputNestedZipExpandedPath = outputNestedZipExpanded.getPath();

		expectedNested = new File(input, "expected-nested");
		expectedNestedPath = expectedNested.getPath();

		System.out.println("Input Nested path [ " + INPUT_NESTED_PATH + " ]");
		System.out.println("Output Nested path [ " + OUTPUT_NESTED_PATH + " ]");

		System.out.println("Input Nested Zip [ " + inputNestedZipPath + " ]");
		System.out.println("Output Nested Zip [ " + outputNestedZipPath + " ]");
		System.out.println("Output Nested Zip Expanded [ " + outputNestedZipExpandedPath + " ]");
		System.out.println("Expected Nested [ " + expectedNestedPath + " ]");

		InputMapping nestedInput = new InputMapping(inputZip, INPUT_NESTED_PATH);
		List<InputMapping> nestings = new ArrayList<>(1);
		nestings.add(nestedInput);

		zip(inputSrc, inputNestedZip, nestings);
	}

	@Test
	void testJavaDir() throws Exception {
		System.out.println("Test JSP and Java package renames; source [ " + inputSrcPath + " ]");

		runTransformer(inputSrcPath, outputSrcPath, options);

		ErrorAccumulator errors = newErrors(outputSrcPath);

		TestUtils.compareFiles(outputSrc, expectedSrc, errors);

		if (errors.display()) {
			System.out.println("Verified output [ " + outputSrcPath + " ]");
		} else {
			fail("Failed to verify output [ " + outputSrcPath + " ]");
		}

	}

	@Test
	void testJavaZip() throws Exception {
		System.out.println("Test JSP and Java package renames; source [ " + inputZipPath + " ]");

		runTransformer(inputZipPath, outputZipPath, options, !DO_LOG);

		ErrorAccumulator errors = newErrors(outputZipPath);

		unzip(outputZip, outputZipExpanded);

		// "javax" is not renamed:
		// The rename might be implied by the "javax/servlet" to
		// "jakarta/servlet".
		//
		// "javax/servlet" is renamed to "jakarta/servlet": Keeping
		// "javax/servlet"
		// might be implied by "javax/servlet/sub" not being renamed.

		List<String> expectedMissing = new ArrayList<>(2);
		expectedMissing.add("jakarta/");
		expectedMissing.add("javax/servlet/");

		if (TestUtils.compareDirectories(outputZip, expectedSrc, expectedMissing, errors)) {
			TestUtils.compareFiles(outputZipExpanded, expectedSrc, errors);
		}

		if (errors.display()) {
			System.out.println("Verified output [ " + outputZipPath + " ]");
		} else {
			fail("Failed to verify output [ " + outputZipPath + " ]");
		}
	}

	@Test
	void testJavaNestedZip() throws Exception {
		System.out.println("Test JSP and Java package renames; source [ " + inputNestedZipPath + " ]");

		runTransformer(inputNestedZipPath, outputNestedZipPath, options);

		unzip(outputNestedZip, outputNestedZipExpanded);

		File nestedZip = new File(outputNestedZipExpanded, OUTPUT_NESTED_PATH);
		String nestedZipPath = nestedZip.getPath();

		String nestedExpandedPath = OUTPUT_NESTED_PATH + "-expanded";
		File nestedZipExpanded = new File(outputNestedZipExpanded, nestedExpandedPath);

		unzip(nestedZip, nestedZipExpanded);
		nestedZip.delete();

		// See 'testJavaZip' for an explanation.

		List<String> expectedMissing = new ArrayList<>(2);
		expectedMissing.add("javax/servlet/");
		expectedMissing.add("jakarta/");
		expectedMissing.add("jakarta/servlet/input.jar-expanded/javax/servlet/");
		expectedMissing.add("jakarta/servlet/input.jar-expanded/jakarta/");

		ErrorAccumulator errors = newErrors(outputNestedZipPath);

		if (TestUtils.compareDirectories(outputNestedZip, expectedNested, expectedMissing, errors)) {
			TestUtils.compareFiles(outputNestedZipExpanded, expectedNested, errors);
		}

		if (errors.display()) {
			System.out.println("Verified output [ " + outputNestedZipPath + " ]");
		} else {
			fail("Failed to verify output [ " + outputNestedZipPath + " ]");
		}
	}
}

