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
	USAGE("u", "usage", "Display usage", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	HELP("h", "help", "Display help", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	// TODO: Refine versioning
	// FULL_VERSION("f", "fullVersion", "Display full version information",
	// !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	// !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	// See 'Transformer.hasArg(AppOption)' for particular issues relating to
	// 'LOG_TERSE' and 'LOG_VERBOSE'.
	LOG_TERSE("q", "quiet", "Display quiet output", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_VERBOSE("v", "verbose", "Display verbose output", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_PROPERTY("lp", "logProperty", "Logging property", !OptionSettings.HAS_ARG, OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_PROPERTY_FILE("lpf", "logPropertyFile", "Logging properties file", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_NAME("ln", "logName", "Logger name", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_LEVEL("ll", "logLevel", "Logging level", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	LOG_FILE("lf", "logFile", "Logging file", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	RULES_SELECTIONS("ts", "selection", "Transformation selections URL", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	RULES_RENAMES("tr", "renames", "Transformation package renames URL", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	RULES_VERSIONS("tv", "versions", "Transformation package versions URL", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	RULES_BUNDLES("tb", "bundles", "Transformation bundle updates URL", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	RULES_DIRECT("td", "direct", "Transformation direct string replacements", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	RULES_MASTER_TEXT("tf", "text", "Map of filenames to property files", OptionSettings.HAS_ARG,
		!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	RULES_IMMEDIATE_DATA("ti", "immediate", "Immediate rule data", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		OptionSettings.HAS_ARG_COUNT, 3,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	// Issue #154: Enable processing of JARs within JARs. See:
	// https://github.com/eclipse/transformer/issues/154
	//
	// By default, archive nesting is restricted to JavaEE active locations.
	// This may be relaxed to enable JAR and ZIP within JAR, ZIP within ZIP,
	// and ZIP within EAR, WAR, and RAR.
	//
	// See 'getRootAction'.

	WIDEN_ARCHIVE_NESTING("w", "widen", "Widen archive nesting", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	// RULES_MASTER_XML("tf", "xml", "Map of XML filenames to property files", OptionSettings.HAS_ARG,
	//    !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	INVERT("i", "invert", "Invert transformation rules", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	FILE_TYPE("t", "type", "Input file type", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	OVERWRITE("o", "overwrite", "Overwrite", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	DRYRUN("d", "dryrun", "Dry run", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED,
		OptionSettings.NO_GROUP),

	RULES_PER_CLASS_CONSTANT("tp", "per-class-constant", "Transformation per class constant string replacements",
		OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP);

	AppOption(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
		boolean hasArgCount, int argCount,
		boolean required, String groupTag) {

		this.settings = new OptionSettings(shortTag, longTag, description, hasArg, hasArgs, hasArgCount, argCount,
			required, groupTag);
	}

	AppOption(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
		boolean required, String groupTag) {

		this.settings = new OptionSettings(shortTag, longTag, description, hasArg, hasArgs, required, groupTag);
	}

	private final OptionSettings settings;

	public OptionSettings getSettings() {
		return settings;
	}

	public String getShortTag() {
		return getSettings().getShortTag();
	}

	public String getLongTag() {
		return getSettings().getLongTag();
	}

	public String getDescription() {
		return getSettings().getDescription();
	}

	public boolean isRequired() {
		return getSettings().isRequired();
	}

	public boolean getHasArg() {
		return getSettings().getHasArg();
	}

	public String getGroupTag() {
		return getSettings().getGroupTag();
	}
}
