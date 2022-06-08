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

import org.eclipse.transformer.action.ElementChanges;
import org.slf4j.Logger;

public class ElementChangesImpl extends ChangesImpl implements ElementChanges {

	public ElementChangesImpl() {
		super();
	}

	//

	/**
	 * Override: Element changes provide a basic mechanism for change recording,
	 * which is to track the number of replacements which were made.
	 * <p>
	 * Subclasses are expected to extend this implementation when other types of
	 * changes must also be tracked.
	 * <p>
	 * If this implementation is used, ** ALL OF THE CHANGE RECORDING ** must
	 * invoke 'addReplacement'. Otherwise, not all content changes will be
	 * detected.
	 *
	 * @return True or false, telling if any content changes were made.
	 */
	@Override
	public boolean isContentChanged() {
		return (getReplacements() != 0);
	}

	@Override
	public String toString() {
		return String.format("%s [%s]: [%d]", getInputResourceName(), getChangeText(), getReplacements());
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

	//

	@Override
	public void logChanges(Logger logger) {
		logger.info(consoleMarker, "[ {} ]: [ {} ]", getChangeText(), getReplacements());
	}
}
