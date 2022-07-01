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

import java.util.Arrays;
import java.util.function.Predicate;

public enum ActionType {
	RENAME("Rename Action"),

	CLASS("Class Action", ".class"),
	MANIFEST("Manifest Action", "manifest.mf"),
	FEATURE("Feature Action", ".mf"), // Sub of MANIFEST
	SERVICE_LOADER_CONFIG("Service Config Action"),
	PROPERTIES("Properties Action", ".properties"), // Sub of TEXT

	TEXT("Text Action"),
	JAVA("Java Action", ".java"), // Sub of TEXT
	JSP("JSP Action", ".jsp"), // Sub of TEXT
	XML("XML Action", ".xml"), // Sub of TEXT

	ZIP("Zip Action", ".zip"),
	JAR("Jar Action", ".jar"),
	WAR("WAR Action", ".war"),
	RAR("RAR Action", ".rar"),
	EAR("EAR Action", ".ear"),

	DIRECTORY("Directory Action");

	private final String name;
	private final Predicate<String> matcher;

	ActionType(String name, String... extensions) {
		this.name = name;
		this.matcher = Arrays.stream(extensions)
			.map(this::extensionPredicate)
			.reduce(Predicate::or)
			.orElse(this::matchingUnsupported);
	}

	private Predicate<String> extensionPredicate(String extension) {
		int length = extension.length();
		return resourceName -> resourceName.regionMatches(true, resourceName.length() - length, extension, 0, length);
	}

	private boolean matchingUnsupported(String resourceName) {
		throw new UnsupportedOperationException(getName().concat(" does not support resource name matching"));
	}

	public Predicate<String> resourceNameMatcher() {
		return matcher;
	}

	public String getName() {
		return name;
	}

	public boolean matches(String tag) {
		return name().regionMatches(true, 0, tag, 0, tag.length());
	}
}
