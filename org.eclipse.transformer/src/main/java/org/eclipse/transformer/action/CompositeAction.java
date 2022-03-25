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

public interface CompositeAction extends Action {
	List<Action> getActions();

	default void addAction(Action action) {
		getActions().add(action);
	}

	Action acceptAction(String resourceName, File resourceFile);

	Action getAcceptedAction();
}
