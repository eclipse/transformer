/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package transformer.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.eclipse.transformer.Transformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestVersion {
	public static void main(String[] args) {
		Date date = new Date();
		DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

		System.out.println( "BUILD_DATE=" + format.format(date) );
	}

	@Test
	public void verifyBuildProperties() throws Exception {
		System.out.println("Verifying build properties [ " + Transformer.TRANSFORMER_BUILD_PROPERTIES + " ]");

		Properties buildProperties = Transformer.loadProperties(Transformer.TRANSFORMER_BUILD_PROPERTIES);

		String copyright = (String) buildProperties.get(Transformer.COPYRIGHT_PROPERTY_NAME);
		String shortVersion = (String) buildProperties.get(Transformer.SHORT_VERSION_PROPERTY_NAME);
		String longVersion = (String) buildProperties.get(Transformer.LONG_VERSION_PROPERTY_NAME);
		String buildDate = (String) buildProperties.get(Transformer.BUILD_DATE_PROPERTY_NAME);

		System.out.println("  Copyright     [ " + copyright + " ] [ " + Transformer.COPYRIGHT_PROPERTY_NAME + " ]");
		System.out.println("  Short version [ " + shortVersion + " ] [ " + Transformer.SHORT_VERSION_PROPERTY_NAME + " ]");
		System.out.println("  Long version  [ " + longVersion + " ] [ " + Transformer.LONG_VERSION_PROPERTY_NAME + " ]");
		System.out.println("  Build date    [ " + buildDate + " ] [ " + Transformer.BUILD_DATE_PROPERTY_NAME + " ]");

		Assertions.assertNotNull(copyright, "Copyright is absent");
		Assertions.assertNotNull(shortVersion, "Short version is absent");
		Assertions.assertNotNull(longVersion, "Long version is absent");
		Assertions.assertNotNull(buildDate, "Build date is absent");
	}
}
