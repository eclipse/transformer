/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package transformer.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import aQute.lib.utf8properties.UTF8Properties;
import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.junit.jupiter.api.Test;
import transformer.test.util.CaptureLoggerImpl;

class SelectionTests {
	CaptureLoggerImpl useLogger = new CaptureLoggerImpl("Test", !CaptureLoggerImpl.CAPTURE_INACTIVE);

	@Test
	void selections() throws Exception {
		UTF8Properties properties = new UTF8Properties();
		properties.load("# comment\n*=UTF-8\n*.properties=ISO-8859-1\n*Abstract*=US-ASCII\nwide/*=UTF-16\n! comment\n*.proto=!", null, null);
		Map<String, String> selectionProperties = new HashMap<>();
		properties.forEach((k,v) -> {
			selectionProperties.put(k.toString(), v.toString());
		});
		Map<String, String> includes = new HashMap<>();
		Map<String, String> excludes = new HashMap<>();
		TransformProperties.addSelections(includes, excludes, selectionProperties);
		SelectionRuleImpl selectionRule = new SelectionRuleImpl(useLogger, includes, excludes);
		assertThat(selectionRule.select("foo.txt")).isTrue();
		assertThat(selectionRule.charset("foo.txt")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(selectionRule.select("foo.properties")).isTrue();
		assertThat(selectionRule.charset("foo.properties")).isEqualTo(StandardCharsets.ISO_8859_1);
		assertThat(selectionRule.select("bar/AbstractThing.class")).isTrue();
		assertThat(selectionRule.charset("bar/AbstractThing.class")).isEqualTo(StandardCharsets.US_ASCII);
		assertThat(selectionRule.select("wide/file.txt")).isTrue();
		assertThat(selectionRule.charset("wide/file.txt")).isEqualTo(StandardCharsets.UTF_16);
		assertThat(selectionRule.select("foo.proto")).isFalse();
	}
}
