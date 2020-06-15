/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.action;

import java.util.Map;
import java.util.Set;

public interface ContainerChanges extends Changes {
	int getAllResources();

	int getAllUnselected();

	int getAllSelected();

	int getAllUnchanged();

	int getAllChanged();

	Map<String, int[]> getChangedByAction();

	Map<String, int[]> getUnchangedByAction();

	Set<String> getActionNames();

	int getChanged(Action action);

	int getChanged(String name);

	int getUnchanged(Action action);

	int getUnchanged(String name);

	//

	void add(ContainerChanges otherChanges);

	boolean hasNestedChanges();

	ContainerChanges getNestedChanges();

	void addNested(ContainerChanges otherChanges);

	//

	void record();

	boolean HAS_CHANGES = true;

	void record(Action action);

	void record(Action action, boolean hasChanges);

	void record(String name, boolean hasChanges);
}
