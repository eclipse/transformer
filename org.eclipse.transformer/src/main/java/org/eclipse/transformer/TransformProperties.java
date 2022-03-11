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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.BundleDataImpl;

public class TransformProperties {
	/** Character used to define a package rename. */
	public static final char	PACKAGE_RENAME_ASSIGNMENT	= '=';

	/** Prefix character for resources which are to be excluded. */
	public static final char	RESOURCE_EXCLUSION_PREFIX	= '!';

	/** Used to demark head and tail regions in resource selections. */
	public static final char	RESOURCE_WILDCARD			= '*';

	//

	public static void addSelections(Set<String> included, Set<String> excluded, Properties selections) {
		for (Map.Entry<Object, Object> selectionEntry : selections.entrySet()) {
			String selection = (String) selectionEntry.getKey();
			addSelection(included, excluded, selection);
		}
	}

	public static void addSelection(Set<String> included, Set<String> excluded, String selection) {
		if (selection.charAt(0) == RESOURCE_EXCLUSION_PREFIX) {
			excluded.add(selection.substring(1));
		} else {
			included.add(selection);
		}
	}

	public static void processSelections(Set<String> selections, Set<String> selectionsExact,
		Set<String> selectionsHead, Set<String> selectionsTail, Set<String> selectionsAny) {

		for (String selection : selections) {
			selection = selection.trim();

			int selectionLength = selection.length();
			if (selectionLength == 0) {
				continue;
			}

			boolean matchHead = (selection.charAt(0) == RESOURCE_WILDCARD);

			// A single '*' matches everything. Matching everything is encoded
			// as an empty selections collections.

			if (selectionLength == 1) {
				selections.clear();
				selectionsExact.clear();
				selectionsHead.clear();
				selectionsTail.clear();
				selectionsAny.clear();
				return;
			}

			boolean matchTail = (selection.charAt(selectionLength - 1) == RESOURCE_WILDCARD);

			if (matchHead) {
				if (matchTail) {
					selectionsAny.add(selection.substring(1, selectionLength - 1));
				} else {
					selectionsHead.add(selection.substring(1));
				}
			} else if (matchTail) {
				selectionsTail.add(selection.substring(0, selectionLength - 1));
			} else {
				selectionsExact.add(selection);
			}
		}
	}

	public static Map<String, String> getPackageRenames(Properties renameProperties) {
		Map<String, String> packageRenames = new HashMap<>(renameProperties.size());
		for (Map.Entry<Object, Object> renameEntry : renameProperties.entrySet()) {
			packageRenames.put((String) renameEntry.getKey(), (String) renameEntry.getValue());
		}
		return packageRenames;
	}

	public static Map<String, String> getDirectStrings(Properties directProperties) {
		Map<String, String> directStrings = new HashMap<>(directProperties.size());
		for (Map.Entry<Object, Object> directEntry : directProperties.entrySet()) {
			directStrings.put((String) directEntry.getKey(), (String) directEntry.getValue());
		}
		return directStrings;
	}

	public static Map<String, String> invert(Map<String, String> properties) {
		Map<String, String> inverseProperties = new HashMap<>(properties.size());
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			inverseProperties.put(entry.getValue(), entry.getKey());
		}
		return inverseProperties;
	}

	public static void setPackageVersions(
		Properties versionProperties,
		Map<String, String> packageVersions,
		Map<String, Map<String, String>> specificPackageVersions) {

		for ( Map.Entry<Object, Object> versionEntry : versionProperties.entrySet() ) {
			setPackageVersions(
				(String) versionEntry.getKey(), (String) versionEntry.getValue(),
				packageVersions, specificPackageVersions);
		}
	}

	// packageName=version
	//
	// packageName=version,pName=version,...
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
				if ( versionBuilder.length() != 0 ) {
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
					if ( nameBuilder.length() != 0 ) {
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

		if ( versionBuilder.length() != 0 ) {
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
		if ( versionBuilder.length() == 0 ) {
			throw new IllegalArgumentException("Package version syntax error: No version in [ " + newVersion + " ]");
		} else {
			versionText = versionBuilder.toString();
			versionBuilder.setLength(0);
		}

		String propertyName;
		if ( nameBuilder.length() == 0 ) {
			propertyName = null;
		} else {
			propertyName = nameBuilder.toString();
			nameBuilder.setLength(0);
		}

		if ( propertyName == null ) {
			packageVersions.put(newPackageName, versionText);
		} else {
			Map<String, String> versionsForProperty = specificPackageVersions.get(propertyName);
			if ( versionsForProperty == null ) {
				specificPackageVersions.put( propertyName, (versionsForProperty = new HashMap<>()) );
			}
			versionsForProperty.put(newPackageName, versionText);
		}
	}

	public static Map<String, BundleData> getBundleUpdates(Properties updateProperties) {
		Map<String, BundleData> bundleUpdates = new HashMap<>(updateProperties.size());
		for (Map.Entry<Object, Object> updateEntry : updateProperties.entrySet()) {
			bundleUpdates.put((String) updateEntry.getKey(), new BundleDataImpl((String) updateEntry.getValue()));
		}
		return bundleUpdates;
	}

	public static Map<String, String> convertPropertiesToMap(Properties properties) {
		Map<String, String> map = new HashMap<>(properties.size());
		for (Map.Entry<Object, Object> fileEntry : properties.entrySet()) {
			map.put((String) fileEntry.getKey(), (String) fileEntry.getValue());
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
	@SuppressWarnings("resource")
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
