# capsule

Package and Deploy JVM Applications with [Capsule](http://www.capsule.io).

A capsule is a single executable JAR that contains everything your application needs to run either in the form of embedded files or as declarative metadata. More at [capsule.io](http://www.capsule.io)

## usage

[Capsule](http://www.capsule.io) integration is provided via [Maven Profiles](http://maven.apache.org/guides/introduction/introduction-to-profiles.html) using the [capsule-maven-plugin](https://github.com/chrischristo/capsule-maven-plugin).

* Write a ```capsule.activator``` file inside the ```src/etc``` directory

* Open a console and type: ```mvn clean package```

* It builds a ```[app-name]-capsule-fat.jar``` file inside the ```target``` directory

It generates a ```fat capsule``` by default inside the ```target``` directory.

If you are a [Gradle](https://gradle.org/) user, checkout the [capsule gradle](https://github.com/puniverse/capsule-demo/blob/master/build.gradle) example.

## profile activation

The ```capsule``` Maven profile is activated by the presence of the ```src/etc/capsule.activator``` file (file content doesn't matter, just the file presence).

## options

The integration provides the following defaults:

```xml
<properties>
  <capsule.resolve>false</capsule.resolve>
  <capsule.chmod>true</capsule.chmod>
  <capsule.trampoline>false</capsule.trampoline>
  <capsule.JVM-Args>-Xms512m -Xmx512m</capsule.JVM-Args>
  <capsule.types>fat</capsule.types>
  <capsule.caplets>Capsule</capsule.caplets>
</properties>
```

For example, if you need or prefer a ```thin capsule```, follow these steps:

* Open your ```pom.xml```
* Go to the ```<properties>``` section and add/set:

```xml
<properties>
  <capsule.resolve>true</capsule.resolve>
  <capsule.types>thin</capsule.types>
</properties>
```

* Open a console and type: ```mvn clean package```

You'll find your ```thin``` capsule in the ```target``` directory.
