# logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described here [logback configuration](http://logback.qos.ch/manual/configuration.html)


## bootstrap

It is useful that we can bundle logging  configuration files inside our jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need want to debug errors the configuration files should /must be outside the jar, so you can turn on/off loggers, change log level etc..

On such cases all you have to do is to put the ```logback.xml``` file outside the jar and in the same directory where the jar is.

    cd /myapp-dir
    ls
    myapp.jar logback.xml

The bootstrap process look for a file in the same directory where you app was launched (user.dir property) if the file is found there it will be selected. Otherwise, it fallback to the root of the classpath.

If at the time you started your app the console shows a lot of logs statement, that is because log wasn't configured properly. Either, the config file is missing or it has syntax errors.
