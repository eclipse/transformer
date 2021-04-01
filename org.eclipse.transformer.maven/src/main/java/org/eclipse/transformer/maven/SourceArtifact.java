/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.maven;

/**
 * @author tevans
 *
 */
public class SourceArtifact {

	private String	groupId;
	private String	artifactId;
	private String	version;
	private String	classifier;
	private String	type;
	private String	scope;

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the classifier
	 */
	public String getClassifier() {
		return classifier;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}

	public boolean isValid() {
		return this.groupId != null && this.groupId.length() > 0 && this.artifactId != null
			&& this.artifactId.length() > 0 && this.version != null && this.version.length() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version
			+ ":" + classifier + ":" + type + ":" + scope;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param artifactId the artifactId to set
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @param classifier the classifier to set
	 */
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}

}
