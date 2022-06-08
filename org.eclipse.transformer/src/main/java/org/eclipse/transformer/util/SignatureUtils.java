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

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for signature processing.
 */
public final class SignatureUtils {
	private SignatureUtils() {}

	public static String putSlashes(String className) {
		return className.replace('.', '/');
	}

	public static String putDots(String className) {
		return className.replace('/', '.');
	}

	/**
	 * This comparator sorts non-wildcard keys before wildcard keys and sorts
	 * longer segment counts before shorter segment counts. So the most precise
	 * package names are encountered before less precise package names.
	 */
	public static class RenameKeyComparator implements Comparator<String> {
		private final char separator;

		public RenameKeyComparator(char separator) {
			this.separator = separator;
		}

		@Override
		public int compare(String first, String second) {
			boolean wildFirst = containsWildcard(first);
			boolean wildSecond = containsWildcard(second);
			if (wildFirst != wildSecond) {
				return wildSecond ? -1 : 1;
			}
			int compare = segments(second, separator) - segments(first, separator);
			if (compare != 0) {
				return compare;
			}
			return first.compareTo(second);
		}
	}

	/**
	 * Return the number of segments in the key as separated by the separator
	 * character.
	 *
	 * @param key Key
	 * @param separator Separator character
	 * @return The number of segments in the key.
	 */
	public static int segments(String key, char separator) {
		int segments = 1;
		for (int index = 0; (index = key.indexOf(separator, index + 1)) >= 0;) {
			segments++;
		}
		return segments;
	}

	/**
	 * Checks the character before and after a match to verify that the match is
	 * not a subset of a larger package, and thus not really a match.
	 *
	 * @param text The text to examine for a match.
	 * @param matchStart Where the match starts in the text.
	 * @param matchEnd Where the match ends in the text.
	 * @param matchPackageStem Control parameter: Are package stem matches
	 *            allowed.
	 * @return End position of the complete package match or -1 if not a package
	 *         match.
	 */
	public static int packageMatch(String text, int matchStart, int matchEnd, boolean matchPackageStem) {
		if (matchStart > 0) {
			char charBeforeMatch = text.charAt(matchStart - 1);
			if ((charBeforeMatch == '.') || (charBeforeMatch == '/')
				|| Character.isJavaIdentifierPart(charBeforeMatch)) {
				return -1; // not a package name
			}
		}

		final int textLength = text.length();
		if (textLength > matchEnd) {
			char charAfterMatch = text.charAt(matchEnd);

			// Check the next character can also be part of a package name then
			// we are looking at a larger package name, and thus not a match.
			if (Character.isJavaIdentifierPart(charAfterMatch)) {
				return -1;
			}

			/*
			 * If the next char is dot or slash, check the character after the
			 * dot/slash. Assume an upper case letter indicates the start of a
			 * class name and thus the end of the package name. ( This doesn't
			 * work for package names that do not follow the convention of using
			 * lower case characters ). If not upper case, then it indicates we
			 * are looking at a larger package name.
			 */
			if ((charAfterMatch == '.') || (charAfterMatch == '/')) {
				if (matchPackageStem) {
					/*
					 * Find the end of the package matching the package stem.
					 */
					int backoff = 1;
					while (textLength > ++matchEnd) {
						charAfterMatch = text.charAt(matchEnd);
						if ((charAfterMatch == '.') || (charAfterMatch == '/')) {
							backoff = 1;
						} else if (Character.isJavaIdentifierPart(charAfterMatch)
							&& !Character.isUpperCase(charAfterMatch)) {
							backoff = 0;
						} else {
							break;
						}
					}
					matchEnd -= backoff;
				} else {
					/*
					 * Confirm any next char is not part of a package name.
					 */
					if (textLength > (matchEnd + 1)) {
						charAfterMatch = text.charAt(matchEnd + 1);
						if (Character.isJavaIdentifierPart(charAfterMatch) && !Character.isUpperCase(charAfterMatch)) {
							return -1;
						}
					}
				}
			}
		}
		return matchEnd;
	}

	/**
	 * Tell if a key contains a wildcard suffix, which indicates that
	 * sub-package names are to be matched.
	 * <p>
	 * Packages names and their replacements are specified in properties files
	 * in <code>key=value</code> pairs with the key specifying a a package name
	 * and the value specifying the replacement for that package name.
	 * <p>
	 * The package name which is to be replaced can contain a wildcard suffix,
	 * either ".*" or "/*". If a wildcard suffix is absent, no sub-packages are
	 * matched. A wildcard must be specified to enable sub-packages to be
	 * matched.
	 *
	 * @param key A package name which is to be tested.
	 * @return True or false telling if the package name contains a wildcard
	 *         suffix.
	 */
	public static boolean containsWildcard(String key) {
		int last = key.length() - 1;
		if (last > 1) {
			if (key.charAt(last) == '*') {
				char separator = key.charAt(last - 1);
				// Testing against either wildcard is slightly improper.
				// However, accidental matches should never happen, since
				// a '.' is not valid in slash cases and a '/' is not valid
				// dot cases.
				return (separator == '.') || (separator == '/');
			}
		}
		return false;
	}

	/**
	 * Removes the last two characters. Must only be called when the string is
	 * known to contain a wildcard suffix, either ".*" or "/*".
	 *
	 * @param key A package name which contains a wildcard suffix.
	 * @return The key with the last two characters removed.
	 */
	public static String stripWildcard(String key) {
		return key.substring(0, key.length() - 2);
	}

	/**
	 * Return a stream of keys including wildcard variations.
	 *
	 * @param key The initial key. It is always the first key in the stream.
	 * @param wildcard The wildcard suffix. Either {@code .*} or {@code /*}.
	 * @return A stream of keys to lookup in the rename map.
	 */
	public static Stream<String> keyStream(String key, String wildcard) {
		Spliterator<String> spliterator = keySpliterator(key, wildcard);
		return StreamSupport.stream(spliterator, false);
	}

	/**
	 * Return a spliterator of keys including wildcard variations.
	 *
	 * @param key The initial key. It is always the first key in the stream.
	 * @param wildcard The wildcard suffix. Either {@code .*} or {@code /*}.
	 * @return A spliterator of keys to lookup in the rename map.
	 */
	public static Spliterator<String> keySpliterator(String key, String wildcard) {
		return new KeySpliterator(key, wildcard);
	}

	static class KeySpliterator extends AbstractSpliterator<String> {
		private String			key;
		private final String	wildcard;

		KeySpliterator(String key, String wildcard) {
			super(segments(key, wildcard.charAt(0)) + 1,
				Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL);
			this.key = key;
			this.wildcard = wildcard;
		}

		@Override
		public boolean tryAdvance(Consumer<? super String> action) {
			if (!key.isEmpty()) {
				action.accept(key);
				advance();
				return true;
			}
			return false;
		}

		@Override
		public void forEachRemaining(Consumer<? super String> action) {
			while (!key.isEmpty()) {
				action.accept(key);
				advance();
			}
		}

		private void advance() {
			if (containsWildcard(key)) {
				int lastIndex = key.lastIndexOf(wildcard.charAt(0), key.length() - 3);
				if (lastIndex <= 0) {
					key = "";
				} else {
					key = key.substring(0, lastIndex)
						.concat(wildcard);
				}
			} else {
				key = key.concat(wildcard);
			}
		}
	}

	private static final String	CLASS_EXTENSION			= ".class";
	private static final int	CLASS_EXTENSION_LENGTH	= CLASS_EXTENSION.length();

	public static String resourceNameToClassName(String resourceName) {
		String className = resourceName.endsWith(CLASS_EXTENSION)
			? resourceName.substring(resourceName.length() - CLASS_EXTENSION_LENGTH)
			: resourceName;
		className = putDots(className);
		return className;
	}

	public static String classNameToResourceName(String className) {
		String resourceName = putSlashes(className).concat(CLASS_EXTENSION);
		return resourceName;
	}
}
