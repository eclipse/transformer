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

package org.eclipse.transformer;

public class OptionSettings {
	public static final boolean	HAS_ARG			= true;
	public static final boolean	HAS_ARGS		= true;
	public static final boolean	HAS_ARG_COUNT	= true;
	public static final boolean	IS_REQUIRED		= true;
	public static final String	NO_GROUP		= null;

	OptionSettings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
		boolean required, String groupTag) {
		this(shortTag, longTag, description, hasArg, hasArgs, !HAS_ARG_COUNT, -1, required, groupTag);
	}

	OptionSettings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
		boolean hasArgCount, int argCount,
		boolean required, String groupTag) {

		this.shortTag = shortTag;
		this.longTag = longTag;
		this.description = description;

		this.required = required;

		this.hasArg = hasArg;
		this.hasArgs = hasArgs;
		this.hasArgCount = hasArgCount;
		this.argCount = argCount;

		this.groupTag = groupTag;
	}

	private final String	shortTag;
	private final String	longTag;
	private final String	description;

	public String getShortTag() {
		return shortTag;
	}

	public String getLongTag() {
		return longTag;
	}

	public String getDescription() {
		return description;
	}

	//

	// Is this option required.
	// If in a group, is at least one of the group required.

	private final boolean	required;

	//

	private final boolean	hasArg;
	private final boolean	hasArgs;
	private final boolean	hasArgCount;
	private final int		argCount;

	private final String	groupTag;

	public boolean getHasArg() {
		return hasArg;
	}

	public boolean getHasArgs() {
		return hasArgs;
	}

	public boolean getHasArgCount() {
		return hasArgCount;
	}

	public int getArgCount() {
		return argCount;
	}

	public String getGroupTag() {
		return groupTag;
	}

	public boolean isRequired() {
		return required;
	}
}
