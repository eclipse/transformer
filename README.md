Liberty Jakarta Prototyping Environment

---

Build artifacts copied from open-liberty:

Gradle artifacts:

  .gradle
  .gradle-wrapper

  build.gradle (stub)
  gradle.properties (trimmed)
  gradlew
  gradlew.bat

GIT artifacts:

  .gitignore

Liberty artifacts:

  build.image (stub)

Prototype artifacts:

  source.image
    -- download liberty image for webProfile8 for liberty version 19.0.0.9:
       'io.openliberty:openliberty-webProfile8:19.0.0.9'

----

Prototype Goal (1):

Setup a build environment which obtains a liberty image and a sample web application and that verifies that the image may be used to run the web application.

(1) Create GIT repository for prototyping
(2) Obtain a sample web application
(3) Identify a source liberty image; setup the build environment to retrieve that image
(4) Create a dummy build step that simply copies the source image to a target location.
(5) Use the target image to run the sample web application.

Prototype Goal (2):

Create jakarta API jars.  Transform the SNOOP web applcation by (1) source conversion and (2) bytecode transormation.  Compile the source using the jakarta API jars.  Verify the transformed classes against the compiled transformed source.

Prototype Goal (3):

Obtain the jakarta version of the sample web application.  Obtain jakarta API jars.  Transform the liberty image to a jakarta liberty image.  Use the jakarta liberty image to run the jakarta sample application.

