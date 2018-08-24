[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-frontend/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-frontend/1.5.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-frontend.svg)](https://javadoc.io/doc/org.jooby/jooby-frontend/1.5.1)
[![jooby-frontend website](https://img.shields.io/badge/jooby-frontend-brightgreen.svg)](http://jooby.org/doc/frontend)
# frontend

Download and install <a href="https://nodejs.org">node.js</a>, install <a href="https://www.npmjs.com">npm</a>, <a href="https://yarnpkg.com">yarn</a> and 
runs <a href="https://www.npmjs.com">npm</a>, <a href="https://webpack.js.org">webpack</a>, <a href="https://gulpjs.com">gulp</a>, <a href="https://gruntjs.com">grunt</a> and more.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-frontend</artifactId>
 <version>1.5.1</version>
</dependency>
```

## usage

Using <a href="https://www.npmjs.com">npm</a>:

```java
{
  on("dev", () -> {
    use(new Npm("v8.6.0"));
  });

}
```

Using <a href="https://yarnpkg.com">yarn</a>:

```java
{
  on("dev", () -> {
    use(new Yarn("v8.6.0", "v1.1.0"));
  });

}
```

> **NOTE**: The module must be used in development only. That's why we wrap the module installation using the environment predicate. In order to build a production version, you need to configure a build time process. The <a href="https://github.com/eirslett/frontend-maven-plugin">frontend</a> maven plugin does this job for maven projects. If you are a Gradle user, you might want to try the <a href="https://github.com/srs/gradle-node-plugin">gradle-node-plugin</a>.

## install phase

The module automatically sync and install dependencies at startup time.

## default task

The module runs ```npm run build``` or ```yarn run build``` at startup time (after install phase).

This mean your ```package.json``` must defines a ```build``` script, like:

```json
{
  "scripts": {
    "build": "webpack"
  }
}
```

The default task is always executed, unless you define one or more custom tasks (see next).

## custom task

Execution of arbitrary scripts is available via [npm.onStart](/apidocs/org/jooby/frontend/Frontend.html#onStart-org.jooby.funzy.Throwing.Consumer-) and [npm.onStarted](/apidocs/org/jooby/frontend/Frontend.html#onStarted-org.jooby.funzy.Throwing.Consumer-).

The next example executes two scripts:

```java
{
  use(new Npm("v8.6.0"))
    .onStart(npm -> {
      npm.executeSync("run", "local");
      npm.executeSync("run", "next");
    });
  );
}
```

The next example executes a script without waiting to finish (useful for background tasks):

```java
{
  use(new Npm("v8.6.0"))
    .onStart(npm -> {
      npm.execute("run", "webserver");
    });
  );

}
```

## starter project

Checkout the [webpack-starter](https://github.com/jooby-project/webpack-starter) demo project.

That's all folks!!
