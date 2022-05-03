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
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

import aQute.lib.io.IO;
import aQute.lib.io.NonClosingInputStream;

public class RenameActionImpl extends ActionImpl<Changes> {

	public RenameActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule, SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "Rename Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.RENAME;
	}

	//

	@Override
	public boolean accept(String resourcePath, File resourceFile) {
		return true;
	}

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		String inputName = inputData.name();
		startRecording(inputName);
		try {
			String outputName = relocateResource(inputName);
			ByteData outputData = (outputName != null) ? new ByteDataImpl(outputName, inputData.buffer()) : inputData;
			setResourceNames(inputName, outputData.name());
			return outputData;
		} finally {
			stopRecording(inputName);
		}
	}

	/**
	 * Optimized method for file copy.
	 * <p>
	 * The caller must handle any renames for the outputFile.
	 */
	@Override
	public void apply(String inputName, File inputFile, File outputFile) throws TransformException {
		startRecording(inputName);
		try {
			setResourceNames(inputName, inputName);
			IO.mkdirs(outputFile.getParentFile());
			IO.copy(inputFile, outputFile);
		} catch (IOException e) {
			throw new TransformException("Failed to copy [" + inputFile + "] to [ " + outputFile + " ]", e);
		} finally {
			stopRecording(inputName);
		}
	}

	/**
	 * Optimized method for stream copy.
	 * <p>
	 * The caller must handle any renames for the outputStream.
	 */
	@Override
	public void apply(String inputName, InputStream inputStream, int inputCount, OutputStream outputStream)
		throws TransformException {
		startRecording(inputName);
		try {
			setResourceNames(inputName, inputName);
			InputStream nonClosingInputStream = new NonClosingInputStream(inputStream);
			if (inputCount < 0) {
				IO.copy(nonClosingInputStream, outputStream);
			} else {
				IO.copy(nonClosingInputStream, outputStream, inputCount);
			}
		} catch (IOException e) {
			throw new TransformException("Failed to write [ " + inputName + " ]" + " count [ " + inputCount + " ]", e);
		} finally {
			stopRecording(inputName);
		}
	}

	@Override
	public boolean useStreams() {
		return true;
	}
}
