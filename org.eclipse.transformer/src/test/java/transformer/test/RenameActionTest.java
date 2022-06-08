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

import java.lang.reflect.Method;
import java.util.Collections;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.impl.ActionImpl;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.RenameActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.unmodifiable.Maps;

/**
 *
 */
class RenameActionTest {
	Logger	logger;
	String	testName;

	@BeforeEach
	public void setUp(TestInfo testInfo) {
		testName = testInfo.getTestClass()
			.map(Class::getName)
			.get() + "."
			+ testInfo.getTestMethod()
				.map(Method::getName)
				.get();
		logger = LoggerFactory.getLogger(testName);
	}

	@Test
	void relocate_resource() {
		Action.ActionInitData initData = new ActionImpl.ActionInitDataImpl(logger, new InputBufferImpl(),
			new SelectionRuleImpl(logger, Collections.emptySet(), Collections.emptySet()), new SignatureRuleImpl(logger,
				Maps.of("com.a.b.*", "com.shaded.a.b"), null, null, null, null, null, Collections.emptyMap()));
		RenameActionImpl action = new RenameActionImpl(initData);

		assertThat(action.relocateResource("a/b/c/packageinfo")).isEqualTo("a/b/c/packageinfo");
		assertThat(action.relocateResource("com/a/b/c/packageinfo")).isEqualTo("com/shaded/a/b/c/packageinfo");
		assertThat(action.relocateResource("WEB-INF/classes/")).isEqualTo("WEB-INF/classes/");
		assertThat(action.relocateResource("WEB-INF/classes/com/a/b/c/packageinfo"))
			.isEqualTo("WEB-INF/classes/com/shaded/a/b/c/packageinfo");
		assertThat(action.relocateResource("META-INF/versions/")).isEqualTo("META-INF/versions/");
		assertThat(action.relocateResource("META-INF/versions/foo")).isEqualTo("META-INF/versions/foo");
		assertThat(action.relocateResource("META-INF/versions/11/")).isEqualTo("META-INF/versions/11/");
		assertThat(action.relocateResource("META-INF/versions/9/com/a/b/c/packageinfo"))
			.isEqualTo("META-INF/versions/9/com/shaded/a/b/c/packageinfo");
	}

}
