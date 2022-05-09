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
 * The input artifact to transform.
 */
public class TransformerArtifact {
	private String			groupId;
	private String			artifactId;
	private String			version;
	private String			type;
	private String			classifier;
	private List<String>	excludes;

	public TransformerArtifact() {}

	/**
	 * The groupId must be specified.
	 *
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * The artifactId must be specified.
	 *
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @param artifactId the artifactId to set
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * The version must be specified.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * If the type is not specified, it defaults to the type of the transformed
	 * artifact.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * If the classifier is not specified, it defaults to the classifier of the
	 * transformed artifact.
	 *
	 * @return the classifier
	 */
	public String getClassifier() {
		return classifier;
	}

	/**
	 * @param classifier the classifier to set
	 */
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	/**
	 * Ant-glob patterns of resources to remove from the artifact before
	 * transformation.
	 *
	 * @return the exclusions
	 */
	public List<String> getExcludes() {
		return excludes;
	}

	/**
	 * @param excludes the exclusions to set
	 */
	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	@Override
	public String toString() {
		String classifier = getClassifier();
		if ((classifier == null) || classifier.isEmpty()) {
			return String.format("%s:%s:%s:%s excludes=%s", getGroupId(), getArtifactId(), getVersion(), getType(),
				getExcludes());
		}
		return String.format("%s:%s:%s:%s:%s excludes=%s", getGroupId(), getArtifactId(), classifier, getVersion(),
			getType(), getExcludes());
	}
}
