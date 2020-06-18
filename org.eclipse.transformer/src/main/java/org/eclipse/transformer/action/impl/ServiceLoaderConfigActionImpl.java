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

package org.eclipse.transformer.action.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;

import aQute.lib.io.ByteBufferInputStream;
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
public class ServiceLoaderConfigActionImpl extends ActionImpl {
	public static final String	META_INF			= "META-INF/";
	public static final String	META_INF_SERVICES	= "META-INF/services/";

	//

	public ServiceLoaderConfigActionImpl(Logger logger, boolean isTerse, boolean isVerbose, InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
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

	@Override
	public ServiceLoaderConfigChangesImpl getLastActiveChanges() {
		return (ServiceLoaderConfigChangesImpl) super.getLastActiveChanges();
	}

	@Override
	public ServiceLoaderConfigChangesImpl getActiveChanges() {
		return (ServiceLoaderConfigChangesImpl) super.getActiveChanges();
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
		return resourceName.contains(META_INF_SERVICES);
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws TransformException {

		String outputName = renameInput(inputName);
		if (outputName == null) {
			outputName = inputName;
		} else {
			verbose("Service name  [ {} ] -> [ {} ]", inputName, outputName);
		}
		setResourceNames(inputName, outputName);

		InputStream inputStream = new ByteBufferInputStream(inputBytes, 0, inputLength);
		InputStreamReader inputReader;
		try {
			inputReader = new InputStreamReader(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			error("Strange: UTF-8 is an unrecognized encoding for reading [ {} ]", e, inputName);
			return null;
		}

		BufferedReader reader = new BufferedReader(inputReader);

		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputLength);
		OutputStreamWriter outputWriter;
		try {
			outputWriter = new OutputStreamWriter(outputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			error("Strange: UTF-8 is an unrecognized encoding for writing [ {} ]", e, inputName);
			return null;
		}

		BufferedWriter writer = new BufferedWriter(outputWriter);

		try {
			transform(reader, writer); // throws IOException
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

	protected void transform(BufferedReader reader, BufferedWriter writer) throws IOException {

		String inputLine;
		while ((inputLine = reader.readLine()) != null) { // throws IOException
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

			writer.write(outputLine); // throws IOException
			writer.newLine(); // throws IOException
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
