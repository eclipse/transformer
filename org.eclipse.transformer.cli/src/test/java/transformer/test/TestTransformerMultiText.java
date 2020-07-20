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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

// Initial:
// The quick brown fox jumps over the lazy dog.
// text[x].ext[y] (x==0..7, y=0..7)
//
// tier0.master.properties:
//   *.ext0=ext0.properties
//     quick=slow
//     lazy=happy
//     The slow brown fox jumps over the happy dog.
//   *.ext1=ext1.properties
//     quick=fast
//     brown=blue
//     -- Not active
//   *.ext2=ext2.properties
//     fox=hen
//     over=under
//     -- Not active
// tier1.master.properties:
//   *.ext1=ext11.properties
//     quick=lethargic
//     lazy=energetic
	//     The lethargic brown fox jumps over the energetic dog.
//   *.ext4=ext4.properties
//      jumps=walks
//      dog=meadow
// 	    The quick brown fox walks over the lazy meadow.";
// tier2.master.properties:
//   *.ext2=ext22.properties
//      fox=elephant
//      over=beside
//      The quick brown elephant jumps beside the lazy dog.";
//   *.ext5=ext5.properties
//      jumps=strolls
//      over=behind
// 	    The quick brown fox strolls behind the lazy dog.

public class TestTransformerMultiText extends TestTransformerBase {

	private static final String DATA_DIR = "src/test/data/multi";

	@Override
	public String getDataDir() {
		return DATA_DIR;
	}

	// Rules data ...

	public static class TextRulesData {
		public final String fileName;
		public final String[] assignments;

		public TextRulesData(String fileName, String... assignments) {
			this.fileName = fileName;
			this.assignments = assignments;
		}

		public void write(String dataDir) throws IOException {
			try ( OutputStream outputStream = new FileOutputStream(dataDir + '/' + fileName, false) ) { // truncate
				OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);

				for ( String assignment : assignments ) {
					outputWriter.write(assignment);
					outputWriter.write('\n');
				}

				outputWriter.flush();
			}
		}
	}

	public static final String TIER0_MASTER_PROPERTIES = "tier0.master.properties";
	public static final String TIER1_MASTER_PROPERTIES = "tier1.master.properties";
	public static final String TIER2_MASTER_PROPERTIES = "tier2.master.properties";

	private static final TextRulesData[] RULES_DATA =
		new TextRulesData[] {
			new TextRulesData(TIER0_MASTER_PROPERTIES,
				"*.ext0=ext0.properties",
				"*.ext1=ext1.properties",
				"*.ext2=ext2.properties"),
			new TextRulesData("ext0.properties", "quick=slow", "lazy=happy"),
			new TextRulesData("ext1.properties", "quick=fast", "brown=blue"),
			new TextRulesData("ext2.properties", "fox=hen", "over=under"),

			new TextRulesData("tier1.master.properties", "*.ext1=ext11.properties", "*.ext4=ext4.properties"),
			new TextRulesData("ext11.properties", "quick=lethargic", "lazy=energetic"),
			new TextRulesData("ext4.properties", "jumps=walks", "dog=meadow"),

			new TextRulesData("tier2.master.properties", "*.ext2=ext22.properties", "*.ext5=ext5.properties"),
			new TextRulesData("ext22.properties", "fox=elephant", "over=beside"),
			new TextRulesData("ext5.properties", "jumps=strolls", "over=behind")
		};

	public static String[] getLogFragments() {
		return new String[] {
			logReplacementFragment("*.ext1", "ext1.properties", "ext11.properties"),
			logReplacementFragment("*.ext2", "ext2.properties", "ext22.properties")
		};
	}

	protected void writeRulesData(String dataDir) throws IOException{
		for ( TextRulesData rulesData : RULES_DATA ) {
			rulesData.write(dataDir); // throws IOException
		}
	}

	// Input data ...

	public static final String INPUT_TEXT =
	    "The quick brown fox jumps over the lazy dog.";

	public static final int NUM_FILES = 8;
	public static final int NUM_EXTS = 8;

	protected static String getExtension(int extNo) {
		return ( '.' + "ext" + Integer.toString(extNo) );
	}

	protected static String getInputName(int fileNo, int extNo) {
		return "text" + Integer.toString(fileNo) + getExtension(extNo);
	}

	// Output data ...

	public static final Map<String, String> OUTPUT_TEXT_MAP;

	static {
		Map<String, String> outputMap = new HashMap<String, String>();

		outputMap.put(".ext0", "The slow brown fox jumps over the happy dog.");
		outputMap.put(".ext3", INPUT_TEXT);

		outputMap.put(".ext1", "The lethargic brown fox jumps over the energetic dog.");
		outputMap.put(".ext4", "The quick brown fox walks over the lazy meadow.");

		outputMap.put(".ext2", "The quick brown elephant jumps beside the lazy dog.");
		outputMap.put(".ext5", "The quick brown fox strolls behind the lazy dog.");

		outputMap.put(".ext6", INPUT_TEXT);
		outputMap.put(".ext7", INPUT_TEXT);

		OUTPUT_TEXT_MAP = outputMap;
	}

	//

	@Test
	void testMultiText() throws Exception {
		String dataDir = getDataDir();

		String propertiesDir = dataDir + '/' + "properties";

		String inputDir = dataDir + '/' + "input";
		String outputDir = dataDir + '/' + "output";

		(new File(propertiesDir)).mkdir();
		writeRulesData(propertiesDir);

		(new File(inputDir)).mkdir();

		TestUtils.writeInputData(
			inputDir, NUM_FILES, NUM_EXTS,
			TestTransformerMultiText::getInputName,
			INPUT_TEXT); // throws IOException

		String[] args = new String[] {
			"-tf", propertiesDir + '/' + TIER0_MASTER_PROPERTIES,
			"-tf", propertiesDir + '/' + TIER1_MASTER_PROPERTIES,
			"-tf", propertiesDir + '/' + TIER2_MASTER_PROPERTIES
		};

		@SuppressWarnings("unused")
		byte[] logBytes = runTransformer(
			inputDir, outputDir,
			args,
			DO_DEBUG_LOGGING,
			new String[] {});
		// getLogFragments());
		// TODO:
		// Log verification runs successfully in eclipse and from the command
		// line, but fails in the GIT automation environment. Log capture is
		// not working the same in the GIT environment, for unknown reasons.

		TestUtils.verifyOutput(
			outputDir, NUM_FILES, NUM_EXTS,
			TestTransformerMultiText::getInputName,
			TestTransformerMultiText::getExtension,
			OUTPUT_TEXT_MAP); // throws IOException,
	}
}