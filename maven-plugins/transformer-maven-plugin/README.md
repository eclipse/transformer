# transformer-maven-plugin

The Eclipse Transformer Maven Plugin provides a means to use the Transformer in a Maven build.

This plugin contains the following goals:

- [`jar`](#jar-goal)
- [`transform`](#transform-goal)
- [`help`](#help-goal)

__Note__: if you are using Java 21 or later see [Using Java 21 or later](#java-21-or-later).

## `jar` Goal

The `jar` goal operates at the JAR file level and transforms the specified input artifact into a project artifact.

When using this goal without specifying a classifier, the plugin must be configured with `<extensions>true</extensions>` in order to coordinate with the `maven-jar-plugin`.

The `jar` goal is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to `package` phase)

### `jar` Goal Configuration Parameters

|Configuration Parameter | Description |
| --- | --- |
|`rules` | The transformation rules. See [Rules](#rules).|
|`packagingTypes` | The list of maven packaging types for which the goal will execute. _Defaults to `jar,war,ear,ejb,ejb3,par,rar,maven-plugin`_. Override with property `transformer.packagingTypes`. |
|`skip` | Skip executing this goal. _Defaults to `false`_. Override with property `transform.skip`.|
|`artifact` | The input artifact to transform. See [Artifact](#artifact).|
|`buildDirectory` | The build directory into which the new transformed artifact is written. _Defaults to `${project.build.outputDirectory}`_.|
|`baseName` | The base name of the transformed artifact. The classifier and extension, based upon the type, will be suffixed to the base name. _Defaults to `${project.build.finalName}`_.|
|`classifier` | The classifier of the transformed artifact. The default value is comes from the `classifier` value in the `artifact` configuration. The value `-` (Hyphen-minus) is treated as no classifier specified. So use `-` to have no classifier when the `artifact` configuration specifies a classifier.|
|`type` | The type of the transformed artifact. _Defaults to `${project.packaging}`_.|
|`outputTimestamp` | Time stamp for [reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html), either formatted as ISO 8601 `yyyy-MM-dd'T'HH:mm:ssXXX` or as an int representing seconds since the epoch (like SOURCE_DATE_EPOCH). _Defaults to `${project.build.outputTimestamp}`_.|
|`attach` | Attach the transformed artifact to the project. _Defaults to `true`_.|

#### Rules

The `rules` configuration paramater has the following nested parameters.
All URIs are resolved relative to the project base directory.
A list value of `-` (Hyphen-minus) is ignored. This can be used to configure an empty list to override the default value when `jakartaDefaults` is `true`.

|Configuration Parameter | Description |
| --- | --- |
|`selections` | A list of URIs to selections properties. |
|`renames` | A list of URIs to renames properties. |
|`versions` | A list of URIs to versions properties. |
|`bundles` | A list of URIs to bundles properties. |
|`directs` | A list of URIs to directs properties. |
|`texts` | A list of URIs to texts properties. |
|`perClassConstants` | A list of URIs to perClassConstants properties. |
|`immediates` | A list of immediate options. |
|`invert` | If `true`, invert the rename rules. _Defaults to `false`_. |
|`overwrite` | If `true`, the items which transform to the same path as an existing item overwrite the existing item. _Defaults to `false`_. |
|`widen` | If `true`, By default, archive nesting is restricted to JavaEE active locations. This may be relaxed to enable JAR and ZIP within JAR, ZIP within ZIP, and ZIP within EAR, WAR, and RAR. _Defaults to `false`_. |
|`jakartaDefaults` | If `true`, the Jakarta rule defaults are included. _Defaults to `false`_. |

```xml
<rules>
    <jakartaDefaults>true</jakartaDefaults>
</rules>
```

```xml
<rules>
    <renames>
        <rename>renames.properties</rename>
    </renames>
    <invert>true</invert>
</rules>
```

#### Artifact

The `artifact` configuration paramater has the following nested parameters.

|Configuration Parameter | Description |
| --- | --- |
|`groupId` | The groupId of the artifact to transform. This must be specified. |
|`artifactId` | The artifactId of the artifact to transform. This must be specified. |
|`version` | The version of the artifact to transform. This can be inferred from inspecting the project main and attached artifacts,  `dependencies`, and `dependencyManagement` configuration to find the specified groupId and artifactId and using the version of the matching artifact. |
|`classifier` | The classifier of the artifact to transform. _Defaults to no classfier_. |
|`type` | The type of the artifact to transform. _Defaults to `${project.packaging}`_. |
|`excludes` | A list of exclusion globs which are paths to be excluded from the artifact to transform. `META-INF/maven/**` is always excluded as the Maven metadata for the result artifact comes from the project. |

```xml
<artifact>
    <groupId>io.smallrye.common</groupId>
    <artifactId>smallrye-common-annotation</artifactId>
</artifact>
```

#### `jar` Goal Plugin Configuration Example

```xml
<plugin>
    <groupId>org.eclipse.transformer</groupId>
    <artifactId>transformer-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <rules>
            <jakartaDefaults>true</jakartaDefaults>
        </rules>
    </configuration>
    <executions>
        <execution>
            <id>default-jar</id>
            <goals>
                <goal>jar</goal>
            </goals>
            <configuration>
                <artifact>
                    <groupId>io.smallrye.common</groupId>
                    <artifactId>smallrye-common-annotation</artifactId>
                    <version>1.11.0</version>
                </artifact>
            </configuration>
        </execution>
        <execution>
            <id>javadoc-jar</id>
            <goals>
                <goal>jar</goal>
            </goals>
            <configuration>
                <skip>${maven.javadoc.skip}</skip>
                <artifact>
                    <groupId>io.smallrye.common</groupId>
                    <artifactId>smallrye-common-annotation</artifactId>
                    <version>1.11.0</version>
                    <classifier>javadoc</classifier>
                </artifact>
            </configuration>
        </execution>
        <execution>
            <id>source-jar</id>
            <goals>
                <goal>jar</goal>
            </goals>
            <configuration>
                <skip>${maven.source.skip}</skip>
                <artifact>
                    <groupId>io.smallrye.common</groupId>
                    <artifactId>smallrye-common-annotation</artifactId>
                    <version>1.11.0</version>
                    <classifier>sources</classifier>
                </artifact>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Note**: In this example, the execution for no classifier **replaces** the _matching_ execution of the `maven-jar-plugin`.

#### Matching executions

The `transformer-maven-plugin` does not blindly remove all `maven-jar-plugin` executions found in the project, only those whose executions match the `jar` goal and the specified classifier.
Therefore it is possible to have both plugins operating within the same project provided they do not overlap.
(And remember that no specified classifier means the main artifact.)

## `transform` Goal

The `transform` goal operates at the directory level and transforms the specified directory, which is normally `target/classes`, to be subsequently included in an artifact by the `maven-jar-plugin`.

The `transform` goal is not executed by default, therefore at least one explicit execution needs to be configured (by default bound to `process-classes` phase)

### `transform` Goal Configuration Parameters

|Configuration Parameter | Description |
| --- | --- |
|`rules` | The transformation rules. See [Rules](#rules).|
|`packagingTypes` | The list of maven packaging types for which the goal will execute. _Defaults to `jar,war,ear,ejb,ejb3,par,rar,maven-plugin`_. Override with property `transformer.packagingTypes`. |
|`skip` | Skip executing this goal. _Defaults to `false`_. Override with property `transform.skip`.|
|`transformDirectory` | The directory to transform. This directory must already be populated with content to transform before this goal executes. _Defaults to `${project.build.outputDirectory}`_.|

#### `transform` Goal Plugin Configuration Example

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>unpack</id>
            <phase>process-classes</phase>
            <goals>
                <goal>unpack</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>io.smallrye.common</groupId>
                        <artifactId>smallrye-common-annotation</artifactId>
                        <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                        <excludes>META-INF/jandex.idx,META-INF/maven/**</excludes>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.eclipse.transformer</groupId>
    <artifactId>transformer-maven-plugin</artifactId>
    <configuration>
        <rules>
            <jakartaDefaults>true</jakartaDefaults>
        </rules>
    </configuration>
    <executions>
        <execution>
            <id>default-transform</id>
            <goals>
                <goal>transform</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <executions>
       <execution>
            <id>default-jar</id>
            <goals>
                <goal>jar</goal>
            </goals>
            <configuration>
                <archive>
                    <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
                </archive>
            </configuration>
       </execution>
    </executions>
</plugin>
```

### IMPORTANT NOTE about Maven JAR Plugin

When using the `transform` goal it is important to take the following into consideration.
The `maven-jar-plugin` will NOT currently use the data from the transformed `MANIFEST.MF` file when using its default configuration.
It is therefore necessary to configure the `maven-jar-plugin` as follows:

```xml
<configuration>
    <archive>
        <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile>
    </archive>
</configuration>
```

## `help` Goal

The `help` goal displays help about the goals of the plugin.
Call `mvn transformer:help -Ddetail=true -Dgoal=<goal-name>` to display configuration details for the specified goal.

## Java 21 or later

If you are using Java 21 or later you will need to configure the Maven plugin to use a specific version of the `biz.aQute.bnd` dependency.

E.g.

```xml
  <dependency>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>biz.aQute.bnd</artifactId>
    <version>7.0.0</version>
  </dependency>
```
