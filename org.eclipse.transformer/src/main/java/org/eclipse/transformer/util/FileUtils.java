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

package org.eclipse.transformer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FileUtils {
	private FileUtils() {}

	/** Usual disk page size. */
	public static final int	PAGE_SIZE			= 4096;

	/** Size for allocating read buffers. */
	public static final int	BUFFER_ADJUSTMENT	= PAGE_SIZE * 16;

	/** Maximum array size. Adjusted per ByteArrayInputStream comments. */
	public static final int	MAX_ARRAY_LENGTH	= Integer.MAX_VALUE - 8;

	/**
	 * Verify that array parameters are usable. Throw an illegal argument
	 * exception if the parameters are not usable. The sum of the offset and
	 * count must be no larger than the maximum array lemgth,
	 * {@link #MAX_ARRAY_LENGTH}.
	 *
	 * @param offset A starting offset into the target array.
	 * @param count A count of bytes to add to the target array.
	 * @return The count value as an int value.
	 */
	public static int verifyArray(int offset, long count) {
		if (offset < 0) {
			throw new IllegalArgumentException("Array offset [ " + offset + " ] less than zero.");
		} else if (offset > MAX_ARRAY_LENGTH) {
			throw new IllegalArgumentException(
				"Array offset [ " + offset + " ] greater than [ " + MAX_ARRAY_LENGTH + " ]");
		}

		if (count < 0) {
			throw new IllegalArgumentException("Array fill amount [ " + count + " ] less than zero [ 0 ]");
		} else if (count > MAX_ARRAY_LENGTH) {
			throw new IllegalArgumentException(
				"Array fill amount [ " + count + " ] greater than [ " + MAX_ARRAY_LENGTH + " ]");
		}

		int intCount = (int) count;

		int maxCount = MAX_ARRAY_LENGTH - offset;
		if (intCount > maxCount) {
			throw new IllegalArgumentException("Array length [ " + maxCount + " ] from offset [ " + offset
				+ " ] and fill amount [ " + intCount + " ] greater than [ " + MAX_ARRAY_LENGTH + " ]");
		}

		// System.out.println("Count [ " + count + " ] Adjusted to [ " +
		// intCount + " ]");

		return intCount;
	}

	/**
	 * Read data from an input stream into a buffer. Allocate a new buffer if
	 * the parameter buffer is too small for the requested read.
	 *
	 * @param inputName A name associated with the input stram.
	 * @param inputStream The stream from which to read bytes.
	 * @param buffer A buffer into which to place the read bytes.
	 * @param count The number of bytes to read.
	 * @return The final buffer which contains the read bytes. The same as the
	 *         parameter buffer if that buffer had a sufficient capacity to read
	 *         the requested count of bytes. A new buffer if the parameter
	 *         buffer was too small.
	 * @throws IOException Thrown if an error occurred during a read.
	 */
	public static ByteBuffer read(String inputName, InputStream inputStream, ByteBuffer buffer, int count)
		throws IOException {
		if (count != -1) {
			verifyArray(0, count);
			if (count > buffer.capacity()) {
				buffer = ByteBuffer.allocate(count);
			}
		}
		buffer.clear();
		for (int bytesRead; (bytesRead = inputStream.read(buffer.array(), buffer.arrayOffset() + buffer.position(),
			buffer.remaining())) != -1;) {
			buffer.position(buffer.position() + bytesRead);
			if (!buffer.hasRemaining()) {
				if (buffer.position() == count) {
					break; // don't grow, we have all the data
				}
				if (buffer.capacity() >= MAX_ARRAY_LENGTH) {
					if (inputStream.read() == -1) {
						break;
					}
					throw new IOException(
						"Overflow of [ " + inputName + " ] after reading [ " + buffer.position() + " ] bytes");
				}
				int capacity = Math.min(MAX_ARRAY_LENGTH, buffer.capacity() + BUFFER_ADJUSTMENT);
				buffer.flip();
				buffer = ByteBuffer.allocate(capacity)
					.put(buffer);
			}
		}
		buffer.flip();
		return buffer;
	}

	public static ByteBuffer read(String inputName, InputStream inputStream, ByteBuffer buffer) throws IOException {
		return read(inputName, inputStream, buffer, -1);
	}

	public static ByteBuffer read(String inputName, InputStream inputStream) throws IOException {
		return read(inputName, inputStream, ByteBuffer.allocate(BUFFER_ADJUSTMENT));
	}

	public static long transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[BUFFER_ADJUSTMENT];

		return transfer(inputStream, outputStream, buffer);
	}

	public static long transfer(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
		long totalBytesRead = 0L;

		for (int bytesRead = 0; (bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1;) {
			totalBytesRead += bytesRead;
			outputStream.write(buffer, 0, bytesRead);
		}

		return totalBytesRead;
	}

	private static final char SLASH = '/';

	public static String getFileNameFromFullyQualifiedFileName(String fqFileName) {
		int index = fqFileName.lastIndexOf(SLASH);
		if (index != -1) {
			return fqFileName.substring(index + 1);
		}
		return fqFileName;
	}
}
