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

// TODO:
//
// Is this action necessary?
//
// The rename action seems to handle the case which is processed by this properties action.
//
// See issue #290

/**
 * Action for properties resources.
 * <p>
 * This action does no content transformation. This action is used <em>only</em>
 * to rename properties files which are in a package dependent location.
 */
public class PropertiesActionImpl extends ElementActionImpl {

	public PropertiesActionImpl(ActionInitData initData) {
		super(initData);
	}

	@Override
	public String getName() {
		return "Properties Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.PROPERTIES;
	}

	@Override
	public String getAcceptExtension() {
		return ".properties";
	}

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		startRecording(inputData);

		try {
			String inputName = inputData.name();
			String outputName = transformBinaryType(inputName);

			if (outputName != null) {
				getLogger().debug("Properties file {}, relocated to {}", inputName, outputName);
				setResourceNames(inputName, outputName);
				return inputData.copy(outputName);

			} else {
				setResourceNames(inputName, inputName);
				return inputData;
			}

		} finally {
			stopRecording(inputData);
		}
	}
}
