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
import java.util.List;
import java.util.Properties;

import org.eclipse.transformer.jakarta.JakartaTransform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestLoad {

	public static final String	RULES_RENAMES_PATH	= JakartaTransform.DEFAULT_RENAMES_REFERENCE;

	public static final String	RULES_VERSIONS_PATH	= JakartaTransform.DEFAULT_VERSIONS_REFERENCE;

	public static final String	TRANSFORMER_PREFIX;

	static {
		String transformerPackageName = JakartaTransform.class.getPackage()
			.getName();
		TRANSFORMER_PREFIX = transformerPackageName.replace('.', '/') + '/';
	}

	public static String putIntoTransformer(String path) {
		return TRANSFORMER_PREFIX + path;
	}

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

	@ParameterizedTest
	@ValueSource(strings = {
		RULES_RENAMES_PATH, RULES_VERSIONS_PATH
	})
	public void testRulesLoad(String path) throws IOException {
		System.out.println("Load [ " + path + " ]");
		String qualifiedRulesPath = putIntoTransformer(path);

		System.out.println("Load (Qualified) [ " + qualifiedRulesPath + " ]");

		InputStream simpleInput = TestUtils.getResourceStream(qualifiedRulesPath);

		List<String> actualLines;
		try {
			actualLines = TestUtils.loadLines(simpleInput);
		} finally {
			simpleInput.close();
		}

		System.out.println("Loaded [ " + path + " ] [ " + actualLines.size() + " ]");
		for (String line : actualLines) {
			System.out.println(" [ " + line + " ]");
		}
	}

	public static final String		SIMPLE_RESOURCE_PATH	= "simple.resource";
	public static final String[]	SIMPLE_RESOURCE_LINES	= {
		"Simple Resource 1", "Simple Resource 2"
	};

	@Test
	public void testSimpleLoad() throws IOException {
		System.out.println("Load [ " + SIMPLE_RESOURCE_PATH + " ]");

		List<String> actualLines;
		try (InputStream simpleInput = TestUtils.getResourceStream(SIMPLE_RESOURCE_PATH)) {
			actualLines = TestUtils.loadLines(simpleInput);
		}

		System.out.println("Loaded [ " + SIMPLE_RESOURCE_PATH + " ] [ " + actualLines.size() + " ]");
		System.out.println("Expected [ " + SIMPLE_RESOURCE_PATH + " ] [ " + SIMPLE_RESOURCE_LINES.length + " ]");

		TestUtils.verify(SIMPLE_RESOURCE_PATH, SIMPLE_RESOURCE_LINES, actualLines);
	}
}
