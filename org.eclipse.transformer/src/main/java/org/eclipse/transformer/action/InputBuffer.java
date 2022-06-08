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

import java.nio.ByteBuffer;

/**
 * <em>{@link ByteBuffer} holder.</em>
 * <p>
 * The intended implementation uses a thread-local variable to hold a byte
 * buffer instance which is shared by a single thread.
 * <p>
 * This enables sharing and reallocating a single byte buffer for multiple read
 * and write operations occurring synchronously in a single thread.
 * <p>
 * This type is part of the action API: The type is expected by
 * {@link Action.ActionInitData}.
 */
public interface InputBuffer {
	/**
	 * Answer the current shared byte buffer instance.
	 * <p>
	 * If necessary, allocate and store a new byte buffer for this thread.
	 *
	 * @return The current shared byte buffer instance.
	 */
	ByteBuffer getInputBuffer();

	/**
	 * Set the current shared byte buffer instance.
	 * <p>
	 * This is necessary, for example, if operations have caused the byte buffer
	 * to be reallocated. For the duration of processing on the current thread,
	 * each reallocated byte buffer is set as the new shared byte buffer
	 * instance.
	 * <p>
	 * Generally, a reallocation will occur when the buffer needs to be
	 * enlarged, for a read of a new resource which is larger than any which was
	 * previously read.
	 *
	 * @param inputBuffer
	 */
	void setInputBuffer(ByteBuffer inputBuffer);
}
