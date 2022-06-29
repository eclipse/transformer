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

import java.io.File;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.RenameAction;

/**
 * Terminal action. Used currently by container actions to ensure that all
 * resources are renamed. <em>All</em> resources which are package specific
 * locations should be renamed using the package rename rules.
 */
public class RenameActionImpl extends ElementActionImpl implements RenameAction {

	public RenameActionImpl(ActionContext context) {
		super(context);
	}

	//

	@Override
	public ActionType getActionType() {
		return ActionType.RENAME;
	}

	//

	@Override
	public boolean acceptResource(String resourcePath, File resourceFile) {
		return true;
	}

	//

	@Override
	public void apply(String inputName, File inputFile, String outputName, File outputFile) throws TransformException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Special to {@code RenameAction}: Rename the action. Let the caller
	 * handle transferring the content.
	 *
	 * @param inputName The name of the input resource.
	 * @return The output name.
	 */
	@Override
	public String apply(String inputName) {
		startRecording(inputName);
		try {
			String outputName = relocateResource(inputName);
			setResourceNames(inputName, outputName);
			return outputName;
		} finally {
			stopRecording(inputName);
		}
	}

	@Override
	public ByteData apply(ByteData inputData) {
		String outputName = apply(inputData.name());
		return inputData.copy(outputName);
	}

}
