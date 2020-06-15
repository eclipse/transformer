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

package transformer.test.util;

import java.io.PrintWriter;

public interface Delta {

	static boolean strCmp(String str1, String str2) {
		if ((str1 == null) && (str2 == null)) {
			return true;
		} else if ((str1 == null) || (str2 == null)) {
			return false;
		} else {
			return str1.equals(str2);
		}
	}

	//

	boolean	DO_RECORD_ADDED			= true;
	boolean	DO_RECORD_REMOVED		= true;
	boolean	DO_RECORD_CHANGED		= true;
	boolean	DO_RECORD_STILL			= true;

	int		ANY_NUMBER				= -1;

	int		ANY_NUMBER_OF_ADDED		= ANY_NUMBER;
	int		ANY_NUMBER_OF_REMOVED	= ANY_NUMBER;
	int		ANY_NUMBER_OF_CHANGED	= ANY_NUMBER;
	int		ANY_NUMBER_OF_STILL		= ANY_NUMBER;

	int		ZERO_ADDED				= 0;
	int		ZERO_REMOVED			= 0;
	int		ZERO_CHANGED			= 0;
	int		ZERO_STILL				= 0;

	//

	String getHashText();

	boolean isNull();

	void log(PrintWriter writer);
}
