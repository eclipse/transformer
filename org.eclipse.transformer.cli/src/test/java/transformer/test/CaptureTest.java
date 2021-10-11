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

package transformer.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.transformer.action.BundleData;
import org.eclipse.transformer.action.impl.InputBufferImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.slf4j.Logger;

import transformer.test.util.CaptureLoggerImpl;

public class CaptureTest {
	public CaptureLoggerImpl captureLogger;

	public CaptureLoggerImpl createLogger() {
		return new CaptureLoggerImpl("Test");
	}

	public CaptureLoggerImpl getCaptureLogger() {
		if (captureLogger == null) {
			captureLogger = createLogger();
		}
		return captureLogger;
	}

	public List<? extends CaptureLoggerImpl.LogEvent> consumeCapturedEvents() {
		if (captureLogger != null) {
			List<? extends CaptureLoggerImpl.LogEvent> capturedEvents = captureLogger.consumeCapturedEvents();
			System.out.println("Cleared [ " + capturedEvents.size() + " ] events");
			return capturedEvents;

		} else {
			return Collections.emptyList();
		}
	}

	public InputBufferImpl createBuffer() {
		return new InputBufferImpl();
	}

	//

	public SelectionRuleImpl createSelectionRule(Logger useLogger, Set<String> useIncludes, Set<String> useExcludes) {

		return new SelectionRuleImpl(useLogger, useIncludes, useExcludes);
	}

	public SignatureRuleImpl createSignatureRule(Logger useLogger, Map<String, String> usePackageRenames,
		Map<String, String> usePackageVersions, Map<String, BundleData> bundleData, Map<String, String> directStrings,
		Map<String, Map<String,String>> perClass) {

		return new SignatureRuleImpl(useLogger, usePackageRenames, usePackageVersions, null,
					bundleData, null, directStrings, perClass);
	}
}
