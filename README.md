# terracotta-platform

## For Developers

### IMPORTANT: settings.xml

You can run all the Maven commands with `-s settings.xml` to use the project's settings.xml 
and isolate downloaded libraries inside. Change the repo location to point to your default m2 home if needed.

Example: `./mvnw -s settings.xml clean install`

### License Headers

Check for missing licenses:

```bash
./mvnw license:check
```

Format files:

```bash
./mvnw license:format
```

### Fast build

This project is big and development and testing times are impacted by Maven build time since there is no gradle cache.
So we need to be able several times per day to build the project *fast*.

```bash
./mvnw clean install -DskipTests -Dfast -Djava.build.vendor=openjdk
```

The command should only do the minimal required steps:

- compilation steps
- felix
- packaging (jars and source jars - to use snapshots in downstream projects)

*No toolchain, verification, license check, findbugs, etc.*

The command needs to be fast: *less than 30 seconds.*

### Unit tests vs IT tests

We are able to run the unit tests only and all tests:

Skip all the tests:

```bash
./mvnw clean verify -DskipTests -Dfast
```

Skip Galvan and Angela IT tests but run unit tests (*last ~2-3min*)

```bash
./mvnw clean verify -DskipITs -Dfast
``` 

Run everything, like on Azure. *Lasts ~1h20min*

```bash
./mvnw clean verify -Dtest.parallel.forks=1C -Djava.test.vendor=openjdk -Djava.build.vendor=openjdk
``` 

### Concurrent Maven executions

It is possible to further speed the execution of the build by using `-TxC`.

Example, running the unit tests with `-T4C` leads to a build lasting only 1 min 20 sec.

```bash
./mvnw clean install -DskipTests -Dfast -T4C
```

### Plugin config

- Use `<trimStackTrace>false</trimStackTrace>`

### Choose the JVM used for testing

We can run the tests with one of the JVM on the toolchain but compilation is done in 1.8.

*Examples*

Run a test with Java 11 (will look at your toolchains.xml file to fine the JVM):

```bash
./mvnw verify -f management/testing/integration-tests/pom.xml -Dit.test=DiagnosticIT -Djava.test.version=1.11 -Dtest.parallel.forks=1C -Djava.test.vendor=openjdk -Djava.build.vendor=openjdk
```

Run a test with Java 8 (will look at your toolchains.xml file to fine the JVM):

```bash
./mvnw verify -f management/testing/integration-tests/pom.xml -Dit.test=DiagnosticIT -Djava.test.version=1.8 -Dtest.parallel.forks=1C -Djava.test.vendor=openjdk -Djava.build.vendor=openjdk
```

Run a test with Java 8 (will look at your toolchains.xml file to fine the JVM):

```bash
./mvnw verify -f management/testing/integration-tests/pom.xml -Dit.test=DiagnosticIT
```

Run a test by using the default JAVA_HOME found from your shell. -Dfast has no enforcement at all and does not use toolchain. -Dfast goal is to compile and package fast.

```bash
./mvnw verify -f management/testing/integration-tests/pom.xml -Dit.test=DiagnosticIT -Dfast
```

The same applies for Angela tests in dynamic-config/testing/system-tests.

*Verification*

Look inside the servers logs of the nodes started by Angela and Galvan.
The server logs contain the list of system properties and JVM details used to start the servers.
The version should match the one requested.

- For Angela tests: `target/angela/work/XYZ/logs/stripe1/node-1-1/terracotta.server.log`
- For Galvan tests: `target/galvan/XYZ/stripe1/testServer0/logs/terracotta.server.log`

Look for something like that in the logs:

```
java.runtime.name             : OpenJDK Runtime Environment
java.runtime.version          : 11.0.6+10
java.specification.name       : Java Platform API Specification
java.specification.vendor     : Oracle Corporation
java.specification.version    : 11
```

### IDE support

After having done a `./mvnw clean verify -DskipTests -Dfast` to build the project and kit, you should be able to go in your IDE and right-click run the test DiagnosticIT for example.
