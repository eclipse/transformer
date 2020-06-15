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

package org.eclipse.transformer.action.impl;

import org.eclipse.transformer.action.ActionType;
import org.slf4j.Logger;

public class ZipActionImpl extends ContainerActionImpl {

	public ZipActionImpl(Logger logger, boolean isTerse, boolean isVerbose, InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "Zip Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.ZIP;
	}

	@Override
	public String getAcceptExtension() {
		return ".zip";
	}
}
