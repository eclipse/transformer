/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package org.eclipse.transformer.action.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.InputBuffer;
import org.eclipse.transformer.action.SelectionRule;
import org.eclipse.transformer.action.SignatureRule;
import org.slf4j.Logger;

public class ActionContextImpl implements ActionContext {
	public ActionContextImpl(Logger logger, InputBuffer inputBuffer, SelectionRule selectionRule, SignatureRule signatureRule) {
		this.logger = requireNonNull(logger);
		this.inputBuffer = requireNonNull(inputBuffer);
		this.selectionRule = requireNonNull(selectionRule);
		this.signatureRule = requireNonNull(signatureRule);
	}

	private final Logger logger;
	private final InputBuffer inputBuffer;
	private final SelectionRule selectionRule;
	private final SignatureRule signatureRule;

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public InputBuffer getBuffer() {
		return inputBuffer;
	}

	@Override
	public SelectionRule getSelectionRule() {
		return selectionRule;
	}

	@Override
	public SignatureRule getSignatureRule() {
		return signatureRule;
	}
}
