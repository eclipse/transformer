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

public enum ActionType {
	RENAME,

	CLASS,
	MANIFEST,
	FEATURE, // Sub of MANIFEST
	SERVICE_LOADER_CONFIG,
	PROPERTIES,

	TEXT,
	JAVA, // Sub of TEXT
	JSP, // Sub of TEXT
	XML, // Currently unused

	ZIP,
	JAR,
	WAR,
	RAR,
	EAR,

	DIRECTORY;

	public boolean matches(String tag) {
		return name().regionMatches(true, 0, tag, 0, tag.length());
	}
}
