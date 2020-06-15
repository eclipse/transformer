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

package transformer.test.data;

public class Sample_DirectStrings {
	public static final String	DIRECT1		= "DIRECT_1";
	public static final String	DIRECT12	= "DIRECT_1_2";
	public static final String	DIRECT21	= "2_DIRECT_1";
	public static final String	DIRECT212	= "2_DIRECT_1_2";

	static {
		System.out.println("Sample value 1");
		System.out.println("Name: Fred");
	}
}
