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

package org.eclipse.transformer.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.transformer.action.ElementAction;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class TransformerRunMojoTest {

	@Rule
	public TestName			name		= new TestName();
	@Rule
	public TemporaryFolder	tmp			= new TemporaryFolder(new File("target"));

	@Rule
	public MojoRule			rule		= new MojoRule();

	@Rule
	public TestResources	resources	= new TestResources();


	@Test
	public void testProjectArtifactTransformerPlugin() throws Exception {
		final TransformerRunMojo mojo = new TransformerRunMojo();
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));
		mojo.setOverwrite(true);
		mojo.setOutputDirectory(tmp.newFolder(name.getMethodName()));
		mojo.setAttach(true);

		final File targetDirectory = this.resources.getBasedir("transform-build-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "war", "rest-sample");
		mavenProject.getArtifact()
			.setFile(createService("war", targetDirectory));

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(1, sourceArtifacts.length);
		assertEquals("org.superbiz.rest", sourceArtifacts[0].getGroupId());
		assertEquals("rest-sample", sourceArtifacts[0].getArtifactId());
		assertEquals("1.0-SNAPSHOT", sourceArtifacts[0].getVersion());
		assertEquals("war", sourceArtifacts[0].getType());
		assertNull(sourceArtifacts[0].getClassifier());

		mojo.transform(sourceArtifacts[0]);

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
		final TransformerRunMojo mojo = new TransformerRunMojo();
		mojo.setOverwrite(true);
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));
		mojo.setAttach(true);

		final File targetDirectory = this.resources.getBasedir("transform-build-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "pom", "simple-service");

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");
		mojo.setOutputDirectory(tmp.newFolder(name.getMethodName()));

		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test1", createService("war", targetDirectory));
		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test2", createService("war", targetDirectory));
		mojo.getProjectHelper()
			.attachArtifact(mavenProject, "zip", "test3", createService("war", targetDirectory));

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(3, sourceArtifacts.length);

		for (int i = 0; i < 3; i++) {
			assertEquals("org.superbiz.rest", sourceArtifacts[i].getGroupId());
			assertEquals("simple-service", sourceArtifacts[i].getArtifactId());
			assertEquals("1.0-SNAPSHOT", sourceArtifacts[i].getVersion());
			assertEquals("zip", sourceArtifacts[i].getType());
			assertEquals("test" + (i + 1), sourceArtifacts[i].getClassifier());
		}

		for (int i = 0; i < 3; i++) {
			mojo.transform(sourceArtifacts[i]);
		}

		assertEquals(6, mavenProject.getAttachedArtifacts()
			.size());
		Set<String> classifiers = mavenProject.getAttachedArtifacts()
			.stream()
			.filter(a -> (a.getType()
				.equals("zip")
				&& a.getArtifactId()
					.equals("simple-service")))
			.map(Artifact::getClassifier)
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

	@Test
	public void testProjectArtifactTransformerPluginNoAttach() throws Exception {
		final TransformerRunMojo mojo = new TransformerRunMojo();
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));
		mojo.setOverwrite(true);
		mojo.setOutputDirectory(tmp.newFolder(name.getMethodName()));
		mojo.setAttach(false);

		final File targetDirectory = this.resources.getBasedir("transform-build-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "war", "rest-sample");
		mavenProject.getArtifact()
			.setFile(createService("war", targetDirectory));

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(1, sourceArtifacts.length);
		assertEquals("org.superbiz.rest", sourceArtifacts[0].getGroupId());
		assertEquals("rest-sample", sourceArtifacts[0].getArtifactId());
		assertEquals("1.0-SNAPSHOT", sourceArtifacts[0].getVersion());
		assertEquals("war", sourceArtifacts[0].getType());
		assertNull(sourceArtifacts[0].getClassifier());

		mojo.transform(sourceArtifacts[0]);

		assertEquals(0, mavenProject.getAttachedArtifacts()
			.size());
	}

	@Test
	public void testProjectArtifactTransformerPluginStripSignatureFiles() throws Exception {
		final TransformerRunMojo mojo = new TransformerRunMojo();
		mojo.setProjectHelper(this.rule.lookup(MavenProjectHelper.class));
		mojo.setOverwrite(true);
		mojo.setOutputDirectory(tmp.newFolder(name.getMethodName()));
		mojo.setAttach(true);
		mojo.setStripSignatures(true);

		final File targetDirectory = this.resources.getBasedir("transform-build-jar-artifact");
		final File modelDirectory = new File(targetDirectory, "target/model");
		final File pom = new File(targetDirectory, "pom.xml");

		final MavenProject mavenProject = createMavenProject(modelDirectory, pom, "jar", "rest-sample");
		mavenProject.getArtifact()
			.setFile(createService("jar", targetDirectory));

		mojo.setProject(mavenProject);
		mojo.setClassifier("transformed");

		final Artifact[] sourceArtifacts = mojo.getSourceArtifacts();
		assertEquals(1, sourceArtifacts.length);
		final Artifact sourceArtifact = sourceArtifacts[0];
		assertEquals("org.superbiz.rest", sourceArtifact.getGroupId());
		assertEquals("rest-sample", sourceArtifact.getArtifactId());
		assertEquals("1.0-SNAPSHOT", sourceArtifact.getVersion());
		assertEquals("jar", sourceArtifact.getType());
		assertNull(sourceArtifact.getClassifier());
		assertThat(getSignatureFileEntries(sourceArtifact.getFile())).containsExactly("META-INF/MYKEY.SF", "META-INF/MYKEY.DSA");

		mojo.transform(sourceArtifacts[0]);

		assertEquals(1, mavenProject.getAttachedArtifacts()
			.size());
		final Artifact transformedArtifact = mavenProject.getAttachedArtifacts()
			.get(0);

		assertEquals("org.superbiz.rest", transformedArtifact.getGroupId());
		assertEquals("rest-sample", transformedArtifact.getArtifactId());
		assertEquals("1.0-SNAPSHOT", transformedArtifact.getVersion());
		assertEquals("jar", transformedArtifact.getType());
		assertEquals("transformed", transformedArtifact.getClassifier());
		assertThat(getSignatureFileEntries(transformedArtifact.getFile())).isEmpty();
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
				null, packaging, null, new DefaultArtifactHandlerStub(packaging, null)));
		return mavenProject;
	}

	private static File createService(String packaging, File targetDirectory) throws IOException {
		final File tempFile = File.createTempFile("service", "." + packaging);
		tempFile.delete();

		final Archive<?> archive;
		if (packaging.equals("jar")) {
			archive = ShrinkWrap.create(JavaArchive.class, "service." + packaging)
				.addClass(EchoService.class);
		} else {
			archive = ShrinkWrap.create(WebArchive.class, "service." + packaging)
				.addClass(EchoService.class);
		}

		if (packaging.equals("jar")) {
			archive.add(new FileAsset(new File(targetDirectory, "META-INF/MYKEY.SF")), "META-INF/MYKEY.SF");
			archive.add(new FileAsset(new File(targetDirectory, "META-INF/MYKEY.DSA")), "META-INF/MYKEY.DSA");
		}

		archive.as(ZipExporter.class)
			.exportTo(tempFile, true);
		return tempFile;
	}

	private static Set<String> getSignatureFileEntries(File file) throws IOException {
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			final Set<String> signatureFiles = new HashSet<>();
			while (entries.hasMoreElements()) {
				final ZipEntry zipEntry = entries.nextElement();
				if (ElementAction.SIGNATURE_FILE_PATTERN.matcher(zipEntry.getName()).matches()) {
					signatureFiles.add(zipEntry.getName());
				}
			}
			return signatureFiles;
		}
	}
}
