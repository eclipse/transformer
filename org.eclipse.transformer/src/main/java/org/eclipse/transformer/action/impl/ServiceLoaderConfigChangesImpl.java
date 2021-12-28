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

import org.slf4j.Logger;

public class ServiceLoaderConfigChangesImpl extends ChangesImpl {

	public ServiceLoaderConfigChangesImpl() {
		super();

		this.clearChanges();
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return (changedProviders > 0);
	}

	@Override
	public void clearChanges() {
		changedProviders = 0;
		unchangedProviders = 0;

		super.clearChanges();
	}

	//

	private int	changedProviders;
	private int	unchangedProviders;

	public void addChangedProvider() {
		changedProviders++;
	}

	public int getChangedProviders() {
		return changedProviders;
	}

	public void addUnchangedProvider() {
		unchangedProviders++;
	}

	public int getUnchangedProviders() {
		return unchangedProviders;
	}

	//

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isDebugEnabled(consoleMarker)) {
			logger.debug(consoleMarker, "Input  [ {} ] as [ {} ]", getInputResourceName(), inputPath);
			logger.debug(consoleMarker, "Output [ {} ] as [ {} ]", getOutputResourceName(), outputPath);
			logger.debug(consoleMarker, "Replacements [ {} ]", getChangedProviders());
		} else {
			super.log(logger, inputPath, outputPath);
		}
	}
}
