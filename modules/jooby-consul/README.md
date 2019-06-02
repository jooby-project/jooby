[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-consul/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-consul/1.6.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-consul.svg)](https://javadoc.io/doc/org.jooby/jooby-consul/1.6.1)
[![jooby-consul website](https://img.shields.io/badge/jooby-consul-brightgreen.svg)](http://jooby.org/doc/consul)
# consul

[Consul](https://www.consul.io) client module. 

Exports a Consul [client](https://github.com/OrbitzWorldwide/consul-client).

Also register the application as a service and setup a health check.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-consul</artifactId>
  <version>1.6.1</version>
</dependency>
```

## usage

```java
{
  use(new Consulby());
  
  get("/myservice/health", req -> {
    Consul consul = require(Consul.class);
    List<ServiceHealth> serviceHealths = consul.healthClient()
      .getHealthyServiceInstances("myservice")
      .getResponse();
    return serviceHealths;
  });
}
```

## configuration

Configuration is done via ```.conf```.
 
For example, one can change the consul endpoint url,
change the advertised service host, and disable registration health check:

```properties
consul.default.url = "http://consul.internal.domain.com:8500"
consul.default.register.host = 10.0.0.2
consul.default.register.check = null
```

or, disable the automatic registration feature completely:

```properties
consul.default.register = null
```

Also, `Consul` and `Registration` objects can be configured programmatically:

```java
{
  use(new Consulby()
      .withConsulBuilder(consulBuilder -> {
        consulBuilder.withPing(false);
        consulBuilder.withBasicAuth("admin", "changeme");
      })
      .withRegistrationBuilder(registrationBuilder -> {
        registrationBuilder.enableTagOverride(true);
        registrationBuilder.id("custom-service-id");
      }));
}
```

## multiple consul

The module can be instantiated more than one time to allow connecting to many Consul installations: 

```java
{
  use(new Consulby("consul1"));
  use(new Consulby("consul2"));
}
```

Since the module will fallback on the `consul.default` config prefix,
it is possible to only override the desired properties in the `.conf`,
for example, here, disabling health check only for `consul2`:

```properties
consul.consul1.url = "http://consul1.internal.domain.com:8500"

consul.consul2.url = "http://consul2.internal.domain.com:8500"
consul.consul2.register.check = null
```

## consul.conf

```properties
consul {
  default {
    url = "http://localhost:8500"
    register {
      name = ${application.name}
      host = ${application.host}
      port = ${application.port}
      tags = []
      check {
        path = /health
        response = ${application.name}-${application.version}
        interval = 15s
        timeout = 5s
      }
    }
  }
}
```
