# deployment

Learn how to build, package and deploy applications.

## intro

Jooby applications are ```self-contained``` and they don't run inside a container or **Servlet** environment.

Application resources (a.k.a conf files) are packaged within your app. Not only that, the [server](/doc/servers) you pick is **also** packaged within your application.

Jooby lack of the concept of server/servlet container it just a raw/plain/normal Java application that can be launched via:

    java -jar myapp.jar

Where ```myapp.jar``` is a ```fat jar```. A ```fat jar``` is a jar with all the app dependencies, conf files and web server... all bundle as a single jar.

## environments

The ```fat jar``` is nice when we need to deploy our application into one single environment, but what if we need to deploy to stage? or prod?

The way we deploy to a different environment is by creating a conf file (.conf or logback.xml) and adding the environment as suffix.

If you need to deploy to ```stage``` and ```prod``` then you probably need:

```
# dev environment
conf/application.conf
conf/logback.xml

conf/application.stage.conf
conf/logback.stage.xml

conf/application.prod.conf
conf/logback.prod.xml
```

The file jar contains **everything** your dev, stage and prod conf files. 

**Jooby** is built around the ```environment``` concept, which means a **Jooby** app always known in which environment it runs.

The ```application.env``` property controls the ```environment```, by default this property is set to ```dev``` (the unique and well known environment).

Suppose you need to deploy your awesome app into ```prod``` and you need to set/update a few properties. The ```application.conf``` file looks like:

```
...
aws.accessKey = AKIAIOSFODNN7EXAMPLE
aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
...
```

For ```prod``` you have a new key pair... then all you have to do is to create a new file ```application.prod.conf``` and just add those new keys:

```
aws.accessKey = AKIAIOSFODNN7PROD
aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYPROD
```

> **TIP**: You don't have to add all the properties, just those that changes between environments.

## run your app

In order to start your application in one specific environment, you need to set the ```application.env``` argument:

```
java myapp.jar application.env=prod
```

or using a shortcut, like:

```
java myapp.jar env=prod
```

or just:

```
java myapp.jar prod
```

Your application will run now in ```prod``` mode. It will find your ```application.conf``` and overrides any property defined there with those from ```application.prod.conf```

It works for ```logback.xml``` too, if ```logback.[env].xml``` is present, then **Jooby** will use it, otherwise it fallbacks to ```logback.xml```.

{{fatjar.md}}

{{stork.md}}

{{capsule.md}}

{{public-dir.md}}

# war?

{{war.md}}

# conclusion

* **jar/capsule deployment** makes perfect sense for PaaS like Heroku, AppEngine, etc...

* **stork deployment** give you more control and the sense of a traditional Java Server with start/stop scripts but also the power of control the application logs at runtime.
