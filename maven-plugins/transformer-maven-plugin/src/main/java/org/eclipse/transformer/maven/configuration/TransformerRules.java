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

package org.eclipse.transformer.maven.configuration;

import java.util.List;

/**
 * Transformer Rules
 */
public class TransformerRules {
	private List<String>	selections;
	private List<String>	renames;
	private List<String>	versions;
	private List<String>	bundles;
	private List<String>	directs;
	private List<String>	texts;
	private List<String>	perClassConstants;
	private List<String>	immediates;
	private boolean			invert;
	private boolean			overwrite;
	private boolean			widen;
	private boolean			regexpRules;
	private boolean			jakartaDefaults;

	public TransformerRules() {}

	public List<String> getSelections() {
		return selections;
	}

	public void setSelections(List<String> selections) {
		this.selections = selections;
	}

	public List<String> getRenames() {
		return renames;
	}

	public void setRenames(List<String> renames) {
		this.renames = renames;
	}

	public List<String> getVersions() {
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
	}

	public List<String> getBundles() {
		return bundles;
	}

	public void setBundles(List<String> bundles) {
		this.bundles = bundles;
	}

	public List<String> getDirects() {
		return directs;
	}

	public void setDirects(List<String> directs) {
		this.directs = directs;
	}

	public List<String> getTexts() {
		return texts;
	}

	public void setTexts(List<String> texts) {
		this.texts = texts;
	}

	public List<String> getPerClassConstants() {
		return perClassConstants;
	}

	public void setPerClassConstants(List<String> perClassConstants) {
		this.perClassConstants = perClassConstants;
	}

	public List<String> getImmediates() {
		return immediates;
	}

	public void setImmediates(List<String> immediates) {
		this.immediates = immediates;
	}

	public boolean isInvert() {
		return invert;
	}

	public void setInvert(boolean invert) {
		this.invert = invert;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean isJakartaDefaults() {
		return jakartaDefaults;
	}

	public void setJakartaDefaults(boolean jakartaDefaults) {
		this.jakartaDefaults = jakartaDefaults;
	}

	/**
	 * @return the widen
	 */
	public boolean isWiden() {
		return widen;
	}

	/**
	 * @param widen the widen to set
	 */
	public void setWiden(boolean widen) {
		this.widen = widen;
	}

	public boolean isRegexpRules() {
		return regexpRules;
	}

	public void setRegexpRules(final boolean regexpRules) {
		this.regexpRules = regexpRules;
	}

	@Override
	public String toString() {
		return String.format(
			"selections=%s, renames=%s, versions=%s, bundles=%s, directs=%s, texts=%s, perClassConstants=%s, immediates=%s, invert=%s, overwrite=%s, widen=%s, jakartaDefaults=%s",
			getSelections(), getRenames(), getVersions(), getBundles(), getDirects(), getTexts(),
			getPerClassConstants(), getImmediates(), isInvert(), isOverwrite(), isWiden(), isJakartaDefaults());
	}

}
