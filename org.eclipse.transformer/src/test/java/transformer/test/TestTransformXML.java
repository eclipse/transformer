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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.action.impl.TextActionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transformer.test.util.CaptureLoggerImpl;

public class TestTransformXML extends CaptureTest {
	private Properties prior;

	@BeforeEach
	public void setUp() {
		prior = new Properties();
		prior.putAll(System.getProperties());
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	public static final String	TEST_DATA_PREFIX						= "transaction";

	public static final String	UTSERVICE_XML_SIMPLE_NAME				= "UTService.xml";
	public static final String	UTSERVICE_XML_PATH						= TEST_DATA_PREFIX + '/' + "OSGI-INF" + '/'
		+ UTSERVICE_XML_SIMPLE_NAME;

	public static final String	JAVAX_TRANSACTION_USER_TRANACTION		= "javax.transaction.UserTransaction";
	public static final String	JAKARTA_TRANSACTION_USER_TRANSACTION	= "jakarta.transaction.UserTransaction";

	public static final String	TRANSACTION_MANAGER_XML_SIMPLE_NAME		= "TransactionManager.xml";
	public static final String	TRANSACTION_MANAGER_XML_PATH			= TEST_DATA_PREFIX + '/' + "OSGI-INF" + '/'
		+ TRANSACTION_MANAGER_XML_SIMPLE_NAME;

	public static final String	JAVAX_TRANSACTION_TRANSACTION_MANAGER	= "javax.transaction.TransactionManager";
	public static final String	JAKARTA_TRANSACTION_TRANSACTION_MANAGER	= "jakarta.transaction.TransactionManager";

	//

	public Map<String, String> getIncludes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getExcludes() {
		return Collections.emptyMap();
	}

	//

	public Map<String, Map<String, String>> masterXmlUpdates;

	public Map<String, Map<String, String>> getMasterXmlUpdates() {
		if (masterXmlUpdates == null) {
			Map<String, Map<String, String>> useXmlUpdates = new HashMap<>(2);

			Map<String, String> utServiceUpdates = new HashMap<>(1);
			utServiceUpdates.put(JAVAX_TRANSACTION_USER_TRANACTION, JAKARTA_TRANSACTION_USER_TRANSACTION);
			useXmlUpdates.put(UTSERVICE_XML_SIMPLE_NAME, utServiceUpdates);

			Map<String, String> tmServiceUpdates = new HashMap<>(1);
			tmServiceUpdates.put(JAVAX_TRANSACTION_TRANSACTION_MANAGER, JAKARTA_TRANSACTION_TRANSACTION_MANAGER);
			useXmlUpdates.put(TRANSACTION_MANAGER_XML_SIMPLE_NAME, tmServiceUpdates);

			masterXmlUpdates = useXmlUpdates;
		}

		return masterXmlUpdates;
	}

	//

	public TextActionImpl textAction;

	public TextActionImpl getTextAction() {
		if (textAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				new SelectionRuleImpl(useLogger, getIncludes(), getExcludes()),
				new SignatureRuleImpl(useLogger, null, null, null, null, getMasterXmlUpdates(), null,
					Collections.emptyMap()));
			textAction = new TextActionImpl(context);
		}

		return textAction;
	}

	//

	protected static final class Occurrences {
		public final String	tag;
		public final int	count;

		public Occurrences(String tag, int count) {
			this.tag = tag;
			this.count = count;
		}
	}

	public static final Occurrences[]	UT_INITIAL_OCCURRENCES	= {
		new Occurrences(JAVAX_TRANSACTION_USER_TRANACTION, 1), new Occurrences(JAKARTA_TRANSACTION_USER_TRANSACTION, 0),
	};

	public static final Occurrences[]	UT_FINAL_OCCURRENCES	= {
		new Occurrences(JAVAX_TRANSACTION_USER_TRANACTION, 0), new Occurrences(JAKARTA_TRANSACTION_USER_TRANSACTION, 1),
	};

	public static final Occurrences[]	TM_INITIAL_OCCURRENCES	= {
		new Occurrences(JAVAX_TRANSACTION_TRANSACTION_MANAGER, 1),
		new Occurrences(JAKARTA_TRANSACTION_TRANSACTION_MANAGER, 0),
	};

	public static final Occurrences[]	TM_FINAL_OCCURRENCES	= {
		new Occurrences(JAVAX_TRANSACTION_TRANSACTION_MANAGER, 0),
		new Occurrences(JAKARTA_TRANSACTION_TRANSACTION_MANAGER, 1),
	};

	//

	public List<String> display(String resourceRef, InputStream resourceStream) throws IOException {
		System.out.println("Resource [ " + resourceRef + " ]");
		List<String> lines = TestUtils.loadLines(resourceStream);

		int numLines = lines.size();
		for (int lineNo = 0; lineNo < numLines; lineNo++) {
			System.out.printf("[ %3d ] [ %s ]\n", lineNo, lines.get(lineNo));
		}

		return lines;
	}

	public void testTransform(String resourceRef, Occurrences[] initialOccurrences, Occurrences[] finalOccurrences)
		throws TransformException, IOException {

		System.out.println("Transform [ " + resourceRef + " ] ...");

		List<String> initialLines;
		try (InputStream resourceInput = TestUtils.getResourceStream(resourceRef)) {
			initialLines = display(resourceRef, resourceInput);
		}

		TextActionImpl useTextAction = getTextAction();
		System.out.println("Transform [ " + resourceRef + " ] using [ " + useTextAction.getName() + " ]");

		List<String> finalLines;
		try (InputStream resourceInput = TestUtils.getResourceStream(resourceRef)) {
			ByteData xmlOutput = useTextAction.apply(useTextAction.collect(resourceRef, resourceInput));
			finalLines = display(resourceRef, xmlOutput.stream());
		}

		verify(resourceRef, "initial lines", initialOccurrences, initialLines);
		verify(resourceRef, "final lines", finalOccurrences, finalLines);

		System.out.println("Transform [ " + resourceRef + " ] ... OK");
	}

	public void verify(String resourceRef, String caseTag, Occurrences[] occurrences, List<String> lines) {

		System.out.println("Verify [ " + resourceRef + " ] [ " + caseTag + " ] ...");

		for (Occurrences occurrence : occurrences) {
			String occurrenceTag = occurrence.tag;
			int expected = occurrence.count;

			int actual = TestUtils.occurrences(lines, occurrenceTag);

			if (expected != actual) {
				Assertions.assertEquals(expected, actual, "Resource [ " + resourceRef + " ] [ " + caseTag
					+ " ] Value [ " + occurrenceTag + " ] Expected [ " + expected + " ] Actual [ " + actual + " ]");
			}
		}

		System.out.println("Verify [ " + resourceRef + " ] [ " + caseTag + " ] ... done");
	}

	//

	@Test
	public void testTransform_UTServiceXml() throws TransformException, IOException {
		testTransform(UTSERVICE_XML_PATH, UT_INITIAL_OCCURRENCES, UT_FINAL_OCCURRENCES);
	}

	@Test
	public void testTransform_TransactionManagerXml() throws TransformException, IOException {
		testTransform(TRANSACTION_MANAGER_XML_PATH, TM_INITIAL_OCCURRENCES, TM_FINAL_OCCURRENCES);
	}
}
