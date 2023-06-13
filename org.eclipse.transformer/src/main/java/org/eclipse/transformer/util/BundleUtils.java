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

package org.eclipse.transformer.util;

import org.slf4j.Logger;

/**
 * Utility methods for bundle processing.
 */
public final class BundleUtils {
	private BundleUtils() {
		// utility class
	}

	// DynamicImport-Package: com.ibm.websphere.monitor.meters;version="1.0.0
	// ",com.ibm.websphere.monitor.jmx;version="1.0.0",com.ibm.ws.jsp.webcon
	// tainerext,com.ibm.wsspi.request.probe.bci,com.ibm.wsspi.probeExtensio
	// n,com.ibm.ws.webcontainer.monitor

	// Import-Package: javax.servlet;version="[2.6,3)",javax.servlet.annotati
	// on;version="[2.6,3)",javax.servlet.descriptor;version="[2.6,3)",javax
	// .servlet.http;version="[2.6,3)",com.ibm.wsspi.http;version="[2.0,3)",
	// com.ibm.ws.javaee.dd;version="1.0",com.ibm.ws.javaee.dd.common;versio
	// n="1.0",com.ibm.ws.javaee.dd.common.wsclient;version="1.0",com.ibm.ws
	// .javaee.dd.web;version="1.0",com.ibm.ws.javaee.dd.web.common;version=
	// "1.0",com.ibm.ws.util;version="[1.0,2)",com.ibm.wsspi.injectionengine
	// ;version="[3.0,4)",com.ibm.ws.runtime.metadata;version="[1.1,2)"

	/**
	 * Answer package attribute text which has been updated with a new version
	 * range. Examples
	 *
	 * <pre>
	 * Import-Package: javax.servlet;version="[2.6,3)",javax.servlet.annotation;version="[2.6,3)"
	 * </pre>
	 *
	 * <pre>
	 * DynamicImport-Package: com.ibm.websphere.monitor.meters;version="1.0.0",com.ibm.websphere.monitor.jmx;version="1.0.0"
	 * </pre>
	 *
	 * The leading package name must be removed from the attribute text. Other
	 * package names and attributes may be present. Attribute text for different
	 * packages use commas as separators, except, commas inside quotation marks
	 * are not separators. This is important because commas are present in
	 * version ranges.
	 *
	 * @param logger: Logger instance
	 * @param text Package attribute text.
	 * @param newVersion Replacement version values for the package attribute.
	 * @return String with version numbers of first package replaced by the
	 *         newVersion.
	 */
	public static String replacePackageVersion(final Logger logger, String text, String newVersion) {
		// getLogger().debug("replacePackageVersion: ( {} )", text );

		String packageText = getPackageAttributeText(logger, text);

		// System.out.println("Package text [ " + packageText + " ]");

		if (packageText == null) {
			return text;
		} else if (packageText.isEmpty()) {
			return text;
		}

		// getLogger().debug("replacePackageVersion: (packageText: {} )",
		// packageText);

		final String VERSION = "version";
		final int VERSION_LEN = 7;
		final char QUOTE_MARK = '\"';

		int versionIndex = packageText.indexOf(VERSION);
		if (versionIndex == -1) {
			return text; // nothing to replace
		}

		// The actual version numbers are after the "version" and the "=" and
		// between quotation marks ("").
		// Ignore white space that occurs around the "=", but do not ignore
		// white space between quotation marks.
		// Everything inside the "" is part of the version and will be replaced.
		boolean foundEquals = false;
		boolean foundQuotationMark = false;
		int versionBeginIndex = -1;
		int versionEndIndex = -1;

		// skip to actual version number which is after "=". Version begins
		// inside double quotation marks
		for (int i = versionIndex + VERSION_LEN; i < packageText.length(); i++) {
			char ch = packageText.charAt(i);

			// skip white space until we find equals sign
			if (!foundEquals) {
				if (ch == '=') {
					foundEquals = true;
					continue;
				}

				if (Character.isWhitespace(ch)) {
					continue;
				}
				logger.error("Found a non-white-space character before the equals sign, in package text [ {} ]",
								  packageText);
				return text; // Syntax error - returning original text
			}

			// Skip white space past the equals sign
			if (Character.isWhitespace(ch)) {
				// getLogger().debug("ch is \'{}\' and is whitespace.", ch);
				continue;
			}

			// When we find the quotation marks past the equals sign, we are
			// finished.
			if (!foundQuotationMark) {
				if (ch == QUOTE_MARK) {
					versionBeginIndex = i + 1; // just past the 1st quotation mark

					versionEndIndex = packageText.indexOf('\"', i + 1);
					if (versionEndIndex == -1) {
						logger.error("Version does not have a closing quotation mark, in package text [ {} ]",
										  packageText);
						return text; // Syntax error - returning original text
					}
					versionEndIndex--; // just before the 2nd quotation mark

					// getLogger().debug("versionBeginIndex = [{}]",
					// versionBeginIndex);
					// getLogger().debug("versionEndIndex = [{}]",
					// versionEndIndex);
					foundQuotationMark = true; // not necessary, just leave loop
					break;
				}

				if (Character.isWhitespace(ch)) {
					continue;
				}

				logger.error("Found a non-white-space character after the equals sign, in package text [ {} ]",
								  packageText);
				return text; // Syntax error - returning original text
			}
		}

		// String oldVersion = packageText.substring(versionBeginIndex,
		// versionEndIndex+1);
		// getLogger().debug("old version[{}] new version[{}]", oldVersion,
		// newVersion);

		String head = text.substring(0, versionBeginIndex);
		String tail = text.substring(versionEndIndex + 1);

		String newText = head + newVersion + tail;
		// getLogger().debug("Old [{}] New [{}]", text , newText);

		return newText;
	}

	//
	// Subsystem-Content: com.ibm.websphere.appserver.javax.el-3.0;
	// apiJar=false; type="osgi.subsystem.feature",
	// com.ibm.websphere.appserver.javax.servlet-3.1; ibm.tolerates:="4.0";
	// apiJar=false; type="osgi.subsystem.feature",
	// com.ibm.websphere.javaee.jsp.2.3; location:="dev/api/spec/,lib/";
	// mavenCoordinates="javax.servlet.jsp:javax.servlet.jsp-api:2.3.1";
	// version="[1.0.0,1.0.200)"

	/**
	 * @param text - A string containing package attribute text at the head of
	 *            the string. Assumptions: - The first package name has already
	 *            been stripped from the embedding text. - Other package names
	 *            and attributes may or may not follow. - Packages are separated
	 *            by a comma. - If a comma is inside quotation marks, it is not
	 *            a package delimiter.
	 * @return package attribute text
	 */
	public static String getPackageAttributeText(final Logger logger, String text) {
		// getLogger().trace("getPackageAttributeText ENTER[ text: {}]", text);

		if (text == null) {
			return null;
		}

		if (!firstCharIsSemicolon(text)) {
			return ""; // no package attributes
		}

		int commaIndex = text.indexOf(',');
		// getLogger().trace("Comma index: [{}]", commaIndex);
		// If there is no comma, then the whole text is the packageAttributeText
		if (commaIndex == -1) {
			return text;
		}

		// packageText is beginning of text up to and including comma.
		// Need to test whether the comma is within quotes - thus not the true
		// end of the packageText.
		// If an odd number of quotes are found, then the comma is in quotes and
		// we need to find the next comma.
		String packageText = text.substring(0, commaIndex + 1);
		logger.trace("packageText [ {} ]", packageText);

		while (!isPackageDelimitingComma(text, packageText, commaIndex)) {
			commaIndex = text.indexOf(',', packageText.length());
			if (commaIndex == -1) {
				packageText = text; // No trailing comma indicates embedding
				// text is the package text.
				break;
			} else {
				packageText = text.substring(0, commaIndex + 1);
			}

			// If there is a syntax error (missing closing quotes) return what
			// we have
			if (!hasEvenNumberOfOccurrencesOfChar(text, '\"')) {
				break;
			}
		}

		logger.trace("getPackageAttributeText returning: [ {} ]", packageText);
		return packageText;
	}

	/**
	 * Tell if the first non-white space character of the parameter is a
	 * semi-colon.
	 *
	 * @param s string
	 * @return true if first char is semi colon.
	 */
	public static boolean firstCharIsSemicolon(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				continue;
			}
			if (s.charAt(i) == ';') {
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * @param testString - The entire remaining unprocessed text of a
	 *            MANIFEST.MF attribute that immediately follows a package name
	 * @param packageText - Text that immediately follows a package name in a
	 *            MANIFEST.MF attribute
	 * @param indexOfComma
	 * @return
	 */
	public static  boolean isPackageDelimitingComma(String testString, String packageText, int indexOfComma) {

		int indexOfNextNonWhiteSpaceCharAfterComma = indexOfNextNonWhiteSpaceChar(testString, indexOfComma + 1);
		char characterAfterComma = testString.charAt(indexOfNextNonWhiteSpaceCharAfterComma);
		if (Character.isAlphabetic(characterAfterComma)) {
			if (!hasEvenNumberOfOccurrencesOfChar(packageText, '\"')) {
				return false;
			}
			return true;
		}

		return false;
	}

	public static boolean hasEvenNumberOfOccurrencesOfChar(String testString, @SuppressWarnings("unused") char testChar) {
		long occurrences = testString.chars()
									 .filter(ch -> ch == '\"')
									 .count();
		return ((occurrences % 2) == 0);
	}

	public static int indexOfNextNonWhiteSpaceChar(String s, int currentIndex) {
		for (int i = currentIndex; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				continue;
			}
			return i;
		}
		return -1;
	}

}
