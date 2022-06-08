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
import org.slf4j.Logger;

public abstract class ChangesImpl implements Changes {

	public ChangesImpl() {
		// Empty
	}

	//

	@Override
	public abstract boolean isContentChanged();

	@Override
	public abstract String toString();

	//

	@Override
	public boolean isChanged() {
		return isRenamed() || isContentChanged();
	}

	@Override
	public boolean isRenamed() {
		String inputResourceName = getInputResourceName();
		// The input name will be null if the transform fails very early.
		return ((inputResourceName != null) && !inputResourceName.equals(getOutputResourceName()));
	}

	@Override
	public String getChangeText() {
		boolean nameChanged = isRenamed();
		boolean contentChanged = isContentChanged();

		if (nameChanged && contentChanged) {
			return "Name and content changes";
		} else if (nameChanged) {
			return "Name changes";
		} else if (contentChanged) {
			return "Content changes";
		} else {
			return "No changes";
		}
	}

	//

	private String	inputResourceName;
	private String	outputResourceName;

	@Override
	public String getInputResourceName() {
		return inputResourceName;
	}

	@Override
	public ChangesImpl setInputResourceName(String inputResourceName) {
		this.inputResourceName = inputResourceName;
		return this;
	}

	@Override
	public String getOutputResourceName() {
		return outputResourceName;
	}

	@Override
	public ChangesImpl setOutputResourceName(String outputResourceName) {
		this.outputResourceName = outputResourceName;
		return this;
	}

	//

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isInfoEnabled(consoleMarker)) {
			String useInputName = getInputResourceName();
			String useOutputName = getOutputResourceName();

			if (useInputName.equals(inputPath)) {
				logger.info(consoleMarker, "Input  [ {} ]", inputPath);
			} else {
				logger.info(consoleMarker, "Input  [ {} ] as [ {} ]", getInputResourceName(), inputPath);
			}
			if (useOutputName.equals(outputPath)) {
				logger.info(consoleMarker, "Output [ {} ]", outputPath);
			} else {
				logger.info(consoleMarker, "Output [ {} ] as [ {} ]", getOutputResourceName(), outputPath);
			}

			logChanges(logger);
		}
	}

	protected void logChanges(Logger logger) {
		logger.info(consoleMarker, "Changes [ {} ]", getChangeText());
	}
}
