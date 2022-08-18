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

import java.io.PrintStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.BiConsumer;

import aQute.lib.io.IO;
import aQute.libg.uri.URIUtil;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

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
	public TransformerLoggerFactory(TransformerCLI cli) {
		this.cli = cli;
		this.settings = new LoggerSettings(cli);
	}

	private final TransformerCLI	cli;

	public LoggerSettings settings;

	public LoggerSettings getSettings() {
		return settings;
	}

	private void verboseOutput(String format, Object... args) {
		if (settings.isDebug || settings.isTrace) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, args);
			consolePrint(cli.getSystemOut()).accept(tp.getMessage(), tp.getThrowable());
		}
	}

	private void nonQuietOutput(String format, Object... args) {
		if (!settings.isQuiet) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, args);
			consolePrint(cli.getSystemOut()).accept(tp.getMessage(), tp.getThrowable());
		}
	}

	//

	public Logger createLogger() {
		String loggerName = selectLoggerName();
		setLoggingProperties(loggerName);
		String logFile = System.getProperty(LoggerProperty.LOG_FILE.toString());
		boolean toSysOut = (logFile != null) && logFile.equals("System.out");
		boolean toSysErr = (logFile == null) || logFile.equals("System.err");
		if (toSysErr) {
			logFile = "System.err";
		}
		verboseOutput("Logging to [ {} ]", logFile);

		Logger logger = LoggerFactory.getLogger(loggerName);
		if (toSysOut || toSysErr) { // if logging to console
			return logger;
		}
		return new DualLogger(logger, consolePrint(cli.getSystemOut()), consolePrint(cli.getSystemErr()));
	}

	private BiConsumer<String, Throwable> consolePrint(PrintStream stream) {
		return (message, t) -> {
			stream.println(message);
			if (t != null) {
				t.printStackTrace(stream);
			}
		};
	}

	private void setLoggingProperties(String loggerName) {
		String logFilePropertyName = LoggerProperty.LOG_FILE.toString();
		if (settings.logFileName != null) {
			setLoggingProperty(logFilePropertyName, settings.logFileName);
		}

		String logLevelPropertyName = LoggerProperty.LOG_LEVEL_ROOT.toString();
		if (settings.logLevel != null) {
			setLoggingProperty(logLevelPropertyName, settings.logLevel);
		}

		if (settings.isTrace) {
			setLoggingProperty(LoggerProperty.LOG_LEVEL_PREFIX + loggerName, "trace");
		} else if (settings.isDebug) {
			setLoggingProperty(LoggerProperty.LOG_LEVEL_PREFIX + loggerName, "debug");
		} else if (settings.isQuiet) {
			setLoggingProperty(LoggerProperty.LOG_LEVEL_PREFIX + loggerName, "error");
		}

		if (settings.properties != null) {
			for (String propertyAssignment : settings.properties) {
				assignLoggingProperty(propertyAssignment);
			}
		}

		if (settings.propertyFileName != null) {
			Properties properties;
			try {
				URL propertyFileUrl = URIUtil.resolve(IO.work.toURI(), settings.propertyFileName)
					.toURL();
				properties = PropertiesUtils.loadProperties(propertyFileUrl);
			} catch (Exception e) {
				throw new TransformException("Failed to load logging properties [ " + settings.propertyFileName + " ]",
					e);
			}

			for (Entry<Object, Object> propertyEntry : properties.entrySet()) {
				String propertyName = propertyEntry.getKey()
					.toString();
				String propertyValue = propertyEntry.getValue()
					.toString();
				setLoggingProperty(propertyName, propertyValue);
			}
		}
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
		verboseOutput("Logger name [ {} ] ({})", logName, logNameCase);

		return logName;
	}

	//

	protected static final String SIMPLE_LOGGER_PROPERTY_PREFIX = "org.slf4j.simpleLogger.";

	protected String completePropertyName(String propertyName) {
		if (propertyName.startsWith(SIMPLE_LOGGER_PROPERTY_PREFIX)) {
			return null;
		} else if (propertyName.indexOf('.') > -1) {
			return null;
		} else {
			return SIMPLE_LOGGER_PROPERTY_PREFIX.concat(propertyName);
		}
	}

	protected String[] parseAssignment(String propertyAssignment) {
		int equalsOffset = propertyAssignment.indexOf('=');
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

	protected void assignLoggingProperty(String propertyAssignment) {
		String[] assignmentValues = parseAssignment(propertyAssignment);
		if (assignmentValues == null) {
			throw new TransformException("Malformed logger property assignment [ " + propertyAssignment + " ]");
		}

		String propertyName = assignmentValues[0];
		String propertyValue = assignmentValues[1];

		String completedPropertyName = completePropertyName(propertyName);
		if (completedPropertyName != null) {
			verboseOutput("Transformer logging property adjusted from [ {} ] to [ {} ]", propertyName,
				completedPropertyName);
		} else {
			completedPropertyName = propertyName;
		}

		setLoggingProperty(completedPropertyName, propertyValue);
	}

	protected void setLoggingProperty(String propertyName, String newPropertyValue) {
		String oldPropertyValue = System.getProperty(propertyName);

		if (oldPropertyValue != null) {
			nonQuietOutput("Blocked assignment of logging property [ {} ] to [ {} ] by prior value [ {} ]",
				propertyName, newPropertyValue, oldPropertyValue);

		} else {
			System.setProperty(propertyName, newPropertyValue);

			nonQuietOutput("Assigning logging property [ {} ] to [ {} ]", propertyName, newPropertyValue);
		}
	}

}
