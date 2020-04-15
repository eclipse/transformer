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

import org.eclipse.transformer.action.ActionType;
import org.slf4j.Logger;

public class RarActionImpl extends ContainerActionImpl {
	public RarActionImpl(
		Logger logger, boolean isTerse, boolean isVerbose,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
	}

	//

	public String getName() {
		return "RAR Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.RAR;
	}

	@Override
	public String getAcceptExtension() {
		return ".rar";
	}
}
