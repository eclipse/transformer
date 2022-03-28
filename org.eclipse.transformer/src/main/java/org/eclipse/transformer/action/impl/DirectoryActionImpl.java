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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

import aQute.lib.io.IO;

public class DirectoryActionImpl extends ContainerActionImpl {

	public DirectoryActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule,
		SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public ActionType getActionType() {
		return ActionType.DIRECTORY;
	}

	@Override
	public String getName() {
		return "Directory Action";
	}

	//

	/**
	 * The choice of using a stream or using an input stream should never occur
	 * on a directory action.
	 */
	@Override
	public boolean useStreams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return (resourceFile != null) && resourceFile.isDirectory();
	}

	@Override
	public void apply(String inputPath, File inputFile, File outputFile) throws TransformException {
		startRecording(inputPath);
		try {
			setResourceNames(inputPath, inputPath);
			/*
			 * Note the asymmetry between the handling of the root directory,
			 * which is selected by a composite action, and the handling of
			 * sub-directories, which are handled automatically by the directory
			 * action. This means that the directory action processes the entire
			 * tree of child directories. The alternative would be to put the
			 * directory action as a child of itself, and have sub-directories
			 * be accepted using composite action selection.
			 */
			if (inputFile.isDirectory()) {
				transformDirectory("", inputFile, outputFile);
			} else {
				transform("", inputFile, outputFile);
			}
		} finally {
			stopRecording(inputPath);
		}
	}

	protected void transformDirectory(String inputName, final File inputFile, final File outputFolder)
		throws TransformException {
		for (File childInputFile : inputFile.listFiles()) {
			try {
				transform(inputName, childInputFile, outputFolder);
			} catch (Exception e) {
				getLogger().error("Transform failure [ {} ]", inputName, e);
			}
		}
	}

	protected void transform(String inputName, final File inputFile, final File outputFolder)
		throws TransformException {
		inputName = inputName.isEmpty() ? inputFile.getName() : inputName + "/" + inputFile.getName();

		if (inputFile.isDirectory()) {
			transformDirectory(inputName, inputFile, outputFolder);
			return;
		}

		try {
			Action selectedAction = acceptAction(inputName, inputFile);
			if (selectedAction == null) {
				recordUnaccepted(inputName);
				copy(inputName, inputFile, outputFolder);
			} else if (!select(inputName)) {
				recordUnselected(selectedAction, inputName);
				copy(inputName, inputFile, outputFolder);
			} else if (selectedAction.getActionType() == ActionType.NULL) {
				/*
				 * For the NULL action type, we are not transforming the
				 * inputFile, so we use the optimized file-to-file apply method.
				 */
				File outputFile = new File(outputFolder, inputName);
				selectedAction.apply(inputName, inputFile, outputFile);
				recordTransform(selectedAction, inputName);
			} else {
				ByteData inputData;
				try (InputStream inputStream = IO.stream(inputFile)) {
					inputData = selectedAction.collect(inputName, inputStream, Math.toIntExact(inputFile.length()));
				} catch (IOException e) {
					throw new TransformException("Failed to read input [ " + inputFile + " ]", e);
				}
				ByteData outputData;
				try {
					outputData = selectedAction.apply(inputData);
					recordTransform(selectedAction, inputName);
					if (selectedAction.getLastActiveChanges()
						.hasChanges()) {
						getLogger().debug("[ {}.apply ]: Active transform [ {} ] [ {} ]", selectedAction.getClass()
							.getSimpleName(), inputName, outputData.name());
					}
				} catch (TransformException t) {
					// Fall back and copy
					outputData = inputData;
					getLogger().error(t.getMessage(), t);
				}
				File outputFile = new File(outputFolder, outputData.name());
				IO.mkdirs(outputFile.getParentFile());
				try (OutputStream outputStream = IO.outputStream(outputFile)) {
					write(outputData, outputStream);
				} catch (IOException e) {
					throw new TransformException("Failed to write output [ " + outputFile + " ]", e);
				}
			}
		} catch (Exception e) {
			getLogger().error("Transform failure [ {} ]", inputName, e);
		}
	}

	private void copy(String inputName, File inputFile, File outputFolder) throws IOException {
		File outputFile = new File(outputFolder, inputName);
		IO.mkdirs(outputFile.getParentFile());
		IO.copy(inputFile, outputFile);
	}
}
