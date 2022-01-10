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

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A type representing data having a name.
 */
public interface ByteData {
	/**
	 * The name associated with the data.
	 *
	 * @return The name associated with the data.
	 */
	String name();

	/**
	 * The ByteBuffer associated with the data.
	 *
	 * @return The buffer associated with the data.
	 */
	ByteBuffer buffer();

	/**
	 * The length of the data.
	 *
	 * @return The length of the data.
	 */
	int length();

	/**
	 * A new InputStream stream to read the data.
	 *
	 * @return The stream containing data.
	 */
	InputStream stream();
}
