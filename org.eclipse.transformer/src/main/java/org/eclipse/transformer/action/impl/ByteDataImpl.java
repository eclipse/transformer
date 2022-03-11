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

package org.eclipse.transformer.action.impl;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.eclipse.transformer.action.ByteData;

import aQute.lib.io.ByteBufferInputStream;

public class ByteDataImpl implements ByteData {
	/** A name associated with the input stream. */
	private final String		name;

	/** A stream containing input data. */
	private final ByteBuffer	buffer;

	/**
	 * Create input data for a name stream.
	 *
	 * @param name A name associated with the data.
	 * @param buffer
	 */
	public ByteDataImpl(String name, ByteBuffer buffer) {
		this.name = name;
		this.buffer = buffer;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public InputStream stream() {
		return new ByteBufferInputStream(buffer());
	}

	@Override
	public int length() {
		return buffer.remaining();
	}

	@Override
	public ByteBuffer buffer() {
		return buffer.duplicate();
	}
}
