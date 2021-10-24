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

import java.util.Map.Entry;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.utf8properties.UTF8Properties;

/*
 * Simple logging properties, per org.slf4j.impl.SimpleLogger.
 *
 * Loggers are initialized from system properties, and from the
 * class loader resource "simplelogger.properties".
 *
 * org.slf4j.simpleLogger.logFile
 * Where logger output is written.  Either a file name, "System.out",
 * or "System.err".
 * Defaults to "System.err".
 *
 * org.slf4j.simpleLogger.cacheOutputStream
 * When true, the output stream will be cached: assigned once during
 * initialization and re-used independently of the current values
 * of System.out and System.err.
 * Defaults to false.
 *
 * org.slf4j.simpleLogger.defaultLogLevel
 * Default log level all root SimpleLogger instances,
 * one of ( "trace", "debug", "info", "warn", "error", or "off" ).
 * Defaults to "info".
 *
 * org.slf4j.simpleLogger.log.a.b.c
 * Logging detail level for the SimpleLogger instance named "a.b.c",
 * one of { "trace", "debug", "info", "warn", "error" or "off" }.  When
 * the named logger is initialized, its level is assigned to the specified
 * value.  When unset, the level is set to the level of the nearest parent
 * logger, or, for a root logger, to the default log level.
 * Defaults to unset.
 *
 * org.slf4j.simpleLogger.showDateTime
 * Set to true to include the date and time in log messages.
 * Defaults to false.
 *
 * org.slf4j.simpleLogger.dateTimeFormat
 * The format to use for date and time values which are included in log messages.
 * Formats are specified according to:
 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.
 * Defaults to display the the number of milliseconds since the logger was
 * initialized.  The default is used if a non-valid format is specified.
 *
 * org.slf4j.simpleLogger.showThreadName
 * Set to true to include the thread name in log messages.
 * Defaults to true.
 *
 * org.slf4j.simpleLogger.showLogName
 * Set to  true to include the logger name in log messages.
 * Defaults to true.
 *
 * org.slf4j.simpleLogger.showShortLogName
 * Set to true to include the last component of the logger name
 * to be included in log messages.
 * Defaults to false.
 *
 * org.slf4j.simpleLogger.levelInBrackets
 * Set to true if the level indicator should be output within brackets.
 * Defaults to false.
 *
 * org.slf4j.simpleLogger.warnLevelString
 * Value to display for warning level messages.
 * Defaults to "WARN".
 */
public class TransformerLoggerFactory {
	public TransformerLoggerFactory(Transformer transformer) {
		this.transformer = transformer;
		this.settings = new LoggerSettings(this.transformer);
	}

	public final Transformer transformer;

	public Transformer getTransformer() {
		return transformer;
	}

	public LoggerSettings settings;

	public LoggerSettings getSettings() {
		return settings;
	}

	// The logger factory operates before before the transformer
	// creates its logger. Usual log output is not available.
	// Logging must be to system output or to system error.

	protected void outputPrint(String message, Object... parms) {
		transformer.outputPrint(message, parms);
	}

	protected void dual_outputPrint(String message, Object... parms) {
		transformer.outputPrint(message, parms);
	}

	protected void verbose(String message, Object... parms) {
		if (settings.isVerbose) {
			outputPrint(message, parms);
		}
	}

	protected void dual_verbose(String message, Object... parms) {
		if (settings.isVerbose) {
			dual_outputPrint(message, parms);
		}
	}

	protected void noTerse(String message, Object... parms) {
		if (!settings.isTerse) {
			outputPrint(message, parms);
		}
	}

	protected void dual_noTerse(String message, Object... parms) {
		if (!settings.isTerse) {
			dual_outputPrint(message, parms);
		}
	}

	//

	public Logger createLogger() throws TransformException {
		setLoggingProperties(); // throws TransformException
		String logName = selectLoggerName();
		return LoggerFactory.getLogger(logName);
	}

	protected void setLoggingProperties() throws TransformException {
		String logFilePropertyName = LoggerProperty.LOG_FILE.getPropertyName();
		if (settings.logFileName != null) {
			setLoggingProperty(logFilePropertyName, settings.logFileName);
		}

		String logLevelPropertyName = LoggerProperty.LOG_LEVEL_ROOT.getPropertyName();
		if (settings.logLevel != null) {
			setLoggingProperty(logLevelPropertyName, settings.logLevel);
		}

		if (settings.properties != null) {
			for (String propertyAssignment : settings.properties) {
				assignLoggingProperty(propertyAssignment);
				// throws TransformException
			}
		}

		if (settings.propertyFileName != null) {
			UTF8Properties properties;
			try {
				properties = transformer.loadExternalProperties("Logging Properties File", settings.propertyFileName);
			} catch (Exception e) {
				throw new TransformException("Failed to load logging properties [ " + settings.propertyFileName + " ]", e);
			}

			for (Entry<Object, Object> pEntry : properties.entrySet()) {
				String pName = pEntry.getKey().toString();
				String pValue = pEntry.getValue().toString();
				setLoggingProperty(pName, pValue); // throws TransformException
			}
		}

		dual_verbose("Verbose output requested"); // Output only when verbose output is enabled.
	}

	protected String selectLoggerName() {
		String logNameCase;
		String logName;
		if (settings.logName == null) {
			logNameCase = "Defaulted";
			logName = Transformer.class.getSimpleName();
		} else {
			logNameCase = "Assigned";
			logName = settings.logName;
		}
		dual_noTerse("Logger name [ %s ] (%s)", logName, logNameCase);

		return logName;
	}

	//

	protected static final String SIMPLE_LOGGER_PROPERTY_PREFIX = "org.slf4j.simpleLogger.";

	protected String completePropertyName(String propertyName) {
		if (propertyName.startsWith(SIMPLE_LOGGER_PROPERTY_PREFIX)) {
			return null;
		} else if (propertyName.contains(".")) {
			return null;
		} else {
			return SIMPLE_LOGGER_PROPERTY_PREFIX + propertyName;
		}
	}

	protected String[] parseAssignment(String propertyAssignment) {
		int equalsOffset = propertyAssignment.indexOf("=");
		if (equalsOffset == -1) {
			return null;
		} else {
			String propertyName = propertyAssignment.substring(0, equalsOffset);
			String propertyValue = propertyAssignment.substring(equalsOffset + 1);
			if (propertyName.isEmpty() || propertyValue.isEmpty()) {
				return null;
			}
			return new String[] {
				propertyName, propertyValue
			};
		}
	}

	protected void assignLoggingProperty(String pAssignment) throws TransformException {
		String[] assignmentValues = parseAssignment(pAssignment);
		if (assignmentValues == null) {
			throw new TransformException("Malformed logger property assignment [ " + pAssignment + " ]");
		}

		String pName = assignmentValues[0];
		String pValue = assignmentValues[1];

		String completedName = completePropertyName(pName);
		if (completedName != null) {
			dual_verbose("Logging property name adjusted from [ %s ] to [ %s ]", pName, completedName, pValue);
		} else {
			completedName = pName;
		}

		setLoggingProperty(completedName, pValue);
	}

	protected void setLoggingProperty(String pName, String newValue) {
		String oldValue = System.getProperty(pName);

		if (oldValue != null) {
			dual_noTerse("Logging property assignment [ %s ] to [ %s ] blocked by prior value [ %s ]",
					pName, newValue, oldValue);

		} else {
			System.setProperty(pName, newValue);
			dual_noTerse("Logging property assigns [ %s ] to [ %s ]", pName, newValue);
		}
	}

	public static boolean logToSysOut() {
		String logFile = System.getProperty(LoggerProperty.LOG_FILE.getPropertyName());
		return ((logFile != null) && logFile.equals("System.out"));
	}

	public static boolean logToSysErr() {
		String logFile = System.getProperty(LoggerProperty.LOG_FILE.getPropertyName());
		return ((logFile == null) || logFile.equals("System.err"));
	}
}
