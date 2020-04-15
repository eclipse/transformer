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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.transformer.TransformProperties;
import org.eclipse.transformer.action.SelectionRule;
import org.slf4j.Logger;

public class SelectionRuleImpl implements SelectionRule {

	public SelectionRuleImpl(Logger logger, Set<String> includes, Set<String> excludes) {
		this.logger = logger;

		if ( includes == null ) {
			this.included = Collections.emptySet();
			this.includedExact = Collections.emptySet();
			this.includedHead = Collections.emptySet();
			this.includedTail = Collections.emptySet();
			this.includedAny = Collections.emptySet();
		} else {
			this.included = new HashSet<String>(includes);		
			this.includedExact = new HashSet<String>();
			this.includedHead = new HashSet<String>();
			this.includedTail = new HashSet<String>();
			this.includedAny = new HashSet<String>();
			TransformProperties.processSelections(
				this.included,
				this.includedExact, this.includedHead, this.includedTail, this.includedAny );
		}

		if ( excludes == null ) {
			this.excluded = Collections.emptySet();
			this.excludedExact = Collections.emptySet();
			this.excludedHead = Collections.emptySet();
			this.excludedTail = Collections.emptySet();
			this.excludedAny = Collections.emptySet();
		} else {
			this.excluded = new HashSet<String>(excludes);
			this.excludedExact = new HashSet<String>();
			this.excludedHead = new HashSet<String>();
			this.excludedTail = new HashSet<String>();
			this.excludedAny = new HashSet<String>();
			TransformProperties.processSelections(
				this.excluded,
				this.excludedExact, this.excludedHead, this.excludedTail, this.excludedAny );
		}
	}

	//
	
	private final Logger logger;

	public Logger getLogger() {
		return logger;
	}

	public void debug(String message, Object... parms) {
		getLogger().debug(message, parms);
	}

	//

	private final Set<String> included;
	private final Set<String> includedExact;
	private final Set<String> includedHead;
	private final Set<String> includedTail;
	private final Set<String> includedAny;
	
	private final Set<String> excluded;
	private final Set<String> excludedExact;
	private final Set<String> excludedHead;
	private final Set<String> excludedTail;	
	private final Set<String> excludedAny;	

	@Override
	public boolean select(String resourceName) {
		boolean isIncluded = selectIncluded(resourceName);
		boolean isExcluded = rejectExcluded(resourceName);

		return ( isIncluded && !isExcluded );
	}

	@Override
	public boolean selectIncluded(String resourceName) {
		if ( included.isEmpty() ) {
			debug("Include [ {} ]: {}", resourceName, "No includes");
			return true;

		} else if ( includedExact.contains(resourceName) ) {
			debug("Include [ {} ]: {}", resourceName, "Exact include");
			return true;

		} else {
			for ( String tail : includedHead ) {
				if ( resourceName.endsWith(tail) ) {
					debug("Include [ {} ]: {} ({})", resourceName, "Match tail", tail);
					return true;
				}
			}
			for ( String head : includedTail ) {
				if ( resourceName.startsWith(head) ) {
					debug("Include [ {} ]: {} ({})", resourceName, "Match head", head);
					return true;
				}
			}
			for ( String middle : includedAny ) {
				if ( resourceName.contains(middle) ) {
					debug("Include [ {} ]: {} ({})", resourceName, "Match middle", middle);
					return true;
				}
			}

			debug("Do not include [ {} ]", resourceName);
			return false;
		}
	}

	@Override
	public boolean rejectExcluded(String resourceName ) {
		if ( excluded.isEmpty() ) {
			debug("Do not exclude[ {} ]: {}", resourceName, "No excludes");
			return false;

		} else if ( excludedExact.contains(resourceName) ) {
			debug("Exclude [ {} ]: {}", resourceName, "Exact exclude");
			return true;

		} else {
			for ( String tail : excludedHead ) {
				if ( resourceName.endsWith(tail) ) {
					debug("Exclude[ {} ]: {} ({})", resourceName, "Match tail", tail);
					return true;
				}
			}
			for ( String head : excludedTail ) {
				if ( resourceName.startsWith(head) ) {
					debug("Exclude[ {} ]: {} ({})", resourceName, "Match head", head);
					return true;
				}
			}
			for ( String middle : excludedAny ) {
				if ( resourceName.contains(middle) ) {
					debug("Exclude[ {} ]: {} ({})", resourceName, "Match middle", middle);
					return true;
				}
			}

			debug("Do not exclude [ {} ]", resourceName);
			return false;
		}
	}
}
