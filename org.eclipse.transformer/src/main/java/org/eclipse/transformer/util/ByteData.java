/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import aQute.lib.io.ByteBufferInputStream;

public class ByteData {
	public final String	name;
	public final byte[]	data;
	public final int	offset;
	public final int	length;

	public ByteData(String name, byte[] data) {
		this(name, data, 0, data.length);
	}

	public ByteData(String name, byte[] data, int offset, int length) {
		// System.out.println("ByteData [ " + name + " ] [ " + offset + " ] [ "
		// + length + " ] [ " + data + " ]");

		this.name = name;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public InputStream asStream() {
		return new ByteBufferInputStream(data, offset, length);
	}

	public void write(OutputStream outputStream) throws IOException {
		outputStream.write(data, offset, length); // throws IOException
	}
}
