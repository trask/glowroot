Xyzzy &nbsp;&nbsp; [![Build Status](https://img.shields.io/travis/glowroot/xyzzy.svg)](https://travis-ci.org/glowroot/xyzzy) [![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.glowroot.xyzzy:xyzzy-parent&metric=coverage)](https://sonarcloud.io/dashboard?id=org.glowroot.xyzzy%3Axyzzy-parent)
=========

## Requirements

* Java 6+

## Building

The usual:

    mvn clean install

Building requires Java 7+ (in order to perform [Immutables](https://immutables.github.io) annotation processing) and Maven 3.3.1+.

## Contributing

Xyzzy uses [Immutables](https://immutables.github.io) annotation processing to eliminate maintenance on lots of boilerplate code. If you are using Eclipse, this requires installing the [m2e-apt](https://github.com/jbosstools/m2e-apt) plugin and changing Window > Preferences > Maven > Annotation Processing to "Automatically configure JDT APT".

## Integration tests

Integration tests are run during Maven's standard `integration-test` lifecycle phase.

The Xyzzy engine has an [integration test harness](test-harness) which makes it easy to run sample application code and then validate the trace captured by the engine.  The integration test harness is able to run tests both using a custom weaving class loader (which is very convenient for running and debugging inside your favorite IDE), and by spawning a JVM with the -javaagent flag (which more correctly simulates real world conditions).

## Microbenchmarks

Microbenchmarks are written using the excellent [JMH](http://openjdk.java.net/projects/code-tools/jmh/) benchmark harness. The microbenchmarks can be built and run under [benchmarks](benchmarks):

    mvn clean package
    java -jar target/benchmarks.jar -jvmArgs -javaagent:path/to/glowroot.jar

## Code quality

[SonarQube](http://www.sonarqube.org) is used to check Java coding conventions, code coverage, duplicate code, package cycles and much more. See analysis at [https://sonarcloud.io](https://sonarcloud.io/dashboard?id=org.glowroot.xyzzy%3Axyzzy-parent).

[Checker Framework](http://types.cs.washington.edu/checker-framework/) is used to eliminate fear of *null* with its rigorous [Nullness Checker](http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#nullness-checker). It is run as part of every Travis CI build (see the job with TARGET=checker) and any violation fails the build.

## License

Xyzzy source code is licensed under the Apache License, Version 2.0.

See [Third Party Software](https://github.com/glowroot/xyzzy/wiki/Third-Party-Software) for license detail of third party software included in the binary distribution.
