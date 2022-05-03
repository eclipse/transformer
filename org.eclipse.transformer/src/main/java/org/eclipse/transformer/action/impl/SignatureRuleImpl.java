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
import org.eclipse.transformer.util.SignatureUtils;
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
		Map<String, String> renames,
		Map<String, String> versions, Map<String, Map<String, String>> specificVersions,
		Map<String, BundleData> bundleUpdates,
		Map<String, Map<String, String>> masterTextUpdates, Map<String, String> directStrings,
		Map<String, Map<String, String>> perClassDirectStrings) {

		this.logger = logger;

		Map<String, String> useRenames;
		Map<String, String> useBinaryRenames;

		if ((renames == null) || renames.isEmpty()) {
			useRenames = Collections.emptyMap();
			useBinaryRenames = Collections.emptyMap();
		} else {
			/*
			 * Order the rename keys to have more specific packages first and
			 * wild cards last.
			 */
			useRenames = new LinkedHashMap<>(renames.size());
			useBinaryRenames = new LinkedHashMap<>(renames.size());

			MapStream.of(renames)
				.sortedByKey(new SignatureUtils.RenameKeyComparator('.'))
				.forEachOrdered((initialName, finalName) -> {
					useRenames.put(initialName, finalName);

					String initialBinaryName = initialName.replace('.', '/');
					String finalBinaryName = finalName.replace('.', '/');
					useBinaryRenames.put(initialBinaryName, finalBinaryName);
				});
		}

		this.dottedPackageRenames = useRenames;
		this.slashedPackageRenames = useBinaryRenames;

		Map<String, String> useVersions;
		if ((versions != null) && !versions.isEmpty()) {
			useVersions = new HashMap<>(versions);
		} else {
			useVersions = Collections.emptyMap();
		}
		this.packageVersions = useVersions;

		Map<String, Map<String, String>> useSpecificVersions = null;
		if ( (specificVersions != null) && !specificVersions.isEmpty() ) {
			for ( Map.Entry<String, Map<String, String>> versionEntry : specificVersions.entrySet() ) {
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

		Map<String, BundleData> useBundleUpdates;
		if ((bundleUpdates != null) && !bundleUpdates.isEmpty()) {
			useBundleUpdates = new HashMap<>(bundleUpdates);
		} else {
			useBundleUpdates = Collections.emptyMap();
		}
		this.bundleUpdates = useBundleUpdates;

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
			this.specificTextUpdates = null;
			this.wildCardTextUpdates = null;
		}

		Map<String, String> useDirectStrings;
		if ((directStrings == null) || directStrings.isEmpty()) {
			useDirectStrings = Collections.emptyMap();
		} else {
			useDirectStrings = new HashMap<>(directStrings);
		}
		this.directStrings = useDirectStrings;

		this.unchangedBinaryTypes = new HashSet<>();
		this.changedBinaryTypes = new HashMap<>();

		this.unchangedSignatures = new HashSet<>();
		this.changedSignatures = new HashMap<>();

		this.unchangedDescriptors = new HashSet<>();
		this.changedDescriptors = new HashMap<>();

		Map<String, Map<String, String>> localPerClassDirectStrings;
		if ((perClassDirectStrings == null) || perClassDirectStrings.isEmpty()) {
			localPerClassDirectStrings = Collections.emptyMap();
		} else {
			localPerClassDirectStrings = new HashMap<>(perClassDirectStrings.size());
			for (Map.Entry<String, Map<String, String>> perClassEntry : perClassDirectStrings.entrySet()) {
				String key = perClassEntry.getKey();
				Map<String, String> value = perClassEntry.getValue();
				Map<String, String> localValue = new HashMap<>(value);
				localPerClassDirectStrings.put(key, localValue);
			}
		}
		this.perClassDirectStrings = localPerClassDirectStrings;
	}



	//

	private final Logger logger;

	public Logger getLogger() {
		return logger;
	}

	//

	private final Map<String, BundleData> bundleUpdates;

	@Override
	public BundleData getBundleUpdate(String symbolicName) {
		return bundleUpdates.get(symbolicName);
	}

	//

	private final Map<String, Map<String, String>>	specificTextUpdates;
	private final Map<Pattern, Map<String, String>>	wildCardTextUpdates;

	public Map<String, Map<String, String>> getSpecificTextUpdates() {
		return ((specificTextUpdates == null) ? Collections.emptyMap() : specificTextUpdates);
	}

	public Map<Pattern, Map<String, String>> getWildCardTextUpdates() {
		return ((wildCardTextUpdates == null) ? Collections.emptyMap() : wildCardTextUpdates);
	}

	//

	private final Map<String, String> directStrings;

	@Override
	public String getDirectString(String initialValue) {
		return directStrings.get(initialValue);
	}

	private final Map<String, Map<String, String>> perClassDirectStrings;

	/**
	 * Perform per-class substitutions on a initial UTF8 value obtained as a
	 * string constant. Answer null if no updates were made. Otherwise, answer
	 * the updated UF8 value. Per-class substitutions are performed on all
	 * occurrences of the per-class key values. This is in contract to
	 * substitutions which are made on all values, which are of entire strings.
	 *
	 * @param initialValue An initial UTF8 string constant value.
	 * @param className The name of the class in which the string constant
	 *            appears.
	 * @return Null if no updates were made. Otherwise, the updated value.
	 */
	@Override
	public String getDirectString(String initialValue, String className) {
		// There is nothing to do if no table was specified
		// for the class which is being updated.

		Map<String, String> directStringsForClass = perClassDirectStrings.get(className);
		if (directStringsForClass == null) {
			return null;
		}

		// If the table has a simple, full substitution, use it.
		// This is an optimization of the token substitution case.

		String full = directStringsForClass.get(initialValue);
		if (full != null) {
			getLogger().debug("Per class direct replacement:[{}], {} => {}", className, initialValue, full);
			return full;
		}

		// Perform all possible substitutions, in order.
		//
		// Match using a simple 'contains' test. Later,
		// a regular expression might be used.

		String transformedString = initialValue;
		boolean transformed = false;
		for (String initialSubstring : directStringsForClass.keySet()) {
			if (transformedString.contains(initialSubstring)) {
				String finalSubstring = directStringsForClass.get(initialSubstring);
				transformedString = transformedString.replace(initialSubstring, finalSubstring);
				getLogger().debug("Per class token replacement:[{}], {} => {}", className, initialSubstring,
					finalSubstring);
				transformed = true;
			}
		}

		if (transformed) {
			getLogger().debug("Per class token replacement: [{}] {} => {}", className, initialValue, transformedString);
			return transformedString;
		} else {
			return null;
		}
	}

	//

	// Package rename: "javax.servlet.Servlet"
	// Direct form : "javax.servlet"
	// Binary form: "javax/servlet"

	protected final Map<String, String>	dottedPackageRenames;
	protected final Map<String, String>	slashedPackageRenames;

	@Override
	public Map<String, String> getPackageRenames() {
		return dottedPackageRenames;
	}

	//

	/**
	 * Package version updates for all occurrences which are not otherwise
	 * specialized by {@link #specificPackageVersions}.
	 */
	protected final Map<String, String> packageVersions;

	@Override
	public Map<String, String> getPackageVersions() {
		return packageVersions;
	}

	/**
	 * Package version updates for specific occurrences. Overrides
	 * {@link #packageVersions}.
	 */
	protected final Map<String, Map<String, String>> specificPackageVersions;

	@Override
	public Map<String, Map<String, String>> getSpecificPackageVersions() {
		return specificPackageVersions;
	}

	public Map<String, String> getSpecificPackageVersions(String propertyName) {
		return getSpecificPackageVersions().get(propertyName);
	}

	public Set<String> getSpecificPackageVersionProperties() {
		return getSpecificPackageVersions().keySet();
	}

	/**
	 * Replace a single package according to the package rename rules. Package
	 * names must match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 * @return The replacement for the initial package name. Null if no
	 *         replacement is available.
	 */
	@Override
	public String replacePackage(String initialName) {
		String finalName = keyStream(initialName, ".*").filter(dottedPackageRenames::containsKey)
			.findFirst()
			.map(key -> {
				String name = dottedPackageRenames.get(key);
				if (containsWildcard(key)) {
					name = name.concat(initialName.substring(key.length() - 2));
				}
				return name;
			})
			.orElse(null);
		return finalName;
	}

	/**
	 * Replace a single package according to the package rename rules. The
	 * package name has '/' separators, not '.' separators. Package names must
	 * match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 * @return The replacement for the initial package name. Null if no
	 *         replacement is available.
	 */
	@Override
	public String replaceBinaryPackage(String initialName) {
		String finalName = keyStream(initialName, "/*").filter(slashedPackageRenames::containsKey)
			.findFirst()
			.map(key -> {
				String name = slashedPackageRenames.get(key);
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
		return replacePackages(text, getPackageRenames());
	}

	/**
	 * Replace all embedded packages of specified text with replacement
	 * packages.
	 *
	 * @param text String embedding zero, one, or more package names.
	 * @param packageRenames map of names and replacement values
	 * @return The text with all embedded package names replaced. Null if no
	 *         replacements were performed.
	 */
	@Override
	public String replacePackages(String text, Map<String, String> packageRenames) {
		String initialText = text;

		for (Map.Entry<String, String> renameEntry : packageRenames.entrySet()) {
			String key = renameEntry.getKey();

			boolean matchPackageStem = containsWildcard(key);
			if (matchPackageStem) {
				key = stripWildcard(key);
			}

			final int keyLen = key.length();
			int textLimit = text.length() - keyLen;

			for (int matchEnd = 0; matchEnd <= textLimit;) {
				final int matchStart = text.indexOf(key, matchEnd);
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

	public static boolean matches(Pattern p, CharSequence input) {
		Matcher m = p.matcher(input);
		return m.matches();
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

	@Override
	public String replaceText(String inputName, String text) {
		Map<String, String> substitutions = getTextSubstitutions(inputName);
		if (substitutions == null) {
			throw new IllegalStateException(
				"Input [ " + inputName + " ] selected for TEXT transformation, but found no substitutions");
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

	//

	private final Map<String, String>	changedBinaryTypes;
	private final Set<String>			unchangedBinaryTypes;

	@Override
	public String transformConstantAsBinaryType(String inputConstant) {
		return transformConstantAsBinaryType(inputConstant, NO_SIMPLE_SUBSTITUTION);
	}

	@Override
	public String transformConstantAsBinaryType(String inputConstant, boolean allowSimpleSubstitution) {
		try {
			return transformBinaryType(inputConstant, allowSimpleSubstitution);
		} catch (Throwable th) {
			getLogger().trace("Failed to parse constant as resource reference [ {} ]", inputConstant, th);
			return null;
		}
	}

	@Override
	public String transformBinaryType(String inputName) {
		return transformBinaryType(inputName, NO_SIMPLE_SUBSTITUTION);
	}

	/**
	 * Modify a fully qualified type name according to the package rename table.
	 * Answer either the transformed type name, or, if the type name was not
	 * changed, a wrapped null.
	 *
	 * @param inputName A fully qualified type name which is to be transformed.
	 * @param allowSimpleSubstitution Control parameter: Should simple
	 *            substitutions be allowed.
	 * @return The transformed type name, or null if no changed was made.
	 */
	protected String transformBinaryType(String inputName, boolean allowSimpleSubstitution) {
		// System.out.println("Input type [ " + inputName + " ]");

		if (unchangedBinaryTypes.contains(inputName)) {
			// System.out.println("Unchanged (Prior)");
			return null;
		}

		String outputName = changedBinaryTypes.get(inputName);
		if (outputName != null) {
			// System.out.println("Change to [ " + outputName + " ] (Prior)");
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
				// System.out.println("Input package [ " + inputPackage + " ]");
				String outputPackage = replaceBinaryPackage(inputPackage);
				if (outputPackage != null) {
					// System.out.println("Output package [ " + outputPackage +
					// " ]");
					outputName = outputPackage + inputName.substring(lastSlashOffset);
				} else {
					// Leave outputName null.
				}
			} else {
				// Leave outputName with null;
			}
		}

		if ((outputName == null) && allowSimpleSubstitution) {
			outputName = replacePackages(inputName, slashedPackageRenames);
		}

		if (outputName == null) {
			unchangedBinaryTypes.add(inputName);
			// System.out.println("Unchanged");
		} else {
			changedBinaryTypes.put(inputName, outputName);
			// System.out.println("Change to [ " + outputName + " ]");
		}

		return outputName;
	}

	//

	@Override
	public String transformConstantAsDescriptor(String inputConstant) {
		return transformConstantAsDescriptor(inputConstant, NO_SIMPLE_SUBSTITUTION);
	}

	@Override
	public String transformConstantAsDescriptor(String inputConstant, boolean allowSimpleSubstitution) {
		try {
			return transformDescriptor(inputConstant, allowSimpleSubstitution);
		} catch (Throwable th) {
			getLogger().trace("Failed to parse constant as descriptor [ {} ]", inputConstant, th);
			return null;
		}
	}

	private final Set<String>			unchangedDescriptors;
	private final Map<String, String>	changedDescriptors;

	@Override
	public String transformDescriptor(String inputDescriptor) {
		return transformDescriptor(inputDescriptor, NO_SIMPLE_SUBSTITUTION);
	}

	@Override
	public String transformDescriptor(String inputDescriptor, boolean allowSimpleSubstitution) {
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
			String outputSignature = transform(inputSignature, SignatureType.METHOD);
			if (outputSignature != null) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else if ((c == '[') || ((c == 'L') && (inputDescriptor.charAt(inputDescriptor.length() - 1) == ';'))) {
			String inputSignature = inputDescriptor.replace('$', '.');
			String outputSignature = transform(inputSignature, SignatureType.FIELD);
			if (outputSignature != null) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else {
			// leave outputDescriptor null
		}

		if ((outputDescriptor == null) && allowSimpleSubstitution) {
			outputDescriptor = replacePackages(inputDescriptor);
		}

		if (outputDescriptor == null) {
			unchangedDescriptors.add(inputDescriptor);
		} else {
			changedDescriptors.put(inputDescriptor, outputDescriptor);
		}
		return outputDescriptor;
	}

	/**
	 * Cache of transformed signatures. A single unified mapping is used, even
	 * through there are three different types of signatures. The different
	 * types of signatures each has its own syntax, meaning, there are not equal
	 * values across signature types.
	 */

	private final Set<String>			unchangedSignatures;
	private final Map<String, String>	changedSignatures;

	/**
	 * Transform a class, field, or method signature. Answer a wrapped null if
	 * the signature is not changed by the transformation rules.
	 *
	 * @param input The signature value which is to be transformed.
	 * @param signatureType The type of the signature value.
	 * @return The transformed signature value. A wrapped null if no change was
	 *         made to the value.
	 */
	@Override
	public String transform(String input, SignatureType signatureType) {
		if (unchangedSignatures.contains(input)) {
			return null;
		}

		String output = changedSignatures.get(input);
		if (output != null) {
			return output;
		}

		if (signatureType == SignatureType.CLASS) {
			ClassSignature inputSignature = ClassSignature.of(input);
			ClassSignature outputSignature = transform(inputSignature);
			if (outputSignature != null) {
				output = outputSignature.toString();
			} else {
				// leave output null;
			}

		} else if (signatureType == SignatureType.FIELD) {
			FieldSignature inputSignature = FieldSignature.of(input);
			FieldSignature outputSignature = transform(inputSignature);
			if (outputSignature != null) {
				output = outputSignature.toString();
			} else {
				// leave output null;
			}

		} else if (signatureType == SignatureType.METHOD) {
			MethodSignature inputSignature = MethodSignature.of(input);
			MethodSignature outputSignature = transform(inputSignature);
			if (outputSignature != null) {
				output = outputSignature.toString();
			} else {
				// leave output null
			}

		} else {
			throw new IllegalArgumentException(
				"Signature [ " + input + " ] uses unknown type [ " + signatureType + " ]");
		}

		if (output == null) {
			unchangedSignatures.add(input);
		} else {
			changedSignatures.put(input, output);
		}

		return output;
	}

	@Override
	public ClassSignature transform(ClassSignature classSignature) {
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

	@Override
	public FieldSignature transform(FieldSignature fieldSignature) {
		ReferenceTypeSignature inputType = fieldSignature.type;
		ReferenceTypeSignature outputType = transform(inputType);

		if (outputType == null) {
			return null;
		} else {
			return new FieldSignature(outputType);
		}
	}

	@Override
	public MethodSignature transform(MethodSignature methodSignature) {
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

	@Override
	public Result transform(Result type) {
		if (type instanceof JavaTypeSignature) {
			return transform((JavaTypeSignature) type);
		} else {
			return null;
		}
	}

	@Override
	public ThrowsSignature transform(ThrowsSignature type) {
		if (type instanceof ClassTypeSignature) {
			return transform((ClassTypeSignature) type);
		} else {
			return null;
		}
	}

	@Override
	public ArrayTypeSignature transform(ArrayTypeSignature inputType) {
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

	@Override
	public TypeParameter transform(TypeParameter inputTypeParameter) {
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

	@Override
	public ClassTypeSignature transform(ClassTypeSignature inputType) {
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

	@Override
	public SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature) {
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

	@Override
	public TypeArgument transform(TypeArgument inputArgument) {
		ReferenceTypeSignature inputSignature = inputArgument.type;
		ReferenceTypeSignature outputSignature = transform(inputSignature);
		if (outputSignature == null) {
			return null;
		} else {
			return new TypeArgument(inputArgument.wildcard, outputSignature);
		}
	}

	@Override
	public JavaTypeSignature transform(JavaTypeSignature type) {
		if (type instanceof ReferenceTypeSignature) {
			return transform((ReferenceTypeSignature) type);
		} else {
			return null;
		}
	}

	@Override
	public ReferenceTypeSignature transform(ReferenceTypeSignature type) {
		if (type instanceof ClassTypeSignature) {
			return transform((ClassTypeSignature) type);

		} else if (type instanceof ArrayTypeSignature) {
			return transform((ArrayTypeSignature) type);

		} else {
			return null;
		}
	}
}
