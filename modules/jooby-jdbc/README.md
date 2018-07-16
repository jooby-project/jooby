[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jdbc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jdbc)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jdbc.svg)](https://javadoc.io/doc/org.jooby/jooby-jdbc/1.5.0)
[![jooby-jdbc website](https://img.shields.io/badge/jooby-jdbc-brightgreen.svg)](http://jooby.org/doc/jdbc)
# jdbc

Production-ready jdbc data source, powered by the [HikariCP](https://github.com/brettwooldridge/HikariCP) library.

## exports

* ```DataSource```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jdbc</artifactId>
  <version>1.5.0</version>
</dependency>
```

## usage

Via `connection string` property:

```java

{
  use(new Jdbc("jdbc:mysql://localhost/db"));

  // accessing to the data source
  get("/my-api", req -> {
    DataSource db = require(DataSource.class);
    // do something with datasource
  }); 
}
```

Via `db` property:

```java

{
  use(new Jdbc("db"));

  // accessing to the data source
  get("/my-api", req -> {
    DataSource db = require(DataSource.class);
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

Database configuration is controlled from your ```application.conf``` file using the ```db``` properties.

### memory

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

### filesystem

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

File system db is useful for dev environment and/or transient data that can be regenerated. Keep in mind this db is saved in a tmp directory and db will be deleted it on OS restart.

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

Same principle applies if you need to tweak [hikari](https://github.com/brettwooldridge/HikariCP) per database: 

```properties
# max pool size for main db

db.main.hikari.maximumPoolSize = 100

# max pool size for audit db

db.audit.hikari.maximumPoolSize = 20
```

The first registered database is the **default** database. The **second** database is accessible by name and type:

```java
{
  use(new Jdbc("db.main"));
  use(new Jdbc("db.audit"));

  get("/db", req -> {
    DataSource maindb = require(DataSource.class);
    DataSource auditdb = require("db.audit", DataSource.class);
    // ...
  });
}
```

or via `@Inject` annotation:

```java
public class Service {

  @Inject
  public Service(DataSource maindb, @Named("db.audit") DataSource auditdb) {
    // ...
  }
}
```

## jdbc.conf
These are the default properties for jdbc:

```properties
# Jdbc defaults

databases {

  ###############################################################################################

  # connection templates

  ###############################################################################################

  mem {

    dataSource.url = "jdbc:h2:mem:{mem.seed};DB_CLOSE_DELAY=-1"

    dataSource.user = sa

    dataSource.password = ""

  }

  fs {

    dataSource.url = "jdbc:h2:"${application.tmpdir}/${application.name}

    dataSource.user = sa

    dataSource.password = ""

  }

  ###############################################################################################

  # derby

  #      url => jdbc:derby:${db};create=true

  ###############################################################################################

  derby {

    dataSourceClassName = org.apache.derby.jdbc.ClientDataSource

  }

  ###############################################################################################

  # db2

  #    url => jdbc:db2://127.0.0.1:50000/SAMPLE 

  ###############################################################################################

  db2 {

    dataSourceClassName = com.ibm.db2.jcc.DB2SimpleDataSource

  }

  ###############################################################################################

  # h2

  #   url => mem, fs or jdbc:h2:${db}

  ###############################################################################################

  h2 {

    dataSourceClassName = org.h2.jdbcx.JdbcDataSource

  }

  ###############################################################################################

  # hsqldb

  #       url =>  jdbc:hsqldb:file:${db}

  ###############################################################################################

  hsqldb {

    dataSourceClassName = org.hsqldb.jdbc.JDBCDataSource

  }

  ###############################################################################################

  # mariadb

  #        url jdbc:mariadb://<host>:<port>/<database>?<key1>=<value1>&<key2>=<value2>...

  ###############################################################################################

  mariadb {

    dataSourceClassName = org.mariadb.jdbc.MySQLDataSource

  }

  ###############################################################################################

  # mysql

  #      url jdbc:mysql://<host>:<port>/<database>?<key1>=<value1>&<key2>=<value2>...

  ###############################################################################################

  mysql: [{

    # v6.x

    dataSourceClassName = com.mysql.cj.jdbc.MysqlDataSource

  }, {

    # v5.x

    dataSourceClassName = com.mysql.jdbc.jdbc2.optional.MysqlDataSource

    dataSource.encoding = ${application.charset}

    dataSource.cachePrepStmts = true

    dataSource.prepStmtCacheSize = 250

    dataSource.prepStmtCacheSqlLimit = 2048

    dataSource.useServerPrepStmts = true

  }]

  ###############################################################################################

  # sqlserver

  #          url => jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]

  ###############################################################################################

  sqlserver {

    dataSourceClassName = com.microsoft.sqlserver.jdbc.SQLServerDataSource

  }

  ###############################################################################################

  # oracle

  #       url => jdbc:oracle:thin:@//<host>:<port>/<service_name>

  ###############################################################################################

  oracle {

    dataSourceClassName = oracle.jdbc.pool.OracleDataSource

  }

  ###############################################################################################

  # pgjdbc-ng

  #          url => jdbc:pgsql://<server>[:<port>]/<database>

  ###############################################################################################

  pgsql {

    dataSourceClassName = com.impossibl.postgres.jdbc.PGDataSourceWithUrl

  }

  ###############################################################################################

  # postgresql

  #            url => jdbc:postgresql://host:port/database

  ###############################################################################################

  postgresql {

    dataSourceClassName = org.postgresql.ds.PGSimpleDataSource

  }

  ###############################################################################################

  # sybase

  #        url => jdbc:jtds:sybase://<host>[:<port>][/<database_name>]

  ###############################################################################################

  sybase {

    dataSourceClassName = com.sybase.jdbcx.SybDataSource

  }

  ###############################################################################################

  # firebird

  #         jdbc:firebirdsql:host[/port]:<database>

  ###############################################################################################

  firebirdsql {

    dataSourceClassName = org.firebirdsql.pool.FBSimpleDataSource

  }

  ###############################################################################################

  # sqlite

  #        jdbc:sqlite:${db}

  ############################################################################################### 

  sqlite {

    dataSourceClassName = org.sqlite.SQLiteDataSource

  }

  ###############################################################################################

  # log4jdbc

  #        jdbc:log4jdbc:${dbtype}:${db}

  ###############################################################################################

  log4jdbc {

    driverClassName = net.sf.log4jdbc.DriverSpy

  }

}

##

# hikari.maximumPoolSize = Math.max(10, ${runtime.processors-x2})
```
