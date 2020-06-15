/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.action;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.util.InputStreamData;

public interface Action {
	/**
	 * Answer a short name for this action. This must be unique among the
	 * collection of actions in use, and will be used to record change
	 * information.
	 *
	 * @return A unique short name for this action.
	 */
	String getName();

	//

	/**
	 * Answer the type of this action.
	 *
	 * @return The type of this action.
	 */
	ActionType getActionType();

	/**
	 * Tell if a resource is of a type which is handled by this action.
	 *
	 * @param resourceName The name of the resource.
	 * @return True or false telling if the resource can be handled by this
	 *         action.
	 */
	boolean accept(String resourceName);

	/**
	 * Tell if a resource is of a type which is handled by this action.
	 *
	 * @param resourceName The name of the resource.
	 * @param resourceFile The file of the resource. This can be null.
	 * @return True or false telling if the resource can be handled by this
	 *         action.
	 */
	boolean accept(String resourceName, File resourceFile);

	//

	/**
	 * Tell if this action uses streams. This is necessary for handing resources
	 * off to composite actions which are run as container actions (except for
	 * directories). To stream a child resource forces a write of the resource
	 * name. But, for certain actions, the output resource name is not known
	 * until after the transform is performed. Leaf actions are expected to use
	 * byte arrays, while nested archives must use streams.
	 *
	 * @return True or false telling if the action processes streams.
	 */
	boolean useStreams();

	//

	/**
	 * Tell if a resource is to be transformed.
	 *
	 * @param resourceName The name of the resource.
	 * @return True or false telling if the resource is to be transformed.
	 */
	boolean select(String resourceName);

	//

	/**
	 * Answer the rules widget for signature type updates.
	 *
	 * @return The rules widget for signature type updates.
	 */
	SignatureRule getSignatureRule();

	//

	/**
	 * Apply this action onto an input file, writing output onto an output file.
	 *
	 * @param inputName A name associated with the input file.
	 * @param inputFile The input file.
	 * @param outputFile The output file.
	 * @throws TransformException Thrown if the action could not be applied.
	 */
	void apply(String inputName, File inputFile, File outputFile) throws TransformException;

	/**
	 * Apply this action on an input stream. Answer a data structure containing
	 * output data. The output data will be the original data if the input
	 * stream if this action declined to process the input data.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @return Transformed input data.
	 * @throws TransformException Thrown if the transform failed.
	 */
	InputStreamData apply(String inputName, InputStream inputStream) throws TransformException;

	/**
	 * Apply this action on an input stream. Answer a data structure containing
	 * output data. The output data will be the original data if the input
	 * stream if this action declined to process the input data. The input count
	 * may be {@link InputStreamData#UNKNOWN_LENGTH}, in which case all
	 * available data will be read from the input stream.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @param inputCount The count of bytes available in the input stream.
	 * @return Transformed input data.
	 * @throws TransformException Thrown if the transform failed.
	 */
	InputStreamData apply(String inputName, InputStream inputStream, int inputCount) throws TransformException;

	/**
	 * Apply this action on an input stream. Write transformed data to the
	 * output stream. The input count may be
	 * {@link InputStreamData#UNKNOWN_LENGTH}, in which case all available data
	 * will be read from the input stream.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @param inputCount The count of bytes available in the input stream.
	 * @param outputStream A stream to which to write transformed data.
	 * @throws TransformException Thrown if the transform failed.
	 */
	void apply(String inputName, InputStream inputStream, long inputCount, OutputStream outputStream)
		throws TransformException;

	//

	/**
	 * Answer the last active changes.
	 *
	 * @return The last active changes.
	 */
	Changes getLastActiveChanges();

	/**
	 * Answer the current active changes.
	 *
	 * @return The current active changes.
	 */
	Changes getActiveChanges();

	/**
	 * Record a single replacement to the changes of this action.
	 */
	void addReplacement();

	/**
	 * Add to the count of replacements made by this action.
	 *
	 * @param additions The count of replacements to add.
	 */
	void addReplacements(int additions);

	//

	/**
	 * Tell if the current application of this action had changes.
	 *
	 * @return True or false telling if the current application of this action
	 *         had changes.
	 */
	boolean hasChanges();

	/**
	 * Tell if the current application of this action had changes other than a
	 * resource name change.
	 *
	 * @return True or false telling if the current application of this action
	 *         had changes other than resource name changes.
	 */
	boolean hasNonResourceNameChanges();

	/**
	 * Tell if the current application of this action changed the name of the
	 * resource.
	 *
	 * @return True or false telling if the current application of this action
	 *         changed the name of the resource.
	 */
	boolean hasResourceNameChange();

	//

	/**
	 * Tell if the last application of this action had changes.
	 *
	 * @return True or false telling if the last application of this action had
	 *         changes.
	 */
	boolean hadChanges();

	/**
	 * Tell if the last application of this action had changes other than a
	 * resource name change.
	 *
	 * @return True or false telling if the last application of this action had
	 *         changes other than resource name changes.
	 */
	boolean hadNonResourceNameChanges();

	/**
	 * Tell if the last application of this action changed the name of the
	 * resource.
	 *
	 * @return True or false telling if the last application of this action
	 *         changed the name of the resource.
	 */
	boolean hadResourceNameChange();

	//

	InputBufferImpl getBuffer();

	byte[] getInputBuffer();

	void setInputBuffer(byte[] inputBuffer);
}
