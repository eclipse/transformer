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

public interface BundleData {
	char	ADDITIVE_CHAR	= '+';
	char	QUOTE_CHAR		= '"';
	char	COMMA_CHAR		= ',';

	String getSymbolicName();

	String getVersion();

	boolean getAddName();

	String getName();

	boolean getAddDescription();

	String getDescription();

	String updateName(String initialName);

	String updateDescription(String initialDescription);
}
