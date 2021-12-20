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

package transformer.test.data;

// This annotation value should be transformed
@Sample_Annotation(value1 = "javax.servlet.Servlet", value2 = "javax.servlet.Servlet=MyServlet,javax.servlet.Listener=MyListener")
public class Sample_DirectOverride {

	// Five cases are tested:
	//
	// (1) A class reference.
	// (2) A class name as a string constant.
	// (3) Multiple class names within a string constant.
	// (4) A class name as an embedded (used by reference) string constant
	// (5) A class name as an annotation value.

	// Three updates may trigger:
	// (1) The package rename based rule.
	// (2) The direct string rule.
	// (3) The per-class direct string rule.

	// This is only updated by the package rename based rule.
	public static final javax.servlet.Servlet	sampleServlet	= null;

	// This will be updated by all three rules.
	public static final String SAMPLE_CLASS_NAME = "javax.servlet.Servlet";

	// This will be updated by the package rename based rule and the
	// per-class rule. The direct rule should not trigger, because that
	// requires an exact match.
	public static final String					SAMPLE_STRING	= "javax.servlet.Servlet=MyServlet,javax.servlet.Listener=MyListener";

	// This will be transformed!
	// The string constant is inlined.
	public static final String SAMPLE_STRING_REFERENCE = Sample_StringConstant.value;
}
