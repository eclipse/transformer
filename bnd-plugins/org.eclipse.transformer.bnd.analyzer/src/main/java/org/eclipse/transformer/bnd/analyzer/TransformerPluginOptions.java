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

package org.eclipse.transformer.bnd.analyzer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;
import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;

public class TransformerPluginOptions implements TransformOptions {
	private final Parameters parameters;
	private final Map<String, String>	optionDefaults;
	private final Function<String, URL>	ruleLoader;

	public TransformerPluginOptions(Processor processor, Parameters parameters, Map<String, String> optionDefaults,
		Function<String, URL> ruleLoader) {
		this.parameters = requireNonNull(parameters);
		this.optionDefaults = requireNonNull(optionDefaults);
		this.ruleLoader = requireNonNull(ruleLoader);
		AppOption[] values = AppOption.values();
		parameters.forEach((key, args) -> {
			String tag = Processor.removeDuplicateMarker(key);
			Optional<AppOption> matching = Arrays.stream(values)
				.filter(option -> {
					String longTag = option.getLongTag();
					String shortTag = option.getShortTag();
					return Objects.equals(longTag, tag) || Objects.equals(shortTag, tag);
				})
				.findFirst();
			if (matching.isPresent()) {
				AppOption appOption = matching.get();
				if (appOption.getHasArg() || appOption.getHasArgs()) {
					if (args.isEmpty()) {
						processor.warning("The transformer option %s requires arguments", tag);
					}
				} else if (appOption.getHasArgCount()) {
					if (appOption.getArgCount() != args.size()) {
						processor.warning("The transformer option %s requires %d arguments", tag,
							appOption.getArgCount());
					}
				}
			} else {
				processor.error("The transformer option %s is unrecognized", tag);
			}
		});
	}

	private List<String> keys(AppOption option) {
		String longTag = option.getLongTag();
		String shortTag = option.getShortTag();
		List<String> keys = parameters.keySet()
			.stream()
			.filter(key -> {
				String tag = Processor.removeDuplicateMarker(key);
				return Objects.equals(longTag, tag) || Objects.equals(shortTag, tag);
			})
			.collect(toList());
		return keys;
	}

	@Override
	public List<String> getOptionValues(AppOption option) {
		List<String> keys = keys(option);
		if (keys.isEmpty()) {
			return null;
		}
		List<String> values = keys.stream()
			.map(parameters::get)
			.flatMap(attrs -> attrs.values()
				.stream())
			.flatMap(Strings::splitQuotedAsStream)
			.collect(toList());
		return values;
	}

	@Override
	public String getOptionValue(AppOption option) {
		List<String> keys = keys(option);
		if (keys.isEmpty()) {
			return null;
		}
		String value = keys.stream()
			.map(parameters::get)
			.flatMap(attrs -> attrs.values()
				.stream())
			.flatMap(Strings::splitQuotedAsStream)
			.findFirst()
			.orElse(null);
		return value;
	}

	@Override
	public boolean hasOption(AppOption option) {
		List<String> keys = keys(option);
		return !keys.isEmpty();
	}

	@Override
	public String getDefaultValue(AppOption option) {
		String longTag = option.getLongTag();
		String defaultValue = optionDefaults.get(longTag);
		if (defaultValue == null) {
			String shortTag = option.getShortTag();
			defaultValue = optionDefaults.get(shortTag);
		}
		return defaultValue;
	}

	@Override
	public Function<String, URL> getRuleLoader() {
		return ruleLoader;
	}
}
