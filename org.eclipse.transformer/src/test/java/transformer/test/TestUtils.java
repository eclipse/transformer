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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class TestUtils {

	public static final boolean DO_CREATE = true;

	public static void verifyDirectory(String targetPath, boolean create, String description) {
		String methodName = "verifyDirectory";

		File targetFile = new File(targetPath);
		String targetAbsPath = targetFile.getAbsolutePath();

		if ( !targetFile.exists() ) {
			if ( create ) {
				System.out.println(methodName + ": Creating " + description + " directory [ " + targetAbsPath + " ]");
				targetFile.mkdirs();
			}
		}

		if (!targetFile.exists() ) {
			fail(methodName + ": Failure: Could not create " + description + " ] directory [ " + targetAbsPath + " ]");
		} else if ( !targetFile.isDirectory() ) {
			fail(methodName + ": Failure: Location " + description + " is not a directory [ " + targetAbsPath + " ]");
		} else {
			System.out.println(methodName + ": Success: Location " + description + " exists and is a directory [ " + targetAbsPath + " ]");
		}
	}

	public static InputStream getResourceStream(String path) {
		return TestUtils.class.getClassLoader()
			.getResourceAsStream(path);
	}

	public static void verify(String tag, String[] expected, List<String> actual) {
		int actualLen = actual.size();

		int minLength = expected.length;
		if (minLength > actualLen) {
			minLength = actualLen;
		}

		for (int lineNo = 0; lineNo < expected.length; lineNo++) {
			Assertions.assertEquals(expected[lineNo], actual.get(lineNo), "Unequal lines [ " + lineNo + " ]");
		}

		Assertions.assertEquals(expected.length, actual.size(), "String [ " + tag + " ] length mismatch");
	}

	public static void filter(List<String> lines) {
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String nextLine = iterator.next();
			String trimLine = nextLine.trim();
			if (trimLine.isEmpty() || (trimLine.charAt(0) == '#')) {
				iterator.remove();
			}
		}
	}

	public static List<String> loadLines(InputStream inputStream) throws IOException {
		InputStreamReader reader = new InputStreamReader(inputStream);
		BufferedReader lineReader = new BufferedReader(reader);

		List<String> lines = new ArrayList<>();
		String line;
		while ((line = lineReader.readLine()) != null) {
			lines.add(line);
		}

		return lines;
	}

	public static int occurrences(List<String> lines, String tag) {
		int occurrences = 0;
		for (String line : lines) {
			occurrences += occurrences(line, tag);
		}
		return occurrences;
	}

	public static int occurrences(String line, String tag) {
		int occurrences = 0;

		int tagLen = tag.length();

		int limit = line.length() - tagLen;
		int lastFindLoc = 0;
		while (lastFindLoc <= limit) {
			lastFindLoc = line.indexOf(tag, lastFindLoc);
			if (lastFindLoc == -1) {
				lastFindLoc = limit + 1;
			} else {
				lastFindLoc += tagLen;
				occurrences++;
			}
		}

		return occurrences;
	}

	public static List<String> manifestCollapse(List<String> inputManifestLines) {
		List<String> outputManifestLines = new ArrayList<>();
		StringBuilder outputBuilder = new StringBuilder();
		for (String inputLine : inputManifestLines) {
			if (inputLine.isEmpty()) {
				continue; // Unexpected
			}

			if (inputLine.charAt(0) == ' ') {
				int lineLen = inputLine.length();
				for (int charNo = 1; charNo < lineLen; charNo++) {
					outputBuilder.append(inputLine.charAt(charNo));
				}
			} else {
				if (outputBuilder.length() > 0) {
					outputManifestLines.add(outputBuilder.toString());
					outputBuilder.setLength(0);
				}
				outputBuilder.append(inputLine);
			}
		}
		if (outputBuilder.length() > 0) {
			outputManifestLines.add(outputBuilder.toString());
			outputBuilder.setLength(0);
		}

		return outputManifestLines;
	}

	public static void transfer(String streamName, InputStream inputStream, OutputStream outputStream, byte[] buffer)
		throws IOException {

		int bytesRead;
		long totalBytes = 0L;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
			totalBytes += bytesRead;
		}

		System.out.println("Transferred [ " + totalBytes + " ] from [ " + streamName + " ]");
	}

	//

	public static Map<String, String> loadPackageVersions(
		String description, String manifestPath, String attributeName)
		throws IOException {

		Manifest manifest = new Manifest();

		try ( InputStream manifestStream = new FileInputStream(manifestPath) ) {
			manifest.read(manifestStream);
		}

		Map<String, String> packageData = new HashMap<String, String>();

		String targetAttribute = manifest.getMainAttributes().getValue(attributeName);
		System.out.println(description + " [ " + manifestPath + " ]:");
		System.out.println("  [ " + targetAttribute + " ]");

		StringTokenizer tokenizer = new StringTokenizer(targetAttribute, ",", false);
		while ( tokenizer.hasMoreElements() ) {
			String nextToken = tokenizer.nextToken();

			int packageEnd = nextToken.indexOf(';');
			String packageName = nextToken.substring(0, packageEnd);

			int versionStart = nextToken.indexOf("version=\"") + "version=\"".length();
			int versionEnd = nextToken.length() - 1; // -1 to omit the closing '"'

			String version = nextToken.substring(versionStart, versionEnd);

			System.out.println("  [ " + nextToken + " ]: [ " + packageName + " ] [ " + version + " ]");

			packageData.put(packageName, version);
		}

		return packageData;
	}

	public static void verifyPackageVersions(
		String description, String actualFileName, Map<String, String> expected,
		String attributeName)
		throws IOException, AssertionFailedError {

		List<String> errors = verifyPackageVersions(
			loadPackageVersions(description, actualFileName, attributeName),
			expected );

		processErrors(description, errors);
	}

	public static List<String> verifyPackageVersions(Map<String, String> actual, Map<String, String> expected) {
		List<String> errors = new ArrayList<String>();

		for ( Map.Entry<String, String> actualEntry : actual.entrySet() ) {
			String actualName = actualEntry.getKey();
			String actualVersion = actualEntry.getValue();

			String expectedVersion = expected.get(actualName);

			if ( expectedVersion == null ) {
				errors.add("Extra actual: Package [ " + actualName + " ] version [ " + actualVersion + " ]");
			} else if ( !actualVersion.equals(expectedVersion) ) {
				errors.add("Incorrect actual: Package [ " + actualName + " ] version [ " + actualVersion + " ]; expected [ " + expectedVersion + " ]");
			} else {
				// OK
			}
		}

		for ( Map.Entry<String, String> expectedEntry : expected.entrySet() ) {
			String expectedName = expectedEntry.getKey();
			String expectedVersion = expectedEntry.getValue();

			String actualVersion = actual.get(expectedName);

			if ( actualVersion == null ) {
				errors.add("Missing expected: Package [ " + expectedName + " ] version [ " + expectedVersion + " ]");
			} else {
				// OK, or already detected
			}
		}

		return errors;
	}

	/**
	 * Verify log text using an array of text fragments. If verification fails,
	 * display the generated validation failure messages, and throw an error. If
	 * verification succeeds, display a success message. Verification is
	 * performed by {@link #verifyLog(List, Collection)}.
	 *
	 * @param description A description to display with verification results.
	 * @param expectedFragments Text fragments which are expected in the
	 *            captured log text.
	 * @param logEvents A stream containing the captured log text.
	 * @throws IOException Thrown in case of a failure to read the log stream.
	 * @throws AssertionFailedError Thrown if verification fails.
	 */
	public static void verifyLog(String description, List<String> expectedFragments,
		Collection<? extends LoggingEvent> logEvents)
		throws IOException, AssertionFailedError {

		List<String> errors = verifyLog(expectedFragments, logEvents);
		processErrors(description, errors);
	}

	/**
	 * Verify log text using an array of text fragments. Currently, each
	 * fragment must occur exactly once. Generate an error message for each
	 * missing text fragment, and for each text fragment which is detected more
	 * than once.
	 *
	 * @param expectedFragments The text fragments which are expected to be
	 *            present in the log stream
	 * @param logEvents A stream containing captured log output.
	 * @return The list of error obtained whilst searching the log text for the
	 *         text fragments.
	 * @throws IOException Thrown in case of an error reading the log stream.
	 */
	public static List<String> verifyLog(List<String> expectedFragments, Collection<? extends LoggingEvent> logEvents)
		throws IOException {
		List<String> errors = new ArrayList<String>();

		int expectedMatches = expectedFragments.size();
		boolean[] matches = new boolean[expectedMatches];

		for (LoggingEvent logEvent : logEvents) {
			FormattingTuple tp = MessageFormatter.arrayFormat(logEvent.getMessage(), logEvent.getArgumentArray(),
				logEvent.getThrowable());
			String nextLine = tp.getMessage();
			// System.out.println("Log [ " + nextLine + " ]");

			for ( int fragmentNo = 0; fragmentNo < expectedMatches; fragmentNo++ ) {
				String fragment = expectedFragments.get(fragmentNo);
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
				errors.add("Missing occurrence of log text [ " + expectedFragments.get(fragmentNo) + " ]");
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
	public static void processErrors(String description, List<String> errors)
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

	//

	public static String readOutputFile(String outputPath) throws IOException {
		try ( InputStream inputStream = new FileInputStream(outputPath) ) {
			BufferedReader inputReader = new BufferedReader( new InputStreamReader(inputStream) );
			String outputLine = inputReader.readLine();
			return outputLine;
		}
	}

	public static List<String> verifyOutputFiles(
		String outputDir,
		int numFiles, int numExts,
		BiFunction<Integer, Integer, String> getInputName,
		Function<Integer, String> getExtension,
		Map<String, String> outputMap)
		throws IOException {

		List<String> errors = new ArrayList<String>();

		for ( int fileNo = 0; fileNo < numFiles; fileNo++ ) {
			for ( int extNo = 0; extNo < numExts; extNo++ ) {
				String outputName = getInputName.apply(fileNo, extNo);
				String outputPath = outputDir + '/' + outputName;
				String outputLine = readOutputFile(outputPath);

				String outputExt = getExtension.apply(extNo);

				String expectedOutput = outputMap.get(outputExt);

				if ( !outputLine.equals(expectedOutput) ) {
					String error = "Incorrect content [ " + outputName + " ]; expected [ " + expectedOutput + " ] got [ " + outputLine + " ]";
					errors.add(error);
				}
			}
		}

		return errors;
	}

	public static void verifyOutput(
		String outputDir,
		int numFiles, int numExts,
		BiFunction<Integer, Integer, String> getInputName,
		Function<Integer, String> getExtension,
		Map<String, String> outputMap)
		throws IOException, AssertionFailedError {

		List<String> outputErrors = verifyOutputFiles(outputDir, numFiles, numExts, getInputName, getExtension, outputMap);
		processErrors("expected output", outputErrors);
	}

	//

	public static void writeInputData(
		String inputDir, int numFiles, int numExts,
		BiFunction<Integer, Integer, String> getInputName,
		String text)
		throws IOException {

		for ( int fileNo = 0; fileNo < numFiles; fileNo++ ) {
			for ( int extNo = 0; extNo < numExts; extNo++ ) {
				String inputName = getInputName.apply(fileNo, extNo);
				String inputPath = inputDir + '/' + inputName;
				writeInputFile(inputPath, text);
			}
		}
	}

	public static void writeInputFile(String inputPath, String text) throws IOException {
		try ( OutputStream outputStream = new FileOutputStream(inputPath, true) ) {
			PrintWriter outputWriter = new PrintWriter(outputStream);
			outputWriter.println(text);
			outputWriter.flush();
		}
	}
}
