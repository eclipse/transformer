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

package org.eclipse.transformer.action;

import java.io.PrintStream;

import org.slf4j.Logger;

public interface Changes {
	String getInputResourceName();
	void setInputResourceName(String inputResourceName);

	String getOutputResourceName();
	void setOutputResourceName(String outputResourceName);

	int getReplacements();
	void addReplacement();
	void addReplacements(int additions);

	void addNestedInto(ContainerChanges containerChanges);

	boolean hasChanges();
	boolean hasNonResourceNameChanges();
	boolean hasResourceNameChange();

	void clearChanges();

	void displayVerbose(PrintStream printStream, String inputPath, String outputPath);
	void displayVerbose(Logger logger, String inputPath, String outputPath);

	void display(PrintStream printStream, String inputPath, String outputPath);
	void display(Logger logger, String inputPath, String outputPath);

	void displayTerse(PrintStream printStream, String inputPath, String outputPath);
	void displayTerse(Logger logger, String inputPath, String outputPath);
}
