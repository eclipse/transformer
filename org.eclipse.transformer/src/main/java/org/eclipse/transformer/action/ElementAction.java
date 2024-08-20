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

package org.eclipse.transformer.action;

import org.eclipse.transformer.TransformException;

import java.util.regex.Pattern;

public interface ElementAction extends Action {

	Pattern SIGNATURE_FILE_PATTERN = Pattern.compile("META-INF/([^/]+\\.(?:DSA|RSA|EC|SF)|SIG-[^/]+)");

	@Override
	default boolean isElementAction() {
		return true;
	}

	/**
	 * Apply this action on an input data.
	 * <p>
	 * This API is specific to element type actions.
	 *
	 * @param inputData The input data.
	 * @return Transformed input data.
	 * @throws TransformException Thrown if the transform failed.
	 */
	ByteData apply(ByteData inputData) throws TransformException;
}
