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

package org.eclipse.transformer.bnd.analyzer.action;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionSelector;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.impl.ByteDataImpl;
import org.eclipse.transformer.action.impl.ContainerActionImpl;
import org.eclipse.transformer.action.impl.ElementActionImpl;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;

public class AnalyzerAction extends ContainerActionImpl {
	private final boolean		overwrite;

	public AnalyzerAction(ActionInitData initData, ActionSelector actionSelector, boolean overwrite) {
		super(initData, actionSelector);
		this.overwrite = overwrite;
	}

	@Override
	public String getName() {
		return "Bnd Analyzer Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.JAR;
	}

	public void apply(Analyzer analyzer) {
		Jar jar = analyzer.getJar();
		String manifestName = jar.getManifestName();
		String bundleSymbolicName = analyzer.getBsn();
		startRecording(bundleSymbolicName);
		try {
			setResourceNames(bundleSymbolicName, bundleSymbolicName);

			Map<String, Resource> resources = jar.getResources();
			List<String> inputPaths = new ArrayList<>(resources.size() + 1);
			if (!resources.containsKey(manifestName)) {
				inputPaths.add(manifestName); // process manifest first
			}
			inputPaths.addAll(resources.keySet());
			for (String inputPath : inputPaths) {
				Action action = selectAction(inputPath);
				if (action == null) {
					recordUnaccepted(inputPath);
					continue;
				} else if (!selectResource(inputPath)) {
					recordUnselected(inputPath);
					continue;
				} else if (!action.isElementAction()) {
					getLogger().warn("Strange non-element action [ {} ] for [ {} ]: Ignoring", action.getClass()
						.getName(), inputPath);
					recordUnaccepted(inputPath);
					continue;
				}

				ElementActionImpl elementAction = (ElementActionImpl) action;
				try {
					Resource resource = jar.getResource(inputPath);
					if (inputPath.equals(manifestName)) {
						if (resource == null) {
							Manifest manifest = jar.getManifest();
							if (manifest == null) {
								continue; // no calculated manifest
							}
							resource = new ManifestResource(manifest);
						}
					}
					ByteBuffer bb = resource.buffer();
					ByteData inputData;
					if (bb != null) {
						inputData = new ByteDataImpl(inputPath, bb);
					} else {
						inputData = collect(inputPath, resource.openInputStream(),
							Math.toIntExact(resource.size()));
					}
					ByteData outputData = elementAction.apply(inputData);
					recordAction(elementAction, inputPath);
					Changes changes = elementAction.getLastActiveChanges();
					if (changes.isChanged()) {
						String outputPath = outputData.name();
						getLogger().debug("[ {}.apply ]: Active transform [ {} ] [ {} ]", elementAction.getClass()
							.getSimpleName(), inputPath, outputPath);
						if (changes.isRenamed()) {
							if (!overwrite && (jar.getResource(outputPath) != null)) {
								analyzer.error(
									"Transform for %s overwrites existing resource %s. Use 'overwrite' option to allow overwriting.",
									inputPath, outputPath);
								continue;
							}
							jar.remove(inputPath);
						}
						Resource outputResource;
						if ( changes.isContentChanged() ) {
							outputResource = new EmbeddedResource(outputData.buffer(), resource.lastModified());
						} else {
							outputResource = resource;
						}
						jar.putResource(outputPath, outputResource);
					}
				} catch (Exception e) {
					analyzer.exception(e, "Failure while transforming %s", inputPath);
					recordError(elementAction, inputPath, e);
				}
			}
		} finally {
			stopRecording(bundleSymbolicName);
		}
	}

	// The analyzer action is a root action and has its own apply API.

	@Override
	public void apply(String inputName, File inputFile, String outputName, File outputFile) throws TransformException {
		throw new UnsupportedOperationException();
	}
}
