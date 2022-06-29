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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import aQute.lib.io.IO;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.action.SignatureRule.SignatureType;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

/**
 * <em>Root action implementation.</em>
 * <p>
 * Action selection operations are delegated to a {@link SelectionRule}
 * instance.
 * <p>
 * Update operations are delegated to a {@link SignatureRule} instance. See
 * {@link SignatureRule#transformBinaryType(String)},
 * {@link SignatureRule#transformSignature(String, SignatureType)}, and
 * {@link SignatureRule#transformDescriptor(String)}.
 * <p>
 * <em>Transformation state</em>
 * <p>
 * As actions form a directed graph, where each action is responsible for
 * transforming a single resource, and where edges represent a transition to a
 * nested resource (see {@link ActionSelectorImpl} and
 * {@link ContainerActionImpl}), action instances are often reused when
 * transforming a hierarchy of resources. For example, a ZIP archive may contain
 * a nested ZIP archive, in which case the transformation of the outer zip will
 * use the Zip action at least twice.
 * <p>
 * The consequence is that actions must carefully manage state as relates to the
 * resource which is being actively transformed.
 * <p>
 * First, data -- as relates to resource selection and as relates to the
 * particular substitutions which are to be performed -- is static and is shared
 * by actions using shared {@link SelectionRule} and {@link SignatureRule}
 * instances. (This data is referred to as <em>transformation rules data</em>.)
 * <p>
 * Second, resource state data consists of the active resource and the
 * accumulated change record. The active resource is passed as method
 * parameters. The accumulated change record is managed within the actions as a
 * stack, with each 'apply' being required to begin and end with
 * {@link #startRecording(String)} and {@link #stopRecording(String)}.
 * <p>
 * Third, there is data which is not transformation rules data and which is is
 * not related to the active resource or to accumulated Changes. This additional
 * data consists of cached transformation values for binary types, binary
 * descriptors, and binary signatures, and consists of a thread-local read
 * buffer. The read buffer is managed as a thread-local value to avoid
 * unnecessary reallocations of the buffer while transforming many resources.
 */
public abstract class ActionImpl implements Action {
	public ActionImpl(ActionContext context) {
		this.context = requireNonNull(context);

		// Change tracking ...

		this.changes = new ArrayDeque<>();
		this.activeChanges = null;
		this.lastActiveChanges = null;
	}

	//

	private final ActionContext context;

	protected ActionContext getContext() {
		return context;
	}

	protected Logger getLogger() {
		return getContext().getLogger();
	}

	//

	@Override
	public SelectionRule getResourceSelectionRule() {
		return getContext().getSelectionRule();
	}

	//

	@Override
	public SignatureRule getSignatureRule() {
		return getContext().getSignatureRule();
	}

	public Map<String, String> getPackageRenames() {
		return getSignatureRule().getPackageRenames();
	}

	public String replacePackage(String initialName) {
		return getSignatureRule().replacePackage(initialName);
	}

	public String replaceBinaryPackage(String initialName) {
		return getSignatureRule().replaceBinaryPackage(initialName);
	}

	public String replacePackages(String initialText) {
		return getSignatureRule().replacePackages(initialText);
	}

	public String replaceBinaryPackages(String initialText) {
		return getSignatureRule().replaceBinaryPackages(initialText);
	}

	public String packageRenameInput(String inputName) {
		return getSignatureRule().packageRenameInput(inputName);
	}

	public String replacePackageVersion(String attributeName, String packageName, String oldVersion) {
		return getSignatureRule().replacePackageVersion(attributeName, packageName, oldVersion);
	}

	public BundleData getBundleUpdate(String symbolicName) {
		return getSignatureRule().getBundleUpdate(symbolicName);
	}

	public Map<String, String> getTextSubstitutions(String inputName) {
		return getSignatureRule().getTextSubstitutions(inputName);
	}

	public String replaceText(String inputFileName, String initialText) {
		return getSignatureRule().replaceText(inputFileName, initialText);
	}

	public String replaceTextDirectPerClass(String initialValue, String inputName) {
		return getSignatureRule().replaceTextDirectPerClass(initialValue, inputName);
	}

	public String replaceTextDirectGlobal(String initialValue, String inputName) {
		return getSignatureRule().replaceTextDirectGlobal(initialValue, inputName);
	}

	public String transformBinaryType(String inputConstant) {
		return getSignatureRule().transformBinaryType(inputConstant);
	}

	public String transformDescriptor(String inputConstant) {
		return getSignatureRule().transformDescriptor(inputConstant);
	}

	public String transformSignature(String initialSignature, SignatureType signatureType) {
		return getSignatureRule().transformSignature(initialSignature, signatureType);
	}

	//

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support this method");
	}

	protected boolean acceptExtension(String resourceName, File resourceFile) {
		String ext = getAcceptExtension();
		int extLen = ext.length();
		int resLen = resourceName.length();

		boolean accept = ((resLen >= extLen) && resourceName.regionMatches(true, resLen - extLen, ext, 0, extLen));
		return accept;
	}

	public String getAcceptExtension() {
		String acceptExtension = getActionType().getExtension();
		if (acceptExtension.isEmpty()) {
			throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support this method");
		}
		return acceptExtension;
	}

	//

	protected abstract Changes newChanges();

	private final Deque<Changes>	changes;
	private Changes					activeChanges;
	private Changes					lastActiveChanges;

	protected void startRecording(ByteData inputData) {
		startRecording(inputData.name());
	}

	protected void stopRecording(ByteData inputData) {
		stopRecording(inputData.name());
	}

	@Override
	public void startRecording(String inputName) {
		getLogger().debug("Start processing [ {} ] using [ {} ]", inputName, getName());

		Changes useActiveChanges = activeChanges;
		if (useActiveChanges != null) {
			changes.addLast(useActiveChanges);
		}
		activeChanges = newChanges();
	}

	@Override
	public void stopRecording(String inputName) {
		Changes useActiveChanges = activeChanges;
		Logger useLogger = getLogger();
		if (useLogger.isDebugEnabled()) {
			useLogger.debug("Stop processing [ {} ] using [ {} ]: {}", inputName, getName(),
				useActiveChanges.getChangeText());
		}
		lastActiveChanges = useActiveChanges;
		activeChanges = changes.pollLast();
	}

	//

	@Override
	public Changes getActiveChanges() {
		return activeChanges;
	}

	//

	@Override
	public Changes getLastActiveChanges() {
		return lastActiveChanges;
	}

	//

	/**
	 * Collect the data for an action on an input stream. Returns a data
	 * structure containing input data upon which the action can be performed.
	 * If the specified count is greater than or equal to zero, read that many
	 * bytes. Otherwise, read all available bytes.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @param inputCount The count of bytes which are to be read. If less than
	 *            zero, all available bytes are read.
	 * @return The read data.
	 * @throws TransformException Thrown if the input data cannot be read.
	 */
	public ByteData collect(String inputName, InputStream inputStream, int inputCount)
		throws TransformException {

		// (BJH, TFB):
		//
		// Very large entries are specifically allowed to be read.
		//
		// Failing with an out-of-memory error is acceptable.
		//
		// This is a good case for a category of files and entries which should
		// be omitted.

		Charset charset = resourceCharset(inputName);
		try {
			return FileUtils.read(getLogger(), inputName, charset, inputStream, inputCount);
		} catch (IOException e) {
			throw new TransformException("Failed to read [ " + inputName + " ] count [ " + inputCount + " ]", e);
		}
	}

	/**
	 * Read all available bytes from an input stream.
	 *
	 * @param inputName A name associated with the input stream.
	 * @param inputStream A stream which is to be read.
	 * @return The read data.
	 * @throws TransformException Thrown if the stream cannot be read.
	 */
	public ByteData collect(String inputName, InputStream inputStream) throws TransformException {
		return collect(inputName, inputStream, -1);
	}

	/**
	 * Read all bytes from a file.
	 *
	 * @param inputPath A name associated with the file.
	 * @param inputFile A file which is to be read.
	 * @return The read data.
	 * @throws TransformException Thrown if the file cannot be read.
	 */
	public ByteData collect(String inputPath, File inputFile) throws TransformException {
		try (InputStream inputStream = IO.stream(inputFile)) {
			return collect(inputPath, inputStream, Math.toIntExact(inputFile.length()));
		} catch (IOException e) {
			throw new TransformException("Failed to read input [ " + inputFile.getAbsolutePath() + " ]", e);
		}
	}

	/**
	 * Write data to an output stream. Convert any exception thrown when
	 * attempting to write into a {@link TransformException}.
	 *
	 * @param outputData Data to be written.
	 * @param outputStream Stream to which to write the data.
	 * @throws TransformException Thrown in case of a write failure.
	 */
	protected void write(ByteData outputData, OutputStream outputStream) throws TransformException {
		try {
			outputData.writeTo(outputStream);
		} catch (IOException e) {
			throw new TransformException("Failed to write [ " + outputData + " ]");
		}
	}

	/**
	 * Write data to a file.
	 *
	 * @param outputData Data which is to be written. This data structure
	 *            provides the path from the output folder to the output file.
	 *            See {@link ByteData#name()}.
	 * @param outputFolder The folder into which to write the data. This might
	 *            not be the immediate parent of the file which is written.
	 * @throws TransformException Thrown if the file is not writable, or if an
	 *             error occurs during the write.
	 */
	protected void writeInto(ByteData outputData, File outputFolder) throws TransformException {
		writeInto(outputData, outputFolder, outputData.name());
	}

	/**
	 * Write data to a file.
	 *
	 * @param outputData Data which is to be written. Ignore the name in this
	 *            data structure.
	 * @param outputPath The relative path of the file which is to be written.
	 * @param outputFolder The folder into which to write the data. This might
	 *            not be the immediate parent of the file which is written.
	 * @throws TransformException Thrown if the file is not writable, or if an
	 *             error occurs during the write.
	 */
	protected void writeInto(ByteData outputData, File outputFolder, String outputPath) throws TransformException {
		File outputFile;
		try {
			outputFile = IO.getBasedFile(outputFolder, outputPath);
		} catch (IOException e) {
			throw new TransformException(
				"Non-valid file [ " + outputFolder.getAbsolutePath() + " ] [ " + outputPath + " ]", e);
		}

		write(outputData, outputFile);
	}

	protected void write(ByteData outputData, File outputFile) {
		File parentFile = outputFile.getParentFile();
		try {
			IO.mkdirs(parentFile);
		} catch (IOException e) {
			throw new TransformException(
				"Failed to create parent directory of [ " + outputFile.getAbsolutePath() + " ]", e);
		}

		try (OutputStream outputStream = IO.outputStream(outputFile)) {
			write(outputData, outputStream);
		} catch (IOException e) {
			throw new TransformException("Failed to write [ " + outputFile.getAbsolutePath() + " ]", e);
		}
	}

	/**
	 * Copy a file to a folder. Use the input file relative path as the output
	 * file relative path.
	 *
	 * @param inputPath The relative path of the input file.
	 * @param inputFile The file which is to be copied.
	 * @param outputFolder The folder into which to copy the file.
	 * @throws TransformException Thrown if the copy fails.
	 */
	protected void copyInto(String inputPath, File inputFile, File outputFolder) throws TransformException {
		copyInto(inputPath, inputFile, outputFolder, inputPath);
	}

	/**
	 * Copy a file to a folder.
	 *
	 * @param inputPath The relative path of the input file.
	 * @param inputFile The file which is to be copied.
	 * @param outputFolder The folder into which to copy the file.
	 * @param outputPath The relative path of the output file.
	 * @throws TransformException Thrown if the copy fails.
	 */
	protected void copyInto(String inputPath, File inputFile, File outputFolder, String outputPath)
		throws TransformException {
		File outputFile;
		try {
			outputFile = IO.getBasedFile(outputFolder, outputPath);
		} catch (IOException e) {
			throw new TransformException(
				"Non-valid file [ " + outputFolder.getAbsolutePath() + " ] [ " + outputPath + " ]", e);
		}

		File parentFile = outputFile.getParentFile();
		try {
			IO.mkdirs(parentFile);
		} catch (IOException e) {
			throw new TransformException(
				"Failed to create parent directory of [ " + outputFile.getAbsolutePath() + " ]", e);
		}

		try {
			IO.copy(inputFile, outputFile);
		} catch (IOException e) {
			throw new TransformException(
				"Failed to copy [ " + inputFile.getAbsolutePath() + " ] to [ " + outputFile.getAbsolutePath() + " ]",
				e);
		}
	}

	//

	@FunctionalInterface
	public interface StringReplacement {
		String apply(String inputName, String initialValue, List<String> cases);
	}

	public static final List<StringReplacement> NO_ACTIVE_REPLACEMENTS = Collections.emptyList();

	protected List<StringReplacement> getActiveReplacements() {
		return NO_ACTIVE_REPLACEMENTS;
	}

	/**
	 * Control API: Subclasses should override to control whether they want to
	 * continue applying updates, or stop after the first update which had a
	 * non-trivial or null effect. This is being used currently by class
	 * actions, which, before the active replacements implementation, stopped
	 * after the first successful update.
	 *
	 * @return True or false telling if multiple updates are allowed.
	 */
	protected boolean allowMultipleReplacements() {
		return true;
	}

	// ClassActionImpl.transformString(String, String, String)
	// TextActionImpl.transformString(String, String, String)

	protected String updateString(String inputName, String valueCase, String initialValue) {
		List<StringReplacement> useReplacements = getActiveReplacements();
		if ((useReplacements == null) || useReplacements.isEmpty()) {
			getLogger().trace("    String {} {}: {} (no-active replacements, unchanged)", inputName, valueCase,
				initialValue);
			return null;
		} else {
			return updateString(inputName, valueCase, initialValue, useReplacements);
		}
	}

	protected String updateString(
		String inputName, String valueCase, String initialValue,
		List<StringReplacement> replacements) {

		Logger useLogger = getLogger();

		if ((initialValue == null) || initialValue.isEmpty()) {
			useLogger.trace("    String {} {}: {} (empty, unchanged)", inputName, valueCase, initialValue);
			return null;
		}

		boolean allowMultiple = allowMultipleReplacements();

		List<String> cases = new ArrayList<>(allowMultiple ? replacements.size() : 1);

		String finalValue = initialValue;
		for ( StringReplacement replacement : replacements ) {
			String priorValue = finalValue;
			finalValue = replacement.apply(inputName, priorValue, cases);
			if ( finalValue == null ) {
				finalValue = priorValue;
			} else {
				useLogger.trace("Input [ {} ] [ {} ] Initial [ {} ] Final [ {} ] ( {} )",
					  inputName, valueCase, priorValue, finalValue, cases);
				if (!allowMultiple) {
					break;
				}
			}
		}

		if ( finalValue == initialValue ) {
			useLogger.trace("    String {} {}: {} (unchanged)", inputName, valueCase, initialValue);
			return null;
		} else {
			useLogger.trace("    String {} {}: {} -> {} ({})", inputName, valueCase, initialValue, finalValue, cases);
			return finalValue;
		}
	}

	//

	protected String packagesUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().replacePackages(initialValue);
		if (finalValue != null) {
			cases.add("packages");
		}
		return finalValue;
	}

	protected String binaryPackagesUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().replaceBinaryPackages(initialValue);
		if (finalValue != null) {
			cases.add("binary packages");
		}
		return finalValue;
	}

	protected String textUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().replaceText(inputName, initialValue);
		if (finalValue != null) {
			cases.add("text");
		}
		return finalValue;
	}

	protected String directPerClassUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().replaceTextDirectPerClass(initialValue, inputName);
		if (finalValue != null) {
			cases.add("direct per class");
		}
		return finalValue;
	}

	/**
	 * Modified direct class lookup for Java resources. Change the Java resource
	 * extension to ".class" then proceed with a usual ".class" text update.
	 *
	 * @param inputName The Java resource name. Extension ".java" is expected.
	 * @param initialValue The initial value which is to be transformed.
	 * @param cases Record of what substitution cases were triggered.
	 * @return The modified value. Null if no updates were made.
	 */
	protected String directPerClassUpdate_java(String inputName, String initialValue, List<String> cases) {
		String lookupName = switchExtensionTo(inputName, ".java", ".class");
		if (lookupName == null) {
			return null;
		}
		String finalValue = getSignatureRule().replaceTextDirectPerClass(initialValue, lookupName);
		if (finalValue != null) {
			cases.add("direct per class (java)");
		}
		return finalValue;
	}

	/**
	 * Switch the extension of an input resource. Remove "WEB-INF/classes" from
	 * the input name.
	 * <p>
	 * This modification is necessary to lookup class direct updates for the
	 * purpose of updating Java resources, which do not have the extension used
	 * by the direct class updates table.
	 *
	 * @param inputName The initial input name.
	 * @param initialExt The initial extension.
	 * @param finalExtension The final extension.
	 * @return The modified input name. Null if the input does not start with
	 *         the specified extension.
	 */
	private String switchExtensionTo(String inputName, String initialExt, String finalExtension) {
		if (!inputName.toLowerCase()
			.endsWith(initialExt)) {
			getLogger().error("Input [ {} ] does not have expected extension [ {} ]", inputName, initialExt);
			return null;
		}

		String head = inputName.substring(0, inputName.length() - initialExt.length());
		return head + finalExtension;
	}

	protected String directGlobalUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().replaceTextDirectGlobal(initialValue, inputName);
		if (finalValue != null) {
			cases.add("direct global");
		}
		return finalValue;
	}

	protected String binaryTypeUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().transformBinaryType(initialValue);
		if (finalValue != null) {
			cases.add("binary type");
		}
		return finalValue;
	}

	protected String descriptorUpdate(String inputName, String initialValue, List<String> cases) {
		String finalValue = getSignatureRule().transformDescriptor(initialValue);
		if (finalValue != null) {
			cases.add("binary descriptor");
		}
		return finalValue;
	}
}
