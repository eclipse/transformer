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

import java.io.PrintStream;

import org.eclipse.transformer.action.Changes;
import org.eclipse.transformer.action.ContainerChanges;
import org.slf4j.Logger;

public class ChangesImpl implements Changes {
	public ChangesImpl() {
		// Empty
	}

	@Override
	public boolean hasChanges() {
		return hasResourceNameChange() || hasNonResourceNameChanges();
	}

	@Override
	public void clearChanges() {
		inputResourceName = null;
		outputResourceName = null;

		replacements = 0;
	}

	//

	private String inputResourceName;
	private String outputResourceName;

	@Override
	public String getInputResourceName() {
		return inputResourceName;
	}

	@Override
	public void setInputResourceName(String inputResourceName) {
		this.inputResourceName = inputResourceName;
	}

	@Override
	public String getOutputResourceName() {
		return outputResourceName;
	}

	@Override
	public void setOutputResourceName(String outputResourceName) {
		this.outputResourceName = outputResourceName;
	}

	@Override
	public boolean hasResourceNameChange() {
		// The input name will be null if the transform fails very early.
		return ( (inputResourceName != null) &&
				 !inputResourceName.equals(outputResourceName) );
	}

	//

	private int replacements;

	@Override
	public int getReplacements() {
		return replacements;
	}

	@Override
	public void addReplacement() {
		replacements++;
	}

	@Override
	public void addReplacements(int additions) {
		replacements += additions;
	}

	@Override
	public boolean hasNonResourceNameChanges() {
		return ( replacements > 0 );
	}

	//

	@Override
	public void addNestedInto(ContainerChanges containerChanges) {
		// By default do nothing. 
	}

	//

	protected String getChangeTag() {
		return ( hasNonResourceNameChanges() ? "Changed" : "Unchanged" );
	}

	@Override
	public void displayTerse(PrintStream printStream, String inputPath, String outputPath) {
		if ( !inputPath.equals(outputPath) ) {
			printStream.printf("Input [ %s ] as [ %s ]: %s\n", inputPath, outputPath, getChangeTag() );
		} else {
			printStream.printf("Input [ %s ]: %s\n", inputPath, getChangeTag() );
		}
	}

	@Override
	public void displayTerse(Logger logger, String inputPath, String outputPath) {
		if ( !logger.isInfoEnabled() ) {
			return;
		}

		if ( !inputPath.equals(outputPath) ) {
			if ( !inputPath.equals(outputPath) ) {
				logger.info("Input [ {} ] as [ {} ]: {}", inputPath, outputPath, getChangeTag() );
			} else {
				logger.info("Input [ {} ]: {}", inputPath, getChangeTag() );
			}
		}
	}

	@Override
	public void display(PrintStream printStream, String inputPath, String outputPath) {
		displayTerse(printStream, inputPath, outputPath);
	}
	
	@Override
	public void display(Logger logger, String inputPath, String outputPath) {
		displayTerse(logger, inputPath, outputPath);
	}

	@Override
	public void displayVerbose(PrintStream printStream, String inputPath, String outputPath) {
		printStream.printf("Input  [ %s ] as [ %s ]\n", getInputResourceName(), inputPath );
		printStream.printf("Output [ %s ] as [ %s ]\n", getOutputResourceName(), outputPath );
		printStream.printf("Replacements  [ %s ]\n", getReplacements() );
	}
	
	@Override
	public void displayVerbose(Logger logger, String inputPath, String outputPath) {
		if ( !logger.isInfoEnabled() ) {
			return;
		}

		logger.info("Input  [ {} ] as [ {} ]", getInputResourceName(), inputPath );
		logger.info("Output [ {} ] as [ {} ]", getOutputResourceName(), outputPath );
		logger.info("Replacements  [ {} ]", getReplacements() );
	}
}
