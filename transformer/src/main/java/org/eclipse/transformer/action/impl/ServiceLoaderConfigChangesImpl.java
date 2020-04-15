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

import org.slf4j.Logger;

public class ServiceLoaderConfigChangesImpl extends ChangesImpl {

	public ServiceLoaderConfigChangesImpl() {
		super();

		this.clearChanges();
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return ( changedProviders > 0 );
	}

	@Override
	public void clearChanges() {
		changedProviders = 0;
		unchangedProviders = 0;

		super.clearChanges();
	}

	//

	private int changedProviders;
	private int unchangedProviders;

	public void addChangedProvider() {
		changedProviders++;
	}

	public int getChangedProviders() {
		return changedProviders;
	}

	public void addUnchangedProvider() {
		unchangedProviders++;
	}

	public int getUnchangedProviders() {
		return unchangedProviders;
	}

	//

	@Override
	public void displayVerbose(PrintStream printStream, String inputPath, String outputPath) {
		printStream.printf("Input  [ %s ] as [ %s ]\n", getInputResourceName(), inputPath);
		printStream.printf("Output [ %s ] as [ %s ]\n", getOutputResourceName(), outputPath);
		printStream.printf( "Replacements [ %s ]\n", getChangedProviders() );
	}

	@Override
	public void displayVerbose(Logger logger, String inputPath, String outputPath) {
		if ( !logger.isInfoEnabled() ) {
			return;
		}

		logger.info("Input  [ {} ] as [ {} ]", getInputResourceName(), inputPath);
		logger.info("Output [ {} ] as [ {} ]", getOutputResourceName(), outputPath);
		logger.info( "Replacements [ {} ]", getChangedProviders() );
	}

}
