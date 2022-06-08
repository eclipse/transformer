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

public class ClassChangesImpl extends ElementChangesImpl {
	public ClassChangesImpl() {
		super();
	}

	@Override
	public boolean isContentChanged() {
		String inputClassName = getInputClassName();
		String outputClassName = getOutputClassName();
		String inputSuperName = getInputSuperName();
		String outputSuperName = getOutputSuperName();

		// Other than the input class name and output class name,
		// change tracking updates 'replacements', which is managed
		// by the superclass, and which is tested by 'isContentChanged'.
		//
		// As a consequence, ** ALL OF THE CHANGE RECORDING ** must invoke
		// 'addReplacement'. Otherwise, not all content changes will be
		// detected.

		return super.isContentChanged() ||
			((inputClassName != null) && (outputClassName != null) && !inputClassName.equals(outputClassName)) ||
			((inputSuperName != null) && (outputSuperName != null) && !inputSuperName.equals(outputSuperName));
	}

	//

	private String	inputClassName;
	private String	outputClassName;

	public String getInputClassName() {
		return inputClassName;
	}

	public void setInputClassName(String inputClassName) {
		this.inputClassName = inputClassName;
	}

	public String getOutputClassName() {
		return outputClassName;
	}

	public void setOutputClassName(String outputClassName) {
		this.outputClassName = outputClassName;
	}

	//

	private String	inputSuperName;
	private String	outputSuperName;

	public String getInputSuperName() {
		return inputSuperName;
	}

	public void setInputSuperName(String inputSuperName) {
		this.inputSuperName = inputSuperName;
	}

	public String getOutputSuperName() {
		return outputSuperName;
	}

	public void setOutputSuperName(String outputSuperName) {
		this.outputSuperName = outputSuperName;
	}

	private int modifiedInterfaces;

	public int getModifiedInterfaces() {
		return modifiedInterfaces;
	}

	public void addModifiedInterface() {
		modifiedInterfaces++;
		addReplacement();
	}

	//

	private int	modifiedFields;
	private int	modifiedMethods;
	private int	modifiedAttributes;

	public int getModifiedFields() {
		return modifiedFields;
	}

	public void addModifiedField() {
		modifiedFields++;
		addReplacement();
	}

	public int getModifiedMethods() {
		return modifiedMethods;
	}

	public void addModifiedMethod() {
		modifiedMethods++;
		addReplacement();
	}

	public int getModifiedAttributes() {
		return modifiedAttributes;
	}

	public void addModifiedAttribute() {
		modifiedAttributes++;
		addReplacement();
	}

	//

	private int modifiedConstants;

	public int getModifiedConstants() {
		return modifiedConstants;
	}

	public void addModifiedConstant() {
		modifiedConstants++;
		addReplacement();
	}

	public void addModifiedConstants(int additions) {
		modifiedConstants += additions;
		addReplacements(additions);
	}

	//

	@Override
	public void logChanges(Logger logger) {
		super.logChanges(logger);

		logger.info(consoleMarker, "Class name [ {} ] [ {} ]", getInputClassName(), getOutputClassName());

		String inputSuperName = getInputSuperName();
		if (inputSuperName != null) {
			logger.debug(consoleMarker, "Super class name [ {} ] [ {} ]", inputSuperName, getOutputSuperName());
		}

		logger.debug(consoleMarker, "Modified interfaces [ {} ]", getModifiedInterfaces());
		logger.debug(consoleMarker, "Modified fields     [ {} ]", getModifiedFields());
		logger.debug(consoleMarker, "Modified methods    [ {} ]", getModifiedMethods());
		logger.debug(consoleMarker, "Modified constants  [ {} ]", getModifiedConstants());
	}
}
