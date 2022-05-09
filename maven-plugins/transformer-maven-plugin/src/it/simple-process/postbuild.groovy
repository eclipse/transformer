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

import static org.assertj.core.api.Assertions.assertThat

//println " basedir: ${basedir}"
//println " localRepositoryPath: ${localRepositoryPath}"
//println " mavenVersion: ${mavenVersion}"

File artifact_main = new File(basedir, "target/smallrye-common-annotation-1.11.0.jar")
assertThat(artifact_main).isFile()

File artifact_sources = new File(basedir, "target/smallrye-common-annotation-1.11.0-sources.jar")
assertThat(artifact_sources).isFile()

File artifact_javadoc = new File(basedir, "target/smallrye-common-annotation-1.11.0-javadoc.jar")
assertThat(artifact_javadoc).isFile()

return true
