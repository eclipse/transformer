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
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.ElementAction;

public abstract class ElementActionImpl extends ActionImpl implements ElementAction {

	public ElementActionImpl(ActionContext context) {
		super(context);
	}

	@Override
	public boolean isElementAction() {
		return true;
	}

	//

	@Override
	public ElementChangesImpl newChanges() {
		return new ElementChangesImpl();
	}

	@Override
	public ElementChangesImpl getActiveChanges() {
		return (ElementChangesImpl) super.getActiveChanges();
	}

	@Override
	public ElementChangesImpl getLastActiveChanges() {
		return (ElementChangesImpl) super.getLastActiveChanges();
	}

	public void addReplacement() {
		getActiveChanges().addReplacement();
	}

	public void addReplacements(int additions) {
		getActiveChanges().addReplacements(additions);
	}

	//

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return acceptExtension(resourceName, resourceFile);
	}

	// Entry from the transformer, and from the directory action.

	@Override
	public void apply(String inputName, File inputFile, String outputName, File outputFile) throws TransformException {
		ByteData inputData = collect(inputName, inputFile);

		ByteData outputData;
		TransformException transformError;
		try {
			outputData = apply(inputData);
			transformError = null;
		} catch (TransformException t) {
			outputData = inputData; // Fallback: copy.
			transformError = t;
		}

		write(outputData, outputFile);

		if (transformError != null) {
			throw transformError;
		}
	}

	/**
	 * Apply this action to a file. Write the transformed content into the
	 * specified output folder. Use the input name, unless the input is renamed
	 * by the action.
	 * <p>
	 * This method provides the main transition
	 *
	 * @param inputName The name of the input file.
	 * @param inputFile The input file.
	 * @param outputRoot The folder into which to write the output.
	 * @throws TransformException Thrown if the transform failed.
	 */
	protected void apply(String inputName, File inputFile, File outputRoot) throws TransformException {
		ByteData inputData = collect(inputName, inputFile);

		ByteData outputData;
		TransformException transformError;
		try {
			outputData = apply(inputData);
			transformError = null;
		} catch (TransformException t) {
			outputData = inputData; // Fallback: copy.
			transformError = t;
		}

		File outputFile = new File(outputRoot, outputData.name());
		write(outputData, outputFile);

		if (transformError != null) {
			throw transformError;
		}
	}

	// Main implementation point: Each element action is expected to implement.
	@Override
	public abstract ByteData apply(ByteData inputData) throws TransformException;
}
