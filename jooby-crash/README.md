# crash

<a href="http://www.crashub.org">CRaSH</a> remote shell: connect and monitor JVM resources via HTTP, SSH or telnet.</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-crash</artifactId>
 <version>1.1.2</version>
</dependency>
```

## usage

```java

import org.jooby.crash;

{
  use(new Crash());
}
```

Just drop your commands in the `cmd` folder and <a href="http://www.crashub.org">CRaSH</a> will pick up all them.

Now let's see how to connect and interact with the <a href="http://www.crashub.org">CRaSH shell</a>.

## connectors

### HTTP connector

The HTTP connector is a simple yet powerful collection of HTTP endpoints where you can run a <a href="http://www.crashub.org/1.3/reference.html#developping_commands">CRaSH command</a>:

```java
{
  use(new Crash()
     .plugin(HttpShellPlugin.class)
  );
}
```

Try it:

```
GET /api/shell/thread/ls
```

OR:

```
GET /api/shell/thread ls
```

The connector is listening at ```/api/shell```. If you want to mount the connector some where else just set the property: ```crash.httpshell.path```.

### SSH connector

Just add the <a href="https://mvnrepository.com/artifact/org.crashub/crash.connectors.ssh/latest">crash.connectors.ssh</a> dependency to your project.

Try it:

```
ssh -p 2000 admin@localhost
```

Default user and password is: ```admin```. See how to provide a custom <a href="http://www.crashub.org/1.3/reference.html#pluggable_auth">authentication plugin</a>.

### telnet connector

Just add the <a href="https://mvnrepository.com/artifact/org.crashub/crash.connectors.telnet/latest">crash.connectors.telnet</a> dependency to your project.

Try it:

```
telnet localhost 5000
```

Checkout complete <a href="http://www.crashub.org/1.3/reference.html#_telnet_connector">telnet connector</a> configuration.

### web connector

Just add the <a href="https://mvnrepository.com/artifact/org.crashub/crash.connectors.web/latest">crash.connectors.web</a> dependency to your project.

Try it:

```
GET /shell
```

A web shell console will be ready to go at ```/shell```. If you want to mount the connector some where else just set the property: ```crash.webshell.path```.

## commands

You can write additional shell commands using Groovy or Java, see the <a href="http://www.crashub.org/1.3/reference.html#developping_commands">CRaSH documentation for details</a>. CRaSH search for commands in the ```cmd``` folder.

Here is a simple ‘hello’ command that could be loaded from ```cmd/hello.groovy``` folder:

```java
package commands

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext

class hello {

  @Usage("Say Hello")
  @Command
  def main(InvocationContext context) {
    return "Hello"
  }
}
```

Jooby adds some additional attributes and commands to `InvocationContext` that you can access from your command:

* registry: Access to [Registry](/apidocs/org/jooby/Registry.html).
* conf: Access to `Config`.

Example:

```java
package commands

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext

class HelloMyService {

  @Usage("MySerivce.doSomething")
  @Command
  def execute(InvocationContext context) {
    def registry = context.registry

    return registry.require(MySerivce.class).doSomething()
  }
}
```

### routes command

The ```routes``` print all the application routes.

```
dev> routes
order method pattern             consumes produces name       source
-------------------------------------------------------------------------------------------------
0     GET    /shell/css/**       [*/*]    [*/*]    /anonymous org.jooby.crash.WebShellPlugin:41
0     GET    /shell/js/**        [*/*]    [*/*]    /anonymous org.jooby.crash.WebShellPlugin:42
0     GET    /shell              [*/*]    [*/*]    /anonymous org.jooby.crash.WebShellPlugin:45
0     GET    /api/shell/{cmd:.*} [*/*]    [*/*]    /anonymous org.jooby.crash.HttpShellPlugin:43
0     *      {before}/path       [*/*]    [*/*]    /anonymous app.CrashApp:21
0     *      {after}/path        [*/*]    [*/*]    /anonymous app.CrashApp:24
0     *      {complete}/path     [*/*]    [*/*]    /anonymous app.CrashApp:28
0     GET    /                   [*/*]    [*/*]    /anonymous app.CrashApp:31


method pattern consumes         produces
-------------------------------------------------
WS     /shell  application/json application/json
```

### conf command

The ```conf tree``` print the application configuration tree (configuration precedence):

```
dev> conf tree
 merge of system properties
  org/jooby/crash/crash.conf @ file:/jooby-crash/target/classes/org/jooby/crash/crash.conf
   org/jooby/spi/server.conf @ file:/jooby-netty/target/classes/org/jooby/spi/server.conf
    org/jooby/mime.properties @ file:/jooby/target/classes/org/jooby/mime.properties
     org/jooby/jooby.conf @ file:/jooby/target/classes/org/jooby/jooby.conf: 1
```

The ```conf props [path]``` print all the application properties, sub-tree or a single property if ```path``` argument is present.

```
dev> conf props
name                                   value
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
mime.pgn                               application/x-chess-pgn
os.version                             10.11.6
mime.ttf                               font/truetype
err.java.io.FileNotFoundException      404
sun.cpu.isalist
mime.wtls-ca-certificate               application/vnd.wap.wtls-ca-certificate
mime.texinfo                           application/x-texinfo
runtime.concurrencyLevel               8
.....
```

```
dev> conf props application
name                       value
--------------------------------------------------------------------------------
application.tmpdir         /var/folders/9l/fyb_j4l553z6ql4443yttbj40000gn/T/app
application.version        0.0.0
application.ns             app
application.port           8080
application.charset        UTF-8
application.redirect_https
application.class          app.CrashApp
application.tz             America/Argentina/Buenos_Aires
application.numberFormat   #,##0.###
application.dateFormat     dd-MMM-yyyy
application.env            dev
application.host           localhost
application.name           app
application.path           /
application.lang           en-US
```

```
dev> conf props application.port
name             value
-----------------------
application.port 8080
```

## fancy banner

Just add the [jooby-banner](/doc/banner) to your project and all the `CRaSH` shell will use it.


```java
{
  use(new Banner("crash me!"));

  use(new Crash());
}
```

    telnet localhost 5000

```
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
_____                        _____
__________________ __________  /_       ______ ___ ____ __  /
  ___/_  ___/  __ `/_  ___/_  __ \      _  __ `__ \  _ \_  / 
 /__    /     /_/ / (__  )   / / /        / / / / /  __//_/  
___/  _/     __,_/  ____/  _/ /_/       _/ /_/ /_/ ___/ _) v0.0.0

dev>
```

Simple and easy!!
