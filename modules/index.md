---
layout: index
title: modules
version: 0.11.2
---

## mvn jooby:run
* [jooby:run](/doc/maven-plugin): maven plugin with hot reload of classes, powered by [JBoss Modules](https://github.com/jboss-modules/jboss-modules).

## asset pipeline
* [asset module](/doc/assets): CSS and JavaScript processing.

## parsers & renderers
* [jackson](/doc/jackson): JSON supports via Jackson.
* [gson](/doc/gson): JSON supports via Gson.

## template engines
* [handlebars/mustache](/doc/hbs): logic less and semantic Mustache templates.
* [freemarker](/doc/ftl): render templates with FreeMarker.

## session
* [redis](/doc/jedis/#redis-session-store): HTTP session on [Redis](http://redis.io).
* [memcached](/doc/spymemcached/#session-store): HTTP session on [Memcached](http://memcached.org).
* [mongodb](/doc/mongodb/#mongodb-session-store): HTTP session on [MongoDB](http://mongodb.github.io/mongo-java-driver/).
* [hazelcast](/doc/hazelcast/#session-store): HTTP session on [Hazelcast](http://hazelcast.org).
* [ehcache](/doc/ehcache/#session-store): HTTP session on [Ehcache](http://ehcache.org).

## sql
* [jdbc](/doc/jdbc): high performance connection pool for jdbc via [Hikari](https://github.com/brettwooldridge/HikariCP).
* [jdbi](/doc/jdbi): fluent API for JDBC.
* [flyway](/doc/flyway): database migrations via [Flyway](http://flywaydb.org).

## object relational mapping - ORM
* [hibernate](/doc/hbm): object/relational mapping via {{hibernate}}.
* [ebean](/doc/ebean): object/relational mapping via [Ebean ORM](http://ebean-orm.github.io).

## validation
* [hibernate validator](/doc/hbv): bean validation.

## mongodb
* [mongodb](/doc/mongodb): Mongodb driver.
* [morphia](/doc/morphia): Object/Document mapper via [Morphia](https://github.com/mongodb/morphia).
* [jongo](/doc/jongo): Query in Java as in Mongo Shell.

## caches
* [redis](/doc/jedis): Advanced cache and key/value store for Java.
* [memcached](/doc/spymemcached): In-memory key-value store for small chunks of arbitrary data.
* [hazelcast](/doc/hazelcast): [Hazelcast](http://hazelcast.org) for [Jooby](http://jooby.org).
* [ehcache](/doc/ehcache): Java's most widely-used cache.

## full text search
* [elastic search](/doc/elasticsearch): enterprise full text search via [Elastic Search](https://github.com/elastic/elasticsearch).

## async
* [akka](/doc/akka): build concurrent & distributed applications via [Akka](http://akka.io).

## amazon web services
* [aws](/doc/aws): Amazon web services ```s3, sns, sqs, ..., etc.```.

## swagger
* [swagger](/doc/swagger): powerful representation of your RESTful API.

## auth & security
* [pac4j](/doc/pac4j): authentication module via: [Pac4j](https://github.com/pac4j/pac4j).

## scheduling
* [quartz](/doc/quartz): advanced job scheduling.

## enterprise integration patterns (EIP)
* [camel](/doc/camel): enterprise service bus for Jooby.

## emails
* [commons-email](/doc/commons-email): Email supports via [Apache Commons Email](https://commons.apache.org/proper/commons-email).

## amazon web services
* [aws](/doc/aws): wire AWS services and expose them in Guice.

## css pre-processors
* [sass](/doc/sass): [Sass](http://sass-lang.com) CSS pre-processor via [Vaadin Sass Compiler](https://github.com/vaadin/sass-compiler).
* [less](/doc/less): [Less](http://lesscss.org) CSS pre-processor via [Less4j](https://github.com/SomMeri/less4j).

## servers
* [netty](/doc/netty)
* [jetty](/doc/jetty)
* [undertow](/doc/undertow)

## servlet
* [servlet](/doc/servlet): supports war deployments.
