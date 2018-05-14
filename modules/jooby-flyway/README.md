[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-flyway/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-flyway)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-flyway.svg)](https://javadoc.io/doc/org.jooby/jooby-flyway/1.3.0)
[![jooby-flyway website](https://img.shields.io/badge/jooby-flyway-brightgreen.svg)](http://jooby.org/doc/flyway)
# flyway

Evolve your database schema easily and reliably across all your instances.

This module run [Flyway](http://flywaydb.org) on startup and apply database migrations.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module to acquire a database connection.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-flyway</artifactId>
  <version>1.3.0</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());

  use(new Flywaydb());
}
```

If for any reason you need to maintain two or more databases:

```java
{
  use(new Jdbc("db1"));
  use(new Flywaydb("db1"));

  use(new Jdbc("db2"));
  use(new Flywaydb("db2"));
}
```

## migration scripts

[Flyway](http://flywaydb.org) looks for migration scripts at the ```db/migration``` classpath location.
We recommend to use [Semantic versioning](http://semver.org) for naming the migration scripts:

```
v0.1.0_My_description.sql
v0.1.1_My_small_change.sql
```

## commands
It is possible to run [Flyway](http://flywaydb.org) commands on startup, default command is: ```migrate```.

If you need to run multiple commands, set the ```flyway.run``` property:

```properties
flyway.run = [clean, migrate, validate, info]
```

## configuration

Configuration is done via ```application.conf``` under the ```flyway.*``` path.
There are some defaults setting that you can see in the appendix section.


For more information, please visit the [Flyway](http://flywaydb.org) site.

## flyway.conf
These are the default properties for flyway:

```properties
#

# Copyright 2010-2015 Axel Fontaine

#

# Licensed under the Apache License, Version 2.0 (the "License");

# you may not use this file except in compliance with the License.

# You may obtain a copy of the License at

#

#         http://www.apache.org/licenses/LICENSE-2.0

#

# Unless required by applicable law or agreed to in writing, software

# distributed under the License is distributed on an "AS IS" BASIS,

# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

# See the License for the specific language governing permissions and

# limitations under the License.

#

# If present, flyway will run as standalone on application startup. Otherwise, it depends on a DataSource provided by Jdbc module (usally)

# flyway.url=

# list of commands to run on startup

flyway.run = [migrate]

# Fully qualified classname of the jdbc driver (autodetected by default based on flyway.url)

# flyway.driver=

# User to use to connect to the database (default: <<null>>)

# flyway.user=

# Password to use to connect to the database (default: <<null>>)

# flyway.password=

# List of schemas managed by Flyway. These schema names are case-sensitive.

# (default: The default schema for the datasource connection)

# Consequences:

# - The first schema in the list will be automatically set as the default one during the migration.

# - The first schema in the list will also be the one containing the metadata table.

# - The schemas will be cleaned in the order of this list.

# flyway.schemas=

# Name of Flyway's metadata table (default: schema_version)

# By default (single-schema mode) the metadata table is placed in the default schema for the connection provided by the datasource.

# When the flyway.schemas property is set (multi-schema mode), the metadata table is placed in the first schema of the list.

# flyway.table=

# List of locations to scan recursively for migrations.

# The location type is determined by its prefix.

# Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both sql and java-based migrations.

# Locations starting with filesystem: point to a directory on the filesystem and may only contain sql migrations.

flyway.locations= [db/migration]

# List of fully qualified class names of custom MigrationResolver to use for resolving migrations.

# flyway.resolvers=

# File name prefix for Sql migrations (default: v )

# Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,

# which using the defaults translates to V1_1__My_description.sql

flyway.sqlMigrationPrefix=v

# File name separator for Sql migrations (default: _)

# Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,

# which using the defaults translates to V1_1__My_description.sql

flyway.sqlMigrationSeparator=_

# File name suffix for Sql migrations (default: .sql)

# Sql migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,

# which using the defaults translates to V1_1__My_description.sql

# flyway.sqlMigrationSuffix=

# Encoding of Sql migrations (default: UTF-8)

flyway.encoding=${application.charset}

# Whether placeholders should be replaced. (default: true)

# flyway.placeholderReplacement=

# Placeholders to replace in Sql migrations

# flyway.placeholders.user=

# flyway.placeholders.my_other_placeholder=

# Prefix of every placeholder (default: ${ )

# flyway.placeholderPrefix=

# Suffix of every placeholder (default: } )

# flyway.placeholderSuffix=

# Target version up to which Flyway should consider migrations.

# The special value 'current' designates the current version of the schema. (default: <<latest version>>)

# flyway.target=

# Whether to automatically call validate or not when running migrate. (default: true)

# flyway.validateOnMigrate=

# Whether to automatically call clean or not when a validation error occurs. (default: false)

# This is exclusively intended as a convenience for development. Even tough we

# strongly recommend not to change migration scripts once they have been checked into SCM and run, this provides a

# way of dealing with this case in a smooth manner. The database will be wiped clean automatically, ensuring that

# the next migration will bring you back to the state checked into SCM.

# Warning ! Do not enable in production !

# flyway.cleanOnValidationError=

# The version to tag an existing schema with when executing baseline. (default: 1)

# flyway.baselineVersion=

# The description to tag an existing schema with when executing baseline. (default: << Flyway Baseline >>)

# flyway.baselineDescription=

# Whether to automatically call baseline when migrate is executed against a non-empty schema with no metadata table.

# This schema will then be initialized with the baselineVersion before executing the migrations.

# Only migrations above baselineVersion will then be applied.

# This is useful for initial Flyway production deployments on projects with an existing DB.

# Be careful when enabling this as it removes the safety net that ensures

# Flyway does not migrate the wrong database in case of a configuration mistake! (default: false)

# flyway.baselineOnMigrate=

# Allows migrations to be run "out of order" (default: false).

# If you already have versions 1 and 3 applied, and now a version 2 is found,

# it will be applied too instead of being ignored.

# flyway.outOfOrder=

# This allows you to tie in custom code and logic to the Flyway lifecycle notifications (default: empty).

# Set this to a comma-separated list of fully qualified FlywayCallback class name implementations

# flyway.callbacks=
```
