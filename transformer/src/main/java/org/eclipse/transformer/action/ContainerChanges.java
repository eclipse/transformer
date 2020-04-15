/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
