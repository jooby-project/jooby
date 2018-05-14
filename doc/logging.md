## logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described in detail [here](http://logback.qos.ch/manual/configuration.html).

You will usually find the `logback.xml` file inside the `conf` directory. Also, you can define a `logback.xml` file per `application.env` by appending the `env`. See some examples:

* `logback.uat.xml` when `application.env = uat`
* `logback.prod.xml` when `application.env = prod`

If the `logback[.env].xml` file isn't present, `logback.xml` will be used as a fallback.
