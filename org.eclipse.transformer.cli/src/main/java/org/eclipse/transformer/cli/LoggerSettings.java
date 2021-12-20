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

package org.eclipse.transformer.cli;

import java.util.List;

import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;

public class LoggerSettings {
	public final boolean	isTerse;
	public final boolean	isVerbose;

	public final List<String>	properties;
	public final String		propertyFileName;

	public final String		logName;
	public final String		logLevel;
	public final String		logFileName;

	public LoggerSettings(TransformOptions options) {
		this.isTerse = options.hasOption(AppOption.LOG_TERSE);
		this.isVerbose = options.hasOption(AppOption.LOG_VERBOSE);

		this.properties = options.getOptionValues(AppOption.LOG_PROPERTY);
		this.propertyFileName = options.normalize(options.getOptionValue(AppOption.LOG_PROPERTY_FILE));

		this.logName = options.getOptionValue(AppOption.LOG_NAME);

		this.logLevel = options.getOptionValue(AppOption.LOG_LEVEL);
		this.logFileName = options.normalize(options.getOptionValue(AppOption.LOG_FILE));

		// System.out.println("LoggerSettings: isTerse [ " + this.isTerse +
		// " ]");
		// System.out.println("LoggerSettings: isVerbose [ " +
		// this.isVerbose + " ]");
		// System.out.println("LoggerSettings: properties [ " +
		// this.properties + " ]");
		// System.out.println("LoggerSettings: propertyFileName [ " +
		// this.propertyFileName + " ]");
		// System.out.println("LoggerSettings: logName [ " + this.logName +
		// " ]");
		// System.out.println("LoggerSettings: logLevel [ " + this.logLevel
		// + " ]");
		// System.out.println("LoggerSettings: logFileName [ " +
		// this.logFileName + " ]");
	}
}
