## logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described in detail [here](http://logback.qos.ch/manual/configuration.html).

You will usually find the `logback.xml` file inside the `conf` directory. Also, you can define a `logback.xml` file per `application.env` by appending the `env`. See some examples:

* `logback.uat.xml` when `application.env = uat`
* `logback.prod.xml` when `application.env = prod`

Of course if the `logback[.env].xml` isn't, present we fallback to default one `logback.xml`.
