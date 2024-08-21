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

package org.eclipse.transformer.maven;

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.codehaus.plexus.util.StringUtils;
import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.jakarta.JakartaTransform;
import org.eclipse.transformer.maven.configuration.TransformerRules;

/**
 *
 */
public class TransformerMojoOptions implements TransformOptions {
	private final TransformerRules		rules;
	private final Map<String, String>	optionDefaults;
	private final Function<String, URL>	ruleLoader;

	public TransformerMojoOptions(TransformerRules rules) {
		this.rules = requireNonNull(rules);
		if (rules.isJakartaDefaults()) {
			this.optionDefaults = JakartaTransform.getOptionDefaults();
			this.ruleLoader = JakartaTransform.getRuleLoader();
		} else {
			this.optionDefaults = Collections.emptyMap();
			this.ruleLoader = getClass()::getResource;
		}
	}

	private static List<String> condition(List<String> values) {
		if (values == null) {
			return null;
		}
		for (ListIterator<String> iterator = values.listIterator(); iterator.hasNext();) {
			String value = StringUtils.trim(iterator.next());
			if (StringUtils.isBlank(value) || Objects.equals("-", value)) {
				iterator.remove();
			} else {
				iterator.set(value);
			}
		}
		return values;
	}

	@Override
	public List<String> getOptionValues(AppOption option) {
		List<String> values = switch (option) {
			case RULES_BUNDLES -> rules.getBundles();
			case RULES_DIRECT -> rules.getDirects();
			case RULES_IMMEDIATE_DATA -> rules.getImmediates();
			case RULES_MASTER_TEXT -> rules.getTexts();
			case RULES_PER_CLASS_CONSTANT -> rules.getPerClassConstants();
			case RULES_RENAMES -> rules.getRenames();
			case RULES_SELECTIONS -> rules.getSelections();
			case RULES_VERSIONS -> rules.getVersions();
			default -> null;
		};
		return condition(values);
	}

	@Override
	public boolean hasOption(AppOption option) {
		boolean has = switch (option) {
			case OVERWRITE -> rules.isOverwrite();
			case INVERT -> rules.isInvert();
			case WIDEN_ARCHIVE_NESTING -> rules.isWiden();
			case STRIP_SIGNATURES -> rules.isStripSignatures();
			default -> TransformOptions.super.hasOption(option);
		};
		return has;
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
