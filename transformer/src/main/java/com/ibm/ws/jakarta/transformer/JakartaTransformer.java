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

package com.ibm.ws.jakarta.transformer;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.Transformer.AppOption;

public class JakartaTransformer {

    public static void main(String[] args) throws Exception {
        Transformer jTrans = new Transformer(System.out, System.err);
        jTrans.setOptionDefaults( JakartaTransformer.class, getOptionDefaults() );
        jTrans.setArgs(args);

        @SuppressWarnings("unused")
        int rc = jTrans.run();
        // System.exit(rc); // TODO: How should this code be returned?
    }

    public static final String DEFAULT_RENAMES_REFERENCE = "jakarta-renames.properties";
    public static final String DEFAULT_VERSIONS_REFERENCE = "jakarta-versions.properties";
    public static final String DEFAULT_BUNDLES_REFERENCE = "jakarta-bundles.properties";
    public static final String DEFAULT_DIRECT_REFERENCE = "jakarta-direct.properties";
    public static final String DEFAULT_MASTER_XML_REFERENCE = "jakarta-xml-master.properties";

    public static Map<Transformer.AppOption, String> getOptionDefaults() {
    	HashMap<Transformer.AppOption, String> optionDefaults =
    		new HashMap<Transformer.AppOption, String>();

    	optionDefaults.put(AppOption.RULES_RENAMES, DEFAULT_RENAMES_REFERENCE);
    	optionDefaults.put(AppOption.RULES_VERSIONS, DEFAULT_VERSIONS_REFERENCE);
    	optionDefaults.put(AppOption.RULES_BUNDLES, DEFAULT_BUNDLES_REFERENCE);
    	optionDefaults.put(AppOption.RULES_DIRECT, DEFAULT_DIRECT_REFERENCE);
    	optionDefaults.put(AppOption.RULES_MASTER_XML, DEFAULT_MASTER_XML_REFERENCE);

    	return optionDefaults;
    }
}
