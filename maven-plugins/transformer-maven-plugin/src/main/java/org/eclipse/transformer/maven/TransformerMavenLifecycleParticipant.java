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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import aQute.bnd.unmodifiable.Sets;

/**
 * This lifecycle participant is meant to simplify the changes required to the
 * configuration of the maven packaging plugins when the
 * {@code transformer-maven-plugin} is used. It will silently "scan" projects,
 * and disable the {@code maven-jar-plugin} appropriately.
 * <p>
 * Lifecycle participants are only active when the host plugin
 * ({@code transformer-maven-plugin} in this case) has:
 * <p>
 * <code><pre>&lt;extensions&gt;true&lt;/extensions&gt;</pre></code>
 * <p>
 * This acts as the opt-in. Without it {@code maven-jar-plugin} behaves in the
 * traditional fashion.
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "transformer")
public class TransformerMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant implements LogEnabled {
	static final String			THIS_GROUP_ID					= "org.eclipse.transformer";

	static final String			THIS_ARTIFACT_ID				= "transformer-maven-plugin";

	static final String			MAVEN_JAR_PLUGIN_GROUP_ID		= "org.apache.maven.plugins";

	static final String			MAVEN_JAR_PLUGIN_ARTIFACT_ID	= "maven-jar-plugin";

	static final String			JAR_PACKAGING					= "jar";

	static final Set<String>	PACKAGING_GOALS	= Sets.of("jar", "test-jar");

	private Logger				logger;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		try {
			for (MavenProject project : session.getProjects()) {
				Model model = project.getModel();
				Plugin transformerMavenPlugin = getTransformerMavenPlugin(model);
				if (transformerMavenPlugin != null) {
					Plugin mavenJarPlugin = getMavenJarPlugin(model);
					if (mavenJarPlugin != null) {
						processExecutions(transformerMavenPlugin.getExecutions(), mavenJarPlugin, project);
					}
				}
			}
		} catch (IllegalStateException e) {
			throw new MavenExecutionException(e.getMessage(), e);
		}
	}

	@Override
	public void enableLogging(Logger logger) {
		this.logger = logger;
	}

	protected Optional<PluginExecution> findMatchingPluginExecution(List<PluginExecution> pluginExecutions,
		String classifier) {
		return pluginExecutions.stream()
			.filter(execution -> matchesClassifier(execution, classifier))
			.findFirst();
	}

	protected Plugin getTransformerMavenPlugin(Model model) {
		Build build = model.getBuild();
		if (build != null) {
			return getTransformerMavenPluginFromContainer(build);
		}
		return null;
	}

	protected Plugin getTransformerMavenPluginFromContainer(PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(THIS_GROUP_ID, THIS_ARTIFACT_ID, pluginContainer);
	}

	protected Plugin getMavenJarPlugin(Model model) {
		if (Objects.equals(model.getPackaging(), JAR_PACKAGING)) {
			Build build = model.getBuild();
			if (build != null) {
				return getMavenJarPluginFromContainer(build);
			}
		}
		return null;
	}

	protected Plugin getMavenJarPluginFromContainer(PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(MAVEN_JAR_PLUGIN_GROUP_ID, MAVEN_JAR_PLUGIN_ARTIFACT_ID, pluginContainer);
	}

	protected Plugin getPluginByGAFromContainer(String groupId, String artifactId, PluginContainer pluginContainer) {
		Plugin result = null;
		for (Plugin plugin : pluginContainer.getPlugins()) {
			if (nullToEmpty(groupId).equals(nullToEmpty(plugin.getGroupId()))
				&& nullToEmpty(artifactId).equals(nullToEmpty(plugin.getArtifactId()))) {
				if (result != null) {
					throw new IllegalStateException(
						"The build contains multiple versions of plugin " + groupId + ":" + artifactId);
				}
				result = plugin;
			}

		}
		return result;
	}

	protected String nullToEmpty(String str) {
		return Optional.ofNullable(str)
			.orElse("");
	}

	protected void processExecutions(List<PluginExecution> transformerExecutions, Plugin mavenPackagingPlugin,
		MavenProject project) {
		transformerExecutions.stream()
			.filter(TransformerMavenLifecycleParticipant::hasPackagingGoal)
			.forEach(transformerExecution -> {
				String classifier = extractClassifier(transformerExecution);
				findMatchingPluginExecution(mavenPackagingPlugin.getExecutions(), classifier).ifPresent(execution -> {
					List<String> goals = execution.getGoals();
					boolean removed = goals.removeIf(goal -> {
						if (isPackagingGoal(goal)) {
							if (logger.isDebugEnabled()) {
								logger.debug(THIS_ARTIFACT_ID + " disabled " + mavenPackagingPlugin.getArtifactId()
									+ ":" + goal + " (" + execution.getId() + ") @ " + project.getArtifactId());
							}
							return true;
						}
						return false;
					});
					if (removed && goals.isEmpty()) {
						mavenPackagingPlugin.removeExecution(execution);
					}
				});
			});
	}

	public static String defaultClassifier(PluginExecution execution) {
		List<String> goals = execution.getGoals();
		if (goals.contains("jar")) {
			return "";
		}
		if (goals.contains("test-jar")) {
			return "tests";
		}
		return "";
	}

	public static String extractClassifier(PluginExecution execution) {
		Optional<Xpp3Dom> rootConfiguration = Optional.ofNullable((Xpp3Dom) execution.getConfiguration());
		Optional<Xpp3Dom> classifierConfiguration = rootConfiguration
			.map(configuration -> configuration.getChild("classifier"));
		if (!classifierConfiguration.isPresent()) {
			classifierConfiguration = rootConfiguration.map(configuration -> configuration.getChild("artifact"))
				.map(configuration -> configuration.getChild("classifier"));
		}
		if (!classifierConfiguration.isPresent()) {
			return defaultClassifier(execution);
		}

		return classifierConfiguration.map(Xpp3Dom::getValue)
			.map(String::trim)
			.filter(s -> !s.isEmpty() && !Objects.equals("-", s))
			.orElse("");
	}

	public static boolean matchesClassifier(PluginExecution execution, String classifier) {
		return Objects.equals(extractClassifier(execution), classifier);
	}

	public static boolean isPackagingGoal(String goal) {
		return PACKAGING_GOALS.contains(goal);
	}

	public static boolean hasPackagingGoal(PluginExecution execution) {
		return execution.getGoals()
			.stream()
			.anyMatch(TransformerMavenLifecycleParticipant::isPackagingGoal);
	}
}
