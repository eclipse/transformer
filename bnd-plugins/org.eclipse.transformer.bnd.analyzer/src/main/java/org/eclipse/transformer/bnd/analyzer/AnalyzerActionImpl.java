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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.CompositeAction;
import org.eclipse.transformer.action.impl.ByteDataImpl;
import org.eclipse.transformer.action.impl.ContainerActionImpl;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;

public class AnalyzerActionImpl extends ContainerActionImpl {
	private final boolean		overwrite;

	public AnalyzerActionImpl(CompositeAction rootAction, boolean overwrite) {
		super(rootAction);
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
		String inputPath = analyzer.getBsn();
		startRecording(inputPath);
		try {
			setResourceNames(inputPath, inputPath);

			Map<String, Resource> resources = jar.getResources();
			List<String> paths = new ArrayList<>(resources.size() + 1);
			if (!resources.containsKey(manifestName)) {
				paths.add(manifestName); // process manifest first
			}
			paths.addAll(resources.keySet());
			for (String path : paths) {
				Action selectedAction = acceptAction(path);
				if (selectedAction == null) {
					recordUnaccepted(path);
					continue;
				}
				if (!select(path)) {
					recordUnselected(selectedAction, path);
					continue;
				}
				try {
					Resource resource = jar.getResource(path);
					if (path.equals(manifestName)) {
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
						inputData = new ByteDataImpl(path, bb);
					} else {
						inputData = selectedAction.collect(path, resource.openInputStream(),
							Math.toIntExact(resource.size()));
					}
					ByteData outputData = selectedAction.apply(inputData);
					recordTransform(selectedAction, path);
					Changes changes = selectedAction.getLastActiveChanges();
					if (changes.hasChanges()) {
						String outputPath = outputData.name();
						getLogger().debug("[ {}.apply ]: Active transform [ {} ] [ {} ]", selectedAction.getClass()
							.getSimpleName(), inputPath, outputPath);
						if (changes.hasResourceNameChange()) {
							if (!overwrite && (jar.getResource(outputPath) != null)) {
								analyzer.error(
									"Transform for %s overwrites existing resource %s. Use 'overwrite' option to allow overwriting.",
									path, outputPath);
								continue;
							}
							jar.remove(path);
						}
						Resource outputResource = changes.hasNonResourceNameChanges()
							? new EmbeddedResource(outputData.buffer(), resource.lastModified())
							: resource;
						jar.putResource(outputPath, outputResource);
					}
				} catch (Exception e) {
					analyzer.exception(e, "Failure while transforming %s", path);
				}
			}
		} finally {
			stopRecording(inputPath);
		}
	}
}
