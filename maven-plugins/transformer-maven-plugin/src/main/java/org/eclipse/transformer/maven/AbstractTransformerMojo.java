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

import java.net.URI;
import java.util.Set;

import aQute.bnd.osgi.Jar;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.transformer.AppOption;
import org.eclipse.transformer.TransformOptions;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.maven.action.TransformerJarAction;
import org.eclipse.transformer.maven.action.TransformerJarChanges;
import org.eclipse.transformer.maven.configuration.TransformerRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Base Mojo class.
 */
public abstract class AbstractTransformerMojo extends AbstractMojo {
	private final Logger			logger	= LoggerFactory.getLogger(getClass());

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject			project;

	@Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
	private MojoExecution			mojoExecution;

	/**
	 * The project packaging types which will not skip the goal.
	 * <p>
	 * If the project packaging is not in the list, such as {@code pom}, then
	 * the goal will be skipped.
	 */
	@Parameter(property = "transformer.packagingTypes", defaultValue = "jar,war,ear,ejb,ejb3,par,rar,maven-plugin")
	private Set<String>				packagingTypes;

	/**
	 * The transformation rules.
	 * <p>
	 * The rules configuration includes: selections, renames, versions, bundles,
	 * directs, texts, perClassConstants, immediates, invert, overwrite, widen,
	 * jakartaDefaults, and stripSignatures.
	 */
	@Parameter
	private TransformerRules		rules	= new TransformerRules();

	/**
	 * Skip executing this goal.
	 * <p>
	 * The default value is false. The value can be set by the
	 * {@code transform.skip} property.
	 */
	@Parameter(property = "transform.skip", defaultValue = "false")
	private boolean					skip;

	@Component
	private BuildContext			buildContext;

	public boolean skip() {
		// Exit without generating anything if this project is not a known
		// packaging type. Probably it's just a parent project.
		if (!getPackagingTypes().contains(getProject().getPackaging())) {
			getLogger().debug("skip project with packaging=" + getProject().getPackaging());
			return true;
		}

		if (isSkip()) {
			getLogger().info("skip project as configured");
			return true;
		}
		return false;
	}

	public TransformerJarChanges transform(Jar jar, String inputName, String outputName)
		throws MojoExecutionException, MojoFailureException {
		TransformOptions options = new TransformerMojoOptions(getRules());

		Transformer transformer = new Transformer(getLogger(), options);
		// For use as the resolve base
		URI base = getProject().getBasedir()
			.toURI();
		getLogger().debug("Setting Transformer base {}", base);
		transformer.setBase(base);

		boolean validRules;
		try {
			validRules = transformer.setRules(transformer.getImmediateData());
		} catch (Exception e) {
			throw new MojoExecutionException("Exception loading transformer rules", e);
		}
		if (!validRules) {
			throw new MojoFailureException("Transformation rules are not valid and cannot be used");
		}
		transformer.logRules();

		TransformerJarAction action = new TransformerJarAction(transformer.getActionContext(),
			transformer.getActionSelector(),
			options.hasOption(AppOption.OVERWRITE));
		action.apply(jar, inputName, outputName);

		TransformerJarChanges lastActiveChanges = action.getLastActiveChanges();
		lastActiveChanges.log(getLogger(), lastActiveChanges.getInputResourceName(),
			lastActiveChanges.getOutputResourceName());

		return lastActiveChanges;
	}

	public boolean isSkip() {
		return skip;
	}

	public TransformerRules getRules() {
		return rules;
	}

	public MavenProject getProject() {
		return project;
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * @return the buildContext
	 */
	public BuildContext getBuildContext() {
		return buildContext;
	}

	/**
	 * @return the mojoExecution
	 */
	public MojoExecution getMojoExecution() {
		return mojoExecution;
	}

	/**
	 * @return the packagingTypes
	 */
	public Set<String> getPackagingTypes() {
		return packagingTypes;
	}
}
