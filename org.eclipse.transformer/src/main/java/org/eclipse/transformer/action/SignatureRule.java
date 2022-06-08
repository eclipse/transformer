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

import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.transformer.util.SignatureUtils;

/**
 * Java text substitution API.
 * <p>
 * <em>Category 1: Package Renaming:</em>
 * <p>
 * The package renaming API includes single name replacement versus multiple
 * name replacement, and includes "dot" format name replacement versus "slash"
 * format name replacement. This results in four main API entry points.
 * <p>
 * Package matching must locate the beginnings of embedded packages. That
 * ensures that accidental matches do not occur. For example, "xjava.servlet"
 * should not be a match for "java.servlet". See
 * {@link SignatureUtils#packageMatch(String, int, int, boolean)}.
 * <p>
 * Package renames may be specified with trailing wildcards. Usually, package
 * matching is of entire package values. That is, "javax.servlet" does not match
 * "javax.servlet.util". (There are cases of transformations which are performed
 * on super-packages but not on sub-packages.) An initial package name which is
 * specified with a trailing wildcard enables matching of subpackages. See
 * {@link SignatureUtils#containsWildcard(String)}.
 * <p>
 * Package updates in both the dot format (.) and the slash format (/) are
 * applied to Java and JSP resources.
 * <p>
 * <em>Category 2: Package Version Updates</em>
 * <p>
 * Package version updates are defined in two varieties: "Global" updates and
 * "specific" updates. "Global" updates are made in the absence of a "specific"
 * override. "Specific" updates are specific to particular manifest attributes,
 * and override any global update made while updating a particular manifest
 * attribute value.
 * <p>
 * <em>Category 4: Text Updates</em>
 * <p>
 * Text updates consist of property-value replacements. The main variations have
 * to do matching replacements to resources. A replacement may be specified to
 * be performed on a specific file, or may be specified to be performed on files
 * which match a specific pattern.
 * <p>
 * The process of performing text updates looks for the first matching updates,
 * then applies those updates. Only one set of updates is applied.
 * <p>
 * <em>Category 5: Direct Updates</em>
 * <p>
 * Direct updates are primarily updates to binary string constants in class
 * resources. Direct updates are applied to Java and JSP resources.
 * <p>
 * Direct updates may be specified to apply to all class resources ("global"
 * direct updates). Direct updates may be specified to apply to specific class
 * resources ("per-class" direct updates).
 * <p>
 * Per-class direct updates are matched to Java and JSP resources by doing a
 * lookup of an adjusted resource name. The resource is adjusted by removing the
 * initial file extension, either ".java" or ".jsp" is removed from the resource
 * name, then adding the extension ".class".
 */
public interface SignatureRule {
	// Category 1: Package renames.

	Map<String, String> getPackageRenames();
	String replacePackage(String initialName);
	String replacePackages(String text);

	Map<String, String> getBinaryPackageRenames();
	String replaceBinaryPackage(String initialName);
	String replaceBinaryPackages(String text);

	// Category 2: Package version updates

	Map<String, String> getPackageVersions();
	Map<String, Map<String, String>> getSpecificPackageVersions();
	String replacePackageVersion(String attributeName, String packageName, String initialVersion);

	// Category 3: Bundle updates

	BundleData getBundleUpdate(String symbolicName);

	// Category 4: Text updates

	boolean hasTextUpdates();

	Map<String, Map<String, String>> getSpecificTextUpdates();
	Map<Pattern, Map<String, String>> getWildCardTextUpdates();

	Map<String, String> getTextSubstitutions(String inputName);

	String replaceText(String inputName, String initialText);

	// Category 5: Direct string updates

	Map<String, String> getDirectGlobalUpdates();
	String replaceTextDirectGlobal(String initialValue, String inputName);

	Map<String, Map<String, String>> getDirectPerClassUpdates();
	String replaceTextDirectPerClass(String initialValue, String inputName);

	// Complex (java specific) transformations.

	String relocateResource(String inputPath);

	String packageRenameInput(String inputName);

	String transformBinaryType(String inputConstant);
	String transformDescriptor(String inputConstant);

	String transformSignature(String initialSignature, SignatureType signatureType);

	enum SignatureType {
		CLASS,
		FIELD,
		METHOD
	}
}
