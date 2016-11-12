# mongodb driver

{{mongodb}} driver for Jooby.

## exports

* [MongoClient]({{mongodbapi}}/MongoClient.html)
* [MongoDatabase]({{mongodbapi}}/DB.html)
* [Session Store]({{defdocs}}/mongodb/MongoSessionStore.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-mongodb</artifactId>
  <version>{{version}}</version>
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

For more detailed information please check: [MongoClientURI]({{mongodbapi}}/MongoClientURI.html).

## two or more connections

Use [named]({{defdocs}}/mongodb/Mongodb.html#-named) when you need two or more ```mongodb``` connections:

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

{{doc/mongodb/mongodb-session.md}}

{{appendix}}
