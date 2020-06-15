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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;

public class TestUtils {

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
}
