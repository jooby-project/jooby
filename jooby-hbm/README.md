# jooby-hbm

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbm</artifactId>
  <version>0.4.2.1</version>
</dependency>
```
## usage


# appendix: hbm.conf
```properties
hibernate {
  id.new_generator_mappings = true
  archive.autodetection = class
  hbm2ddl.auto = update
}

javax.persistence {
}

```


