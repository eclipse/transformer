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

package org.eclipse.transformer.bnd.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.RuntimeInvisibleAnnotationsAttribute;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.unmodifiable.Maps;
import aQute.lib.io.IO;
import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj.XmlAssert;

class BndAnalyzerPluginTest {
	static final Map<String, String> ds13 = Maps.of("scr", "http://www.osgi.org/xmlns/scr/v1.3.0");

	@BeforeEach
	void before_each() {
		System.setProperty("org.slf4j.simpleLogger.log.org.eclipse.transformer.bnd.analyzer", "info");
	}

	@AfterEach
	void after_each() {
		System.clearProperty("org.slf4j.simpleLogger.log.org.eclipse.transformer.bnd.analyzer");
	}

	@Test
	void analyzer_jakarta_transform(@TempDir
	File tmp) throws Exception {
		try (Builder builder = new Builder()) {
			builder.setBase(IO.getFile("target/test-classes"));
			builder.addClasspath(builder.getBase());
			builder.setProperty("-plugin.transformer",
				"org.eclipse.transformer.bnd.analyzer.JakartaTransformerAnalyzerPlugin;command:=-transformer");
			builder.setProperty("-includepackage", "org.eclipse.transformer.test.*");
			builder.setProperty("-includeresource", "META-INF/services=META-INF/services,META-INF/maven=META-INF/maven");
			builder.setProperty("-transformer",
				"immediate;option=versions;package=org.eclipse.transformer.test.api;version=\"[2.0\\,3.0);Export-Package=2.0.0\"");
			Jar jar = builder.build();
			assertThat(builder.check()).as("builder errors %s, warnings %s", builder.getErrors(), builder.getWarnings())
				.isTrue();

			assertThat(jar).isNotNull();
			File output = new File(tmp, "output.jar");
			jar.write(output);
			assertThat(output).isFile();

			try (JarFile jarFile = new JarFile(output)) {
				Manifest manifest = jarFile.getManifest();
				assertThat(manifest).isNotNull();
				Attributes mainAttributes = manifest.getMainAttributes();
				assertThat(mainAttributes).isNotNull();
				Parameters imports = new Parameters(mainAttributes.getValue("Import-Package"));
				assertThat(imports).containsOnlyKeys("jakarta.inject",
					"jakarta.annotation", "jakarta.enterprise.concurrent");
				Parameters exports = new Parameters(mainAttributes.getValue("Export-Package"));
				assertThat(exports)
					.containsOnlyKeys("org.eclipse.transformer.test.api");
				assertThat(exports.get("org.eclipse.transformer.test.api")).containsEntry("version", "2.0.0");

				ZipEntry packageinfo = jarFile.getEntry("org/eclipse/transformer/test/api/package-info.class");
				assertThat(packageinfo).isNotNull();
				try (DataInputStream in = new DataInputStream(jarFile.getInputStream(packageinfo))) {
					ClassFile classFile = ClassFile.parseClassFile(in);
					assertThat(Arrays.stream(classFile.attributes)
						.filter(RuntimeInvisibleAnnotationsAttribute.class::isInstance)
						.map(RuntimeInvisibleAnnotationsAttribute.class::cast)
						.flatMap(attr -> Arrays.stream(attr.annotations))
						.filter(anno -> anno.type.equals("Lorg/osgi/annotation/versioning/Version;"))
						.flatMap(anno -> Arrays.stream(anno.values))
						.filter(value -> value.name.equals("value"))
						.map(value -> value.value)
						.findFirst()).hasValue("2.0.0");
				}

				ZipEntry ballXmlEntry = jarFile.getEntry("OSGI-INF/org.eclipse.transformer.test.impl.BallImpl.xml");
				assertThat(ballXmlEntry).isNotNull();
				String ballXml = IO.collect(jarFile.getInputStream(ballXmlEntry));
				XmlAssert.assertThat(ballXml)
					.withNamespaceContext(ds13)
					.nodesByXPath("scr:component/service/provide")
					.exist()
					.extractingAttribute("interface")
					.contains("jakarta.enterprise.concurrent.ManageableThread")
					.doesNotContain("javax.enterprise.concurrent.ManageableThread");

				ZipEntry playerXmlEntry = jarFile.getEntry("OSGI-INF/org.eclipse.transformer.test.impl.PlayerImpl.xml");
				assertThat(playerXmlEntry).isNotNull();
				String playerXml = IO.collect(jarFile.getInputStream(playerXmlEntry));
				XmlAssert.assertThat(playerXml)
					.withNamespaceContext(ds13)
					.nodesByXPath("scr:component/reference")
					.exist()
					.extractingAttribute("interface")
					.contains("jakarta.enterprise.concurrent.ManageableThread")
					.doesNotContain("javax.enterprise.concurrent.ManageableThread");

				assertThat(jarFile.getEntry("META-INF/services/javax.ws.rs.client.ClientBuilder")).isNull();
				ZipEntry metainfServices = jarFile.getEntry("META-INF/services/jakarta.ws.rs.client.ClientBuilder");
				assertThat(metainfServices).isNotNull();
				assertThat(IO.collect(jarFile.getInputStream(metainfServices))
					.trim()).isEqualTo("org.glassfish.jersey.client.JerseyClientBuilder");

				assertThat(jarFile.getEntry("org/eclipse/transformer/test/impl/resource.foo")).isNotNull();
				assertThat(jarFile.getEntry("org/eclipse/transformer/test/impl/sac-1.3.jar")).isNotNull();
				assertThat(jarFile.getEntry("META-INF/maven/groupId/artifactId/pom.xml")).isNotNull();
				assertThat(jarFile.getEntry("META-INF/maven/groupId/artifactId/pom.properties")).isNotNull();
			}
		}
	}

	@Test
	void verifier_jakarta_transform(@TempDir
	File tmp) throws Exception {
		try (Builder builder = new Builder()) {
			builder.setBase(IO.getFile("target/test-classes"));
			builder.addClasspath(builder.getBase());
			builder.setProperty("-plugin.transformer",
				"org.eclipse.transformer.bnd.analyzer.JakartaTransformerVerifierPlugin;command:=-transformer");
			builder.setProperty("-includepackage", "org.eclipse.transformer.test.*");
			builder.setProperty("-includeresource", "META-INF/services=META-INF/services,META-INF/maven=META-INF/maven");
			builder.setProperty("-transformer",
				"immediate;option=renames;package=org.eclipse.transformer.test.api;rename=org.eclipse.transformer.test.api,"
					+ "immediate;option=versions;package=org.eclipse.transformer.test.api;version=\"[2.0\\,3.0);Export-Package=2.0.0\"," //
					+ "immediate;option=text;selector=org.eclipse.transformer.test.impl.*.xml;rules=jakarta-renames.properties" //
			);
			Jar jar = builder.build();
			assertThat(builder.check()).as("builder errors %s, warnings %s", builder.getErrors(), builder.getWarnings())
				.isTrue();

			assertThat(jar).isNotNull();
			File output = new File(tmp, "output.jar");
			jar.write(output);
			assertThat(output).isFile();

			try (JarFile jarFile = new JarFile(output)) {
				Manifest manifest = jarFile.getManifest();
				assertThat(manifest).isNotNull();
				Attributes mainAttributes = manifest.getMainAttributes();
				assertThat(mainAttributes).isNotNull();
				Parameters imports = new Parameters(mainAttributes.getValue("Import-Package"));
				assertThat(imports).containsOnlyKeys("jakarta.inject", "jakarta.annotation",
					"jakarta.enterprise.concurrent");
				Parameters exports = new Parameters(mainAttributes.getValue("Export-Package"));
				assertThat(exports).containsOnlyKeys("org.eclipse.transformer.test.api");
				assertThat(exports.get("org.eclipse.transformer.test.api")).containsEntry("version", "2.0.0");

				ZipEntry packageinfo = jarFile.getEntry("org/eclipse/transformer/test/api/package-info.class");
				assertThat(packageinfo).isNotNull();
				try (DataInputStream in = new DataInputStream(jarFile.getInputStream(packageinfo))) {
					ClassFile classFile = ClassFile.parseClassFile(in);
					assertThat(Arrays.stream(classFile.attributes)
						.filter(RuntimeInvisibleAnnotationsAttribute.class::isInstance)
						.map(RuntimeInvisibleAnnotationsAttribute.class::cast)
						.flatMap(attr -> Arrays.stream(attr.annotations))
						.filter(anno -> anno.type.equals("Lorg/osgi/annotation/versioning/Version;"))
						.flatMap(anno -> Arrays.stream(anno.values))
						.filter(value -> value.name.equals("value"))
						.map(value -> value.value)
						.findFirst()).hasValue("2.0.0");
				}

				ZipEntry ballXmlEntry = jarFile.getEntry("OSGI-INF/org.eclipse.transformer.test.impl.BallImpl.xml");
				assertThat(ballXmlEntry).isNotNull();
				String ballXml = IO.collect(jarFile.getInputStream(ballXmlEntry));
				XmlAssert.assertThat(ballXml)
					.withNamespaceContext(ds13)
					.nodesByXPath("scr:component/service/provide")
					.exist()
					.extractingAttribute("interface")
					.contains("jakarta.enterprise.concurrent.ManageableThread")
					.doesNotContain("javax.enterprise.concurrent.ManageableThread");

				ZipEntry playerXmlEntry = jarFile.getEntry("OSGI-INF/org.eclipse.transformer.test.impl.PlayerImpl.xml");
				assertThat(playerXmlEntry).isNotNull();
				String playerXml = IO.collect(jarFile.getInputStream(playerXmlEntry));
				XmlAssert.assertThat(playerXml)
					.withNamespaceContext(ds13)
					.nodesByXPath("scr:component/reference")
					.exist()
					.extractingAttribute("interface")
					.contains("jakarta.enterprise.concurrent.ManageableThread")
					.doesNotContain("javax.enterprise.concurrent.ManageableThread");

				assertThat(jarFile.getEntry("META-INF/services/javax.ws.rs.client.ClientBuilder")).isNull();
				ZipEntry metainfServices = jarFile.getEntry("META-INF/services/jakarta.ws.rs.client.ClientBuilder");
				assertThat(metainfServices).isNotNull();
				assertThat(IO.collect(jarFile.getInputStream(metainfServices))
					.trim()).isEqualTo("org.glassfish.jersey.client.JerseyClientBuilder");

				assertThat(jarFile.getEntry("org/eclipse/transformer/test/impl/resource.foo")).isNotNull();
				assertThat(jarFile.getEntry("org/eclipse/transformer/test/impl/sac-1.3.jar")).isNotNull();
				assertThat(jarFile.getEntry("META-INF/maven/groupId/artifactId/pom.xml")).isNotNull();
				assertThat(jarFile.getEntry("META-INF/maven/groupId/artifactId/pom.properties")).isNotNull();
			}
		}
	}

	@Test
	void options_test() throws Exception {
		try (Processor processor = new Processor()) {
			processor.setProperty("-transformer", "tb;arg=value1");
			processor.setProperty("-transformer.overwrite", "overwrite");
			processor.setProperty("-transformer.morebundles", "bundles;arg=value2");
			processor.setProperty("-transformer.immediate",
				"immediate;option=tv;package=org.eclipse.transformer.test.api;version=\"[2.0\\,3.0);Export-Package=2.0.0\"");
			Parameters parameters = processor.decorated("-transformer", true);
			Map<String, String> defaultValues = Maps.of("bundles", "value3", "o", "true");
			TransformOptions options = new TransformerPluginOptions(processor, parameters, defaultValues,
				getClass()::getResource);
			assertThat(processor.check())
				.as("Errors:\n%s\nWarnings:\n%s", processor.getErrors(), processor.getWarnings())
				.isTrue();
			assertThat(options.getDefaultValue(AppOption.RULES_BUNDLES)).isEqualTo("value3");
			assertThat(options.getOptionValue(AppOption.RULES_BUNDLES)).isEqualTo("value1");
			assertThat(options.getOptionValues(AppOption.RULES_BUNDLES)).containsExactly("value1", "value2");

			assertThat(options.getDefaultValue(AppOption.OVERWRITE)).isEqualTo("true");
			assertThat(options.hasOption(AppOption.OVERWRITE)).isTrue();
			assertThat(options.getOptionValues(AppOption.OVERWRITE)).isEmpty();
			assertThat(options.getOptionValue(AppOption.OVERWRITE)).isNull();

			assertThat(options.hasOption(AppOption.WIDEN_ARCHIVE_NESTING)).isFalse();
			assertThat(options.getOptionValues(AppOption.WIDEN_ARCHIVE_NESTING)).isNull();
			assertThat(options.getOptionValue(AppOption.WIDEN_ARCHIVE_NESTING)).isNull();

			assertThat(options.getOptionValues(AppOption.RULES_IMMEDIATE_DATA)).containsExactly("tv",
				"org.eclipse.transformer.test.api", "[2.0\\,3.0);Export-Package=2.0.0");
		}
	}

	@Test
	void options_test_validation() throws Exception {
		try (Processor processor = new Processor()) {
			processor.setProperty("-transformer", "tb;overwrite,foo,renames");
			processor.setProperty("-transformer.immediate", "immediate;option=versions");
			Parameters parameters = processor.decorated("-transformer", true);
			Map<String, String> defaultValues = Maps.of("bundles", "value3", "o", "true");
			TransformOptions options = new TransformerPluginOptions(processor, parameters, defaultValues,
				getClass()::getResource);
			assertThat(processor.check("The transformer option tb requires arguments",
				"The transformer option foo is unrecognized", "The transformer option renames requires arguments",
				"The transformer option immediate requires 3 arguments"))
				.as("Errors:\n%s\nWarnings:\n%s", processor.getErrors(), processor.getWarnings())
				.isTrue();

			assertThat(options.getDefaultValue(AppOption.OVERWRITE)).isEqualTo("true");
			assertThat(options.hasOption(AppOption.OVERWRITE)).isTrue();
			assertThat(options.getOptionValues(AppOption.OVERWRITE)).isEmpty();
			assertThat(options.getOptionValue(AppOption.OVERWRITE)).isNull();
		}
	}
}
