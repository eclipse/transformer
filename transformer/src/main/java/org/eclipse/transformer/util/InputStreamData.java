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

package org.eclipse.transformer.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class InputStreamData {
	/** A name associated with the input stream. */
	public final String name;

	/** A stream containing input data. */
	public final InputStream stream;

	/**
	 * Control parameter: Indicates that the number of bytes which are
	 * available in an input stream is unknown.
	 */
	public static final int UNKNOWN_LENGTH = -1;

	/**
	 * The count of bytes available in the input stream.
	 * will be {@link #UNKNOWN_LENGTH} if the number of
	 * available bytes is not known.
	 */
	public final int length;

	/**
	 * Create input data for a name stream.
	 *
	 * @param name A name associated with the data.
	 * @param stream An stream containing the input data.
	 * @param length The number of bytes available in the
	 *     stream.  Possibly {@link #UNKNOWN_LENGTH}.
	 */
	public InputStreamData(String name, InputStream stream, int length) {
		this.name = name;
		this.stream = stream;
		this.length = length;
	}

	/**
	 * Create input data from byte data.
	 *
	 * The input data is almost a direct conversion of the byte data,
	 * except that the bytes of the byte data are converted to a
	 * byte array based input stream.
	 *
	 * @param byteData Byte data from which to create input data.
	 */
	public InputStreamData(ByteData byteData) {
		this.name = byteData.name;
		this.stream = new ByteArrayInputStream( byteData.data, byteData.offset, byteData.length );
		this.length = byteData.length;
	}
}
