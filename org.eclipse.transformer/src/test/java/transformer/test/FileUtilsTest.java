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

package transformer.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import org.eclipse.transformer.util.FileUtils;
import org.junit.jupiter.api.Test;

class FileUtilsTest {
	static Random rd = new Random();

	@Test
	void read_count() throws Exception {
		byte[] source = new byte[2048];
		rd.nextBytes(source);
		InputStream in = new ByteArrayInputStream(source);
		ByteBuffer buffer = ByteBuffer.allocate(source.length);
		ByteBuffer result = FileUtils.read("random", in, buffer, source.length);

		assertThat(result).isSameAs(buffer);
		assertThat(result.capacity()).isEqualTo(buffer.capacity());
		assertThat(result.remaining()).isEqualTo(source.length);
		byte[] array = new byte[result.remaining()];
		result.get(array);
		assertThat(array).containsExactly(source);
	}

	@Test
	void read_growth_count() throws Exception {
		byte[] source = new byte[2048];
		rd.nextBytes(source);
		InputStream in = new ByteArrayInputStream(source);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteBuffer result = FileUtils.read("random", in, buffer, source.length);

		assertThat(result).isNotSameAs(buffer);
		assertThat(result.capacity()).isGreaterThan(buffer.capacity());
		assertThat(result.remaining()).isEqualTo(source.length);
		byte[] array = new byte[result.remaining()];
		result.get(array);
		assertThat(array).containsExactly(source);
	}

	@Test
	void read_growth() throws Exception {
		byte[] source = new byte[2048];
		rd.nextBytes(source);
		InputStream in = new ByteArrayInputStream(source);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.position(10);
		buffer = buffer.slice(); // get ByteBuffer with non-zero array offset
		ByteBuffer result = FileUtils.read("random", in, buffer);

		assertThat(result).isNotSameAs(buffer);
		assertThat(result.capacity()).isGreaterThan(buffer.capacity());
		assertThat(result.remaining()).isEqualTo(source.length);
		byte[] array = new byte[result.remaining()];
		result.get(array);
		assertThat(array).containsExactly(source);
	}

	@Test
	void read_no_buffer() throws Exception {
		byte[] source = new byte[2048];
		rd.nextBytes(source);
		InputStream in = new ByteArrayInputStream(source);
		ByteBuffer result = FileUtils.read("random", in);

		assertThat(result.remaining()).isEqualTo(source.length);
		byte[] array = new byte[result.remaining()];
		result.get(array);
		assertThat(array).containsExactly(source);
	}

}
