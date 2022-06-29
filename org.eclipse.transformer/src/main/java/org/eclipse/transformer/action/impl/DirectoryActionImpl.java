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

package org.eclipse.transformer.action.impl;

import java.io.File;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.ElementAction;
import org.eclipse.transformer.action.RenameAction;
import org.eclipse.transformer.util.FileUtils;

/**
 * Top level directory action. A directory action is a container action which
 * iterates recursively, across directory entries, selecting and applying
 * actions on each of these entries and aggregating the changes.
 * <p>
 * Note that the directory action embeds steps to recursively walk the target
 * directory. The directory action does not recursively invoke itself.
 */
public class DirectoryActionImpl extends ContainerActionImpl {

	public DirectoryActionImpl(ActionContext context) {
		super(context);
	}

	//

	@Override
	public ActionType getActionType() {
		return ActionType.DIRECTORY;
	}

	//

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return (resourceFile != null) && resourceFile.isDirectory();
	}

	//

	// Only the transformer initiates directory transformation.

	/**
	 * Transform a directory.
	 *
	 * @param rootInputPath The path to the input root file.
	 * @param rootInputFile The root input file.
	 * @param rootOutputFile The root output file.
	 * @throws TransformException Thrown in case of a transformation error.
	 *             Currently, never thrown: A failure to transform a single file
	 *             does not cause the transformation to fail as a whole. The
	 *             single file is copied, an error is emitted, and the
	 *             transformation continues.
	 */
	@Override
	public void apply(String rootInputPath, File rootInputFile, String rootOutputPath, File rootOutputFile)
		throws TransformException {
		startRecording(rootInputPath);

		try {
			setResourceNames(rootInputPath, rootOutputPath);

			if (rootInputFile.isDirectory()) {
				transformDirectory("", rootInputFile, rootOutputFile);
			} else {
				transformFile("", rootInputFile, rootOutputFile);
			}

		} finally {
			stopRecording(rootInputPath);
		}
	}

	/**
	 * Transform a directory. This is done by recursively walking the files of
	 * the directory. Failures do not cause processing as a whole to fail.
	 * <p>
	 * Output directories are not created except as needed for individual
	 * transformed files.
	 * <p>
	 * Because output files may be renamed, output files may not be created in
	 * the same relative order as the input files.
	 *
	 * @param pathFromRoot The path to the directory from the input root.
	 * @param inputDirectory The directory which is to be transformed.
	 * @param rootOutputFile The root output file.
	 */
	protected void transformDirectory(String pathFromRoot, File inputDirectory, File rootOutputFile) {
		for (File child : inputDirectory.listFiles()) {

			String childPathFromRoot;
			if (pathFromRoot.isEmpty()) {
				childPathFromRoot = child.getName();
			} else {
				childPathFromRoot = pathFromRoot + '/' + child.getName();
			}

			// Directories are specifically not transformed.
			//
			// The problem is what to do with empty directories.
			//
			// Both answers (do transform, do not transform) have problems.
			//
			// For simplicity, we have chosen to not transform them.

			if (child.isDirectory()) {
				transformDirectory(childPathFromRoot, child, rootOutputFile);
			} else {
				transformFile(childPathFromRoot, child, rootOutputFile);
			}
		}
	}

	// TODO: Add duplicate checking when --overwrite is not enabled.
	//
	// See issue #306.

	/**
	 * Transform a single file which is known to not be a directory.
	 * <p>
	 * Failure to transform a single file does not cause processing as a whole
	 * to fail: When a single file fails to transform, an error is logged, and
	 * the file is instead copied. If the copy fails, or if the write of a
	 * transformed file fails, an error is logged, and processing continues.
	 *
	 * @param pathFromRoot The path from the root of the target file. This
	 *            includes the name of the file.
	 * @param inputFile The file which is to be transformed.
	 * @param rootOutputFile The root output file.
	 */
	protected void transformFile(String pathFromRoot, File inputFile, File rootOutputFile) {
		Action action = selectAction(pathFromRoot, inputFile);
		try {
			if (action == null) {
				copyInto(pathFromRoot, inputFile, rootOutputFile);
				recordUnaccepted(pathFromRoot);
			} else if (!selectResource(pathFromRoot)) {
				// Unselected resources are *not* renamed.
				// The expectation is that files which are deliberately
				// omitted should not be transformed in any way.
				copyInto(pathFromRoot, inputFile, rootOutputFile);
				recordUnselected(pathFromRoot);

			} else if (action.isRenameAction()) {
				RenameAction renameAction = (RenameAction) action;
				String outputPathFromRoot = renameAction.apply(pathFromRoot);
				outputPathFromRoot = FileUtils.sanitize(outputPathFromRoot);
				copyInto(pathFromRoot, inputFile, rootOutputFile, outputPathFromRoot);
				recordAction(action, pathFromRoot);

			} else if (action.isArchiveAction()) {
				ZipActionImpl zipAction = (ZipActionImpl) action;

				String outputPathFromRoot = zipAction.relocateResource(pathFromRoot);
				outputPathFromRoot = FileUtils.sanitize(outputPathFromRoot);
				File outputFile = new File(rootOutputFile, outputPathFromRoot);

				zipAction.apply(pathFromRoot, inputFile, outputPathFromRoot, outputFile);
				recordAction(zipAction, pathFromRoot);

			} else if (!action.isElementAction()) {
				getLogger().warn("Strange: Unknown action type [ {} ] for [ {} ]", action.getClass()
					.getName(), inputFile.getAbsolutePath());

				copyInto(pathFromRoot, inputFile, rootOutputFile);
				recordUnaccepted(pathFromRoot);

			} else {
				ElementAction elementAction = (ElementAction) action;
				transformFile(elementAction, pathFromRoot, inputFile, rootOutputFile);
				recordAction(elementAction, pathFromRoot);
			}

		} catch (Throwable th) {
			recordError(action, pathFromRoot, th);
		}
	}

	private void transformFile(ElementAction elementAction, String inputName, File inputFile, File outputRoot)
		throws TransformException {
		ByteData inputData = collect(inputName, inputFile);

		ByteData outputData;
		TransformException transformError;
		try {
			outputData = elementAction.apply(inputData);
			transformError = null;
		} catch (TransformException t) {
			outputData = inputData; // Fallback: copy.
			transformError = t;
		}

		File outputFile = new File(outputRoot, outputData.name());
		write(outputData, outputFile);

		if (transformError != null) {
			throw transformError;
		}
	}
}
