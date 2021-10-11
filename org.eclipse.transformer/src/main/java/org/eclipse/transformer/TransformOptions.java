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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.impl.ActionImpl;
import org.eclipse.transformer.action.impl.BundleDataImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.CompositeActionImpl;
import org.eclipse.transformer.action.impl.DirectoryActionImpl;
import org.eclipse.transformer.action.impl.EarActionImpl;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.JarActionImpl;
import org.eclipse.transformer.action.impl.JavaActionImpl;
import org.eclipse.transformer.action.impl.ManifestActionImpl;
import org.eclipse.transformer.action.impl.NullActionImpl;
import org.eclipse.transformer.action.impl.PropertiesActionImpl;
import org.eclipse.transformer.action.impl.RarActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.action.impl.TextActionImpl;
import org.eclipse.transformer.action.impl.WarActionImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.log.TransformerLoggerFactory;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

import aQute.lib.utf8properties.UTF8Properties;

public class TransformOptions {
	/**
	 *
	 */
	private final Transformer transformer;

	/**
	 * @param transformer
	 */
	TransformOptions(Transformer transformer) {
		this.transformer = transformer;
	}

	public boolean							isVerbose;
	public boolean							isTerse;

	public Set<String>						includes;
	public Set<String>						excludes;

	public boolean							invert;
	public Map<String, String>				packageRenames;
	public Map<String, String>				packageVersions;
	public Map<String, Map<String, String>> specificPackageVersions;
	public Map<String, BundleData>			bundleUpdates;
	public Map<String, String>				masterSubstitutionRefs;
	public Map<String, Map<String, String>>	masterTextUpdates;
	// ( pattern -> ( initial-> final ) )

	public Map<String, String>				directStrings;

	public boolean							widenArchiveNesting;
	public CompositeActionImpl				rootAction;
	public ActionImpl						acceptedAction;

	public String							inputName;
	public String							inputPath;
	public File								inputFile;

	public boolean							allowOverwrite;

	public String							outputName;
	public String							outputPath;
	public File								outputFile;
	public Map<String, Map<String, String>> perClassConstantStrings;

	//

	public void setLogging() throws TransformException {
		this.transformer.logger = new TransformerLoggerFactory(this.transformer).createLogger(); // throws
																				// TransformException

		if (this.transformer.hasOption(AppOption.LOG_TERSE)) {
			isTerse = true;
		} else if (this.transformer.hasOption(AppOption.LOG_VERBOSE)) {
			isVerbose = true;
		}
	}

	protected void info(String message, Object... parms) {
		this.transformer.getLogger().info(message, parms);
	}

	protected void error(String message, Object... parms) {
		this.transformer.getLogger().error(message, parms);
	}

	protected void error(String message, Throwable th, Object... parms) {
		Logger useLogger = this.transformer.getLogger();
		if (useLogger.isErrorEnabled()) {
			message = String.format(message, parms);
			useLogger.error(message, th);
		}
	}

	//

	public String getInputFileName() {
		return inputName;
	}

	public String getOutputFileName() {
		return outputName;
	}

	private InputBufferImpl buffer;

	protected InputBufferImpl getBuffer() {
		if (buffer == null) {
			buffer = new InputBufferImpl();
		}
		return buffer;
	}

	/**
	 * Process the rules data.  Load and validate the data.
	 *
	 * @return True or false telling if the data was successfully loaded
	 *     and is usable.
	 *
	 * @throws Exception Thrown if an error occurred while loading or
	 *     validating the data.
	 */
	public boolean setRules() throws Exception {
		ImmediateRuleData[] immediateData = this.transformer.getImmediateData();
		if ( immediateData == null ) {
			return false;
		}

		Set<String> orphanedFinalPackages = new HashSet<String>();

		UTF8Properties selectionProperties = this.transformer.loadProperties(AppOption.RULES_SELECTIONS, null);
		UTF8Properties renameProperties = this.transformer.loadProperties(AppOption.RULES_RENAMES, orphanedFinalPackages);
		UTF8Properties versionProperties = this.transformer.loadProperties(AppOption.RULES_VERSIONS, null);
		UTF8Properties updateProperties = this.transformer.loadProperties(AppOption.RULES_BUNDLES, null);
		UTF8Properties directProperties = this.transformer.loadProperties(AppOption.RULES_DIRECT, null);
		UTF8Properties textMasterProperties = this.transformer.loadProperties(AppOption.RULES_MASTER_TEXT, null);
		UTF8Properties perClassConstantProperties = this.transformer.loadProperties(AppOption.RULES_PER_CLASS_CONSTANT, null);

		invert = this.transformer.hasOption(AppOption.INVERT);

		if ( !selectionProperties.isEmpty() ) {
			includes = new HashSet<>();
			excludes = new HashSet<>();
			TransformProperties.addSelections(includes, excludes, selectionProperties);
			this.transformer.dual_info("Selection rules are in use");
		} else {
			includes = null;
			excludes = null;
		}

		if ( !renameProperties.isEmpty() ) {
			Map<String, String> renames = TransformProperties.getPackageRenames(renameProperties);
			if ( invert ) {
				renames = TransformProperties.invert(renames);
			}
			packageRenames = renames;
			this.transformer.dual_info("Package renames are in use");
		} else {
			packageRenames = null;
		}

		if ( !versionProperties.isEmpty() ) {
			packageVersions = new HashMap<>( versionProperties.size() );
			specificPackageVersions = new HashMap<>();
			TransformProperties.setPackageVersions(versionProperties, packageVersions, specificPackageVersions);
			this.transformer.dual_info("Package versions will be updated");
		} else {
			packageVersions = null;
			specificPackageVersions = null;
		}

		if ( !updateProperties.isEmpty() ) {
			bundleUpdates = TransformProperties.getBundleUpdates(updateProperties);
			// throws IllegalArgumentException
			this.transformer.dual_info("Bundle identities will be updated");
		} else {
			bundleUpdates = null;
		}

		String masterTextRef;

		if ( !textMasterProperties.isEmpty() ) {
			masterTextRef = this.transformer.getOptionValue(AppOption.RULES_MASTER_TEXT, Transformer.DO_NORMALIZE);

			Map<String, String> substitutionRefs =
				TransformProperties.convertPropertiesToMap(textMasterProperties);
			// throws IllegalArgumentException

			Map<String, Map<String, String>> masterUpdates = new HashMap<>();
			for (Map.Entry<String, String> substitutionRefEntry : substitutionRefs.entrySet()) {
				String simpleNameSelector = substitutionRefEntry.getKey();
				String substitutionsRef = FileUtils.normalize(substitutionRefEntry.getValue());

				Map<String, String> substitutionsMap =
					loadSubstitutions(masterTextRef, simpleNameSelector, substitutionsRef);
				// throws URISyntaxException, IOException

				substitutionRefs.put(simpleNameSelector, substitutionsRef);
				masterUpdates.put(simpleNameSelector, substitutionsMap);
			}

			masterSubstitutionRefs = substitutionRefs;
			masterTextUpdates = masterUpdates;
			this.transformer.dual_info("Text files will be updated");

		} else {
			masterTextRef = null;
			masterTextUpdates = null;
		}

		if ( !directProperties.isEmpty() ) {
			directStrings = TransformProperties.getDirectStrings(directProperties);
			this.transformer.dual_info("Java direct string updates will be performed");
		} else {
			directStrings = null;
			this.transformer.dual_info("Java direct string updates will not be performed");
		}

		if ( !perClassConstantProperties.isEmpty() ) {
			String masterDirect = this.transformer.getOptionValue(AppOption.RULES_PER_CLASS_CONSTANT, Transformer.DO_NORMALIZE);

			Map<String, String> substitutionRefs =
				TransformProperties.convertPropertiesToMap(perClassConstantProperties);
			// throws IllegalArgumentException

			Map<String, Map<String, String>> masterUpdates = new HashMap<String, Map<String, String>>();
			for ( Map.Entry<String, String> substitutionRefEntry : substitutionRefs.entrySet() ) {
				String classSelector = substitutionRefEntry.getKey();
				String substitutionsRef = FileUtils.normalize(substitutionRefEntry.getValue());

				UTF8Properties substitutions = new UTF8Properties();
				if ( masterDirect == null ) {
					substitutions = this.transformer.loadInternalProperties("Substitions matching [ " + classSelector + " ]", substitutionsRef);
				}
				Map<String, String> substitutionsMap =
					TransformProperties.convertPropertiesToMap(substitutions);
				// throws IllegalArgumentException
				masterUpdates.put(classSelector, substitutionsMap);
			}

			perClassConstantStrings = masterUpdates;
			this.transformer.dual_info("Per class constant mapping files are enabled");

		} else {
			perClassConstantStrings = null;
			this.transformer.dual_info("Per class constant mapping files are not enabled");
		}

		processImmediateData(immediateData, masterTextRef, orphanedFinalPackages);

		// Delay reporting null property sets: These are assigned directly
		// and by immediate data.

		if ( includes == null ) {
			this.transformer.dual_info("All resources will be selected");
		}
		if ( packageRenames == null ) {
			this.transformer.dual_info("Packages will not be renamed");
		}
		if ( packageVersions == null ) {
			this.transformer.dual_info("Package versions will not be updated");
		}
		if ( bundleUpdates == null ) {
			this.transformer.dual_info("Bundle identities will not be updated");
		}
		if ( masterTextUpdates == null ) {
			this.transformer.dual_info("Text files will not be updated");
		}
		if ( directStrings == null ) {
			this.transformer.dual_info("Java direct string updates will not be performed");
		}
		if ( perClassConstantStrings == null ) {
			this.transformer.dual_info("Per class constant mapping files are not enabled");
		}

		return validateVersionUpdates(orphanedFinalPackages);
	}

	protected void processImmediateData(
		ImmediateRuleData[] immediateData, String masterTextRef,
		Set<String> orphanedFinalVersions)
		throws IOException, URISyntaxException {

		for ( ImmediateRuleData nextData : immediateData ) {
			switch ( nextData.target ) {
				case RULES_SELECTIONS:
					addImmediateSelection(nextData.key, nextData.value);
					break;
				case RULES_RENAMES:
					addImmediateRename(nextData.key, nextData.value, orphanedFinalVersions);
					break;
				case RULES_VERSIONS:
					addImmediateVersion(nextData.key, nextData.value);
					break;
				case RULES_BUNDLES:
					addImmediateBundleData(nextData.key, nextData.value);
					break;
				case RULES_DIRECT:
					addImmediateDirect(nextData.key, nextData.value);
					break;
				case RULES_MASTER_TEXT:
					addImmediateMasterText(masterTextRef, nextData.key, nextData.value);
					// throws IOException, URISyntaxException
					break;

				default:
					this.transformer.dual_error("Unrecognized immediate data target [ %s ]", nextData.target);
			}
		}
	}

	private void addImmediateSelection(String selection, @SuppressWarnings("unused") String ignored) {
		if ( includes == null ) {
			includes = new HashSet<>();
			excludes = new HashSet<>();
			this.transformer.dual_info("Selection rules use forced by immediate data");
		}

		TransformProperties.addSelection(includes, excludes, selection);
	}

	private void addImmediateRename(
		String initialPackageName, String finalPackageName,
		Set<String> orphanedFinalPackages) {

		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			this.transformer.dual_info("Package renames forced by immediate data.");
		}

		if ( invert ) {
			String initialHold = initialPackageName;
			initialPackageName = finalPackageName;
			finalPackageName = initialHold;
		}

		String oldFinalPackageName = packageRenames.put(initialPackageName, finalPackageName);

		this.transformer.processOrphan(
			"immediate rename data", "renameData",
			initialPackageName, oldFinalPackageName, finalPackageName,
			orphanedFinalPackages);

		this.transformer.logMerge(
			"immediate rename data", "rename data",
			initialPackageName, oldFinalPackageName, finalPackageName);
	}

	private void addImmediateVersion(String finalPackageName, String versionText) {
		if ( packageVersions == null ) {
			packageVersions = new HashMap<>();
			specificPackageVersions = new HashMap<>();
			this.transformer.dual_info("Package version updates forced by immediate data.");
		}
		TransformProperties.setPackageVersions(finalPackageName, versionText, packageVersions, specificPackageVersions);
	}

	private void addImmediateBundleData(String bundleId, String value) {
		if ( bundleUpdates == null ) {
			bundleUpdates = new HashMap<>();
			this.transformer.dual_info("Bundle identity updates forced by immediate data.");
		}
		BundleData newBundleData = new BundleDataImpl(value);
		BundleData oldBundleData = bundleUpdates.put(bundleId, newBundleData);
		if ( oldBundleData != null ) {
			this.transformer.logMerge("immediate bundle data", "bundle data", bundleId, oldBundleData.getPrintString(), newBundleData.getPrintString());
		}
	}

	private void addImmediateDirect(String initialText, String finalText) {
		if ( directStrings == null ) {
			directStrings = new HashMap<>();
			this.transformer.dual_info("Java direct string updates forced by immediate data");
		}

		String oldFinalText = directStrings.put(initialText, finalText);
		if ( oldFinalText != null ) {
			this.transformer.logMerge("immediate direct string data", "direct string data", initialText, oldFinalText, finalText);
		}
	}

	private Map<String, String> loadSubstitutions(String masterRef, String selector, String substitutionsRef)
		throws IOException, URISyntaxException {
		UTF8Properties substitutions;
		if ( masterRef == null ) {
			substitutions = this.transformer.loadInternalProperties(
				"Substitions matching [ " + selector + " ]", substitutionsRef);
			// throws IOException
		} else {
			String relativeSubstitutionsRef = this.transformer.relativize(substitutionsRef, masterRef);
			if ( !relativeSubstitutionsRef.equals(substitutionsRef) ) {
				this.transformer.dual_info("Adjusted substition reference from [ %s ] to [ %s ]",
					substitutionsRef, relativeSubstitutionsRef);
			}

			substitutions = this.transformer.loadExternalProperties(
				"Substitions matching [ " + selector + " ]", relativeSubstitutionsRef);
			// throws URISyntaxException, IOException
		}

		Map<String, String> substitutionsMap =
			TransformProperties.convertPropertiesToMap(substitutions);
		// throws IllegalArgumentException

		return substitutionsMap;
	}

	private void addImmediateMasterText(
		String masterTextRef, String simpleNameSelector, String substitutionsRef)
		throws IOException, URISyntaxException {

		if ( masterTextUpdates == null ) {
			masterTextUpdates = new HashMap<>();
			this.transformer.dual_info("Text files updates forced by immediate data.");
		}

		substitutionsRef = FileUtils.normalize(substitutionsRef);

		Map<String, String> substitutionsMap =
			loadSubstitutions(masterTextRef, simpleNameSelector, substitutionsRef);
		// throws URISyntaxException, IOException

		String oldSubstitutionsRef =
			masterSubstitutionRefs.put(simpleNameSelector, substitutionsRef);
		@SuppressWarnings("unused")
		Map<String, String> oldSubstitutionMap =
			masterTextUpdates.put(simpleNameSelector, substitutionsMap);

		if ( oldSubstitutionsRef != null ) {
			this.transformer.logMerge("immediate master text data", "master text data", simpleNameSelector, oldSubstitutionsRef,
				substitutionsRef);
		}
	}

	/**
	 * Validate package version updates.  Answer true or false, telling if
	 * the version updates are valid.
	 *
	 * That is, validate the generic and the specific package version updates.
	 * The version updates are valid if and only if each package version update
	 * uses a package name which is the final package name of a package rename.
	 *
	 * See {@link #validateVersionUpdates(Map, Set)}.
	 *
	 * @param orphanedFinalPackages Orphaned final packages.
	 *
	 * @return True or false, telling if the package version updates are
	 *     valid.
	 */
	protected boolean validateVersionUpdates(Set<String> orphanedFinalPackages) {
		if ( ((packageVersions == null) || packageVersions.isEmpty()) &&
			 ((specificPackageVersions == null) || specificPackageVersions.isEmpty()) ) {
			return true; // Nothing to validate
		}

		// Don't bother listing all of the missing package names if no
		// renames were specified.  A single error message is sufficient.

		if ( (packageRenames == null) || packageRenames.isEmpty() ) {
			this.transformer.dual_error("Package version updates were specified but no package renames were specified.");
			return false;
		}

		boolean missingFinalPackages = !validateVersionUpdates(packageVersions, orphanedFinalPackages);

		for ( Map.Entry<String, Map<String, String>> specificEntry : specificPackageVersions.entrySet() ) {
			String attributeName = specificEntry.getKey();
			Map<String, String> updatesForAttribute = specificEntry.getValue();
			if ( !validateVersionUpdates(updatesForAttribute, orphanedFinalPackages) ) {
				missingFinalPackages = true;
			}
		}

		Set<String> ignoredAttributes = null;
		for ( String attributeName : specificPackageVersions.keySet() ) {
			if ( !ManifestActionImpl.selectAttribute(attributeName) ) {
				if ( ignoredAttributes == null ) {
					ignoredAttributes = new HashSet<>();
				}
				ignoredAttributes.add(attributeName);
			}
		}
		if ( ignoredAttributes != null ) {
			this.transformer.dual_info("Warning: Ignoring unknown attributes " + ignoredAttributes.toString() + " used for specific package version updates.");
		}

		return !missingFinalPackages;
	}

	protected boolean validateVersionUpdates(Map<String, String> versionUpdates, Set<String> orphanedFinalPackages) {
		boolean isValid = true;
		for ( String finalPackage : versionUpdates.keySet() ) {
			// Keep looping even after a non-valid update is detected:
			// Emit error messages for all problem version updates.
			if ( !validateVersionUpdate(finalPackage, orphanedFinalPackages) ) {
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * Validate a package version update against the specified package renames.
	 * Answer true or false, telling if the update is valid.
	 *
	 * There are three cases:
	 *
	 * The version update uses a package which is the final package
	 * in a package rename rule.  The update is valid.
	 *
	 * The version update uses a package which has been orphaned by a
	 * package rename rule.  That is, the version update would be valid
	 * if the final package was not orphaned.  The update is considered
	 * valid.
	 *
	 * The version update uses a package which was not specified as the
	 * final package in a package rename rule.  The update is not valid.
	 *
	 * @param finalPackage The package which was used as the key to a
	 *     package version update.  This package must be used as the
	 *     final package of a package rename.
	 * @param orphanedFinalPackages Final packages which were orphaned.
	 *
	 * @return True or false, telling if the package version update is
	 *     valid.
	 */
	protected boolean validateVersionUpdate(String finalPackage, Set<String> orphanedFinalPackages) {
		if ( packageRenames.containsValue(finalPackage) ) {
			return true;
		}

		// This orphaned case is curious:
		//
		// False might be returned, because the version update will never be used.
		//
		// However, true is returned, since we want to allow overrides which create
		// orphans.

		if ( orphanedFinalPackages.contains(finalPackage) ) {
			this.transformer.dual_info("Package [ %s ] has a version update but was orphaned.", finalPackage);
			return true;
		}

		this.transformer.dual_error("Package [ %s ] has a version update but was not renamed.", finalPackage);
		return false;
	}

	protected String[] getRuleFileNames(AppOption ruleOption) {
		String[] rulesFileNames = this.transformer.getOptionValues(ruleOption, Transformer.DO_NORMALIZE);
		if (rulesFileNames != null) {
			return rulesFileNames;
		} else {
			String defaultReference = this.transformer.getDefaultReference(ruleOption);
			return ( (defaultReference == null) ? null : new String[] { defaultReference } );
		}
	}

	protected void logRules() {
		info("Includes:");
		if ((includes == null) || includes.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (String include : includes) {
				info("  [ " + include + " ]");
			}
		}

		info("Excludes:");
		if ((excludes == null) || excludes.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (String exclude : excludes) {
				info("  [ " + exclude + " ]");
			}
		}

		if (invert) {
			info("Package Renames: [ ** INVERTED ** ]");
		} else {
			info("Package Renames:");
		}

		if ((packageRenames == null) || packageRenames.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> renameEntry : packageRenames.entrySet()) {
				info("  [ " + renameEntry.getKey() + " ]: [ " + renameEntry.getValue() + " ]");
			}
		}

		info("Package Versions:");
		if ((packageVersions == null) || packageVersions.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> versionEntry : packageVersions.entrySet()) {
				info("  [ " + versionEntry.getKey() + " ]: [ " + versionEntry.getValue() + " ]");
			}
		}

		info("Bundle Updates:");
		if ((bundleUpdates == null) || bundleUpdates.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, BundleData> updateEntry : bundleUpdates.entrySet()) {
				BundleData updateData = updateEntry.getValue();

				info("  [ " + updateEntry.getKey() + " ]: [ " + updateData.getSymbolicName() + " ]");

				info("    [ Version ]: [ " + updateData.getVersion() + " ]");

				if (updateData.getAddName()) {
					info("    [ Name ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getName() + " ]");
				} else {
					info("    [ Name ]: [ " + updateData.getName() + " ]");
				}

				if (updateData.getAddDescription()) {
					info("    [ Description ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getDescription() + " ]");
				} else {
					info("    [ Description ]: [ " + updateData.getDescription() + " ]");
				}
			}
		}

		info("Java string substitutions:");
		if ((directStrings == null) || directStrings.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> directEntry : directStrings.entrySet()) {
				info("  [ " + directEntry.getKey() + " ]: [ " + directEntry.getValue() + "]");
			}
		}

		info("Text substitutions:");
		if ((masterTextUpdates == null) || masterTextUpdates.isEmpty()) {
			info("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, Map<String, String>> masterTextEntry : masterTextUpdates.entrySet()) {
				info("  Pattern [ " + masterTextEntry.getKey() + " ]");
				for (Map.Entry<String, String> substitution : masterTextEntry.getValue()
					.entrySet()) {
					info("    [ " + substitution.getKey() + " ]: [ " + substitution.getValue() + " ]");
				}
			}
		}
	}

	private SelectionRuleImpl selectionRules;

	protected SelectionRuleImpl getSelectionRule() {
		if (selectionRules == null) {
			selectionRules = new SelectionRuleImpl(this.transformer.logger, includes, excludes);
		}
		return selectionRules;
	}

	private SignatureRuleImpl signatureRules;

	protected SignatureRuleImpl getSignatureRule() {
		if (signatureRules == null) {
			signatureRules = new SignatureRuleImpl(
				this.transformer.logger,
				packageRenames, packageVersions, specificPackageVersions,
				bundleUpdates,
				masterTextUpdates, directStrings, perClassConstantStrings);
		}
		return signatureRules;
	}

	public boolean setInput() {
		String useInputName = this.transformer.getInputFileNameFromCommandLine();
		if (useInputName == null) {
			this.transformer.dual_error("No input file was specified");
			return false;
		}

		inputName = FileUtils.normalize(useInputName);
		inputFile = new File(inputName);
		inputPath = inputFile.getAbsolutePath();

		if (!inputFile.exists()) {
			this.transformer.dual_error("Input does not exist [ %s ] [ %s ]", inputName, inputPath);
			return false;
		}

		this.transformer.dual_info("Input     [ %s ]", inputName);
		this.transformer.dual_info("          [ %s ]", inputPath);
		return true;
	}

	public static final String OUTPUT_PREFIX = "output_";

	public boolean setOutput() {
		String useOutputName = this.transformer.getOutputFileNameFromCommandLine();

		boolean isExplicit = (useOutputName != null);

		if (isExplicit) {
			useOutputName = FileUtils.normalize(useOutputName);

		} else {
			int indexOfLastSlash = inputName.lastIndexOf('/');
			if (indexOfLastSlash == -1) {
				useOutputName = OUTPUT_PREFIX + inputName;
			} else {
				String inputPrefix = inputName.substring(0, indexOfLastSlash + 1);
				String inputSuffix = inputName.substring(indexOfLastSlash + 1);
				useOutputName = inputPrefix + OUTPUT_PREFIX + inputSuffix;
			}
		}

		File useOutputFile = new File(useOutputName);
		String useOutputPath = useOutputFile.getAbsolutePath();

		boolean putIntoDirectory = (inputFile.isFile() && useOutputFile.isDirectory());

		if (putIntoDirectory) {
			useOutputName = useOutputName + '/' + inputName;
			if (isVerbose) {
				this.transformer.dual_info("Output generated using input name and output directory [ %s ]", useOutputName);
			}

			useOutputFile = new File(useOutputName);
			useOutputPath = useOutputFile.getAbsolutePath();
		}

		String outputCase;
		if (isExplicit) {
			if (putIntoDirectory) {
				outputCase = "Explicit directory";
			} else {
				outputCase = "Explicit";
			}
		} else {
			if (putIntoDirectory) {
				outputCase = "Directory generated from input";
			} else {
				outputCase = "Generated from input";
			}
		}

		this.transformer.dual_info("Output    [ %s ] (%s)", useOutputName, outputCase);
		this.transformer.dual_info("          [ %s ]", useOutputPath);

		allowOverwrite = this.transformer.hasOption(AppOption.OVERWRITE);
		if (allowOverwrite) {
			this.transformer.dual_info("Overwrite of output is enabled");
		}

		if (useOutputFile.exists()) {
			if (allowOverwrite) {
				this.transformer.dual_info("Output exists and will be overwritten [ %s ]", useOutputPath);
			} else {
				this.transformer.dual_error("Output already exists [ %s ]", useOutputPath);
				return false;
			}
		} else {
			if (allowOverwrite) {
				if (isVerbose) {
					this.transformer.dual_info("Overwritten specified, but output [ %s ] does not exist", useOutputPath);
				}
			}
		}

		outputName = useOutputName;
		outputFile = useOutputFile;
		outputPath = useOutputPath;

		return true;
	}

	public void setActions() {
		if (this.transformer.hasOption(AppOption.WIDEN_ARCHIVE_NESTING)) {
			widenArchiveNesting = true;
			this.transformer.dual_info("Widened action nesting is enabled.");
		} else {
			widenArchiveNesting = false;
		}
	}

	public CompositeActionImpl getRootAction() {
		if (rootAction == null) {
			CompositeActionImpl useRootAction = new CompositeActionImpl(this.transformer.getLogger(), isTerse, isVerbose,
				getBuffer(), getSelectionRule(), getSignatureRule());

			DirectoryActionImpl directoryAction = useRootAction.addUsing(DirectoryActionImpl::new);

			ClassActionImpl classAction = useRootAction.addUsing(ClassActionImpl::new);
			JavaActionImpl javaAction = useRootAction.addUsing(JavaActionImpl::new);
			ServiceLoaderConfigActionImpl serviceConfigAction = useRootAction
				.addUsing(ServiceLoaderConfigActionImpl::new);
			ManifestActionImpl manifestAction = useRootAction.addUsing(ManifestActionImpl::newManifestAction);
			ManifestActionImpl featureAction = useRootAction.addUsing(ManifestActionImpl::newFeatureAction);
			PropertiesActionImpl propertiesAction = useRootAction.addUsing(PropertiesActionImpl::new);

			JarActionImpl jarAction = useRootAction.addUsing(JarActionImpl::new);
			WarActionImpl warAction = useRootAction.addUsing(WarActionImpl::new);
			RarActionImpl rarAction = useRootAction.addUsing(RarActionImpl::new);
			EarActionImpl earAction = useRootAction.addUsing(EarActionImpl::new);

			TextActionImpl textAction = useRootAction.addUsing(TextActionImpl::new);
			// XmlActionImpl xmlAction =
			// useRootAction.addUsing( XmlActionImpl::new );

			ZipActionImpl zipAction = useRootAction.addUsing(ZipActionImpl::new);

			NullActionImpl nullAction = useRootAction.addUsing(NullActionImpl::new);

			// Directory actions know about all actions except for directory
			// actions.

			directoryAction.addAction(classAction);
			directoryAction.addAction(javaAction);
			directoryAction.addAction(serviceConfigAction);
			directoryAction.addAction(manifestAction);
			directoryAction.addAction(featureAction);
			directoryAction.addAction(zipAction);
			directoryAction.addAction(jarAction);
			directoryAction.addAction(warAction);
			directoryAction.addAction(rarAction);
			directoryAction.addAction(earAction);
			directoryAction.addAction(textAction);

			// Container actions nest per usual JavaEE nesting rules.
			// That is, EAR can contain JAR, WAR, and RAR,
			// WAR can container JAR, and RAR can contain JAR.

			jarAction.addAction(classAction);
			jarAction.addAction(javaAction);
			jarAction.addAction(serviceConfigAction);
			jarAction.addAction(manifestAction);
			jarAction.addAction(featureAction);
			jarAction.addAction(textAction);
			jarAction.addAction(propertiesAction);

			warAction.addAction(classAction);
			warAction.addAction(javaAction);
			warAction.addAction(serviceConfigAction);
			warAction.addAction(manifestAction);
			warAction.addAction(featureAction);
			warAction.addAction(jarAction);
			warAction.addAction(textAction);

			rarAction.addAction(classAction);
			rarAction.addAction(javaAction);
			rarAction.addAction(serviceConfigAction);
			rarAction.addAction(manifestAction);
			rarAction.addAction(featureAction);
			rarAction.addAction(jarAction);
			rarAction.addAction(textAction);

			earAction.addAction(manifestAction);
			earAction.addAction(jarAction);
			earAction.addAction(warAction);
			earAction.addAction(rarAction);
			earAction.addAction(textAction);

			zipAction.addAction(classAction);
			zipAction.addAction(javaAction);
			zipAction.addAction(serviceConfigAction);
			zipAction.addAction(manifestAction);
			zipAction.addAction(featureAction);
			zipAction.addAction(jarAction);
			zipAction.addAction(warAction);
			zipAction.addAction(rarAction);
			zipAction.addAction(earAction);
			zipAction.addAction(textAction);

			// On occasion, the JavaEE nesting rules are too
			// restrictive. Allow a slight widening of the
			// usual nesting.

			if (widenArchiveNesting) {
				jarAction.addAction(jarAction);
				jarAction.addAction(zipAction);

				rarAction.addAction(zipAction);
				warAction.addAction(zipAction);
				earAction.addAction(zipAction);

				zipAction.addAction(zipAction);
			}

			// Null actions must be added last: The null
			// is always selected, which prevents selection of
			// any action added after the null action.

			directoryAction.addAction(nullAction);
			jarAction.addAction(nullAction);
			warAction.addAction(nullAction);
			rarAction.addAction(nullAction);
			earAction.addAction(nullAction);
			zipAction.addAction(nullAction);

			rootAction = useRootAction;
		}

		return rootAction;
	}

	public boolean acceptAction() {
		String actionName = this.transformer.getOptionValue(AppOption.FILE_TYPE);
		if (actionName != null) {
			for (ActionImpl action : getRootAction().getActions()) {
				if (action.getActionType()
					.matches(actionName)) {
					this.transformer.dual_info("Forced action [ %s ] [ %s ]", actionName, action.getName());
					acceptedAction = action;
					return true;
				}
			}
			this.transformer.dual_error("No match for forced action [ %s ]", actionName);
			return false;

		} else {
			acceptedAction = getRootAction().acceptAction(inputName, inputFile);
			if (acceptedAction == null) {
				this.transformer.dual_error("No action selected for input [ %s ]", inputName);
				return false;
			} else {
				this.transformer.dual_info("Action selected for input [ %s ]: %s", inputName, acceptedAction.getName());
				return true;
			}
		}
	}

	public void transform() throws TransformException {

		acceptedAction.apply(inputName, inputFile, outputFile);

		if (isTerse) {
			if (!this.transformer.toSysOut && !this.transformer.toSysErr) {
				acceptedAction.getLastActiveChanges()
					.displayTerse(this.transformer.getSystemOut(), inputPath, outputPath);
			}
			acceptedAction.getLastActiveChanges()
				.displayTerse(this.transformer.getLogger(), inputPath, outputPath);
		} else if (isVerbose) {
			if (!this.transformer.toSysOut && !this.transformer.toSysErr) {
				acceptedAction.getLastActiveChanges()
					.displayVerbose(this.transformer.getSystemOut(), inputPath, outputPath);
			}
			acceptedAction.getLastActiveChanges()
				.displayVerbose(this.transformer.getLogger(), inputPath, outputPath);
		} else {
			if (!this.transformer.toSysOut && !this.transformer.toSysErr) {
				acceptedAction.getLastActiveChanges()
					.display(this.transformer.getSystemOut(), inputPath, outputPath);
			}
			acceptedAction.getLastActiveChanges()
				.display(this.transformer.getLogger(), inputPath, outputPath);
		}
	}

	public Changes getLastActiveChanges() {
		if (acceptedAction != null) {
			return acceptedAction.getLastActiveChanges();
		}
		return null;
	}
}
