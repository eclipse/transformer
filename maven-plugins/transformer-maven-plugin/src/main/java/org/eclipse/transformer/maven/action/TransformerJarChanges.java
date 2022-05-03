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

package org.eclipse.transformer.maven.action;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.transformer.action.impl.ContainerChangesImpl;

public class TransformerJarChanges extends ContainerChangesImpl {
	private Set<String>	changed	= new HashSet<>();
	private Set<String>	removed	= new HashSet<>();

	public TransformerJarChanges() {
		super();
	}

	public Set<String> getChanged() {
		return changed;
	}

	public void addChanged(String change) {
		getChanged().add(change);
	}

	public Set<String> getRemoved() {
		return removed;
	}

	public void addRemoved(String remove) {
		getRemoved().add(remove);
	}
}
