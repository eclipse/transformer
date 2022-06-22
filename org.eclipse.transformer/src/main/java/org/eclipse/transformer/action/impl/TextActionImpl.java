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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.util.FileUtils;

import aQute.lib.io.ByteBufferOutputStream;

/**
 * Action for general text updates. This action performs text updates, either
 * for specific text resources, or for wildcard selected text resources.
 * <p>
 * Extended for Java and JSP resources. {@link JavaActionImpl} and
 * {@link JSPActionImpl} perform the usual text updates, and add in replacements
 * derived from class update rule data. The Java action adds package
 * replacements, direct string constant updates, and direct per-class string
 * constant updates. The JSP action adds package replacements and direct
 * constant updates.
 */
public class TextActionImpl extends ElementActionImpl {

	public TextActionImpl(ActionContext context) {
		super(context);

		List<StringReplacement> replacements = createActiveReplacements(context.getSignatureRule());
		this.activeReplacements = replacements.isEmpty() ? NO_ACTIVE_REPLACEMENTS : replacements;
	}

	private final List<StringReplacement> activeReplacements;

	@Override
	protected List<StringReplacement> getActiveReplacements() {
		return activeReplacements;
	}

	protected List<StringReplacement> createActiveReplacements(SignatureRule signatureRule) {
		List<StringReplacement> replacements = new ArrayList<>();
		if ( signatureRule.hasTextUpdates() ) {
			replacements.add(this::textUpdate);
		}
		return replacements;
	}

	@Override
	public String getName() {
		return "Text Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.TEXT;
	}

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return (getTextSubstitutions(resourceName) != null);
	}

	@Override
	public String getAcceptExtension() {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support this method");
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

			try (BufferedReader reader = inputData.reader(); BufferedWriter writer = FileUtils.writer(outputStream)) {
				transform(inputName, reader, writer);
			} catch (IOException e) {
				throw new TransformException("Failed to transform [ " + inputName + " ]", e);
			}

			if (!isChanged()) {
				return inputData;
			} else if (!isContentChanged()) {
				return inputData.copy(outputName);
			} else {
				return new ByteDataImpl(outputName, outputStream.toByteBuffer());
			}

		} finally {
			stopRecording(inputName);
		}
	}

	//

	protected void transform(String inputName, BufferedReader reader, BufferedWriter writer) throws IOException {
		String inputLine;
		while ((inputLine = reader.readLine()) != null) {
			String outputLine = transformString(inputName, "text line", inputLine);
			if (outputLine != null) {
				addReplacement(); // Count lines, not individual replacements.
			} else {
				outputLine = inputLine;
			}

			writer.write(outputLine);
			writer.write('\n');
		}
	}

	protected String transformString(String inputName, String inputCase, String initialValue) {
		return updateString(inputName, inputCase, initialValue);
	}
}
