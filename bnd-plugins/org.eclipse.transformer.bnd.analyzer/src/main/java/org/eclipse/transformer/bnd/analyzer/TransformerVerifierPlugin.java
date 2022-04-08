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
import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.verifier.VerifierPlugin;

/**
 * Bnd Verifier Plugin for Transformer.
 */
@ExternalPlugin(name = "Transformer", objectClass = VerifierPlugin.class)
public class TransformerVerifierPlugin extends BaseTransformerPlugin implements VerifierPlugin {
	public TransformerVerifierPlugin() {
		/**
		 * We want to run after other VerifierPlugins so we will operate on the
		 * final classes and resources.
		 */
		super(10_000);
	}

	@Override
	public int ordering() {
		return super.ordering();
	}

	@Override
	public void verify(Analyzer analyzer) throws Exception {
		transform(analyzer);
	}
}
