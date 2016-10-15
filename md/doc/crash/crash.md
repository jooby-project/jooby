# crash

<a href="http://www.crashub.org">CRaSH</a> remote shell: connect and monitor JVM resources via HTTP, SSH or telnet.</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-crash</artifactId>
 <version>{{version}}</version>
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

* registry: Access to [Registry]({{defdocs}}/Registry.html).
* conf: Access to `Config`.

### routes command

The ```routes``` print all the application routes.

### conf command

The ```conf tree``` print the application configuration tree (configuration precedence).

The ```conf props [path]``` print all the application properties, sub-tree or a single property if ```path``` argument is present.

## fancy banner

Just add the [jooby-banner](/doc/banner) to your project and all the `CRaSH` shell will use it. Simple and easy!!
