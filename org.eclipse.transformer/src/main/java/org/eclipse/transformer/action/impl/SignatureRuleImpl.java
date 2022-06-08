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

import static org.eclipse.transformer.util.SignatureUtils.containsWildcard;
import static org.eclipse.transformer.util.SignatureUtils.keyStream;
import static org.eclipse.transformer.util.SignatureUtils.packageMatch;
import static org.eclipse.transformer.util.SignatureUtils.putSlashes;
import static org.eclipse.transformer.util.SignatureUtils.stripWildcard;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.SignatureUtils.RenameKeyComparator;
import org.slf4j.Logger;

import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.BaseType;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;
import aQute.bnd.signatures.TypeVariableSignature;
import aQute.bnd.stream.MapStream;
import aQute.libg.glob.Glob;

public class SignatureRuleImpl implements SignatureRule {

	public SignatureRuleImpl(Logger logger,
		Map<String, String> packageRenames,
		Map<String, String> packageVersions, Map<String, Map<String, String>> specificPackageVersions,
		Map<String, BundleData> bundleUpdates,
		Map<String, Map<String, String>> masterTextUpdates, Map<String, String> directStrings,
		Map<String, Map<String, String>> perClassDirectStrings) {

		this.logger = logger;

		// Cat 1: Package renames.

		Map<String, String> useDottedRenames;
		Map<String, String> useSlashedRenames;

		if ((packageRenames == null) || packageRenames.isEmpty()) {
			useDottedRenames = Collections.emptyMap();
			useSlashedRenames = Collections.emptyMap();
		} else {
			useDottedRenames = new LinkedHashMap<>(packageRenames.size());
			useSlashedRenames = new LinkedHashMap<>(packageRenames.size());

			// Order the rename keys to have more specific packages first and
			// wild cards last.

			MapStream.of(packageRenames)
				.sortedByKey(new RenameKeyComparator('.'))
				.forEachOrdered((initialName, finalName) -> {
					useDottedRenames.put(initialName, finalName);
					useSlashedRenames.put(putSlashes(initialName), putSlashes(finalName));
				});
		}

		this.dottedPackageRenames = useDottedRenames;
		this.slashedPackageRenames = useSlashedRenames;

		// Cat 2: Package version updates.

		Map<String, String> useVersions;
		if ((packageVersions != null) && !packageVersions.isEmpty()) {
			useVersions = new HashMap<>(packageVersions);
		} else {
			useVersions = Collections.emptyMap();
		}
		this.packageVersions = useVersions;

		Map<String, Map<String, String>> useSpecificVersions = null;
		if ( (specificPackageVersions != null) && !specificPackageVersions.isEmpty() ) {
			for ( Map.Entry<String, Map<String, String>> versionEntry : specificPackageVersions.entrySet() ) {
				String propertyName = versionEntry.getKey();
				Map<String, String> versionsForProperty = versionEntry.getValue();
				if ( useSpecificVersions == null ) {
					useSpecificVersions = new HashMap<>();
				}
				if ( (versionsForProperty != null) && !versionsForProperty.isEmpty() ) {
					useSpecificVersions.put(propertyName, new HashMap<>(versionsForProperty));
				}
			}
		}
		if ( useSpecificVersions == null ) {
			useSpecificVersions = new HashMap<>();
		}
		this.specificPackageVersions = useSpecificVersions;

		// Cat 3: Bundle updates

		Map<String, BundleData> useBundleUpdates;
		if ((bundleUpdates != null) && !bundleUpdates.isEmpty()) {
			useBundleUpdates = new HashMap<>(bundleUpdates);
		} else {
			useBundleUpdates = Collections.emptyMap();
		}
		this.bundleUpdates = useBundleUpdates;

		// Cat 4: Text updates

		if ((masterTextUpdates != null) && !masterTextUpdates.isEmpty()) {
			Map<String, Map<String, String>> useSpecificTextUpdates = new HashMap<>();
			Map<Pattern, Map<String, String>> useWildCardTextUpdates = new HashMap<>();

			for (Map.Entry<String, Map<String, String>> entry : masterTextUpdates.entrySet()) {
				String matchesFileName = entry.getKey();
				Map<String, String> substitutions = entry.getValue();

				if ((matchesFileName.indexOf('?') != -1) || (matchesFileName.indexOf('*') != -1)) {
					Pattern matchPattern = Glob.toPattern(matchesFileName);
					useWildCardTextUpdates.put(matchPattern, substitutions);
				} else {
					useSpecificTextUpdates.put(matchesFileName, substitutions);
				}
			}

			this.specificTextUpdates = useSpecificTextUpdates;
			this.wildCardTextUpdates = useWildCardTextUpdates;

		} else {
			this.specificTextUpdates = Collections.emptyMap();
			this.wildCardTextUpdates = Collections.emptyMap();
		}

		// Cat 5: Direct string updates.

		Map<String, String> useDirectStrings;
		if ((directStrings == null) || directStrings.isEmpty()) {
			useDirectStrings = Collections.emptyMap();
		} else {
			useDirectStrings = new HashMap<>(directStrings);
		}
		this.directStrings = useDirectStrings;

		Map<String, Map<String, String>> usePerClassDirectStrings;
		if ((perClassDirectStrings == null) || perClassDirectStrings.isEmpty()) {
			usePerClassDirectStrings = Collections.emptyMap();
		} else {
			usePerClassDirectStrings = new HashMap<>(perClassDirectStrings.size());
			for (Map.Entry<String, Map<String, String>> perClassEntry : perClassDirectStrings.entrySet()) {
				String key = perClassEntry.getKey();
				Map<String, String> value = perClassEntry.getValue();
				Map<String, String> localValue = new HashMap<>(value);
				usePerClassDirectStrings.put(key, localValue);
			}
		}
		this.perClassDirectStrings = usePerClassDirectStrings;

		//

		this.unchangedBinaryTypes = new HashSet<>();
		this.changedBinaryTypes = new HashMap<>();

		this.unchangedDescriptors = new HashSet<>();
		this.changedDescriptors = new HashMap<>();

		this.unchangedSignatures = new HashSet<>();
		this.changedSignatures = new HashMap<>();
	}

	//

	private final Logger logger;

	public Logger getLogger() {
		return logger;
	}

	// Cat 1: Package renames

	// Package rename: "javax.servlet" ==> "jakarta.servlet"
	// Dotted form : "javax.servlet" ==> "jakarta.servlet"
	// Slashed form: "javax/servlet" ==> "jakarta/servlet"

	protected final Map<String, String>	dottedPackageRenames;
	protected final Map<String, String>	slashedPackageRenames;

	@Override
	public Map<String, String> getPackageRenames() {
		return dottedPackageRenames;
	}

	@Override
	public Map<String, String> getBinaryPackageRenames() {
		return slashedPackageRenames;
	}

	@Override
	public String replacePackage(String initialName) {
		return replacePackage(initialName, DOT_WILDCARD, dottedPackageRenames);
	}

	@Override
	public String replaceBinaryPackage(String initialName) {
		return replacePackage(initialName, SLASH_WILDCARD, slashedPackageRenames);
	}

	private static final String	DOT_WILDCARD	= ".*";
	private static final String	SLASH_WILDCARD	= "/*";

	private String replacePackage(String initialName, String wildcard, Map<String, String> renames) {
		String finalName = keyStream(initialName, wildcard).filter(renames::containsKey)
			.findFirst()
			.map(key -> {
				String name = renames.get(key);
				if (containsWildcard(key)) {
					name = name.concat(initialName.substring(key.length() - 2));
				}
				return name;
			})
			.orElse(null);
		return finalName;
	}

	@Override
	public String replacePackages(String text) {
		return replacePackages(text, dottedPackageRenames);
	}

	@Override
	public String replaceBinaryPackages(String text) {
		return replacePackages(text, slashedPackageRenames);
	}

	// TODO: Unify the implementations of 'replacePackages'
	// and 'replacePackage'.
	//
	// See issue #307.

	private String replacePackages(String text, Map<String, String> renames) {
		String initialText = text;

		for (Map.Entry<String, String> renameEntry : renames.entrySet()) {
			String key = renameEntry.getKey();

			boolean matchPackageStem = containsWildcard(key);
			if (matchPackageStem) {
				key = stripWildcard(key);
			}

			int keyLen = key.length();
			int textLimit = text.length() - keyLen;

			for (int matchEnd = 0; matchEnd <= textLimit;) {
				int matchStart = text.indexOf(key, matchEnd);
				if (matchStart == -1) {
					break;
				}

				matchEnd = matchStart + keyLen;
				int packageEnd = packageMatch(text, matchStart, matchEnd, matchPackageStem);
				if (packageEnd == -1) {
					continue;
				}

				String value = renameEntry.getValue();
				if (matchEnd < packageEnd) {
					value = value.concat(text.substring(matchEnd, packageEnd));
				}

				String head = text.substring(0, matchStart);
				String tail = text.substring(packageEnd);
				text = head + value + tail;

				matchEnd = matchStart + value.length();
				textLimit = text.length() - keyLen;
			}
		}

		if (initialText == text) {
			return null;
		} else {
			return text;
		}
	}

	// Cat 2: Package Version Updates

	protected final Map<String, String> packageVersions;

	protected final Map<String, Map<String, String>>	specificPackageVersions;

	@Override
	public Map<String, String> getPackageVersions() {
		return packageVersions;
	}

	@Override
	public Map<String, Map<String, String>> getSpecificPackageVersions() {
		return specificPackageVersions;
	}

	@Override
	public String replacePackageVersion(String attributeName, String packageName, String oldVersion) {
		Logger useLogger = getLogger();

		Map<String, String> versionsForAttribute = getSpecificPackageVersions().get(attributeName);

		String specificVersion;
		if (versionsForAttribute != null) {
			specificVersion = versionsForAttribute.get(packageName);
		} else {
			specificVersion = null;
		}

		String genericVersion = getPackageVersions().get(packageName);

		if ((specificVersion == null) && (genericVersion == null)) {
			useLogger.trace("Manifest attribute {}: Package {} version {} is unchanged", attributeName, packageName,
				oldVersion);
			return null;
		} else if (specificVersion == null) {
			useLogger.trace("Manifest attribute {}: Generic update of package {} version {} to {}", attributeName,
				packageName,
				oldVersion, genericVersion);
			return genericVersion;
		} else if (genericVersion == null) {
			useLogger.trace("Manifest attribute {}: Specific update of package {} version {} to {}", attributeName,
				packageName,
				oldVersion, specificVersion);
			return specificVersion;
		} else {
			useLogger.trace(
				"Manifest attribute {}: Specific update of package {} version {} to {} overrides generic version update {}",
				attributeName, packageName, oldVersion, specificVersion, genericVersion);
			return specificVersion;
		}
	}

	// Category 3: Bundle updates

	private final Map<String, BundleData> bundleUpdates;

	@Override
	public BundleData getBundleUpdate(String symbolicName) {
		return bundleUpdates.get(symbolicName);
	}

	// Category 4: Text updates

	private final Map<String, Map<String, String>>	specificTextUpdates;
	private final Map<Pattern, Map<String, String>>	wildCardTextUpdates;

	@Override
	public Map<String, Map<String, String>> getSpecificTextUpdates() {
		return specificTextUpdates;
	}

	@Override
	public Map<Pattern, Map<String, String>> getWildCardTextUpdates() {
		return wildCardTextUpdates;
	}

	@Override
	public Map<String, String> getTextSubstitutions(String inputName) {
		String simpleFileName = FileUtils.getFileNameFromFullyQualifiedFileName(inputName);

		Map<String, Map<String, String>> specificUpdates = getSpecificTextUpdates();
		Map<String, String> updates = specificUpdates.get(simpleFileName);
		if (updates != null) {
			return updates;
		}

		Map<Pattern, Map<String, String>> wildcardUpdates = getWildCardTextUpdates();
		for (Map.Entry<Pattern, Map<String, String>> wildcardEntry : wildcardUpdates.entrySet()) {
			if (matches(wildcardEntry.getKey(), simpleFileName)) {
				return wildcardEntry.getValue();
			}
		}

		return null;
	}

	private static boolean matches(Pattern p, CharSequence input) {
		Matcher m = p.matcher(input);
		return m.matches();
	}

	@Override
	public String replaceText(String inputName, String text) {
		Map<String, String> substitutions = getTextSubstitutions(inputName);
		if (substitutions == null) {
			// This is now allowed, because of of the new
			// substitution cases (direct, package-rename).
			return null;
		}

		String initialText = text;

		for (Map.Entry<String, String> entry : substitutions.entrySet()) {
			String key = entry.getKey();
			int keyLen = key.length();

			int textLimit = text.length() - keyLen;

			int lastMatchEnd = 0;
			while (lastMatchEnd <= textLimit) {
				int matchStart = text.indexOf(key, lastMatchEnd);
				if (matchStart == -1) {
					break;
				}

				String value = entry.getValue();
				int valueLen = value.length();

				String head = text.substring(0, matchStart);
				String tail = text.substring(matchStart + keyLen);
				text = head + value + tail;

				lastMatchEnd = matchStart + valueLen;
				textLimit += (valueLen - keyLen);
			}
		}

		if (initialText == text) {
			return null;
		} else {
			return text;
		}
	}

	// Cat 5: Direct string updates

	private final Map<String, String> directStrings;

	private final Map<String, Map<String, String>>	perClassDirectStrings;

	@Override
	public boolean hasTextUpdates() {
		return (!getSpecificTextUpdates().isEmpty() || !getWildCardTextUpdates().isEmpty());
	}

	@Override
	public Map<String, String> getDirectGlobalUpdates() {
		return directStrings;
	}

	@Override
	public Map<String, Map<String, String>> getDirectPerClassUpdates() {
		return perClassDirectStrings;
	}

	/**
	 * Perform global (non-resource specific) substitutions on a UTF8 value
	 * obtained as a string constant from a java class resource, or as a line in
	 * a java-like UTF8 resource, such as a JSP or java text resource.
	 *
	 * @param initialValue An initial UTF8 string constant value.
	 * @param inputName The name of the resource in which the value appears.
	 * @return Null if no updates were made. Otherwise, the updated value.
	 */
	@Override
	public String replaceTextDirectGlobal(String initialValue, String inputName) {
		return replaceTextDirect(initialValue, inputName, directStrings, "Global");
	}

	/**
	 * Perform per-class substitutions on a UTF8 value obtained as a string
	 * constant from a java class resource, or as a line in a java-like UTF8
	 * resource, such as a JSP or java text resource.
	 * <p>
	 * Answer null if no updates were made. Otherwise, answer the updated UF8
	 * value.
	 *
	 * @param initialValue An initial UTF8 string constant value.
	 * @param inputName The name of the resource in which the value appears.
	 * @return Null if no updates were made. Otherwise, the updated value.
	 */
	@Override
	public String replaceTextDirectPerClass(String initialValue, String inputName) {
		Map<String, String> directStringsForClass = perClassDirectStrings.get(inputName);
		if (directStringsForClass == null) {
			return null; // Nothing specific to do.
		}
		return replaceTextDirect(initialValue, inputName, directStringsForClass, "Per-Class");
	}

	private String replaceTextDirect(String initialValue, String inputName, Map<String, String> updates, String updateCase) {
		Logger useLogger = getLogger();

		// If the table has a simple, full substitution, use it.
		// This is an optimization of the token substitution case.

		String fullFinalValue = updates.get(initialValue);
		if (fullFinalValue != null) {
			useLogger.debug("{} full direct replacement: [ {} ]: [ {} => {} ]", updateCase, inputName, initialValue,
				fullFinalValue);
			return fullFinalValue;
		}

		// Perform all possible substitutions, in order.
		//
		// Match using a simple 'contains' test. Later,
		// a regular expression might be used.

		String finalValue = initialValue;

		for (Map.Entry<String, String> directEntry : updates.entrySet()) {
			String initialSubValue = directEntry.getKey();
			String finalSubValue = directEntry.getValue();
			if (finalValue.contains(initialSubValue)) {
				finalValue = finalValue.replace(initialSubValue, finalSubValue);
				useLogger.debug("{} token direct replacement: [ {} ]: [ {} => {} ]", updateCase, inputName,
					initialSubValue,
					finalSubValue);
			}
		}

		if (finalValue != initialValue) {
			return finalValue;
		} else {
			return null;
		}
	}

	/**
	 * Rename an input using the package rename rules. This is done for
	 * {@link ServiceLoaderConfigActionImpl} and for {@link JavaActionImpl}, but
	 * not for {@link ClassActionImpl}. The class action implementation uses the
	 * updated name of the class which results from transforming the class
	 * bytes. The service loader configuration and java actions do not have this
	 * available.
	 * </p>
	 * See {@replacePackage}.
	 *
	 * @param inputName The input name which is to be renamed.
	 * @return The updated name. Null if no change is to be made.
	 */
	@Override
	public String packageRenameInput(String inputName) {
		String inputPrefix;
		String qualifiedName;

		int lastSlash = inputName.lastIndexOf('/');
		if (lastSlash == -1) {
			inputPrefix = null;
			qualifiedName = inputName;
		} else {
			inputPrefix = inputName.substring(0, lastSlash + 1);
			qualifiedName = inputName.substring(lastSlash + 1);
		}

		int classStart = qualifiedName.lastIndexOf('.');
		if (classStart == -1) {
			return null;
		}

		String packageName = qualifiedName.substring(0, classStart);
		if (packageName.isEmpty()) {
			return null;
		}

		// 'className' includes a leading '.'
		String className = qualifiedName.substring(classStart);

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

	// Complex transformations

	/**
	 * Complex java transformations are transformations of binary types,
	 * descriptors, and signatures, which use the package rename table.
	 * <p>
	 * The transformations are complex in that binary types, descriptors, and
	 * signatures must be unpacked to reveal embedded package names, then
	 * repacked to generate a transformed value.
	 * <p>
	 * For efficiency, a caches are maintained of complex values which have been
	 * transformed. That enables quick lookup of the previously transformed
	 * values, skipping the complex unpacking and repacking of the repeated
	 * values.
	 *
	 * @param inputConstant An initial binary type value.
	 * @return The transformed binary type. Null if no change was made.
	 */
	@Override
	public String transformBinaryType(String inputConstant) {
		try {
			return basicTransformBinaryType(inputConstant);
		} catch (Throwable th) {
			getLogger().trace("Failed to parse constant as resource reference [ {} ]", inputConstant, th);
			return null;
		}
	}

	private final Map<String, String>	changedBinaryTypes;
	private final Set<String>			unchangedBinaryTypes;

	/**
	 * Modify a fully qualified type name according to the package rename table.
	 * Answer either the transformed type name, or, if the type name was not
	 * changed, null.
	 *
	 * @param inputName A fully qualified type name which is to be transformed.
	 * @return The transformed type name, or null if no changed was made.
	 */
	private String basicTransformBinaryType(String inputName) {
		if (unchangedBinaryTypes.contains(inputName)) {
			return null;
		}

		String outputName = changedBinaryTypes.get(inputName);
		if (outputName != null) {
			return outputName;
		}

		char c = inputName.charAt(0);
		if ((c == '[') || ((c == 'L') && (inputName.charAt(inputName.length() - 1) == ';'))) {
			JavaTypeSignature inputSignature = JavaTypeSignature.of(inputName.replace('$', '.'));
			JavaTypeSignature outputSignature = transform(inputSignature);
			if (outputSignature != null) {
				outputName = outputSignature.toString()
					.replace('.', '$');
			} else {
				// Leave outputName null.
			}

		} else {
			int lastSlashOffset = inputName.lastIndexOf('/');
			if (lastSlashOffset != -1) {
				String inputPackage = inputName.substring(0, lastSlashOffset);
				String outputPackage = replaceBinaryPackage(inputPackage);
				if (outputPackage != null) {
					outputName = outputPackage + inputName.substring(lastSlashOffset);
				} else {
					// Leave outputName null.
				}
			} else {
				// Leave outputName with null;
			}
		}

		if (outputName == null) {
			unchangedBinaryTypes.add(inputName);
		} else {
			changedBinaryTypes.put(inputName, outputName);
		}

		return outputName;
	}

	//

	@Override
	public String transformDescriptor(String inputConstant) {
		try {
			return basicTransformDescriptor(inputConstant);
		} catch (Throwable th) {
			getLogger().trace("Failed to parse constant as descriptor [ {} ]", inputConstant, th);
			return null;
		}
	}

	@Override
	public String relocateResource(String inputPath) {
		String prefix;
		String inputName;
		if (inputPath.startsWith("WEB-INF/classes/")) {
			prefix = "WEB-INF/classes/";
			inputName = inputPath.substring(prefix.length());
		} else if (inputPath.startsWith("META-INF/versions/")) {
			prefix = "META-INF/versions/";
			int nextSlash = inputPath.indexOf('/', prefix.length());
			if (nextSlash != -1) {
				prefix = inputPath.substring(0, nextSlash + 1);
			}
			inputName = inputPath.substring(prefix.length());
		} else {
			prefix = "";
			inputName = inputPath;
		}
		if (!inputName.isEmpty()) {
			String outputName = transformBinaryType(inputName);
			if (outputName != null) {
				String outputPath = prefix.isEmpty() ? outputName : prefix.concat(outputName);
				return outputPath;
			}
		}
		return inputPath;
	}

	private final Set<String>			unchangedDescriptors;
	private final Map<String, String>	changedDescriptors;

	private String basicTransformDescriptor(String inputDescriptor) {
		if (unchangedDescriptors.contains(inputDescriptor)) {
			return null;
		}

		String outputDescriptor = changedDescriptors.get(inputDescriptor);
		if (outputDescriptor != null) {
			return outputDescriptor;
		}

		char c = inputDescriptor.charAt(0);
		if (c == '(') {
			String inputSignature = inputDescriptor.replace('$', '.');
			String outputSignature = transformSignature(inputSignature, SignatureType.METHOD);
			if (outputSignature != null) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else if ((c == '[') || ((c == 'L') && (inputDescriptor.charAt(inputDescriptor.length() - 1) == ';'))) {
			String inputSignature = inputDescriptor.replace('$', '.');
			String outputSignature = transformSignature(inputSignature, SignatureType.FIELD);
			if (outputSignature != null) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else {
			// leave outputDescriptor null
		}

		if (outputDescriptor == null) {
			unchangedDescriptors.add(inputDescriptor);
		} else {
			changedDescriptors.put(inputDescriptor, outputDescriptor);
		}
		return outputDescriptor;
	}

	/**
	 * Caches of transformed signatures. A single unified mapping is used, even
	 * through there are three different types of signatures. The different
	 * types of signatures each has its own syntax. There are not equal values
	 * across signature types.
	 */

	private final Set<String>			unchangedSignatures;
	private final Map<String, String>	changedSignatures;

	/**
	 * Transform a class, field, or method signature. Answer a wrapped null if
	 * the signature is not changed by the transformation rules.
	 *
	 * @param initialSignature The signature value which is to be transformed.
	 * @param signatureType The type of the signature value.
	 * @return The transformed signature value. A wrapped null if no change was
	 *         made to the value.
	 */
	@Override
	public String transformSignature(String initialSignature, SignatureType signatureType) {
		if (unchangedSignatures.contains(initialSignature)) {
			return null;
		}

		// The signature print strings have distinct formats. They may be safely
		// stored in a single hash map.

		String finalSignature = changedSignatures.get(initialSignature);
		if (finalSignature != null) {
			return finalSignature;
		}

		if (signatureType == SignatureType.CLASS) {
			ClassSignature initialClassSignature = ClassSignature.of(initialSignature);
			ClassSignature finalClassSignature = transform(initialClassSignature);
			if (finalClassSignature != null) {
				finalSignature = finalClassSignature.toString();
			} else {
				// leave output null;
			}

		} else if (signatureType == SignatureType.FIELD) {
			FieldSignature initialFieldSignature = FieldSignature.of(initialSignature);
			FieldSignature finalFieldSignature = transform(initialFieldSignature);
			if (finalFieldSignature != null) {
				finalSignature = finalFieldSignature.toString();
			} else {
				// leave output null;
			}

		} else if (signatureType == SignatureType.METHOD) {
			MethodSignature initialMethodSignature = MethodSignature.of(initialSignature);
			MethodSignature finalMethodSignature = transform(initialMethodSignature);
			if (finalMethodSignature != null) {
				finalSignature = finalMethodSignature.toString();
			} else {
				// leave output null
			}

		} else {
			throw new IllegalArgumentException(
				"Signature [ " + initialSignature + " ] uses unknown type [ " + signatureType + " ]");
		}

		if (finalSignature == null) {
			unchangedSignatures.add(initialSignature);
		} else {
			changedSignatures.put(initialSignature, finalSignature);
		}

		return finalSignature;
	}

	private ClassSignature transform(ClassSignature classSignature) {
		TypeParameter[] inputTypes = classSignature.typeParameters;
		TypeParameter[] outputTypes = null;

		for (int parmNo = 0; parmNo < inputTypes.length; parmNo++) {
			TypeParameter inputType = inputTypes[parmNo];
			TypeParameter outputType = transform(inputType);

			if (outputType != null) {
				if (outputTypes == null) {
					outputTypes = inputTypes.clone();
				}
				outputTypes[parmNo] = outputType;
			}
		}

		ClassTypeSignature inputSuperClass = classSignature.superClass;
		ClassTypeSignature outputSuperClass = transform(inputSuperClass);

		ClassTypeSignature[] inputInterfaces = classSignature.superInterfaces;
		ClassTypeSignature[] outputInterfaces = null;

		for (int interfaceNo = 0; interfaceNo < inputInterfaces.length; interfaceNo++) {
			ClassTypeSignature inputInterface = inputInterfaces[interfaceNo];
			ClassTypeSignature outputInterface = transform(inputInterface);

			if (outputInterface != null) {
				if (outputInterfaces == null) {
					outputInterfaces = inputInterfaces.clone();
				}
				outputInterfaces[interfaceNo] = outputInterface;
			}
		}

		if ((outputTypes == null) && (outputSuperClass == null) && (outputInterfaces == null)) {
			return null;
		} else {
			return new ClassSignature(((outputTypes == null) ? inputTypes : outputTypes),
				((outputSuperClass == null) ? inputSuperClass : outputSuperClass),
				((outputInterfaces == null) ? inputInterfaces : outputInterfaces));
		}
	}

	private FieldSignature transform(FieldSignature fieldSignature) {
		ReferenceTypeSignature inputType = fieldSignature.type;
		ReferenceTypeSignature outputType = transform(inputType);

		if (outputType == null) {
			return null;
		} else {
			return new FieldSignature(outputType);
		}
	}

	private MethodSignature transform(MethodSignature methodSignature) {
		TypeParameter[] inputTypeParms = methodSignature.typeParameters;
		TypeParameter[] outputTypeParms = null;

		for (int parmNo = 0; parmNo < inputTypeParms.length; parmNo++) {
			TypeParameter inputTypeParm = inputTypeParms[parmNo];
			TypeParameter outputTypeParm = transform(inputTypeParm);
			if (outputTypeParm != null) {
				if (outputTypeParms == null) {
					outputTypeParms = inputTypeParms.clone();
				}
				outputTypeParms[parmNo] = outputTypeParm;
			}
		}

		JavaTypeSignature[] inputParmTypes = methodSignature.parameterTypes;
		JavaTypeSignature[] outputParmTypes = null;

		for (int parmNo = 0; parmNo < inputParmTypes.length; parmNo++) {
			JavaTypeSignature inputParmType = inputParmTypes[parmNo];
			JavaTypeSignature outputParmType = transform(inputParmType);
			if (outputParmType != null) {
				if (outputParmTypes == null) {
					outputParmTypes = inputParmTypes.clone();
				}
				outputParmTypes[parmNo] = outputParmType;
			}
		}

		Result inputResult = methodSignature.resultType;
		Result outputResult = transform(inputResult);

		ThrowsSignature[] inputThrows = methodSignature.throwTypes;
		ThrowsSignature[] outputThrows = null;

		for (int throwNo = 0; throwNo < inputThrows.length; throwNo++) {
			ThrowsSignature inputThrow = inputThrows[throwNo];
			ThrowsSignature outputThrow = transform(inputThrow);
			if (outputThrow != null) {
				if (outputThrows == null) {
					outputThrows = inputThrows.clone();
				}
				outputThrows[throwNo] = outputThrow;
			}
		}

		if ((outputTypeParms == null) && (outputParmTypes == null) && (outputResult == null)
			&& (outputThrows == null)) {
			return null;

		} else {
			return new MethodSignature(((outputTypeParms == null) ? inputTypeParms : outputTypeParms),
				((outputParmTypes == null) ? inputParmTypes : outputParmTypes),
				((outputResult == null) ? inputResult : outputResult),
				((outputThrows == null) ? inputThrows : outputThrows));
		}
	}

	private Result transform(Result type) {
		if (type instanceof JavaTypeSignature) {
			return transform((JavaTypeSignature) type);
		} else {
			return null;
		}
	}

	private ThrowsSignature transform(ThrowsSignature type) {
		if (type instanceof ClassTypeSignature) {
			return transform((ClassTypeSignature) type);
		} else {
			return null;
		}
	}

	private ArrayTypeSignature transform(ArrayTypeSignature inputType) {
		JavaTypeSignature inputComponent = inputType.component;
		int componentDepth = 1;
		while (inputComponent instanceof ArrayTypeSignature) {
			componentDepth++;
			inputComponent = ((ArrayTypeSignature) inputComponent).component;
		}
		if ((inputComponent instanceof BaseType) || (inputComponent instanceof TypeVariableSignature)) {
			return null;
		}

		JavaTypeSignature outputComponent = transform((ClassTypeSignature) inputComponent);
		if (outputComponent == null) {
			return null;
		}

		ArrayTypeSignature outputType = new ArrayTypeSignature(outputComponent);
		while (--componentDepth > 0) {
			outputType = new ArrayTypeSignature(outputType);
		}
		return outputType;
	}

	private TypeParameter transform(TypeParameter inputTypeParameter) {
		ReferenceTypeSignature inputClassBound = inputTypeParameter.classBound;
		ReferenceTypeSignature outputClassBound = transform(inputClassBound);

		ReferenceTypeSignature[] inputBounds = inputTypeParameter.interfaceBounds;
		ReferenceTypeSignature[] outputBounds = null;

		for (int boundNo = 0; boundNo < inputBounds.length; boundNo++) {
			ReferenceTypeSignature inputBound = inputBounds[boundNo];
			ReferenceTypeSignature outputBound = transform(inputBound);
			if (outputBound != null) {
				if (outputBounds == null) {
					outputBounds = inputBounds.clone();
				}
				outputBounds[boundNo] = outputBound;
			}
		}

		if ((outputClassBound == null) && (outputBounds == null)) {
			return null;
		} else {
			return new TypeParameter(inputTypeParameter.identifier,
				((outputClassBound == null) ? inputClassBound : outputClassBound),
				((outputBounds == null) ? inputBounds : outputBounds));
		}
	}

	private ClassTypeSignature transform(ClassTypeSignature inputType) {
		String inputPackageSpecifier = inputType.packageSpecifier;
		String outputPackageSpecifier = null;

		int length = inputPackageSpecifier.length();
		if (length > 0) {
			String inputBinaryPackage = inputPackageSpecifier.substring(0, length - 1);
			String outputBinaryPackage = replaceBinaryPackage(inputBinaryPackage);
			if (outputBinaryPackage != null) {
				outputPackageSpecifier = outputBinaryPackage + '/';
			}
		}

		SimpleClassTypeSignature inputClassType = inputType.classType;
		SimpleClassTypeSignature outputClassType = transform(inputClassType);

		SimpleClassTypeSignature[] inputInnerTypes = inputType.innerTypes;
		SimpleClassTypeSignature[] outputInnerTypes = null;

		for (int typeNo = 0; typeNo < inputInnerTypes.length; typeNo++) {
			SimpleClassTypeSignature inputInnerType = inputInnerTypes[typeNo];
			SimpleClassTypeSignature outputInnerType = transform(inputInnerType);
			if (outputInnerType != null) {
				if (outputInnerTypes == null) {
					outputInnerTypes = inputInnerTypes.clone();
				}
				outputInnerTypes[typeNo] = outputInnerType;
			}
		}

		// Do not transform 'type.binary'.

		if ((outputPackageSpecifier == null) && (outputClassType == null) && (outputInnerTypes == null)) {
			return null;
		} else {
			return new ClassTypeSignature(inputType.binary,
				((outputPackageSpecifier == null) ? inputPackageSpecifier : outputPackageSpecifier),
				((outputClassType == null) ? inputClassType : outputClassType),
				((outputInnerTypes == null) ? inputInnerTypes : outputInnerTypes));
		}
	}

	private SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature) {
		TypeArgument[] inputArgs = inputSignature.typeArguments;
		TypeArgument[] outputArgs = null;

		for (int argNo = 0; argNo < inputArgs.length; argNo++) {
			TypeArgument inputArg = inputArgs[argNo];
			TypeArgument outputArg = transform(inputArg);
			if (outputArg != null) {
				if (outputArgs == null) {
					outputArgs = inputArgs.clone();
				}
				outputArgs[argNo] = outputArg;
			}
		}

		if (outputArgs == null) {
			return null;
		} else {
			return new SimpleClassTypeSignature(inputSignature.identifier, outputArgs);
		}
	}

	private TypeArgument transform(TypeArgument inputArgument) {
		ReferenceTypeSignature inputSignature = inputArgument.type;
		ReferenceTypeSignature outputSignature = transform(inputSignature);
		if (outputSignature == null) {
			return null;
		} else {
			return new TypeArgument(inputArgument.wildcard, outputSignature);
		}
	}

	private JavaTypeSignature transform(JavaTypeSignature type) {
		if (type instanceof ReferenceTypeSignature) {
			return transform((ReferenceTypeSignature) type);
		} else {
			return null;
		}
	}

	private ReferenceTypeSignature transform(ReferenceTypeSignature type) {
		if (type instanceof ClassTypeSignature) {
			return transform((ClassTypeSignature) type);

		} else if (type instanceof ArrayTypeSignature) {
			return transform((ArrayTypeSignature) type);

		} else {
			return null;
		}
	}
}
