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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.ResultCode;
import org.eclipse.transformer.jakarta.JakartaTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms all the project's artifacts.
 * <p>
 * This goal is deprecated. Executions of the "jar" goal should be used instead.
 * <p>
 * This goal has the default phase of "package".
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, requiresProject = true, threadSafe = true)
public class TransformerRunMojo extends AbstractMojo {
	static final Logger			logger	= LoggerFactory.getLogger(TransformerRunMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject		project;

	@Parameter(defaultValue = "false", property = "transformer-plugin.invert", required = true)
	private boolean				invert;

	@Parameter(defaultValue = "true", property = "transformer-plugin.overwrite", required = true)
	private boolean				overwrite;

	@Parameter(defaultValue = "false", property = "transformer-plugin.stripSignatures", required = false)
	private boolean				stripSignatures;

	@Parameter(defaultValue = "true", property = "transformer-plugin.attach", required = true)
	private boolean				attach;

	@Parameter(property = "transformer-plugin.renames", defaultValue = "")
	private String				rulesRenamesUri;

	@Parameter(property = "transformer-plugin.versions", defaultValue = "")
	private String				rulesVersionUri;

	@Parameter(property = "transformer-plugin.bundles", defaultValue = "")
	private String				rulesBundlesUri;

	@Parameter(property = "transformer-plugin.direct", defaultValue = "")
	private String				rulesDirectUri;

	@Parameter(property = "transformer-plugin.per-class-constant", defaultValue = "")
	private String rulesPerClassConstantUri;

	@Parameter(property = "transformer-plugin.xml", defaultValue = "")
	private String				rulesXmlsUri;

	@Parameter(defaultValue = "transformed")
	private String				classifier;

	@Parameter(defaultValue = "${project.build.directory}", required = true)
	private File				outputDirectory;

	@Component
	private MavenProjectHelper	projectHelper;

	/**
	 * Main execution point of the plugin. This looks at the attached artifacts,
	 * and runs the transformer on them.
	 *
	 * @throws MojoFailureException Thrown if there is an error during plugin
	 *             execution
	 * @throws MojoExecutionException
	 */
	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {
		final Artifact[] sourceArtifacts = getSourceArtifacts();
		for (final Artifact sourceArtifact : sourceArtifacts) {
			transform(sourceArtifact);
		}
	}

	/**
	 * This runs the transformation process on the source artifact with the
	 * transformer provided. The transformed artifact is attached to the
	 * project.
	 *
	 * @param sourceArtifact The Artifact to transform
	 * @throws MojoFailureException if plugin execution fails
	 * @throws MojoExecutionException
	 */
	public void transform(final Artifact sourceArtifact) throws MojoFailureException, MojoExecutionException {
		final String sourceClassifier = sourceArtifact.getClassifier();
		final String targetClassifier = (sourceClassifier == null || sourceClassifier.isEmpty()) ? this.classifier
			: sourceClassifier + "-" + this.classifier;

		final File targetFile = new File(outputDirectory, sourceArtifact.getArtifactId() + "-" + targetClassifier + "-"
			+ sourceArtifact.getVersion() + "." + sourceArtifact.getType());
		TransformOptions options = new TransformOptions() {
			final Map<String, String>	optionDefaults	= JakartaTransform.getOptionDefaults();
			final Function<String, URL>		ruleLoader		= JakartaTransform.getRuleLoader();
			@Override
			public boolean hasOption(AppOption option) {
				return switch (option) {
					case OVERWRITE -> overwrite;
					case INVERT -> invert;
					case STRIP_SIGNATURES -> stripSignatures;
					default -> TransformOptions.super.hasOption(option);
				};
			}

			@Override
			public String getOptionValue(AppOption option) {
				return switch (option) {
					case RULES_RENAMES -> emptyAsNull(rulesRenamesUri);
					case RULES_VERSIONS -> emptyAsNull(rulesVersionUri);
					case RULES_BUNDLES -> emptyAsNull(rulesBundlesUri);
					case RULES_DIRECT -> emptyAsNull(rulesDirectUri);
					case RULES_MASTER_TEXT -> emptyAsNull(rulesXmlsUri);
					case RULES_PER_CLASS_CONSTANT -> emptyAsNull(rulesPerClassConstantUri);
					default -> null;
				};
			}

			@Override
			public List<String> getOptionValues(AppOption option) {
				String result = getOptionValue(option);
				if (Objects.nonNull(result)) {
					return Collections.singletonList(result);
				}
				return null;
			}

			@Override
			public String getDefaultValue(AppOption option) {
				return optionDefaults.get(option.getLongTag());
			}

			@Override
			public Function<String, URL> getRuleLoader() {
				return ruleLoader;
			}

			@Override
			public String getInputFileName() {
				return sourceArtifact.getFile()
					.getAbsolutePath();
			}

			@Override
			public String getOutputFileName() {
				return targetFile.getAbsolutePath();
			}
		};

		Transformer transformer = new Transformer(logger, options);

		ResultCode rc;
		try {
			rc = transformer.run();
		} catch (Exception e) {
			throw new MojoExecutionException("Transformer failed with an exception", e);
		}
		if (rc != ResultCode.SUCCESS_RC) {
			throw new MojoFailureException("Transformer failed with an error: " + rc);
		}

		if (attach) {
			projectHelper.attachArtifact(project, sourceArtifact.getType(), targetClassifier, targetFile);
		}
	}

	/**
	 * Gets the source artifacts that should be transformed
	 *
	 * @return an array to artifacts to be transformed
	 */
	public Artifact[] getSourceArtifacts() {
		List<Artifact> artifactList = new ArrayList<>();
		if (project.getArtifact() != null && project.getArtifact()
			.getFile() != null) {
			artifactList.add(project.getArtifact());
		}

		for (final Artifact attachedArtifact : project.getAttachedArtifacts()) {
			if (attachedArtifact.getFile() != null) {
				artifactList.add(attachedArtifact);
			}
		}

		return artifactList.toArray(new Artifact[0]);
	}

	private String emptyAsNull(String input) {
		return (Objects.nonNull(input) && !(input = input.trim()).isEmpty()) ? input : null;
	}

	void setProject(MavenProject project) {
		this.project = project;
	}

	void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	MavenProjectHelper getProjectHelper() {
		return projectHelper;
	}

	void setProjectHelper(MavenProjectHelper projectHelper) {
		this.projectHelper = projectHelper;
	}

	void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	void setAttach(boolean attach) {
		this.attach = attach;
	}

	void setStripSignatures(boolean stripSignatures) {
		this.stripSignatures = stripSignatures;
	}
}
