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
	RENAME("Rename Action", ""),

	CLASS("Class Action", ".class"),
	MANIFEST("Manifest Action", "manifest.mf"),
	FEATURE("Feature Action", ".mf"), // Sub of MANIFEST
	SERVICE_LOADER_CONFIG("Service Config Action", ""),
	PROPERTIES("Properties Action", ".properties"), // Sub of TEXT

	TEXT("Text Action", ""),
	JAVA("Java Action", ".java"), // Sub of TEXT
	JSP("JSP Action", ".jsp"), // Sub of TEXT
	XML("XML Action", ".xml"), // Sub of TEXT

	ZIP("Zip Action", ".zip"),
	JAR("Jar Action", ".jar"),
	WAR("WAR Action", ".war"),
	RAR("RAR Action", ".rar"),
	EAR("EAR Action", ".ear"),

	DIRECTORY("Directory Action", "");

	private final String name;
	private final String extension;

	ActionType(String name, String extension) {
		this.name = name;
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

	public String getName() {
		return name;
	}

	public boolean matches(String tag) {
		return name().regionMatches(true, 0, tag, 0, tag.length());
	}
}
