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
import java.net.URL;
import java.util.Properties;

import aQute.lib.utf8properties.UTF8Properties;

public class PropertiesUtils {

	private PropertiesUtils() {}

	public static Properties loadProperties(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			return loadProperties(stream);
		}
	}

	public static Properties loadProperties(InputStream inputStream) throws IOException {
		Properties properties = createProperties();
		properties.load(inputStream);
		return properties;
	}

	public static Properties createProperties() {
		return new UTF8Properties();
	}
}
