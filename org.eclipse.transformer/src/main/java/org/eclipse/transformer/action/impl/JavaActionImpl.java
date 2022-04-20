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

public class JavaActionImpl extends ActionImpl<Changes> {

	public JavaActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule, SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "Java Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.JAVA;
	}

	//

	@Override
	public String getAcceptExtension() {
		return ".java";
	}

	//

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		startRecording(inputData.name());
		try {
			String outputName = inputData.name();
			setResourceNames(inputData.name(), outputName);

			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());

			try (BufferedReader reader = reader(inputData.buffer()); BufferedWriter writer = writer(outputStream)) {
				transform(reader, writer);
			} catch (IOException e) {
				throw new TransformException("Failed to transform [ " + inputData.name() + " ]", e);
			}

			if (!hasChanges()) {
				return inputData;
			}

			ByteBuffer outputBuffer = hasNonResourceNameChanges() ? outputStream.toByteBuffer() : inputData.buffer();
			ByteData outputData = new ByteDataImpl(outputName, outputBuffer);
			return outputData;
		} finally {
			stopRecording(inputData.name());
		}
	}

	protected void transform(BufferedReader reader, BufferedWriter writer) throws IOException {
		String inputLine;
		while ((inputLine = reader.readLine()) != null) {
			String outputLine = getSignatureRule().replacePackages(inputLine);
			if (outputLine == null) {
				outputLine = inputLine;
			} else {
				addReplacement();
			}
			writer.write(outputLine);
			writer.write('\n');
		}
	}

	// TODO: Copied from ServiceConfigActionImpl; need to update
	// to work for paths.

	protected String renameInput(String inputName) {
		String inputPrefix;
		String classQualifiedName;

		int lastSlash = inputName.lastIndexOf('/');
		if (lastSlash == -1) {
			inputPrefix = null;
			classQualifiedName = inputName;
		} else {
			inputPrefix = inputName.substring(0, lastSlash + 1);
			classQualifiedName = inputName.substring(lastSlash + 1);
		}

		int classStart = classQualifiedName.lastIndexOf('.');
		if (classStart == -1) {
			return null;
		}

		String packageName = classQualifiedName.substring(0, classStart);
		if (packageName.isEmpty()) {
			return null;
		}

		// 'className' includes a leading '.'
		String className = classQualifiedName.substring(classStart);

		String outputName = replacePackage(packageName);
		if (outputName == null) {
			return null;
		}

		if (inputPrefix == null) {
			return outputName + className;
		} else {
			return inputPrefix + outputName + className;
		}
	}
}
