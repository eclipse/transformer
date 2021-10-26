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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

public class TextActionImpl extends ActionImpl {

	public TextActionImpl(Logger logger, boolean isTerse, boolean isVerbose, boolean isExtraDebug,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, isExtraDebug, buffer, selectionRule, signatureRule);
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
	public String getAcceptExtension() {
		throw new UnsupportedOperationException("Text does not use this API");
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		if (signatureRule.getTextSubstitutions(resourceName) != null) {
			return true;
		}
		return false;
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws TransformException {

		String outputName = inputName;

		setResourceNames(inputName, outputName);

		InputStream inputStream = new ByteBufferInputStream(inputBytes, 0, inputLength);
		InputStreamReader inputReader = new InputStreamReader(inputStream, UTF_8);

		BufferedReader reader = new BufferedReader(inputReader);

		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputBytes.length);
		OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, UTF_8);

		BufferedWriter writer = new BufferedWriter(outputWriter);

		try {
			transform(inputName, reader, writer); // throws IOException
		} catch (IOException e) {
			error("Failed to transform [ {} ]", e, inputName);
			return null;
		}

		try {
			writer.flush(); // throws
		} catch (IOException e) {
			error("Failed to flush [ {} ]", e, inputName);
			return null;
		}

		if (!hasNonResourceNameChanges()) {
			return null;
		}

		byte[] outputBytes = outputStream.toByteArray();
		return new ByteData(inputName, outputBytes, 0, outputBytes.length);
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
