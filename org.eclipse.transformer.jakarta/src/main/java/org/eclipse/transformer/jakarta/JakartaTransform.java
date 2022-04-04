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

package org.eclipse.transformer.jakarta;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JakartaTransform {

	public static final String	DEFAULT_RENAMES_REFERENCE		= "jakarta-renames.properties";
	public static final String	DEFAULT_VERSIONS_REFERENCE		= "jakarta-versions.properties";
	public static final String	DEFAULT_BUNDLES_REFERENCE		= "jakarta-bundles.properties";
	public static final String	DEFAULT_DIRECT_REFERENCE		= "jakarta-direct.properties";
	public static final String	DEFAULT_MASTER_TEXT_REFERENCE	= "jakarta-text-master.properties";

	public static Function<String, URL> getRuleLoader() {
		return JakartaTransform.class::getResource;
	}

	public static Map<String, String> getOptionDefaults() {
		Map<String, String> optionDefaults = new HashMap<>();

		optionDefaults.put("renames", DEFAULT_RENAMES_REFERENCE);
		optionDefaults.put("versions", DEFAULT_VERSIONS_REFERENCE);
		optionDefaults.put("bundles", DEFAULT_BUNDLES_REFERENCE);
		optionDefaults.put("direct", DEFAULT_DIRECT_REFERENCE);
		optionDefaults.put("text", DEFAULT_MASTER_TEXT_REFERENCE);
		return optionDefaults;
	}
}
