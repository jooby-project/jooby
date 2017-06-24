# Prerequisites

You should have JDK8 and Maven 3.5.0 or above.

# Building everything

From the root directory:

```
mvn install
```

# Building one module only

Say you're wanting to contribute a modification to module of Jooby via a regular pull request. 
After clone/checkout of this repo, you can target **just one module** with command-line arguments 
for Maven like so:

```
# example module is 'jooby-mongodb' - replace as appropriate
mvn install -pl jooby-mongodb -am
```

Note: Maven builds the smallest amount on dependent modules necessary before it gets to this one.  The resulting 
build time is shorter, and less is downloaded from Maven Central to your local cache of jars.
