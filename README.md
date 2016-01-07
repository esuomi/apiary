# Apiary - HTTP API client generator

Apiary is an annotation driven runtime HTTP API client generation tool.

## Quickstart for users

### Prerequisites

 - **JDK 8 installed**
   - Runtime compilation relies on implementation of [`javax.tools.JavaCompiler`](https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html) 
     being present in classpath which is provided by `tools.jar` (*or `classes.jar`*) only available in JDK distributions.
  
### Setup

Apiary is not yet available through public repositories. See developer instructions below.

### Usage

 - Apiary generation is controlled by annotation metadata. See [`NASA.java`](src/test/java/io/induct/apiary/nasa/NASA.java) for example on how this is done.
 - If you use use [Lombok](https://projectlombok.org/), the recommended bean definition should be similar to [`ApodImage.java`](src/test/java/io/induct/apiary/nasa/ApodImage.java).

## Quickstart for developers

### Prerequisites

 1. JDK 8 installed.
 
### Setup

 1. Fork the repository.
 2. Clone the forked repository.
 3. Ensure Java compiler has `-parameters` flag enabled
   - IntelliJ IDEA 15: *Preferences > Build, Execution, Deployment > Compiler > Java Compiler* and locate compiler parameters input
   - Eclipse: *Preferences > Java > Compiler* check "Store information about method parameters (usable via reflection)"

## Good to know, remarks etc.

Apiary works but is not nowhere complete. Off the top of my head the following should be implemented at least:

 - Injectable `RequestBuilder`
 - Generic rate limiting support
 - Per API request/response filters for eg. authentication
 - IoC for environment configurations to enable eg. hot loading of changes
