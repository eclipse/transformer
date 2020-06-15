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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import aQute.lib.utf8properties.UTF8Properties;

public class FileUtils {

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
	 * Read data from an input stream. Answer the data as a byte array sized
	 * exactly to the read amount.
	 *
	 * @param inputName A name associated with the input stram.
	 * @param inputStream The stream from which to read data.
	 * @param count The count of bytes to read.
	 * @return A byte array containing the read bytes.
	 * @throws IOException Thrown if the read failed.
	 */
	public static ByteData read(String inputName, InputStream inputStream, int count) throws IOException {
		return read(inputName, inputStream, null, count); // throws IOException
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
	public static ByteData read(String inputName, InputStream inputStream, byte[] buffer, int count)
		throws IOException {

		if (count == -1) {
			return read(inputName, inputStream, new byte[BUFFER_ADJUSTMENT]);

		} else {
			count = verifyArray(0, count);

			if ((buffer == null) || (count > buffer.length)) {
				buffer = new byte[count];
			}

			int offset = 0;
			int remaining = count;

			while (remaining > 0) {
				int actual = inputStream.read(buffer, offset, remaining); // throws
																			// IOException
				if (actual == -1) {
					throw new IOException("Premature end-of-stream [ " + inputName + " at [ " + offset
						+ " ] requested [ " + remaining + " ]");
				}

				// System.out.println("Read requested [ " + inputName + " ] [ "
				// + remaining + " ] actual [ " + actual + " ]");
				offset += actual;
				remaining -= actual;
			}

			return new ByteData(inputName, buffer, 0, count);
		}
	}

	//

	public static ByteData read(String inputName, InputStream inputStream) throws IOException {
		return read(inputName, inputStream, new byte[BUFFER_ADJUSTMENT]);
	}

	public static ByteData read(String inputName, InputStream inputStream, byte[] buffer) throws IOException {
		int bytesUsed = 0;
		int bytesRemaining = buffer.length;

		int bytesRead;
		while ((bytesRead = inputStream.read(buffer, bytesUsed, bytesRemaining)) != -1) { // throws
																							// IOEXception
			bytesUsed += bytesRead;
			bytesRemaining -= bytesRead;

			if (bytesRemaining == 0) {
				int bytesAdded = MAX_ARRAY_LENGTH - bytesUsed;
				if (bytesAdded == 0) {
					if (inputStream.read() == -1) {
						break;
					} else {
						throw new IOException(
							"Overflow of [ " + inputName + " ] after reading [ " + bytesUsed + " ] bytes");
					}
				} else if (bytesAdded > BUFFER_ADJUSTMENT) {
					bytesAdded = BUFFER_ADJUSTMENT;
				}

				int nextLength = bytesUsed + BUFFER_ADJUSTMENT;
				bytesRemaining = BUFFER_ADJUSTMENT;

				byte[] nextBuffer = new byte[nextLength];
				System.arraycopy(buffer, 0, nextBuffer, 0, bytesUsed);
				buffer = nextBuffer;
			}
		}

		return new ByteData(inputName, buffer, 0, bytesUsed);
	}

	public static long transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[BUFFER_ADJUSTMENT];

		return transfer(inputStream, outputStream, buffer);
	}

	public static long transfer(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
		long totalBytesRead = 0L;

		int bytesRead = 0;
		while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) { // throws
																					// IOEXception
			totalBytesRead += bytesRead;
			outputStream.write(buffer, 0, bytesRead);
		}

		return totalBytesRead;
	}

	//

	public static final char SLASH = '/';

	public static String normalize(String path) {
		if (File.separatorChar == '\\') {
			return path.replace(File.separatorChar, SLASH);
		} else {
			return path;
		}
	}

	//

	public static UTF8Properties loadProperties(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			UTF8Properties properties = createProperties();
			properties.load(stream);
			return properties;
		}
	}

	public static UTF8Properties createProperties() {
		return new UTF8Properties();
	}

	public static String getFileNameFromFullyQualifiedFileName(String fqFileName) {
		int index = fqFileName.lastIndexOf(SLASH);
		if (index != -1) {
			return fqFileName.substring(index + 1);
		}
		return fqFileName;
	}
}
