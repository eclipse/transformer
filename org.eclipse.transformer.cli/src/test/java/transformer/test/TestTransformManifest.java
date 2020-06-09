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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.BundleDataImpl;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.InputStreamData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import transformer.test.util.CaptureLoggerImpl;

public class TestTransformManifest extends CaptureTest {

	public static final String JAVAX_ANNOTATION = "javax.annotation";
	public static final String JAVAX_ANNOTATION_SECURITY = "javax.annotation.security";	

	public static final String JAKARTA_ANNOTATION = "jakarta.annotation";
	public static final String JAKARTA_ANNOTATION_SECURITY = "jakarta.annotation.security";
	
	public static final String JAVAX_SERVLET = "javax.servlet";
	public static final String JAVAX_SERVLET_ANNOTATION = "javax.servlet.annotation";
	public static final String JAVAX_SERVLET_DESCRIPTOR = "javax.servlet.descriptor";
	public static final String JAVAX_SERVLET_HTTP = "javax.servlet.http";
	public static final String JAVAX_SERVLET_RESOURCES = "javax.servlet.resources";
	public static final String JAVAX_SERVLET_SCI = "javax.servlet.ServletContainerInitializer";

	public static final String JAKARTA_SERVLET = "jakarta.servlet";
	public static final String JAKARTA_SERVLET_ANNOTATION = "jakarta.servlet.annotation";
	public static final String JAKARTA_SERVLET_DESCRIPTOR = "jakarta.servlet.descriptor";
	public static final String JAKARTA_SERVLET_HTTP = "jakarta.servlet.http";
	public static final String JAKARTA_SERVLET_RESOURCES = "jakarta.servlet.resources";
	public static final String JAKARTA_SERVLET_SCI = "jakarta.servlet.ServletContainerInitializer";

	public static final String JAVAX_TRANSACTION = "javax.transaction";
	public static final String JAVAX_TRANSACTION_XA = "javax.transaction.xa";
	public static final String JAVAX_TRANSACTION_TM = "javax.transaction.TransactionManager";
	public static final String JAVAX_TRANSACTION_TSR = "javax.transaction.TransactionSynchronizationRegistry";
	public static final String JAVAX_TRANSACTION_UT = "javax.transaction.UserTransaction";

	public static final String JAKARTA_TRANSACTION = "jakarta.transaction";
	public static final String JAKARTA_TRANSACTION_XA = "jakarta.transaction.xa";
	public static final String JAKARTA_TRANSACTION_TM = "jakarta.transaction.TransactionManager";
	public static final String JAKARTA_TRANSACTION_TSR = "jakarta.transaction.TransactionSynchronizationRegistry";
	public static final String JAKARTA_TRANSACTION_UT = "jakarta.transaction.UserTransaction";
	
	public static final String JAKARTA_SERVLET_VERSION = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_ANNOTATION_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_DESCRIPTOR_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_HTTP_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_RESOURCES_VERSION  = "[2.6, 6.0)";
	
	public Set<String> getIncludes() {
		return Collections.emptySet();
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(JAVAX_ANNOTATION, JAKARTA_ANNOTATION);
			packageRenames.put(JAVAX_ANNOTATION_SECURITY, JAKARTA_ANNOTATION_SECURITY);

			packageRenames.put(JAVAX_SERVLET, JAKARTA_SERVLET);
			packageRenames.put(JAVAX_SERVLET_ANNOTATION, JAKARTA_SERVLET_ANNOTATION);
			packageRenames.put(JAVAX_SERVLET_DESCRIPTOR, JAKARTA_SERVLET_DESCRIPTOR);
			packageRenames.put(JAVAX_SERVLET_HTTP, JAKARTA_SERVLET_HTTP);
			packageRenames.put(JAVAX_SERVLET_RESOURCES, JAKARTA_SERVLET_RESOURCES);
			packageRenames.put(JAVAX_SERVLET_SCI, JAKARTA_SERVLET_SCI);

			packageRenames.put(JAVAX_TRANSACTION, JAKARTA_TRANSACTION);
			// Do not rename javax.transaction.xa
			packageRenames.put(JAVAX_TRANSACTION_TM, JAKARTA_TRANSACTION_TM);
			packageRenames.put(JAVAX_TRANSACTION_TSR, JAKARTA_TRANSACTION_TSR);
			packageRenames.put(JAVAX_TRANSACTION_UT, JAKARTA_TRANSACTION_UT);
		}
		return packageRenames;
	}
	
	protected Map<String, String> packageVersions;
	
	public Map<String, String> getPackageVersions() {
		if ( packageVersions == null ) {
			packageVersions = new HashMap<String, String>();
			packageVersions.put(JAVAX_SERVLET,            JAKARTA_SERVLET_VERSION );
			packageVersions.put(JAVAX_SERVLET_ANNOTATION, JAKARTA_SERVLET_ANNOTATION_VERSION );
			packageVersions.put(JAVAX_SERVLET_DESCRIPTOR, JAKARTA_SERVLET_DESCRIPTOR_VERSION );
			packageVersions.put(JAVAX_SERVLET_HTTP,       JAKARTA_SERVLET_HTTP_VERSION );
			packageVersions.put(JAVAX_SERVLET_RESOURCES,  JAKARTA_SERVLET_RESOURCES_VERSION );		
		}
		return packageVersions;
	}

	//

	public static final String WEBCONTAINER_SYMBOLIC_NAME =
		"com.ibm.ws.webcontainer";
	public static final String WEBCONTAINER_BUNDLE_TEXT =
		"com.ibm.ws.webcontainer.jakarta,2.0,+\" Jakarta\",+\"; Jakarta Enabled\"";

	public static final String[][] WEBCONTAINER_BUNDLE_OUTPUT = new String[][] {
		{ "Bundle-SymbolicName: ", "com.ibm.ws.webcontainer.jakarta" },
		{ "Bundle-Version: ", "2.0" },
		{ "Bundle-Name: ", "WAS WebContainer Jakarta"},
		{ "Bundle-Description: ", "WAS WebContainer 8.0 with Servlet 3.0 support; Jakarta Enabled" }
	};

	//

	public static final String TRANSACTION_SYMBOLIC_NAME =
		"com.ibm.ws.transaction";

	public static final String WILDCARD_SYMBOLIC_NAME =
		"*";

	public static final String WILDCARD_BUNDLE_TEXT =
		"*.jakarta,2.0,+\" Jakarta\",+\"; Jakarta Enabled\"";

	public static final String[][] TRANSACTION_BUNDLE_OUTPUT = new String[][] {
		{ "Bundle-SymbolicName: ", "com.ibm.ws.transaction.jakarta" },

		// Not changed: Wildcard identity updates to not update the version
		{ "Bundle-Version: ", "1.0.40.202005041216" },

		{ "Bundle-Name: ", "Transaction Jakarta"},
		{ "Bundle-Description: ", "Transaction support, version 1.0; Jakarta Enabled" }
	};

	//

	public static final BundleData WEBCONTAINER_BUNDLE_DATA =
		new BundleDataImpl(WEBCONTAINER_BUNDLE_TEXT);

	protected Map<String, BundleData> bundleUpdates;

	public Map<String, BundleData> getBundleUpdates() {
		if ( bundleUpdates == null ) {
			bundleUpdates = new HashMap<String, BundleData>();
			bundleUpdates.put(WEBCONTAINER_SYMBOLIC_NAME, WEBCONTAINER_BUNDLE_DATA);
		}
		return bundleUpdates;
	}

	//

	public static final BundleData WILDCARD_BUNDLE_DATA =
		new BundleDataImpl(WILDCARD_BUNDLE_TEXT);

	protected Map<String, BundleData> bundleUpdatesTx;

	public Map<String, BundleData> getBundleUpdatesTx() {
		if ( bundleUpdatesTx == null ) {
			bundleUpdatesTx = new HashMap<String, BundleData>();
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
		if ( jakartaManifestAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			jakartaManifestAction = new ManifestActionImpl(
				useLogger, false, false,
				new InputBufferImpl(),
				new SelectionRuleImpl( useLogger, getIncludes(), getExcludes() ),
				new SignatureRuleImpl(
					useLogger,
					getPackageRenames(), getPackageVersions(),
					getBundleUpdates(),
					null,
					getDirectStrings() ),
				ManifestActionImpl.IS_MANIFEST );
		}
		return jakartaManifestAction;
	}

	public ManifestActionImpl jakartaFeatureAction;

	public ManifestActionImpl getJakartaFeatureAction() {
		if ( jakartaFeatureAction == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			jakartaFeatureAction = new ManifestActionImpl(
				useLogger, false, false,
				new InputBufferImpl(),
				new SelectionRuleImpl( useLogger, getIncludes(), getExcludes() ),
				new SignatureRuleImpl( useLogger, getPackageRenames(), getPackageVersions(), null, null, null ),
				ManifestActionImpl.IS_FEATURE );
		}

		return jakartaFeatureAction;
	}

	//

	public ManifestActionImpl jakartaManifestActionTx;

	public ManifestActionImpl getJakartaManifestActionTx() {
		if ( jakartaManifestActionTx == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			jakartaManifestActionTx = new ManifestActionImpl(
				useLogger, false, false,
				new InputBufferImpl(),
				new SelectionRuleImpl( useLogger, getIncludes(), getExcludes() ),
				new SignatureRuleImpl(
					useLogger,
					getPackageRenames(), getPackageVersions(),
					getBundleUpdatesTx(),
					null,
					getDirectStrings() ),
				ManifestActionImpl.IS_MANIFEST );
		}
		return jakartaManifestActionTx;
	}

	//

	protected static final class Occurrences {
		public final String initialTag;
		public final int initialTagInitialCount;
		public final int initialTagFinalCount;

		public final String finalTag;
		public final int finalTagInitialCount;
		public final int finalTagFinalCount;
		
		public Occurrences(
			String initialTag, int initialTagInitialCount,  int initialTagFinalCount,
			String finalTag, int finalTagInitialCount, int finalTagFinalCount) {

			this.initialTag = initialTag;
			this.initialTagInitialCount = initialTagInitialCount;
			this.initialTagFinalCount = initialTagFinalCount;

			this.finalTag = finalTag;
			this.finalTagInitialCount = finalTagInitialCount;
			this.finalTagFinalCount = finalTagFinalCount;
		}
		
		public void verifyInitial(List<String> lines) {
			int actualInitialTagInitial = TestUtils.occurrences(lines, initialTag);
			System.out.println("Tag [ " + initialTag + " ] Expected [ " + initialTagInitialCount + " ] Actual [ " + actualInitialTagInitial + " ]");
			Assertions.assertEquals(initialTagInitialCount, actualInitialTagInitial, initialTag);

			int actualFinalTagInitial = TestUtils.occurrences(lines, finalTag);
			System.out.println("Tag [ " + finalTag + " ] Expected [ " + finalTagInitialCount + " ] Actual [ " + actualFinalTagInitial + " ]");
			Assertions.assertEquals(finalTagInitialCount, actualFinalTagInitial, initialTag);
		}
		
		public void verifyFinal(List<String> lines) {
			int actualInitialTagFinal = TestUtils.occurrences(lines, initialTag);
			System.out.println("Tag [ " + initialTag + " ] Expected [ " + initialTagFinalCount + " ] Actual [ " + actualInitialTagFinal + " ]");
			Assertions.assertEquals(initialTagFinalCount, actualInitialTagFinal, initialTag);

			int actualFinalTagFinal = TestUtils.occurrences(lines, finalTag);
			System.out.println("Tag [ " + finalTag + " ] Expected [ " + finalTagFinalCount + " ] Actual [ " + actualFinalTagFinal + " ]");
			Assertions.assertEquals(finalTagFinalCount, actualFinalTagFinal, initialTag);
		}
	}

	//

	public List<String> displayManifest(String manifestPath, InputStream manifestStream) throws IOException {
		System.out.println("Manifest [ " + manifestPath + " ]");
		List<String> manifestLines = TestUtils.loadLines(manifestStream); // throws IOException

		List<String> collapsedLines = TestUtils.manifestCollapse(manifestLines);

		int numLines = collapsedLines.size();
		for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
			System.out.printf( "[ %3d ] [ %s ]\n", lineNo, collapsedLines.get(lineNo) );
		}

		return collapsedLines;
	}

	public void testTransform(
		String inputPath,
		Occurrences[] occurrences, String[][] identityUpdates,
		ManifestActionImpl manifestAction)
		throws TransformException, IOException {

		System.out.println("Transform [ " + inputPath + " ] using [ " + manifestAction.getName() + " ] ...");

		System.out.println("Read [ " + inputPath + " ]");
		InputStream manifestInput = TestUtils.getResourceStream(inputPath); // throws IOException

		List<String> inputLines = displayManifest(inputPath, manifestInput);

		System.out.println("Verify input [ " + inputPath + " ]");
		for ( Occurrences occurrence : occurrences ) {
			occurrence.verifyInitial(inputLines);
		}

		InputStreamData manifestOutput;
		try ( InputStream input = TestUtils.getResourceStream(inputPath) ) { // throws IOException
			manifestOutput = manifestAction.apply(inputPath, input); // throws JakartaTransformException
		}

		List<String> outputLines = displayManifest(inputPath, manifestOutput.stream);

		System.out.println("Verify output [ " + inputPath + " ]");
		for ( Occurrences occurrence : occurrences ) {
			occurrence.verifyFinal(outputLines);
		}

		if ( identityUpdates != null ) {
			System.out.println("Verify identity update [ " + inputPath + " ]");

			String errorMessage = validate(outputLines, identityUpdates);
			if ( errorMessage != null ) {
				System.out.println("Bundle identity update failure: " + errorMessage);
				Assertions.assertNull(errorMessage, "Bundle identity update failure");
			}
		}

		System.out.println("Transform [ " + inputPath + " ] using [ " + manifestAction.getName() + " ] ... done");
	}

	// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support
	// Bundle-Name: WAS WebContainer
	// Bundle-SymbolicName: com.ibm.ws.webcontainer
	// Bundle-Version: 1.1.35.cl191220191120-0300
	// 
	// com.ibm.ws.webcontainer=com.ibm.ws.webcontainer.jakarta,2.0,+" Jakarta",+"; Jakarta Enabled"
	// 
	// Bundle-SymbolicName: com.ibm.ws.webcontainer.jakarta
	// Bundle-Version: 2.0
	// Bundle-Name: WAS WebContainer Jakarta
	// Bundle-Description: WAS WebContainer 8.0 with Servlet 3.0 support; Jakarta Enabled

	protected String validate(List<String> manifestLines, String[][] expectedOutput) {
		boolean [] matches = new boolean[expectedOutput.length];
		int numMatches = 0;

		int numLines = manifestLines.size();

		System.out.println("Validating updated bundle identity: Lines [ " + numLines + " ]");
		for ( int matchNo = 0; matchNo < expectedOutput.length; matchNo++ ) {
			String prefix = expectedOutput[matchNo][0];
			String suffix = expectedOutput[matchNo][1];
			System.out.println("Match [ " + matchNo + " ] Prefix [ " + prefix + " ] Suffix [ " + suffix + " ]");
		}

		for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
			String line = manifestLines.get(lineNo);

			for ( int matchNo = 0; matchNo < expectedOutput.length; matchNo++ ) {
				String prefix = expectedOutput[matchNo][0];
				String suffix = expectedOutput[matchNo][1];
				
				String message;

				if ( line.startsWith(prefix) ) {
					if ( line.length() != prefix.length() + suffix.length() ) {
						message = "Incorrect line length";
					} else if ( !line.endsWith(suffix) ) {
						message = "Incorrect attribute value";

					} else if ( matches[matchNo] ) {
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

					if ( message != null ) {
						System.out.println("Failed [ " + matchNo + " ]: [ " + message + " ]");
						System.out.println("  Prefix [ " + prefix + " ]");
						System.out.println("  Suffix [ " + suffix + " ]");
						System.out.println("  Line [ " + lineNo + " ] [ " + line + " ]");

						return message;
					}
				}
			}
		}

		if ( numMatches < expectedOutput.length ) {
			return "Located [ " + numMatches + " ] out of [ " + expectedOutput.length + " ] matches";
		} else {
			return null;
		}
	}

	//

	public static final String TEST_MANIFEST_PATH_WEBCONTAINER = "transformer/test/data/servlet/META-INF/MANIFEST.MF";

	public static final Occurrences[] MANIFEST_TO_JAKARTA_DATA = {
		new Occurrences(JAVAX_ANNOTATION, 2, 0, JAKARTA_ANNOTATION, 0, 2),
		new Occurrences(JAVAX_ANNOTATION_SECURITY, 1, 0, JAKARTA_ANNOTATION_SECURITY, 0, 1),

		new Occurrences(JAVAX_SERVLET, 67, 0, JAKARTA_SERVLET, 0, 67),
		new Occurrences(JAVAX_SERVLET_DESCRIPTOR, 3, 0, JAKARTA_SERVLET_DESCRIPTOR, 0, 3),
		new Occurrences(JAVAX_SERVLET_HTTP, 23, 0, JAKARTA_SERVLET_HTTP, 0, 23),
		new Occurrences(JAVAX_SERVLET_RESOURCES, 1, 0, JAKARTA_SERVLET_RESOURCES, 0, 1),
		new Occurrences(JAVAX_SERVLET_SCI, 1, 0, JAKARTA_SERVLET_SCI, 0, 1)
	};

	//

	public static final String TEST_FEATURE_PATH = "transformer/test/data/servlet/META-INF/servlet-4.0.mf";

	public static final Occurrences[] FEATURE_TO_JAKARTA_DATA = {
		// EMPTY
	};

	//

	@Test
	public void testTransformManifest_Servlet() throws TransformException, IOException {
		testTransform(
			TEST_MANIFEST_PATH_WEBCONTAINER,
			MANIFEST_TO_JAKARTA_DATA, WEBCONTAINER_BUNDLE_OUTPUT,
			getJakartaManifestAction());
		// throws JakartaTransformException, IOException
	}

	@Test
	public void testTransformFeature_Servlet() throws TransformException, IOException {
		testTransform(
			TEST_FEATURE_PATH,
			FEATURE_TO_JAKARTA_DATA, null,
			getJakartaFeatureAction());
		// throws JakartaTransformException, IOException
	}

	//

	public static final String TEST_MANIFEST_PATH_TX = "transformer/test/data/transaction/META-INF/MANIFEST.MF";

	public static final Occurrences[] MANIFEST_TO_JAKARTA_DATA_TX = {
		new Occurrences(JAVAX_ANNOTATION, 1, 0, JAKARTA_ANNOTATION, 0, 1),
		new Occurrences(JAVAX_ANNOTATION_SECURITY, 0, 0, JAKARTA_ANNOTATION_SECURITY, 0, 0), 

		new Occurrences(JAVAX_SERVLET, 4, 0, JAKARTA_SERVLET, 0, 4),
		new Occurrences(JAVAX_SERVLET_HTTP, 2, 0, JAKARTA_SERVLET_HTTP, 0, 2),

		new Occurrences(JAVAX_TRANSACTION, 9, 2, JAKARTA_TRANSACTION, 0, 7), // The two '.xa' are not transformed.
		new Occurrences(JAVAX_TRANSACTION_XA, 2, 2, JAKARTA_TRANSACTION_XA, 0, 0),
		new Occurrences(JAVAX_TRANSACTION_UT, 1, 0, JAKARTA_TRANSACTION_UT, 0, 1),
		new Occurrences(JAVAX_TRANSACTION_TM, 1, 0, JAKARTA_TRANSACTION_TM, 0, 1),
		new Occurrences(JAVAX_TRANSACTION_TSR, 1, 0, JAKARTA_TRANSACTION_TSR, 0, 1)
	};

	@Test
	public void testTransformManifest_Transaction() throws TransformException, IOException {
		testTransform(
			TEST_MANIFEST_PATH_TX,
			MANIFEST_TO_JAKARTA_DATA_TX, TRANSACTION_BUNDLE_OUTPUT,
			getJakartaManifestActionTx());

		// throws JakartaTransformException, IOException
	}

    String newVersion = "[4.0,5)";

	// Embedding text is the input for each test
    String embeddingText0 = "; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"[1.0.0,1.0.200)\"";    
	String embeddingText1 = ";version=\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText2 = ";version= \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText3 = ";version =\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText4 = ";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String embeddingText5 = ";version = \"[2.6,3)\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText6 = ";resolution:=\"optional\";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String embeddingText7 = ";version=\"[2.6,3)\"";
	String embeddingText8 = "";
	String embeddingText9 = ",";
	String embeddingText10 = ";resolution:=\"optional\"";   // no version
	String embeddingText11 = ",javax.servlet.annotation;version=\"[2.6,3)\""; // leading comma
	String embeddingText12 = ";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\"";  // missing quote after version
	String embeddingText13 = "\",com.ibm.ws.webcontainer.core;version=\"1.1.0\""; // first char is a quote (no package attributes)

	// Expected results: When replacing the version, the expected result is the entire 
	// embedding text with the version of the first package replaced with the new version.
	String expectedResultText0_ReplaceVersion = "; location:=\"dev/api/spec/,lib/\"; mavenCoordinates=\"javax.servlet:javax.servlet-api:4.0.1\"; version=\"" + newVersion +"\"";
	String expectedResultText1_ReplaceVersion = ";version=\""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText2_ReplaceVersion = ";version= \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText3_ReplaceVersion = ";version =\""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText4_ReplaceVersion = ";version = \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String expectedResultText5_ReplaceVersion = ";version = \""+ newVersion + "\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText6_ReplaceVersion = ";resolution:=\"optional\";version = \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText7_ReplaceVersion = ";version=\""+ newVersion + "\"";
	String expectedResultText8_ReplaceVersion = "";
	String expectedResultText9_ReplaceVersion = ",";
	String expectedResultText10_ReplaceVersion = ";resolution:=\"optional\"";
	String expectedResultText11_ReplaceVersion = ",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText12_ReplaceVersion = ";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\""; // missing quote (no version replacement)
	String expectedResultText13_ReplaceVersion = "\",com.ibm.ws.webcontainer.core;version=\"1.1.0\""; 

	// Expected results: When getting package attributes, expected result is 
	// just the package attribute text which is at the beginning of the embedding text
	String expectedResultText1_GetPackageText = ";version=\"[2.6,3)\",";
	String expectedResultText2_GetPackageText = ";version= \"[2.6,3)\",";
	String expectedResultText3_GetPackageText = ";version =\"[2.6,3)\",";
	String expectedResultText4_GetPackageText = ";version = \"[2.6,3)\",";	
	String expectedResultText5_GetPackageText = ";version = \"[2.6,3)\";resolution:=\"optional\",";	
	String expectedResultText6_GetPackageText = ";resolution:=\"optional\";version = \"[2.6,3)\",";	
	String expectedResultText7_GetPackageText = ";version=\"[2.6,3)\"";
	String expectedResultText8_GetPackageText = "";  //empty string produces empty string
	String expectedResultText9_GetPackageText = "";  // comma produces empty string
	String expectedResultText10_GetPackageText = ";resolution:=\"optional\"";
	String expectedResultText11_GetPackageText = ""; // leading comma followed by package is empty string
	String expectedResultText12_GetPackageText = ";version=\"[2.6,3),"; // missing quote (no version replacement)
	String expectedResultText13_GetPackageText = "";  //Not starting with ';' produces empty string

	/**
	 * Subclass which allows us to call protected methods of ManifestActionImpl
	 */
	class ManifestActionImpl_Test extends ManifestActionImpl {
		public ManifestActionImpl_Test (
			Logger logger, boolean isTerse, boolean isVerbose,
			InputBufferImpl buffer,
			SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule,
			boolean isManifest) {

			super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule, isManifest);
		}

		public boolean callIsTrueMatch(String text, int matchStart, int keyLen ) {
			return SignatureRuleImpl.isTruePackageMatch(text, matchStart, keyLen, false );
		}

		public String callReplacePackages(String text) {
			return replacePackages(text);
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
		if ( manifestAction_test == null ) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			manifestAction_test = new ManifestActionImpl_Test(
				useLogger, false, false,
				new InputBufferImpl(),
				new SelectionRuleImpl( useLogger, getIncludes(), getExcludes() ), 
				new SignatureRuleImpl( useLogger, getPackageRenames(), getPackageVersions(), null, null, null ),
				ManifestActionImpl.IS_MANIFEST );
		}

		return manifestAction_test;
	}

	/**
	 * Test the ManifestActionImpl.isTrueMatch(...) method, which verifies that
	 * the package found is not part of a larger package name.  
	 * For example when searching for javax.servelet, the following are NOT true
	 * matches:
	 *      my.javax.servlet
	 *      javax.servlet.http
	 */
	@Test
	public void testIsTrueMatch() {
		ManifestActionImpl_Test manifestAction = getManifestAction();
		
		boolean result;
		
		//  *** TEST CASES ****
        // 1.  myPackage            YES  -- package is exact length of search text
        //
        // 2.  myPackage.           NO   -- trailing '.' indicates part of a larger package
        // 3.  myPackage,           YES  -- trailing ',' not part of a package name
        // 4.  myPackage;           YES  -- trailing ';' not part of a package name
        // 5.  myPackage$           NO   -- trailing '$' indicates part of a larger package
        // 6.  myPackage=           YES  -- trailing '=' not part of a package name, but likely busted syntax
        // 7.  "myPackage" + " "    YES  -- trailing ' ' not part of a package name
        //
        // 8.  =myPackage           YES  -- leading '=' not part of a package name, but likely busted syntax
        // 9.  .myPackage           NO   -- leading '.' indicates part of a larger package
        //10.  ,myPackage           YES  -- leading ',' not part of a package name
        //11.  ;myPackage           YES  -- leading ';' not part of a package name
        //12.  $myPackage           NO   -- leading '$' indicates part of a larger package
        //13.  " " + myPackage      YES  -- leading ' ' not part of a package name
        //
        //14.  myPachagePlus        NO   -- trailing valid java identifier indicates part of a larger package
        //15.  plusmyPackage        NO   -- leading valid java identifier indicates part of a larger package

		
		final String TEXT = "=abc.defgh,ijklm;nopqrs$tuv wxyz= ";
		int matchStart;
		int keyLen;
		
	    matchStart = 0;  // Test 1
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch("abc", matchStart, keyLen);
		assertTrue(result, "(Package name == text) is MATCH");    
		
	    matchStart = 1;  // Test 2 "abc"   Trailing period   
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('.' after key) is NO MATCH");
	
	    matchStart = 1; //  Test 3 "abc.defgh"   Trailing comma 
	    keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(',' after key) is MATCH");	
		
	    matchStart = 11; //  Test 4 "ijklm"   Trailing semicolon 
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(';' after key) is MATCH");
		
	    matchStart = 17; //  Test 5 "nopqrs"   Trailing $ 
	    keyLen = 6;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('$' after key) is MATCH");
		
	    matchStart = 28; //  Test 6 "wxyz"   Trailing = 
	    keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "('=' after key) is MATCH");
		
	    matchStart = 17; //  Test 7 "nopqrs$tuv"   Trailing ' ' 
	    keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(' ' after key) is MATCH");
		
	    matchStart = 1; //  Test 8 "abc.defgh"   Prior char "="
	    keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "('=' before key) is MATCH");
				
	    matchStart = 5; //  Test 9 "defgh"   Prior char "."
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('.' before key) is NO MATCH");
		
	    matchStart = 11; //  Test 10 "ijklm"   Prior char ","
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(',' before key) is MATCH");
		
	    matchStart = 17; //  Test 11 "nopqrs$tuv"   Prior char ";"
	    keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(';' before key) is MATCH");
		
	    matchStart = 24; //  Test 12 "tuv"   Prior char "$"
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "('$' before key) is NO MATCH");
				
	    matchStart = 28; //  Test 13 "wxyz"   Prior char " "
	    keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertTrue(result, "(' ' before key) is MATCH");
			
	    matchStart = 17; //  Test 14 "no"   char after is a valid package char
	    keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "(valid package character after key) is NO MATCH");
		
	    matchStart = 8; //  Test 15 "gh"   char before is a valid package char
	    keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, matchStart, keyLen);
		assertFalse(result, "(valid package character before key) is NO MATCH");
	}
	
	@Test
	public void testReplacePackageVersionInEmbeddingText() {
		ManifestActionImpl_Test manifestAction = getManifestAction();

		String failureText = "Package version transformation failure";
		String result;

		result = manifestAction.callReplacePackageVersion(embeddingText0, newVersion);
		assertEquals(expectedResultText0_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText1, newVersion);
		assertEquals(expectedResultText1_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText2, newVersion);
		assertEquals(expectedResultText2_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText3, newVersion);
		assertEquals(expectedResultText3_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText4, newVersion);
		assertEquals(expectedResultText4_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText5, newVersion);
		assertEquals(expectedResultText5_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText6, newVersion);
		assertEquals(expectedResultText6_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText7, newVersion);
		assertEquals(expectedResultText7_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText9, newVersion);
		assertEquals(expectedResultText9_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText10, newVersion);
		assertEquals(expectedResultText10_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText11, newVersion);
		assertEquals(expectedResultText11_ReplaceVersion, result, failureText);

        // Check syntax error in Manifest (Case of no closing quotes)
		result = manifestAction.callReplacePackageVersion(embeddingText12, newVersion);
		assertEquals(expectedResultText12_ReplaceVersion, result, failureText);

		result = manifestAction.callReplacePackageVersion(embeddingText13, newVersion);
		assertEquals(expectedResultText13_ReplaceVersion, result, failureText);
	}

	@Test
	public void testGetPackageAttributeText() {
		ManifestActionImpl_Test manifestAction = getManifestAction();

		String failureText = "Package attribute transformation failure";

		String result;

		result = manifestAction.callGetPackageAttributeText(embeddingText1);		
		assertEquals(expectedResultText1_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText2);
		assertEquals(expectedResultText2_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText3);
		assertEquals(expectedResultText3_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText4);
		assertEquals(expectedResultText4_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText5);
		assertEquals(expectedResultText5_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText7);
		assertEquals(expectedResultText7_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText8);
		assertEquals(expectedResultText8_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText9);
		assertEquals(expectedResultText9_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText10);
		assertEquals(expectedResultText10_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText11);
		assertEquals(expectedResultText11_GetPackageText, result, failureText);

		// Check syntax error in Manifest (Case of no closing quotes)		
		result = manifestAction.callGetPackageAttributeText(embeddingText12);
		assertEquals(expectedResultText12_GetPackageText, result, failureText);

		result = manifestAction.callGetPackageAttributeText(embeddingText13);
		assertEquals(expectedResultText13_GetPackageText, result, failureText);
	}

	//

	public static final String	TX_PROVIDE_TEXT_INPUT =
		"Provide-Capability: osgi.service;objectClass:List<String>=\"com.ibm.tx."
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

	public static final String	TX_PROVIDE_TEXT_OUTPUT =
		"Provide-Capability: osgi.service;objectClass:List<String>=\"com.ibm.tx."
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

	public static final String	TX_REQUIRE_TEXT_INPUT =
	    "Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""
		+ ",osgi.service;filter:=\"(objectClass=com.ibm.tx.util.TMService)\";effec"
		+ "tive:=active,osgi.service;filter:=\"(objectClass=com.ibm.ws.Transactio"
		+ "n.UOWCurrent)\";effective:=active,osgi.service;filter:=\"(objectClass=c"
		+ "om.ibm.ws.transaction.services.TransactionJavaColonHelper)\";effective"
		+ ":=active,osgi.service;filter:=\"(objectClass=javax.transaction.Transac"
		+ "tionManager)\";effective:=active,osgi.extender;filter:=\"(&(osgi.extend"
		+ "er=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))\"";

	public static final String	TX_REQUIRE_TEXT_OUTPUT =
		"Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""
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
		// bundle identity updates, which is all that different about the transaction
		// manifest action.

		ManifestActionImpl_Test manifestAction = getManifestAction();

		String txProvideOutput = manifestAction.callReplacePackages(TX_PROVIDE_TEXT_INPUT);
		assertEquals(TX_PROVIDE_TEXT_OUTPUT, txProvideOutput, "'Provide-Capability' transform failure"); 

		String txRequireOutput = manifestAction.callReplacePackages(TX_REQUIRE_TEXT_INPUT);
		assertEquals(TX_REQUIRE_TEXT_OUTPUT, txRequireOutput, "'Require-Capability' transform failure"); 
	}
}
