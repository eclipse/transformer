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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.transformer.TransformerLoggerFactory.LoggerProperty;
import org.eclipse.transformer.action.ActionType;
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
// import org.eclipse.transformer.action.impl.XmlActionImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.util.FileUtils;
import org.slf4j.Logger;

import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.uri.URIUtil;

public class Transformer {
	// TODO: Make this an enum?

	public static final int	SUCCESS_RC					= 0;
	public static final int	PARSE_ERROR_RC				= 1;
	public static final int	RULES_ERROR_RC				= 2;
	public static final int	TRANSFORM_ERROR_RC			= 3;
	public static final int	FILE_TYPE_ERROR_RC			= 4;
	public static final int	LOGGER_SETTINGS_ERROR_RC	= 5;

	public static String[]	RC_DESCRIPTIONS				= new String[] {
		"Success", "Parse Error", "Rules Error", "Transform Error", "File Type Error", "Logger Settings Error"
	};

	//

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		int rc = runWith(System.out, System.err, args);
		// System.exit(rc); // TODO: How should this code be returned?
	}

	public static int runWith(PrintStream sysOut, PrintStream sysErr, String... args) {
		Transformer trans = new Transformer(sysOut, sysErr);
		trans.setArgs(args);

		int rc = trans.run();
		if (rc == SUCCESS_RC) {
			System.out.println("Return Code [ 0 ]: Success");
		} else {
			System.err.println("Return Code [ " + rc + " ]: Failure [ " + RC_DESCRIPTIONS[rc] + " ]");
		}
		return rc;
	}

	//

	public static class OptionSettings {
		private static final boolean	HAS_ARG			= true;
		private static final boolean	HAS_ARGS		= true;
		private static final boolean	HAS_ARG_COUNT	= true;
		private static final boolean	IS_REQUIRED		= true;
		private static final String		NO_GROUP		= null;

		private OptionSettings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
			boolean isRequired, String groupTag) {
			this(shortTag, longTag, description, hasArg, hasArgs, !HAS_ARG_COUNT, -1, isRequired, groupTag);
		}

		private OptionSettings(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
			boolean hasArgCount, int argCount,
			boolean isRequired, String groupTag) {

			this.shortTag = shortTag;
			this.longTag = longTag;
			this.description = description;

			this.isRequired = isRequired;

			this.hasArg = hasArg;
			this.hasArgs = hasArgs;
			this.hasArgCount = hasArgCount;
			this.argCount = argCount;

			this.groupTag = groupTag;
		}

		private final String	shortTag;
		private final String	longTag;
		private final String	description;

		public String getShortTag() {
			return shortTag;
		}

		public String getLongTag() {
			return longTag;
		}

		public String getDescription() {
			return description;
		}

		//

		// Is this option required.
		// If in a group, is at least one of the group required.

		private final boolean	isRequired;

		//

		private final boolean	hasArg;
		private final boolean	hasArgs;
		private final boolean	hasArgCount;
		private final int		argCount;

		private final String	groupTag;

		public boolean getHasArg() {
			return hasArg;
		}

		public boolean getHasArgs() {
			return hasArgs;
		}

		public boolean getHasArgCount() {
			return hasArgCount;
		}

		public int getArgCount() {
			return argCount;
		}

		public String getGroupTag() {
			return groupTag;
		}

		public boolean getIsRequired() {
			return isRequired;
		}

		//

		public static Options build(OptionSettings[] settings) {
			Options options = new Options();

			Map<String, OptionGroup> groups = new HashMap<>();

			for (OptionSettings optionSettings : settings) {
				String groupTag = optionSettings.getGroupTag();
				OptionGroup group;
				if (groupTag != null) {
					group = groups.get(groupTag);
					if (group == null) {
						group = new OptionGroup();
						if (optionSettings.getIsRequired()) {
							group.setRequired(true);
						}
						groups.put(groupTag, group);

						options.addOptionGroup(group);
					}

				} else {
					group = null;
				}

				Builder builder = Option.builder(optionSettings.getShortTag());
				builder.longOpt(optionSettings.getLongTag());
				builder.desc(optionSettings.getDescription());
				if (optionSettings.getHasArgs()) {
					builder.hasArg(false);
					builder.hasArgs();
				} else if (optionSettings.getHasArg()) {
					builder.hasArg();
				} else if (optionSettings.getHasArgCount()) {
					builder.numberOfArgs(optionSettings.getArgCount());
				} else {
					// No arguments are required for this option.
				}
				builder.required((group == null) && optionSettings.getIsRequired());

				Option option = builder.build();

				if (group != null) {
					group.addOption(option);
				} else {
					options.addOption(option);
				}
			}

			return options;
		}
	}

	// Not in use, until option grouping is figured out.

	public static final String	INPUT_GROUP		= "input";
	public static final String	LOGGING_GROUP	= "logging";

	public static final String	HELP_SHORT_TAG	= "-h";
	public static final String	HELP_LONG_TAG	= "--help";

	public static final String	USAGE_SHORT_TAG	= "-u";
	public static final String	USAGE_LONG_TAG	= "--usage";

	public enum AppOption {
		USAGE("u", "usage", "Display usage", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		HELP("h", "help", "Display help", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		// TODO: Refine versioning
		// FULL_VERSION("f", "fullVersion", "Display full version information",
		// !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
		// !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		LOG_TERSE("q", "quiet", "Display quiet output", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_VERBOSE("v", "verbose", "Display verbose output", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_PROPERTY("lp", "logProperty", "Logging property", !OptionSettings.HAS_ARG, OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_PROPERTY_FILE("lpf", "logPropertyFile", "Logging properties file", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_NAME("ln", "logName", "Logger name", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_LEVEL("ll", "logLevel", "Logging level", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		LOG_FILE("lf", "logFile", "Logging file", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		RULES_SELECTIONS("ts", "selection", "Transformation selections URL", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		RULES_RENAMES("tr", "renames", "Transformation package renames URL", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		RULES_VERSIONS("tv", "versions", "Transformation package versions URL", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		RULES_BUNDLES("tb", "bundles", "Transformation bundle updates URL", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		RULES_DIRECT("td", "direct", "Transformation direct string replacements", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		RULES_MASTER_TEXT("tf", "text", "Map of filenames to property files", OptionSettings.HAS_ARG,
			!OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		RULES_IMMEDIATE_DATA("ti", "immediate", "Immediate rule data", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			OptionSettings.HAS_ARG_COUNT, 3,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		// RULES_MASTER_XML("tf", "xml", "Map of XML filenames to property files", OptionSettings.HAS_ARG,
		//    !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		INVERT("i", "invert", "Invert transformation rules", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		FILE_TYPE("t", "type", "Input file type", OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
		OVERWRITE("o", "overwrite", "Overwrite", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

		DRYRUN("d", "dryrun", "Dry run", !OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS, !OptionSettings.IS_REQUIRED,
			OptionSettings.NO_GROUP),

		RULES_PER_CLASS_CONSTANT("tp", "per-class-constant", "Transformation per class constant string replacements",
			OptionSettings.HAS_ARG, !OptionSettings.HAS_ARGS,
			!OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP);

		private AppOption(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
			boolean hasArgCount, int argCount,
			boolean isRequired, String groupTag) {

			this.settings = new OptionSettings(shortTag, longTag, description, hasArg, hasArgs, hasArgCount, argCount,
				isRequired, groupTag);
		}

		private AppOption(String shortTag, String longTag, String description, boolean hasArg, boolean hasArgs,
			boolean isRequired, String groupTag) {

			this.settings = new OptionSettings(shortTag, longTag, description, hasArg, hasArgs, isRequired, groupTag);
		}

		private final OptionSettings settings;

		public OptionSettings getSettings() {
			return settings;
		}

		public String getShortTag() {
			return getSettings().getShortTag();
		}

		public String getLongTag() {
			return getSettings().getLongTag();
		}

		public String getDescription() {
			return getSettings().getDescription();
		}

		public boolean getIsRequired() {
			return getSettings().getIsRequired();
		}

		public boolean getHasArg() {
			return getSettings().getHasArg();
		}

		public String getGroupTag() {
			return getSettings().getGroupTag();
		}

		//

		private static OptionSettings[] getAllSettings() {
			AppOption[] allAppOptions = AppOption.values();

			OptionSettings[] allSettings = new OptionSettings[allAppOptions.length];

			for (int optionNo = 0; optionNo < allAppOptions.length; optionNo++) {
				allSettings[optionNo] = allAppOptions[optionNo].getSettings();
			}

			return allSettings;
		}

		public static Options build() {
			return OptionSettings.build(getAllSettings());
		}
	}

	//

	public static InputStream getResourceStream(String resourceRef) {
		return Transformer.class.getClassLoader()
			.getResourceAsStream(resourceRef);
	}

	public static Properties loadProperties(String resourceRef) throws IOException {
		Properties properties = new Properties();
		try (InputStream inputStream = Transformer.getResourceStream(resourceRef)) {
			properties.load(inputStream);
		}
		return properties;
	}

	public Transformer(PrintStream sysOut, PrintStream sysErr) {
		this.sysOut = sysOut;
		this.sysErr = sysErr;

		Properties useProperties;
		try {
			useProperties = Transformer.loadProperties(TRANSFORMER_BUILD_PROPERTIES);
		} catch (IOException e) {
			useProperties = new Properties();
			this.error("Failed to load properties [ " + TRANSFORMER_BUILD_PROPERTIES + " ]", e);
		}
		this.buildProperties = useProperties;

		this.appOptions = AppOption.build();
	}

	//

	private static final String[]	COPYRIGHT_LINES					= {
		"Copyright (c) 2020 Contributors to the Eclipse Foundation",
		"This program and the accompanying materials are made available under the",
		"terms of the Eclipse Public License 2.0 which is available at",
		"http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0",
		"which is available at https://www.apache.org/licenses/LICENSE-2.0.",
		"SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)", ""
	};

	private static final String		SHORT_VERSION_PROPERTY_NAME		= "version";

	private static final String		TRANSFORMER_BUILD_PROPERTIES	= "META-INF/maven/org.eclipse.transformer/org.eclipse.transformer/pom.properties";

	private final Properties		buildProperties;

	private final Properties getBuildProperties() {
		return buildProperties;
	}

	// TODO: Usual command line usage puts SysOut and SysErr together, which
	// results
	// in the properties writing out twice.

	private void preInitDisplay(String message) {
		PrintStream useSysOut = getSystemOut();
		// PrintStream useSysErr = getSystemErr();

		useSysOut.println(message);
		// if ( useSysErr != useSysOut ) {
		// useSysErr.println(message);
		// }
	}

	private void displayCopyright() {
		for (String copyrightLine : COPYRIGHT_LINES) {
			preInitDisplay(copyrightLine);
		}
	}

	private void displayBuildProperties() {
		Properties useBuildProperties = getBuildProperties();

		preInitDisplay(getClass().getName());
		preInitDisplay("  Version [ " + useBuildProperties.getProperty(SHORT_VERSION_PROPERTY_NAME) + " ]");
		preInitDisplay("");
	}

	//

	private final PrintStream sysOut;

	protected PrintStream getSystemOut() {
		return sysOut;
	}

	private final PrintStream sysErr;

	protected PrintStream getSystemErr() {
		return sysErr;
	}

	public void systemPrint(PrintStream output, String message, Object... parms) {
		if (parms.length != 0) {
			message = String.format(message, parms);
		}
		output.println(message);
	}

	public void errorPrint(String message, Object... parms) {
		systemPrint(getSystemErr(), message, parms);
	}

	public void outputPrint(String message, Object... parms) {
		systemPrint(getSystemOut(), message, parms);
	}

	//

	private final Options appOptions;

	public Options getAppOptions() {
		return appOptions;
	}

	private Class<?>				ruleLoader;
	private Map<AppOption, String>	ruleDefaultRefs;

	private String[]				args;
	private CommandLine				parsedArgs;

	private Changes					lastActiveChanges;

	public Changes getLastActiveChanges() {
		return lastActiveChanges;
	}

	/**
	 * Set default resource references for the several 'RULE" options. Values
	 * are located relative to the option loader class.
	 *
	 * @param optionLoader The class relative to which to load the default
	 *            resources.
	 * @param optionDefaults Table ot default resource references.
	 */
	public void setOptionDefaults(Class<?> optionLoader, Map<AppOption, String> optionDefaults) {
		this.ruleLoader = optionLoader;
		this.ruleDefaultRefs = new HashMap<>(optionDefaults);
	}

	public Class<?> getRuleLoader() {
		return ruleLoader;
	}

	public Map<AppOption, String> getRuleDefaultRefs() {
		return ruleDefaultRefs;
	}

	public String getDefaultReference(AppOption appOption) {
		Map<AppOption, String> useDefaultRefs = getRuleDefaultRefs();
		return ((useDefaultRefs == null) ? null : getRuleDefaultRefs().get(appOption));
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	protected String[] getArgs() {
		return args;
	}

	public void setParsedArgs() throws ParseException {
		CommandLineParser parser = new DefaultParser();
		parsedArgs = parser.parse(getAppOptions(), getArgs());
	}

	protected CommandLine getParsedArgs() {
		return parsedArgs;
	}

	protected String getInputFileNameFromCommandLine() {
		String[] useArgs = parsedArgs.getArgs();
		if (useArgs != null) {
			if (useArgs.length > 0) {
				return useArgs[0]; // First argument
			}
		}
		return null;
	}

	protected String getOutputFileNameFromCommandLine() {
		String[] useArgs = parsedArgs.getArgs();
		if (useArgs != null) {
			if (useArgs.length > 1) {
				return useArgs[1]; // Second argument
			}
		}
		return null;
	}

	protected boolean hasOption(AppOption option) {
		return getParsedArgs().hasOption(option.getShortTag());
	}

	protected static boolean DO_NORMALIZE = true;

	protected String getOptionValue(AppOption option) {
		return getOptionValue(option, !DO_NORMALIZE);
	}

	protected String getOptionValue(AppOption option, boolean normalize) {
		CommandLine useParsedArgs = getParsedArgs();
		String useShortTag = option.getShortTag();
		if (useParsedArgs.hasOption(useShortTag)) {
			String optionValue = useParsedArgs.getOptionValue(useShortTag);
			if (normalize) {
				optionValue = FileUtils.normalize(optionValue);
			}
			return optionValue;
		} else {
			return null;
		}
	}

	protected String[] getOptionValues(AppOption option) {
		return getOptionValues(option, !DO_NORMALIZE);
	}

	protected String[] getOptionValues(AppOption option, boolean normalize) {
		CommandLine useParsedArgs = getParsedArgs();
		String useShortTag = option.getShortTag();
		if (useParsedArgs.hasOption(useShortTag)) {
			String[] optionValues = useParsedArgs.getOptionValues(useShortTag);
			if (normalize) {
				for (int optionNo = 0; optionNo < optionValues.length; optionNo++) {
					optionValues[optionNo] = FileUtils.normalize(optionValues[optionNo]);
				}
			}
			return optionValues;
		} else {
			return null;
		}
	}

	//

	private void usage(PrintStream helpStream) {
		helpStream.println("Usage: " + Transformer.class.getName() + " input [ output ] [ options ]");
		helpStream.println();
		helpStream
			.println("Use option [ " + HELP_SHORT_TAG + " ] or [ " + HELP_LONG_TAG + " ] to display help information.");
		helpStream.flush();
	}

	private void help(PrintStream helpStream) {
		try (PrintWriter helpWriter = new PrintWriter(helpStream)) {
			helpWriter.println();

			HelpFormatter helpFormatter = new HelpFormatter();
			boolean AUTO_USAGE = true;
			helpFormatter.printHelp(helpWriter, HelpFormatter.DEFAULT_WIDTH + 5,
				Transformer.class.getName() + " input [ output ] [ options ]", // Command
																				// line
																				// syntax
				"Options:", // Header
				getAppOptions(), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "", // Footer
				!AUTO_USAGE);

			helpWriter.println();
			helpWriter.println("Actions:");
			for (ActionType actionType : ActionType.values()) {
				helpWriter.println("  [ " + actionType.name() + " ]");
			}

			helpWriter.println();
			helpWriter.println("Logging Properties:");
			for (TransformerLoggerFactory.LoggerProperty loggerProperty : TransformerLoggerFactory.LoggerProperty
				.values()) {
				helpWriter.println("  [ " + loggerProperty.getPropertyName() + " ]");
			}

			helpWriter.flush();
		}
	}

	//

	/**
	 * Load properties for the specified rule option. Answer an empty collection
	 * if the rule option was not provided. Options loading tries
	 * {@link #getOptionValue(AppOption)}, then tries
	 * {@link #getDefaultReference(AppOption)}. If neither is set, an empty
	 * collection is returned.
	 *
	 * @param ruleOption The option for which to load properties.
	 * @param orphanedValues Values which have been orphaned by merges.
	 * @return Properties loaded using the reference set for the option.
	 * @throws IOException Thrown if the load failed.
	 * @throws URISyntaxException Thrown if the load failed because a non-valid
	 *             URI was specified.
	 */
	protected UTF8Properties loadProperties(AppOption ruleOption, Set<String> orphanedValues)
		throws IOException, URISyntaxException {
		String[] rulesReferences = getOptionValues(ruleOption, DO_NORMALIZE);

		if (rulesReferences == null) {
			String rulesReference = getDefaultReference(ruleOption);
			if (rulesReference == null) {
				dual_info("Skipping option [ %s ]", ruleOption);
				return FileUtils.createProperties();
			} else {
				return loadInternalProperties(ruleOption, rulesReference);
			}

		} else if ( rulesReferences.length == 1 ) {
			return loadExternalProperties(ruleOption, rulesReferences[0]);

		} else {
			UTF8Properties[] properties = new UTF8Properties[ rulesReferences.length ];
			for ( int referenceNo = 0; referenceNo < rulesReferences.length; referenceNo++ ) {
				properties[referenceNo] = loadExternalProperties( ruleOption, rulesReferences[referenceNo] );
			}

			String baseReference = rulesReferences[0];
			UTF8Properties mergedProperties = properties[0];

			for ( int referenceNo = 1; referenceNo < rulesReferences.length; referenceNo++ ) {
				merge( baseReference, mergedProperties,
					   rulesReferences[referenceNo], properties[referenceNo],
					   orphanedValues );
			}

			return mergedProperties;
		}
	}

	private String relativize(String relativeRef, String baseRef) {
		Path basePath = Paths.get(baseRef);
		Path siblingPath = basePath.resolveSibling(relativeRef);
		return siblingPath.toString();
	}

	/*
		Results of 'relativize':

		Base reference [ c:\dev\rules\textMaster ]
		Sibling reference [ sibling1 ]
		Base path [ c:\dev\rules\textMaster ]
		Sibling path [ c:\dev\rules\sibling1 ]

		Base reference [ c:\textMaster ]
		Sibling reference [ sibling1 ]
		Base path [ c:\textMaster ]
		Sibling path [ c:\sibling1 ]

		Base reference [ \textMaster ]
		Sibling reference [ sibling1 ]
		Base path [ \textMaster ]
		Sibling path [ \sibling1 ]

		Base reference [ textMaster ]
		Sibling reference [ sibling1 ]
		Base path [ textMaster ]
		Sibling path [ sibling1 ]
	*/

	protected UTF8Properties loadInternalProperties(AppOption ruleOption, String resourceRef) throws IOException {
		return loadInternalProperties(ruleOption.toString(), resourceRef);
	}

	protected UTF8Properties loadInternalProperties(String ruleOption, String resourceRef) throws IOException {
		// dual_info("Using internal [ %s ]: [ %s ]", ruleOption, resourceRef);
		URL rulesUrl = getRuleLoader().getResource(resourceRef);
		if (rulesUrl == null) {
			dual_info("Internal [ %s ] were not found [ %s ]", ruleOption, resourceRef);
			throw new IOException("Resource [ " + resourceRef + " ] not found on [ " + getRuleLoader() + " ]");
		} else {
			dual_info("Internal [ %s ] URL [ %s ]", ruleOption, rulesUrl);
		}
		return FileUtils.loadProperties(rulesUrl);
	}

	protected UTF8Properties loadExternalProperties
	    (AppOption ruleOption, String resourceRef)
		throws URISyntaxException, IOException {

		return loadExternalProperties(ruleOption.toString(), resourceRef);
	}

	protected UTF8Properties loadExternalProperties
	    (String referenceName, String externalReference)
		throws URISyntaxException, IOException {

		return loadExternalProperties(referenceName, externalReference, IO.work);
	}

	protected UTF8Properties loadExternalProperties(
		String referenceName, String externalReference, File relativeHome)
		throws URISyntaxException, IOException {

		// dual_info("Using external [ %s ]: [ %s ]", referenceName, externalReference);

		URI relativeHomeUri = relativeHome.toURI();
		URL rulesUrl = URIUtil.resolve(relativeHomeUri, externalReference).toURL();
		dual_info("External [ %s ] URL [ %s ]", referenceName, rulesUrl);

		return FileUtils.loadProperties(rulesUrl);
	}

	protected void merge(
		String sinkName, UTF8Properties sink,
		String sourceName, UTF8Properties source,
		Set<String> orphanedValues) {

		for ( Map.Entry<Object, Object> sourceEntry : source.entrySet() ) {
			String key = (String) sourceEntry.getKey();
			String newValue = (String) sourceEntry.getValue();
			String oldValue = (String) sink.put(key, newValue);

			if ( orphanedValues != null ) {
				processOrphan(sourceName, sinkName, key, oldValue, newValue, orphanedValues);
			}

			logMerge(sourceName, sinkName, key, oldValue, newValue);
		}
	}

	protected void processOrphan(
		String sourceName, String sinkName,
		String key, String oldValue, String newValue,
		Set<String> orphans) {

		if ( oldValue != null ) {
			dual_debug(
				"Merge of [ %s ] into [ %s ], key [ %s ] orphans [ %s ]",
				sourceName, sinkName, key, oldValue);
			orphans.add(oldValue);
		}
		if ( orphans.remove(newValue) ) {
			dual_debug(
				"Merge of [ %s ] into [ %s ], key [ %s ] un-orphans [ %s ]",
				sourceName, sinkName, key, newValue);
		}		
	}
	
	protected void logMerge(String sourceName, String sinkName, Object key, Object oldValue, Object newValue) {
		// System.out.println("Key [ " + key + " ] Old [ " + oldValue + " ] New
		// [ " + newValue + " ]");

		if (oldValue != null) {
			dual_debug("Merge of [ %s ] into [ %s ], key [ %s ] replaces value [ %s ] with [ %s ]", sourceName,
				sinkName, key, oldValue, newValue);
		}
	}

	/**
	 * Fetch all immediate option data from the command line. Organize the data
	 * into separate objects, one object per specific immediate option on the
	 * command line.
	 *
	 * @return Grouped immediate option data from the command line.
	 */
	protected ImmediateRuleData[] getImmediateData() {
		if ( !hasOption(AppOption.RULES_IMMEDIATE_DATA) ) {
			return new ImmediateRuleData[] {};
		}

		String[] immediateArgs = getOptionValues(AppOption.RULES_IMMEDIATE_DATA);

		if ((immediateArgs.length % 3) != 0) {
			dual_error(
				"Incorrect number of arguments to option [ " + AppOption.RULES_IMMEDIATE_DATA.getShortTag() + " ]");
			return null;
		}

		int argCount = immediateArgs.length / 3;

		ImmediateRuleData[] immediateData = new ImmediateRuleData[argCount];

		for (int argNo = 0; argNo < argCount; argNo++) {
			int baseNo = argNo * 3;
			String targetText = immediateArgs[baseNo];
			String key = immediateArgs[baseNo + 1];
			String value = immediateArgs[baseNo + 2];

			dual_info("Immediate rule data specified; target [ %s ], key [ %s ], value [ %s ]", targetText, key, value);

			AppOption target = getTargetOption(targetText);
			if (target == null) {
				dual_error("Immediate rules target [ " + targetText + " ] is not valid.");
				return null;
			}

			immediateData[argNo] = new ImmediateRuleData(target, key, value);
		}

		return immediateData;
	}

	public static final AppOption[] TARGETABLE_RULES = new AppOption[] {
		AppOption.RULES_SELECTIONS,

		AppOption.RULES_RENAMES, AppOption.RULES_VERSIONS,
		AppOption.RULES_DIRECT,
		AppOption.RULES_PER_CLASS_CONSTANT,

		AppOption.RULES_BUNDLES, AppOption.RULES_MASTER_TEXT
	};

	public AppOption getTargetOption(String targetText) {
		for ( AppOption appOption : TARGETABLE_RULES ) {
			if (targetText.contentEquals(appOption.getShortTag())) {
				return appOption;
			}
		}
		return null;
	}

	public static class ImmediateRuleData {
		public final AppOption	target;
		public final String		key;
		public final String		value;

		public ImmediateRuleData(AppOption target, String key, String value) {
			this.target = target;
			this.key = key;
			this.value = value;
		}
	}

	//

	private Logger logger;

	public Logger getLogger() {
		return logger;
	}

	public void info(String message, Object... parms) {
		getLogger().info(message, parms);
	}

	public boolean isDebugEnabled() {
		return getLogger().isDebugEnabled();
	}

	public void debug(String message, Object... parms) {
		getLogger().debug(message, parms);
	}

	protected void error(String message, Object... parms) {
		getLogger().error(message, parms);
	}

	protected void error(String message, Throwable th, Object... parms) {
		Logger useLogger = getLogger();
		if (useLogger.isErrorEnabled()) {
			message = String.format(message, parms);
			useLogger.error(message, th);
		}
	}

	//

	public boolean	toSysOut;
	public boolean	toSysErr;

	protected void detectLogFile() {
		toSysOut = TransformerLoggerFactory.logToSysOut();
		if (toSysOut) {
			outputPrint("Logging is to System.out\n");
		}

		toSysErr = TransformerLoggerFactory.logToSysErr();
		if (toSysOut) {
			outputPrint("Logging is to System.err\n");
		}

		outputPrint("Log file [ " + System.getProperty(LoggerProperty.LOG_FILE.getPropertyName()) + " ]");
	}

	public void dual_info(String message, Object... parms) {
		if (parms.length != 0) {
			message = String.format(message, parms);
		}
		if (!toSysOut && !toSysErr) {
			systemPrint(getSystemOut(), message);
		}
		info(message);
	}

	public void dual_debug(String message, Object... parms) {
		if ( !isDebugEnabled() ) {
			return;
		}

		if (parms.length != 0) {
			message = String.format(message, parms);
		}
		if (!toSysOut && !toSysErr) {
			systemPrint(getSystemOut(), message);
		}
		debug(message);
	}

	protected void dual_error(String message, Object... parms) {
		if (parms.length != 0) {
			message = String.format(message, parms);
		}
		if (!toSysOut && !toSysErr) {
			systemPrint(getSystemErr(), message);
		}
		info(message);
	}

	protected void dual_error(String message, Throwable th) {
		if (!toSysOut && !toSysErr) {
			PrintStream useOutput = getSystemErr();
			systemPrint(useOutput, message);
			th.printStackTrace(useOutput);
		}
		getLogger().error(message, th);
	}

	//

	public TransformOptions createTransformOptions() {
		return new TransformOptions();
	}

	public class TransformOptions {
		public boolean							isVerbose;
		public boolean							isTerse;

		public Set<String>						includes;
		public Set<String>						excludes;

		public boolean							invert;
		public Map<String, String>				packageRenames;
		public Map<String, String>				packageVersions;
		public Map<String, BundleData>			bundleUpdates;
		public Map<String, String>				masterSubstitutionRefs;
		public Map<String, Map<String, String>>	masterTextUpdates;
		// ( pattern -> ( initial-> final ) )

		public Map<String, String>				directStrings;

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
			logger = new TransformerLoggerFactory(Transformer.this).createLogger(); // throws
																					// TransformException

			if (hasOption(AppOption.LOG_TERSE)) {
				isTerse = true;
			} else if (hasOption(AppOption.LOG_VERBOSE)) {
				isVerbose = true;
			}
		}

		protected void info(String message, Object... parms) {
			getLogger().info(message, parms);
		}

		protected void error(String message, Object... parms) {
			getLogger().error(message, parms);
		}

		protected void error(String message, Throwable th, Object... parms) {
			Logger useLogger = getLogger();
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

		public boolean setRules() throws IOException, URISyntaxException, IllegalArgumentException {
			ImmediateRuleData[] immediateData = getImmediateData();
			if (immediateData == null) {
				return false;
			}

			Set<String> orphanedFinalPackages = new HashSet<String>();

			UTF8Properties selectionProperties = loadProperties(AppOption.RULES_SELECTIONS, null);
			UTF8Properties renameProperties = loadProperties(AppOption.RULES_RENAMES, orphanedFinalPackages);
			UTF8Properties versionProperties = loadProperties(AppOption.RULES_VERSIONS, null);
			UTF8Properties updateProperties = loadProperties(AppOption.RULES_BUNDLES, null);
			UTF8Properties directProperties = loadProperties(AppOption.RULES_DIRECT, null);
			UTF8Properties textMasterProperties = loadProperties(AppOption.RULES_MASTER_TEXT, null);
			UTF8Properties perClassConstantProperties = loadProperties(AppOption.RULES_PER_CLASS_CONSTANT, null);

			invert = hasOption(AppOption.INVERT);

			if (!selectionProperties.isEmpty()) {
				includes = new HashSet<>();
				excludes = new HashSet<>();
				TransformProperties.addSelections(includes, excludes, selectionProperties);
				dual_info("Selection rules are in use");
			} else {
				includes = null;
				excludes = null;
			}

			if (!renameProperties.isEmpty()) {
				Map<String, String> renames = TransformProperties.getPackageRenames(renameProperties);
				if (invert) {
					renames = TransformProperties.invert(renames);
				}
				packageRenames = renames;
				dual_info("Package renames are in use");
			} else {
				packageRenames = null;
			}

			if (!versionProperties.isEmpty()) {
				packageVersions = TransformProperties.getPackageVersions(versionProperties);
				dual_info("Package versions will be updated");
			} else {
				packageVersions = null;
			}

			if (!updateProperties.isEmpty()) {
				bundleUpdates = TransformProperties.getBundleUpdates(updateProperties);
				// throws IllegalArgumentException
				dual_info("Bundle identities will be updated");
			} else {
				bundleUpdates = null;
			}

			String masterTextRef;

			if (!textMasterProperties.isEmpty()) {
				masterTextRef = getOptionValue(AppOption.RULES_MASTER_TEXT, DO_NORMALIZE);

				Map<String, String> substitutionRefs = TransformProperties.convertPropertiesToMap(textMasterProperties);
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
				dual_info("Text files will be updated");

			} else {
				masterTextRef = null;
				masterTextUpdates = null;
			}

			if (!directProperties.isEmpty()) {
				directStrings = TransformProperties.getDirectStrings(directProperties);
				dual_info("Java direct string updates will be performed");
			} else {
				directStrings = null;
				dual_info("Java direct string updates will not be performed");
			}

			if (!perClassConstantProperties.isEmpty()) {
				String masterDirect = getOptionValue(AppOption.RULES_PER_CLASS_CONSTANT, DO_NORMALIZE);

				Map<String, String> substitutionRefs
						= TransformProperties.convertPropertiesToMap(perClassConstantProperties); // throws IllegalArgumentException

				Map<String, Map<String, String>> masterUpdates = new HashMap<String, Map<String, String>>();
				for (Map.Entry<String, String> substitutionRefEntry : substitutionRefs.entrySet()) {
					String classSelector = substitutionRefEntry.getKey();
					String substitutionsRef = FileUtils.normalize(substitutionRefEntry.getValue());

					UTF8Properties substitutions = new UTF8Properties();
					if (masterDirect == null) {
						substitutions = loadInternalProperties("Substitions matching [ " + classSelector + " ]", substitutionsRef);
					}
					Map<String, String> substitutionsMap
							= TransformProperties.convertPropertiesToMap(substitutions); // throws IllegalArgumentException
					masterUpdates.put(classSelector, substitutionsMap);
				}

				perClassConstantStrings = masterUpdates;
				dual_info("Per class constant mapping files are enabled");

			} else {
				perClassConstantStrings = null;
				dual_info("Per class constant mapping files are not enabled");
			}

			processImmediateData(immediateData, masterTextRef, orphanedFinalPackages);

			// Delay reporting null property sets: These are assigned directly
			// and by immediate data.

			if (includes == null) {
				dual_info("All resources will be selected");
			}
			if (packageRenames == null) {
				dual_info("No package renames are available");
			}
			if (packageVersions == null) {
				dual_info("Package versions will not be updated");
			}
			if (bundleUpdates == null) {
				dual_info("Bundle identities will not be updated");
			}
			if (masterTextUpdates == null) {
				dual_info("Text files will not be updated");
			}
			if (directStrings == null) {
				dual_info("Java direct string updates will not be performed");
			}
			if (perClassConstantStrings == null) {
				dual_info("Per class constant mapping files are not enabled");
			}

			return validateRules(packageRenames, packageVersions, orphanedFinalPackages);
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
						dual_error("Unrecognized immediate data target [ {} ]", nextData.target);
				}
			}
		}

		private void addImmediateSelection(String selection, @SuppressWarnings("unused") String ignored) {
			if (includes == null) {
				includes = new HashSet<>();
				excludes = new HashSet<>();
				dual_info("Selection rules use forced by immediate data");
			}

			TransformProperties.addSelection(includes, excludes, selection);
		}

		private void addImmediateRename(
			String initialPackageName, String finalPackageName,
			Set<String> orphanedFinalPackages) {

			if ( packageRenames == null ) {
				packageRenames = new HashMap<String, String>();
				dual_info("Package renames forced by immediate data.");
			}

			if (invert) {
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
			if (packageVersions == null) {
				packageVersions = new HashMap<>();
				dual_info("Package version updates forced by immediate data.");
			}
			String oldVersionText = packageVersions.put(finalPackageName, versionText);
			logMerge("immediate package version data", "package version data", finalPackageName, oldVersionText,
				versionText);
		}

		private void addImmediateBundleData(String bundleId, String value) {
			if (bundleUpdates == null) {
				bundleUpdates = new HashMap<>();
				dual_info("Bundle identity updates forced by immediate data.");
			}
			BundleData newBundleData = new BundleDataImpl(value);
			BundleData oldBundleData = bundleUpdates.put(bundleId, newBundleData);
			if ( oldBundleData != null ) {
				logMerge("immediate bundle data", "bundle data", bundleId, oldBundleData.getPrintString(), newBundleData.getPrintString());
			}
		}

		private void addImmediateDirect(String initialText, String finalText) {
			if (directStrings == null) {
				directStrings = new HashMap<>();
				dual_info("Java direct string updates forced by immediate data");
			}

			String oldFinalText = directStrings.put(initialText, finalText);
			if (oldFinalText != null) {
				logMerge("immediate direct string data", "direct string data", initialText, oldFinalText, finalText);
			}
		}

		private Map<String, String> loadSubstitutions(String masterRef, String selector, String substitutionsRef)
			throws IOException, URISyntaxException {
			UTF8Properties substitutions;
			if (masterRef == null) {
				substitutions = loadInternalProperties(
					"Substitions matching [ " + selector + " ]", substitutionsRef); // throws IOException
			} else {
				String relativeSubstitutionsRef = relativize(substitutionsRef, masterRef);
				if (!relativeSubstitutionsRef.equals(substitutionsRef)) {
					dual_info("Adjusted substition reference from [ %s ] to [ %s ]",
						substitutionsRef,
						relativeSubstitutionsRef);
				}
				
				substitutions = loadExternalProperties(
					"Substitions matching [ " + selector + " ]", relativeSubstitutionsRef);
				// throws URISyntaxException, IOException
			}

			Map<String, String> substitutionsMap = TransformProperties.convertPropertiesToMap(substitutions);
			// throws IllegalArgumentException

			return substitutionsMap;
		}

		private void addImmediateMasterText(String masterTextRef, String simpleNameSelector, String substitutionsRef)
			throws IOException, URISyntaxException {

			if (masterTextUpdates == null) {
				masterTextUpdates = new HashMap<>();
				dual_info("Text files updates forced by immediate data.");
			}

			substitutionsRef = FileUtils.normalize(substitutionsRef);

			Map<String, String> substitutionsMap = loadSubstitutions(masterTextRef, simpleNameSelector,
				substitutionsRef);
			// throws URISyntaxException, IOException

			String oldSubstitutionsRef = masterSubstitutionRefs.put(simpleNameSelector, substitutionsRef);
			@SuppressWarnings("unused")
			Map<String, String> oldSubstitutionMap = masterTextUpdates.put(simpleNameSelector, substitutionsMap);

			if (oldSubstitutionsRef != null) {
				logMerge("immediate master text data", "master text data", simpleNameSelector, oldSubstitutionsRef,
					substitutionsRef);
			}
		}

		protected boolean validateRules(
			Map<String, String> renamesMap,
			Map<String, String> versionsMap,
			Set<String> orphanedFinalPackages) {

			if ((versionsMap == null) || versionsMap.isEmpty()) {
				return true; // Nothing to validate
			}

			if ((renamesMap == null) || renamesMap.isEmpty()) {
				String[] renamesRefs = getRuleFileNames(AppOption.RULES_RENAMES);
				// String[] versionsRefs = getRuleFileNames(AppOption.RULES_VERSIONS);

				if (renamesRefs == null) {
					dual_error("Package version updates were specified but no rename rules were specified.");
				} else {
					dual_error("Package version updates were specified but no rename rules were specified.");
				}
				return false;
			}

			boolean missingFinalPackages = false;
			for ( String finalPackage : versionsMap.keySet() ) {
				if ( !renamesMap.containsValue(finalPackage) ) {
					if ( orphanedFinalPackages.contains(finalPackage) ) {
						dual_debug("Final package [ " + finalPackage + " ] was orphaned.");
					} else {
						dual_error(
							"Version rule package name [ " + finalPackage + " ]" +
							" was not found as a final package name in the package renames data.");
						missingFinalPackages = true;
					}
				}
			}

			return !missingFinalPackages;
		}

		protected String[] getRuleFileNames(AppOption ruleOption) {
			String[] rulesFileNames = getOptionValues(ruleOption, DO_NORMALIZE);
			if (rulesFileNames != null) {
				return rulesFileNames;
			} else {
				String defaultReference = getDefaultReference(ruleOption);
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
				selectionRules = new SelectionRuleImpl(logger, includes, excludes);
			}
			return selectionRules;
		}

		private SignatureRuleImpl signatureRules;

		protected SignatureRuleImpl getSignatureRule() {
			if (signatureRules == null) {
				signatureRules = new SignatureRuleImpl(logger, packageRenames, packageVersions, bundleUpdates,
					masterTextUpdates, directStrings, perClassConstantStrings);
			}
			return signatureRules;
		}

		public boolean setInput() {
			String useInputName = getInputFileNameFromCommandLine();
			if (useInputName == null) {
				dual_error("No input file was specified");
				return false;
			}

			inputName = FileUtils.normalize(useInputName);
			inputFile = new File(inputName);
			inputPath = inputFile.getAbsolutePath();

			if (!inputFile.exists()) {
				dual_error("Input does not exist [ %s ] [ %s ]", inputName, inputPath);
				return false;
			}

			dual_info("Input     [ %s ]", inputName);
			dual_info("          [ %s ]", inputPath);
			return true;
		}

		public static final String OUTPUT_PREFIX = "output_";

		// info("Output file not specified.");
		//
		// final String OUTPUT_PREFIX = "output_";
		// String inputFileName = getInputFileName();
		// int indexOfLastSlash = inputFileName.lastIndexOf('/');
		// if (indexOfLastSlash == -1 ) {
		// return OUTPUT_PREFIX + inputFileName;
		// } else {
		// return inputFileName.substring(0, indexOfLastSlash+1) + OUTPUT_PREFIX
		// + inputFileName.substring(indexOfLastSlash+1);
		// }

		public boolean setOutput() {
			String useOutputName = getOutputFileNameFromCommandLine();

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
					dual_info("Output generated using input name and output directory [ %s ]", useOutputName);
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

			dual_info("Output    [ %s ] (%s)", useOutputName, outputCase);
			dual_info("          [ %s ]", useOutputPath);

			allowOverwrite = hasOption(AppOption.OVERWRITE);
			if (allowOverwrite) {
				dual_info("Overwrite of output is enabled");
			}

			if (useOutputFile.exists()) {
				if (allowOverwrite) {
					dual_info("Output exists and will be overwritten [ %s ]", useOutputPath);
				} else {
					dual_error("Output already exists [ %s ]", useOutputPath);
					return false;
				}
			} else {
				if (allowOverwrite) {
					if (isVerbose) {
						dual_info("Overwritten specified, but output [ %s ] does not exist", useOutputPath);
					}
				}
			}

			outputName = useOutputName;
			outputFile = useOutputFile;
			outputPath = useOutputPath;

			return true;
		}

		public CompositeActionImpl getRootAction() {
			if (rootAction == null) {
				CompositeActionImpl useRootAction = new CompositeActionImpl(getLogger(), isTerse, isVerbose,
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
				directoryAction.addAction(nullAction);

				jarAction.addAction(classAction);
				jarAction.addAction(javaAction);
				jarAction.addAction(serviceConfigAction);
				jarAction.addAction(manifestAction);
				jarAction.addAction(featureAction);
				jarAction.addAction(textAction);
				jarAction.addAction(propertiesAction);
				jarAction.addAction(nullAction);

				warAction.addAction(classAction);
				warAction.addAction(javaAction);
				warAction.addAction(serviceConfigAction);
				warAction.addAction(manifestAction);
				warAction.addAction(featureAction);
				warAction.addAction(jarAction);
				warAction.addAction(textAction);
				warAction.addAction(nullAction);

				rarAction.addAction(classAction);
				rarAction.addAction(javaAction);
				rarAction.addAction(serviceConfigAction);
				rarAction.addAction(manifestAction);
				rarAction.addAction(featureAction);
				rarAction.addAction(jarAction);
				rarAction.addAction(textAction);
				rarAction.addAction(nullAction);

				earAction.addAction(manifestAction);
				earAction.addAction(jarAction);
				earAction.addAction(warAction);
				earAction.addAction(rarAction);
				earAction.addAction(textAction);
				earAction.addAction(nullAction);

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
				zipAction.addAction(nullAction);

				rootAction = useRootAction;
			}

			return rootAction;
		}

		public boolean acceptAction() {
			String actionName = getOptionValue(AppOption.FILE_TYPE);
			if (actionName != null) {
				for (ActionImpl action : getRootAction().getActions()) {
					if (action.getActionType()
						.matches(actionName)) {
						dual_info("Forced action [ %s ] [ %s ]", actionName, action.getName());
						acceptedAction = action;
						return true;
					}
				}
				dual_error("No match for forced action [ %s ]", actionName);
				return false;

			} else {
				acceptedAction = getRootAction().acceptAction(inputName, inputFile);
				if (acceptedAction == null) {
					dual_error("No action selected for input [ %s ]", inputName);
					return false;
				} else {
					dual_info("Action selected for input [ %s ]: %s", inputName, acceptedAction.getName());
					return true;
				}
			}
		}

		public void transform() throws TransformException {

			acceptedAction.apply(inputName, inputFile, outputFile);

			if (isTerse) {
				if (!toSysOut && !toSysErr) {
					acceptedAction.getLastActiveChanges()
						.displayTerse(getSystemOut(), inputPath, outputPath);
				}
				acceptedAction.getLastActiveChanges()
					.displayTerse(getLogger(), inputPath, outputPath);
			} else if (isVerbose) {
				if (!toSysOut && !toSysErr) {
					acceptedAction.getLastActiveChanges()
						.displayVerbose(getSystemOut(), inputPath, outputPath);
				}
				acceptedAction.getLastActiveChanges()
					.displayVerbose(getLogger(), inputPath, outputPath);
			} else {
				if (!toSysOut && !toSysErr) {
					acceptedAction.getLastActiveChanges()
						.display(getSystemOut(), inputPath, outputPath);
				}
				acceptedAction.getLastActiveChanges()
					.display(getLogger(), inputPath, outputPath);
			}
		}

		public Changes getLastActiveChanges() {
			if (acceptedAction != null) {
				return acceptedAction.getLastActiveChanges();
			}
			return null;
		}
	}

	public int run() {
		displayCopyright();
		displayBuildProperties();

		try {
			setParsedArgs();
		} catch (ParseException e) {
			errorPrint("Exception parsing command line arguments: %s", e);
			help(getSystemOut());
			return PARSE_ERROR_RC;
		}

		if ((getArgs().length == 0) || hasOption(AppOption.USAGE)) {
			usage(getSystemOut());
			return SUCCESS_RC; // TODO: Is this the correct return value?
		} else if (hasOption(AppOption.HELP)) {
			help(getSystemOut());
			return SUCCESS_RC; // TODO: Is this the correct return value?
		}

		TransformOptions options = createTransformOptions();

		try {
			options.setLogging();
		} catch (Exception e) {
			errorPrint("Logger settings error: %s", e);
			return LOGGER_SETTINGS_ERROR_RC;
		}
		detectLogFile();

		if (!options.setInput()) {
			return TRANSFORM_ERROR_RC;
		}

		if (!options.setOutput()) {
			return TRANSFORM_ERROR_RC;
		}

		boolean loadedRules;
		try {
			loadedRules = options.setRules();
		} catch (Exception e) {
			dual_error("Exception loading rules:", e);
			return RULES_ERROR_RC;
		}
		if (!loadedRules) {
			dual_error("Transformation rules cannot be used");
			return RULES_ERROR_RC;
		}
		if (options.isVerbose) {
			options.logRules();
		}

		if (!options.acceptAction()) {
			dual_error("No action selected");
			return FILE_TYPE_ERROR_RC;
		}

		try {
			options.transform(); // throws JakartaTransformException
			lastActiveChanges = options.getLastActiveChanges();
		} catch (TransformException e) {
			dual_error("Transform failure:", e);
			return TRANSFORM_ERROR_RC;
		} catch (Throwable th) {
			dual_error("Unexpected failure:", th);
			return TRANSFORM_ERROR_RC;
		}

		return SUCCESS_RC;
	}
}
