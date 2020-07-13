/** ******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.impl.PropertiesActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import transformer.test.util.CaptureLoggerImpl;

public class TestTransformPropertiesFile extends CaptureTest {

	public SelectionRuleImpl createSelectionRule(CaptureLoggerImpl useLogger, Set<String> useIncludes,
		Set<String> useExcludes) {

		return new SelectionRuleImpl(useLogger, useIncludes, useExcludes);
	}

	public SignatureRuleImpl createSignatureRule(CaptureLoggerImpl useLogger, Map<String, String> packageRename) {

		return new SignatureRuleImpl(useLogger, packageRename, null, null, null, null, Collections.emptyMap());
	}

	public static final String	JAKARTA_SERVLET	= "jakarta.servlet";

	public static final String	JAVAX_SERVLET	= "javax.servlet";

	public static final String	JAVAX_PATH		= "javax/servlet/Bundle.properties";

	public static final String	JAKARTA_PATH	= "jakarta/servlet/Bundle.properties";

	protected Set<String>		includes;

	public Set<String> getIncludes() {
		if (includes == null) {
			includes = new HashSet<>();
			includes.add(JAVAX_PATH);
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
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

			jakartaPropertiesAction = new PropertiesActionImpl(useLogger, false, false, createBuffer(),
				createSelectionRule(useLogger, getIncludes(), getExcludes()),
				createSignatureRule(useLogger, getPackageRenames()));
		}
		return jakartaPropertiesAction;
	}

	@Test
	public void testJakartaTransform() throws IOException, TransformException {
		PropertiesActionImpl propsAction = getJakartaPropertiesAction();

		byte[] content = {};
		propsAction.apply(JAVAX_PATH, new ByteArrayInputStream(content));
		Assertions.assertTrue(JAKARTA_PATH.equals(propsAction.getLastActiveChanges()
			.getOutputResourceName()));
	}

}
