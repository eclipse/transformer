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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.IO;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ByteDataImpl;
import org.slf4j.Logger;

public class FileUtils {
	private FileUtils() {}

	/** Usual disk page size. */
	public static final int	PAGE_SIZE			= 4096;

	/** Size for allocating read buffers. */
	public static final int	BUFFER_ADJUSTMENT	= PAGE_SIZE * 16;

	/** Maximum array size. Adjusted per ByteArrayInputStream comments. */
	public static final int	MAX_ARRAY_LENGTH	= Integer.MAX_VALUE - 8;

	/**
	 * Default Charset to use when the selection rules do not specify a
	 * charset for a resource.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Verify that an integer value is usable as an array size.
	 *
	 * @param size A requested array size.
	 * @throws IllegalArgumentException Thrown if the requested size is less
	 *             than zero or greater than {@link #MAX_ARRAY_LENGTH}.
	 */
	public static void verifyArraySize(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("Array fill amount [ " + size + " ] less than zero [ 0 ]");
		} else if (size > MAX_ARRAY_LENGTH) {
			throw new IllegalArgumentException(
				"Array fill amount [ " + size + " ] greater than [ " + MAX_ARRAY_LENGTH + " ]");
		}
	}

	/**
	 * Verify that an array offset is usable.
	 *
	 * @param offset The array offset which is to be verified.
	 * @param size The array size which is used to verify the array offset.
	 * @throws IllegalArgumentException Thrown if the offset is less than zero
	 *             or greater than {@link #MAX_ARRAY_LENGTH}, or greater than
	 *             the size parameter. The size parameter is not validated.
	 */
	public static void verifyArrayOffset(int offset, int size) {
		if (offset < 0) {
			throw new IllegalArgumentException("Array offset [ " + offset + " ] less than zero.");
		} else if (offset > MAX_ARRAY_LENGTH) {
			throw new IllegalArgumentException(
				"Array offset [ " + offset + " ] greater than [ " + MAX_ARRAY_LENGTH + " ]");
		} else if (offset > size) {
			throw new IllegalArgumentException(
				"Array offset [ " + offset + " ] greater than array size [ " + size + " ]");
		}
	}

	/**
	 * Verify that array parameters are usable. See
	 * {@link #verifyArraySize(int)} and {@link #verifyArrayOffset(int, int)}.
	 *
	 * @param offset A starting offset into the target array.
	 * @param size A count of bytes to add to the target array.
	 * @throws IllegalArgumentException Thrown if either parameter is not
	 *             usable.
	 */
	public static void verifyArray(int offset, int size) {
		verifyArraySize(size);
		verifyArrayOffset(offset, size);
	}

	/**
	 * Read data from an input stream. Use an possibly update the input buffer.
	 * See {@link #read(String, InputStream, ByteBuffer)}.
	 *
	 * @param logger A logger for debugging output.
	 * @param inputName The name of the input stream.
	 * @param inputStream A stream to be read.
	 * @param requested The count of bytes to read from the stream. If less than
	 *            zero, all available bytes will be read.
	 * @return Byte data from the read.
	 * @throws IOException Thrown if a read fails, or if overflow or underflow
	 *             occurs.
	 */
	public static ByteData read(Logger logger, String inputName, Charset charset, InputStream inputStream, int requested)
		throws IOException {

		logger.debug("Reading [ {} ] bytes [ {} ]", inputName, requested);

		ByteBuffer initialBuffer = ByteBuffer.allocate((requested < 0) ? BUFFER_ADJUSTMENT : requested);

		ByteBuffer finalBuffer = read(inputName, inputStream, initialBuffer, requested);

		if ( requested < 0 ) {
			logger.debug("Read [ {} ] bytes [ {} ]", inputName, finalBuffer.limit());
		}

		return new ByteDataImpl(inputName, finalBuffer, charset);
	}

	/**
	 * Read bytes from an input stream.
	 * <p>
	 * Place the read bytes in the byte buffer.  Allocate a new buffer if the initial buffer
	 * is too small.
	 * <p>
	 * If the requested count of bytes is less than zero, read all available bytes from the input stream.  Otherwise, read the requested number of bytes.
	 * <p>
	 * Exceptions are thrown if a read fails, or if too few bytes were read, or if an array could not
	 * be allocated large enough for all bytes which were to be read.
	 * <p>
	 * An exception is thrown if the requested number of bytes is too large.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream A stream to be read.
	 * @param outputBuffer A buffer to be written.
	 * @param requested The count of bytes to read from the stream. If less than
	 *            zero, all available bytes will be read.
	 * @return Byte data from the read.  The original byte buffer, if it had enough capacity.
	 *     Otherwise, a newly allocated byte buffer.
	 * @throws IOException Thrown if a read fails, or if overflow or underflow
	 *             occurs.
	 */
	public static ByteBuffer read(String inputName, InputStream inputStream, ByteBuffer outputBuffer, int requested)
		throws IOException {

		if ( requested == 0 ) {
			// Indicate that the buffer is empty.
			outputBuffer.position(0);
			outputBuffer.limit(0);

			// Return the same buffer as was input.  The store of buffers
			// does not need to be updated with a new buffer.
			return outputBuffer;
		}

		// Keep reading until all requested bytes are read.

		// 'requested > 0' means a specific count was requested.
		// In which case, the buffer can be pre-allocated to the requested
		// size, and the read can proceed without additional allocations.

		// 'requested < 0' means we don't know how many bytes to read.
		// In which case, read until exceeding the maximum array size
		// or until reaching the end of data.  The buffer is reallocated
		// as needed.

		// We intend to read starting at position 0 of the output buffer.
		// The entire capacity of the output buffer is available.
		int outputCapacity = outputBuffer.capacity();

		int read = 0;

		if ( requested > 0 ) {
			verifyArraySize(requested);
			if ( requested > outputCapacity ) {
				// 'allocate' sets the position to zero, but also sets the limit to
				// the capacity.  We will correct that before returning the buffer.
				outputBuffer = ByteBuffer.allocate(requested);
				outputCapacity = requested;
			}

			int unread = requested;

			// Our write into the buffer's array starts at the beginning
			// of the array, which is position 0.  The output buffer's position
			// will need to be updated to 0.

			byte[] buf = outputBuffer.array();
			int bytesRead;
			while ( (unread > 0) && ((bytesRead = inputStream.read(buf, read, unread)) != -1) ) {
				read += bytesRead;
				unread -= bytesRead;
			}
			if ( unread > 0 ) {
				throw new IOException(
					"Underflow of [ " + inputName + " ].  Read [ " + read + " ] bytes, expected [ " + requested + " ].");
			}

		} else { // requested < 0
			// Our write into the buffer's array starts at the beginning
			// of the array, which is position 0.  The output buffer's position
			// will need to be updated to 0.

			byte[] buf = outputBuffer.array();
			int unused = outputCapacity;

			int bytesRead;
			while ( (bytesRead = inputStream.read(buf, read, unused)) != -1 ) {
				read += bytesRead;
				unused -= bytesRead;

				if ( inputStream.available() == 0 ) {
					break;
				}

				if ( unused != 0 ) {
					// Unusual: There are available bytes, and we had space for them,
					// but we didn't read them.
					continue;
				}

				// Limit the adjustment to the maximum array length.
				// We are very unlikely to reach this limit, but check to be safe.
				// The operations here are arranged to avoid integer overflow.
				if ( read == MAX_ARRAY_LENGTH ) {
					throw new IOException(
						"Overflow of [ " + inputName + " ].  Read [ " + read + " ] bytes (MAX_ARRAY_LENGTH).");
				}
				int maxAdjustment = MAX_ARRAY_LENGTH - read;
				int adjustment = ( BUFFER_ADJUSTMENT > maxAdjustment ) ? maxAdjustment : BUFFER_ADJUSTMENT;

				// 'allocate' sets the position to zero, but also sets the limit to
				// the capacity.  We will correct that before returning the buffer.
				outputBuffer = ByteBuffer.allocate(outputCapacity += adjustment);
				unused = adjustment; // 'unused' must initially be zero.

				byte[] oldBuf = buf;
				buf = outputBuffer.array();
				System.arraycopy(oldBuf, 0, buf, 0, read);
			}
		}

		// The write began at the beginning of the output buffer.
		outputBuffer.position(0);
		// 'allocate' set the limit to the buffer's capacity.  Correct this
		// to the actual amount read.
		outputBuffer.limit(read);
		return outputBuffer;
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

	//

	public static InputStream stream(ByteBuffer buffer) {
		InputStream stream = IO.stream(buffer);
		return stream;
	}

	public static Reader reader(ByteBuffer buffer, Charset charset) {
		Reader reader = new InputStreamReader(new ByteBufferInputStream(buffer), charset);
		return reader;
	}

	public static BufferedWriter writer(OutputStream outputStream, Charset charset) {
		OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, charset);
		BufferedWriter writer = new BufferedWriter(outputWriter);
		return writer;
	}

	//

	private final static Pattern PATH_SPLITTER = Pattern.compile("[/\\\\]");

	/**
	 * Sanitize an entry name. This avoids ZipSlip, and possibly other problems.
	 *
	 * @param entryName The path which is to be sanitized.
	 * @return The sanitized path.
	 */
	public static String sanitize(String entryName) {
		if (entryName.isEmpty()) {
			return entryName;
		}

		String normalized;
		try {
			normalized = Paths.get("", PATH_SPLITTER.split(entryName))
				.normalize() // Remove redundant path elements.
				.toString()
				.replace('\\', '/');
		} catch (InvalidPathException e) {
			throw new TransformException("Zip entry name is not a valid path [ " + entryName + " ]", e);
		}

		if (normalized.startsWith("..") &&
			((normalized.length() == 2) || (normalized.charAt(2) == '/'))) {
			throw new TransformException("[ZIPSLIP]: Zip entry name has leading \"..\" [ " + entryName + " ]");
		}

		// Put back a trailing slash which was removed by normalize.
		char lastChar = entryName.charAt(entryName.length() - 1);
		if ((lastChar == '/') || (lastChar == '\\')) {
			normalized = normalized.concat("/");
		}

		return normalized;
	}
}
