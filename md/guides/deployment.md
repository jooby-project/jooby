# deployment

In this guide you will learn how to deploy a **Jooby** application into a **production like** environment.

# jar deployment

Jooby is a micro-web framework, it doesn't run inside a container or **Servlet** environment. Instead, the web server: {{netty}}, {{jetty}} or {{undertow}} is packaged within your app.
This techniques is known as: **fat jar** distribution or deployment, which basically means everything is bundle into a single **.jar** file.

## build and package

In order to create a **fat jar** go to your project home, open a terminal and run:

```
mvn clean package
```

Once it finish, the jar will be available inside the ```target``` directory.

## run / start

Due everything was bundle into a single ```jar``` file, all you have to do is:

```bash
java -jar myapp.jar
```

Pretty easy! No complex deployment, **no heavy-weight servers**, **no classpath hell**, nothing!!! Your application is up and running!!

## env and conf

**Jooby** is built around the ```environment``` concept, which means a **Jooby** app always known when it runs in ```dev``` or anything else (prod like).

The ```application.env``` property controls the ```environment```, by default this property is set to ```dev``` (the unique and well known env).

Now, suppose you need to deploy your awesome app into ```prod``` and you need to set/update a few properties. The ```application.conf``` file looks like:

```
...
aws.accessKey = AKIAIOSFODNN7EXAMPLE
aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
...
```

Now, for ```prod``` you have a new key pair... then all you have to do is to create a new file ```application.prod.conf``` and just add those new keys:

```
aws.accessKey = AKIAIOSFODNN7PROD
aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYPROD
```

> **TIP**: You don't have to add all the properties, just those who are different for the environment.

The final step is to start the app with ```application.env=prod```:

```
java -Dapplication.env=prod myapp.jar
```

Your application will run now in ```prod```. It will find your ```application.conf``` and overrides in property defined there with those from ```application.prod.conf```

It works for ```logback.xml``` too, if ```logback.[env].xml``` is present, then **Jooby** will use it, otherwise it fallbacks to ```logback.xml```.

# zip deployment

A **Jooby** app created from {{maven}} ```archetype``` will also generates a ```myapp.zip``` file, every time you run:


```
mvn clean package
```

This ```.zip``` file **mimics** a typical server layout:

```bash
.
├── public
|   ├── <public content goes here>
├── conf
|   ├── application.conf
|   └── logback.xml
├── myapp.jar
├── start.sh
└── stop.sh
```

The scripts: ```start/stop.sh``` are very simple and they will start/stop a **Jooby** app.

The scripts are inside the ```src/etc/bin``` directory you can edit them, add more, etc... They will be always included in the final ```.zip```.

The ```zip``` deployment makes sense when you want to have more control over your application. For example:

* If you want to turn on/off loggers at runtime via: ```conf/logback.xml```
* The ```public``` folder contains all your static files, you can configure {{nginx}} or {{apache}} server to serve them from disk

## build and package

You don't have to do anything special to get a ```.zip``` if you created the app from {{maven}} ```archetype```.

If you don't you then open the ```pom.xml``` add these lines:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-assembly-plugin</artifactId>
  <configuration>
    <descriptorRefs>
      <descriptorRef>jooby.zip</descriptorRef>
    </descriptorRefs>
  </configuration>
</plugin>
```

The ```maven-assembly-plugin``` will generate the ```.zip``` file.

## run / start

It is usually don't via ```start.sh```:

```
./start.sh prod
```

## env and conf

It works like the [jar deployment](#jar-deployment-env-and-conf)

# conclusion

**Jar deployment** makes perfect sense for PaaS like Heroku, AppEngine, etc...

**Zip deployment** give you more control for starting and stopping but also control the application log at runtime.

Happy coding!!
