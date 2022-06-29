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

package org.eclipse.transformer.action;

import java.io.File;
import java.nio.charset.Charset;

import org.eclipse.transformer.TransformException;

public interface Action {
	/**
	 * Answer a short name for this action. This must be unique among the
	 * collection of actions in use, and will be used to record change
	 * information.
	 *
	 * @return A unique short name for this action.
	 */
	default String getName() {
		return getActionType().getName();
	}

	//

	/**
	 * Answer the type of this action.
	 *
	 * @return The type of this action.
	 */
	ActionType getActionType();

	/**
	 * Tell if this is an element type action.
	 *
	 * @return True or false telling if this is an element type action.
	 */
	default boolean isElementAction() {
		return false;
	}

	/**
	 * Tell if this is an container type action.
	 *
	 * @return True or false telling if this is an container type action.
	 */
	default boolean isContainerAction() {
		return false;
	}

	/**
	 * Tell if this is a rename type action.
	 *
	 * @return True or false telling if this is a rename type action.
	 */
	default boolean isRenameAction() {
		return false;
	}

	/**
	 * Tell if this is an archive type action.
	 *
	 * @return True or false telling if this is an archive type action.
	 */
	default boolean isArchiveAction() {
		return false;
	}

	/**
	 * Tell if a resource is of a type which is handled by this action.
	 *
	 * @param resourceName The name of the resource.
	 * @return True or false telling if the resource can be handled by this
	 *         action.
	 */
	default boolean acceptResource(String resourceName) {
		return acceptResource(resourceName, null);
	}

	/**
	 * Tell if a resource is of a type which is handled by this action.
	 *
	 * @param resourceName The name of the resource.
	 * @param resourceFile The file of the resource. This can be null.
	 * @return True or false telling if the resource can be handled by this
	 *         action.
	 */
	boolean acceptResource(String resourceName, File resourceFile);

	/**
	 * Tell if the action matches the specified action type name.
	 * <p>
	 * See {@link ActionType#matches(String)}.
	 *
	 * @param actionTypeName An action type name.
	 * @return True or false telling if the action's type matches the specified
	 *         name.
	 */
	default boolean acceptType(String actionTypeName) {
		return getActionType().matches(actionTypeName);
	}

	//

	/**
	 * Tell if a resource is to be transformed.
	 *
	 * @param resourceName The name of the resource.
	 * @return True or false telling if the resource is to be transformed.
	 */
	default boolean selectResource(String resourceName) {
		return getResourceSelectionRule().select(resourceName);
	}

	/**
	 * Return the charset for the specified resource name.
	 *
	 * @param resourceName The name of the resource.
	 * @return The charset to use when reading or writing.
	 */
	default Charset resourceCharset(String resourceName) {
		return getResourceSelectionRule().charset(resourceName);
	}

	//

	/**
	 * Answer the rules widget for signature type updates.
	 *
	 * @return The rules widget for signature type updates.
	 */
	SignatureRule getSignatureRule();

	/**
	 * Answer the rules widget for selection.
	 *
	 * @return The rules widget for selection.
	 */
	SelectionRule getResourceSelectionRule();

	//

	/**
	 * Apply this action to an input file. Write the transformed content to the
	 * specified output file. Ignore any renames.
	 * <p>
	 * This is the usual entry point for top level invocations.
	 *
	 * @param inputName The name of the input file.
	 * @param inputFile The input file.
	 * @param outputName The name of the output file.
	 * @param outputFile The output file.
	 * @throws TransformException Thrown if the transform failed.
	 */
	void apply(String inputName, File inputFile, String outputName, File outputFile) throws TransformException;

	//

	/**
	 * Start recording transformation of a resource.
	 *
	 * @param inputName The name of the resource.
	 */
	void startRecording(String inputName);

	/**
	 * Adjust an input path according to the changes made to the name of the
	 * resource stored at that path. Special prefixes "WEB-INF/classes/" and
	 * "META-INF/versions/n/" are handled.
	 *
	 * @param inputPath The initial path to the resource.
	 * @return An output path for the resource.
	 */
	default String relocateResource(String inputPath) {
		return getSignatureRule().relocateResource(inputPath);
	}

	/**
	 * Stop recording transformation of a resource.
	 *
	 * @param inputName The name of the resource.
	 */
	void stopRecording(String inputName);

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
	 * Record the input and output resource names to the active changes.
	 *
	 * @param inputResourceName The input resource name.
	 * @param outputResourceName The output resource name.
	 */
	default void setResourceNames(String inputResourceName, String outputResourceName) {
		getActiveChanges().setInputResourceName(inputResourceName)
			.setOutputResourceName(outputResourceName);
	}

	//

	/**
	 * Tell if the current application of this action had changes.
	 *
	 * @return True or false telling if the current application of this action
	 *         had changes.
	 */
	default boolean isChanged() {
		return getActiveChanges().isChanged();
	}

	/**
	 * Tell if the current application of this action had changes other than a
	 * resource name change.
	 *
	 * @return True or false telling if the current application of this action
	 *         had changes other than resource name changes.
	 */
	default boolean isContentChanged() {
		return getActiveChanges().isContentChanged();
	}

	/**
	 * Tell if the current application of this action changed the name of the
	 * resource.
	 *
	 * @return True or false telling if the current application of this action
	 *         changed the name of the resource.
	 */
	default boolean isRenamed() {
		return getActiveChanges().isRenamed();
	}

	//

}
