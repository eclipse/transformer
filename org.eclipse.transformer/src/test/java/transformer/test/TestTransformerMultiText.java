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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.io.IO;
import org.eclipse.transformer.AppOption;
import org.junit.jupiter.api.Test;

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
// 	    The quick brown fox walks over the lazy meadow.
// tier2.master.properties:
//   *.ext2=ext22.properties
//      fox=elephant
//      over=beside
//      The quick brown elephant jumps beside the lazy dog.
//   *.ext5=ext5.properties
//      jumps=strolls
//      over=behind
// 	    The quick brown fox strolls behind the lazy dog.

public class TestTransformerMultiText extends TestTransformerBase {

	@Override
	public String getStaticContentDir() {
		return null;
	}

	@Override
	public String getDynamicContentDir() {
		return "target/test/data/multi-text";
	}

	// Rules data ...

	public record TextRulesData(String fileName, String... assignments) {

		public void write(String propertiesDir) throws IOException {
				try (OutputStream outputStream = new FileOutputStream(propertiesDir + '/' + fileName(), false)) { // truncate
					OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);

					for (String assignment : assignments()) {
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

			new TextRulesData(TIER1_MASTER_PROPERTIES, "*.ext1=ext11.properties", "*.ext4=ext4.properties"),
			new TextRulesData("ext11.properties", "quick=lethargic", "lazy=energetic"),
			new TextRulesData("ext4.properties", "jumps=walks", "dog=meadow"),

			new TextRulesData(TIER2_MASTER_PROPERTIES, "*.ext2=ext22.properties", "*.ext5=ext5.properties"),
			new TextRulesData("ext22.properties", "fox=elephant", "over=beside"),
			new TextRulesData("ext5.properties", "jumps=strolls", "over=behind")
		};

	public static List<String> getLogFragments() {
		return Arrays.asList(
			logReplacementFragment("*.ext1", "ext1.properties", "ext11.properties"),
			logReplacementFragment("*.ext2", "ext2.properties", "ext22.properties")
		);
	}

	protected void writeRulesData(String propertiesDir) throws IOException{
		for ( TextRulesData rulesData : RULES_DATA ) {
			rulesData.write(propertiesDir);
		}
	}

	// Input data ...

	public static final String INPUT_TEXT =
	    "The quick brown fox jumps over the lazy dog.";

	public static final int NUM_FILES = 8;
	public static final int NUM_EXTS = 8;

	protected static String getExtension(int extNo) {
		return ".ext" + Integer.toString(extNo);
	}

	protected static String getInputName(int fileNo, int extNo) {
		return "folder/text" + Integer.toString(fileNo) + getExtension(extNo);
	}

	// Output data ...

	public static final Map<String, String> OUTPUT_TEXT_MAP;

	static {
		Map<String, String> outputMap = new HashMap<>();

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
	void testMultiTextDir() throws Exception {
		File dynamicContentDir = new File(getDynamicContentDir());
		IO.deleteContent(dynamicContentDir);

		File propertiesDir = new File(dynamicContentDir, "properties");

		File inputDir = new File(dynamicContentDir, "input");
		File outputDir = new File(dynamicContentDir, "output");

		IO.mkdirs(propertiesDir);
		writeRulesData(propertiesDir.getPath());

		IO.mkdirs(inputDir);
		IO.mkdirs(outputDir);
		TestUtils.writeInputData(
			inputDir.getPath(), NUM_FILES, NUM_EXTS,
			TestTransformerMultiText::getInputName,
			INPUT_TEXT);

		Map<AppOption, List<String>> options = new HashMap<>();
		options.put(AppOption.RULES_MASTER_TEXT,
			Arrays.asList(new File(propertiesDir, TIER0_MASTER_PROPERTIES).getPath(),
				new File(propertiesDir, TIER1_MASTER_PROPERTIES).getPath(),
				new File(propertiesDir, TIER2_MASTER_PROPERTIES).getPath()));
		runTransformer(inputDir.getPath(), outputDir.getPath(), options, getLogFragments());

		TestUtils.verifyOutput(
			outputDir.getPath(), NUM_FILES, NUM_EXTS,
			TestTransformerMultiText::getInputName,
			TestTransformerMultiText::getExtension,
			OUTPUT_TEXT_MAP);
	}

	@Test
	void testMultiTextZip() throws Exception {
		File dynamicContentDir = new File(getDynamicContentDir());
		IO.deleteContent(dynamicContentDir);

		File propertiesDir = new File(dynamicContentDir, "properties");

		File inputDir = new File(dynamicContentDir, "input");
		File outputDir = new File(dynamicContentDir, "output");

		IO.mkdirs(propertiesDir);
		writeRulesData(propertiesDir.getPath());

		IO.mkdirs(inputDir);
		IO.mkdirs(outputDir);
		TestUtils.writeInputData(inputDir.getPath(), NUM_FILES, NUM_EXTS, TestTransformerMultiText::getInputName,
			INPUT_TEXT);

		Map<AppOption, List<String>> options = new HashMap<>();
		options.put(AppOption.RULES_MASTER_TEXT,
			Arrays.asList(new File(propertiesDir, TIER0_MASTER_PROPERTIES).getPath(),
				new File(propertiesDir, TIER1_MASTER_PROPERTIES).getPath(),
				new File(propertiesDir, TIER2_MASTER_PROPERTIES).getPath()));

		File inputZip = new File(dynamicContentDir, "input.zip");
		File outputZip = new File(dynamicContentDir, "output.zip");

		System.out.println("Input dir [ " + inputDir.getAbsolutePath() + " ]");
		System.out.println("Input zip [ " + inputZip.getAbsolutePath() + " ]");
		System.out.println("Output dir [ " + outputDir.getAbsolutePath() + " ]");
		System.out.println("Output zip [ " + outputZip.getAbsolutePath() + " ]");

		TestUtils.zip(inputDir, inputZip);

		runTransformer(inputZip.getPath(), outputZip.getPath(), options, getLogFragments());

		TestUtils.unzip(outputZip, outputDir);

		TestUtils.verifyOutput(
			outputDir.getPath(), NUM_FILES, NUM_EXTS,
			TestTransformerMultiText::getInputName,
			TestTransformerMultiText::getExtension,
			OUTPUT_TEXT_MAP);
	}
}
