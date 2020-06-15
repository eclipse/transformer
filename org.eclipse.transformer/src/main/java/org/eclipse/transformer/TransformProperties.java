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

package org.eclipse.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.BundleDataImpl;

import aQute.lib.utf8properties.UTF8Properties;

public class TransformProperties {
	/** Character used to define a package rename. */
	public static final char	PACKAGE_RENAME_ASSIGNMENT	= '=';

	/** Prefix character for resources which are to be excluded. */
	public static final char	RESOURCE_EXCLUSION_PREFIX	= '!';

	/** Used to demark head and tail regions in resource selections. */
	public static final char	RESOURCE_WILDCARD			= '*';

	//

	public static void setSelections(Set<String> included, Set<String> excluded, UTF8Properties selections) {

		for (Map.Entry<Object, Object> selectionEntry : selections.entrySet()) {
			String selection = (String) selectionEntry.getKey();
			if (selection.charAt(0) == RESOURCE_EXCLUSION_PREFIX) {
				excluded.add(selection.substring(1));
			} else {
				included.add(selection);
			}
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

	public static Map<String, String> getPackageRenames(UTF8Properties renameProperties) {
		Map<String, String> packageRenames = new HashMap<>(renameProperties.size());
		for (Map.Entry<Object, Object> renameEntry : renameProperties.entrySet()) {
			packageRenames.put((String) renameEntry.getKey(), (String) renameEntry.getValue());
		}
		return packageRenames;
	}

	public static Map<String, String> getDirectStrings(UTF8Properties directProperties) {
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

	public static Map<String, String> getPackageVersions(UTF8Properties versionProperties) {
		Map<String, String> packageVersions = new HashMap<>(versionProperties.size());
		for (Map.Entry<Object, Object> versionEntry : versionProperties.entrySet()) {
			packageVersions.put((String) versionEntry.getKey(), (String) versionEntry.getValue());
		}
		return packageVersions;
	}

	public static Map<String, BundleData> getBundleUpdates(UTF8Properties updateProperties) {
		Map<String, BundleData> bundleUpdates = new HashMap<>(updateProperties.size());
		for (Map.Entry<Object, Object> updateEntry : updateProperties.entrySet()) {
			bundleUpdates.put((String) updateEntry.getKey(), new BundleDataImpl((String) updateEntry.getValue()));
		}
		return bundleUpdates;
	}

	public static Map<String, String> convertPropertiesToMap(UTF8Properties properties) {
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
