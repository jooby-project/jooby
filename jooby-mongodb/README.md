# jooby-mongodb

[MongoDB](http://mongodb.github.io/mongo-java-driver/) driver for Jooby.

Exposes a [MongoClient](http://api.mongodb.org/java/2.13/com/mongodb/MongoClient.html) and a [DB](http://api.mongodb.org/java/2.13/com/mongodb/DB.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-mongodb</artifactId>
  <version>0.4.2.1</version>
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
    MongoClient client = req.require(MongoClient.class);
    // work with client
    DB = req.require(DB.class);
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

  get("/", req {@literal ->} {
    DB mydb = req.require(DB.class);
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
  use(new Mongodb("db1").named());
  use(new Mongodb("db2").named());

  get("/", req -> {
    MongoClient client1 = req.require("db1", MongoClient.class);
    // work with db1
    MongoClient client2 = req.require("db2", MongoClient.class);
    // work with db2
  });
}
```

That's all folks! Enjoy it!!
