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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.Action.ActionInit;
import org.eclipse.transformer.action.Action.ActionInitData;
import org.eclipse.transformer.action.ActionSelector;

/**
 * Action selector.
 * <p>
 * A selector is populated with a list of actions.
 * <p>
 * The name <em>CompositeActionImpl</em> is a historical artifact. This type is
 * no longer an action implementation.
 */
public class ActionSelectorImpl implements ActionSelector {

	public ActionSelectorImpl() {
		this.actions = new ArrayList<>();
	}

	//

	public <A extends Action> A createUsing(ActionInit<A> init, ActionInitData initData) {
		return init.apply(initData);
	}

	public <A extends Action> A addUsing(ActionInit<A> init, ActionInitData initData) {
		A action = createUsing(init, initData);
		addAction(action);
		return action;
	}

	//

	private final List<Action> actions;

	@Override
	public List<Action> getActions() {
		return actions;
	}
}
