/** ******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ******************************************************************************* */

package org.eclipse.transformer.action.impl;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;

/**
 * @author jdenise
 */
public class PropertiesActionImpl extends ActionImpl {

	public PropertiesActionImpl(Logger logger, boolean isTerse, boolean isVerbose, InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
	}

	@Override
	public String getAcceptExtension() {
		return ".properties";
	}

	@Override
	protected ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws TransformException {

		String outputName = transformBinaryType(inputName);
		if (outputName != null) {
			verbose("Properties file %s, relocated to %s", inputName, outputName);
			setResourceNames(inputName, outputName);
			return new ByteData(outputName, inputBytes, 0, inputLength);
		} else {
			setResourceNames(inputName, inputName);
			return new ByteData(inputName, inputBytes, 0, inputLength);
		}
	}

	@Override
	public String getName() {
		return "Properties File Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.PROPERTIES;
	}

}
