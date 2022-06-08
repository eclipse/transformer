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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;

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
		int expectedLen = expected.length;
		int minLength = ((actualLen < expectedLen) ? actualLen : expectedLen);

		for (int lineNo = 0; lineNo < minLength; lineNo++) {
			String expectedLine = expected[lineNo];
			String actualLine = actual.get(lineNo);
			if (!expectedLine.equals(actualLine)) {
				System.out.println("Mismatch [ " + tag + " ] [ " + lineNo + " ]" + " Expected [ " + expectedLine
					+ " ] Actual [ " + actualLine + " ]");
			}
		}
		if (minLength < actualLen) {
			for (int extraNo = minLength; extraNo < actualLen; extraNo++) {
				System.out.println("Extra [ " + tag + " ]: [ " + actual.get(extraNo) + " ]");
			}
		} else if (minLength < expectedLen) {
			for (int missingNo = minLength; missingNo < expectedLen; missingNo++) {
				System.out.println("Missing [ " + tag + " ]: [ " + expected[missingNo] + " ]");
			}
		}

		for (int lineNo = 0; lineNo < minLength; lineNo++) {
			Assertions.assertEquals(expected[lineNo], actual.get(lineNo), "Unequal lines [ " + lineNo + " ]");
		}

		Assertions.assertEquals(expectedLen, actualLen, "String [ " + tag + " ] length mismatch");
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

	public static List<String> loadLines(File inputFile) throws IOException {
		try (InputStream inputStream = new FileInputStream(inputFile)) {
			return loadLines(inputStream);
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
		File inputFile = new File(inputPath);
		IO.mkdirs(inputFile.getParentFile());
		try (OutputStream outputStream = new FileOutputStream(inputFile, true)) {
			PrintWriter outputWriter = new PrintWriter(outputStream);
			outputWriter.println(text);
			outputWriter.flush();
		}
	}

	//

	public static class ErrorAccumulator {
		public final String			name;

		public final int			capacity;
		protected int				size;
		public final List<String>	errors;

		public ErrorAccumulator(String name, int maxCount) {
			this.name = name;

			this.capacity = maxCount;
			this.size = 0;
			this.errors = new ArrayList<String>(maxCount);
		}

		/**
		 * Add a message. Answer true or false telling if the accumulator is
		 * full after adding the message. (Answer false if the accumulator is
		 * already full.)
		 *
		 * @param message The message which is to be added.
		 * @return True or full telling if the accumulator is already full, or
		 *         is full after adding the message.
		 */
		public boolean add(String message) {
			if (capacity == -1) {
				errors.add(message);
				return true;
			} else {
				if (size == capacity) {
					return false;
				} else {
					errors.add(message);
					return (++size < capacity);
				}
			}
		}

		/**
		 * Display the accumulated messages.
		 *
		 * @return True or false telling if there were no errors.
		 */
		public boolean display() {
			if (size == 0) {
				return true;
			}

			System.out.println("Errors: Output [ " + name + " ]: ");
			for (String error : errors) {
				System.out.println("  [ " + error + " ]");
			}

			if (size == capacity) {
				System.out.println("Maximum errors reached for output [ " + name + " ]");
			}

			return false;
		}
	}

	/**
	 * Standard maximum number of errors to accumulate.
	 */
	public static final int MAX_ERRORS = 10;

	/**
	 * Create a new error accumulator. Use {@link #MAX_ERRORS} as the capacity.
	 *
	 * @param name The name used by the accumulator.
	 * @return The new accumulator.
	 */
	public static ErrorAccumulator newErrors(String name) {
		return new ErrorAccumulator(name, MAX_ERRORS);
	}

	//

	//

	public static class InputMapping {
		public final File	inputFile;
		public final String	entryName;

		public InputMapping(File inputFile, String entryName) {
			this.inputFile = inputFile;
			this.entryName = entryName;
		}
	}

	public static void zip(File sourceDir, File zipFile) throws IOException {
		zip(sourceDir, zipFile, null);
	}

	public static void zip(File sourceDir, File zipFile, List<InputMapping> inputMappings) throws IOException {
		System.out.println("Source dir [ " + sourceDir.getAbsolutePath() + " ]");
		System.out.println("Output zip [ " + zipFile.getAbsolutePath() + " ]");

		Path rootSrcPath = sourceDir.toPath();
		Path zipPath = zipFile.toPath();

		IO.mkdirs(zipPath.getParent());

		try (ZipOutputStream zip = new ZipOutputStream(IO.outputStream(zipPath));
			Stream<Path> srcPaths = Files.walk(rootSrcPath)) {
			srcPaths.forEach(srcPath -> {
				boolean isDirectory = Files.isDirectory(srcPath);

				String entryName = rootSrcPath.relativize(srcPath)
					.toString()
					.replace('\\', '/');
				if (isDirectory) {
					entryName += '/';
				}
				if (entryName.equals("/")) {
					// Streaming includes the root input path.
					// That generates entry name "/", which should
					// never be added to the archive.
					return;
				}

				System.out.println("Zip entry [ " + entryName + " ]");

				ZipEntry zipEntry = new ZipEntry(entryName);
				try {
					zip.putNextEntry(zipEntry);
					try {
						if (!isDirectory) {
							IO.copy(srcPath, zip);
						}
					} finally {
						zip.closeEntry();
					}
				} catch (IOException e) {
					throw Exceptions.duck(e);
				}
			});

			if (inputMappings != null) {
				inputMappings.forEach(inputMapping -> {
					System.out.println(
						"Zip entry [ " + inputMapping.inputFile.getPath() + " ] as [ " + inputMapping.entryName + " ]");

					ZipEntry zipEntry = new ZipEntry(inputMapping.entryName);
					try {
						zip.putNextEntry(zipEntry);
						try {
							IO.copy(inputMapping.inputFile, zip);
						} finally {
							zip.closeEntry();
						}
					} catch (IOException e) {
						throw Exceptions.duck(e);
					}
				});
			}
		}
	}

	public static void unzip(File zipFile, File outputDir) throws IOException {
		System.out.println("Source zip [ " + zipFile.getAbsolutePath() + " ]");
		System.out.println("Output dir [ " + outputDir.getAbsolutePath() + " ]");

		Path out = IO.mkdirs(outputDir.toPath());
		try (ZipFile zip = new ZipFile(zipFile)) {
			zip.stream()
				.filter(entry -> !entry.isDirectory())
				.forEach(entry -> {
					System.out.println("Zip entry [ " + entry.getName() + " ]");
					Path resolved = out.resolve(entry.getName());
					try {
						IO.mkdirs(resolved.getParent());
						IO.copy(zip.getInputStream(entry), resolved);
					} catch (IOException e) {
						throw Exceptions.duck(e);
					}
				});
		}
	}

	//

	public static String addSlash(String path) {
		if (path.charAt(path.length() - 1) != File.separatorChar) {
			return path + File.separatorChar;
		} else {
			return path;
		}
	}

	public static File addSlash(File file) {
		String path = file.getPath();
		String slashPath = addSlash(path);
		if (slashPath != path) {
			file = new File(slashPath);
		}
		return file;
	}

	public static Set<String> convertSlashes(Set<String> dirNames) {
		if (File.separatorChar == '/') {
			return dirNames;
		}
		Set<String> convertedDirNames = new HashSet<>(dirNames.size());
		for (String dirName : dirNames) {
			dirName = dirName.replace(File.separatorChar, '/');
			convertedDirNames.add(dirName);
		}
		return convertedDirNames;
	}

	public static Set<String> list(File parent) {
		Set<String> childNames = new HashSet<>();

		File[] children = parent.listFiles();

		if (children != null) {
			for (File child : children) {
				childNames.add(child.getName());
			}
		}

		return childNames;
	}

	public static Set<String> listDirectories(File rootFile) throws IOException {
		Set<String> dirNames = new HashSet<>();

		rootFile = addSlash(rootFile);

		listDirectories(rootFile, rootFile, dirNames);

		return dirNames;
	}

	public static void listDirectories(File rootFile, File nextDir, Set<String> dirNames) throws IOException {
		if (nextDir != rootFile) {
			String rootPath = rootFile.getPath();
			String nextPath = nextDir.getPath();
			String nextRelPath = nextPath.substring(rootPath.length() + 1);

			nextRelPath = addSlash(nextRelPath);

			dirNames.add(nextRelPath);
		}

		for (File nextFile : nextDir.listFiles()) {
			if (nextFile.isDirectory()) {
				listDirectories(rootFile, nextFile, dirNames);
			}
		}
	}

	/**
	 * Tell if an entry name is the name of an archive entry.
	 * <p>
	 * This is a simplistic test which only handles ".zip" and ".jar"
	 * extensions.
	 *
	 * @param name The entry name which is to be tested.
	 * @return True or false telling if the name is an archive name.
	 */
	public static final boolean isArchive(String name) {
		return (name.endsWith(".zip") || name.endsWith(".jar"));
	}

	/**
	 * List the directories of a zip file. Include the names of directories in
	 * nested archives. Names from nested archives include their enclosing
	 * archive name, plus "-expanded". This parallels steps taken by
	 * {@link #unzip}, which uses the same name for expanded nested archives.
	 *
	 * @param rawZipFile The raw zip file which is to be listed.
	 * @return The names of directory entries of the zip file, including the
	 *         names of directories in nested archives.
	 * @throws IOException Thrown if an error occurrs reading the zip file.
	 */
	public static Set<String> listZipDirectories(File rawZipFile) throws IOException {
		Set<String> dirNames = new HashSet<>();
		try (ZipFile zipFile = new ZipFile(rawZipFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry nextEntry = entries.nextElement();
				String nextName = nextEntry.getName();
				if (nextEntry.isDirectory()) {
					dirNames.add(nextName);
				} else if (isArchive(nextName)) {
					try (InputStream inputStream = zipFile.getInputStream(nextEntry)) {
						listZipDirectories(inputStream, nextName + "-expanded/", dirNames);
					}
				} else {
					// skip it
				}
			}
		}
		return dirNames;
	}

	/**
	 * List the directories of a zip stream. Include the names of directories in
	 * nested archives. Names from nested archives include their enclosing
	 * archive name, plus "-expanded". This parallels steps taken by
	 * {@link #unzip}, which uses the same name for expanded nested archives.
	 *
	 * @param inputStream The input stream of a zip archive. Usually, a stream
	 *            obtained from a zip entry.
	 * @param prefix The prefix which is to prepended to directory names.
	 *            Usually, the name of an enclosing entry plus "-expanded".
	 * @param dirNames The accumulated directory names.
	 * @throws IOException Thrown if an error occurrs reading the zip file.
	 */
	private static void listZipDirectories(InputStream inputStream, String prefix, Set<String> dirNames)
		throws IOException {

		try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
			// A valid archive is not allowed to be empty.
			dirNames.add(prefix);

			ZipEntry nextEntry;
			while ((nextEntry = zipStream.getNextEntry()) != null) {
				String nextName = nextEntry.getName();
				if (nextEntry.isDirectory()) {
					dirNames.add(prefix + nextName);
				} else if (isArchive(nextName)) {
					listZipDirectories(zipStream, prefix + "-expanded/", dirNames);
				} else {
					// skip it
				}
			}
		}
	}

	/**
	 * Compare the directory entries of a zip file against an actual directory tree.
	 * Ignore the root directory of the actual directory.
	 * <p>
	 * The allowed missing parameter is necessary to handle incomplete name substitution.
	 * Parent folders of renamed directories will usually not be renamed.  For example,
	 * "javax/" is not renamed (as might be expected) when "javax/servlet/" is renamed
	 * to "jakarta/servlet/".  Also, "javax/servlet/" will be renamed even if there is
	 * a sub-package which is not renamed, for example, "javax/servlet/sub".
	 *
	 * @param zipFile The zip file which is to be tested.
	 * @param expectedParent The root of the expected directory tree.
	 * @param allowMissing List of directories which are expected to be missing.
	 * @param errors An accumulator of errors.
	 * @return True or false telling if the accumulator has room for more errors.
	 */
	public static boolean compareDirectories(
		File zipFile,
		File expectedParent, List<String> allowMissing,
		ErrorAccumulator errors) {

		String zipPath = zipFile.getPath();
		String expectedPath = expectedParent.getPath();
		System.out.println("Comparing directories of [ " + zipPath + " ] against [ " + expectedPath + " ]");

		Set<String> zipDirectories;
		try {
			zipDirectories = listZipDirectories(zipFile);
		} catch ( IOException e ) {
			e.printStackTrace();
			if ( !errors.add("IOException listing actual [ " + zipPath + " ]") ) {
				return false;
			} else {
				return true;
			}
		}

		if ( (allowMissing != null) && !allowMissing.isEmpty() ) {
			for ( String allow : allowMissing ) {
				zipDirectories.add(allow);
				System.out.println("Allow missing directory [ " + allow + " ]");
			}
		}

		Set<String> expectedDirectories;
		try {
			expectedDirectories = listDirectories(expectedParent);
		} catch ( IOException e ) {
			e.printStackTrace();
			if ( !errors.add("IOException listing expected [ " + expectedPath + " ]") ) {
				return false;
			} else {
				return true;
			}
		}
		expectedDirectories = convertSlashes(expectedDirectories);

		return compareNames(zipPath, zipDirectories, expectedPath, expectedDirectories, errors);
	}

	/**
	 * Compare two file trees.  The root of both trees are expected to be directories.
	 *
	 * @param actualParent The root directory which is to be tested.
	 * @param expectedParent The root directory which is to be tested against.
	 * @param errors An accumulator of errors.
	 * @return True or false telling if the accumulator has room for more errors.
	 */
	public static boolean compareFiles(File actualParent, File expectedParent, ErrorAccumulator errors) {
		String actualPath = actualParent.getPath();
		String expectedPath = expectedParent.getPath();
		System.out.println("Comparing [ " + actualPath + " ] against [ " + expectedPath + " ]");

		if (!actualParent.exists()) {
			return errors.add("actual [ " + actualPath + " ] does not exist");
		} else if (!actualParent.isDirectory()) {
			return errors.add("actual [ " + actualPath + " ] is not a directory");
		} else {
			Set<String> actualChildNames = list(actualParent);
			Set<String> expectedChildNames = list(expectedParent);
			return ( compareNames(actualPath, actualChildNames,
				                  expectedPath, expectedChildNames, errors) &&
				     compareChildren(actualParent, actualChildNames,
				    	             expectedParent, expectedChildNames, errors) );
		}
	}

	private static boolean compareChildren(
		File actualParent, Set<String> actualChildNames,
		File expectedParent, Set<String> expectedNames,
		ErrorAccumulator errors) {

		for (String childName : actualChildNames) {
			if (!expectedNames.contains(childName)) {
				continue;
			}

			File actualChild = new File(actualParent, childName);
			File expectedChild = new File(expectedParent, childName);

			if (expectedChild.isDirectory()) {
				if (!actualChild.isDirectory()) {
					if ( !errors.add("actual [ " + childName + " ] is not a directory") ) {
						return false;
					}
				} else {
					if ( !compareFiles(actualChild, expectedChild, errors) ) {
						return false;
					}
				}
				continue;
			}

			if (actualChild.isDirectory()) {
				if ( !errors.add("actual [ " + childName + " ] is a directory") ) {
					return false;
				} else {
					continue;
				}
			}

			List<String> actualLines;
			try {
				actualLines = TestUtils.loadLines(actualChild);
			} catch (IOException e) {
				if ( !errors.add("failed to read actual [ " + childName + " ]") ) {
					return false;
				} else {
					continue;
				}
			}

			List<String> expectedLines;
			try {
				expectedLines = TestUtils.loadLines(actualChild);
			} catch (IOException e) {
				if ( !errors.add("failed to read expected [ " + childName + " ]") ) {
					return false;
				} else {
					continue;
				}
			}

			if ( !compareLines(childName, actualLines, childName, expectedLines, errors) ) {
				return false;
			}
		}

		return true;
	}

	public static boolean compareLines(
		String actualTag, List<String> actualLines,
		String expectedTag, List<String> expectedLines,
		ErrorAccumulator errors) {

		int actualCount = actualLines.size();
		int expectedCount = expectedLines.size();

		if (actualCount != expectedCount) {
			if ( !errors.add("actual size [ " + actualCount + " ] expected size [ " + expectedCount + " ]") ) {
				return false;
			}
		}

		int minCount = ((actualCount > expectedCount) ? expectedCount : actualCount);

		for (int lineNo = 0; lineNo < minCount; lineNo++) {
			String actualLine = actualLines.get(lineNo);
			String expectedLine = expectedLines.get(lineNo);
			if (actualLine.equals(expectedLine)) {
				continue;
			}

			if (!errors.add("line [ " + lineNo + " ] actual [ " + actualLine + " ] expected [ " + expectedLine + " ]")) {
				return false;
			}
		}

		return true;
	}

	public static boolean compareNames(
		String actualTag, Set<String> actual,
		String expectedTag, Set<String> expected,
		ErrorAccumulator errors) {

		int actualCount = actual.size();
		int expectedCount = expected.size();

		if (actualCount != expectedCount) {
			if (!errors.add("actual size [ " + actualCount + " ] expected size [ " + expectedCount + " ]")) {
				return false;
			}
		}

		for (String nextExpected : expected) {
			if (!actual.contains(nextExpected)) {
				if (!errors.add("missing [ " + nextExpected + " ]")) {
					return false;
				}
			}
		}

		for (String nextActual : actual) {
			if (!expected.contains(nextActual)) {
				if (!errors.add("extra [ " + nextActual + " ]")) {
					return false;
				}
			}
		}

		return true;
	}
}
