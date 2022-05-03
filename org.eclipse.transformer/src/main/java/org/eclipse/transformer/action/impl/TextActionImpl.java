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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

import aQute.lib.io.ByteBufferOutputStream;

public class TextActionImpl extends ActionImpl<Changes> {

	public TextActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule, SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "Text Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.TEXT;
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		if (getSignatureRule().getTextSubstitutions(resourceName) != null) {
			return true;
		}
		return false;
	}

	//

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		String inputName = inputData.name();
		startRecording(inputName);
		try {
			String outputName = relocateResource(inputName);

			setResourceNames(inputName, outputName);

			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());

			try (BufferedReader reader = reader(inputData.buffer()); BufferedWriter writer = writer(outputStream)) {
				transform(inputName, reader, writer);
			} catch (IOException e) {
				throw new TransformException("Failed to transform [ " + inputName + " ]", e);
			}

			if (!hasChanges()) {
				return inputData;
			}

			ByteBuffer outputBuffer = hasNonResourceNameChanges() ? outputStream.toByteBuffer() : inputData.buffer();
			ByteData outputData = new ByteDataImpl(outputName, outputBuffer);
			return outputData;
		} finally {
			stopRecording(inputName);
		}
	}

	//

	protected void transform(String inputName, BufferedReader reader, BufferedWriter writer) throws IOException {
		String inputLine;
		while ((inputLine = reader.readLine()) != null) {
			String outputLine = replaceText(inputName, inputLine);
			if (outputLine == null) {
				outputLine = inputLine;
			} else {
				addReplacement();
			}
			writer.write(outputLine);
			writer.write('\n');
		}
	}
}
