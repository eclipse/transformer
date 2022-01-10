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

package org.eclipse.transformer;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Callback to supply transformation options to the Transformer.
 */
public interface TransformOptions {
	/**
	 * Returns whether the specified option is in effect.
	 *
	 * @param option The requested option.
	 * @return {@code true} if the requested options is in effect.
	 */
	default boolean hasOption(AppOption option) {
		String result = getOptionValue(option);
		return Objects.nonNull(result);
	}

	/**
	 * Returns the value of the specified option.
	 *
	 * @param option The requested option.
	 * @return The value of the specified option or {@code null} if the
	 *         requested option has no value.
	 */
	default String getOptionValue(AppOption option) {
		List<String> result = getOptionValues(option);
		return (Objects.nonNull(result) && !result.isEmpty()) ? result.get(0) : null;
	}

	/**
	 * Returns the values of the specified option.
	 *
	 * @param option The requested option.
	 * @return The values of the specified option or {@code null} if the
	 *         requested option has no values.
	 */
	default List<String> getOptionValues(AppOption option) {
		return null;
	}

	/**
	 * Returns a default value of the specified option.
	 *
	 * @param option The requested option.
	 * @return The default value of the specified option or {@code null} if the
	 *         requested option has no default value.
	 */
	default String getDefaultValue(AppOption option) {
		return null;
	}

	/**
	 * Returns a rule loader for loading rule property files.
	 *
	 * @return A function which can return a URL to read a requested property
	 *         file.
	 */
	default Function<String, URL> getRuleLoader() {
		return getClass()::getResource;
	}

	/**
	 * Returns the input file for the transformation.
	 *
	 * @return The input file for the transformation.
	 */
	default String getInputFileName() {
		throw new UnsupportedOperationException("method not implemented");
	}

	/**
	 * Returns the output file for the transformation.
	 *
	 * @return The output file for the transformation.
	 */
	default String getOutputFileName() {
		throw new UnsupportedOperationException("method not implemented");
	}

	/**
	 * Normalize the specified value.
	 *
	 * @param value The value to normalize.
	 * @return The normalized value.
	 */
	default String normalize(String value) {
		return Objects.nonNull(value) ? value.replace(File.separatorChar, '/') : null;
	}

	/**
	 * Normalize the specified values.
	 *
	 * @param values The values to normalize.
	 * @return The normalized values.
	 */
	default List<String> normalize(List<String> values) {
		return Objects.nonNull(values) ? values.stream()
			.map(this::normalize)
			.collect(toList()) : null;
	}
}
