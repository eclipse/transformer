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

package javax.servlet;

import javax.servlet.Sample_Annotation;
import javax.servlet.data.Sample_StringConstant;

@Sample_Annotation(
	value1 = "javax.servlet.Servlet",
	value2 = "javax.servlet.Servlet=MyServlet,javax.servlet.Listener=MyListener")
public class Sample {
	public static final javax.servlet.Servlet sampleServlet = null;
	public static final String SAMPLE_CLASS_NAME = "javax.servlet.Servlet";
	public static final String SAMPLE_STRING = "javax.servlet.Servlet=MyServlet,javax.servlet.Listener=MyListener";
	public static final String SAMPLE_STRING_REFERENCE = Sample_StringConstant.value;
}