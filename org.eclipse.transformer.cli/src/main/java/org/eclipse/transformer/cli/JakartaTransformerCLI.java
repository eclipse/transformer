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

package org.eclipse.transformer.cli;

import java.io.PrintStream;

import org.eclipse.transformer.Transformer.ResultCode;
import org.eclipse.transformer.jakarta.JakartaTransform;
import org.osgi.annotation.bundle.Header;

@Header(name = "Main-Class", value = "${@class}")
public class JakartaTransformerCLI extends TransformerCLI {
	public static void main(String[] args) throws Exception {
		JakartaTransformerCLI cli = new JakartaTransformerCLI(System.out, System.err, args);
		cli.setOptionDefaults(JakartaTransform.getRuleLoader(), JakartaTransform.getOptionDefaults());
		@SuppressWarnings("unused")
		ResultCode rc = runWith(cli);
		// System.exit(rc); // TODO: How should this code be returned?
	}

	/**
	 * @param sysOut
	 * @param sysErr
	 * @param args
	 */
	public JakartaTransformerCLI(PrintStream sysOut, PrintStream sysErr, String... args) {
		super(sysOut, sysErr, args);
	}
}
