/** ******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ******************************************************************************* */
package transformer.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.PropertiesActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transformer.test.util.CaptureLoggerImpl;

public class TestTransformPropertiesFile extends CaptureTest {
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

	public SelectionRuleImpl createSelectionRule(CaptureLoggerImpl useLogger, Map<String, String> useIncludes,
												 Map<String, String> useExcludes) {

		return new SelectionRuleImpl(useLogger, useIncludes, useExcludes);
	}

	public SignatureRuleImpl createSignatureRule(CaptureLoggerImpl useLogger, Map<String, String> packageRename, Map<String, String> directStrings) {

		return new SignatureRuleImpl(useLogger, packageRename, null, null, null, null, directStrings, Collections.emptyMap());
	}

	public static final String	JAKARTA_SERVLET	= "jakarta.servlet";

	public static final String	JAVAX_SERVLET	= "javax.servlet";

	public static final String	JAVAX_PATH		= "javax/servlet/Bundle.properties";

	public static final String	JAKARTA_PATH	= "jakarta/servlet/Bundle.properties";

	protected Map<String, String>		includes;

	public Map<String, String> getIncludes() {
		if (includes == null) {
			includes = new HashMap<>();
			includes.put(JAVAX_PATH, FileUtils.DEFAULT_CHARSET.name());
		}

		return includes;
	}

	public Map<String, String> getExcludes() {
		return Collections.emptyMap();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if (packageRenames == null) {
			packageRenames = new HashMap<>();
			packageRenames.put(JAVAX_SERVLET, JAKARTA_SERVLET);
		}
		return packageRenames;
	}

	protected Map<String, String> directStrings;

	public Map<String, String> getDirectStrings() {
		if (directStrings == null) {
			directStrings = new HashMap<>();
			directStrings.put("javax.servlet.async.mapping", "jakarta.servlet.async.mapping");
		}
		return directStrings;
	}

	public PropertiesActionImpl jakartaPropertiesAction;

	public PropertiesActionImpl getJakartaPropertiesAction() {
		if (jakartaPropertiesAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger,
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getPackageRenames(), getDirectStrings()));

			jakartaPropertiesAction = new PropertiesActionImpl(context);

		}
		return jakartaPropertiesAction;
	}

	@Test
	public void transform_key_value() throws TransformException {
		PropertiesActionImpl propsAction = getJakartaPropertiesAction();

		String content = "javax.servlet.async.mapping=value\n"
			+ "key javax.servlet.async.mapping";
		byte[] bytes = content.getBytes(FileUtils.DEFAULT_CHARSET);
		ByteData inputData = propsAction.collect(JAVAX_PATH, new ByteArrayInputStream(bytes), bytes.length);
		ByteData outputData = propsAction.apply(inputData);
		assertThat(propsAction.getLastActiveChanges().getOutputResourceName()).as("output name").isEqualTo(JAKARTA_PATH);
		String transformed = outputData.charset()
			.decode(outputData.buffer()).toString();
		assertThat(transformed).as("output value").isEqualTo("jakarta.servlet.async.mapping=value\n"
			+ "key jakarta.servlet.async.mapping");
	}

	@Test
	public void transform_comments() throws TransformException {
		PropertiesActionImpl propsAction = getJakartaPropertiesAction();

		String content = "  # javax.servlet.Bundle \\U20AC properties\n"
			+ "# no change \\U20ac line\r"
			+ "   \n" // blank line
			+ "\t\f\r\n" // blank line
			+ "! no escaped unicode line\r\n"
			+ "! javax.servlet.async.mapping comment";
		byte[] bytes = content.getBytes(FileUtils.DEFAULT_CHARSET);
		ByteData inputData = propsAction.collect(JAVAX_PATH, new ByteArrayInputStream(bytes), bytes.length);
		ByteData outputData = propsAction.apply(inputData);
		assertThat(propsAction.getLastActiveChanges().getOutputResourceName()).as("output name").isEqualTo(JAKARTA_PATH);
		String transformed = outputData.charset()
			.decode(outputData.buffer()).toString();
		assertThat(transformed).as("output value").isEqualTo("  # jakarta.servlet.Bundle \\U20AC properties\n"
			+ "# no change \\U20ac line\r"
			+ "   \n" // blank line
			+ "\t\f\r\n" // blank line
			+ "! no escaped unicode line\r\n"
			+ "! jakarta.servlet.async.mapping comment");
	}

}
