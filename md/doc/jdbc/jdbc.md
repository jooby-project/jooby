# jdbc

Production-ready jdbc data source, powered by the [HikariCP](https://github.com/brettwooldridge/HikariCP) library.

## exports

* ```DataSource```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jdbc</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

Via `connection string` property:

```java

{
  use(new Jdbc("jdbc:mysql://localhost/db"));

  // accessing to the data source
  get("/my-api", req -> {
    DataSource db = req.require(DataSource.class);
    // do something with datasource
  }); 
}
```

Via `db` property:

```java

{
  use(new Jdbc("db));

  // accessing to the data source
  get("/my-api", req -> {
    DataSource db = req.require(DataSource.class);
    // do something with datasource
  }); 
}
```

Or :

```java
public class Service {

   @Inject
   public Service(DataSource ds) {
     ...
   }
}
```

## configuration
Database configuration is controlled from your ```application.conf``` file using the ```db``` property and friends: ```db.*```.

### mem db

```properties
db = mem
```

Mem db is implemented with [h2 database](http://www.h2database.com/), before using it make sure to add the h2 dependency to your ```pom.xml```:

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
</dependency>
```

Mem db is useful for dev environment and/or transient data that can be regenerated.

### fs db

```properties
db = fs
```

File system db is implemented with [h2 database](http://www.h2database.com/), before using it make sure to add the h2 dependency to your ```pom.xml```:

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
</dependency>
```

File system db is useful for dev environment and/or transient data that can be regenerated. Keep in mind this db is saved in a tmp directory and db will be deleted it on restarts.


### db.url
Connect to a database using a jdbc url, some examples here:

```properties
# mysql
db.url = jdbc:mysql://localhost/mydb
db.user = myuser
db.password = password
```

Previous example, show you how to connect to **mysql**, setting user and password. But of course you need the jdbc driver on your ```pom.xml```:

```xml
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
</dependency>
```

## hikari configuration
If you need to configure or tweak the [hikari pool](https://github.com/brettwooldridge/HikariCP) just add ```hikari.*``` entries to your ```application.conf``` file:

```properties
db.url = jdbc:mysql://localhost/mydb
db.user = myuser
db.password = password
db.cachePrepStmts = true

# hikari
hikari.autoCommit = true
hikari.maximumPoolSize = 20
# etc...
```

Also, all the ```db.*``` properties are converted to ```dataSource.*``` to let [hikari](https://github.com/brettwooldridge/HikariCP) configurer the target jdbc connection.


## multiple connections
It is pretty simple to configure two or more db connections in [jooby](http://jooby.org).

Let's suppose we have a main database and an audit database for tracking changes:

```java
{
  use(new Jdbc("db.main")); // main database
  use(new Jdbc("db.audit")); // audit database
}
```

application.conf

```properties
# main database
db.main.url = ...
db.main.user=...
db.main.password = ...

# audit
db.audit.url = ....
db.audit.user = ....
db.audit.password = ....
```

Same principle applies if you need to tweak [hikari](https://github.com/brettwooldridge/HikariCP): 

```properties
# max pool size for main db
hikari.main.maximumPoolSize = 100

# max pool size for audit db
hikari.audit.maximumPoolSize = 20
```

Finally, if you need to inject the audit data source, all you have to do is to use the *Name* annotation, like ```@Name("db.audit")```

That's all folks! Enjoy it!!!

{{appendix}}
