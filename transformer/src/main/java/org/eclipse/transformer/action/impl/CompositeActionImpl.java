/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.transformer.action.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.CompositeAction;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;

public class CompositeActionImpl extends ActionImpl implements CompositeAction {

	public <A extends ActionImpl> A addUsing(ActionInit<A> init) {
		A action = createUsing(init);
		addAction(action);
		return action;
	}

	public CompositeActionImpl(
		Logger logger,
		boolean isTerse, boolean isVerbose,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule,
		SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);

		this.actions = new ArrayList<ActionImpl>();
		this.acceptedAction = null;
	}

	//

	@Override
	public String getName() {
		return ( (acceptedAction == null) ? null : acceptedAction.getName() );
	}

	@Override
	public ActionType getActionType() {
		return ( (acceptedAction == null) ? null : acceptedAction.getActionType() );
	}

	@Override
	public ChangesImpl getLastActiveChanges() {
		return ( (acceptedAction == null) ? null : acceptedAction.getLastActiveChanges() );
	}

	@Override
	public ChangesImpl getActiveChanges() {
		return ( (acceptedAction == null) ? null : acceptedAction.getActiveChanges() );
	}

	@Override
	protected ChangesImpl newChanges() {
		// Invoked by 'ActionImpl.init(): A return value must be provided.
		return null;
	}

	//

	private final List<ActionImpl> actions;
	private ActionImpl acceptedAction;

	@Override
	public List<ActionImpl> getActions() {
		return actions;
	}

	protected void addAction(ActionImpl action) {
		getActions().add(action);
	}

	@Override
	public String getAcceptExtension() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ActionImpl acceptAction(String resourceName, File resourceFile) {
		for ( ActionImpl action : getActions() ) {
			if ( action.accept(resourceName, resourceFile) ) {
				acceptedAction = action;
				return action;
			}
		}
		acceptedAction = null;
		return null;
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return ( acceptAction(resourceName, resourceFile) != null );
	}

	@Override
	public ActionImpl getAcceptedAction() {
		return ( (acceptedAction == null) ? null : acceptedAction );
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength)
		throws TransformException {

		return getAcceptedAction().apply(inputName, inputBytes, inputLength);
	}
}
