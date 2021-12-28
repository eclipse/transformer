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

public class ClassChangesImpl extends ChangesImpl {
	@Override
	public void clearChanges() {
		inputClassName = null;
		outputClassName = null;

		inputSuperName = null;
		outputSuperName = null;

		modifiedInterfaces = 0;

		modifiedFields = 0;
		modifiedMethods = 0;
		modifiedAttributes = 0;

		modifiedConstants = 0;
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return (((inputClassName != null) && (outputClassName != null) && !inputClassName.equals(outputClassName))
			|| ((inputSuperName != null) && (outputSuperName != null) && !inputSuperName.equals(outputSuperName)) ||

			(modifiedInterfaces > 0) ||

			(modifiedFields > 0) || (modifiedMethods > 0) || (modifiedAttributes > 0) ||

			(modifiedConstants > 0));
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

	public void setModifiedInterfaces(int modifiedInterfaces) {
		this.modifiedInterfaces = modifiedInterfaces;
	}

	public void addModifiedInterface() {
		modifiedInterfaces++;

	}

	//

	private int	modifiedFields;
	private int	modifiedMethods;
	private int	modifiedAttributes;

	public int getModifiedFields() {
		return modifiedFields;
	}

	public void setModifiedFields(int modifiedFields) {
		this.modifiedFields = modifiedFields;
	}

	public void addModifiedField() {
		modifiedFields++;
	}

	public int getModifiedMethods() {
		return modifiedMethods;
	}

	public void setModifiedMethods(int modifiedMethods) {
		this.modifiedMethods = modifiedMethods;
	}

	public void addModifiedMethod() {
		modifiedMethods++;
	}

	public int getModifiedAttributes() {
		return modifiedAttributes;
	}

	public void setModifiedAttributes(int modifiedAttributes) {
		this.modifiedAttributes = modifiedAttributes;
	}

	public void addModifiedAttribute() {
		modifiedAttributes++;
	}

	//

	private int modifiedConstants;

	public int getModifiedConstants() {
		return modifiedConstants;
	}

	public void setModifiedConstants(int modifiedConstants) {
		this.modifiedConstants = modifiedConstants;
	}

	public void addModifiedConstant() {
		modifiedConstants++;
	}

	//

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isDebugEnabled(consoleMarker)) {
			logger.debug(consoleMarker, "Input name [ {} ] as [ {} ]", getInputResourceName(), inputPath);

			logger.debug(consoleMarker, "Output name [ {} ] as [ {} ]", getOutputResourceName(), outputPath);

			logger.debug(consoleMarker, "Class name [ {} ] [ {} ]", getInputClassName(), getOutputClassName());

			String useInputSuperName = getInputSuperName();
			if (useInputSuperName != null) {
				logger.debug(consoleMarker, "Super class name [ {} ] [ {} ]", useInputSuperName, getOutputSuperName());
			}

			logger.debug(consoleMarker, "Modified interfaces [ {} ]", getModifiedInterfaces());
			logger.debug(consoleMarker, "Modified fields     [ {} ]", getModifiedFields());
			logger.debug(consoleMarker, "Modified methods    [ {} ]", getModifiedMethods());
			logger.debug(consoleMarker, "Modified constants  [ {} ]", getModifiedConstants());
		} else {
			super.log(logger, inputPath, outputPath);
		}
	}
}
