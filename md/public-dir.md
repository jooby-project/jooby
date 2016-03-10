# static resources

This isn't a deployment option, just a way to collect static resources and deploy them somewhere else.

The ```static``` assembly let you collect and bundle all your static resources into a ```zip``` file.

This is nice if you want or prefer to serve static resources via {{nginx}} or {{apache}}.

## usage

* Open your ```pom.xml``` and setup the ```jooby.static``` assembly, like:

```xml
...
<build>
  <plugins>
    ...
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-assembly-plugin</artifactId>
      <configuration>
        <descriptorRefs>
          <descriptorRef>jooby.static</descriptorRef>
        </descriptorRefs>
      </configuration>
    </plugin>
    ...
  </plugins>
</build>
...
```

* Open a console and type: ```mvn clean package```

* Look for ```[app-name]tar.gz``` inside the ```target``` direactory
