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

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.SubstituteLogger;

/**
 * The Logger wraps a real delegate Logger and will also send output to system
 * out/err if the console marker is supplied.
 */
class DualLogger extends SubstituteLogger {

	private final BiConsumer<String, Throwable>	systemOut;
	private final BiConsumer<String, Throwable>	systemErr;

	DualLogger(Logger delegate, BiConsumer<String, Throwable> systemOut, BiConsumer<String, Throwable> systemErr) {
		super(delegate.getName(), null, true);
		setDelegate(delegate);
		this.systemOut = requireNonNull(systemOut);
		this.systemErr = requireNonNull(systemErr);
	}

	private boolean isConsoleMarker(Marker marker) {
		return Objects.nonNull(marker) && marker.contains("console");
	}

	@Override
	public void debug(Marker marker, String msg) {
		if (isDebugEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				systemOut.accept(msg, null);
			}
			super.debug(marker, msg);
		}
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		if (isDebugEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arg);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.debug(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.debug(marker, format, arg);
			}
		}
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		if (isDebugEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.debug(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.debug(marker, format, arg1, arg2);
			}
		}
	}

	@Override
	public void debug(Marker marker, String format, Object... arguments) {
		if (isDebugEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arguments);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.debug(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.debug(marker, format, arguments);
			}
		}
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		if (isDebugEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				systemOut.accept(msg, t);
			}
			super.debug(marker, msg, t);
		}
	}

	@Override
	public void info(Marker marker, String msg) {
		if (isConsoleMarker(marker)) {
			systemOut.accept(msg, null);
		}
		super.info(marker, msg);
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg);
			systemOut.accept(tp.getMessage(), tp.getThrowable());
			super.info(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.info(marker, format, arg);
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			systemOut.accept(tp.getMessage(), tp.getThrowable());
			super.info(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.info(marker, format, arg1, arg2);
	}

	@Override
	public void info(Marker marker, String format, Object... arguments) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arguments);
			systemOut.accept(tp.getMessage(), tp.getThrowable());
			super.info(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.info(marker, format, arguments);
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		if (isConsoleMarker(marker)) {
			systemOut.accept(msg, t);
		}
		super.info(marker, msg, t);
	}

	@Override
	public void error(Marker marker, String msg) {
		if (isConsoleMarker(marker)) {
			systemErr.accept(msg, null);
		}
		super.error(marker, msg);
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.error(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.error(marker, format, arg);
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.error(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.error(marker, format, arg1, arg2);
	}

	@Override
	public void error(Marker marker, String format, Object... arguments) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arguments);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.error(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.error(marker, format, arguments);
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		if (isConsoleMarker(marker)) {
			systemErr.accept(msg, t);
		}
		super.error(marker, msg, t);
	}

	@Override
	public void trace(Marker marker, String msg) {
		if (isTraceEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				systemOut.accept(msg, null);
			}
			super.trace(marker, msg);
		}
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		if (isTraceEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arg);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.trace(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.trace(marker, format, arg);
			}
		}
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		if (isTraceEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.trace(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.trace(marker, format, arg1, arg2);
			}
		}
	}

	@Override
	public void trace(Marker marker, String format, Object... arguments) {
		if (isTraceEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				FormattingTuple tp = MessageFormatter.format(format, arguments);
				systemOut.accept(tp.getMessage(), tp.getThrowable());
				super.trace(marker, tp.getMessage(), tp.getThrowable());
			} else {
				super.trace(marker, format, arguments);
			}
		}
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		if (isTraceEnabled(marker)) {
			if (isConsoleMarker(marker)) {
				systemOut.accept(msg, t);
			}
			super.trace(marker, msg, t);
		}
	}

	@Override
	public void warn(Marker marker, String msg) {
		if (isConsoleMarker(marker)) {
			systemErr.accept(msg, null);
		}
		super.warn(marker, msg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.warn(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.warn(marker, format, arg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.warn(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.warn(marker, format, arg1, arg2);
	}

	@Override
	public void warn(Marker marker, String format, Object... arguments) {
		if (isConsoleMarker(marker)) {
			FormattingTuple tp = MessageFormatter.format(format, arguments);
			systemErr.accept(tp.getMessage(), tp.getThrowable());
			super.warn(marker, tp.getMessage(), tp.getThrowable());
			return;
		}
		super.warn(marker, format, arguments);
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		if (isConsoleMarker(marker)) {
			systemErr.accept(msg, t);
		}
		super.warn(marker, msg, t);
	}
}
