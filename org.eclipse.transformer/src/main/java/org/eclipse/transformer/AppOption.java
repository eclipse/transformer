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

public enum AppOption {
	USAGE(new Settings("u", "usage", "Display usage", !Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),
	HELP(new Settings("h", "help", "Display help", !Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),

	LOG_QUIET(
		new Settings("q", "quiet", "Display quiet output: error level logging", !Settings.HAS_ARG, !Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, "LOG_GROUP")),
	LOG_DEBUG(new Settings("v", "verbose", "Display verbose output: debug level logging", !Settings.HAS_ARG,
		!Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, "LOG_GROUP")),
	LOG_TRACE(
		new Settings("x", "trace", "Display trace output: trace level logging", !Settings.HAS_ARG, !Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, "LOG_GROUP")),
	LOG_PROPERTY(new Settings("lp", "logProperty", "Logging property", !Settings.HAS_ARG, Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, Settings.NO_GROUP)),
	LOG_PROPERTY_FILE(new Settings("lpf", "logPropertyFile", "Logging properties file", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),
	LOG_NAME(new Settings("ln", "logName", "Logger name", Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),
	LOG_LEVEL(new Settings("ll", "logLevel", "Logging level", Settings.HAS_ARG, !Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, Settings.NO_GROUP)),
	LOG_FILE(new Settings("lf", "logFile", "Logging file", Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),

	RULES_SELECTIONS(new Settings("ts", "selection", "Transformation selections URL", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),
	RULES_RENAMES(new Settings("tr", "renames", "Transformation package renames URL", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),
	RULES_VERSIONS(new Settings("tv", "versions", "Transformation package versions URL", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),
	RULES_BUNDLES(new Settings("tb", "bundles", "Transformation bundle updates URL", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),
	RULES_DIRECT(new Settings("td", "direct", "Transformation direct string replacements", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),

	RULES_MASTER_TEXT(new Settings("tf", "text", "Map of filenames to property files", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),

	RULES_IMMEDIATE_DATA(new Settings("ti", "immediate", "Immediate rule data", !Settings.HAS_ARG, !Settings.HAS_ARGS,
		Settings.HAS_ARG_COUNT, 3, !Settings.IS_REQUIRED, Settings.NO_GROUP)),

	// Issue #154: Enable processing of JARs within JARs. See:
	// https://github.com/eclipse/transformer/issues/154
	//
	// By default, archive nesting is restricted to JavaEE active locations.
	// This may be relaxed to enable JAR and ZIP within JAR, ZIP within ZIP,
	// and ZIP within EAR, WAR, and RAR.
	//
	// See 'getRootAction'.

	WIDEN_ARCHIVE_NESTING(new Settings("w", "widen", "Widen archive nesting", !Settings.HAS_ARG, !Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, Settings.NO_GROUP)),

	// RULES_MASTER_XML("tf", "xml", "Map of XML filenames to property files",
	// OptionSettings.HAS_ARG,
	// !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED,
	// OptionSettings.NO_GROUP),

	INVERT(new Settings("i", "invert", "Invert transformation rules", !Settings.HAS_ARG, !Settings.HAS_ARGS,
		!Settings.IS_REQUIRED, Settings.NO_GROUP)),

	FILE_TYPE(new Settings("t", "type", "Input file type", Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),
	OVERWRITE(new Settings("o", "overwrite", "Overwrite", !Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),
	ZIP_ENTRY_ENCODE(new Settings("zipenc", "zip-entry-encode", "Entry encode in zip (default: UTF-8)", Settings.HAS_ARG,
		!Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP)),

	DRYRUN(new Settings("d", "dryrun", "Dry run", !Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED,
		Settings.NO_GROUP)),

	RULES_PER_CLASS_CONSTANT(
		new Settings("tp", "per-class-constant", "Transformation per class constant string replacements",
			Settings.HAS_ARG, !Settings.HAS_ARGS, !Settings.IS_REQUIRED, Settings.NO_GROUP));

	AppOption(Settings settings) {
		this.settings = settings;
	}

	private final Settings settings;

	public String getShortTag() {
		return settings.getShortTag();
	}

	public String getLongTag() {
		return settings.getLongTag();
	}

	public String getDescription() {
		return settings.getDescription();
	}

	public boolean isRequired() {
		return settings.isRequired();
	}

	public boolean getHasArg() {
		return settings.getHasArg();
	}

	public boolean getHasArgs() {
		return settings.getHasArgs();
	}

	public boolean getHasArgCount() {
		return settings.getHasArgCount();
	}

	public int getArgCount() {
		return settings.getArgCount();
	}

	public String getGroupTag() {
		return settings.getGroupTag();
	}

	static class Settings {
		static final boolean	HAS_ARG			= true;
		static final boolean	HAS_ARGS		= true;
		static final boolean	HAS_ARG_COUNT	= true;
		static final boolean	IS_REQUIRED		= true;
		static final String		NO_GROUP		= null;

		Settings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs, boolean required,
			String groupTag) {
			this(shortTag, longTag, description, hasArg, hasArgs, !HAS_ARG_COUNT, -1, required, groupTag);
		}

		Settings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
			boolean hasArgCount, int argCount, boolean required, String groupTag) {

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
		// Is this option required.
		// If in a group, is at least one of the group required.

		private final boolean	required;

		private final boolean	hasArg;
		private final boolean	hasArgs;
		private final boolean	hasArgCount;
		private final int		argCount;

		private final String	groupTag;

		String getShortTag() {
			return shortTag;
		}

		String getLongTag() {
			return longTag;
		}

		String getDescription() {
			return description;
		}

		boolean getHasArg() {
			return hasArg;
		}

		boolean getHasArgs() {
			return hasArgs;
		}

		boolean getHasArgCount() {
			return hasArgCount;
		}

		int getArgCount() {
			return argCount;
		}

		String getGroupTag() {
			return groupTag;
		}

		boolean isRequired() {
			return required;
		}
	}
}
