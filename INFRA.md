# Infrastructure Notes

## Repository Structure

The Eclipse Transformer is stored in a standard Git repository, hosted under the Eclipse Foundation GIT organization:

  [https://github.com/eclipse/transformer]

Build scripts and standard files are located at the root:

| File      | Description                   |
|-----------|-------------------------------|
| README.md | Standard project readme       |
| INFRA.md  | Infrastructure notes          |
| DEV.md    | Developer notes               |
| RULES.md  | Update rules notes            |
|           |                               |
| mvnw      | Maven build script            |
| mvnw.cmd  | Maven build script (windows)  |
|           |                               |
| pom.xml   | Project parent maven metadata |

Project contents are currently managed as two maven modules:

| Module                          | Description                                     |
|---------------------------------|-------------------------------------------------|
| org.eclipse.transformer         | Core transformer library                        |
| org.eclipse.transformer.jakarta | Jakarta EE ruleset                              |
| org.eclipse.transformer.cli     | Jakarta command line interface                  |
| transformer-maven-plugin        | Maven plugin                                    |

## Build

The Eclipse Transformer uses maven build scripts.  See comments the maven build script "mvnw" (or "mvnw.cmd") for the most current build setup details.

Generally, a JDK home directory must be set via environment variable "JAVA_HOME", and a maven home directory should be set via environment variable "M2_HOME":

| Property  | Description           |
|-----------|-----------------------|
| JAVA_HOME | JDK home directory    |
| M2_HOME   | maven2 home directory |

## Packaging

Current packaging consists of the Jakarta command line interface:

    org.eclipse.transformer.cli-0.1.1-SNAPSHOT.jar

Plus the core command line interface and the jar containing core function, together with dependency libraries, placed in the libs folder:

    libs/
    libs/org.eclipse.transformer-0.1.1-SNAPSHOT.jar
    libs/biz.aQute.bnd.transform-5.1.0.jar
    libs/slf4j-api-1.7.25.jar
    libs/commons-cli-1.4.jar
    libs/slf4j-simple-1.7.29.jar

TODO: This packaging structure is likely to change as the core API evolves.
