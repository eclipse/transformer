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
import java.io.IOException;
import java.io.OutputStream;

public class ByteData {
	public final String name;
	public final byte[] data;
	public final int offset;
	public final int length;

	public ByteData(String name, byte[] data) {
		this(name, data, 0, data.length);
	}

	public ByteData(String name, byte[] data, int offset, int length) {
		// System.out.println("ByteData [ " + name + " ] [ " + offset + " ] [ " + length + " ] [ " + data + " ]");

		this.name = name;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public ByteArrayInputStream asStream() {
		return new ByteArrayInputStream(data, offset, length);
	}

	public void write(OutputStream outputStream) throws IOException {
		outputStream.write(data, offset, length); // throws IOException
	}
}
