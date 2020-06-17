# Eclipse Transformer

## Additional documentation

See the following files for more detailed information:

* LICENSE       Apache Licence, Version 2.0; Eclipse Public License Version 2.0
* README.md     Root readme (this file)
* INFRA.md      Project infrastructure and build notes
* DEV.md        Developer overview
* RULES.md      Transformer actions and rules details

## Summary

The Eclipse Transformer is a generic framework for transforming resources (files and archive) based on updates specified by property files.

The Eclipse Transformer is provided with two specific built-in capabilities:

* To update the target resources according to user specified rules data, which includes Jakarta package renaming rules.  The target resource is an JavaEE version of an implementation jar, or is a JavaEE version of an OSGi implementation jar, or is a JavaEE version of an application archive.

* To shade a target resource according to user specified rules, including, in particular, package renaming rules.  The target resource is an implementation jar.

In addition to the specific built-in capabilities, other transformations may be performed by changing the rules data, or by adding new types of update action.

The Eclipse Transformer is provided with Command Line and with Java APIs:

* Transformer classes may be used directly within java code.

* Transformation function is accessible through two Command Line APIs.  A generic command line API, which requires user specified update data, may be used.  A Jakarta command line API, which has built-in Jakarta rules data, may also be used.

TODO: Provisions for use from Maven and Gradle are expected to be provided, but are not yet available.

## Scope

The Eclipse Transformer operates on resources, including but not limited to the following:

Simple resources:

* Java class files
* OSGi feature manifest files
* Properties files
* Service loader configuration files
* Text files (of several types: java source, XML, TLD, HTML, and JSP)

Container resources:

* Directories
* JavaEE archives (JAR, WAR, RAR, and EAR files)
* ZIP archives

## APIs:

The Eclipse Transformer provides three core APIs: A java entry point and two command line entry points:

* Class **org.eclipse.transformer.Transformer** provides a standard **main** command line entry point.

* Class **org.eclipse.transformer.Transformer** also provides for direct invocation from java using **int Transformer.runWith(PrintStream, PrintStream, String[])**.

* Class **org.eclipse.transformer.jakarta.JakartaTransformer** provides an addition **main** command line entry point.  **JakartaTransformer** is packaged with default update properties files and supplies these as default property files for the several update properties files.

## Use Cases

Primary use cases are as follows:

* **Simple resource case**: A target simple file is specified to be transformed.  The command line interface is used to create a transformed copy of the file.  Particular transformations are selected for the file based on the type of the file.  Particular updates made by the transformation are based on property files which are specified to the transformer.

* **Archive case**: A target archive is specified to be transformed.  The command line interface is used to create a transformed copy of the archive.  All contents of the target archive, including nested archives, are transformed.  Particular transformations are selected for the archive based on the type of the file.  Particular updates made by the transformation are based on property files which are specified to the transformer.

* **Directory case**: A target directory is specified to be transformed.  The command line interface is used to create a transformed copy of all of the resources within the directory, and within all sub-directories of the specified directory.  Transformation of each file within the target directory is according the simple resource case and the archive case.

Possible secondary use cases are as follows:

* **Classloading case**: A java class action is created and initialized, and is wired into a custom class loader.  When loading resources, in particular, when loading a class resource, the java class action is used to transform the contents of the resource before making them available to the class loader.

## Usage

The primary use of the Eclipse Transformer is to create an updated copy an input file or directory.  Generally:

    Transformer inputFile outputFile [ options ... ]

For simplicity, the transformer does not perform any "in-place" updates.  Replacement of an input file with an output file must be performed as an operation external to the transformer.

The input file is expected to be either a specific type of file (for example, a java class file), or a container type file; usually, a zip, java, or JavaEE  archive, or a directory.

In addition to the output file, the transformer produces a change report.  The change report tells if any updates were made to the target file, and provides details on what changes were made.  What details are provided depends on the type of file which is transformer.

When the input file is a container type file, the transformer is applied to all of the elements of the container.

The transformer recursively processes nested archives.  For example, Web Application Archives (WAR files) located within Enterprise Application Archives (EAR files) are processed.

## Updates

A core function of the Eclipse Transformer is to locate java package references within resources and to update these references using package rename data.

Additional functions are to update OSGi bundle metadata, for example, to change the bundle metadata, or to change package versions for renamed package references.

For class shading, usual target resources are java class files or a java archives.

For Jakarta related updates, usual target resources are implementation jar files and JavaEE application archives.

## Rules Data Files

Update data is specified using property files.  A set of default property files are included in the packaging of the Jakarta command line interface.  User specified property files must be provided when using the generic command line interface.  Property files are specified using command line options:

| Command Line Option | Description                  | Jakarta Example                |
| ------------------- | ---------------------------- | ------------------------------ |
| -ts --selection     | Target selections            | None (select all)              |
| -tr --renames       | Package renames              | jakarta-renames.properties     |
| -tv --versions      | Package version assignments  | jakarta-versions.properties    |
| -tb --bundles       | Bundle identity data         | jakarta-bundles.properties     |
| -td --direct        | Java class direct updates    | jakarta-direct.properties      |

An additional core property file is used as a master table for text updates:

| Command Line Option | Description                  | Jakarta Example                |
| ------------------- | ---------------------------- | ------------------------------ |
| -tf --text          | Text master data             | jakarta-text-master.properties |

The master text properties file maps file selection patterns (for example, "*.xml", or "*.html") to specific text properties files.
