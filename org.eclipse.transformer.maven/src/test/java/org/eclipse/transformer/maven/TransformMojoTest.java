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

package org.eclipse.transformer.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.transformer.Transformer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.Test;

public class TransformMojoTest {

	@Rule
	public MojoRule			rule		= new MojoRule();

	@Rule
	public TestResources	resources	= new TestResources();

	@Test
	public void testProjectArtifactTransformerPlugin() throws Exception {
		final TransformMojo mojo = new TransformMojo();
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));
		mojo.setOverwrite(true);
		mojo.setOutputDirectory(new File("target"));

		assertNotNull(mojo);

		final File targetDirectory = this.resources.getBasedir("transform-build-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "war", "rest-sample");
		mavenProject.getArtifact()
			.setFile(createService());

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(1, sourceArtifacts.length);
		assertEquals("org.superbiz.rest", sourceArtifacts[0].getGroupId());
		assertEquals("rest-sample", sourceArtifacts[0].getArtifactId());
		assertEquals("1.0-SNAPSHOT", sourceArtifacts[0].getVersion());
		assertEquals("war", sourceArtifacts[0].getType());
		assertNull(sourceArtifacts[0].getClassifier());

		final Transformer transformer = mojo.getTransformer();
		assertNotNull(transformer);

		mojo.transform(transformer, sourceArtifacts[0]);

		assertEquals(1, mavenProject.getAttachedArtifacts()
			.size());
		final Artifact transformedArtifact = mavenProject.getAttachedArtifacts()
			.get(0);

		assertEquals("org.superbiz.rest", transformedArtifact.getGroupId());
		assertEquals("rest-sample", transformedArtifact.getArtifactId());
		assertEquals("1.0-SNAPSHOT", transformedArtifact.getVersion());
		assertEquals("war", transformedArtifact.getType());
		assertEquals("transformed", transformedArtifact.getClassifier());
	}

	@Test
	public void testMultipleArtifactTransformerPlugin() throws Exception {
		final TransformMojo mojo = new TransformMojo();
		mojo.setOverwrite(true);
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));

		assertNotNull(mojo);

		final File targetDirectory = this.resources.getBasedir("transform-build-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "pom", "simple-service");

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");
		mojo.setOutputDirectory(new File("target"));

		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test1", createService());
		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test2", createService());
		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test3", createService());

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(3, sourceArtifacts.length);

		for (int i = 0; i < 3; i++) {
			assertEquals("org.superbiz.rest", sourceArtifacts[i].getGroupId());
			assertEquals("simple-service", sourceArtifacts[i].getArtifactId());
			assertEquals("1.0-SNAPSHOT", sourceArtifacts[i].getVersion());
			assertEquals("zip", sourceArtifacts[i].getType());
			assertEquals("test" + (i + 1), sourceArtifacts[i].getClassifier());
		}

		final Transformer transformer = mojo.getTransformer();
		assertNotNull(transformer);

		for (int i = 0; i < 3; i++) {
			mojo.transform(transformer, sourceArtifacts[i]);
		}

		assertEquals(6, mavenProject.getAttachedArtifacts()
			.size());
		Set<String> classifiers = mavenProject.getAttachedArtifacts()
			.stream()
			.filter(a -> (a.getType()
				.equals("zip")
				&& a.getArtifactId()
					.equals("simple-service")))
			.map(a -> a.getClassifier())
			.collect(Collectors.toSet());

		assertEquals(6, mavenProject.getAttachedArtifacts()
			.size());
		assertTrue(classifiers.contains("test1"));
		assertTrue(classifiers.contains("test2"));
		assertTrue(classifiers.contains("test3"));
		assertTrue(classifiers.contains("test1-transformed"));
		assertTrue(classifiers.contains("test2-transformed"));
		assertTrue(classifiers.contains("test3-transformed"));
	}

	public MavenProject createMavenProject(final File modelDirectory, final File pom, final String packaging,
		final String artfifactId) {
		final MavenProject mavenProject = new MavenProject();
		mavenProject.setFile(pom);
		mavenProject.setGroupId("org.superbiz.rest");
		mavenProject.setArtifactId(artfifactId);
		mavenProject.setVersion("1.0-SNAPSHOT");
		mavenProject.setPackaging(packaging);
		mavenProject.getBuild()
			.setDirectory(modelDirectory.getParentFile()
				.getAbsolutePath());
		mavenProject.getBuild()
			.setOutputDirectory(modelDirectory.getAbsolutePath());
		mavenProject.setArtifact(
			new DefaultArtifact(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion(),
				(String) null, "war", (String) null, new DefaultArtifactHandlerStub(packaging, null)));
		return mavenProject;
	}

	public File createService() throws IOException {
		final File tempFile = File.createTempFile("service", ".war");
		tempFile.delete();

		final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "service.war")
			.addClass(EchoService.class);

		webArchive.as(ZipExporter.class)
			.exportTo(tempFile, true);
		return tempFile;
	}
}
