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
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
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
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.log.LoggerProperty;
import org.eclipse.transformer.log.TransformerLoggerFactory;
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

	// Not in use, until option grouping is figured out.

	public static final String	INPUT_GROUP		= "input";
	public static final String	LOGGING_GROUP	= "logging";

	public static final String	HELP_SHORT_TAG	= "-h";
	public static final String	HELP_LONG_TAG	= "--help";

	public static final String	USAGE_SHORT_TAG	= "-u";
	public static final String	USAGE_LONG_TAG	= "--usage";



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

		this.appOptions = buildOptions();
	}

	private Options buildOptions() {
		Options options = new Options();
		Map<String, OptionGroup> groups = new HashMap<>();

		Arrays.stream(AppOption.values())
			.map(AppOption::getSettings)
			.forEach(optionSettings -> {
				String groupTag = optionSettings.getGroupTag();
				OptionGroup group;
				if (groupTag != null) {
					group = groups.computeIfAbsent(groupTag, k -> {
						OptionGroup result = new OptionGroup();
						if (optionSettings.isRequired()) {
							result.setRequired(true);
						}
						options.addOptionGroup(result);
						return result;
					});
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
				builder.required((group == null) && optionSettings.isRequired());

				Option option = builder.build();

				if (group != null) {
					group.addOption(option);
				} else {
					options.addOption(option);
				}
			});

		return options;
	}

	//

	private static final String[]	COPYRIGHT_LINES					= {
		"Copyright (c) Contributors to the Eclipse Foundation",
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

	public boolean hasOption(AppOption option) {
		return getParsedArgs().hasOption(option.getShortTag());
	}

	public static boolean DO_NORMALIZE = true;

	public String getOptionValue(AppOption option) {
		return getOptionValue(option, !DO_NORMALIZE);
	}

	public String getOptionValue(AppOption option, boolean normalize) {
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

	public String[] getOptionValues(AppOption option) {
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
			for (LoggerProperty loggerProperty : LoggerProperty
				.values()) {
				helpWriter.println("  [ " + loggerProperty.getPropertyName() + " ]");
			}

			helpWriter.flush();
		}
	}

	//

	/**
	 * Load properties for the specified rule option. Answer an empty collection
	 * if the rule option was not provided.
	 *
	 * Options loading tries {@link #getOptionValue(AppOption)} then tries
	 * {@link #getDefaultReference(AppOption)}. An empty collection is returned
	 * when neither is available.
	 *
	 * Orphaned values are
	 *
	 * @param ruleOption The option for which to load properties.
	 * @param orphanedValues Values which have been orphaned by merges.
	 * @return Properties loaded using the reference set for the option.
	 *
	 * @throws IOException Thrown if the load failed.
	 *
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

	String relativize(String relativeRef, String baseRef) {
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

	public UTF8Properties loadExternalProperties
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

	/**
	 * Detect orphaned and un-orphaned property assignments.
	 *
	 * An orphaned property assignment occurs when a new property
	 * assignment changes the assigned property value.
	 *
	 * An orphaned value become un-orphaned if a property assignment
	 * has that value.
	 *
	 * @param sourceName A name associated with the properties which were added.
	 *     Used for logging.
	 * @param sinkName A name associated with the properties into which the source
	 *     properties were added.  Used for logging.
	 * @param key The key which was overridden by the source properties.
	 * @param oldValue The value previously assigned to the key in the sink properties.
	 * @param newValue The value newly assigned to the key in the sink properties.
	 * @param orphans Accumulated sink property values which were orphaned.
	 */
	protected void processOrphan(
		String sourceName, String sinkName,
		String key, String oldValue, String newValue,
		Set<String> orphans) {

		if ( (oldValue != null) && oldValue.equals(newValue) ) {
			return; // Nothing to do: The old and new assignments are the same.
		}

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

	protected void logMerge(String propertyName,
		String sourceName, String sinkName,
		Object key, Object oldValue, Object newValue) {

		if (oldValue != null) {
			dual_debug("Under property [ %s ]:" +
				" Merge of [ %s ] into [ %s ]," +
				" key [ %s ] replaces value [ %s ] with [ %s ]",
				propertyName,
				sourceName, sinkName,
				key, oldValue, newValue);
		}
	}

	protected void logMerge(String sourceName, String sinkName, Object key, Object oldValue, Object newValue) {
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
			return new ImmediateRuleData[] {
				// EMPTY
			};
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



	//

	Logger logger;

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
		return new TransformOptions(this);
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

		options.setActions();

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
