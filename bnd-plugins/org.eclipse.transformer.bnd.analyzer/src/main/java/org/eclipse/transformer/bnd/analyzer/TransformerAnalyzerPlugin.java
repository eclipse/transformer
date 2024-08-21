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

package org.eclipse.transformer.bnd.analyzer;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.externalplugin.ExternalPlugin;

/**
 * Bnd Analyzer Plugin for Transformer.
 */
@ExternalPlugin(name = "Transformer", objectClass = AnalyzerPlugin.class)
public class TransformerAnalyzerPlugin extends BaseTransformerPlugin implements AnalyzerPlugin {
	public TransformerAnalyzerPlugin() {
		/*
		  We want to run before other AnalyzerPlugins so they will operate on
		  the transformed classes and resources.
		 */
		super(-10_000);
	}

	@Override
	public int ordering() {
		return super.ordering();
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		return transform(analyzer);
	}
}
