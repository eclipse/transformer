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
import java.util.List;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.CompositeAction;
import org.eclipse.transformer.action.ContainerAction;
import org.eclipse.transformer.action.ContainerChanges;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

import aQute.lib.io.ByteBufferOutputStream;

public abstract class ContainerActionImpl extends ActionImpl<ContainerChangesImpl> implements ContainerAction {

	public <A extends Action> A addUsing(ActionInit<A> init) {
		A action = createUsing(init);
		addAction(action);
		return action;
	}

	public ContainerActionImpl(Logger logger, InputBuffer buffer, SelectionRule selectionRule,
		SignatureRule signatureRule) {
		super(logger, buffer, selectionRule, signatureRule);
		this.compositeAction = createUsing(CompositeActionImpl::new);
	}

	public ContainerActionImpl(CompositeAction compositeAction) {
		super(compositeAction.getLogger(), compositeAction.getBuffer(), compositeAction.getSelectionRule(),
			compositeAction.getSignatureRule());
		this.compositeAction = compositeAction;
	}

	//

	private final CompositeAction compositeAction;

	@Override
	public CompositeAction getAction() {
		return compositeAction;
	}

	public void addAction(Action action) {
		getAction().addAction(action);
	}

	@Override
	public List<Action> getActions() {
		return getAction().getActions();
	}

	@Override
	public String getAcceptExtension() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Action acceptAction(String resourceName) {
		return acceptAction(resourceName, null);
	}

	@Override
	public Action acceptAction(String resourceName, File resourceFile) {
		return getAction().acceptAction(resourceName, resourceFile);
	}

	//

	@Override
	protected ContainerChangesImpl newChanges() {
		return new ContainerChangesImpl();
	}

	//

	protected void recordUnaccepted(String resourceName) {
		getLogger().debug("Resource [ {} ]: Not accepted", resourceName);

		getActiveChanges().record();
	}

	protected void recordUnselected(Action action, String resourceName) {
		getLogger().debug("Resource [ {} ] Action [ {} ]: Accepted but not selected", resourceName, action.getName());

		getActiveChanges().record(action, !ContainerChanges.HAS_CHANGES);
	}

	protected void recordTransform(Action action, String resourceName) {
		getLogger().debug("Resource [ {} ] Action [ {} ]: Changes [ {} ]", resourceName, action.getName(),
			action.hadChanges());

		getActiveChanges().record(action);
	}

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());
		apply(inputData.name(), inputData.stream(), inputData.length(), outputStream);

		if (!getLastActiveChanges().hasChanges()) {
			return inputData;
		}

		ByteData outputData = new ByteDataImpl(inputData.name(), outputStream.toByteBuffer());
		return outputData;
	}
}
