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

package org.eclipse.transformer.action.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.OptionalAssert;
import org.eclipse.transformer.action.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.classfile.Attribute;
import aQute.bnd.classfile.ClassFile;
import aQute.bnd.classfile.ElementInfo;
import aQute.bnd.classfile.ModuleAttribute;
import aQute.bnd.classfile.ModuleMainClassAttribute;
import aQute.bnd.classfile.ModulePackagesAttribute;
import aQute.bnd.classfile.builder.ModuleInfoBuilder;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

public class ClassActionTest {
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

	<ATTRIBUTE extends Attribute> Stream<ATTRIBUTE> attributes(Class<ATTRIBUTE> attributeType, ElementInfo element) {
		@SuppressWarnings("unchecked")
		Stream<ATTRIBUTE> stream = (Stream<ATTRIBUTE>) Arrays.stream(element.attributes)
			.filter(attributeType::isInstance);
		return stream;
	}

	<ATTRIBUTE extends Attribute> Optional<ATTRIBUTE> attribute(Class<ATTRIBUTE> attributeType, ElementInfo element) {
		return attributes(attributeType, element).findFirst();
	}

	@Test
	public void module_transform() throws Exception {
		ClassFile original = new ModuleInfoBuilder().module_name("module.name")
			.module_version("1.0.0")
			.requires("requires.something", 0, "1.2.0")
			.requires("original.requires", ModuleAttribute.Require.ACC_TRANSITIVE, "1.1.0")
			.exports("pkg/something", 0, "another.module")
			.exports("original/exports", 0)
			.opens("pkg/opens", 0, "another.module")
			.opens("original/opens", 0, "another.module")
			.uses("pkg/uses/SomeInterface")
			.uses("original/uses/Interface")
			.provides("pkg/provides/SomeInterface", "pkg/provides/impl/SomeImpl")
			.provides("original/provides/Interface", "original/provides/impl/SomeImpl")
			.mainClass("original/main/Main")
			.build();
		assertThat(original.this_class).as("test class name is a module")
			.isEqualTo("module-info");
		assertThat(attribute(ModuleAttribute.class, original)).as("test class has a %s attribute", ModuleAttribute.NAME)
			.isNotEmpty();
		assertThat(attribute(ModulePackagesAttribute.class, original))
			.as("test class has a %s attribute", ModulePackagesAttribute.NAME)
			.isNotEmpty();
		assertThat(attribute(ModuleMainClassAttribute.class, original))
			.as("test class has a %s attribute", ModuleMainClassAttribute.NAME)
			.isNotEmpty();

		ModuleAttribute originalModule = attribute(ModuleAttribute.class, original)
			.orElseGet(() -> fail("missing attribute %s", ModuleAttribute.NAME));

		ByteBufferDataOutput dataOutput = new ByteBufferDataOutput();
		original.write(dataOutput);
		ByteBufferInputStream inputStream = new ByteBufferInputStream(dataOutput.toByteBuffer());
		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputStream.available());

		Map<String, String> renames = new HashMap<>();
		renames.put("original.exports", "transformed.exports");
		renames.put("original.opens", "transformed.opens");
		renames.put("original.uses", "transformed.uses");
		renames.put("original.provides", "transformed.provides");
		renames.put("original.provides.impl", "transformed.provides.impl");
		renames.put("original.main", "transformed.main");
		Action classAction = new ClassActionImpl(logger, false, false, new InputBufferImpl(),
			new SelectionRuleImpl(logger, Collections.emptySet(), Collections.emptySet()),
			new SignatureRuleImpl(logger, renames, null, null, null, null));
		classAction.apply(testName, inputStream, inputStream.available(), outputStream);

		ClassFile transformed = ClassFile.parseClassFile(ByteBufferDataInput.wrap(outputStream.toByteBuffer()));

		assertThat(transformed.this_class)
			.as("transformed class name is a module")
			.isEqualTo("module-info");

		OptionalAssert<ModuleAttribute> moduleAssert = assertThat(attribute(ModuleAttribute.class, transformed))
			.as("transformed class module attribute");
		moduleAssert.as("transformed class module name, flags, and version")
			.get()
			.isEqualToComparingOnlyGivenFields(originalModule, "module_name", "module_flags", "module_version");

		moduleAssert.map(m -> m.requires)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Require[].class))
			.as("transformed class module requires")
			.usingElementComparatorIgnoringFields()
			.isEqualTo(originalModule.requires);

		moduleAssert.map(m -> m.opens)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Open[].class))
			.as("transformed class module opens flags and to")
			.usingElementComparatorIgnoringFields("opens")
			.isEqualTo(originalModule.opens);
		moduleAssert.map(m -> m.opens)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Open[].class))
			.as("transformed class module opens packages")
			.extracting(o -> o.opens)
			.containsExactlyInAnyOrder("pkg/opens", "transformed/opens");

		moduleAssert.map(m -> m.exports)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Export[].class))
			.as("transformed class module exports flags and to")
			.usingElementComparatorIgnoringFields("exports")
			.isEqualTo(originalModule.exports);
		moduleAssert.map(m -> m.exports)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Export[].class))
			.as("transformed class module exports packages")
			.extracting(o -> o.exports)
			.containsExactlyInAnyOrder("pkg/something", "transformed/exports");

		moduleAssert.map(m -> m.provides)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Provide[].class))
			.as("transformed class module provides packages")
			.extracting(p -> p.provides)
			.containsExactlyInAnyOrder("pkg/provides/SomeInterface", "transformed/provides/Interface");
		moduleAssert.map(m -> m.provides)
			.get(InstanceOfAssertFactories.array(ModuleAttribute.Provide[].class))
			.as("transformed class module provides packages")
			.flatExtracting(p -> Arrays.asList(p.provides_with))
			.containsExactlyInAnyOrder("pkg/provides/impl/SomeImpl", "transformed/provides/impl/SomeImpl");

		assertThat(attribute(ModulePackagesAttribute.class, transformed))
			.as("transformed class module packages")
			.map(m -> m.packages)
			.get(InstanceOfAssertFactories.array(String[].class))
			.noneMatch(p -> p.contains("original"));

		assertThat(attribute(ModuleMainClassAttribute.class, transformed))
			.as("transformed class module main class")
			.map(m -> m.main_class)
			.get(InstanceOfAssertFactories.STRING)
			.isEqualTo("transformed/main/Main");
	}

}
