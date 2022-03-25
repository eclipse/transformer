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

import static org.eclipse.transformer.Transformer.consoleMarker;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ContainerChanges;
import org.slf4j.Logger;

public class ContainerChangesImpl extends ChangesImpl implements ContainerChanges {

	protected ContainerChangesImpl() {
		super();

		this.changedByAction = new HashMap<>();
		this.unchangedByAction = new HashMap<>();

		this.allChanged = 0;
		this.allUnchanged = 0;

		this.allSelected = 0;
		this.allUnselected = 0;
		this.allResources = 0;

		this.allNestedChanges = null;
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return allChanged > 0;
	}

	@Override
	public void clearChanges() {
		changedByAction.clear();
		unchangedByAction.clear();

		allChanged = 0;
		allUnchanged = 0;

		allSelected = 0;
		allUnselected = 0;
		allResources = 0;

		allNestedChanges = null;

		super.clearChanges();
	}

	//

	private final Map<String, int[]>	changedByAction;
	private final Map<String, int[]>	unchangedByAction;

	private int							allUnchanged;
	private int							allChanged;

	private int							allSelected;
	private int							allUnselected;
	private int							allResources;

	//

	@Override
	public Set<String> getActionNames() {
		Set<String> changedNames = changedByAction.keySet();
		Set<String> unchangedNames = unchangedByAction.keySet();

		Set<String> allNames = new HashSet<>(changedNames.size() + unchangedNames.size());

		allNames.addAll(changedNames);
		allNames.addAll(unchangedNames);

		return allNames;
	}

	//

	@Override
	public Map<String, int[]> getChangedByAction() {
		return Collections.unmodifiableMap(changedByAction);
	}

	@Override
	public Map<String, int[]> getUnchangedByAction() {
		return Collections.unmodifiableMap(unchangedByAction);
	}

	//

	@Override
	public int getAllResources() {
		return allResources;
	}

	@Override
	public int getAllUnselected() {
		return allUnselected;
	}

	@Override
	public int getAllSelected() {
		return allSelected;
	}

	@Override
	public int getAllUnchanged() {
		return allUnchanged;
	}

	@Override
	public int getAllChanged() {
		return allChanged;
	}

	@Override
	public int getChanged(Action action) {
		return getChanged(action.getName());
	}

	@Override
	public int getChanged(String name) {
		int[] changes = changedByAction.get(name);
		return (changes == null) ? 0 : changes[0];
	}

	@Override
	public int getUnchanged(Action action) {
		return getUnchanged(action.getName());
	}

	@Override
	public int getUnchanged(String name) {
		int[] changes = unchangedByAction.get(name);
		return (changes == null) ? 0 : changes[0];
	}

	@Override
	public void record(Action action) {
		record(action.getName(), action.hadChanges());

		action.getLastActiveChanges()
			.addNestedInto(this);
	}

	@Override
	public void record(Action action, boolean hasChanges) {
		record(action.getName(), hasChanges);
	}

	@Override
	public void record(String name, boolean hasChanges) {
		allResources++;
		allSelected++;

		Map<String, int[]> target;
		if (hasChanges) {
			allChanged++;
			target = changedByAction;
		} else {
			allUnchanged++;
			target = unchangedByAction;
		}

		int[] changes = target.get(name);
		if (changes == null) {
			changes = new int[] {
				1
			};
			target.put(name, changes);
		} else {
			changes[0]++;
		}
	}

	@Override
	public void record() {
		allResources++;
		allUnselected++;
	}

	@Override
	public void addNestedInto(ContainerChanges containerChanges) {
		containerChanges.addNested(this);
	}

	//

	private ContainerChangesImpl allNestedChanges;

	@Override
	public boolean hasNestedChanges() {
		return allNestedChanges != null;
	}

	@Override
	public ContainerChangesImpl getNestedChanges() {
		return allNestedChanges;
	}

	/**
	 * Add other changes as nested changes. Both the immediate part of the other
	 * changes and the nested part of the other changes are added.
	 *
	 * @param otherChanges Other container changes to add as nested changes.
	 */
	@Override
	public void addNested(ContainerChanges otherChanges) {
		if (allNestedChanges == null) {
			allNestedChanges = new ContainerChangesImpl();
		}
		allNestedChanges.add(otherChanges);

		ContainerChanges otherNestedChanges = otherChanges.getNestedChanges();
		if (otherNestedChanges != null) {
			allNestedChanges.add(otherNestedChanges);
		}
	}

	@Override
	public void add(ContainerChanges otherChanges) {
		addChangeMap(this.changedByAction, otherChanges.getChangedByAction());
		addChangeMap(this.unchangedByAction, otherChanges.getUnchangedByAction());

		this.allChanged += otherChanges.getAllChanged();
		this.allUnchanged += otherChanges.getAllUnchanged();

		this.allSelected += otherChanges.getAllSelected();
		this.allUnselected += otherChanges.getAllUnselected();
		this.allResources += otherChanges.getAllResources();
	}

	private void addChangeMap(Map<String, int[]> thisChangeMap, Map<String, int[]> otherChangeMap) {
		int[] nextChanges = new int[1];
		for (Map.Entry<String, int[]> mapEntry : otherChangeMap.entrySet()) {
			int[] thisChanges = thisChangeMap.putIfAbsent(mapEntry.getKey(), nextChanges);
			if (thisChanges == null) {
				thisChanges = nextChanges;
				nextChanges = new int[1];
			}
			thisChanges[0] += mapEntry.getValue()[0];
		}
	}

	//

	private static final String	DASH_LINE		= "================================================================================";
	private static final String	SMALL_DASH_LINE	= "--------------------------------------------------------------------------------";

	private static final String	DATA_LINE		= "[ %22s ] [ %6s ] %10s [ %6s ] %8s [ %6s ]%s";

	private String formatData(Object... parms) {
		return String.format(DATA_LINE, parms);
	}

	private void displayChanges(Logger logger) {
		logger.debug(consoleMarker,
			formatData("All Resources", getAllResources(), "Unselected", getAllUnselected(), "Selected",
			getAllSelected(), ""));

		logger.debug(consoleMarker, SMALL_DASH_LINE);
		logger.debug(consoleMarker,
			formatData("All Actions", getAllSelected(), "Unchanged", getAllUnchanged(), "Changed",
			getAllChanged(), ""));

		for (String actionName : getActionNames()) {
			int useUnchangedByAction = getUnchanged(actionName);
			int useChangedByAction = getChanged(actionName);
			logger.debug(consoleMarker, formatData(actionName, useUnchangedByAction + useChangedByAction, "Unchanged",
				useUnchangedByAction, "Changed", useChangedByAction, ""));
		}
	}

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isDebugEnabled(consoleMarker)) {
			logger.debug(consoleMarker, DASH_LINE);

			logger.debug(consoleMarker, "[ Input  ] [ {} ]", getInputResourceName());
			logger.debug(consoleMarker, "           [ {} ]", inputPath);
			logger.debug(consoleMarker, "[ Output ] [ {} ]", getOutputResourceName());
			logger.debug(consoleMarker, "           [ {} ]", outputPath);
			logger.debug(consoleMarker, DASH_LINE);

			logger.debug(consoleMarker, "[ Immediate changes: ]");
			logger.debug(consoleMarker, SMALL_DASH_LINE);
			displayChanges(logger);
			logger.debug(consoleMarker, DASH_LINE);

			if (allNestedChanges != null) {
				logger.debug(consoleMarker, "[ Nested changes: ]");
				logger.debug(consoleMarker, SMALL_DASH_LINE);
				allNestedChanges.displayChanges(logger);
				logger.debug(consoleMarker, DASH_LINE);
			}
		} else if (logger.isInfoEnabled(consoleMarker)) {
			if (!inputPath.equals(outputPath)) {
				logger.info(consoleMarker, "Input [ {} ] as [ {} ]: {}", inputPath, outputPath, getChangeTag());
			}

			logger.info(consoleMarker, formatData("All Resources", getAllResources(), "Unselected", getAllUnselected(),
				"Selected", getAllSelected(), ""));
			logger.info(consoleMarker, formatData("All Actions", getAllSelected(), "Unchanged", getAllUnchanged(),
				"Changed", getAllChanged(), ""));
			if (allNestedChanges != null) {
				logger.info(consoleMarker,
					formatData("Nested Resources", allNestedChanges.getAllResources(), "Unselected",
						allNestedChanges.getAllUnselected(), "Selected", allNestedChanges.getAllSelected(), ""));
				logger.info(consoleMarker, formatData("Nested Actions", allNestedChanges.getAllSelected(), "Unchanged",
					allNestedChanges.getAllUnchanged(), "Changed", allNestedChanges.getAllChanged(), ""));
			}
		}
	}
}
