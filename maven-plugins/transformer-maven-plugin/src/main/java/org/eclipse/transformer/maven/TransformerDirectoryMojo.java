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
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.transformer.maven.action.TransformerJarChanges;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

/**
 * Transforms the specified directory. This is normally the build output
 * directory.
 * <p>
 * This goal has the default phase of "process-classes".
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class TransformerDirectoryMojo extends AbstractTransformerMojo {
	/**
	 * The directory to transform.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File transformDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip()) {
			return;
		}

		File transformDirectory = getTransformDirectory();
		getLogger().debug("Transforming directory {}", transformDirectory);
		try (Jar jar = new Jar(transformDirectory)) {
			MojoExecution mojoExecution = getMojoExecution();
			String inputName = "transformer:" + mojoExecution.getGoal() + "@" + mojoExecution.getExecutionId();
			String outputName = inputName;

			TransformerJarChanges lastActiveChanges = transform(jar, inputName, outputName);

			writeOutput(jar, transformDirectory, lastActiveChanges.getChanged(), lastActiveChanges.getRemoved());
		} catch (Exception e) {
			throw new MojoFailureException("Exception transforming directory", e);
		}
	}

	private void writeOutput(Jar jar, File transformDirectory, Set<String> changed, Set<String> removed)
		throws Exception {
		getLogger().debug("Updating directory {}", transformDirectory);

		// Write changed files first, then remove
		getLogger().debug("Changed in outputDirectory {}", changed);
		for (String change : changed) {
			File file = IO.getBasedFile(transformDirectory, change);
			IO.mkdirs(file.getParentFile());
			Resource resource = jar.getResource(change);
			resource.write(file);
		}

		getLogger().debug("Removed from outputDirectory {}", removed);
		for (String remove : removed) {
			if (!changed.contains(remove)) {
				File file = IO.getBasedFile(transformDirectory, remove);
				IO.delete(file);
			}
		}

		getBuildContext().refresh(transformDirectory);
	}

	/**
	 * @return the transformDirectory
	 */
	public File getTransformDirectory() {
		return transformDirectory;
	}
}
