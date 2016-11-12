# hazelcast

Provides cache solution and session storage via {{hazelcast}}.

## exports

* ```HazelcastInstance```
* Optionally, a [session store]({{defdocs}}/hazelcast/HcastSessionStore.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hazelcast</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Hcast());

  get("/", req -> {
    HazelcastInstance hcast = require(HazelcastInstance.class);
    ...
  });
}
```

## configuration

Any property under ```hazelcast.*``` will be automatically add it while bootstrapping a ```HazelcastInstance```.

Configuration can be done programmatically via: ```doWith(Consumer)```

```java
{
  use(new Hcast()
   .doWith(config -> {
     config.setXxx
   })
  );
}
```

{{doc/hazelcast/hazelcast-session.md}}

Happy coding!!!

{{appendix}}
