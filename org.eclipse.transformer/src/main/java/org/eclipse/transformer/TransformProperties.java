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

package org.eclipse.transformer;

import static org.eclipse.transformer.util.SignatureUtils.containsWildcard;
import static org.eclipse.transformer.util.SignatureUtils.stripWildcard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.BundleDataImpl;

public class TransformProperties {
	/** Character used to define a package rename. */
	public static final char	PACKAGE_RENAME_ASSIGNMENT	= '=';

	/** Charset value for resources which are to be excluded.
	 * We cannot use '!' as a key prefix since lines that start
	 * with '!' are comments in a properties file.
	 */
	public static final String	RESOURCE_EXCLUSION	= "!";

	//

	public static void addSelections(Map<String, String> included, Map<String, String> excluded, Map<String, String> selections) {
		for (Map.Entry<String, String> selectionEntry : selections.entrySet()) {
			String selection = selectionEntry.getKey();
			String charset = selectionEntry.getValue();
			addSelection(included, excluded, selection, charset);
		}
	}

	public static void addSelection(Map<String, String> included, Map<String, String> excluded, String selection, String charset) {
		selection = selection.trim();
		charset = charset.trim();
		if (charset.startsWith(RESOURCE_EXCLUSION)) {
			excluded.put(selection, charset.substring(1));
		} else {
			included.put(selection, charset);
		}
	}

	public static Map<String, String> invert(Map<String, String> properties) {
		Map<String, String> inverseProperties = new HashMap<>(properties.size());
		properties.forEach((key, value) -> {
			if (containsWildcard(key)) {
				value = value.concat(key.substring(key.length() - 2));
				key = stripWildcard(key);
			}
			inverseProperties.put(value, key);
		});
		return inverseProperties;
	}

	public static void setPackageVersions(
		Map<?, ?> versionProperties,
		Map<String, String> packageVersions,
		Map<String, Map<String, String>> specificPackageVersions) {

		for (Map.Entry<?, ?> versionEntry : versionProperties.entrySet()) {
			setPackageVersions(
				(String) versionEntry.getKey(), (String) versionEntry.getValue(),
				packageVersions, specificPackageVersions);
		}
	}

	// packageName=version
	//
	// packageName=version;pName=version;...
	//
	// Use '\' to escape ';' and '=' special characters:
	//
	// Note that this collides with property file character escaping:
	// When writing property files, the escape character must be doubled.
	//
	// Within the property file:
	//
	// packageName=vBegin\\;vEnd;pname=Version;...
	// packageName=vBegin\\;vMiddle\\=vEnd;pName=version;...
	//
	// After reading the property file:
	//
	// packageName=vBegin\;vEnd;pname=Version;...
	// packageName=vBegin\;vMiddle\=vEnd;pName=version;...

	public static void setPackageVersions(
		String newPackageName, String newVersion,
		Map<String, String> packageVersions,
		Map<String, Map<String, String>> specificPackageVersions) {

		// Simple case: Neither special character is present.
		// No parsing is needed.  However, escapes must be removed.
		//
		// The case of a new version having only escaped special
		// characters are not handled by this check.

		if ( (newVersion.indexOf('=') == -1) && (newVersion.indexOf(';') == -1) ) {
			newVersion = newVersion.replace("\\", "");
			setVersion(newPackageName, newVersion, packageVersions);
			return;
		}

		// Case with specific version assignments.

		int length = newVersion.length();

		boolean escaped = false;

		StringBuilder nameBuilder = new StringBuilder();
		StringBuilder versionBuilder = new StringBuilder();

		for ( int offset = 0; offset < length; offset++ ) {
			char nextChar = newVersion.charAt(offset);

			if ( escaped ) {
				escaped = false;
				versionBuilder.append(nextChar);

			} else if ( nextChar == '\\' ) {
				escaped = true; // ... which consumes the escape character

			} else if ( nextChar == '=' ) {
				if ( versionBuilder == nameBuilder ) {
					throw new IllegalArgumentException("Package version syntax error: Too many '=' in [ " + newVersion + " ]");
				} else {
					// Accumulate first expecting a version.
					// When an '=' is encountered, the accumulation is known
					// to be an attribute name, not a version.
					StringBuilder currentBuilder = nameBuilder;
					nameBuilder = versionBuilder;
					versionBuilder = currentBuilder;
				}

			} else if ( nextChar == ';' ) {
				// The length test guards against 'p=v;a1=v1;;a2=v2'
				if (!versionBuilder.isEmpty()) {
					setVersion(
						newPackageName, newVersion,
						nameBuilder, versionBuilder,
						packageVersions, specificPackageVersions);
					// Both builders will be empty.  They don't need to be unswizzled.
				} else {
					// Have to be careful:
					// 'p=v;a1=;a2=v2' will leave the version builder
					// while leaving the name builder non-empty.
					//
					// TODO: We might want to skip an update in a specific
					//       case.  A blank/empty update might be used to
					//       encode this.
					//
					// See issue #300.

					if (!nameBuilder.isEmpty()) {
						throw new IllegalArgumentException("Package version syntax error: Version missing for package [ " + newPackageName + " ] and attribute [ " + nameBuilder.toString() + " ]");
					}
				}

			} else {
				versionBuilder.append(nextChar);
			}
		}

		// If the version builder is not empty, all new version
		// characters were processed.  There will usually be
		// characters left in the accumulators, which need to
		// be added to the tables.
		//
		// The length check is to make sure the new version did not
		// end with a ';', in which case the builders will be empty,
		// and there is no update to be done.

		if (!versionBuilder.isEmpty()) {
			setVersion(
				newPackageName, newVersion,
				nameBuilder, versionBuilder,
				packageVersions, specificPackageVersions);
		}
	}

	public static String setVersion(
		String newPackageName, String newVersion,
		Map<String, String> packageVersions) {

		return packageVersions.put(newPackageName, newVersion);
	}

	public static void setVersion(
		String newPackageName, String newVersion,
		StringBuilder nameBuilder, StringBuilder versionBuilder,
		Map<String, String> packageVersions,
		Map<String, Map<String, String>> specificPackageVersions) {

		String versionText;
		if (versionBuilder.isEmpty()) {
			throw new IllegalArgumentException("Package version syntax error: No version in [ " + newVersion + " ]");
		} else {
			versionText = versionBuilder.toString();
			versionBuilder.setLength(0);
		}

		String propertyName;
		if (nameBuilder.isEmpty()) {
			propertyName = null;
		} else {
			propertyName = nameBuilder.toString();
			nameBuilder.setLength(0);
		}

		if ( propertyName == null ) {
			packageVersions.put(newPackageName, versionText);
		} else {
			Map<String, String> versionsForProperty = specificPackageVersions.computeIfAbsent(propertyName, k -> new HashMap<>());
			versionsForProperty.put(newPackageName, versionText);
		}
	}

	public static Map<String, BundleData> getBundleUpdates(Map<?, ?> updateProperties) {
		Map<String, BundleData> bundleUpdates = new HashMap<>(updateProperties.size());
		for (Map.Entry<?, ?> updateEntry : updateProperties.entrySet()) {
			bundleUpdates.put((String) updateEntry.getKey(), new BundleDataImpl((String) updateEntry.getValue()));
		}
		return bundleUpdates;
	}

	public static Map<String, String> convertPropertiesToMap(Properties properties) {
		return copyPropertiesToMap(properties, new HashMap<>(properties.size()));
	}

	public static <MAP extends Map<String, String>> MAP copyPropertiesToMap(Properties properties, MAP map) {
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			map.put((String) entry.getKey(), (String) entry.getValue());
		}
		return map;
	}

	//

	/**
	 * Tell if a manifest file is formatted properly as a manifest file.
	 * Properly formatted manifest files must have no lines longer than 72
	 * characters wide. This test is required to distinguish feature-manifests,
	 * which have a manifest-like format but which do not obey the line length
	 * restriction.
	 *
	 * @param manifestPath The path to a file to be tested.
	 * @param manifestFile The manifest file.
	 * @return True or false telling if the file is a feature-manifest type
	 *         file.
	 * @throws TransformException Thrown if an error occurs processing the file.
	 */
	public static boolean isFeatureManifest(String manifestPath, File manifestFile) throws TransformException {
		FileReader manifestReader;
		try {
			manifestReader = new FileReader(manifestFile);
		} catch (IOException e) {
			throw new TransformException("Failed to open [ " + manifestPath + " ]", e);
		}

		try {
			BufferedReader bufferedReader = new BufferedReader(manifestReader);

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.length() > 72) {
					return true;
				}
			}
			return false;

		} catch (IOException e) {
			throw new TransformException("Failed to read  [ " + manifestPath + " ]", e);

		} finally {
			try {
				manifestReader.close();
			} catch (IOException e) {
				throw new TransformException("Failed to close  [ " + manifestPath + " ]", e);
			}
		}
	}
}
