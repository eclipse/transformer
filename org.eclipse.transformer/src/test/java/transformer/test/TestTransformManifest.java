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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.BundleDataImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.PropertiesUtils;
import org.eclipse.transformer.util.SignatureUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transformer.test.util.CaptureLoggerImpl;

public class TestTransformManifest extends CaptureTest {
	private Properties prior;
	private Map<String, String>	packageRenames;
	private Map<String, String>	packageVersions;
	protected Map<String, Map<String, String>>	specificPackageVersions;

	@BeforeEach
	public void setUp() throws IOException {
		prior = new Properties();
		prior.putAll(System.getProperties());

		Properties renameProperties;
		try (InputStream renameStream = getClass().getClassLoader()
			.getResourceAsStream("manifest/renames.properties")) {
			renameProperties = PropertiesUtils.loadProperties(renameStream);
		}
		packageRenames = TransformProperties.convertPropertiesToMap(renameProperties);

		Properties versionProperties;
		try (InputStream versionsStream = getClass().getClassLoader()
			.getResourceAsStream("manifest/versions.properties")) {
			versionProperties = PropertiesUtils.loadProperties(versionsStream);
		}
		packageVersions = new HashMap<>();
		specificPackageVersions = new HashMap<>();
		TransformProperties.setPackageVersions(versionProperties, packageVersions, specificPackageVersions);
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	public static final String	JAVAX_ANNOTATION					= "javax.annotation";
	public static final String	JAVAX_ANNOTATION_SECURITY			= "javax.annotation.security";

	public static final String	JAKARTA_ANNOTATION					= "jakarta.annotation";
	public static final String	JAKARTA_ANNOTATION_SECURITY			= "jakarta.annotation.security";

	public static final String	JAVAX_SERVLET						= "javax.servlet";
	public static final String	JAVAX_SERVLET_HTTP					= "javax.servlet.http";
	public static final String	JAVAX_SERVLET_SCI					= "javax.servlet.ServletContainerInitializer";

	public static final String	JAKARTA_SERVLET						= "jakarta.servlet";
	public static final String	JAKARTA_SERVLET_HTTP				= "jakarta.servlet.http";
	public static final String	JAKARTA_SERVLET_SCI					= "jakarta.servlet.ServletContainerInitializer";

	public static final String	JAVAX_TRANSACTION					= "javax.transaction";
	public static final String	JAVAX_TRANSACTION_XA				= "javax.transaction.xa";
	public static final String	JAVAX_TRANSACTION_TM				= "javax.transaction.TransactionManager";
	public static final String	JAVAX_TRANSACTION_TSR				= "javax.transaction.TransactionSynchronizationRegistry";
	public static final String	JAVAX_TRANSACTION_UT				= "javax.transaction.UserTransaction";

	public static final String	JAKARTA_TRANSACTION					= "jakarta.transaction";
	public static final String	JAKARTA_TRANSACTION_XA				= "jakarta.transaction.xa";
	public static final String	JAKARTA_TRANSACTION_TM				= "jakarta.transaction.TransactionManager";
	public static final String	JAKARTA_TRANSACTION_TSR				= "jakarta.transaction.TransactionSynchronizationRegistry";
	public static final String	JAKARTA_TRANSACTION_UT				= "jakarta.transaction.UserTransaction";

	public Map<String, String> getIncludes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getExcludes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getPackageRenames() {
		return packageRenames;
	}

	public Map<String, String> getPackageVersions() {
		return packageVersions;
	}

	// ManifestActionImpl supports these attributes for version updates:
	// "DynamicImport-Package"
	// "Import-Package"
	// "Export-Package"
	// "Subsystem-Content"
	// "IBM-API-Package"
	// "Provide-Capability"
	// "Require-Capability"

	public Map<String, Map<String, String>> getSpecificPackageVersions() {
		return specificPackageVersions;
	}

	//

	public static final String			WEBCONTAINER_SYMBOLIC_NAME	= "com.ibm.ws.webcontainer";
	public static final String			WEBCONTAINER_BUNDLE_TEXT	= "com.ibm.ws.webcontainer.jakarta,2.0,+\" Jakarta\",+\"; Jakarta Enabled\"";

	public static final String[][]		WEBCONTAINER_BUNDLE_OUTPUT	= new String[][] {
		{ "Bundle-SymbolicName: ", "com.ibm.ws.webcontainer.jakarta" },
		{ "Bundle-Version: ", "2.0" },
		{ "Bundle-Name: ", "WAS WebContainer Jakarta" },
		{ "Bundle-Description: ", "WAS WebContainer 8.0 with Servlet 3.0 support; Jakarta Enabled" }
	};

	//

	public static final String			TRANSACTION_SYMBOLIC_NAME	= "com.ibm.ws.transaction";
	public static final String			WILDCARD_SYMBOLIC_NAME		= "*";
	public static final String			WILDCARD_BUNDLE_TEXT		= "*.jakarta,2.0,+\" Jakarta\",+\"; Jakarta Enabled\"";

	public static final String[][]		TRANSACTION_BUNDLE_OUTPUT	= new String[][] {
		{ "Bundle-SymbolicName: ", "com.ibm.ws.transaction.jakarta" },
		// Not changed: Wildcard identity updates to not update the version
		{ "Bundle-Version: ", "1.0.40.202005041216" },
		{ "Bundle-Name: ", "Transaction Jakarta" },
		{ "Bundle-Description: ", "Transaction support, version 1.0; Jakarta Enabled" }
	};

	//

	public static final BundleData		WEBCONTAINER_BUNDLE_DATA	= new BundleDataImpl(WEBCONTAINER_BUNDLE_TEXT);

	protected Map<String, BundleData>	bundleUpdates;

	public Map<String, BundleData> getBundleUpdates() {
		if (bundleUpdates == null) {
			bundleUpdates = new HashMap<>();
			bundleUpdates.put(WEBCONTAINER_SYMBOLIC_NAME, WEBCONTAINER_BUNDLE_DATA);
		}
		return bundleUpdates;
	}

	//

	public static final BundleData		WILDCARD_BUNDLE_DATA	= new BundleDataImpl(WILDCARD_BUNDLE_TEXT);

	protected Map<String, BundleData>	bundleUpdatesTx;

	public Map<String, BundleData> getBundleUpdatesTx() {
		if (bundleUpdatesTx == null) {
			bundleUpdatesTx = new HashMap<>();
			bundleUpdatesTx.put(WILDCARD_SYMBOLIC_NAME, WILDCARD_BUNDLE_DATA);
		}
		return bundleUpdatesTx;
	}

	//

	public Map<String, String> getDirectStrings() {
		return Collections.emptyMap();
	}

	public ManifestActionImpl jakartaManifestAction;

	public ManifestActionImpl getJakartaManifestAction() {
		if (jakartaManifestAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()),
				new SignatureRuleImpl(useLogger, getPackageRenames(), getPackageVersions(), null, getBundleUpdates(),
					null, getDirectStrings(), Collections.emptyMap()));

			jakartaManifestAction = new ManifestActionImpl(context, ActionType.MANIFEST);
		}
		return jakartaManifestAction;
	}

	public ManifestActionImpl jakartaFeatureAction;

	public ManifestActionImpl getJakartaFeatureAction() {
		if (jakartaFeatureAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()),
				new SignatureRuleImpl(useLogger, getPackageRenames(), getPackageVersions(), null, null, null, null,
					Collections.emptyMap()));

			jakartaFeatureAction = new ManifestActionImpl(context, ActionType.FEATURE);
		}

		return jakartaFeatureAction;
	}

	//

	public ManifestActionImpl jakartaManifestActionTx;

	public ManifestActionImpl getJakartaManifestActionTx() {
		if (jakartaManifestActionTx == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()), new SignatureRuleImpl(useLogger,
					getPackageRenames(), getPackageVersions(), null, getBundleUpdatesTx(), null, getDirectStrings(),
					Collections.emptyMap()));

			jakartaManifestActionTx = new ManifestActionImpl(context, ActionType.MANIFEST);

		}
		return jakartaManifestActionTx;
	}

	public ManifestActionImpl specificJakartaManifestAction;

	public ManifestActionImpl getSpecificJakartaManifestAction() {
		if (specificJakartaManifestAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()), new SignatureRuleImpl(useLogger,
					getPackageRenames(), getPackageVersions(), getSpecificPackageVersions(), getBundleUpdatesTx(),
					null, getDirectStrings(), Collections.emptyMap()));

			specificJakartaManifestAction = new ManifestActionImpl(context, ActionType.MANIFEST);
		}

		return specificJakartaManifestAction;
	}

	//

	protected static final class Occurrences {
		public final String	initialTag;
		public final int	initialTagInitialCount;
		public final int	initialTagFinalCount;

		public final String	finalTag;
		public final int	finalTagInitialCount;
		public final int	finalTagFinalCount;

		public Occurrences(String initialTag, int initialTagInitialCount, int initialTagFinalCount, String finalTag,
			int finalTagInitialCount, int finalTagFinalCount) {

			this.initialTag = initialTag;
			this.initialTagInitialCount = initialTagInitialCount;
			this.initialTagFinalCount = initialTagFinalCount;

			this.finalTag = finalTag;
			this.finalTagInitialCount = finalTagInitialCount;
			this.finalTagFinalCount = finalTagFinalCount;
		}

		public void verifyInitial(List<String> lines) {
			int actualInitialTagInitial = TestUtils.occurrences(lines, initialTag);
			System.out.println("Tag [ " + initialTag + " ]" +
				" Expected [ " + initialTagInitialCount + " ]" +
				" Actual [ " + actualInitialTagInitial + " ]");
			Assertions.assertEquals(initialTagInitialCount, actualInitialTagInitial, initialTag);

			int actualFinalTagInitial = TestUtils.occurrences(lines, finalTag);
			System.out.println("Tag [ " + finalTag + " ]" +
				" Expected [ " + finalTagInitialCount + " ]" +
				" Actual [ " + actualFinalTagInitial + " ]");
			Assertions.assertEquals(finalTagInitialCount, actualFinalTagInitial, initialTag);
		}

		public void verifyFinal(List<String> lines) {
			int actualInitialTagFinal = TestUtils.occurrences(lines, initialTag);
			System.out.println("Tag [ " + initialTag + " ]" +
				" Expected [ " + initialTagFinalCount + " ]" +
				" Actual [ " + actualInitialTagFinal + " ]");
			Assertions.assertEquals(initialTagFinalCount, actualInitialTagFinal, initialTag);

			int actualFinalTagFinal = TestUtils.occurrences(lines, finalTag);
			System.out.println("Tag [ " + finalTag + " ]" +
				" Expected [ " + finalTagFinalCount + " ]" +
				" Actual [ " + actualFinalTagFinal + " ]");
			Assertions.assertEquals(finalTagFinalCount, actualFinalTagFinal, initialTag);
		}
	}

	//

	public List<String> displayManifest(String manifestPath, InputStream manifestStream) throws IOException {
		System.out.println("Manifest [ " + manifestPath + " ]");
		List<String> manifestLines = TestUtils.loadLines(manifestStream);

		List<String> collapsedLines = TestUtils.manifestCollapse(manifestLines);

		int numLines = collapsedLines.size();
		for (int lineNo = 0; lineNo < numLines; lineNo++) {
			System.out.printf("[ %3d ] [ %s ]\n", lineNo, collapsedLines.get(lineNo));
		}

		return collapsedLines;
	}

	public static final String UNUSED_EXPECTED_OUTPUT_PATH = null;
	public static final Occurrences[] UNUSED_OCCURRENCES = null;
	public static final String[][] UNUSED_IDENTITY_UPDATES = null;

	public void testTransform(
		String inputPath,
		String expectedOutputPath,
		Occurrences[] occurrences,
		String[][] identityUpdates,
		ManifestActionImpl manifestAction) throws TransformException, IOException {

		System.out.println("Transform [ " + inputPath + " ] using [ " + manifestAction.getName() + " ] ...");

		System.out.println("Read [ " + inputPath + " ]");
		InputStream manifestInput = TestUtils.getResourceStream(inputPath);

		List<String> inputLines = displayManifest(inputPath, manifestInput);

		if ( occurrences != null ) {
			System.out.println("Verify input [ " + inputPath + " ]");
			for (Occurrences occurrence : occurrences) {
				occurrence.verifyInitial(inputLines);
			}
		}

		ByteData manifestOutput;
		try (InputStream input = TestUtils.getResourceStream(inputPath)) {
			manifestOutput = manifestAction.apply(manifestAction.collect(inputPath, input));
		}

		List<String> outputLines = displayManifest(inputPath + " transformed", manifestOutput.stream());

		System.out.println("Verify output [ " + inputPath + " ]");

		if ( expectedOutputPath != null ) {
			System.out.println("Read expected output [ " + expectedOutputPath + " ]");
			InputStream expectedInput = TestUtils.getResourceStream(expectedOutputPath);

			List<String> expectedLines = displayManifest(expectedOutputPath, expectedInput);

			System.out.println("Verify input [ " + inputPath + " ] against expected [ " + expectedOutputPath + " ]");

			int numLines = outputLines.size();

			assertThat(outputLines).as("Incorrect number of output lines")
				.hasSameSizeAs(inputLines);

			for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
				String actualLine = outputLines.get(lineNo);
				String expectedLine = expectedLines.get(lineNo);

				assertThat(actualLine).as("Line [ %s ] mismatch", lineNo)
					.isEqualTo(expectedLine);
			}
		}

		if ( occurrences != null ) {
			for (Occurrences occurrence : occurrences) {
				occurrence.verifyFinal(outputLines);
			}
		}

		if (identityUpdates != null) {
			System.out.println("Verify identity update [ " + inputPath + " ]");

			String errorMessage = validate(outputLines, identityUpdates);
			assertThat(errorMessage).as("Bundle identity update failure")
				.isNull();
		}

		System.out.println("Transform [ " + inputPath + " ] using [ " + manifestAction.getName() + " ] ... done");
	}

	// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support
	// Bundle-Name: WAS WebContainer
	// Bundle-SymbolicName: com.ibm.ws.webcontainer
	// Bundle-Version: 1.1.35.cl191220191120-0300
	//
	// com.ibm.ws.webcontainer=com.ibm.ws.webcontainer.jakarta,2.0,+"
	// Jakarta",+"; Jakarta Enabled"
	//
	// Bundle-SymbolicName: com.ibm.ws.webcontainer.jakarta
	// Bundle-Version: 2.0
	// Bundle-Name: WAS WebContainer Jakarta
	// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support;
	// Jakarta Enabled

	protected String validate(List<String> manifestLines, String[][] expectedOutput) {
		boolean[] matches = new boolean[expectedOutput.length];
		int numMatches = 0;

		int numLines = manifestLines.size();

		System.out.println("Validating updated bundle identity: Lines [ " + numLines + " ]");
		for (int matchNo = 0; matchNo < expectedOutput.length; matchNo++) {
			String prefix = expectedOutput[matchNo][0];
			String suffix = expectedOutput[matchNo][1];
			System.out.println("Match [ " + matchNo + " ] Prefix [ " + prefix + " ] Suffix [ " + suffix + " ]");
		}

		for (int lineNo = 0; lineNo < numLines; lineNo++) {
			String line = manifestLines.get(lineNo);

			for (int matchNo = 0; matchNo < expectedOutput.length; matchNo++) {
				String prefix = expectedOutput[matchNo][0];
				String suffix = expectedOutput[matchNo][1];

				String message;

				if (line.startsWith(prefix)) {
					if (line.length() != prefix.length() + suffix.length()) {
						message = "Incorrect line length";
					} else if (!line.endsWith(suffix)) {
						message = "Incorrect attribute value";

					} else if (matches[matchNo]) {
						message = "Duplicate match of [ " + prefix + " ] at line [ " + lineNo + " ]";

					} else {
						System.out.println("Validated [ " + matchNo + " ]:");
						System.out.println("  Prefix [ " + prefix + " ]");
						System.out.println("  Suffix [ " + suffix + " ]");
						System.out.println("  Line [ " + lineNo + " ] [ " + line + " ]");

						message = null;

						matches[matchNo] = true;
						numMatches++;
					}

					if (message != null) {
						System.out.println("Failed [ " + matchNo + " ]: [ " + message + " ]");
						System.out.println("  Prefix [ " + prefix + " ]");
						System.out.println("  Suffix [ " + suffix + " ]");
						System.out.println("  Line [ " + lineNo + " ] [ " + line + " ]");

						return message;
					}
				}
			}
		}

		if (numMatches < expectedOutput.length) {
			return "Located [ " + numMatches + " ] out of [ " + expectedOutput.length + " ] matches";
		} else {
			return null;
		}
	}

	//

	public static final String			TEST_MANIFEST_PATH_WEBCONTAINER	=
		"servlet/META-INF/MANIFEST.MF";

	public static final Occurrences[]	MANIFEST_TO_JAKARTA_DATA		= {
		new Occurrences(JAVAX_SERVLET,      6, 0, JAKARTA_SERVLET,      0, 6),
		new Occurrences(JAVAX_SERVLET_HTTP, 1, 0, JAKARTA_SERVLET_HTTP, 0, 1),
		new Occurrences(JAVAX_SERVLET_SCI,  1, 0, JAKARTA_SERVLET_SCI,  0, 1)
	};

	//

	public static final String			TEST_FEATURE_PATH				=
		"servlet/META-INF/servlet-4.0.mf";

	public static final Occurrences[]	FEATURE_TO_JAKARTA_DATA			= {
		// EMPTY
	};

	//

	@Test
	public void testTransformManifest_Servlet() throws TransformException, IOException {
		testTransform(
			TEST_MANIFEST_PATH_WEBCONTAINER,
			UNUSED_EXPECTED_OUTPUT_PATH,
			MANIFEST_TO_JAKARTA_DATA,
			WEBCONTAINER_BUNDLE_OUTPUT,
			getJakartaManifestAction());
	}

	@Test
	public void testTransformFeature_Servlet() throws TransformException, IOException {
		testTransform(
			TEST_FEATURE_PATH,
			UNUSED_EXPECTED_OUTPUT_PATH,
			FEATURE_TO_JAKARTA_DATA,
			UNUSED_IDENTITY_UPDATES,
			getJakartaFeatureAction());
	}

	//

	public static final String			TEST_MANIFEST_PATH_TX		= "transaction/META-INF/MANIFEST.MF";

	public static final Occurrences[]	MANIFEST_TO_JAKARTA_DATA_TX	= {
		new Occurrences(JAVAX_ANNOTATION,           1, 0,  JAKARTA_ANNOTATION,           0, 1),
		new Occurrences(JAVAX_ANNOTATION_SECURITY,  0, 0,  JAKARTA_ANNOTATION_SECURITY,  0, 0),
		new Occurrences(JAVAX_SERVLET,              4, 0,  JAKARTA_SERVLET,              0, 4),
		new Occurrences(JAVAX_SERVLET_HTTP,         2, 0,  JAKARTA_SERVLET_HTTP,         0, 2),
		new Occurrences(JAVAX_TRANSACTION,          9, 2,  JAKARTA_TRANSACTION,          0, 7),
		// The two '.xa' are not transformed.
		new Occurrences(JAVAX_TRANSACTION_XA,       2, 2,  JAKARTA_TRANSACTION_XA,       0, 0),
		new Occurrences(JAVAX_TRANSACTION_UT,       1, 0,  JAKARTA_TRANSACTION_UT,       0, 1),
		new Occurrences(JAVAX_TRANSACTION_TM,       1, 0,  JAKARTA_TRANSACTION_TM,       0, 1),
		new Occurrences(JAVAX_TRANSACTION_TSR,      1, 0,  JAKARTA_TRANSACTION_TSR,      0, 1)
	};

	@Test
	public void testTransformManifest_Transaction() throws TransformException, IOException {
		testTransform(
			TEST_MANIFEST_PATH_TX,
			UNUSED_EXPECTED_OUTPUT_PATH,
			MANIFEST_TO_JAKARTA_DATA_TX,
			TRANSACTION_BUNDLE_OUTPUT,
			getJakartaManifestActionTx());
	}

	public static final String	newVersion							= "[4.0,5)";

	// Embedding text is the input for each test

	public static final String[] ATTRIBUTE_TEXT = new String[] {
		"; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"[1.0.0,1.0.200)\"",
		";version=\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version= \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version =\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version = \"[2.6,3)\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";resolution:=\"optional\";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version=\"[2.6,3)\"",
		"",
		",",
		";resolution:=\"optional\"", // no version
		",javax.servlet.annotation;version=\"[2.6,3)\"", // leading comma
		";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\"", // missing quote after version
		"\",com.ibm.ws.webcontainer.core;version=\"1.1.0\"" // first char is a quote (no package attributes)
	};

	// Expected results: When replacing the version, the expected result is the
	// entire embedding text with the version of the first package replaced with
	// the new version.

	public static final String[] UPDATED_ATTRIBUTE_TEXT = new String[] {
		"; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"" + newVersion + "\"",
		";version=\"" + newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version= \"" + newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version =\"" + newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version = \"" + newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version = \"" + newVersion + "\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";resolution:=\"optional\";version = \"" + newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version=\"" + newVersion + "\"",
		"",
		",",
		";resolution:=\"optional\"",
		",javax.servlet.annotation;version=\"[2.6,3)\"",
		";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\"", // missing quote (no version replacement)
		"\",com.ibm.ws.webcontainer.core;version=\"1.1.0\""
	};

	// Expected results: When getting package attributes, expected result is
	// just the package attribute text which is at the beginning of the
	// embedding text

	// "; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"[1.0.0,1.0.200)\"";

	public static final String[] HEAD_ATTRIBUTE_TEXT = new String[] {
		"; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"[1.0.0,1.0.200)\"",
		";version=\"[2.6,3)\",",
		";version= \"[2.6,3)\",",
		";version =\"[2.6,3)\",",
		";version = \"[2.6,3)\",",
		";version = \"[2.6,3)\";resolution:=\"optional\",",
		";resolution:=\"optional\";version = \"[2.6,3)\",",
		";version=\"[2.6,3)\"",
		"", // empty string produces empty string
		"", // comma produces empty string
		";resolution:=\"optional\"",
		"", // leading comma followed by package is empty string
		";version=\"[2.6,3),", // missing quote (no version replacement)
		"" // Not starting with ';' produces empty string
	};

	/**
	 * Subclass which allows us to call protected methods of ManifestActionImpl
	 */
	class ManifestActionImpl_Test extends ManifestActionImpl {
		public ManifestActionImpl_Test(ActionContext context) {
			super(context, ActionType.MANIFEST);
		}

		public boolean callIsTrueMatch(String text, int matchStart, int keyLen) {
			return SignatureUtils.packageMatch(text, matchStart, matchStart + keyLen, false) != -1;
		}

		public String callReplacePackages(String attributeName, String text) {
			return replacePackages(attributeName, text);
		}

		public String callReplacePackageVersion(String embeddingText, String newPackageVersion) {
			return replacePackageVersion(embeddingText, newPackageVersion);
		}

		public String callGetPackageAttributeText(String embeddingText) {
			return getPackageAttributeText(embeddingText);
		}
	}

	private ManifestActionImpl_Test manifestAction_test;

	protected ManifestActionImpl_Test getManifestAction() {
		if (manifestAction_test == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()), new SignatureRuleImpl(useLogger,
					getPackageRenames(), getPackageVersions(), null, null, null, null, Collections.emptyMap()));
			manifestAction_test = new ManifestActionImpl_Test(context);
		}

		return manifestAction_test;
	}

	private ManifestActionImpl_Test specificManifestAction_test;

	protected ManifestActionImpl_Test getSpecificManifestAction() {
		if (specificManifestAction_test == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()),
				new SignatureRuleImpl(useLogger,
					getPackageRenames(), getPackageVersions(), getSpecificPackageVersions(),
					null, null, null, Collections.emptyMap()));

			specificManifestAction_test = new ManifestActionImpl_Test(context);
		}

		return specificManifestAction_test;
	}

	/**
	 * Test the ManifestActionImpl.isTrueMatch(...) method, which verifies that
	 * the package found is not part of a larger package name. For example when
	 * searching for javax.servelet, the following are NOT true matches:
	 * my.javax.servlet javax.servlet.http
	 */
	@Test
	public void testIsTrueMatch() {
		ManifestActionImpl_Test manifestAction = getManifestAction();

		boolean result;

		// *** TEST CASES ****
		// 1. myPackage YES -- package is exact length of search text
		//
		// 2. myPackage. NO -- trailing '.' indicates part of a larger package
		// 3. myPackage, YES -- trailing ',' not part of a package name
		// 4. myPackage; YES -- trailing ';' not part of a package name
		// 5. myPackage$ NO -- trailing '$' indicates part of a larger package
		// 6. myPackage= YES -- trailing '=' not part of a package name, but
		// likely busted syntax
		// 7. "myPackage" + " " YES -- trailing ' ' not part of a package name
		//
		// 8. =myPackage YES -- leading '=' not part of a package name, but
		// likely busted syntax
		// 9. .myPackage NO -- leading '.' indicates part of a larger package
		// 10. ,myPackage YES -- leading ',' not part of a package name
		// 11. ;myPackage YES -- leading ';' not part of a package name
		// 12. $myPackage NO -- leading '$' indicates part of a larger package
		// 13. " " + myPackage YES -- leading ' ' not part of a package name
		//
		// 14. myPachagePlus NO -- trailing valid java identifier indicates part
		// of a larger package
		// 15. plusmyPackage NO -- leading valid java identifier indicates part
		// of a larger package

		final String TEXT = "=abc.defgh,ijklm;nopqrs$tuv wxyz= ";
		int matchStart;
		int keyLen;

		matchStart = 0; // Test 1
		keyLen = 3;
		result = manifestAction.callIsTrueMatch("abc", matchStart, keyLen);
		assertTrue(result, "(Package name == text) is MATCH");

		matchStart = 1; // Test 2 "abc" Trailing period
		keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('.' after key) is NO MATCH");

		matchStart = 1; // Test 3 "abc.defgh" Trailing comma
		keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(',' after key) is MATCH");

		matchStart = 11; // Test 4 "ijklm" Trailing semicolon
		keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(';' after key) is MATCH");

		matchStart = 17; // Test 5 "nopqrs" Trailing $
		keyLen = 6;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('$' after key) is MATCH");

		matchStart = 28; // Test 6 "wxyz" Trailing =
		keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "('=' after key) is MATCH");

		matchStart = 17; // Test 7 "nopqrs$tuv" Trailing ' '
		keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(' ' after key) is MATCH");

		matchStart = 1; // Test 8 "abc.defgh" Prior char "="
		keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "('=' before key) is MATCH");

		matchStart = 5; // Test 9 "defgh" Prior char "."
		keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('.' before key) is NO MATCH");

		matchStart = 11; // Test 10 "ijklm" Prior char ","
		keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(',' before key) is MATCH");

		matchStart = 17; // Test 11 "nopqrs$tuv" Prior char ";"
		keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(';' before key) is MATCH");

		matchStart = 24; // Test 12 "tuv" Prior char "$"
		keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('$' before key) is NO MATCH");

		matchStart = 28; // Test 13 "wxyz" Prior char " "
		keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(' ' before key) is MATCH");

		matchStart = 17; // Test 14 "no" char after is a valid package char
		keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "(valid package character after key) is NO MATCH");

		matchStart = 8; // Test 15 "gh" char before is a valid package char
		keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "(valid package character before key) is NO MATCH");
	}

	@Test
	public void testUpdatePackageVersion() {
		ManifestActionImpl_Test manifestAction = getManifestAction();

		String failureText = "Package version transformation failure";

		for ( int testNo = 0; testNo < ATTRIBUTE_TEXT.length; testNo++ ) {
			String result = manifestAction.callReplacePackageVersion(ATTRIBUTE_TEXT[testNo], newVersion);
			assertEquals(UPDATED_ATTRIBUTE_TEXT[testNo], result, failureText);
		}
	}

	@Test
	public void testGetPackageAttribute() {
		ManifestActionImpl_Test manifestAction = getManifestAction();

		String failureText = "Get package attribute failure";

		for ( int testNo = 0; testNo < ATTRIBUTE_TEXT.length; testNo++ ) {
			String result = manifestAction.callGetPackageAttributeText(ATTRIBUTE_TEXT[testNo]);
			assertEquals(HEAD_ATTRIBUTE_TEXT[testNo], result, failureText);
		}
	}

	//

	public static final String  TX_PROVIDE_ATTRIBUTE_NAME = "Provide-Capability";

	public static final String	TX_PROVIDE_TEXT_INPUT	=
		"osgi.service;objectClass:List<String>=\"com.ibm.tx."
		+ "jta.TransactionInflowManager\";uses:=\"com.ibm.tx.jta\",osgi.service;obj"
		+ "ectClass:List<String>=\"com.ibm.tx.remote.RemoteTransactionController\""
		+ ";uses:=\"com.ibm.tx.remote\",osgi.service;objectClass:List<String>=\"com"
		+ ".ibm.ws.transaction.services.TransactionObjectFactory,javax.naming.sp"
		+ "i.ObjectFactory\";uses:=\"com.ibm.ws.transaction.services,javax.naming."
		+ "spi\",osgi.service;objectClass:List<String>=\"com.ibm.ws.tx.embeddable."
		+ "EmbeddableWebSphereUserTransaction,javax.transaction.UserTransaction\""
		+ ";uses:=\"com.ibm.ws.tx.embeddable,javax.transaction\",osgi.service;obje"
		+ "ctClass:List<String>=\"com.ibm.ws.uow.UOWScopeCallback\";uses:=\"com.ibm"
		+ ".ws.uow\",osgi.service;objectClass:List<String>=\"com.ibm.wsspi.injecti"
		+ "onengine.ObjectFactoryInfo\";uses:=\"com.ibm.wsspi.injectionengine\",osg"
		+ "i.service;objectClass:List<String>=\"com.ibm.wsspi.uow.UOWManager\";use"
		+ "s:=\"com.ibm.wsspi.uow\",osgi.service;objectClass:List<String>=\"javax.t"
		+ "ransaction.TransactionSynchronizationRegistry\";uses:=\"javax.transacti"
		+ "on\"";

	public static final String	TX_PROVIDE_TEXT_OUTPUT	=
		"osgi.service;objectClass:List<String>=\"com.ibm.tx."
		+ "jta.TransactionInflowManager\";uses:=\"com.ibm.tx.jta\",osgi.service;obj"
		+ "ectClass:List<String>=\"com.ibm.tx.remote.RemoteTransactionController\""
		+ ";uses:=\"com.ibm.tx.remote\",osgi.service;objectClass:List<String>=\"com"
		+ ".ibm.ws.transaction.services.TransactionObjectFactory,javax.naming.sp"
		+ "i.ObjectFactory\";uses:=\"com.ibm.ws.transaction.services,javax.naming."
		+ "spi\",osgi.service;objectClass:List<String>=\"com.ibm.ws.tx.embeddable."
		+ "EmbeddableWebSphereUserTransaction,jakarta.transaction.UserTransaction\""
		+ ";uses:=\"com.ibm.ws.tx.embeddable,jakarta.transaction\",osgi.service;obje"
		+ "ctClass:List<String>=\"com.ibm.ws.uow.UOWScopeCallback\";uses:=\"com.ibm"
		+ ".ws.uow\",osgi.service;objectClass:List<String>=\"com.ibm.wsspi.injecti"
		+ "onengine.ObjectFactoryInfo\";uses:=\"com.ibm.wsspi.injectionengine\",osg"
		+ "i.service;objectClass:List<String>=\"com.ibm.wsspi.uow.UOWManager\";use"
		+ "s:=\"com.ibm.wsspi.uow\",osgi.service;objectClass:List<String>=\"jakarta.t"
		+ "ransaction.TransactionSynchronizationRegistry\";uses:=\"jakarta.transacti"
		+ "on\"";

	public static final String  TX_REQUIRE_ATTRIBUTE_NAME = "Require-Capability";

	public static final String	TX_REQUIRE_TEXT_INPUT	= "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""
		+ ",osgi.service;filter:=\"(objectClass=com.ibm.tx.util.TMService)\";effec"
		+ "tive:=active,osgi.service;filter:=\"(objectClass=com.ibm.ws.Transactio"
		+ "n.UOWCurrent)\";effective:=active,osgi.service;filter:=\"(objectClass=c"
		+ "om.ibm.ws.transaction.services.TransactionJavaColonHelper)\";effective"
		+ ":=active,osgi.service;filter:=\"(objectClass=javax.transaction.Transac"
		+ "tionManager)\";effective:=active,osgi.extender;filter:=\"(&(osgi.extend"
		+ "er=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))\"";

	public static final String	TX_REQUIRE_TEXT_OUTPUT	= "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""
		+ ",osgi.service;filter:=\"(objectClass=com.ibm.tx.util.TMService)\";effec"
		+ "tive:=active,osgi.service;filter:=\"(objectClass=com.ibm.ws.Transactio"
		+ "n.UOWCurrent)\";effective:=active,osgi.service;filter:=\"(objectClass=c"
		+ "om.ibm.ws.transaction.services.TransactionJavaColonHelper)\";effective"
		+ ":=active,osgi.service;filter:=\"(objectClass=jakarta.transaction.Transac"
		+ "tionManager)\";effective:=active,osgi.extender;filter:=\"(&(osgi.extend"
		+ "er=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))\"";

	@Test
	public void testTransactionAttributes() {
		// Use the common manifest action; this test doesn't care about
		// bundle identity updates, which is all that different about the
		// transaction manifest action.

		ManifestActionImpl_Test manifestAction = getManifestAction();

		String txProvideOutput = manifestAction.callReplacePackages(TX_PROVIDE_ATTRIBUTE_NAME, TX_PROVIDE_TEXT_INPUT);
		assertThat(txProvideOutput).as("'Provide-Capability' transform failure")
			.isEqualTo(TX_PROVIDE_TEXT_OUTPUT);

		String txRequireOutput = manifestAction.callReplacePackages(TX_REQUIRE_ATTRIBUTE_NAME, TX_REQUIRE_TEXT_INPUT);
		assertThat(txRequireOutput).as("'Require-Capability' transform failure")
			.isEqualTo(TX_REQUIRE_TEXT_OUTPUT);
	}

	//

	public static final String DYNAMIC_IMPORT_PACKAGE = "DynamicImport-Package";
	public static final String IMPORT_PACKAGE = "Import-Package";
	public static final String EXPORT_PACKAGE = "Export-Package";

	public static final String SPECIFIC_ATTRIBUTE_INPUT =
		"javax.servlet;version=\"4.0.0\"" +
		",javax.servlet.annotation;version=\"4.0.0\"" +
		",javax.servlet.http;version=\"4.0.0\"" +
		",javax.annotation.security;version=\"4.0.0\"";

	// Generic:                    "[2.6, 6.0)"
	//   (does not include servlet.http)
	// Import (servlet):           "5.0"
	// Import (servlet.http)       "6.0"
	// Export (servlet.annotation) "[3.0, 6.0)"

	// No overrides.  All use the generic version update.
	public static final String SPECIFIC_ATTRIBUTE_DYNAMIC_OUTPUT =
		"jakarta.servlet;version=\"[2.6,6.0)\"" +
		",jakarta.servlet.annotation;version=\"[2.6,6.0)\"" +
		",jakarta.servlet.http;version=\"4.0.0\"" +
		",jakarta.annotation.security;version=\"4.0.0\"";

	// "jakarta.servlet" is overridden.
	public static final String SPECIFIC_ATTRIBUTE_IMPORT_OUTPUT =
		"jakarta.servlet;version=\"5.0\"" +
		",jakarta.servlet.annotation;version=\"[2.6,6.0)\"" +
		",jakarta.servlet.http;version=\"6.0\"" +
		",jakarta.annotation.security;version=\"4.0.0\"";

	// "jakarta.servlet.annotation" is overridden.
	public static final String SPECIFIC_ATTRIBUTE_EXPORT_OUTPUT =
		"jakarta.servlet;version=\"6.0\"" +														//
			",jakarta.servlet.annotation;version=\"6.0\"" +										//
			",jakarta.servlet.http;version=\"6.0\"" +											//
		",jakarta.annotation.security;version=\"4.0.0\"";

	@Test
	public void testSpecificAttributes() {
		ManifestActionImpl_Test specificManifestAction = getSpecificManifestAction();

		String dynamicOutput = specificManifestAction.callReplacePackages(DYNAMIC_IMPORT_PACKAGE, SPECIFIC_ATTRIBUTE_INPUT);
		assertThat(dynamicOutput).as("'DynamicImport-Package' transform failure")
			.isEqualTo(SPECIFIC_ATTRIBUTE_DYNAMIC_OUTPUT);

		String importOutput = specificManifestAction.callReplacePackages(IMPORT_PACKAGE, SPECIFIC_ATTRIBUTE_INPUT);
		assertThat(importOutput).as("'Import-Packages' transform failure")
			.isEqualTo(SPECIFIC_ATTRIBUTE_IMPORT_OUTPUT);

		String exportOutput = specificManifestAction.callReplacePackages(EXPORT_PACKAGE, SPECIFIC_ATTRIBUTE_INPUT);
		assertThat(exportOutput).as("'Export-Packages' transform failure")
			.isEqualTo(SPECIFIC_ATTRIBUTE_EXPORT_OUTPUT);
	}

	public static final String MANIFEST_PATH_SPECIFIC =
		"specific/META-INF/MANIFEST.MF";

	public static final String MANIFEST_PATH_SPECIFIC_EXPECTED =
		"specific/META-INF/MANIFEST.MF.EXPECTED";

	@Test
	public void testTransformManifest_Specific() throws TransformException, IOException {
		testTransform(
			MANIFEST_PATH_SPECIFIC,
			MANIFEST_PATH_SPECIFIC_EXPECTED,
			UNUSED_OCCURRENCES,
			UNUSED_IDENTITY_UPDATES,
			getSpecificJakartaManifestAction());
	}
}
