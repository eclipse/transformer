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

package transformer.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestTransformerImmediateRenames extends TestTransformerBase {

	private static final String STATIC_CONTENT_DIR = "src/test/data/immediate";
	private static final String DYNAMIC_CONTENT_DIR = "target/test/data/immediate";	

	@Override
	public String getStaticContentDir() {
		return STATIC_CONTENT_DIR;
	}
	
	@Override
	public String getDynamicContentDir() {
		return DYNAMIC_CONTENT_DIR;
	}

	//

	// Renames and version assignments are linked.
	//
	// This test overlays both, and verifies that updates were made correctly
	// according to the test plan.

	// RULES_RENAMES("tr", "renames", "Transformation package renames URL", OptionSettings.HAS_ARG,
	//     !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
	// RULES_VERSIONS("tv", "versions", "Transformation package versions URL", OptionSettings.HAS_ARG,
	//     !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

	// Test plan:
	//
	// Initial:                               Final:
	//
	// javax.package0;version="0.0.0"         jakarta.package0;version="0.0.1"
	// javax.package1;version="1.0.0"         jakarta.package11;version="1.1.1"
	// javax.package2;version="2.0.0"         jakarta.package22;version="2.1.1"
	// javax.package3;version="3.0.0"         jakarta.package3;version="3.0.0"
	//
	// tier0: javax.package0=jakarta.package0
	//        javax.package1=jakarta.package1
	//
	// immediate: javax.package1=jakarta.package11
	//            javax.package2=jakarta.package22
	//
	// tier0: jakarta.package0="0.0.1"
	//        jakarta.package1="1.0.1"
	//
	// immediate: jakarta.package11="1.1.1"
	//            jakarta.package22="2.1.1"

	@Test
	void testMultiRenames() throws Exception {
		String inputDir = getStaticContentDir();
		String outputDir = getDynamicContentDir();
		
		String inputFileName = inputDir + '/' + "MANIFEST.MF";
		String outputFileName = outputDir + '/' + "MANIFEST.MF";

		TestUtils.verifyPackageVersions("initial package versions", inputFileName, initialPackageVersions, TARGET_ATTRIBUTE_NAME);
		// throws IOException, AssertionFailedError

		String[] args = new String[] {
			"-tr", inputDir + '/' + "tier0.renames.properties",
			"-tv", inputDir + '/' + "tier0.versions.properties",

            "-ti", "tr", "javax.package1", "jakarta.package11",
            "-ti", "tr", "javax.package2", "jakarta.package22",

            "-ti", "tv", "jakarta.package11", "1.1.1",
            "-ti", "tv", "jakarta.package22", "2.1.1"
		};

		@SuppressWarnings("unused")
		byte[] logBytes = runTransformer(
			inputFileName, outputFileName,
			args,
			DO_DEBUG_LOGGING,
			new String[] {});
		// getLogFragments());
		// TODO:
		// Log verification runs successfully in eclipse and from the command
		// line, but fails in the GIT automation environment. Log capture is
		// not working the same in the GIT environment, for unknown reasons.

		TestUtils.verifyPackageVersions("final package versions", outputFileName, finalPackageVersions, TARGET_ATTRIBUTE_NAME);
		// throws IOException, AssertionFailedError
	}

	//

	public String[] getLogFragments() {
		return new String[] {
			logReplacementFragment("javax.package1", "jakarta.package1", "jakarta.package11"),
			logReplacementFragment("javax.package2", "jakarta.package2", "jakarta.package22")
		};
	}

	//

	public static final Map<String, String> initialPackageVersions;
	public static final Map<String, String> finalPackageVersions;

	static {
		initialPackageVersions = new HashMap<String, String>(8);

		initialPackageVersions.put("javax.package0", "0.0.0");
		initialPackageVersions.put("javax.package1", "1.0.0");
		initialPackageVersions.put("javax.package2", "2.0.0");
		initialPackageVersions.put("javax.package3", "3.0.0");

		finalPackageVersions = new HashMap<String, String>(8);

		finalPackageVersions.put("jakarta.package0", "0.0.1");
		finalPackageVersions.put("jakarta.package11", "1.1.1");
		finalPackageVersions.put("jakarta.package22", "2.1.1");
		finalPackageVersions.put("javax.package3", "3.0.0");
	}

	public static final String TARGET_ATTRIBUTE_NAME = "DynamicImport-Package";

	// "javax.package6;version=\"6.0.0\",javax.package7;version=\"7.0.0\""
}
