# logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described here [logback configuration](http://logback.qos.ch/manual/configuration.html)


## bootstrap

It is useful that we can bundle logging  configuration files inside our jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need/want to debug errors the configuration files should/must be outside the jar, so you can turn on/off loggers, change log level etc..

On such cases all you have to do is start the application with the location of the logback configuration file:

    java -Dlogback.configFile=logback.xml -jar myapp.jar

The ```-Dlogback.configFile``` property controls the configuration file to load. More information can be found [here](http://logback.qos.ch/manual/configuration.html)
