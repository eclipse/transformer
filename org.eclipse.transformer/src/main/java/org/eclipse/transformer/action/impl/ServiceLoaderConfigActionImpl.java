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
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

import aQute.lib.io.ByteBufferOutputStream;

/**
 * Transform service configuration bytes. Per:
 * https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html A
 * service provider is identified by placing a provider-configuration file in
 * the resource directory META-INF/services. The file's name is the
 * fully-qualified binary name of the service's type. The file contains a list
 * of fully-qualified binary names of concrete provider classes, one per line.
 * Space and tab characters surrounding each name, as well as blank lines, are
 * ignored. The comment character is '#' ('\u0023', NUMBER SIGN); on each line
 * all characters following the first comment character are ignored. The file
 * must be encoded in UTF-8.
 */
public class ServiceLoaderConfigActionImpl extends ActionImpl<ServiceLoaderConfigChangesImpl> {
	public static final String	META_INF			= "META-INF/";
	public static final String	META_INF_SERVICES	= "META-INF/services/";

	//

	public ServiceLoaderConfigActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule,
		SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "Service Config Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.SERVICE_LOADER_CONFIG;
	}

	//

	@Override
	protected ServiceLoaderConfigChangesImpl newChanges() {
		return new ServiceLoaderConfigChangesImpl();
	}

	protected void addUnchangedProvider() {
		getActiveChanges().addUnchangedProvider();
	}

	protected void addChangedProvider() {
		getActiveChanges().addChangedProvider();
	}

	//

	@Override
	public String getAcceptExtension() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return resourceName.contains(META_INF_SERVICES) && !resourceName.endsWith(META_INF_SERVICES);
	}

	//

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		startRecording(inputData.name());
		try {
			String outputName = renameInput(inputData.name());
			if (outputName == null) {
				outputName = inputData.name();
			} else {
				getLogger().debug("Service name  [ {} ] -> [ {} ]", inputData.name(), outputName);
			}
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
			// Goal is to find the input package name. Find it by
			// successively taking text off of the input line.

			String inputPackageName;

			// The first '#' and all following characters are ignored.

			int poundLocation = inputLine.indexOf('#');
			if (poundLocation != -1) {
				inputPackageName = inputLine.substring(0, poundLocation);
			} else {
				inputPackageName = inputLine;
			}

			// Leading and trailing whitespace which surrounds the fully
			// qualified name is ignored. This step must be done after
			// trimming off a comment, since the trim must be of immediately
			// surrounding whitespace.

			inputPackageName = inputPackageName.trim();

			// Renames are performed on package names. Per the documentation,
			// the values are fully qualified class names.

			int dotLocation;
			String outputPackageName;

			if (inputPackageName.isEmpty()) {
				// The line was either entirely blank space, or was just
				// comment. There is no package to rename.
				dotLocation = -1;
				outputPackageName = null;

			} else {
				dotLocation = inputPackageName.lastIndexOf('.');
				if (dotLocation == -1) {
					// A class which uses the default package: There is no
					// package
					// to rename.
					outputPackageName = null;
				} else if (dotLocation == 0) {
					// Strange leading ".": Ignore it.
					outputPackageName = null;
				} else {
					// Nab just the fully qualified package name.
					inputPackageName = inputPackageName.substring(0, dotLocation);
					// And perform any renames which apply.
					outputPackageName = replacePackage(inputPackageName);
				}
			}

			String outputLine;

			if (outputPackageName == null) {
				// For one of the reasons, above, no rename was performed on the
				// line.
				outputLine = inputLine;
				addUnchangedProvider();

			} else {
				// Not most efficient, but good enough:
				// Service configuration files are expected to have only a few
				// values, and these are expected to use little or no white
				// space.

				// Figure where the input fully qualified package name began and
				// ended.

				int inputPackageStart = inputLine.indexOf(inputPackageName);
				int inputPackageEnd = inputPackageStart + dotLocation;

				// Recover as much of the original file as possible.

				outputLine = inputLine.substring(0, inputPackageStart) + outputPackageName
					+ inputLine.substring(inputPackageEnd);

				addChangedProvider();
			}

			writer.write(outputLine);
			writer.newLine();
		}
	}

	protected String renameInput(String inputName) {
		String inputPrefix;
		String serviceQualifiedName;

		int lastSlash = inputName.lastIndexOf('/');
		if (lastSlash == -1) {
			inputPrefix = null;
			serviceQualifiedName = inputName;
		} else {
			inputPrefix = inputName.substring(0, lastSlash + 1);
			serviceQualifiedName = inputName.substring(lastSlash + 1);
		}

		int classStart = serviceQualifiedName.lastIndexOf('.');
		if (classStart == -1) {
			return null;
		}

		String packageName = serviceQualifiedName.substring(0, classStart);
		if (packageName.isEmpty()) {
			return null;
		}

		// 'className' includes a leading '.'
		String className = serviceQualifiedName.substring(classStart);

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
