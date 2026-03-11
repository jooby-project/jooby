[![Maven Central](https://img.shields.io/maven-central/v/io.jooby/jooby?label=stable)](https://central.sonatype.com/artifact/io.jooby/jooby)
[![Javadoc](https://javadoc.io/badge/io.jooby/jooby.svg)](https://javadoc.io/doc/io.jooby/jooby/latest)
[![Github](https://github.com/jooby-project/jooby/workflows/Full%20Build/badge.svg)](https://github.com/jooby-project/jooby/actions)
[![Discord](https://img.shields.io/discord/1225457509909922015?label=discord)](https://discord.gg/JmyxrKPvjY)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/io/jooby/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/io/jooby/README.md)
![GitHub Sponsors](https://img.shields.io/github/sponsors/jknack)

# &infin; do more, more easily

[Jooby](https://jooby.io) is a modular, high-performance web framework for Java and Kotlin. Designed for simplicity and speed, it gives you the freedom to build on your favorite server with a clean, modern API.

## 🚀 Built for Speed
- **High Performance**: Consistently ranks among the fastest Java frameworks in TechEmpower benchmarks.
- **Lightweight Footprint**: Low memory usage and fast startup times make it ideal for microservices environments.
- **Choose Your Engine**: Built to run on your favorite high-performance servers: Netty, Jetty, or Undertow.

## 🛠️ Developer Productivity
- **Instant Hot-Reload**: Save your code and see changes immediately without restarting the entire JVM.
- **Modular by Design**: Only use what you need. Jooby offers over 50 "thin" modules for database access (Hibernate, JDBI, Flyway), security (Pac4j), and more.
- **OpenAPI & Swagger**: Automatically generate interactive documentation for your APIs with built-in OpenAPI 3 support.

## 🧩 Unrivaled Flexibility
- **The Power of Choice**: Use the Script API (fluent, lambda-based routes) for simple apps, or the MVC API (annotation-based) for complex enterprise projects.
- **Reactive & Non-Blocking**: Full support for modern async patterns, including Kotlin Coroutines, RxJava, Reactor, and CompletableFutures.
- **First-Class Kotlin Support**: Native DSLs and features designed specifically to make Kotlin development feel intuitive and type-safe.

## Quick Start

Java:

```java
import static io.jooby.Jooby.runApp;

public class App {

  public static void main(final String[] args) {
    runApp(args, app -> {
      app.get("/", ctx -> "Welcome to Jooby!");
    });
  }
}

```

Kotlin:

```kotlin
import io.jooby.runApp

fun main(args: Array<String>) {
  runApp(args) {
    get ("/") {
      "Welcome to Jooby!"
    }
  }
}

```

documentation
=====

Documentation is available at [https://jooby.io](https://jooby.io)

help
=====
[Discord](https://discord.gg/JmyxrKPvjY)

support my work
=====
- [Sponsor](https://github.com/sponsors/jknack)
- [Paypal](https://www.paypal.com/paypalme2/edgarespina)
- [support@jooby.io](mailto:support@jooby.io?Subject=Jooby%20Support)

sponsors
======

| Logo                                                                                                                                   | Sponsor                                           |
|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| <img src="https://github.com/user-attachments/assets/4a3f519e-0b2e-4bb4-b2eb-624b05720e31" alt="Premium Minds" width="32" height="32"> | [@premium-minds](https://github.com/premium-minds) |
| <img src="https://avatars.githubusercontent.com/u/567706?v=4" alt="Adam Gent" width="32" height="32">     | [@agentgt](https://github.com/agentgt)                     |
| <img src="https://github.com/user-attachments/assets/51073649-6cba-4e7b-8eee-8c05f4b9648e" alt="David" width="32" height="32">         | [@tipsy](https://github.com/tipsy)                |

Previous version
=====

- v3: [Documentation](https://jooby.io/v3) and [source code](https://github.com/jooby-project/jooby/tree/3.x)
- v2: [Documentation](https://jooby.io/v2) and [source code](https://github.com/jooby-project/jooby/tree/2.x)
- v1: [Documentation](https://jooby.io/v1) and [source code](https://github.com/jooby-project/jooby/tree/1.x)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)

### Powered by
[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)

