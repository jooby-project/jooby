[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-mongodb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-mongodb)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-mongodb.svg)](https://javadoc.io/doc/org.jooby/jooby-mongodb/1.4.1)
[![jooby-mongodb website](https://img.shields.io/badge/jooby-mongodb-brightgreen.svg)](http://jooby.org/doc/mongodb)
# mongodb driver

[MongoDB](http://mongodb.github.io/mongo-java-driver/) driver for Jooby.

## exports

* [MongoClient](http://api.mongodb.org/java/2.13/com/mongodb/MongoClient.html)
* [MongoDatabase](http://api.mongodb.org/java/2.13/com/mongodb/DB.html)
* [Session Store](/apidocs/org/jooby/mongodb/MongoSessionStore.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-mongodb</artifactId>
  <version>1.4.1</version>
</dependency>
```

## usage

application.conf:

```properties
db = "mongodb://localhost/mydb"
```

```java
{
  use(new Mongodb());

  get("/", req -> {
    MongoClient client = require(MongoClient.class);
    // work with client
    MongoDatabase = require(MongoDatabase.class);
    // work with mydb
  });
}
```

Default URI connection property is ```db``` but of course you can use any other name:

application.conf:

```properties
mydb = "mongodb://localhost/mydb"
```

```java
{
  use(new Mongodb("mydb"));

  get("/", req -> {
    MongoDatabase mydb = require(MongoDatabase.class);
    // work with mydb
  });
}
```

## options

Options can be set via ```.conf``` file:

```properties
mongodb.connectionsPerHost  = 100
```

or programmatically:

```java
{
  use(new Mongodb()
    .options((options, config) -> {
      options.connectionsPerHost(100);
    })
  );
}
```

### connection URI

Default connection URI is defined by the ```db``` property. Mongodb URI looks like:

```properties
db = mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
```

For more detailed information please check: [MongoClientURI](http://api.mongodb.org/java/2.13/com/mongodb/MongoClientURI.html).

## two or more connections

Use [named](/apidocs/org/jooby/mongodb/Mongodb.html#-named) when you need two or more ```mongodb``` connections:

```java
{
  use(new Mongodb("db1"));
  use(new Mongodb("db2"));

  get("/", req -> {
    MongoClient client1 = require("db1", MongoClient.class);
    // work with db1
    MongoClient client2 = require("db2", MongoClient.class);
    // work with db2
  });
}
```

# mongodb session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-mongodb</artifactId>
  <version>1.4.1</version>
</dependency>
```

## usage

```java
{
  use(new Mongodb());

  session(MongoSessionStore.class);

  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

The ```name``` attribute and value will be stored in a [MongoDB](http://mongodb.github.io/mongo-java-driver/).

## options

### timeout

By default, a mongodb session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties

# 8 hours

session.timeout = 8h

# 15 seconds

session.timeout = 15

# 120 minutes

session.timeout = 120m

# no timeout

session.timeout = -1
```

It uses [MongoDB's TTL](docs.mongodb.org/manual/core/index-ttl) collection feature (2.2+) to have ```mongod``` automatically remove expired sessions.

### session collection

Default [MongoDB](http://mongodb.github.io/mongo-java-driver/) collection is ```sessions```.

It's possible to change the default key setting the ```mongodb.sesssion.collection``` properties.

## mongodb.conf
These are the default properties for mongodb:

```properties
###################################################################################################

# mongodb

###################################################################################################

mongodb.connectionsPerHost = 100

mongodb.threadsAllowedToBlockForConnectionMultiplier = 5

mongodb.maxWaitTime = 120s

mongodb.connectTimeout = 10s

mongodb.socketTimeout = 0

mongodb.socketKeepAlive = false

mongodb.cursorFinalizerEnabled = true

mongodb.alwaysUseMBeans = false

mongodb.heartbeatFrequency = 5000

mongodb.minHeartbeatFrequency = 500

mongodb.heartbeatConnectTimeout = 20s

mongodb.heartbeatSocketTimeout = 20s

###################################################################################################

# session datastore

#  collection: sessions

#  timeout: 30m

###################################################################################################

mongodb.session.collection = sessions
```
