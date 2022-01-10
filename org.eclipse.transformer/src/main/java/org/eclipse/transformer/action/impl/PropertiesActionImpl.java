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
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

/**
 * @author jdenise
 */
public class PropertiesActionImpl extends ActionImpl<Changes> {

	public PropertiesActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule,
		SignatureRule signatureRule) {

		super(logger, buffer, selectionRule, signatureRule);
	}

	@Override
	public String getAcceptExtension() {
		return ".properties";
	}

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		String outputName = transformBinaryType(inputData.name());
		if (outputName != null) {
			getLogger().debug("Properties file {}, relocated to {}", inputData.name(), outputName);
			setResourceNames(inputData.name(), outputName);
			return new ByteDataImpl(outputName, inputData.buffer());
		} else {
			setResourceNames(inputData.name(), inputData.name());
			return inputData;
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
