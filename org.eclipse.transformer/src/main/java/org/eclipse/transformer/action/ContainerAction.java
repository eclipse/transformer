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

import java.io.File;
import java.util.List;

public interface ContainerAction extends Action {
	@Override
	default boolean isContainerAction() {
		return true;
	}

	@Override
	ContainerChanges getActiveChanges();

	@Override
	ContainerChanges getLastActiveChanges();

	ActionSelector getActionSelector();

	List<Action> getActions();

	Action selectAction(String resourceName);

	Action selectAction(String resourceName, File resourceFile);
}
