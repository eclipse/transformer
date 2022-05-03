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

package main;

import conditioner.Conditioner;

public class Greeter {

	public static void main(String[] args) {
		new Greeter(args).greet();
	}

	private String[] args;
	public Greeter(String[] args) {
		this.args = args;
	}

	public void greet() {
		Conditioner conditioner = new Conditioner();
		for (String arg : args) {
			System.out.printf("Hello %s\n", conditioner.condition(arg));
		}
	}
}
