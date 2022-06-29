# Transformer Rules:

## Overview of actions:

All are in package **org.eclipse.transformer.action.impl**:

* Container Type Actions

  * DirectoryActionImpl
  * ZipActionImpl

* Leaf Type Actions

  * ClassActionImpl
  * ManifestActionImpl
  * RenameActionImpl
  * ServiceLoaderConfigActionImpl
  * TextActionImpl
	  * JavaActionImpl
	  * JSPActionImpl
	  * PropertiesActionImpl
      * XmlActionImpl

## Update Cases

### Case: Package Rename

**Command line argument**: -tr, --renames

**Property format**: Specifies a package rename as a key-value pair which maps an initial package name to a final package name.

For example:

    javax.servlet=jakarta.servlet

**Wildcards support**: The initial package name can contain a ".*" suffix, which will cause sub-packages are to be matched.  By default, sub-packages are not updated.

For example:

    javax.servlet.*=jakartax.servlet

**Description**: Modify package references using a table of package name updates.  Package references may occur in several types of files, including java class files, OSGi bundle manifests, service loader configuration files, JavaEE deployment descriptors, JSP files, TLD files, and HTML files.  Package references may occur within the names of specific resources, meaning, an update may change the name of the resource.  Typically, service loader configuration file which are updated are renamed.

**Used by**: Service configuration loader action

* Changes the names of resource configuration files which include the renamed package.
* Changes the values within resource configuration files which include the renamed package.

**Used by**: Java action

* Changes any source text which includes the renamed package.

**Used by**: Manifest action and feature action

* Updates any property values which include the renamed package.

**Used by**: Properties action

* Rename any properties file using the package rename rules.  (Note that property file contents are not updated.)

### Case: Package Version Update

**Command line argument**: -tv, --version

**Property Format**: Specifies a package version assignment as a key-value pair which maps a package name to a version range.

For example:

    jakarta.servlet=[5.0,6)

**Wildcard support**: None

**Description**: Updates version values of package references which occur in OSGi attributes "DynamicImport-Package", "Export-Package", "Import-Package", "Subsystem-Content", and "IBM-API-Package".

**Used by**: Manifest action and feature manifest action

### Case: Bundle Identity Update

**Command line argument**: -tb, --bundles

**Property format**:  An initial bundle symbolic name mapped to a comma delimited list of bundle identity update information. The bundle update information consists of a final bundle symbolic value, a version value for the final bundle, a name for the final bundle, and a description for the final bundle.

For example:

    *=*.jakarta,2.0,+" Jakarta",+"; Jakarta Enabled"

**Wildcard support**: The initial and final bundle symbolic IDs may include a '*'.  The final bundle symbolic ID may include a '*', in which case, the bundle symbolic name is updated by replacing the '*' with the initial bundle symbolic name.

The final bundle name and the final bundle description may contain a leading '+', in which case the final values are formed by replacing the '+' with the initial name or description.

The bundle version is not updated when the initial bundle symbolic name is '*'.

**Description**: Updates bundle identity attributes of manifest which match specified bundle symbolic names.  The presence of bundle identity data is determined by matching manifest attribute "Bundle-SymbolicName".  Updates are made to bundle identity attributes according to the property data.  Updates are made to the attributes "Bundle-SymbolicName", "Bundle-Version", "Bundle-Name", and "Bundle-Description".

**Used by**: Manifest action and feature manifest action

### Case: Class simple string update

**Command line argument**: -td, --direct

**Property format**: An initial text value mapped to a final text value.

**Wildcard support**: None

**Used by**: Class action

### Case: Text simple string update

**Command line argument**: -tf

**Property format**: Two tiers of property files are used.  The first tier maps file names and file name patterns to second tier files.  The second tier maps initial text values to final text values.

For example:

~~~
jakarta-text-master.properties
~~~

~~~
application.xml=jakarta-direct.properties
application-client.xml=jakarta-direct.properties
~~~

~~~
jakarta-direct.properties
~~~

~~~
javax.ejb.EJBContext=jakarta.ejb.EJBContext
javax.ejb.MessageDrivenContext=jakarta.ejb.MessageDrivenContext
javax.ejb.SessionContext=jakarta.ejb.SessionContext
javax.ejb.Timer=jakarta.ejb.Timer
javax.ejb.TimerService=jakarta.ejb.TimerService
...
~~~

**Wildcard support**: File name patterns may include a single '*' wildcard.

**Description**: Updates target files, reading these as UTF-8, line delimited, files.  Select update values (a table of initial and final text values) based on file name patterns.

**Used by**: Text action

### Case: Per class constant string update

**Command line argument**: -tp, --per-class-constant

**Property format**: Two tiers of property files are used.  The first tier maps class files to second tier files.  The second tier maps initial text values to final text values.

For example:

~~~
jakarta-per-class-constant-master.properties
~~~

~~~
org/apache/jasper/compiler/Generator.class=jsp-compiler.properties
~~~

~~~
jsp-compiler.properties
~~~

~~~
javax.servlet=jakarta.servlet
~~~

**Wildcard support**: None

**Description**: Updates target classes. All occurrences of a mapping key present in constant strings are replaced with the mapping value.

**Used by**: Class action
