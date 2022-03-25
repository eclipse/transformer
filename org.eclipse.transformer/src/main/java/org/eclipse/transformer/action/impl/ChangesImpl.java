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

package org.eclipse.transformer.action.impl;

import static org.eclipse.transformer.Transformer.consoleMarker;

import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.ContainerChanges;
import org.slf4j.Logger;

public class ChangesImpl implements Changes {

	public ChangesImpl() {
		// Empty
	}

	@Override
	public boolean hasChanges() {
		return hasResourceNameChange() || hasNonResourceNameChanges();
	}

	@Override
	public void clearChanges() {
		inputResourceName = null;
		outputResourceName = null;

		replacements = 0;
	}

	//

	private String	inputResourceName;
	private String	outputResourceName;

	@Override
	public String getInputResourceName() {
		return inputResourceName;
	}

	@Override
	public void setInputResourceName(String inputResourceName) {
		this.inputResourceName = inputResourceName;
	}

	@Override
	public String getOutputResourceName() {
		return outputResourceName;
	}

	@Override
	public void setOutputResourceName(String outputResourceName) {
		this.outputResourceName = outputResourceName;
	}

	@Override
	public boolean hasResourceNameChange() {
		String inputResourceName = getInputResourceName();
		// The input name will be null if the transform fails very early.
		return ((inputResourceName != null) && !inputResourceName.equals(getOutputResourceName()));
	}

	//

	private int replacements;

	@Override
	public int getReplacements() {
		return replacements;
	}

	@Override
	public void addReplacement() {
		replacements++;
	}

	@Override
	public void addReplacements(int additions) {
		replacements += additions;
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return getReplacements() > 0;
	}

	//

	@Override
	public void addNestedInto(ContainerChanges containerChanges) {
		// By default do nothing.
	}

	//

	protected String getChangeTag() {
		return hasNonResourceNameChanges() ? "Changed" : "Unchanged";
	}

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isDebugEnabled(consoleMarker)) {
			logger.debug(consoleMarker, "Input  [ {} ] as [ {} ]", getInputResourceName(), inputPath);
			logger.debug(consoleMarker, "Output [ {} ] as [ {} ]", getOutputResourceName(), outputPath);
			logger.debug(consoleMarker, "Replacements  [ {} ]", getReplacements());
		} else if (logger.isInfoEnabled(consoleMarker)) {
			if (!inputPath.equals(outputPath)) {
				logger.info(consoleMarker, "Input [ {} ] as [ {} ]: {}", inputPath, outputPath, getChangeTag());
			}
		}
	}
}
