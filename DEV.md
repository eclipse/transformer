# Top Level Design

The Eclipse Transformer comprises:

* File update actions;
* Which are property file driven;
* Composed with container actions;
* And invoked using java and command line APIs.

Further:

* The core operation is to create a transformed copy of a root input file:
  * The core inputs are a target root input file, a target root output file, and a set of processing options.
  * The majority of processing options are used to specify property files which provide data to the several actions used by the transformer.
  * "Update in place" is specifically not supported, and must be supplied as external function.

* Processing of specific files (including container type files) is performed by an action framework:
  * Actions are distinguished by the type of file which they process.  For example, the "Class Action" is responsible for processing java class files.
  * Actions are stateless; state is factored into a separate "TransformerState" object which is passed as a parameter to actions.
  * An action may (but usually does not) change the name of a resource which is being transformed.
  * An action may change the contents of a target resource.

* Actions record and report on their effects:
  * Each action provides a change object which records, minimally, the count of changes, and whether the target resource name was changed.
  * An action may specialize the type of change object that it uses.

* Actions are driven by configuration data, which is supplied by properties files.
  * Property values usually encode an 'initial value - final value' pair, with the initial value as the property name and with the final value as the property value.
  * Other property values encode more detailed rule data.
  * Property data is not used in a one-to-one correspondence with actions.  What property data is used by which actions is peculiar to the several types of actions.

* Action types are broadly partitioned into "container type actions", "non-container (leaf) type actions, and a special "null" action.

* Container actions are responsible for iterating across their contents, and recursively invoking the action API on each container element.
  * Container actions each has an ordered collection of nested actions, which are used to process the elements of the container.
  * Container actions provide input stream to a recursively invoked action.
  * For best performance, container actions are strongly encouraged to use streaming to process container elements.
  * Streaming avoids the creation of temporary files.

* Leaf actions are responsible for reading, transforming, and writing the contents of a specific file of the type handled by the type of action.
   * Leaf actions are invoked with either a file or an input stream.
   * An action may decline to process a specific file.

* The null action handles file types which are otherwise not handled by any type of action, and performs a simple pass-through of the input data.

## Java and Command Line APIs

Core APIs consist of the following:

* Class **org.eclipse.transformer.cli.TransformerCLI** provides a standard **main** command line entry point.

* Class **org.eclipse.transformer.Transformer** also provides for direct invocation from java.

A third API is provided for Jakarta type transformations:

* Class **org.eclipse.transformer.cli.JakartaTransformerCLI** provides an addition **main** command line entry point.  **JakartaTransformerCLI** uses **JakartaTransform** which is packaged with default update properties files and supplies these as default property files for the several update properties files.

------

## Action Hierarchy

The available actions are organized into a hierarchy.  Each parent action is a container type action.  The parent-child relationship between actions defines which actions may be invoked on resources contained within a container which is being processed by the parent action.

In more detail, each parent action holds a composite action, which in turn holds an ordered list of candidate child actions.  For a resource which is being processed by the container action, a child action is selected by the composite by iterating across the child actions and selecting the first that accepts the resource.

The distinguished root action is created as a composite action.  The root action is responsible for selecting the actions which is used to process the target root input file.

The archive hierarchy is constructed according to these rules:

* The directory container action is only available as a child of the root action.

* All archive container actions are children of the directory container action.

* Archive container actions are selectively children of each other, according to usual JavaEE archive structure.  For example, the War container action is a child of the Ear container action, but is not a child of any other archive container action.

* All the leaf actions are children of all of each of the container actions.

The archive hierarchy is constructed by the transformer, with the structure fixed by transformer code.  The hierarchy  subject to modification, depending on use requirements, and is likely to be made configurable.

Current available actions are as follows:

* Directory container action:
  * Directory

* Archive container actions:
  * Zip with static factory methods for Jar, War, Rar, and Ear

* Leaf actions:
  * Class
  * Java
  * JSP
  * Manifest
  * ServiceLoaderConfig
  * Feature
  * Text

* Rename action:
  * Rename

## Action Implementation Hierarchy

Actions are implemented with a minimal set of interfaces, and several shared implementation classes.

The action implementation hierarchy is set to minimally define interfaces: Action, CompositeAction, and ContainerAction define the core action APIs.  ActionImpl, CompositeActionImpl, and ContainerActionImpl provide common implementations of the core APIs.  Action implementations are defined for specific types of files.  Specific action implementations are generally not provided with their own interfaces, since the specific actions usually do not define new APIs.

As new requirements are discovered, new types of actions are expected to be added to this implementation hierarchy.

The current action hierarchy is as follows:

Core action interfaces: Actions may be leaf type actions or container actions:

* Action
  * ElementAction
  * ContainerAction

Common action implementation: Common implementations of the core interfaces:

* Action
  * ActionImpl

* ContainerAction, ActionImpl
  * ContainerActionImpl

Container action implementations:

* ContainerActionImpl
  * DirectoryActionImpl
  * ZipActionImpl

Leaf type action implementations:

* ActionImpl
  * ClassActionImpl
  * JavaActionImpl
  * ManifestActionImpl
  * PropertiesActionImpl
  * ServiceLoaderConfigActionImpl
  * TextActionIMpl
  * XmlActionImpl

The rename action implementation:

* ActionImpl
  * RenameActionImpl



