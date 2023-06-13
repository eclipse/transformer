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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to parse OSGI Bundle headers. This has been stolen from the Apache Felix project
 * See https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html for the format of the headers
 */
public final class HeaderParser {
	private HeaderParser() {
		// utility class
	}

	public static Clause[] parseHeader(final String header) throws IllegalArgumentException {
		Clause[] clauses = null;
		if (header != null) {
			if (header.length() == 0) {
				throw new IllegalArgumentException("The header cannot be an empty string.");
			}
			final String[] segments = parseDelimitedString(header, ",");
			clauses = parseClauses(segments);
		}
		return (clauses == null) ? new Clause[0] : clauses;
	}

	public static Clause[] parseClauses(final String[] segments) throws IllegalArgumentException {
		if (segments == null) {
			return null;
		}

		final List completeList = new ArrayList();
		for (final String segement : segments) {
			// Break string into semi-colon delimited pieces.
			String[] pieces = parseDelimitedString(segement, ";");

			// Count the number of different clauses; clauses
			// will not have an '=' in their string. This assumes
			// that clauses come first, before directives and
			// attributes.
			int pathCount = 0;
			for (final String piece : pieces) {
				if (piece.indexOf('=') >= 0) {
					break;
				}
				pathCount++;
			}

			// Error if no packages were specified.
			if (pathCount == 0) {
				throw new IllegalArgumentException("No path specified on clause: " + segement);
			}

			// Parse the directives/attributes.
			Directive[] dirs = new Directive[pieces.length - pathCount];
			Attribute[] attrs = new Attribute[pieces.length - pathCount];
			int dirCount = 0, attrCount = 0;
			int idx = -1;
			String sep = null;
			for (int pieceIdx = pathCount; pieceIdx < pieces.length; pieceIdx++) {
				if ((idx = pieces[pieceIdx].indexOf("=")) <= 0) {
					// It is an error.
					throw new IllegalArgumentException("Not a directive/attribute: " + segement);
				}
				// This a directive.
				if (pieces[pieceIdx].charAt(idx - 1) == ':') {
					idx--;
					sep = ":=";
				}
				// This an attribute.
				else {
					sep = "=";
				}

				String key = pieces[pieceIdx].substring(0, idx).trim();
				String value = pieces[pieceIdx].substring(idx + sep.length()).trim();

				// Remove quotes, if value is quoted.
				if (value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}

				// Save the directive/attribute in the appropriate array.
				if (sep.equals(":=")) {
					dirs[dirCount++] = new Directive(key, value);
				} else {
					attrs[attrCount++] = new Attribute(key, value);
				}
			}

			// Shrink directive array.
			Directive[] dirsFinal = new Directive[dirCount];
			System.arraycopy(dirs, 0, dirsFinal, 0, dirCount);
			// Shrink attribute array.
			Attribute[] attrsFinal = new Attribute[attrCount];
			System.arraycopy(attrs, 0, attrsFinal, 0, attrCount);

			// Create package attributes for each package and
			// set directives/attributes. Add each package to
			// completel list of packages.
			Clause[] pkgs = new Clause[pathCount];
			for (int pkgIdx = 0; pkgIdx < pathCount; pkgIdx++) {
				pkgs[pkgIdx] = new Clause(pieces[pkgIdx], dirsFinal, attrsFinal);
				completeList.add(pkgs[pkgIdx]);
			}
		}

		return (Clause[]) completeList.toArray(new Clause[completeList.size()]);
	}

	/**
	 * Parses delimited string and returns an array containing the tokens. This
	 * parser obeys quotes, so the delimiter character will be ignored if it is
	 * inside of a quote. This method assumes that the quote character is not
	 * included in the set of delimiter characters.
	 *
	 * @param value the delimited string to parse.
	 * @param delim the characters delimiting the tokens.
	 * @return an array of string tokens or null if there were no tokens.
	 **/
	public static String[] parseDelimitedString(String value, String delim) {
		if (value == null) {
			value = "";
		}

		final List<String> list = new ArrayList<>();

		int CHAR = 1;
		int DELIMITER = 2;
		int STARTQUOTE = 4;
		int ENDQUOTE = 8;

		final StringBuilder sb = new StringBuilder();

		int expecting = (CHAR | DELIMITER | STARTQUOTE);

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);

			final boolean isDelimiter = (delim.indexOf(c) >= 0);
			final boolean isQuote = (c == '"');

			if (isDelimiter && ((expecting & DELIMITER) > 0)) {
				list.add(sb.toString().trim());
				sb.delete(0, sb.length());
				expecting = (CHAR | DELIMITER | STARTQUOTE);
			} else if (isQuote && ((expecting & STARTQUOTE) > 0)) {
				sb.append(c);
				expecting = CHAR | ENDQUOTE;
			} else if (isQuote && ((expecting & ENDQUOTE) > 0)) {
				sb.append(c);
				expecting = (CHAR | STARTQUOTE | DELIMITER);
			} else if ((expecting & CHAR) > 0) {
				sb.append(c);
			} else {
				throw new IllegalArgumentException("Invalid delimited string: " + value);
			}
		}

		final String s = sb.toString().trim();
		if (s.length() > 0) {
			list.add(s);
		}

		return list.toArray(new String[0]);
	}

	public static class Clause {

		private final String name;
		private final Directive[] directives;
		private final Attribute[] attributes;

		public Clause(String name, Directive[] directives, Attribute[] attributes) {
			this.name = name;
			this.directives = directives;
			this.attributes = attributes;
		}

		public String getName() {
			return name;
		}

		public Directive[] getDirectives() {
			return directives;
		}

		public Attribute[] getAttributes() {
			return attributes;
		}

		public String getDirective(String name) {
			for (final Directive directive : directives) {
				if (name.equals(directive.getName())) {
					return directive.getValue();
				}
			}
			return null;
		}

		public String getAttribute(String name) {
			for (final Attribute attribute : attributes) {
				if (name.equals(attribute.getName())) {
					return attribute.getValue();
				}
			}
			return null;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(name);
			for (int i = 0; directives != null && i < directives.length; i++) {
				sb.append(";").append(directives[i].getName()).append(":=");
				if (directives[i].getValue().contains(",")) {
					sb.append("\"").append(directives[i].getValue()).append("\"");
				} else {
					sb.append(directives[i].getValue());
				}
			}
			for (int i = 0; attributes != null && i < attributes.length; i++) {
				sb.append(";").append(attributes[i].getName()).append("=");
				if (attributes[i].getValue().contains(",")) {
					sb.append("\"").append(attributes[i].getValue()).append("\"");
				} else {
					sb.append(attributes[i].getValue());
				}
			}
			return sb.toString();
		}
	}

	public static class Attribute {

		private final String name;
		private final String value;

		public Attribute(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

	}

	public static class Directive {

		private final String name;
		private final String value;

		public Directive(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

	}
}
