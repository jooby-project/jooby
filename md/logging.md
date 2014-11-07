# logging

Logging is done via [logback](http://logback.qos.ch). Logging configuration files looks like: ```logback.[mode].xml```.

Configuration files can be bundle with the **fat jar**. This for example is allowed:

    /config
               logback.dev.xml
               logback.stage.xml
               logback.prod.xml

In **dev** the file with the **logback.dev.xml** will be used for logging. The others can be selected in one of two ways:

* **application.mode**

```
    java -Dapplication.mode=stage -jar myapp.jar // logback.stage.xml

    java -Dapplication.mode=prod -jar myapp.jar // logback.prod.xml
 
```

* **logback.configurationFile**

```
    java -DconfigurationFile=logback.stage.xml -jar myapp.jar // logback.stage.xml

    java -DconfigurationFile=logback.prod.xml -jar myapp.jar // logback.prod.xml
```

## bootstrap

It is useful that we can bundle logging  configuration files inside our jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need want to debug errors the configuration files should /must be outside the jar, so you can turn on/off loggers, change log level etc..

For such cases all you have to do is to put the ```logback.[mode].xml``` file outside the jar and in the same directory where the jar is.

    cd /myapp-dir
    ls
    myapp.jar logback.prod.xml

The bootstrap process look for a file in the same directory where you app was launched (user.dir property) if the file is found there it will be selected. Otherwise, it fallback to the root of the classpath.

If at the time you started your app the console shows a lot of logs statement, that is because log wasn't configured properly. Either, the config file is missing or it has syntax errors.
 
