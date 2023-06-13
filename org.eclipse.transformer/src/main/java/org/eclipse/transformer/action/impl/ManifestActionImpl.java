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

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.unmodifiable.Sets;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.manifest.ManifestUtil;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.ByteData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Action for manifest, including feature manifest.
 * <p>
 * Performs package, package version, and bundle identity updates on select
 * manifest attributes.
 * <p>
 * Feature manifest updates are the same as simple manifest updates, with two
 * differences: First, feature manifest generally will have the extension ".mf"
 * but will not have the name "MANIFEST". Second, feature manifest do not have
 * the line length restrictions as are had by manifest.
 */
public class ManifestActionImpl extends ElementActionImpl {

	public ManifestActionImpl(ActionContext context, ActionType actionType) {
		super(context);
		this.actionType = actionType;
	}

	//

	private final ActionType actionType;

	@Override
	public ActionType getActionType() {
		return actionType;
	}
	//


	private boolean isManifest() {
		return getActionType() == ActionType.MANIFEST;
	}

	//

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		startRecording(inputData);
		try {
			String className = getClass().getSimpleName();
			String methodName = "apply";

			getLogger().debug("[ {}.{} ]: Initial [ {} ]", className, methodName, inputData);

			setResourceNames(inputData.name(), inputData.name());

			Manifest initialManifest;
			try {
				initialManifest = new Manifest(inputData.stream());
			} catch (IOException e) {
				throw new TransformException("Failed to parse manifest [ " + inputData.name() + " ]", e);
			}

			Manifest finalManifest = new Manifest();

			transform(inputData.name(), initialManifest, finalManifest);

			if (!isChanged()) {
				getLogger().debug("[ {}.{} ]: [ {} ] Null transform", className, methodName, inputData.name());
				return inputData;
			}

			Charset charset = inputData.charset();
			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());
			try {
				write(finalManifest, outputStream);
			} catch (IOException e) {
				throw new TransformException("Failed to write manifest [ " + inputData.name() + " ]", e);
			}

			ByteData outputData = new ByteDataImpl(inputData.name(), outputStream.toByteBuffer(), charset);
			getLogger().debug("[ {}.{} ]: Final [ {} ]", className, methodName, outputData);
			return outputData;
		} finally {
			stopRecording(inputData);
		}
	}

	protected void transform(String inputName, Manifest initialManifest, Manifest finalManifest) {
		Attributes initialMainAttributes = initialManifest.getMainAttributes();
		Attributes finalMainAttributes = finalManifest.getMainAttributes();

		addReplacements(transformPackages(inputName, "main", initialMainAttributes, finalMainAttributes));

		if (transformBundleIdentity(inputName, initialMainAttributes, finalMainAttributes)) {
			addReplacement();
		}

		Map<String, Attributes> initialEntries = initialManifest.getEntries();
		Map<String, Attributes> finalEntries = finalManifest.getEntries();

		for (Map.Entry<String, Attributes> entry : initialEntries.entrySet()) {
			String entryKey = entry.getKey();
			Attributes initialEntryAttributes = entry.getValue();

			Attributes finalAttributes = new Attributes(initialEntryAttributes.size());
			finalEntries.put(entryKey, finalAttributes);

			addReplacements(transformPackages(inputName, entryKey, initialEntryAttributes, finalAttributes));
		}
	}

	private static final Set<String> SELECT_ATTRIBUTES = Sets.of("DynamicImport-Package", "Import-Package",
		"Export-Package", "Subsystem-Content", "IBM-API-Package", "Provide-Capability", "Require-Capability");

	public static Set<String> getSelectedAttributes() {
		return SELECT_ATTRIBUTES;
	}

	public static boolean selectAttribute(String name) {
		return SELECT_ATTRIBUTES.contains(name);
	}

	protected int transformPackages(String inputName, String entryName, Attributes initialAttributes,
		Attributes finalAttributes) {

		getLogger().trace("Transforming [ {} ]: [ {} ] Attributes [ {} ]", inputName, entryName,
			initialAttributes.size());

		int replacements = 0;

		for (Map.Entry<Object, Object> entries : initialAttributes.entrySet()) {
			Object untypedName = entries.getKey();
			String typedName = untypedName.toString();

			String initialValue = (String) entries.getValue();
			String finalValue = null;

			if (selectAttribute(typedName)) {
				finalValue = getSignatureRule().replaceAttributePackages(typedName, initialValue);
			}

			if (finalValue == null) {
				finalValue = initialValue;
			} else {
				replacements++;
			}

			finalAttributes.put(untypedName, finalValue);
		}

		getLogger().trace("Transformed [ {} ]: [ {} ] Attributes [ {} ] Replacements [ {} ]", inputName, entryName,
			finalAttributes.size(), replacements);

		return replacements;
	}

	protected void write(Manifest manifest, OutputStream outputStream) throws IOException {
		if (isManifest()) {
			writeAsManifest(manifest, outputStream);
		} else {
			writeAsFeature(manifest, outputStream);
		}
	}

	protected void writeAsManifest(Manifest manifest, OutputStream outputStream) throws IOException {
		// manifest.write(outputStream);
		ManifestUtil.write(manifest, outputStream);
	}

	// Copied and updated from:
	// https://github.com/OpenLiberty/open-liberty/blob/integration/
	// dev/wlp-featureTasks/src/com/ibm/ws/wlp/feature/tasks/FeatureBuilder.java

	@SuppressWarnings("unused")
	protected void writeAsFeature(Manifest manifest, OutputStream outputStream) throws IOException {
		PrintWriter writer = new PrintWriter(outputStream);

		StringBuilder builder = new StringBuilder();

		for (Map.Entry<Object, Object> mainEntry : manifest.getMainAttributes().entrySet()) {
			writer.append(mainEntry.getKey().toString());
			writer.append(": ");

			String value = (String) mainEntry.getValue();
			if (value.indexOf(',') == -1) {
				writer.append(value);

			} else {
				Parameters parms = OSGiHeader.parseHeader(value);

				boolean continuedLine = false;
				for (Map.Entry<String, Attrs> parmEntry : parms.entrySet()) {
					if (continuedLine) {
						writer.append(",\r ");
					}

					// bnd might have added ~ characters if there are duplicates
					// in
					// the source, so we should remove them before we output it
					// so we
					// get back to the original intended content.

					String parmName = parmEntry.getKey();
					int index = parmName.indexOf('~');
					if (index != -1) {
						parmName = parmName.substring(0, index);
					}
					writer.append(parmName);

					Attrs parmAttrs = parmEntry.getValue();
					for (Map.Entry<String, String> parmAttrEntry : parmAttrs.entrySet()) {
						String parmAttrName = parmAttrEntry.getKey();
						String parmAttrValue = quote(builder, parmAttrEntry.getValue());

						writer.append("; ");
						writer.append(parmAttrName);
						writer.append('=');
						writer.append(parmAttrValue);
					}

					continuedLine = true;
				}
			}

			writer.append("\r");
		}

		writer.flush();
	}

	public String quote(StringBuilder sb, String value) {
		@SuppressWarnings("unused")
		boolean isClean = OSGiHeader.quote(sb, value);
		String quotedValue = sb.toString();
		sb.setLength(0);
		return quotedValue;
	}

	//

	public static final String	SYMBOLIC_NAME_PROPERTY_NAME	= "Bundle-SymbolicName";
	public static final String	VERSION_PROPERTY_NAME		= "Bundle-Version";
	public static final String	NAME_PROPERTY_NAME			= "Bundle-Name";
	public static final String	DESCRIPTION_PROPERTY_NAME	= "Bundle-Description";

	// Bundle case:
	// Bundle updates:
	//
	// Updated:
	//
	// Bundle-Description: WAS WebContainer 8.1 with Servlet 4.0 support
	// Bundle-Name: WAS WebContainer
	// Bundle-SymbolicName: com.ibm.ws.webcontainer.servlet.4.0
	// Bundle-Version: 1.0.36.cl200120200108-0300
	//
	// Ignored:
	//
	// Bundle-Copyright: Copyright (c) 1999, 2019 IBM Corporation and others.
	// All rights reserved. This program and the accompanying materials are
	// made available under the terms of the Eclipse Public License v1.0 wh
	// ich accompanies this distribution, and is available at http://www.ecl
	// ipse.org/legal/epl-v10.html.
	// Bundle-License: Eclipse Public License; url=https://www.eclipse.org/le
	// gal/epl-v10.html
	// Bundle-ManifestVersion: 2
	// Bundle-SCM: connection=scm:git:https://github.com/OpenLiberty/open-lib
	// erty.git, developerConnection=scm:git:https://github.com:OpenLiberty/
	// open-liberty.git, url=https://github.com/OpenLiberty/open-liberty/tre
	// e/master
	// Bundle-Vendor: IBM

	// Subsystem case:
	//
	// Subsystem-Description: %description
	// Subsystem-License: https://www.eclipse.org/legal/epl-v10.html
	// Subsystem-Localization: OSGI-INF/l10n/com.ibm.websphere.appserver.jsp-2.3
	// Subsystem-ManifestVersion: 1
	// Subsystem-Name: JavaServer Pages 2.3
	// Subsystem-SymbolicName: com.ibm.websphere.appserver.jsp-2.3;
	// visibility:=public; singleton:=true
	// Subsystem-Type: osgi.subsystem.feature
	// Subsystem-Vendor: IBM Corp.
	// Subsystem-Version: 1.0.0

	public boolean transformBundleIdentity(String inputName, Attributes initialMainAttributes,
		Attributes finalMainAttributes) {

		String initialSymbolicName = initialMainAttributes.getValue(SYMBOLIC_NAME_PROPERTY_NAME);
		if (initialSymbolicName == null) {
			getLogger().trace("Input [ {} ] has no bundle symbolic name", inputName);
			return false;
		}

		int indexOfSemiColon = initialSymbolicName.indexOf(';');
		String symbolicNamesAttributes = null;
		if (indexOfSemiColon != -1) {
			symbolicNamesAttributes = initialSymbolicName.substring(indexOfSemiColon);
			initialSymbolicName = initialSymbolicName.substring(0, indexOfSemiColon);
		}

		String matchCase;
		boolean matched;
		boolean isWildcard;
		BundleData bundleUpdate = getBundleUpdate(initialSymbolicName);
		if (bundleUpdate == null) {
			bundleUpdate = getBundleUpdate("*");
			if (bundleUpdate != null) {
				matched = true;
				isWildcard = true;
				matchCase = "a wildcard identity update";
			} else {
				matched = false;
				isWildcard = false;
				matchCase = "no identity update";
			}
		} else {
			matched = true;
			isWildcard = false;
			matchCase = "identity update";
		}
		getLogger().trace("Input [ {} ] symbolic name [ {} ] has {}", inputName, initialSymbolicName, matchCase);
		if (!matched) {
			return false;
		}

		@SuppressWarnings("null")
		String finalSymbolicName = bundleUpdate.getSymbolicName();

		if (isWildcard) {
			int wildcardOffset = finalSymbolicName.indexOf('*');
			if (wildcardOffset != -1) {
				finalSymbolicName = finalSymbolicName.substring(0, wildcardOffset) + initialSymbolicName
					+ finalSymbolicName.substring(wildcardOffset + 1);
			}
		}

		if (symbolicNamesAttributes != null) {
			finalSymbolicName += symbolicNamesAttributes;
		}

		finalMainAttributes.putValue(SYMBOLIC_NAME_PROPERTY_NAME, finalSymbolicName);
		getLogger().debug("Bundle symbolic name: {} --> {}", initialSymbolicName, finalSymbolicName);

		if (!isWildcard) {
			String initialVersion = initialMainAttributes.getValue(VERSION_PROPERTY_NAME);
			if (initialVersion != null) {
				String finalVersion = bundleUpdate.getVersion();
				if ((finalVersion != null) && !finalVersion.isEmpty()) {
					finalMainAttributes.putValue(VERSION_PROPERTY_NAME, finalVersion);
					getLogger().debug("Bundle version: {} --> {}", initialVersion, finalVersion);
				}
			}
		}

		String initialName = initialMainAttributes.getValue(NAME_PROPERTY_NAME);
		if (initialName != null) {
			String finalName = bundleUpdate.updateName(initialName);
			if ((finalName != null) && !finalName.isEmpty()) {
				finalMainAttributes.putValue(NAME_PROPERTY_NAME, finalName);
				getLogger().debug("Bundle name: {} --> {}", initialName, finalName);
			}
		}

		String initialDescription = initialMainAttributes.getValue(DESCRIPTION_PROPERTY_NAME);
		if (initialDescription != null) {
			String finalDescription = bundleUpdate.updateDescription(initialDescription);
			if ((finalDescription != null) && !finalDescription.isEmpty()) {
				finalMainAttributes.putValue(DESCRIPTION_PROPERTY_NAME, finalDescription);
				getLogger().debug("Bundle description: {} --> {}", initialDescription, finalDescription);
			}
		}

		return true;
	}
}
