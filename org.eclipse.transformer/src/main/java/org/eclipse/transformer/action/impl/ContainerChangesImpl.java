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
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.ContainerChanges;
import org.slf4j.Logger;

public class ContainerChangesImpl extends ChangesImpl implements ContainerChanges {

	public ContainerChangesImpl() {
		super();

		this.unchangedByAction = new HashMap<>();
		this.changedByAction = new HashMap<>();
		this.renamedByAction = new HashMap<>();
		this.contentChangedByAction = new HashMap<>();
		this.failedByAction = new HashMap<>();
		this.duplicatedByAction = new HashMap<>();

		this.allResources = 0;

		this.allUnselected = 0;
		this.allSelected = 0;

		this.allUnaccepted = 0;
		this.allAccepted = 0;

		this.allUnchanged = 0;
		this.allChanged = 0;

		this.allRenamed = 0;
		this.allContentChanged = 0;

		this.allFailed = 0;
		this.allDuplicated = 0;
	}

	@Override
	public boolean isContentChanged() {
		return (getAllChanged() != 0);
	}

	@Override
	public String toString() {
		return String.format("%s [%s]: [%d:%d]", getInputResourceName(), getChangeText(), getAllResources(),
			getAllChanged());
	}

	//

	private final Map<String, int[]>	unchangedByAction;
	private final Map<String, int[]>	changedByAction;
	private final Map<String, int[]>	renamedByAction;
	private final Map<String, int[]>	contentChangedByAction;
	private final Map<String, int[]>	failedByAction;
	private final Map<String, int[]>	duplicatedByAction;

	private int							allResources;
	private int							allUnselected;
	private int							allSelected;
	private int							allUnaccepted;
	private int							allAccepted;

	private int							allUnchanged;
	private int							allChanged;
	private int							allFailed;
	private int							allDuplicated;

	private int							allRenamed;
	private int							allContentChanged;

	//

	@Override
	public Set<String> getActionNames() {
		Set<String> unchangedNames = unchangedByAction.keySet();
		Set<String> changedNames = contentChangedByAction.keySet();

		Set<String> allNames = new HashSet<>(unchangedNames.size() + changedNames.size());

		allNames.addAll(unchangedNames);
		allNames.addAll(changedNames);

		return allNames;
	}

	//

	private void increment(Map<String, int[]> counter, String name) {
		int[] count = counter.get(name);
		if (count == null) {
			count = new int[] {
				1
			};
			counter.put(name, count);
		} else {
			count[0]++;
		}
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

	@Override
	public void add(ContainerChanges otherChanges) {
		addChangeMap(this.unchangedByAction, otherChanges.getUnchangedByAction());
		addChangeMap(this.changedByAction, otherChanges.getChangedByAction());

		addChangeMap(this.failedByAction, otherChanges.getFailedByAction());
		addChangeMap(this.duplicatedByAction, otherChanges.getDuplicatedByAction());

		addChangeMap(this.renamedByAction, otherChanges.getRenamedByAction());
		addChangeMap(this.contentChangedByAction, otherChanges.getChangedByAction());

		this.allResources += otherChanges.getAllResources();
		this.allUnselected += otherChanges.getAllUnselected();
		this.allUnaccepted += otherChanges.getAllUnaccepted();
		this.allAccepted += otherChanges.getAllAccepted();
		this.allUnchanged += otherChanges.getAllUnchanged();
		this.allChanged += otherChanges.getAllChanged();

		this.allFailed += otherChanges.getAllFailed();
		this.allDuplicated += otherChanges.getAllDuplicated();

		this.allContentChanged += otherChanges.getAllChanged();
		this.allRenamed += otherChanges.getAllRenamed();
	}

	//

	@Override
	public Map<String, int[]> getUnchangedByAction() {
		return Collections.unmodifiableMap(unchangedByAction);
	}

	@Override
	public Map<String, int[]> getChangedByAction() {
		return Collections.unmodifiableMap(changedByAction);
	}

	@Override
	public Map<String, int[]> getRenamedByAction() {
		return Collections.unmodifiableMap(renamedByAction);
	}

	@Override
	public Map<String, int[]> getContentChangedByAction() {
		return Collections.unmodifiableMap(contentChangedByAction);
	}

	@Override
	public Map<String, int[]> getFailedByAction() {
		return Collections.unmodifiableMap(failedByAction);
	}

	@Override
	public Map<String, int[]> getDuplicatedByAction() {
		return Collections.unmodifiableMap(duplicatedByAction);
	}

	//

	@Override
	public int getAllResources() {
		return allResources;
	}

	@Override
	public int getAllUnaccepted() {
		return allUnaccepted;
	}

	@Override
	public int getAllAccepted() {
		return allAccepted;
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
	public int getAllRenamed() {
		return allRenamed;
	}

	@Override
	public int getAllContentChanged() {
		return allContentChanged;
	}

	@Override
	public int getAllFailed() {
		return allFailed;
	}

	@Override
	public int getAllDuplicated() {
		return allDuplicated;
	}

	//

	@Override
	public int getUnchanged(Action action) {
		return getUnchanged(action.getName());
	}

	@Override
	public int getUnchanged(String name) {
		int[] changes = unchangedByAction.get(name);
		return ((changes == null) ? 0 : changes[0]);
	}

	@Override
	public int getChanged(Action action) {
		return getChanged(action.getName());
	}

	@Override
	public int getChanged(String name) {
		int[] changes = changedByAction.get(name);
		return ((changes == null) ? 0 : changes[0]);
	}

	@Override
	public int getRenamed(Action action) {
		return getRenamed(action.getName());
	}

	@Override
	public int getRenamed(String name) {
		int[] renamed = renamedByAction.get(name);
		return ((renamed == null) ? 0 : renamed[0]);
	}

	@Override
	public int getContentChanged(Action action) {
		return getContentChanged(action.getName());
	}

	@Override
	public int getContentChanged(String name) {
		int[] changes = contentChangedByAction.get(name);
		return ((changes == null) ? 0 : changes[0]);
	}

	@Override
	public int getFailed(Action action) {
		return getFailed(action.getName());
	}

	@Override
	public int getFailed(String name) {
		int[] failed = failedByAction.get(name);
		return ((failed == null) ? 0 : failed[0]);
	}

	@Override
	public int getDuplicated(Action action) {
		return getDuplicated(action.getName());
	}

	@Override
	public int getDuplicated(String name) {
		int[] duplicated = duplicatedByAction.get(name);
		return ((duplicated == null) ? 0 : duplicated[0]);
	}

	//

	@Override
	public void recordUnselected() {
		allResources++;
		allUnselected++;
	}

	@Override
	public void recordUnaccepted() {
		allResources++;
		allSelected++;
		allUnaccepted++;
	}

	private void recordAccepted() {
		allResources++;
		allSelected++;
		allAccepted++;
	}

	@Override
	public void recordUnchanged(Action action) {
		recordAccepted();

		allUnchanged++;
		increment(unchangedByAction, action.getName());
	}

	@Override
	public void recordAction(Action action) {
		recordAccepted();

		String name = action.getName();

		boolean anyChanges = false;

		Changes lastChanges = action.getLastActiveChanges();
		if (lastChanges.isRenamed()) {
			anyChanges = true;
			allRenamed++;
			increment(renamedByAction, name);
		}
		Map<String, int[]> target;
		if (lastChanges.isContentChanged()) {
			anyChanges = true;
			allContentChanged++;
			increment(contentChangedByAction, name);
		}

		if (anyChanges) {
			allChanged++;
			increment(changedByAction, name);
		} else {
			allUnchanged++;
			increment(unchangedByAction, name);
		}
	}

	// TODO: Need a better way to handle this:
	//
	// Failures and duplications can occur on un-accepted
	// actions. Handle this, for now, by setting the action
	// name to "null".
	//
	// See issue #297

	@Override
	public void recordFailed(Action action) {
		recordAccepted();

		String name = ((action == null) ? "null" : action.getName());

		allUnchanged++;
		increment(unchangedByAction, name);

		allFailed++;
		increment(failedByAction, name);
	}

	@Override
	public void recordDuplicated(Action action) {
		recordAccepted();

		String name = ((action == null) ? "null" : action.getName());

		allUnchanged++;
		increment(unchangedByAction, name);

		allDuplicated++;
		increment(duplicatedByAction, name);
	}

	//

	private static final String	DASH_LINE		= "================================================================================";
	private static final String	SMALL_DASH_LINE	= "--------------------------------------------------------------------------------";

	private static final String	DATA_LINE		= "[ %14s ] [ %6s ] %10s [ %6s ] %10s [ %6s ]%s";

	private String formatData(Object... parms) {
		return String.format(DATA_LINE, parms);
	}

	@Override
	public void log(Logger logger, String inputPath, String outputPath) {
		if (logger.isDebugEnabled(consoleMarker)) {
			logger.info(consoleMarker, DASH_LINE);
		}
		super.log(logger, inputPath, outputPath);
	}

	@Override
	public void logChanges(Logger logger) {
		if (logger.isInfoEnabled(consoleMarker)) {
			displaySummary(logger);
		}
		if (logger.isDebugEnabled(consoleMarker)) {
			displayActions(logger);
		}
		logger.debug(consoleMarker, DASH_LINE);
	}

	private void displaySummary(Logger logger) {
		logger.debug(consoleMarker, DASH_LINE);
		logger.debug(consoleMarker, "[ Summary ]");
		logger.debug(consoleMarker, SMALL_DASH_LINE);

		logger.info(consoleMarker,
			formatData("All Resources", getAllResources(),
				       "Unaccepted", getAllUnaccepted(),
				       "Accepted", getAllAccepted(), ""));

		logger.debug(consoleMarker,
			formatData("All Accepted", getAllAccepted(),
				       "Unselected", getAllUnselected(),
				       "Selected", getAllSelected(), ""));

		logger.debug(consoleMarker,
			formatData("All Selected", getAllAccepted(),
				       "Unchanged", getAllUnchanged(),
				       "Changed", getAllChanged(), ""));

		logger.info(consoleMarker,
			formatData("All Unchanged", getAllUnchanged(),
				       "Failed", getAllFailed(),
				       "Duplicated", getAllDuplicated(), ""));

		logger.info(consoleMarker,
			formatData("All Changed", getAllChanged(),
				       "Renamed", getAllRenamed(),
				       "Content", getAllContentChanged(), ""));
	}

	private void displayActions(Logger logger) {
		logger.debug(consoleMarker, DASH_LINE);
		logger.debug(consoleMarker, "[ Actions ]");
		logger.debug(consoleMarker, SMALL_DASH_LINE);

		for (String actionName : getActionNames()) {
			int useUnchangedByAction = getUnchanged(actionName);
			int useChangedByAction = getChanged(actionName);
			logger.debug(consoleMarker,
				formatData(actionName,
					useUnchangedByAction + useChangedByAction,
					"Unchanged", useUnchangedByAction,
					"Changed", useChangedByAction, ""));
		}
	}
}
