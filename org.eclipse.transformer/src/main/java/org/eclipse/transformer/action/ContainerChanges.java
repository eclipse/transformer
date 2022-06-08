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

import java.util.Map;
import java.util.Set;

public interface ContainerChanges extends Changes {
	int getAllResources();
	int getAllUnselected();
	int getAllSelected();
	int getAllUnaccepted();
	int getAllAccepted();

	int getAllUnchanged();
	int getAllChanged();
	int getAllRenamed();
	int getAllContentChanged();

	int getAllFailed();
	int getAllDuplicated();

	Map<String, int[]> getUnchangedByAction();

	Map<String, int[]> getChangedByAction();
	Map<String, int[]> getRenamedByAction();
	Map<String, int[]> getContentChangedByAction();

	Map<String, int[]> getFailedByAction();
	Map<String, int[]> getDuplicatedByAction();

	Set<String> getActionNames();

	int getUnchanged(Action action);
	int getUnchanged(String name);

	int getChanged(Action action);
	int getChanged(String name);

	int getRenamed(Action action);
	int getRenamed(String name);

	int getContentChanged(Action action);
	int getContentChanged(String name);

	int getFailed(Action action);
	int getFailed(String name);

	int getDuplicated(Action action);
	int getDuplicated(String name);

	//

	void recordUnselected();
	void recordUnaccepted();
	void recordUnchanged(Action action);
	void recordAction(Action action);
	void recordFailed(Action action);
	void recordDuplicated(Action action);

	void add(ContainerChanges otherChanges);
}
