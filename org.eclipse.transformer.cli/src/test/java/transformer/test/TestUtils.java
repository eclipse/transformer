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

package transformer.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

public class TestUtils {

	public static final boolean DO_CREATE = true;

	public static void verifyDirectory(String targetPath, boolean create, String description) {
		String methodName = "verifyDirectory";

		File targetFile = new File(targetPath);
		String targetAbsPath = targetFile.getAbsolutePath();

		if ( !targetFile.exists() ) {
			if ( create ) {
				System.out.println(methodName + ": Creating " + description + " directory [ " + targetAbsPath + " ]");
				targetFile.mkdirs();
			}
		}

		if (!targetFile.exists() ) {
			fail(methodName + ": Failure: Could not create " + description + " ] directory [ " + targetAbsPath + " ]");
		} else if ( !targetFile.isDirectory() ) {
			fail(methodName + ": Failure: Location " + description + " is not a directory [ " + targetAbsPath + " ]");
		} else {
			System.out.println(methodName + ": Success: Location " + description + " exists and is a directory [ " + targetAbsPath + " ]");
		}
	}

}
