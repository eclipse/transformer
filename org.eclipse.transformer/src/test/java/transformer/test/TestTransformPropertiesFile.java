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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.PropertiesActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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

	public SignatureRuleImpl createSignatureRule(CaptureLoggerImpl useLogger, Map<String, String> packageRename) {

		return new SignatureRuleImpl(useLogger, packageRename, null, null, null, null, null, Collections.emptyMap());
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

	public PropertiesActionImpl jakartaPropertiesAction;

	public PropertiesActionImpl getJakartaPropertiesAction() {
		if (jakartaPropertiesAction == null) {
			CaptureLoggerImpl useLogger = getCaptureLogger();

			ActionContext context = new ActionContextImpl(useLogger, new InputBufferImpl(),
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getPackageRenames()));

			jakartaPropertiesAction = new PropertiesActionImpl(context);

		}
		return jakartaPropertiesAction;
	}

	@Test
	public void testJakartaTransform() throws TransformException {
		PropertiesActionImpl propsAction = getJakartaPropertiesAction();

		byte[] content = {};
		propsAction.apply(propsAction.collect(JAVAX_PATH, new ByteArrayInputStream(content), content.length));
		Assertions.assertTrue(JAKARTA_PATH.equals(propsAction.getLastActiveChanges()
			.getOutputResourceName()));
	}

}
