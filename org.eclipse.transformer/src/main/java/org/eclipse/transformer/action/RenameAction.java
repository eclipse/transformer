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

public interface RenameAction extends ElementAction {
	@Override
	default boolean isRenameAction() {
		return true;
	}

	/**
	 * Special to {@code RenameAction}: Rename the action. Let the caller
	 * handle transferring the content.
	 * <p>
	 * This API is specific to the rename action.
	 *
	 * @param inputName The name of the input resource.
	 * @return The output name.
	 */
	String apply(String inputName);
}
