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

package org.eclipse.transformer.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A byte data buffer.
 * <p>
 * Storage is using a {@link ByteBuffer}, with an attached name.
 * <p>
 * No access through this byte data buffer will update read state of the
 * underlying buffer. All operations start by copying the buffer. (Copying a
 * byte buffer copies its read state while sharing the underlying byte data. No
 * underlying data is copied.)
 * <p>
 * Absent independent modifications to the underlying byte buffer, this data
 * structure uses the initial state of the byte buffer to define what data is
 * available, and call to retrieve the data (using {@link #buffer()} will obtain
 * a copy of that initial data.
 */
public interface ByteData {
	/**
	 * The name associated with the data.
	 *
	 * @return The name associated with the data.
	 */
	String name();

	/**
	 * Answer a copy of the buffer associated with this data.
	 * <p>
	 * The contents of the copied buffer are shared with the buffer of this
	 * data. The settings of the copy buffer (length, limit, and remaining)
	 * start the same as the settings of the buffer of this data, but are
	 * updated independently.
	 * <p>
	 * For a buffer which has its position set to zero, use instead
	 * {@link #buffer()}.
	 *
	 * @return The buffer associated with the data.
	 */
	ByteBuffer buffer();

	/**
	 * Tell how many bytes are available to be read from the buffer.
	 *
	 * @return The number of bytes available to be read from the buffer.
	 */
	int length();

	//

	/**
	 * Return an input stream over this buffer.
	 *
	 * @return An input stream over this buffer.
	 */
	InputStream stream();

	/**
	 * Answer a reader over this buffer.
	 * The reader uses the charset returned by {@link #charset()}.
	 * @return A reader on this buffer.
	 */
	BufferedReader reader();

	/**
	 * Copy this byte data to an output stream.
	 *
	 * @param outputStream The stream that is to receive the byte data.
	 * @return The output stream.
	 * @throws IOException Thrown if the stream data cannot be copied.
	 */
	OutputStream writeTo(OutputStream outputStream) throws IOException;

	//

	/**
	 * Copy factory method. Create a copy of byte data, including the name. Copy
	 * the underlying buffer while preserving its settings, and sharing its
	 * contents.
	 *
	 * @return The copied byte data.
	 */
	ByteData copy();

	/**
	 * Copy factory method. Copy the underlying buffer while preserving its
	 * settings, and sharing its contents. Assign the specified name to the new
	 * byte data.
	 *
	 * @param name The name to assign to the new data.
	 * @return The copied byte data.
	 */
	ByteData copy(String name);
}
