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

import java.io.File;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.Action;
import org.eclipse.transformer.action.ActionType;
import org.slf4j.Logger;

public class DirectoryActionImpl extends ContainerActionImpl {

    public DirectoryActionImpl(
        Logger logger, boolean isTerse, boolean isVerbose,
    	InputBufferImpl buffer,
    	SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

    	super(logger,  isTerse, isVerbose, buffer, selectionRule, signatureRule);
    }

	//

	@Override
	public ActionType getActionType() {
		return ActionType.DIRECTORY;
	}

	@Override
	public String getName() {
		return "Directory Action";
	}

	//

	/**
	 * The choice of using a stream or using an input stream should never occur
	 * on a directory action.
	 */
	public boolean useStreams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		return ( (resourceFile != null) && resourceFile.isDirectory() );
	}

    @Override
	public void apply(String inputPath, File inputFile, File outputFile)
		throws TransformException {

    	startRecording(inputPath);
    	try {
    		setResourceNames(inputPath, inputPath);
    		transform(".", inputFile, outputFile);
    	} finally {
    		stopRecording(inputPath);
    	}
	}

	protected void transform(
		String inputPath, File inputFile,
		File outputFile)  throws TransformException {

	    inputPath = inputPath + '/' + inputFile.getName();

	    // Note the asymmetry between the handling of the root directory, 
	    // which is selected by a composite action, and the handling of sub-directories,
	    // which are handled automatically by the directory action.
	    //
	    // This means that the directory action processes the entire tree
	    // of child directories.
	    //
	    // The alternative would be to put the directory action as a child of itself,
	    // and have sub-directories be accepted using composite action selection.

	    if ( inputFile.isDirectory() ) {
	    	if ( !outputFile.exists() ) {
	    		outputFile.mkdir();
	    	}

	    	for ( File childInputFile : inputFile.listFiles() ) {
	    		File childOutputFile = new File( outputFile, childInputFile.getName() );
	    		transform(inputPath, childInputFile, childOutputFile);
	    	}

	    } else {
	    	Action selectedAction = acceptAction(inputPath, inputFile);
	    	if ( selectedAction == null ) {
	    		recordUnaccepted(inputPath);
	    	} else if ( !select(inputPath) ) {
	    		recordUnselected(selectedAction, inputPath);
	    	} else {
	    		selectedAction.apply(inputPath, inputFile, outputFile);
	    		recordTransform(selectedAction, inputPath);
	    	}
	    }
	}
}
