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

public enum LoggerProperty {
	LOG_FILE("org.slf4j.simpleLogger.logFile"),
	LOG_CACHE_OUTPUT("org.slf4j.simpleLogger.cacheOutputStream"),

	LOG_LEVEL_ROOT("org.slf4j.simpleLogger.defaultLogLevel"),
	LOG_LEVEL_CHILD("org.slf4j.simpleLogger.log.a.b.c"),
	LOG_LEVEL_IN_BRACkETS("org.slf4j.simpleLogger.levelInBrackets"),

	LOG_SHOW_DATE_TIME("org.slf4j.simpleLogger.showDateTime"),
	LOG_DATE_TIME_FORMAT("org.slf4j.simpleLogger.dateTimeFormat"),
	LOG_SHOW_THREAD_NAME("org.slf4j.simpleLogger.showThreadName"),
	LOG_SHOW_LOG_NAME("org.slf4j.simpleLogger.showLogName"),
	LOG_SHOW_SHORT_LOG_NAME("org.slf4j.simpleLogger.showShortLogName"),
	LOG_WARN_STRING("org.slf4j.simpleLogger.warnLevelString");

	LoggerProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	private final String propertyName;

	public String getPropertyName() {
		return propertyName;
	}
}
