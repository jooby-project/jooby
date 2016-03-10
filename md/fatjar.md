# fat jar

This is the default deployment option and you (usually) don't need to do anything if you created your application via [maven archetype](/quickstart).

In order to create a **fat jar** go to your project home, open a terminal and run:

```
mvn clean package
```

The jar will be available inside the ```target``` directory.

## configuration

We use the [maven-shade-plugin](https://maven.apache.org/plugins/maven-shade-plugin/) for creating the fat jar:

```xml
...
<build>
  <plugins>
    ...
    <!-- Build fat jar -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
    </plugin>
    ...
  </plugins>
</build>
```

If you created your application via [maven archetype](/quickstart) this setup is already present in your application.

## run / start

Due everything was bundle into a single ```jar``` file, all you have to do is:

```bash
java -jar myapp.jar [env]
```

Easy hugh? No complex deployment, **no heavy-weight servers**, **no classpath hell**, nothing!!!

Your application is up and running!!
