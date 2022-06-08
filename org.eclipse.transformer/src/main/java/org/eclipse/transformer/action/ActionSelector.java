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
import java.util.Collection;
import java.util.List;

/**
 * Action selector.
 * <p>
 * The name <em>CompositeAction</em> is a historical artifact.
 */
public interface ActionSelector {
	List<Action> getActions();

	default void addAction(Action action) {
		getActions().add(action);
	}

	default void addActions(Collection<Action> actions) {
		getActions().addAll(actions);
	}

	default Action selectAction(String resourceName, File resourceFile) {
		for (Action action : getActions()) {
			if (action.acceptResource(resourceName, resourceFile)) {
				return action;
			}
		}
		return null;
	}

	default Action acceptType(String actionTypeName) {
		for (Action action : getActions()) {
			if (action.acceptType(actionTypeName)) {
				return action;
			}
		}
		return null;
	}
}
