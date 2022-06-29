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
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.ElementAction;
import org.eclipse.transformer.action.RenameAction;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

/**
 * Top level ZIP action. A ZIP action is a container action which iterates
 * across the ZIP file elements, selecting and applying actions on each of these
 * elements and aggregating the changes.
 * <p>
 * ZIP actions come in several flavors -- EAR, Jar, RAR, and WAR. These are all
 * almost the same, with differences in the kinds of nested archives are
 * expected. Also, the Jar container action is unique in using the properties
 * action.
 * <p>
 * ZIP action is also an element action since a zip can be encountered in a container.
 * This is important for Bnd and Maven plugins.
 */
public class ZipActionImpl extends ContainerActionImpl implements ElementAction {

	public ZipActionImpl(ActionContext context, ActionType actionType) {
		super(context);
		this.actionType = actionType;
	}

	private final ActionType	actionType;

	@Override
	public ActionType getActionType() {
		return actionType;
	}

	@Override
	public boolean isArchiveAction() {
		return true;
	}

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return acceptExtension(resourceName, resourceFile);
	}

	// Entry from the transformer, or, from the directory action.

	@Override
	public void apply(String inputPath, File inputFile, String outputPath, File outputFile) throws TransformException {
		// Recording must be done as the first step: Otherwise, an apply failure
		// can leave the action without change data, which will cause errors
		// when attempting to record the action.

		startRecording(inputPath);
		try {
			setResourceNames(inputPath, outputPath);
			applyFile(inputPath, inputFile, outputPath, outputFile);
		} finally {
			stopRecording(inputPath);
		}
	}

	/**
	 * The method is for when ZipActionImpl is used as an ElementAction.
	 */
	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		String inputPath = inputData.name();
		startRecording(inputPath);
		try {
			String outputPath = relocateResource(inputPath);
			setResourceNames(inputPath, outputPath);
			InputStream inputStream = inputData.stream();
			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());
			applyStream(inputPath, inputStream, outputPath, outputStream);
			if (!isChanged()) {
				return inputData;
			}
			ByteBuffer outputBuffer = isContentChanged()
				? outputStream.toByteBuffer()
				: inputData.buffer();
			ByteData outputData = new ByteDataImpl(outputPath, outputBuffer, inputData.charset());
			return outputData;
		} finally {
			stopRecording(inputPath);
		}
	}

	// Nested entry from the zip action.

	private void apply(String inputPath, InputStream inputStream, String outputPath, OutputStream outputStream)
		throws TransformException {

		// Recording must be done as the first step: Otherwise, an apply failure
		// can leave the action without change data, which will cause errors
		// when attempting to record the action.

		startRecording(inputPath);
		try {
			setResourceNames(inputPath, outputPath);
			applyStream(inputPath, inputStream, outputPath, outputStream);
		} finally {
			stopRecording(inputPath);
		}
	}

	// Apply implementations, with recording already handled. These are split
	// from the entry methods to simplify the recording steps.

	private void applyFile(String inputPath, File inputFile, String outputPath, File outputFile)
		throws TransformException {
		File outputParent = outputFile.getParentFile();
		try {
			IO.mkdirs(outputParent);
		} catch (IOException e) {
			throw new TransformException("Failed to create directory [ " + outputParent.getAbsolutePath() + " ]", e);
		}

		try (InputStream inputStream = IO.stream(inputFile)) {
			try (OutputStream outputStream = IO.outputStream(outputFile)) {
				applyStream(inputPath, inputStream, outputPath, outputStream);
			} catch (IOException e) {
				throw new TransformException("Failed to write [ " + outputFile.getAbsolutePath() + " ]", e);
			}
		} catch (IOException e) {
			throw new TransformException("Failed to read [ " + inputFile.getAbsolutePath() + " ]", e);
		}
	}

	private void applyStream(String inputPath, InputStream inputStream, String outputPath, OutputStream outputStream)
		throws TransformException {

		// Use Zip streams instead of Jar streams. Jar streams automatically
		// read and consume the manifest, which we don't want.

		// Don't use try-with-resources: Zip streams, when closed, close their
		// base stream. We don't want that here.

		Charset charset = resourceCharset(inputPath);
		getLogger().debug("Zip Charset [ {} ]: {}", inputPath, charset);

		try {
			ZipInputStream zipInputStream = new ZipInputStream(inputStream, charset);

			ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, charset);
			try {
				applyZipStream(inputPath, zipInputStream, outputPath, zipOutputStream);
			} finally {
				zipOutputStream.finish();
			}

		} catch (IOException e) {
			throw new TransformException("Failed to complete output [ " + inputPath + " ]", e);
		}

	}

	protected boolean isDuplicate(
		String inputName, String inputPath, String outputName, String outputPath,
		Set<String> seen) {

		if (seen.add(outputName)) {
			return false;

		} else {
			if (inputName.equals(outputName)) {
				getLogger().error(
					"Duplicate entry: Entry [ {} ] of [ {} ] is a duplicate and cannot be written to [ {} ].  Ignoring.",
					inputName, inputPath, outputPath);
			} else {
				getLogger().error(
					"Duplicate entry: Entry [ {} ], initially [ {} ] of [ {} ], is a duplicate and cannot be written to [ {} ].  Ignoring.",
					outputName, inputName, inputPath, outputPath);
			}
			return true;
		}
	}

	/**
	 * Transform the entries of a zip-type archive. Write transformed entries to
	 * a zip-type archive.
	 *
	 * @param inputPath A name associated with the input stream.
	 * @param zipInputStream An input stream for the input archive.
	 * @param outputPath A name associated with the output stream.
	 * @param zipOutputStream An output stream for the output archive.
	 * @throws TransformException Thrown if reading or writing the archives
	 *             fails, or if transformation of an entry fails.
	 */
	private void applyZipStream(
		String inputPath, ZipInputStream zipInputStream,
		String outputPath, ZipOutputStream zipOutputStream) throws TransformException {

		String className = getClass().getSimpleName();
		String methodName = "apply";

		Logger useLogger = getLogger();

		useLogger.debug("[ {}.{} ] [ {} ]", className, methodName, inputPath);

		byte[] copyBuffer = new byte[FileUtils.BUFFER_ADJUSTMENT]; // Shared transfer buffer.

		// TODO: The replication of the 'isDuplicate' checks, below,
		//       indicates that the apply/record pattern is clumsy
		//       and should be replaced.
		//
		// See issue #304.

		Set<String> seen = new HashSet<>();

		String prevName = null;
		String inputName = null;

		try {
			for ( ZipEntry inputEntry;
				  (inputEntry = zipInputStream.getNextEntry()) != null;
				  prevName = inputName, inputName = null ) {

				try {
					inputName = FileUtils.sanitize(inputEntry.getName()); // Avoid ZipSlip
					int inputLength = Math.toIntExact(inputEntry.getSize());

					useLogger.debug("[ {}.{} ] Entry [ {} ] Size [ {} ]", className, methodName, inputName, inputLength);

					Action action = selectAction(inputName);

					// Duplicate checks must be done for each case
					// and must be done on the output name.
					//
					// Although unlikely, we could have:
					//   A renamed to B
					//   B renamed to A.
					// Or,
					//   A renamed to B
					//   B unselected

					// Putting entries into a zip stream requires that their name and
					// length details be determined before writing entry contents.
					//
					// That complicates the action API, since actions want to handle
					// naming the entry.
					//
					// As a compromise:
					//
					// * Un-accepted and un-selected entries are not renamed.
					//
					// * Zip entries are renamed as a step external to the zip entry apply
					//   processing, and which is performed before that processing.
					//
					// * Other entries are read entirely, transformed, then the entry is
					//   put and written.

					if (action == null) {
						if ( isDuplicate(inputName, inputPath, inputName, outputPath, seen) ) {
							recordDuplicate(action, inputName);
						} else {
							copy(inputEntry, zipInputStream, inputName, zipOutputStream, copyBuffer);
							recordUnaccepted(inputName);
						}
					} else if (!selectResource(inputName)) {
						// Unselected resources are *not* renamed.
						// The expectation is that files which are deliberately
						// omitted should not be transformed in any way.
						if ( isDuplicate(inputName, inputPath, inputName, outputPath, seen) ) {
							recordDuplicate(action, inputName);
						} else {
							copy(inputEntry, zipInputStream, inputName, zipOutputStream, copyBuffer);
							recordUnselected(inputName);
						}

					} else if (action.isRenameAction()) {
						RenameAction renameAction = (RenameAction) action;
						String outputName = renameAction.apply(inputName);
						outputName = FileUtils.sanitize(outputName);

						if ( isDuplicate(inputName, inputPath, outputName, outputPath, seen) ) {
							recordDuplicate(action, inputName);
						} else {
							copy(inputEntry, zipInputStream, outputName, zipOutputStream, copyBuffer);
							recordAction(action, inputName);
						}

					} else if (action.isArchiveAction()) {
						ZipActionImpl zipAction = (ZipActionImpl) action;
						String outputName = zipAction.relocateResource(inputName);
						outputName = FileUtils.sanitize(outputName);

						if ( isDuplicate(inputName, inputPath, outputName, outputPath, seen) ) {
							recordDuplicate(zipAction, inputName);
						} else {
							try {
								if (inputEntry.getMethod() == ZipEntry.STORED) {
									// For STORED, we must know the size of the result
									// before creating the ZipEntry. So we cannot stream.
									ByteData inputData = collect(inputName, zipInputStream, inputLength);
									ByteData outputData = zipAction.apply(inputData);
									ZipEntry outputEntry = createEntry(inputEntry, outputName, outputData);
									putEntry(zipOutputStream, outputEntry, () -> {
										outputData.writeTo(zipOutputStream);
									});
								} else {
									// For COMPRESSED, we use streaming.
									// Loading entire archives into memory is to be avoided.
									// However, that means the new entry name must be known
									// before invoking 'apply' on the selected action.
									ZipEntry outputEntry = createEntry(inputEntry, outputName);
									String putInputName = inputName; // Need these to be effectively final
									String putOutputName = outputName;
									putEntry(zipOutputStream, outputEntry, () -> {
										// Note the use of 'apply' and not the internal 'applyStream'.
										// Recording must be performed.  And, the streams must be put through
										// conversion to zip streams as a part of handling nested archives.
										zipAction.apply(putInputName, zipInputStream, putOutputName, zipOutputStream);
									});
								}

								recordAction(zipAction, inputName);
							} catch (Throwable th) {
								recordError(zipAction, inputName, th);
							}
						}

					} else if ( !action.isElementAction() ) {
						useLogger.warn("Strange: Unknown action type [ {} ] for [ {} ] in {} ]",
							action.getClass().getName(), inputName, inputPath);

						if ( isDuplicate(inputName, inputPath, inputName, outputPath, seen) ) {
							recordDuplicate(action, inputName);
						} else {
							copy(inputEntry, zipInputStream, inputName, zipOutputStream, copyBuffer);
							recordUnaccepted(inputName);
						}

					} else {
						ElementAction elementAction = (ElementAction) action;

						// Collect up front, then allow the action to run, which includes
						// both renaming and content transformation, then put and write the
						// entry.

						ByteData inputData = collect(inputName, zipInputStream, inputLength);
						boolean beganWrite = false;
						try {
							ByteData outputData = elementAction.apply(inputData);
							String outputName = outputData.name();
							outputName = FileUtils.sanitize(outputName); // Avoid ZipSlip

							if ( isDuplicate(inputName, inputPath, outputName, outputPath, seen) ) {
								recordDuplicate(elementAction, inputName);
							} else {
								beganWrite = true;
								if ( elementAction.getLastActiveChanges().isContentChanged() ) {
									writeModified(inputEntry, inputData, outputData, outputName, zipOutputStream);
								} else {
									writeUnmodified(inputEntry, inputData, outputName, zipOutputStream);
								}
								recordAction(elementAction, inputName);
							}

						} catch (Throwable t) {
							if ( !beganWrite ) {
								writeUnmodified(inputEntry, inputData, inputName, zipOutputStream);
							} else {
								useLogger.error("Write failure of [ {} ] of [ {} ]", inputName, inputPath);
							}
							recordError(action, inputName, t);
						}
					}
				} catch (Throwable t) {
					useLogger.error("Transform failure [ {} ] of [ {} ]", inputName, inputPath, t);
				}
			}

		} catch (IOException e) {
			String message;
			if (inputName != null) {
				// Actively processing an entry.
				message = "Failure while processing [ " + inputName + " ] from [ " + inputPath + " ]";
			} else if (prevName != null) {
				// Moving to a new entry but not the first entry.
				message = "Failure after processing [ " + prevName + " ] from [ " + inputPath + " ]";
			} else {
				// Moving to the first entry.
				message = "Failed to process first entry of [ " + inputPath + " ]";
			}
			throw new TransformException(message, e);
		}
	}

	private void copy(
		ZipEntry inputEntry, ZipInputStream zipInputStream,
        String outputName, ZipOutputStream zipOutputStream,
        byte[] buffer) throws IOException {

		getLogger().trace("Copy entry [ {} ] Directory [ {} ] as [ {} ]",
			inputEntry.getName(), inputEntry.isDirectory(), outputName);
		ZipEntry outputEntry = copyEntry(inputEntry, outputName);
		putEntry(zipOutputStream, outputEntry, () -> {
			if ( !inputEntry.isDirectory() ) {
				long bytesWritten = FileUtils.transfer(zipInputStream, zipOutputStream, buffer);
				getLogger().trace("Copied [ {} ] bytes to [ {} ]", bytesWritten, outputName);
			}
		});
	}

	public void writeUnmodified(
		ZipEntry inputEntry,
		ByteData outputData, String outputName, ZipOutputStream zipOutputStream)
		throws IOException {

		getLogger().trace("Write unmodified entry [ {} ] bytes [ {} ]", outputName, outputData.length());

		ZipEntry outputEntry = copyEntry(inputEntry, outputName);
		putEntry(zipOutputStream, outputEntry, () -> {
			outputData.writeTo(zipOutputStream);
		});
	}

	public void writeModified(
		ZipEntry inputEntry, ByteData inputData,
		ByteData outputData, String outputName, ZipOutputStream zipOutputStream)
		throws IOException {

		getLogger().trace("Write modified entry [ {} ] bytes [ {} ]", outputName, outputData.length());

		ZipEntry outputEntry = createEntry(inputEntry, outputName, outputData);
		putEntry(zipOutputStream, outputEntry, () -> {
			outputData.writeTo(zipOutputStream);
		});
	}

	private ZipEntry createEntry(ZipEntry inputEntry, String outputName) {
		// Unless already set, putNextEntry sets the last modified time.
		// of the entry.

		ZipEntry outputEntry = new ZipEntry(outputName);

		int method = inputEntry.getMethod();
		if (method != -1) {
			outputEntry.setMethod(method);
		}

		String inputComment = inputEntry.getComment();
		if ( inputComment != null ) {
			outputEntry.setComment(inputComment);
		}
		byte[] inputExtra = inputEntry.getExtra();
		if ( inputExtra != null ) {
			outputEntry.setExtra(inputExtra);
		}

		return outputEntry;
	}

	private ZipEntry copyEntry(ZipEntry inputEntry, String outputName) {
		ZipEntry outputEntry = createEntry(inputEntry, outputName);

		if (outputEntry.getMethod() == ZipEntry.STORED) {
			long size = inputEntry.getSize();
			if ( size >= 0L ) {
				// A size of '-1' is possible from the input entry.
				// Setting -1 causes an exception.
				outputEntry.setSize(size);
			}
			long cSize = inputEntry.getCompressedSize();
			if ( cSize >= 0L ) {
				// This is not checked, but let's be safe anyway.
				outputEntry.setCompressedSize(cSize);
			}
			long crc = inputEntry.getCrc();
			if ( crc >= 0L ) {
				// A CRC of '-1' is possible from the input entry.
				// Setting -1 causes an exception.
				outputEntry.setCrc(crc);
			}
		}

		// TODO:
		//
		// Not sure about whether these time values should be set.
		// Does changing the entry name invalidate the prior values?
		// Also, setting these may override any updates made when
		// setting the extra data.
		//
		// See issue #305.

		outputEntry.setTime( inputEntry.getTime() );

		FileTime lastTime = inputEntry.getLastAccessTime();
		if ( lastTime != null ) {
			outputEntry.setLastAccessTime(lastTime);
		}
		FileTime createTime = inputEntry.getCreationTime();
		if ( createTime != null ) {
			outputEntry.setCreationTime(createTime);
		}
		FileTime modTime = inputEntry.getLastModifiedTime();
		if ( modTime != null ) {
			outputEntry.setLastModifiedTime(modTime);
		}

		return outputEntry;
	}

	private ZipEntry createEntry(ZipEntry inputEntry, String outputName, ByteData outputData) {
		ZipEntry outputEntry = createEntry(inputEntry, outputName);

		if (outputEntry.getMethod() == ZipEntry.STORED) {
			int length = outputData.length();
			outputEntry.setSize(length);
			outputEntry.setCompressedSize(length);

			CRC32 crc = new CRC32();
			crc.update( outputData.buffer() );
			outputEntry.setCrc( crc.getValue() );
		}

		return outputEntry;
	}

	@FunctionalInterface
	private interface TransformerRunnable {
		void run() throws IOException, TransformException;
	}

	private void putEntry(ZipOutputStream zipOutputStream, ZipEntry outputEntry, TransformerRunnable populator) throws IOException, TransformException {
		zipOutputStream.putNextEntry(outputEntry); // throws IOException
		try {
			populator.run(); // throws TransformException
		} finally {
			zipOutputStream.closeEntry(); // throws IOException
		}
	}
}
