# Prerequisites

You should have JDK8 and Maven 3.5.0 or above.

# Building everything

From the root directory:

```
mvn clean package
```

# Building one module only

Say you're wanting to contribute a modification to module of Jooby via a regular pull request. 
After clone/checkout of this repo, you can target **just one module** with command-line arguments 
for Maven like so:

```
# example module is 'jooby-hikari' - replace as appropriate
mvn package -pl jooby-hikari -am
```

Note: Maven builds the smallest amount on dependent modules necessary before it gets to this one. The resulting 
build time is shorter, and less is downloaded from Maven Central to your local cache of jars.

# Docker build

- docker build -t jooby .
- docker run -it --rm jooby /bin/sh
- /build # mvn clean package
