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

package org.eclipse.transformer.log;

import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.Transformer;

public class LoggerSettings {
	public final boolean	isTerse;
	public final boolean	isVerbose;
	public final boolean	isExtraDebug;

	public final String[]	properties;
	public final String		propertyFileName;

	public final String		logName;
	public final String		logLevel;
	public final String		logFileName;

	public LoggerSettings(Transformer transformer) {
		this.isTerse = transformer.hasOption(AppOption.LOG_TERSE);
		this.isVerbose = transformer.hasOption(AppOption.LOG_VERBOSE);
		this.isExtraDebug = transformer.hasOption(AppOption.LOG_EXTRA_DEBUG);

		this.properties = transformer.getOptionValues(AppOption.LOG_PROPERTY);
		this.propertyFileName = transformer.getOptionValue(AppOption.LOG_PROPERTY_FILE,
			Transformer.DO_NORMALIZE);

		this.logName = transformer.getOptionValue(AppOption.LOG_NAME);

		this.logLevel = transformer.getOptionValue(AppOption.LOG_LEVEL);
		this.logFileName = transformer.getOptionValue(AppOption.LOG_FILE, Transformer.DO_NORMALIZE);
	}
}
