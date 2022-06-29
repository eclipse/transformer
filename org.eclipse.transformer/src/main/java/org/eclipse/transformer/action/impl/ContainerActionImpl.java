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

import java.io.File;
import java.util.function.Function;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionSelector;
import org.eclipse.transformer.action.ContainerAction;

/**
 * Action type used to transform a collection of resources.
 * <p>
 * Container actions share responsibility with selection actions for
 * transforming resource containers. Responsibility is divided, with
 * {@link ActionSelectorImpl} being responsible for selecting and applying a
 * single action to a single specified resource, and with
 * {@link ContainerActionImpl} responsible for invoking its selection action on
 * a collection of resources.
 * <p>
 * Container actions currently have two main varieties: Directory actions and
 * ZIP (archive) actions. While several kinds of ZIP actions exist, these are
 * minor variations of the basic ZIP action.
 * <p>
 * The main difference between the directory and ZIP actions is that the
 * directory action knows how to recursively walk a target directory. Zip
 * actions know how to open and iterate across the elements of the target
 * archive.
 */
public abstract class ContainerActionImpl extends ActionImpl implements ContainerAction {

	public ContainerActionImpl(ActionContext context, ActionSelector actionSelector) {
		super(context);
		this.actionSelector = actionSelector;
	}

	public ContainerActionImpl(ActionContext context) {
		this(context, new ActionSelectorImpl());
	}

	public <ACTION extends Action> ACTION addUsing(Function<? super ActionContext, ACTION> init) {
		return getActionSelector().addUsing(init, getContext());
	}

	//

	private final ActionSelector actionSelector;

	@Override
	public ActionSelector getActionSelector() {
		return actionSelector;
	}

	//

	@Override
	protected ContainerChangesImpl newChanges() {
		return new ContainerChangesImpl();
	}

	@Override
	public ContainerChangesImpl getActiveChanges() {
		return (ContainerChangesImpl) super.getActiveChanges();
	}

	@Override
	public ContainerChangesImpl getLastActiveChanges() {
		return (ContainerChangesImpl) super.getLastActiveChanges();
	}

	protected void recordUnaccepted(String resourceName) {
		getLogger().debug("Resource [ {} ]: Not accepted", resourceName);
		getActiveChanges().recordUnaccepted();
	}

	protected void recordUnselected(String resourceName) {
		getLogger().debug("Resource [ {} ]: Not selected", resourceName);
		getActiveChanges().recordUnselected();
	}

	protected void recordUnchanged(Action action, String resourceName) {
		getLogger().debug("Resource [ {} ]: Action [ {} ]: Not changed", action.getName(), resourceName);
		getActiveChanges().recordUnchanged(action);
	}

	protected void recordAction(Action action, String resourceName) {
		getLogger().debug("Resource [ {} ]: Action [ {} ]", action.getName(), resourceName);
		getActiveChanges().recordAction(action);
		if (action.isContainerAction()) {
			getActiveChanges().add(((ContainerAction) action).getLastActiveChanges());
		}
	}

	protected void recordError(Action action, String resourceName, Throwable error) {
		String actionName = ( (action == null) ? "null" : action.getName() );
		getLogger().error("Resource [ " + resourceName + " ] Action [ " + actionName + " ]: Failed transform", error);
		getActiveChanges().recordFailed(action);
	}

	protected void recordDuplicate(Action action, String resourceName) {
		String actionName = ( (action == null) ? "null" : action.getName() );
		getLogger().error("Resource [ {} ] Action [ {} ]: Duplicate", resourceName, actionName);
		getActiveChanges().recordDuplicated(action);
	}

	@Override
	public abstract void apply(String inputName, File inputFile, String outputName, File outputFile);
}
