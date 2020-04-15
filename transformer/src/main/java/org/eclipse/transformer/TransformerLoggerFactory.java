/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.transformer;

import java.util.Map.Entry;

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

	protected void verboseOutput(String message, Object...parms) {
		if ( settings.isVerbose ) {
			transformer.outputPrint(message, parms);
		}
	}
	
	protected void normalOutput(String message, Object...parms) {
		if ( !settings.isVerbose && !settings.isTerse ) {
			transformer.outputPrint(message, parms);
		}
	}
	
	protected void nonTerseOutput(String message, Object...parms) {
		if ( !settings.isTerse ) {
			transformer.outputPrint(message, parms);
		}
	}

	protected void terseOutput(String message, Object...parms) {
		if ( settings.isTerse ) {
			transformer.outputPrint(message, parms);
		}
	}

	// Options from the transformer:
	//
	// LOG_TERSE("q", "quiet", "Display quiet output",
	//     !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	// LOG_VERBOSE("v", "verbose", "Display verbose output",
	//     !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	// LOG_PROPERTY("lp", "logProperty", "Logging property",
	//     !OptionSettings.HAS_ARG, OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	// LOG_PROPERTY_FILE("lpf", "logPropertyFile", "Logging properties file",
	//     OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	// LOG_LEVEL("ll", "logLevel", "Logging level",
	//     OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
    // LOG_FILE("lf", "logFile", "Logging file",
	//     OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
	//     !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	public static class LoggerSettings {
		public final boolean isTerse;
		public final boolean isVerbose;

		public final String[] properties;
		public final String propertyFileName;

		public final String logName;
		public final String logLevel;
		public final String logFileName;
		
		public LoggerSettings(Transformer transformer) {
			this.isTerse = transformer.hasOption(Transformer.AppOption.LOG_TERSE);
			this.isVerbose = transformer.hasOption(Transformer.AppOption.LOG_VERBOSE);

			this.properties = transformer.getOptionValues(Transformer.AppOption.LOG_PROPERTY);
			this.propertyFileName = transformer.getOptionValue(Transformer.AppOption.LOG_PROPERTY_FILE);

			this.logName = transformer.getOptionValue(Transformer.AppOption.LOG_NAME);

			this.logLevel = transformer.getOptionValue(Transformer.AppOption.LOG_LEVEL);
			this.logFileName = transformer.getOptionValue(Transformer.AppOption.LOG_FILE);
		}
	}

	//

	public Logger createLogger() throws TransformException {
		setLoggingProperties(); // throws TransformException
		String logName = selectLoggerName();
		return LoggerFactory.getLogger(logName);
	}

	protected void setLoggingProperties() throws TransformException {
		if ( settings.logFileName != null ) {
			setLoggingProperty(LoggerProperty.LOG_FILE.getPropertyName(), settings.logFileName);
		}

		if ( settings.logLevel != null ) {
			setLoggingProperty(LoggerProperty.LOG_LEVEL_ROOT.getPropertyName(), settings.logLevel);
		}

		if ( settings.properties != null ) {
			for ( String propertyAssignment : settings.properties ) {
				assignLoggingProperty(propertyAssignment); // throws TransformException
			}
		}

		if ( settings.propertyFileName != null ) {
			UTF8Properties properties;
			try {
				properties = transformer.loadExternalProperties("Logging Properties File", settings.propertyFileName);
			} catch ( Exception e ) {
				throw new TransformException("Failed to load logging properties [ " + settings.propertyFileName + " ]", e);
			}

			for ( Entry<Object, Object> propertyEntry : properties.entrySet() ) {
				String propertyName = propertyEntry.getKey().toString();
				String propertyValue = propertyEntry.getValue().toString();
				setLoggingProperty(propertyName, propertyValue); // throws TransformException
			}
		}

		if ( settings.isTerse ) {
			// Don't report; in terse mode!
		} else if ( settings.isVerbose ) {
			nonTerseOutput("Verbose output requested");
		} else {
			// Don't use of default logging mode
		}
	}

	protected String selectLoggerName() {
		String logNameCase;
		String logName;
		if ( settings.logName == null ) {
			logNameCase = "Defaulted";
			logName = Transformer.class.getSimpleName();
		} else {
			logNameCase = "Assigned";
			logName = settings.logName;
		}
		nonTerseOutput("Logger name [ %s ] (%s)", logName, logNameCase);

		return logName;
	}

	//
	
	protected static final String SIMPLE_LOGGER_PROPERTY_PREFIX = "org.slf4j.simpleLogger."; 

	protected String completePropertyName(String propertyName) {
		if ( propertyName.startsWith(SIMPLE_LOGGER_PROPERTY_PREFIX) ) {
			return null;
		} else if ( propertyName.contains(".") ) {
			return null;
		} else {
			return SIMPLE_LOGGER_PROPERTY_PREFIX + propertyName;
		}
	}

	protected String[] parseAssignment(String propertyAssignment) {
		int equalsOffset = propertyAssignment.indexOf("=");
		if ( equalsOffset == -1 ) {
			return null;
		} else {
			String propertyName = propertyAssignment.substring(0, equalsOffset);
			String propertyValue = propertyAssignment.substring(equalsOffset + 1);
			if ( propertyName.isEmpty() || propertyValue.isEmpty() ) {
				return null;
			}
			return new String[] { propertyName, propertyValue };
		}
	}

	protected void assignLoggingProperty(String propertyAssignment) throws TransformException {
		String[] assignmentValues = parseAssignment(propertyAssignment);
		if ( assignmentValues == null ) {
			throw new TransformException("Malformed logger property assignment [ " + propertyAssignment + " ]");
		}
		
		String propertyName = assignmentValues[0];
		String propertyValue = assignmentValues[1];

		String completedPropertyName = completePropertyName(propertyName);
		if ( completedPropertyName != null ) {
			verboseOutput(
				"Transformer logging property adjusted from [ %s ] to [ %s ]",
				propertyName, completedPropertyName, propertyValue);
		} else {
			completedPropertyName = propertyName;
		}

		setLoggingProperty(completedPropertyName, propertyValue);
	}

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

		private LoggerProperty(String propertyName) {
			this.propertyName = propertyName;
		}

		private final String propertyName;

		public String getPropertyName() {
			return propertyName;
		}
	}

	protected void setLoggingProperty(String propertyName, String newPropertyValue) {
		String oldPropertyValue = System.getProperty(propertyName);

		if ( oldPropertyValue != null ) {
			nonTerseOutput(
				"Blocked assignment of logging property [ %s ] to [ %s ] by prior value [ %s ]",
				propertyName, newPropertyValue, oldPropertyValue);

		} else {
			System.setProperty(propertyName, newPropertyValue);

			nonTerseOutput(
				"Assigning logging property [ %s ] to [ %s ]",
				propertyName, newPropertyValue);
		}
	}

	public static boolean logToSysOut() {
		String logFile = System.getProperty( LoggerProperty.LOG_FILE.getPropertyName() );
		return ( (logFile != null) && logFile.equals("System.out") );
	}

	public static boolean logToSysErr() {
		String logFile = System.getProperty( LoggerProperty.LOG_FILE.getPropertyName() );
		return ( (logFile == null) || logFile.equals("System.err") );
	}
}
