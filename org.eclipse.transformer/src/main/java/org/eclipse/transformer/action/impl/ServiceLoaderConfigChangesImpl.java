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

public class ServiceLoaderConfigChangesImpl extends ElementChangesImpl {

	public ServiceLoaderConfigChangesImpl() {
		super();
	}

	//

	@Override
	public String toString() {
		return String.format("%s [%s]: [%d] [%d]", getInputResourceName(), getChangeText(), getReplacements(),
			getChangedProviders());
	}

	//

	private int	changedProviders;
	private int	unchangedProviders;

	public void addChangedProvider() {
		changedProviders++;
		addReplacement();
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
	public void logChanges(Logger logger) {
		logger.info(consoleMarker, "[ {} ]: [ {} ] [ {} ]", getChangeText(), getReplacements(), getChangedProviders());
	}
}
