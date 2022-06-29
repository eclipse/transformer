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

import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.SignatureRule;

/**
 * Action for xml resources.
 * <p>
 * This action does content transformation. This action performs text updates
 * to xml file lines.
 */
public class XmlActionImpl extends TextActionImpl {

	public XmlActionImpl(ActionContext context) {
		super(context);
	}

	@Override
	protected List<StringReplacement> createActiveReplacements(SignatureRule signatureRule) {
		List<StringReplacement> replacements = super.createActiveReplacements(signatureRule);
		if ( !signatureRule.getPackageRenames().isEmpty() ) {
			replacements.add(this::packagesUpdate);
			replacements.add(this::binaryPackagesUpdate);
		}
		// Do NOT add direct-per-class updates.
		if ( !signatureRule.getDirectGlobalUpdates().isEmpty() ) {
			replacements.add(this::directGlobalUpdate);
		}
		return replacements;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.XML;
	}

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		return acceptExtension(resourceName, resourceFile);
	}
}
