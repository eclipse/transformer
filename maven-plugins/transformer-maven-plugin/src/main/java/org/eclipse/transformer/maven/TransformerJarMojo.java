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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import aQute.bnd.maven.PomPropertiesResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import aQute.libg.glob.PathSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.transformer.maven.configuration.TransformerArtifact;

import javax.inject.Inject;

/**
 * Transforms a specified artifact into a new artifact.
 * <p>
 * This goal has the default phase of "package".
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class TransformerJarMojo extends AbstractTransformerMojo {
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession				session;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
	private List<ArtifactRepository>	remoteRepositories;

	/**
	 * The build directory into which the new transformed artifact is written.
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	private File						buildDirectory;

	/**
	 * The base name of the transformed artifact.
	 * <p>
	 * The classifier and type will be suffixed to the base name.
	 */
	@Parameter(defaultValue = "${project.build.finalName}")
	private String						baseName;

	/**
	 * The classifier of the transformed artifact.
	 * <p>
	 * The default value is comes from the {@code classifier} value in the
	 * {@link #artifact} configuration. The value "-" (Hyphen-minus) is treated
	 * as no classifier specified. So to avoid inheriting the classifier of the
	 * artifact configuration, use "-".
	 */
	@Parameter
	private String						classifier;

	/**
	 * The type of the transformed artifact.
	 * <p>
	 * The default value is the packaging of the project which is normally
	 * "jar".
	 */
	@Parameter(defaultValue = "${project.packaging}")
	private String						type;

	/**
	 * Time stamp for reproducible output archive entries, either formatted as
	 * ISO 8601 yyyy-MM-dd'T'HH:mm:ssXXX or as an int representing seconds since
	 * the epoch (like SOURCE_DATE_EPOCH).
	 */
	@Parameter(defaultValue = "${project.build.outputTimestamp}")
	private String						outputTimestamp;

	/**
	 * Attach the transformed artifact to the project.
	 */
	@Parameter(defaultValue = "true")
	private boolean						attach;

	/**
	 * The input artifact to transform.
	 * <p>
	 * The archive configuration includes: groupId, artifactId, version, type,
	 * classifier, and excludes.
	 */
	@Parameter
	private TransformerArtifact			artifact	= new TransformerArtifact();

	@Inject
	private ArtifactResolver			artifactResolver;

	@Inject
	private MavenProjectHelper			projectHelper;

	@Inject
	private ArtifactHandlerManager		artifactHandlerManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip()) {
			return;
		}

		TransformerArtifact artifactDescription = prepareArtifactDescription(getArtifact());

		// Adjust classifier
		if (StringUtils.isBlank(getClassifier())) {
			setClassifier(artifactDescription.getClassifier());
		} else if (Objects.equals("-", getClassifier())) {
			setClassifier(null);
		}

		if (TransformerMavenLifecycleParticipant.isPackagingGoal(getMojoExecution().getGoal())
			&& StringUtils.isBlank(getClassifier()) && !getMojoExecution().getPlugin()
				.isExtensions()) {
			throw new MojoExecutionException(String.format(
				"In order to use the %s packaging goal %s without a classifier, <extensions>true</extensions> must be set on the plugin configuration",
				TransformerMavenLifecycleParticipant.THIS_ARTIFACT_ID, getMojoExecution().getGoal()));
		}

		Artifact input = getArtifact(artifactDescription);

		try (Jar jar = new Jar(input.getFile())) {
			File output = getOutput();

			getLogger().debug("Transforming {} to {}", input, output);

			// Remove original maven metadata before transform
			PathSet excludes = new PathSet("META-INF/maven/**");
			excludes.includes(artifactDescription.getExcludes());
			List<String> remove = jar.getResourceNames(excludes.matches())
				.collect(toList());
			getLogger().debug("Excluding {}", remove);

			remove.forEach(jar::remove);

			String inputName = input.getFile()
				.getAbsolutePath();
			String outputName = output.getAbsolutePath();

			transform(jar, inputName, outputName);

			writeOutput(jar, output);
		} catch (Exception e) {
			throw new MojoFailureException("Exception transforming jar", e);
		}
	}

	private String getExtension(String type) {
		ArtifactHandler artifactHandler = getArtifactHandlerManager().getArtifactHandler(type);
		if (artifactHandler != null) {
			type = artifactHandler.getExtension();
		}
		return type;
	}

	private File getOutput() throws IOException {
		String extension = getExtension(getType());
		String classifier = getClassifier();
		String name;
		if (StringUtils.isBlank(classifier)) {
			name = getBaseName() + "." + extension;
		} else {
			name = getBaseName() + "-" + classifier + "." + extension;
		}
		File output = IO.getBasedFile(getBuildDirectory(), name);
		return output;
	}

	private void writeOutput(Jar jar, File output) throws Exception {
		// https://maven.apache.org/guides/mini/guide-reproducible-builds.html
		String outputTimestamp = getOutputTimestamp();
		boolean isReproducible = StringUtils.isNotEmpty(outputTimestamp)
			// no timestamp configured (1 character configuration is useful
			// to override a full value during pom inheritance)
			&& ((outputTimestamp.length() > 1) || Character.isDigit(outputTimestamp.charAt(0)));

		if (isReproducible) {
			jar.setReproducible(outputTimestamp);
		}

		addMavenMetadataToJar(jar);

		IO.mkdirs(output.getParentFile());
		jar.write(output);
		getBuildContext().refresh(output);

		if (isAttach()) {
			String classifier = getClassifier();
			if (StringUtils.isBlank(classifier)) {
				getLogger().debug("Setting {} as project main artifact", output);
				Artifact main = getProject().getArtifact();
				main.setFile(output);
			} else {
				getLogger().debug("Attaching {} to project", output);
				getProjectHelper().attachArtifact(getProject(), getType(), classifier, output);
			}
		}
	}

	private void addMavenMetadataToJar(Jar jar) throws IOException {
		MavenProject project = getProject();
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String version = project.getArtifact()
			.isSnapshot()
				? project.getArtifact()
					.getVersion()
				: project.getVersion();

		jar.putResource(String.format("META-INF/maven/%s/%s/pom.xml", groupId, artifactId),
			new FileResource(project.getFile()));
		PomPropertiesResource pomProperties = new PomPropertiesResource(groupId, artifactId, version);
		jar.putResource(pomProperties.getWhere(), pomProperties);
	}

	private Artifact getArtifact(TransformerArtifact artifactDescription) throws MojoExecutionException {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
			getSession().getProjectBuildingRequest());
		buildingRequest.setRemoteRepositories(getRemoteRepositories());

		DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
		coordinate.setGroupId(artifactDescription.getGroupId());
		coordinate.setArtifactId(artifactDescription.getArtifactId());
		coordinate.setVersion(artifactDescription.getVersion());
		coordinate.setClassifier(artifactDescription.getClassifier());
		coordinate.setExtension(getExtension(artifactDescription.getType()));

		try {
			Artifact artifact = getArtifactResolver().resolveArtifact(buildingRequest, coordinate)
				.getArtifact();
			return artifact;
		} catch (ArtifactResolverException e) {
			throw new MojoExecutionException(String.format("Unable to resolve artifact %s", artifactDescription));
		}
	}

	private TransformerArtifact prepareArtifactDescription(TransformerArtifact artifactDescription)
		throws MojoExecutionException {
		MavenProject project = getProject();

		if (StringUtils.isBlank(artifactDescription.getType())) {
			artifactDescription.setType(project.getPackaging());
		}

		if (StringUtils.isBlank(artifactDescription.getVersion())) {
			List<Dependency> dependencies = new ArrayList<>();
			dependencies.add(toDependency(project.getArtifact()));
			for (Artifact artifact : project.getAttachedArtifacts()) {
				dependencies.add(toDependency(artifact));
			}
			dependencies.addAll(project.getDependencies());
			Optional.ofNullable(project.getDependencyManagement())
				.map(DependencyManagement::getDependencies)
				.ifPresent(dependencies::addAll);

			List<Dependency> firstPass = dependencies.stream()
				.filter(dependency -> Objects.equals(artifactDescription.getGroupId(), dependency.getGroupId())
					&& Objects.equals(artifactDescription.getArtifactId(), dependency.getArtifactId()))
				.toList();

			Optional<Dependency> matchingDependency = firstPass.stream()
				.filter(dependency -> Objects.equals(artifactDescription.getClassifier(), dependency.getClassifier())
					&& Objects.equals(artifactDescription.getType(), dependency.getType()))
				.findFirst();
			if (matchingDependency.isEmpty()) {
				matchingDependency = firstPass.stream()
					.findFirst();
				if (matchingDependency.isEmpty()) {
					throw new MojoExecutionException(String.format(
						"No version found for artifact %s:%s in project, dependencies, or dependency management",
						artifactDescription.getGroupId(), artifactDescription.getArtifactId()));
				}
			}
			artifactDescription.setVersion(matchingDependency.get()
				.getVersion());
		}
		return artifactDescription;
	}

	private static Dependency toDependency(Artifact artifact) {
		Dependency dependency = new Dependency();
		dependency.setGroupId(artifact.getGroupId());
		dependency.setArtifactId(artifact.getArtifactId());
		dependency.setVersion(artifact.getVersion());
		dependency.setClassifier(artifact.getClassifier());
		dependency.setType(artifact.getType());
		return dependency;
	}

	/**
	 * @return the remoteRepositories
	 */
	public List<ArtifactRepository> getRemoteRepositories() {
		return remoteRepositories;
	}

	/**
	 * @return the session
	 */
	public MavenSession getSession() {
		return session;
	}

	/**
	 * @return the projectHelper
	 */
	public MavenProjectHelper getProjectHelper() {
		return projectHelper;
	}

	/**
	 * @return the artifactResolver
	 */
	public ArtifactResolver getArtifactResolver() {
		return artifactResolver;
	}

	/**
	 * @return the artifactHandlerManager
	 */
	public ArtifactHandlerManager getArtifactHandlerManager() {
		return artifactHandlerManager;
	}

	public TransformerArtifact getArtifact() {
		return artifact;
	}

	public String getBaseName() {
		return baseName;
	}

	/**
	 * @return the buildDirectory
	 */
	public File getBuildDirectory() {
		return buildDirectory;
	}

	/**
	 * @return the outputTimestamp
	 */
	public String getOutputTimestamp() {
		return outputTimestamp;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getType() {
		return type;
	}

	public boolean isAttach() {
		return attach;
	}
}
