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

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionSelector;
import org.eclipse.transformer.action.ContainerChanges;
import org.eclipse.transformer.bnd.analyzer.action.AnalyzerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * Base Bnd Plugin for Transformer.
 */
public class BaseTransformerPlugin implements Plugin {
	private final Logger		logger		= LoggerFactory.getLogger(getClass());
	public static final String	TRANSFORMER	= "-transformer";
	private String				command		= TRANSFORMER;
	private int					ordering;

	public BaseTransformerPlugin(int ordering) {
		this.ordering = ordering;
	}

	protected Map<String, String> getOptionDefaults() {
		return Collections.emptyMap();
	}

	protected Function<String, URL> getRuleLoader() {
		return getClass()::getResource;
	}

	public Logger getLogger() {
		return logger;
	}

	public boolean transform(Analyzer analyzer) throws Exception {
		Parameters parameters = analyzer.decorated(command, true);
		if (parameters.isEmpty()) {
			return false;
		}

		TransformOptions options = new TransformerPluginOptions(analyzer, parameters, getOptionDefaults(),
			getRuleLoader());
		Transformer transformer = new Transformer(getLogger(), options);
		// For use as the resolve base
		URI base = analyzer.getBaseURI();
		if (base != null) {
			getLogger().debug("Setting Transformer base {}", base);
			transformer.setBase(base);
		}

		boolean validRules;
		try {
			validRules = transformer.setRules(transformer.getImmediateData());
		} catch (Exception e) {
			analyzer.exception(e, "Exception loading transformer rules");
			return false;
		}
		if (!validRules) {
			analyzer.error("Transformation rules are not valid and cannot be used");
			return false;
		}
		transformer.logRules();

		// TODO: Still figuring out the best pattern for action and transformer
		// construction.
		//
		// See issue #296

		ActionSelector actionSelector = transformer.getActionSelector();
		ActionContext context = transformer.getActionContext();
		boolean overwrite = options.hasOption(AppOption.OVERWRITE);
		AnalyzerAction analyzerAction = new AnalyzerAction(context, actionSelector, overwrite);

		analyzerAction.apply(analyzer);

		ContainerChanges lastActiveChanges = analyzerAction.getLastActiveChanges();
		lastActiveChanges.log(getLogger(), lastActiveChanges.getInputResourceName(),
			lastActiveChanges.getOutputResourceName());

		return lastActiveChanges.isChanged();
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	/**
	 * We want to run relative to other plugins.
	 *
	 * @return Ordering value
	 */
	public int ordering() {
		return ordering;
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		command = map.getOrDefault(Constants.COMMAND_DIRECTIVE, TRANSFORMER);
		if (map.containsKey("ordering")) {
			try {
				ordering = Integer.parseInt(map.get("ordering"));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	@Override
	public void setReporter(Reporter processor) {
		// ignore;
	}
}
