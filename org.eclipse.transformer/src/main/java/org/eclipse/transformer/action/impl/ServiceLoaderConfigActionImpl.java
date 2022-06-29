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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import aQute.lib.io.ByteBufferOutputStream;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.LineSeparatorBufferedReader;

/**
 * Transform service configuration bytes. Per:
 * <a href="https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html">...</a> A
 * service provider is identified by placing a provider-configuration file in
 * the resource directory META-INF/services. The file's name is the
 * fully-qualified binary name of the service's type. The file contains a list
 * of fully-qualified binary names of concrete provider classes, one per line.
 * Space and tab characters surrounding each name, as well as blank lines, are
 * ignored. The comment character is '#' ('\u0023', NUMBER SIGN); on each line
 * all characters following the first comment character are ignored. The file
 * must be encoded in UTF-8.
 */
public class ServiceLoaderConfigActionImpl extends ElementActionImpl {
	public static final String	META_INF_SERVICES	= "META-INF/services/";

	//

	public ServiceLoaderConfigActionImpl(ActionContext context) {
		super(context);
	}

	//

	@Override
	public ActionType getActionType() {
		return ActionType.SERVICE_LOADER_CONFIG;
	}

	//

	@Override
	public ServiceLoaderConfigChangesImpl newChanges() {
		return new ServiceLoaderConfigChangesImpl();
	}

	@Override
	public ServiceLoaderConfigChangesImpl getActiveChanges() {
		return (ServiceLoaderConfigChangesImpl) super.getActiveChanges();
	}

	@Override
	public ServiceLoaderConfigChangesImpl getLastActiveChanges() {
		return (ServiceLoaderConfigChangesImpl) super.getLastActiveChanges();
	}

	protected void addUnchangedProvider() {
		getActiveChanges().addUnchangedProvider();
	}

	protected void addChangedProvider() {
		getActiveChanges().addChangedProvider();
	}

	//

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return resourceName.contains(META_INF_SERVICES) && !resourceName.endsWith(META_INF_SERVICES);
	}

	//

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		startRecording(inputData);
		try {
			String inputName = inputData.name();
			String outputName = packageRenameInput(inputName);
			if (outputName == null) {
				outputName = inputName;
			} else {
				getLogger().debug("Service name  [ {} ] -> [ {} ]", inputName, outputName);
			}
			setResourceNames(inputName, outputName);

			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());

			Charset charset = inputData.charset();
			try (LineSeparatorBufferedReader reader = new LineSeparatorBufferedReader(inputData.reader()); BufferedWriter writer = FileUtils.writer(outputStream, charset)) {
				transform(reader, writer);
			} catch (IOException e) {
				throw new TransformException("Failed to transform [ " + inputName + " ]", e);
			}

			if (!isChanged()) {
				return inputData;
			} else if (!isContentChanged()) {
				return inputData.copy(outputName);
			} else {
				return new ByteDataImpl(outputName, outputStream.toByteBuffer(), charset);
			}

		} finally {
			stopRecording(inputData);
		}
	}

	protected void transform(LineSeparatorBufferedReader reader, BufferedWriter writer) throws IOException {
		for (String inputLine; (inputLine = reader.readLine()) != null; writer.write(reader.lineSeparator())) {
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
					// package to rename.
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
		}
	}
}
