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

package org.eclipse.transformer.jakarta;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.AppOption;
import org.osgi.annotation.bundle.Header;

@Header(name = "Main-Class", value = "${@class}")
public class JakartaTransformer {

	public static void main(String[] args) throws Exception {
		Transformer jTrans = new Transformer(System.out, System.err);
		jTrans.setOptionDefaults(JakartaTransformer.class, getOptionDefaults());
		jTrans.setArgs(args);

		@SuppressWarnings("unused")
		int rc = jTrans.run();
		// System.exit(rc); // TODO: How should this code be returned?
	}

	public static final String	DEFAULT_RENAMES_REFERENCE		= "jakarta-renames.properties";
	public static final String	DEFAULT_VERSIONS_REFERENCE		= "jakarta-versions.properties";
	public static final String	DEFAULT_BUNDLES_REFERENCE		= "jakarta-bundles.properties";
	public static final String	DEFAULT_DIRECT_REFERENCE		= "jakarta-direct.properties";
	public static final String	DEFAULT_MASTER_TEXT_REFERENCE	= "jakarta-text-master.properties";
	public static final String	DEFAULT_PER_CLASS_CONSTANT_MASTER_REFERENCE	= "jakarta-per-class-constant-master.properties";

	public static Map<Transformer.AppOption, String> getOptionDefaults() {
		HashMap<Transformer.AppOption, String> optionDefaults = new HashMap<>();

		optionDefaults.put(AppOption.RULES_RENAMES, DEFAULT_RENAMES_REFERENCE);
		optionDefaults.put(AppOption.RULES_VERSIONS, DEFAULT_VERSIONS_REFERENCE);
		optionDefaults.put(AppOption.RULES_BUNDLES, DEFAULT_BUNDLES_REFERENCE);
		optionDefaults.put(AppOption.RULES_DIRECT, DEFAULT_DIRECT_REFERENCE);
		optionDefaults.put(AppOption.RULES_MASTER_TEXT, DEFAULT_MASTER_TEXT_REFERENCE);
		optionDefaults.put(AppOption.RULES_PER_CLASS_CONSTANT, DEFAULT_PER_CLASS_CONSTANT_MASTER_REFERENCE);

		return optionDefaults;
	}
}
