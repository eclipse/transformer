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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.CompositeAction;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;

public class CompositeActionImpl extends ActionImpl<Changes> implements CompositeAction {

	public <A extends Action> A addUsing(ActionInit<A> init) {
		A action = createUsing(init);
		addAction(action);
		return action;
	}

	public CompositeActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule,
		SignatureRule signatureRule) {

		super(logger, buffer, selectionRule, signatureRule);

		this.actions = new ArrayList<>();
		this.acceptedAction = null;
	}

	//

	@Override
	public String getName() {
		return ((acceptedAction == null) ? null : acceptedAction.getName());
	}

	@Override
	public ActionType getActionType() {
		return ((acceptedAction == null) ? null : acceptedAction.getActionType());
	}

	@Override
	public Changes getLastActiveChanges() {
		return ((acceptedAction == null) ? null : acceptedAction.getLastActiveChanges());
	}

	@Override
	public Changes getActiveChanges() {
		return ((acceptedAction == null) ? null : acceptedAction.getActiveChanges());
	}

	@Override
	protected Changes newChanges() {
		// Invoked by 'ActionImpl.init(): A return value must be provided.
		return null;
	}

	//

	private final List<Action>	actions;
	private Action				acceptedAction;

	@Override
	public List<Action> getActions() {
		return actions;
	}

	protected void addAction(Action action) {
		getActions().add(action);
	}

	@Override
	public String getAcceptExtension() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Action acceptAction(String resourceName, File resourceFile) {
		for (Action action : getActions()) {
			if (action.accept(resourceName, resourceFile)) {
				acceptedAction = action;
				return action;
			}
		}
		acceptedAction = null;
		return null;
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return (acceptAction(resourceName, resourceFile) != null);
	}

	@Override
	public Action getAcceptedAction() {
		return ((acceptedAction == null) ? null : acceptedAction);
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws TransformException {
		return ((ActionImpl<?>) getAcceptedAction()).apply(inputName, inputBytes, inputLength);
	}
}
