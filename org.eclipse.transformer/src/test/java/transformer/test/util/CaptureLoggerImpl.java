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

package transformer.test.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

public class CaptureLoggerImpl implements Logger {
	public static final boolean CAPTURE_INACTIVE = true;

	public CaptureLoggerImpl(String baseLoggerName, boolean captureInactive) {
		this(LoggerFactory.getLogger(baseLoggerName), captureInactive);
	}

	public CaptureLoggerImpl(String baseLoggerName) {
		this(LoggerFactory.getLogger(baseLoggerName));
	}

	public CaptureLoggerImpl(Logger baseLogger) {
		this(baseLogger, !CAPTURE_INACTIVE);
	}

	public CaptureLoggerImpl(Logger baseLogger, boolean captureInactive) {
		this.baseLogger = baseLogger;

		this.captureInactive = captureInactive;
		this.capturedEvents = new ArrayList<>();
	}

	//

	private final Logger baseLogger;

	public boolean isLoggable(Level level) {
		Logger useLogger = getBaseLogger();
		switch (level) {
			case ERROR :
				return useLogger.isErrorEnabled();
			case WARN :
				return useLogger.isWarnEnabled();
			case INFO :
				return useLogger.isInfoEnabled();
			case DEBUG :
				return useLogger.isDebugEnabled();
			case TRACE :
				return useLogger.isTraceEnabled();
			default :
				throw new IllegalArgumentException("Unknown level [ " + level + " ]");
		}
	}

	public Logger getBaseLogger() {
		return baseLogger;
	}

	@Override
	public String getName() {
		return getBaseLogger().getName();
	}

	//

	private final boolean captureInactive;

	public boolean getCaptureInactive() {
		return captureInactive;
	}

	public boolean capture(Level level) {
		return (getCaptureInactive() || isLoggable(level));
	}

	//

	public LogEvent capture(Level level, Marker marker, String message, Object... rawParms) {
		return capture(level, marker, null, message, rawParms);
	}

	public LogEvent capture(Level level, String message, Object... rawParms) {
		return capture(level, null, null, message, rawParms);
	}

	public LogEvent capture(Level level, Marker marker, Throwable th, String message, Object... rawParms) {

		if (!capture(level)) {
			return null;
		}

		LogEvent logEvent = new LogEvent(level, marker, th, message, rawParms);
		addEvent(logEvent);
		return logEvent;
	}

	private static final String[] EMPTY_STRINGS = new String[0];

	public static class LogEvent {
		public final Level		level;
		public final Marker		marker;

		public final String		threadName;
		public final long		timeStamp;

		public final String		message;
		public final String[]	parms;
		public final String		thrownMessage;

		private String			printString;

		private boolean append(Object object, boolean isFirst, StringBuilder builder) {
			if (object == null) {
				return false;
			}
			if (!isFirst) {
				builder.append(" ");
			}
			builder.append("[ ");
			builder.append(object);
			builder.append(" ]");
			return true;
		}

		@Override
		public String toString() {
			if (printString == null) {
				boolean isFirst = true;

				StringBuilder builder = new StringBuilder();

				boolean didAdd = append(level, isFirst, builder);
				if (didAdd) {
					isFirst = false;
				}
				didAdd = append(marker, isFirst, builder);
				if (didAdd) {
					isFirst = false;
				}
				didAdd = append(threadName, isFirst, builder);
				if (didAdd) {
					isFirst = false;
				}
				append(Long.valueOf(timeStamp), isFirst, builder);
				isFirst = false;

				append(message, isFirst, builder);
				if (parms != null) {
					for (String parm : parms) {
						append(parm, isFirst, builder);
					}
				}

				append(thrownMessage, isFirst, builder);

				printString = builder.toString();
			}

			return printString;
		}

		public String toStringFormatted() {
			boolean isFirst = true;

			StringBuilder builder = new StringBuilder();

			boolean didAdd = append(level, isFirst, builder);
			if (didAdd) {
				isFirst = false;
			}
			didAdd = append(marker, isFirst, builder);
			if (didAdd) {
				isFirst = false;
			}
			didAdd = append(threadName, isFirst, builder);
			if (didAdd) {
				isFirst = false;
			}
			append(Long.valueOf(timeStamp), isFirst, builder);
			isFirst = false;

			String useMessage;
			if ((parms == null) || (parms.length == 0)) {
				useMessage = message;
			} else {
				useMessage = message.replace("{}", "%s");
				useMessage = String.format(useMessage, (Object[]) parms);
			}

			append(useMessage, isFirst, builder);
			append(thrownMessage, isFirst, builder);

			return builder.toString();
		}

		public LogEvent(Level level, Marker marker, Throwable th, String message, Object... rawParms) {
			this.level = level;
			this.marker = marker;

			this.threadName = Thread.currentThread()
				.getName();
			this.timeStamp = System.nanoTime();

			this.message = message;

			if ((rawParms == null) || (rawParms.length == 0)) {
				parms = EMPTY_STRINGS;
			} else {
				String[] useParms = new String[rawParms.length];
				for (int parmNo = 0; parmNo < rawParms.length; parmNo++) {
					Object nextParm = rawParms[parmNo];
					useParms[parmNo] = ((nextParm == null) ? null : nextParm.toString());
				}
				this.parms = useParms;
			}

			this.thrownMessage = ((th == null) ? null : th.getMessage());
		}
	}

	private final List<LogEvent> capturedEvents;

	public List<LogEvent> getCapturedEvents() {
		return capturedEvents;
	}

	protected void addEvent(LogEvent logEvent) {
		capturedEvents.add(logEvent);
	}

	public List<LogEvent> consumeCapturedEvents() {
		List<LogEvent> events = new ArrayList<>(capturedEvents);
		capturedEvents.clear();
		return events;
	}

	public int getCaptureEventCount() {
		return getCapturedEvents().size();
	}

	public LogEvent getCapturedEvent(int eventNo) {
		return getCapturedEvents().get(eventNo);
	}

	//

	@Override
	public boolean isTraceEnabled() {
		return getBaseLogger().isTraceEnabled();
	}

	@Override
	public void trace(String msg) {
		capture(Level.TRACE, msg);
		getBaseLogger().trace(msg);
	}

	@Override
	public void trace(String format, Object arg) {
		capture(Level.TRACE, format, arg);
		getBaseLogger().trace(format, arg);
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		capture(Level.TRACE, format, arg1, arg2);
		getBaseLogger().trace(format, arg1, arg2);
	}

	@Override
	public void trace(String format, Object... args) {
		capture(Level.TRACE, format, args);
		getBaseLogger().trace(format, args);
	}

	@Override
	public void trace(String msg, Throwable t) {
		capture(Level.TRACE, null, t, msg, (Object[]) null);
		getBaseLogger().trace(msg, t);
	}

	@Override
	public boolean isDebugEnabled() {
		return getBaseLogger().isDebugEnabled();
	}

	@Override
	public void debug(String msg) {
		capture(Level.DEBUG, msg);
		getBaseLogger().debug(msg);
	}

	@Override
	public void debug(String format, Object arg) {
		capture(Level.DEBUG, format, arg);
		getBaseLogger().debug(format, arg);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		capture(Level.DEBUG, format, arg1, arg2);
		getBaseLogger().debug(format, arg1, arg2);
	}

	@Override
	public void debug(String format, Object... args) {
		capture(Level.DEBUG, format, args);
		getBaseLogger().debug(format, args);
	}

	@Override
	public void debug(String msg, Throwable t) {
		capture(Level.DEBUG, null, t, msg, (Object[]) null);
		getBaseLogger().debug(msg, t);
	}

	@Override
	public boolean isInfoEnabled() {
		return getBaseLogger().isInfoEnabled();
	}

	@Override
	public void info(String msg) {
		capture(Level.INFO, msg);
		getBaseLogger().info(msg);
	}

	@Override
	public void info(String format, Object arg) {
		capture(Level.INFO, format, arg);
		getBaseLogger().info(format, arg);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		capture(Level.INFO, format, arg1, arg2);
		getBaseLogger().info(format, arg1, arg2);
	}

	@Override
	public void info(String format, Object... args) {
		capture(Level.INFO, format, args);
		getBaseLogger().info(format, args);
	}

	@Override
	public void info(String msg, Throwable t) {
		capture(Level.INFO, null, t, msg, (Object[]) null);
		getBaseLogger().info(msg, t);
	}

	@Override
	public boolean isWarnEnabled() {
		return getBaseLogger().isWarnEnabled();
	}

	@Override
	public void warn(String msg) {
		capture(Level.WARN, msg);
		getBaseLogger().warn(msg);
	}

	@Override
	public void warn(String format, Object arg) {
		capture(Level.WARN, format, arg);
		getBaseLogger().warn(format, arg);
	}

	@Override
	public void warn(String format, Object... args) {
		capture(Level.WARN, format, args);
		getBaseLogger().warn(format, args);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		capture(Level.WARN, format, arg1, arg2);
		getBaseLogger().warn(format, arg1, arg2);
	}

	@Override
	public void warn(String msg, Throwable t) {
		capture(Level.WARN, null, t, msg, (Object[]) null);
		getBaseLogger().warn(msg, t);
	}

	@Override
	public boolean isErrorEnabled() {
		return getBaseLogger().isErrorEnabled();
	}

	@Override
	public void error(String msg) {
		capture(Level.ERROR, msg);
		getBaseLogger().error(msg);
	}

	@Override
	public void error(String format, Object arg) {
		capture(Level.ERROR, format, arg);
		getBaseLogger().error(format, arg);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		capture(Level.ERROR, format, arg1, arg2);
		getBaseLogger().error(format, arg1, arg2);
	}

	@Override
	public void error(String format, Object... args) {
		capture(Level.ERROR, format, args);
		getBaseLogger().error(format, args);
	}

	@Override
	public void error(String msg, Throwable t) {
		capture(Level.ERROR, null, t, msg, (Object[]) null);
		getBaseLogger().error(msg, t);
	}

	//

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return getBaseLogger().isTraceEnabled(marker);
	}

	@Override
	public void trace(Marker marker, String msg) {
		capture(Level.TRACE, marker, msg);
		getBaseLogger().trace(marker, msg);
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		capture(Level.TRACE, marker, format, arg);
		getBaseLogger().trace(marker, format, arg);
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		capture(Level.TRACE, marker, format, arg1, arg2);
		getBaseLogger().trace(marker, format, arg1, arg2);
	}

	@Override
	public void trace(Marker marker, String format, Object... args) {
		capture(Level.TRACE, marker, format, args);
		getBaseLogger().trace(marker, format, args);
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		capture(Level.TRACE, marker, t, msg);
		getBaseLogger().trace(marker, msg, t);
	}

	//

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return getBaseLogger().isDebugEnabled(marker);
	}

	@Override
	public void debug(Marker marker, String msg) {
		capture(Level.DEBUG, marker, msg);
		getBaseLogger().debug(marker, msg);
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		capture(Level.DEBUG, marker, format, arg);
		getBaseLogger().debug(marker, format, arg);
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		capture(Level.DEBUG, marker, format, arg1, arg2);
		getBaseLogger().debug(marker, format, arg1, arg2);
	}

	@Override
	public void debug(Marker marker, String format, Object... args) {
		capture(Level.DEBUG, marker, format, args);
		getBaseLogger().debug(marker, format, args);
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		capture(Level.DEBUG, marker, t, msg);
		getBaseLogger().debug(marker, msg, t);
	}

	//

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return getBaseLogger().isInfoEnabled(marker);
	}

	@Override
	public void info(Marker marker, String msg) {
		capture(Level.INFO, marker, msg);
		getBaseLogger().info(marker, msg);
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		capture(Level.INFO, marker, format, arg);
		getBaseLogger().info(marker, format, arg);
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		capture(Level.INFO, marker, format, arg1, arg2);
		getBaseLogger().info(marker, format, arg1, arg2);
	}

	@Override
	public void info(Marker marker, String format, Object... args) {
		capture(Level.INFO, marker, format, args);
		getBaseLogger().info(marker, format, args);
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		capture(Level.INFO, marker, t, msg);
		getBaseLogger().info(marker, msg, t);
	}

	//

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return getBaseLogger().isWarnEnabled(marker);
	}

	@Override
	public void warn(Marker marker, String msg) {
		capture(Level.WARN, marker, msg);
		getBaseLogger().warn(marker, msg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		capture(Level.WARN, marker, format, arg);
		getBaseLogger().warn(marker, format, arg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		capture(Level.WARN, marker, format, arg1, arg2);
		getBaseLogger().warn(marker, format, arg1, arg2);
	}

	@Override
	public void warn(Marker marker, String format, Object... args) {
		capture(Level.WARN, marker, format, args);
		getBaseLogger().warn(marker, format, args);
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		capture(Level.WARN, marker, t, msg);
		getBaseLogger().warn(marker, msg, t);
	}

	//

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return getBaseLogger().isErrorEnabled(marker);
	}

	@Override
	public void error(Marker marker, String msg) {
		capture(Level.ERROR, marker, msg);
		getBaseLogger().error(marker, msg);
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		capture(Level.ERROR, marker, format, arg);
		getBaseLogger().error(marker, format, arg);
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		capture(Level.ERROR, marker, format, arg1, arg2);
		getBaseLogger().error(marker, format, arg1, arg2);
	}

	@Override
	public void error(Marker marker, String format, Object... args) {
		capture(Level.ERROR, marker, format, args);
		getBaseLogger().error(marker, format, args);
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		capture(Level.ERROR, marker, t, msg);
		getBaseLogger().error(marker, msg, t);
	}
}
