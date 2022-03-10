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
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.action.SignatureRule.SignatureType;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;
import aQute.lib.io.IO;

public abstract class ActionImpl<CHANGES extends Changes> implements Action {
	public ActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule, SignatureRule signatureRule) {

		this.logger = logger;

		this.buffer = buffer;

		this.selectionRule = selectionRule;
		this.signatureRule = signatureRule;

		this.changes = new ArrayDeque<>();
		this.activeChanges = null;
		this.lastActiveChanges = null;
	}

	//

	public interface ActionInit<A extends Action> {
		A apply(Logger logger, InputBuffer buffer, SelectionRule selectionRule, SignatureRule signatureRule);
	}

	public <A extends Action> A createUsing(ActionInit<A> init) {
		return init.apply(getLogger(), getBuffer(), getSelectionRule(),
			getSignatureRule());
	}

	//

	private final Logger	logger;
	public Logger getLogger() {
		return logger;
	}

	//

	private final InputBuffer buffer;

	@Override
	public InputBuffer getBuffer() {
		return buffer;
	}

	public ByteBuffer getInputBuffer() {
		return getBuffer().getInputBuffer();
	}

	public void setInputBuffer(ByteBuffer inputBuffer) {
		getBuffer().setInputBuffer(inputBuffer);
	}

	//

	private final SelectionRule selectionRule;

	public SelectionRule getSelectionRule() {
		return selectionRule;
	}

	@Override
	public boolean select(String resourceName) {
		return getSelectionRule().select(resourceName);
	}

	public boolean selectIncluded(String resourceName) {
		return getSelectionRule().selectIncluded(resourceName);
	}

	public boolean rejectExcluded(String resourceName) {
		return getSelectionRule().rejectExcluded(resourceName);
	}

	//

	protected final SignatureRule signatureRule;

	@Override
	public SignatureRule getSignatureRule() {
		return signatureRule;
	}

	public BundleData getBundleUpdate(String symbolicName) {
		return getSignatureRule().getBundleUpdate(symbolicName);
	}

	public Map<String, String> getPackageRenames() {
		return getSignatureRule().getPackageRenames();
	}

	public Map<String, String> getPackageVersions() {
		return getSignatureRule().getPackageVersions();
	}

	public Map<String, Map<String, String>> getSpecificPackageVersions() {
		return getSignatureRule().getSpecificPackageVersions();
	}

	public String getReplacementVersion(String attributeName, String packageName, String oldVersion) {
		Map<String, String> versionsForAttribute = getSpecificPackageVersions().get(attributeName);

		String specificVersion;
		if ( versionsForAttribute != null ) {
			specificVersion = versionsForAttribute.get(packageName);
		} else {
			specificVersion = null;
		}

		String genericVersion = getPackageVersions().get(packageName);

		// System.out.println("Attribute [ " + attributeName + " ] Package [ " + packageName + " ]");
		// System.out.println("  Generic version  [ " + genericVersion + " ]");
		// System.out.println("  Specific version [ " + specificVersion + " ]");

		if ( (specificVersion == null) && (genericVersion == null) ) {
			getLogger().debug("Manifest attribute {}: Package {} version {} is unchanged",
				attributeName, packageName, oldVersion);
			return null;
		} else if (specificVersion == null) {
			getLogger().debug("Manifest attribute {}: Generic update of package {} version {} to {}",
				attributeName, packageName, oldVersion, genericVersion);
			return genericVersion;
		} else if (genericVersion == null) {
			getLogger().debug("Manifest attribute {}: Specific update of package {} version {} to {}",
				attributeName, packageName, oldVersion, specificVersion);
			return specificVersion;
		} else {
			getLogger().debug(
				"Manifest attribute {}: Specific update of package {} version {} to {} overrides generic version update {}",
				attributeName, packageName, oldVersion, specificVersion, genericVersion);
			return specificVersion;
		}
	}

	public String replacePackage(String initialName) {
		return getSignatureRule().replacePackage(initialName);
	}

	public String replaceBinaryPackage(String initialName) {
		return getSignatureRule().replaceBinaryPackage(initialName);
	}

	public String replaceEmbeddedPackages(String embeddingText) {
		return getSignatureRule().replacePackages(embeddingText);
	}

	public String replaceText(String inputFileName, String text) {
		return getSignatureRule().replaceText(inputFileName, text);
	}

	public String transformConstantAsBinaryType(String inputConstant) {
		return getSignatureRule().transformConstantAsBinaryType(inputConstant);
	}

	public String transformConstantAsBinaryType(String inputConstant, boolean simpleSubstitution) {
		return getSignatureRule().transformConstantAsBinaryType(inputConstant, simpleSubstitution);
	}

	public String transformBinaryType(String inputName) {
		return getSignatureRule().transformBinaryType(inputName);
	}

	public String transformConstantAsDescriptor(String inputConstant) {
		return getSignatureRule().transformConstantAsDescriptor(inputConstant);
	}

	public String transformConstantAsDescriptor(String inputConstant, boolean simpleSubstitution) {
		return getSignatureRule().transformConstantAsDescriptor(inputConstant, simpleSubstitution);
	}

	public String transformDescriptor(String inputDescriptor) {
		return getSignatureRule().transformDescriptor(inputDescriptor);
	}

	public String transform(String input, SignatureType signatureType) {
		return getSignatureRule().transform(input, signatureType);
	}

	public ClassSignature transform(ClassSignature classSignature) {
		return getSignatureRule().transform(classSignature);
	}

	public FieldSignature transform(FieldSignature fieldSignature) {
		return getSignatureRule().transform(fieldSignature);
	}

	public MethodSignature transform(MethodSignature methodSignature) {
		return getSignatureRule().transform(methodSignature);
	}

	public Result transform(Result type) {
		return getSignatureRule().transform(type);
	}

	public ThrowsSignature transform(ThrowsSignature type) {
		return getSignatureRule().transform(type);
	}

	public ArrayTypeSignature transform(ArrayTypeSignature inputType) {
		return getSignatureRule().transform(inputType);
	}

	public TypeParameter transform(TypeParameter inputTypeParameter) {
		return getSignatureRule().transform(inputTypeParameter);
	}

	public ClassTypeSignature transform(ClassTypeSignature inputType) {
		return getSignatureRule().transform(inputType);
	}

	public SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature) {
		return getSignatureRule().transform(inputSignature);
	}

	public TypeArgument transform(TypeArgument inputArgument) {
		return getSignatureRule().transform(inputArgument);
	}

	public JavaTypeSignature transform(JavaTypeSignature type) {
		return getSignatureRule().transform(type);
	}

	public ReferenceTypeSignature transform(ReferenceTypeSignature type) {
		return getSignatureRule().transform(type);
	}

	public String transformDirectString(String initialValue) {
		return getSignatureRule().getDirectString(initialValue);
	}

	public String transformDirectString(String initialValue, String className) {
		return getSignatureRule().getDirectString(initialValue, className);
	}

	//

	public abstract String getAcceptExtension();

	@Override
	public boolean accept(String resourceName) {
		return accept(resourceName, null);
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return resourceName.toLowerCase()
			.endsWith(getAcceptExtension());
	}

	//

	@SuppressWarnings("unchecked")
	protected CHANGES newChanges() {
		return (CHANGES) new ChangesImpl();
	}

	protected final Deque<CHANGES>	changes;
	protected CHANGES				activeChanges;
	protected CHANGES				lastActiveChanges;

	protected void startRecording(String inputName) {
		getLogger().debug("Start processing [ {} ] using [ {} ]", inputName, getActionType());

		activeChanges = newChanges();
		changes.addLast(activeChanges);
	}

	protected void stopRecording(String inputName) {
		if (getLogger().isDebugEnabled()) {
			String changeText;

			boolean nameChanged = activeChanges.hasResourceNameChange();
			boolean contentChanged = activeChanges.hasNonResourceNameChanges();

			if (nameChanged && contentChanged) {
				changeText = "Name and content changes";
			} else if (nameChanged) {
				changeText = "Name changes";
			} else if (contentChanged) {
				changeText = "Content changes";
			} else {
				changeText = "No changes";
			}

			getLogger().debug("Stop processing [ {} ] using [ {} ]: {}", inputName, getActionType(), changeText);
		}

		lastActiveChanges = activeChanges;
		activeChanges = changes.pollLast();
	}

	//

	@Override
	public CHANGES getActiveChanges() {
		return activeChanges;
	}

	protected void setResourceNames(String inputResourceName, String outputResourceName) {
		CHANGES useChanges = getActiveChanges();
		useChanges.setInputResourceName(inputResourceName);
		useChanges.setOutputResourceName(outputResourceName);
	}

	@Override
	public void addReplacement() {
		getActiveChanges().addReplacement();
	}

	@Override
	public void addReplacements(int additions) {
		getActiveChanges().addReplacements(additions);
	}

	//

	@Override
	public boolean hasChanges() {
		return getActiveChanges().hasChanges();
	}

	@Override
	public boolean hasResourceNameChange() {
		return getActiveChanges().hasResourceNameChange();
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return getActiveChanges().hasNonResourceNameChanges();
	}

	//

	@Override
	public CHANGES getLastActiveChanges() {
		return lastActiveChanges;
	}

	@Override
	public boolean hadChanges() {
		return getLastActiveChanges().hasChanges();
	}

	@Override
	public boolean hadResourceNameChange() {
		return getLastActiveChanges().hasResourceNameChange();
	}

	@Override
	public boolean hadNonResourceNameChanges() {
		return getLastActiveChanges().hasNonResourceNameChanges();
	}

	//

	@Override
	public boolean useStreams() {
		return false;
	}

	/**
	 * Read bytes from an input stream. Answer byte data and a count of bytes
	 * read.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream A stream to be read.
	 * @param inputCount The count of bytes to read from the stream. {@code -1}
	 *            if the count of input bytes is not known.
	 * @return Byte data from the read.
	 * @throws TransformException Indicates a read failure.
	 */
	protected ByteData read(String inputName, InputStream inputStream, int inputCount)
		throws TransformException {

		ByteBuffer readData = getInputBuffer();
		try {
			readData = FileUtils.read(inputName, inputStream, readData, inputCount);
		} catch (IOException e) {
			throw new TransformException("Failed to read raw bytes [ " + inputName + " ] count [ " + inputCount + " ]",
				e);
		}

		setInputBuffer(readData);

		return new ByteDataImpl(inputName, readData);
	}

	/**
	 * Write data to an output stream. Convert any exception thrown when
	 * attempting the write into a {@link TransformException}.
	 *
	 * @param outputData Data to be written.
	 * @param outputStream Stream to which to write the data.
	 * @throws TransformException Thrown in case of a write failure.
	 */
	protected void write(ByteData outputData, OutputStream outputStream) throws TransformException {
		try {
			IO.copy(outputData.buffer(), outputStream);
		} catch (IOException e) {
			throw new TransformException("Failed to write [ " + outputData.name() + " ]" +  " count [ " + outputData.length() + " ]", e);
		}
	}

	//

	@Override
	public ByteData apply(String inputName, InputStream inputStream) throws TransformException {
		return apply(inputName, inputStream, -1);
	}

	@Override
	public ByteData apply(String inputName, InputStream inputStream, int inputCount) throws TransformException {
		return basicApply(inputName, inputStream, inputCount);
	}

	private void basicApply(String inputName, InputStream inputStream, long inputCount, OutputStream outputStream) {
		int intInputCount;
		if (inputCount == -1L) {
			intInputCount = -1;
		} else {
			intInputCount = FileUtils.verifyArray(0, inputCount);
		}
		ByteData outputData = basicApply(inputName, inputStream, intInputCount);
		write(outputData, outputStream);
	}

	private ByteData basicApply(String inputName, InputStream inputStream, int inputCount) {
		startRecording(inputName);
		try {
			String className = getClass().getSimpleName();
			String methodName = "apply";

			getLogger().debug("[ {}.{} ]: Requested [ {} ] [ {} ]", className, methodName, inputName, inputCount);
			ByteData inputData = read(inputName, inputStream, inputCount);
			getLogger().debug("[ {}.{} ]: Obtained [ {} ] [ {} ]", className, methodName, inputName,
				inputData.length());

			ByteData outputData;
			try {
				outputData = apply(inputData);
			} catch (Throwable th) {
				getLogger().error("Transform failure [ {} ]", inputName, th);
				outputData = null;
			}

			if (outputData == null) {
				getLogger().debug("[ {}.{} ]: Null transform", className, methodName);
				outputData = inputData;
			} else {
				getLogger().debug("[ {}.{} ]: Active transform [ {} ] [ {} ]", className, methodName, outputData.name(),
					outputData.length());
			}
			return outputData;
		} finally {
			stopRecording(inputName);
		}
	}

	@Override
	public void apply(String inputName, InputStream inputStream, long inputCount, OutputStream outputStream)
		throws TransformException {
		basicApply(inputName, inputStream, inputCount, outputStream);
	}

	@Override
	public void apply(String inputName, File inputFile, File outputFile) throws TransformException {
		long inputLength = inputFile.length();
		getLogger().debug("Input [ {} ] Length [ {} ]", inputName, inputLength);

		InputStream inputStream = openInputStream(inputFile);
		try {
			OutputStream outputStream = openOutputStream(outputFile);
			try {
				apply(inputName, inputStream, inputLength, outputStream);
			} finally {
				closeOutputStream(outputFile, outputStream);
			}
		} finally {
			closeInputStream(inputFile, inputStream);
		}
	}

	//

	protected InputStream openInputStream(File inputFile) throws TransformException {
		try {
			return IO.stream(inputFile);
		} catch (IOException e) {
			throw new TransformException("Failed to open input [ " + inputFile.getAbsolutePath() + " ]", e);
		}
	}

	protected void closeInputStream(File inputFile, InputStream inputStream) throws TransformException {
		try {
			inputStream.close();
		} catch (IOException e) {
			throw new TransformException("Failed to close input [ " + inputFile.getAbsolutePath() + " ]", e);
		}
	}

	private OutputStream openOutputStream(File outputFile) throws TransformException {
		try {
			return IO.outputStream(outputFile);
		} catch (IOException e) {
			throw new TransformException("Failed to open output [ " + outputFile.getAbsolutePath() + " ]", e);
		}
	}

	private void closeOutputStream(File outputFile, OutputStream outputStream) throws TransformException {
		try {
			outputStream.close();
		} catch (IOException e) {
			throw new TransformException("Failed to close output [ " + outputFile.getAbsolutePath() + " ]", e);
		}
	}
}
