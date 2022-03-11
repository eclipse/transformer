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

import static aQute.bnd.exceptions.BiFunctionWithException.asBiFunction;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.CompositeAction;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.action.impl.BundleDataImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.CompositeActionImpl;
import org.eclipse.transformer.action.impl.ContainerActionImpl;
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
import org.eclipse.transformer.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import aQute.lib.io.IO;
import aQute.libg.uri.URIUtil;

public class Transformer {
	public enum ResultCode {
		SUCCESS_RC("Success"),
		ARGS_ERROR_RC("Argument Error"),
		RULES_ERROR_RC("Rules Error"),
		TRANSFORM_ERROR_RC("Transform Error"),
		FILE_TYPE_ERROR_RC("File Type Error");

		private final String description;

		ResultCode(String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	/**
	 *
	 */
	public static final Marker		consoleMarker	= MarkerFactory.getMarker("console");
	private final Logger			logger;
	private final TransformOptions options;

	/**
	 * @param options
	 */
	public Transformer(TransformOptions options) {
		this(LoggerFactory.getLogger(Transformer.class), options);
	}

	/**
	 * @param logger
	 * @param options
	 */
	public Transformer(Logger logger, TransformOptions options) {
		this.logger = requireNonNull(logger);
		this.options = requireNonNull(options);
	}

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
	public CompositeAction					rootAction;
	public Action							acceptedAction;

	public String							inputName;
	public String							inputPath;
	public File								inputFile;

	public boolean							allowOverwrite;

	public String							outputName;
	public String							outputPath;
	public File								outputFile;
	public Map<String, Map<String, String>> perClassConstantStrings;

	//

	public Logger getLogger() {
		return logger;
	}

	private void logMerge(String sourceName, String sinkName, Object key, Object oldValue, Object newValue) {
		if (oldValue != null) {
			getLogger().debug(consoleMarker,
				"Merge of [ {} ] into [ {} ], key [ {} ] replaces value [ {} ] with [ {} ]", sourceName, sinkName, key,
				oldValue, newValue);
		}
	}

	public ResultCode run() {
		if (!setInput()) {
			return ResultCode.TRANSFORM_ERROR_RC;
		}

		if (!setOutput()) {
			return ResultCode.TRANSFORM_ERROR_RC;
		}

		boolean loadedRules;
		try {
			loadedRules = setRules(getImmediateData());
		} catch (Exception e) {
			getLogger().error(consoleMarker, "Exception loading rules:", e);
			return ResultCode.RULES_ERROR_RC;
		}
		if (!loadedRules) {
			getLogger().error(consoleMarker, "Transformation rules cannot be used");
			return ResultCode.RULES_ERROR_RC;
		}
		logRules();

		setActions();

		if (!acceptAction()) {
			getLogger().error(consoleMarker, "No action selected");
			return ResultCode.FILE_TYPE_ERROR_RC;
		}

		transform();

		return ResultCode.SUCCESS_RC;
	}

	//

	public String getInputFileName() {
		return inputName;
	}

	public String getOutputFileName() {
		return outputName;
	}

	private final InputBuffer buffer = new InputBufferImpl();

	protected InputBuffer getBuffer() {
		return buffer;
	}

	public static final AppOption[] TARGETABLE_RULES = new AppOption[] {
		AppOption.RULES_SELECTIONS,

		AppOption.RULES_RENAMES, AppOption.RULES_VERSIONS, AppOption.RULES_DIRECT, AppOption.RULES_PER_CLASS_CONSTANT,

		AppOption.RULES_BUNDLES, AppOption.RULES_MASTER_TEXT
	};

	public AppOption getTargetOption(String targetText) {
		for (AppOption appOption : TARGETABLE_RULES) {
			if (targetText.equals(appOption.getShortTag())) {
				return appOption;
			}
		}
		return null;
	}

	/**
	 * Fetch all immediate option data from the command line. Organize the data
	 * into separate objects, one object per specific immediate option on the
	 * command line.
	 *
	 * @return Grouped immediate option data from the command line.
	 */
	public ImmediateRuleData[] getImmediateData() {
		if (!options.hasOption(AppOption.RULES_IMMEDIATE_DATA)) {
			return new ImmediateRuleData[] {
				// EMPTY
			};
		}

		List<String> immediateArgs = options.getOptionValues(AppOption.RULES_IMMEDIATE_DATA);

		if ((immediateArgs.size() % 3) != 0) {
			getLogger().error(consoleMarker, "Incorrect number of arguments to option [ {} ]",
				AppOption.RULES_IMMEDIATE_DATA.getShortTag());
			return null;
		}

		int argCount = immediateArgs.size() / 3;

		ImmediateRuleData[] immediateData = new ImmediateRuleData[argCount];

		for (int argNo = 0; argNo < argCount; argNo++) {
			int baseNo = argNo * 3;
			String targetText = immediateArgs.get(baseNo);
			String key = immediateArgs.get(baseNo + 1);
			String value = immediateArgs.get(baseNo + 2);

			getLogger().info(consoleMarker, "Immediate rule data specified; target [ {} ], key [ {} ], value [ {} ]",
				targetText, key, value);

			AppOption target = getTargetOption(targetText);
			if (target == null) {
				getLogger().error(consoleMarker, "Immediate rules target [ {} ] is not valid.", targetText);
				return null;
			}

			immediateData[argNo] = new ImmediateRuleData(target, key, value);
		}

		return immediateData;
	}

	/**
	 * Process the rules data. Load and validate the data.
	 *
	 * @param immediateData Immediate rules data.
	 * @return True or false telling if the data was successfully loaded and is
	 *         usable.
	 * @throws Exception Thrown if an error occurred while loading or validating
	 *             the data.
	 */
	public boolean setRules(ImmediateRuleData[] immediateData) throws Exception {
		if ( immediateData == null ) {
			return false;
		}

		Set<String> orphanedFinalPackages = new HashSet<>();

		Properties selectionProperties = loadProperties(AppOption.RULES_SELECTIONS, null);
		Properties renameProperties = loadProperties(AppOption.RULES_RENAMES, orphanedFinalPackages);
		Properties versionProperties = loadProperties(AppOption.RULES_VERSIONS, null);
		Properties updateProperties = loadProperties(AppOption.RULES_BUNDLES, null);
		Properties directProperties = loadProperties(AppOption.RULES_DIRECT, null);
		Properties textMasterProperties = loadProperties(AppOption.RULES_MASTER_TEXT, null);
		Properties perClassConstantProperties = loadProperties(AppOption.RULES_PER_CLASS_CONSTANT,
			null);

		invert = options.hasOption(AppOption.INVERT);

		if ( !selectionProperties.isEmpty() ) {
			includes = new HashSet<>();
			excludes = new HashSet<>();
			TransformProperties.addSelections(includes, excludes, selectionProperties);
			getLogger().info(consoleMarker, "Selection rules are in use");
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
			getLogger().info(consoleMarker, "Package renames are in use");
		} else {
			packageRenames = null;
		}

		if ( !versionProperties.isEmpty() ) {
			packageVersions = new HashMap<>( versionProperties.size() );
			specificPackageVersions = new HashMap<>();
			TransformProperties.setPackageVersions(versionProperties, packageVersions, specificPackageVersions);
			getLogger().info(consoleMarker, "Package versions will be updated");
		} else {
			packageVersions = null;
			specificPackageVersions = null;
		}

		if ( !updateProperties.isEmpty() ) {
			bundleUpdates = TransformProperties.getBundleUpdates(updateProperties);
			getLogger().info(consoleMarker, "Bundle identities will be updated");
		} else {
			bundleUpdates = null;
		}

		String masterTextRef;

		if ( !textMasterProperties.isEmpty() ) {
			masterTextRef = options.normalize(options.getOptionValue(AppOption.RULES_MASTER_TEXT));

			Map<String, String> substitutionRefs =
				TransformProperties.convertPropertiesToMap(textMasterProperties);

			Map<String, Map<String, String>> masterUpdates = new HashMap<>();
			for (Map.Entry<String, String> substitutionRefEntry : substitutionRefs.entrySet()) {
				String simpleNameSelector = substitutionRefEntry.getKey();
				String substitutionsRef = options.normalize(substitutionRefEntry.getValue());

				Map<String, String> substitutionsMap =
					loadSubstitutions(masterTextRef, simpleNameSelector, substitutionsRef);

				substitutionRefs.put(simpleNameSelector, substitutionsRef);
				masterUpdates.put(simpleNameSelector, substitutionsMap);
			}

			masterSubstitutionRefs = substitutionRefs;
			masterTextUpdates = masterUpdates;
			getLogger().info(consoleMarker, "Text files will be updated");

		} else {
			masterTextRef = null;
			masterTextUpdates = null;
		}

		if ( !directProperties.isEmpty() ) {
			directStrings = TransformProperties.getDirectStrings(directProperties);
			getLogger().info(consoleMarker, "Java direct string updates will be performed");
		} else {
			directStrings = null;
			getLogger().debug(consoleMarker, "Java direct string updates will not be performed");
		}

		if ( !perClassConstantProperties.isEmpty() ) {
			String masterDirect = options.normalize(options.getOptionValue(AppOption.RULES_PER_CLASS_CONSTANT));

			Map<String, String> substitutionRefs =
				TransformProperties.convertPropertiesToMap(perClassConstantProperties);

			Map<String, Map<String, String>> masterUpdates = new HashMap<String, Map<String, String>>();
			for ( Map.Entry<String, String> substitutionRefEntry : substitutionRefs.entrySet() ) {
				String classSelector = substitutionRefEntry.getKey();
				String substitutionsRef = options.normalize(substitutionRefEntry.getValue());

				Properties substitutions = PropertiesUtils.createProperties();
				if ( masterDirect == null ) {
					substitutions = loadInternalProperties("Substitutions matching [ " + classSelector + " ]",
						substitutionsRef);
				}
				Map<String, String> substitutionsMap =
					TransformProperties.convertPropertiesToMap(substitutions);
				masterUpdates.put(classSelector, substitutionsMap);
			}

			perClassConstantStrings = masterUpdates;
			getLogger().info(consoleMarker, "Per class constant mapping files are enabled");

		} else {
			perClassConstantStrings = null;
			getLogger().debug(consoleMarker, "Per class constant mapping files are not enabled");
		}

		processImmediateData(immediateData, masterTextRef, orphanedFinalPackages);

		// Delay reporting null property sets: These are assigned directly
		// and by immediate data.

		if ( includes == null ) {
			getLogger().info(consoleMarker, "All resources will be selected");
		}
		if ( packageRenames == null ) {
			getLogger().debug(consoleMarker, "Packages will not be renamed");
		}
		if ( packageVersions == null ) {
			getLogger().debug(consoleMarker, "Package versions will not be updated");
		}
		if ( bundleUpdates == null ) {
			getLogger().debug(consoleMarker, "Bundle identities will not be updated");
		}
		if ( masterTextUpdates == null ) {
			getLogger().debug(consoleMarker, "Text files will not be updated");
		}
		if ( directStrings == null ) {
			getLogger().debug(consoleMarker, "Java direct string updates will not be performed");
		}
		if ( perClassConstantStrings == null ) {
			getLogger().debug(consoleMarker, "Per class constant mapping files are not enabled");
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
					break;

				default:
					getLogger().error(consoleMarker, "Unrecognized immediate data target [ {} ]", nextData.target);
			}
		}
	}

	private void addImmediateSelection(String selection, @SuppressWarnings("unused") String ignored) {
		if ( includes == null ) {
			includes = new HashSet<>();
			excludes = new HashSet<>();
			getLogger().info(consoleMarker, "Selection rules use forced by immediate data");
		}

		TransformProperties.addSelection(includes, excludes, selection);
	}

	private void addImmediateRename(
		String initialPackageName, String finalPackageName,
		Set<String> orphanedFinalPackages) {

		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			getLogger().info(consoleMarker, "Package renames forced by immediate data.");
		}

		if ( invert ) {
			String initialHold = initialPackageName;
			initialPackageName = finalPackageName;
			finalPackageName = initialHold;
		}

		String oldFinalPackageName = packageRenames.put(initialPackageName, finalPackageName);

		processOrphan(
			"immediate rename data", "renameData",
			initialPackageName, oldFinalPackageName, finalPackageName,
			orphanedFinalPackages);

		logMerge(
			"immediate rename data", "rename data",
			initialPackageName, oldFinalPackageName, finalPackageName);
	}

	private void addImmediateVersion(String finalPackageName, String versionText) {
		if ( packageVersions == null ) {
			packageVersions = new HashMap<>();
			specificPackageVersions = new HashMap<>();
			getLogger().info(consoleMarker, "Package version updates forced by immediate data.");
		}
		TransformProperties.setPackageVersions(finalPackageName, versionText, packageVersions, specificPackageVersions);
	}

	private void addImmediateBundleData(String bundleId, String value) {
		if ( bundleUpdates == null ) {
			bundleUpdates = new HashMap<>();
			getLogger().info(consoleMarker, "Bundle identity updates forced by immediate data.");
		}
		BundleData newBundleData = new BundleDataImpl(value);
		BundleData oldBundleData = bundleUpdates.put(bundleId, newBundleData);
		if ( oldBundleData != null ) {
			logMerge("immediate bundle data", "bundle data", bundleId, oldBundleData.getPrintString(),
				newBundleData.getPrintString());
		}
	}

	private void addImmediateDirect(String initialText, String finalText) {
		if ( directStrings == null ) {
			directStrings = new HashMap<>();
			getLogger().info(consoleMarker, "Java direct string updates forced by immediate data");
		}

		String oldFinalText = directStrings.put(initialText, finalText);
		if ( oldFinalText != null ) {
			logMerge("immediate direct string data", "direct string data", initialText, oldFinalText,
				finalText);
		}
	}

	//

	/**
	 * Load properties for the specified rule option. Answer an empty collection
	 * if the rule option was not provided. Options loading tries
	 * {@link TransformOptions#getOptionValue(AppOption)} then tries
	 * {@link TransformOptions#getDefaultValue(AppOption)}. An empty
	 * collection is returned when neither is available. Orphaned values are
	 *
	 * @param ruleOption The option for which to load properties.
	 * @param orphanedValues Values which have been orphaned by merges.
	 * @return Properties loaded using the reference set for the option.
	 * @throws IOException Thrown if the load failed.
	 * @throws URISyntaxException Thrown if the load failed because a non-valid
	 *             URI was specified.
	 */
	public Properties loadProperties(AppOption ruleOption, Set<String> orphanedValues)
		throws IOException, URISyntaxException {
		List<String> rulesReferences = options.normalize(options.getOptionValues(ruleOption));

		if (rulesReferences == null) {
			String rulesReference = options.getDefaultValue(ruleOption);
			if (rulesReference == null) {
				getLogger().debug(consoleMarker, "Skipping option [ {} ]", ruleOption);
				return PropertiesUtils.createProperties();
			} else {
				return loadInternalProperties(ruleOption, rulesReference);
			}
		} else if (rulesReferences.size() == 1) {
			return loadExternalProperties(ruleOption, rulesReferences.get(0));
		} else {
			String baseReference = rulesReferences.get(0);
			Properties mergedProperties = rulesReferences.stream()
				.reduce(null, asBiFunction((props, ref) -> {
					Properties p = loadExternalProperties(ruleOption, ref);
					if (props == null) {
						return p;
					}
					merge(baseReference, props, ref, p, orphanedValues);
					return props;
				}), (props1, props2) -> {
					props1.putAll(props2);
					return props1;
				});

			return mergedProperties;
		}
	}

	String relativize(String relativeRef, String baseRef) {
		Path basePath = Paths.get(baseRef);
		Path siblingPath = basePath.resolveSibling(relativeRef);
		return siblingPath.toString();
	}

	/*
	 * Results of 'relativize': Base reference [ c:\dev\rules\textMaster ]
	 * Sibling reference [ sibling1 ] Base path [ c:\dev\rules\textMaster ]
	 * Sibling path [ c:\dev\rules\sibling1 ] Base reference [ c:\textMaster ]
	 * Sibling reference [ sibling1 ] Base path [ c:\textMaster ] Sibling path [
	 * c:\sibling1 ] Base reference [ \textMaster ] Sibling reference [ sibling1
	 * ] Base path [ \textMaster ] Sibling path [ \sibling1 ] Base reference [
	 * textMaster ] Sibling reference [ sibling1 ] Base path [ textMaster ]
	 * Sibling path [ sibling1 ]
	 */

	protected Properties loadInternalProperties(AppOption ruleOption, String resourceRef) throws IOException {
		return loadInternalProperties(ruleOption.toString(), resourceRef);
	}

	protected Properties loadInternalProperties(String ruleOption, String resourceRef) throws IOException {
		// getLogger().info(consoleMarker, "Using internal [ {} ]: [ {} ]",
		// ruleOption, resourceRef);
		URL rulesUrl = options.getRuleLoader()
			.apply(resourceRef);
		if (rulesUrl == null) {
			getLogger().debug(consoleMarker, "Internal [ {} ] were not found [ {} ]", ruleOption, resourceRef);
			throw new IOException("Resource [ " + resourceRef + " ] not found on [ " + options.getRuleLoader() + " ]");
		} else {
			getLogger().info(consoleMarker, "Internal [ {}] URL [ {} ]", ruleOption, rulesUrl);
		}
		return PropertiesUtils.loadProperties(rulesUrl);
	}

	protected Properties loadExternalProperties(AppOption ruleOption, String resourceRef)
		throws URISyntaxException, IOException {

		return loadExternalProperties(ruleOption.toString(), resourceRef);
	}

	public Properties loadExternalProperties(String referenceName, String externalReference)
		throws URISyntaxException, IOException {

		return loadExternalProperties(referenceName, externalReference, IO.work);
	}

	protected Properties loadExternalProperties(String referenceName, String externalReference, File relativeHome)
		throws URISyntaxException, IOException {

		URI relativeHomeUri = relativeHome.toURI();
		URL rulesUrl = URIUtil.resolve(relativeHomeUri, externalReference)
			.toURL();
		getLogger().info(consoleMarker, "External [ {} ] URL [ {} ]", referenceName, rulesUrl);

		return PropertiesUtils.loadProperties(rulesUrl);
	}

	protected void merge(String sinkName, Properties sink, String sourceName, Properties source,
		Set<String> orphanedValues) {

		for (Map.Entry<Object, Object> sourceEntry : source.entrySet()) {
			String key = (String) sourceEntry.getKey();
			String newValue = (String) sourceEntry.getValue();
			String oldValue = (String) sink.put(key, newValue);

			if (orphanedValues != null) {
				processOrphan(sourceName, sinkName, key, oldValue, newValue, orphanedValues);
			}

			logMerge(sourceName, sinkName, key, oldValue, newValue);
		}
	}

	/**
	 * Detect orphaned and un-orphaned property assignments. An orphaned
	 * property assignment occurs when a new property assignment changes the
	 * assigned property value. An orphaned value become un-orphaned if a
	 * property assignment has that value.
	 *
	 * @param sourceName A name associated with the properties which were added.
	 *            Used for logging.
	 * @param sinkName A name associated with the properties into which the
	 *            source properties were added. Used for logging.
	 * @param key The key which was overridden by the source properties.
	 * @param oldValue The value previously assigned to the key in the sink
	 *            properties.
	 * @param newValue The value newly assigned to the key in the sink
	 *            properties.
	 * @param orphans Accumulated sink property values which were orphaned.
	 */
	protected void processOrphan(String sourceName, String sinkName, String key, String oldValue, String newValue,
		Set<String> orphans) {

		if ((oldValue != null) && oldValue.equals(newValue)) {
			return; // Nothing to do: The old and new assignments are the same.
		}

		if (oldValue != null) {
			getLogger().debug(consoleMarker, "Merge of [ {} ] into [ {} ], key [ {} ] orphans [ {} ]", sourceName,
				sinkName, key, oldValue);
			orphans.add(oldValue);
		}

		if (orphans.remove(newValue)) {
			getLogger().debug(consoleMarker, "Merge of [ {} ] into [ {} ], key [ {} ] un-orphans [ {} ]", sourceName,
				sinkName, key, newValue);
		}
	}

	private Map<String, String> loadSubstitutions(String masterRef, String selector, String substitutionsRef)
		throws IOException, URISyntaxException {
		Properties substitutions;
		if ( masterRef == null ) {
			substitutions = loadInternalProperties(
				"Substitutions matching [ " + selector + " ]", substitutionsRef);
		} else {
			String relativeSubstitutionsRef = relativize(substitutionsRef, masterRef);
			if ( !relativeSubstitutionsRef.equals(substitutionsRef) ) {
				getLogger().debug(consoleMarker, "Adjusted substition reference from [ {} ] to [ {} ]",
					substitutionsRef, relativeSubstitutionsRef);
			}

			substitutions = loadExternalProperties(
				"Substitutions matching [ " + selector + " ]", relativeSubstitutionsRef);
		}

		Map<String, String> substitutionsMap =
			TransformProperties.convertPropertiesToMap(substitutions);

		return substitutionsMap;
	}

	private void addImmediateMasterText(
		String masterTextRef, String simpleNameSelector, String substitutionsRef)
		throws IOException, URISyntaxException {

		if ( masterTextUpdates == null ) {
			masterTextUpdates = new HashMap<>();
			getLogger().info(consoleMarker, "Text files updates forced by immediate data.");
		}

		substitutionsRef = options.normalize(substitutionsRef);

		Map<String, String> substitutionsMap =
			loadSubstitutions(masterTextRef, simpleNameSelector, substitutionsRef);

		String oldSubstitutionsRef =
			masterSubstitutionRefs.put(simpleNameSelector, substitutionsRef);
		@SuppressWarnings("unused")
		Map<String, String> oldSubstitutionMap =
			masterTextUpdates.put(simpleNameSelector, substitutionsMap);

		if ( oldSubstitutionsRef != null ) {
			logMerge("immediate master text data", "master text data", simpleNameSelector,
				oldSubstitutionsRef,
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
			getLogger().error(consoleMarker,
				"Package version updates were specified but no package renames were specified.");
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
			getLogger().info(consoleMarker,
				"Warning: Ignoring unknown attributes {} used for specific package version updates.",
				ignoredAttributes);
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
			getLogger().info(consoleMarker, "Package [ {} ] has a version update but was orphaned.", finalPackage);
			return true;
		}

		getLogger().error(consoleMarker, "Package [ {} ] has a version update but was not renamed.", finalPackage);
		return false;
	}

	// protected List<String> getRuleFileNames(AppOption ruleOption) {
	// List<String> rulesFileNames =
	// options.normalize(options.getOptionValues(ruleOption));
	// if (rulesFileNames != null) {
	// return rulesFileNames;
	// } else {
	// String defaultReference = options.getDefaultReference(ruleOption);
	// return ( (defaultReference == null) ? null : new String[] {
	// defaultReference } );
	// }
	// }

	public void logRules() {
		if (!getLogger().isDebugEnabled()) {
			return;
		}
		getLogger().debug("Includes:");
		if ((includes == null) || includes.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (String include : includes) {
				getLogger().debug("  [ {} ]", include);
			}
		}

		getLogger().debug("Excludes:");
		if ((excludes == null) || excludes.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (String exclude : excludes) {
				getLogger().debug("  [ {} ]", exclude);
			}
		}

		if (invert) {
			getLogger().debug("Package Renames: [ ** INVERTED ** ]");
		} else {
			getLogger().debug("Package Renames:");
		}

		if ((packageRenames == null) || packageRenames.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> renameEntry : packageRenames.entrySet()) {
				getLogger().debug("  [ {} ]: [ {} ]", renameEntry.getKey(), renameEntry.getValue());
			}
		}

		getLogger().debug("Package Versions:");
		if ((packageVersions == null) || packageVersions.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> versionEntry : packageVersions.entrySet()) {
				getLogger().debug("  [ {} ]: [ {} ]", versionEntry.getKey(), versionEntry.getValue());
			}
		}

		getLogger().debug("Bundle Updates:");
		if ((bundleUpdates == null) || bundleUpdates.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, BundleData> updateEntry : bundleUpdates.entrySet()) {
				BundleData updateData = updateEntry.getValue();

				getLogger().debug("  [ {} ]: [ {} ]", updateEntry.getKey(), updateData.getSymbolicName());

				getLogger().debug("    [ Version ]: [ {} ]", updateData.getVersion());

				if (updateData.getAddName()) {
					getLogger().debug("    [ Name ]: [ " + BundleData.ADDITIVE_CHAR + "{} ]", updateData.getName());
				} else {
					getLogger().debug("    [ Name ]: [ {} ]", updateData.getName());
				}

				if (updateData.getAddDescription()) {
					getLogger().debug(
						"    [ Description ]: [ " + BundleData.ADDITIVE_CHAR + "{} ]", updateData.getDescription());
				} else {
					getLogger().debug("    [ Description ]: [ {} ]", updateData.getDescription());
				}
			}
		}

		getLogger().debug("Java string substitutions:");
		if ((directStrings == null) || directStrings.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, String> directEntry : directStrings.entrySet()) {
				getLogger().debug("  [ {} ]: [ {} ]", directEntry.getKey(), directEntry.getValue());
			}
		}

		getLogger().debug("Text substitutions:");
		if ((masterTextUpdates == null) || masterTextUpdates.isEmpty()) {
			getLogger().debug("  [ ** NONE ** ]");
		} else {
			for (Map.Entry<String, Map<String, String>> masterTextEntry : masterTextUpdates.entrySet()) {
				getLogger().debug("  Pattern [ {} ]", masterTextEntry.getKey());
				for (Map.Entry<String, String> substitution : masterTextEntry.getValue()
					.entrySet()) {
					getLogger().debug("    [ {} ]: [ {} ]", substitution.getKey(), substitution.getValue());
				}
			}
		}
	}

	private SelectionRule selectionRules;

	protected SelectionRule getSelectionRule() {
		if (selectionRules == null) {
			selectionRules = new SelectionRuleImpl(getLogger(), includes, excludes);
		}
		return selectionRules;
	}

	private SignatureRule signatureRules;

	protected SignatureRule getSignatureRule() {
		if (signatureRules == null) {
			signatureRules = new SignatureRuleImpl(
				getLogger(),
				packageRenames, packageVersions, specificPackageVersions,
				bundleUpdates,
				masterTextUpdates, directStrings, perClassConstantStrings);
		}
		return signatureRules;
	}

	public boolean setInput() {
		String useInputName = options.getInputFileName();
		if (useInputName == null) {
			getLogger().error(consoleMarker, "No input file was specified");
			return false;
		}

		inputName = options.normalize(useInputName);
		inputFile = new File(inputName);
		inputPath = inputFile.getAbsolutePath();

		if (!inputFile.exists()) {
			getLogger().error(consoleMarker, "Input does not exist [ {} ] [ {} ]", inputName, inputPath);
			return false;
		}

		getLogger().debug(consoleMarker, "Input [ {} ]", inputName);
		getLogger().info(consoleMarker, "Input [ {} ]", inputPath);
		return true;
	}

	public static final String OUTPUT_PREFIX = "output_";

	public boolean setOutput() {
		String useOutputName = options.getOutputFileName();

		boolean isExplicit = (useOutputName != null);

		if (isExplicit) {
			useOutputName = options.normalize(useOutputName);

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
			getLogger().debug(consoleMarker, "Output generated using input name and output directory [ {} ]",
				useOutputName);

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

		getLogger().debug(consoleMarker, "Output [ {} ] ({})", useOutputName, outputCase);
		getLogger().info(consoleMarker, "Output [ {} ]", useOutputPath);

		allowOverwrite = options.hasOption(AppOption.OVERWRITE);
		if (allowOverwrite) {
			getLogger().info(consoleMarker, "Overwrite of output is enabled");
		}

		if (useOutputFile.exists()) {
			if (allowOverwrite) {
				getLogger().info(consoleMarker, "Output exists and will be overwritten [ {} ]", useOutputPath);
			} else {
				getLogger().error(consoleMarker, "Output already exists [ {} ]", useOutputPath);
				return false;
			}
		} else {
			if (allowOverwrite) {
				getLogger().debug(consoleMarker, "Overwritten specified, but output [ {} ] does not exist",
					useOutputPath);
			}
		}

		outputName = useOutputName;
		outputFile = useOutputFile;
		outputPath = useOutputPath;

		return true;
	}

	public void setActions() {
		if (options.hasOption(AppOption.WIDEN_ARCHIVE_NESTING)) {
			widenArchiveNesting = true;
			getLogger().info(consoleMarker, "Widened action nesting is enabled.");
		} else {
			widenArchiveNesting = false;
		}
	}

	public CompositeAction getRootAction() {
		if (rootAction == null) {
			CompositeActionImpl useRootAction = new CompositeActionImpl(getLogger(), getBuffer(), getSelectionRule(),
				getSignatureRule());

			ContainerActionImpl directoryAction = useRootAction.addUsing(DirectoryActionImpl::new);

			Action classAction = useRootAction.addUsing(ClassActionImpl::new);
			Action javaAction = useRootAction.addUsing(JavaActionImpl::new);
			Action serviceConfigAction = useRootAction
				.addUsing(ServiceLoaderConfigActionImpl::new);
			Action manifestAction = useRootAction.addUsing(ManifestActionImpl::newManifestAction);
			Action featureAction = useRootAction.addUsing(ManifestActionImpl::newFeatureAction);
			Action propertiesAction = useRootAction.addUsing(PropertiesActionImpl::new);

			ContainerActionImpl jarAction = useRootAction.addUsing(JarActionImpl::new);
			ContainerActionImpl warAction = useRootAction.addUsing(WarActionImpl::new);
			ContainerActionImpl rarAction = useRootAction.addUsing(RarActionImpl::new);
			ContainerActionImpl earAction = useRootAction.addUsing(EarActionImpl::new);

			Action textAction = useRootAction.addUsing(TextActionImpl::new);
			// Action xmlAction =
			// useRootAction.addUsing( XmlActionImpl::new );

			ContainerActionImpl zipAction = useRootAction.addUsing(ZipActionImpl::new);

			Action nullAction = useRootAction.addUsing(NullActionImpl::new);

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
		String actionName = options.getOptionValue(AppOption.FILE_TYPE);
		if (actionName != null) {
			for (Action action : getRootAction().getActions()) {
				if (action.getActionType()
					.matches(actionName)) {
					getLogger().info(consoleMarker, "Forced action [ {} ] [ {} ]", actionName, action.getName());
					acceptedAction = action;
					return true;
				}
			}
			getLogger().error(consoleMarker, "No match for forced action [ {} ]", actionName);
			return false;

		} else {
			acceptedAction = getRootAction().acceptAction(inputName, inputFile);
			if (acceptedAction == null) {
				getLogger().error(consoleMarker, "No action selected for input [ {} ]", inputName);
				return false;
			} else {
				getLogger().info(consoleMarker, "Action selected for input [ {} ]: {}", inputName,
					acceptedAction.getName());
				return true;
			}
		}
	}

	public void transform() throws TransformException {

		acceptedAction.apply(inputName, inputFile, outputFile);

		acceptedAction.getLastActiveChanges()
			.log(getLogger(), inputPath, outputPath);
	}

	public Changes getLastActiveChanges() {
		if (acceptedAction != null) {
			return acceptedAction.getLastActiveChanges();
		}
		return null;
	}
}
