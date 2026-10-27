# Rainbow Gum integration review notes

Working notes for this branch. Remove before merging.

## Public API added outside this module

Only `io.jooby.LoggingService` changes:

- `default void initialize(Environment environment)`: a hook for logging systems configured from the
  Jooby environment. The default does nothing, so `jooby-logback` and `jooby-log4j` are unchanged.
- `static void configure(Environment environment)`: finds the installed `LoggingService` with the
  environment's class loader and calls `initialize`. It is public only because interface static
  methods cannot be package private; `Jooby` is its only caller. It overloads the existing
  `configure(ClassLoader, List<String>)`.

`Jooby` keeps its signatures, but `setEnvironment`, `setEnvironmentOptions` and the lazy load in
`getEnvironment` now all go through `setEnvironment`, which calls `LoggingService.configure`. Every
environment Jooby loads or is given now does a `ServiceLoader` lookup.

The BOM and the parent pom gain `jooby-rainbowgum`, and the parent pom gains `rainbowgum.version`.

## Changed in review

- **Bootstrap marker.** The marker that lets Jooby replace the bootstrap Rainbow Gum was added by a
  `PropertiesProvider`, which runs for every service loaded `LogConfig`. A Rainbow Gum the
  application built from such a config, or supplied by its own `RainbowGumProvider`, was closed and
  replaced when the environment loaded. `RainbowGumBootstrap` now builds its own config with the
  marker and has the lowest priority.
- **Configured once.** Only the queued bootstrap instance is replaced. Loggers with a fixed level
  stay bound to the instance that created them, so rebuilding logging for a later environment left
  them writing to a closed instance and their events were lost. Making every logger changeable hid
  this. Later environments (`runApp` with several applications, another `setEnvironment(...)`, test
  suites starting an application per class) now keep the first configuration, as Logback and Log4j
  do.
- **Level changes only where muting needs them.** Defaults allow level changes for the loggers Jetty
  and Undertow mute (`org.eclipse.jetty.server`, `io.undertow`, `org.xnio`, `org.jboss.threads`)
  instead of every logger, so other loggers keep the fixed level fast path. Rereading levels with
  `changePublisher().publish()` is opt in through `logging.change.logger`.

## Open on this branch

- **Unbounded queue.** If no Jooby environment is ever loaded (unit tests that never read the
  config, command line tools), INFO and above queue in memory for the life of the JVM and are never
  written. Only ERROR reaches stderr. Probably needs a Rainbow Gum side fix: a bounded queue or a
  flush at shutdown.
- **Muting with an application Rainbow Gum.** Muting levels are only wired into the module's own
  config, so `logOff` does nothing when the application installed its own Rainbow Gum.
- **Failed logging setup.** `Jooby.setEnvironment` stores the environment before configuring
  logging. If the logging config is invalid the call throws, but `getEnvironment()` then returns that
  environment and logging stays queued.
- **Jooby Run.** Dependencies stay loaded across restarts in the default mode, so logging keeps the
  first start's configuration (documented). The configured instance also keeps the first
  `Environment`, and with it the first project class loader, reachable. With
  `useSingleClassLoader` everything reloads, so each restart configures a new instance, but nothing
  closes the previous class loader's instance or removes its shutdown hook. This is from reading the
  code, not tested.

## GraalVM native image

Checked with GraalVM CE 25.0.4 `native-image` on a small Jetty application with a static logger
and a `logging` block in `application.conf`. This module needs no native image metadata: its
service providers are registered from `META-INF/services`, and the native executable printed the
same output as the JVM run (early message replayed through the configured pattern, debug levels
from the config, Jetty startup muted).

Jooby itself ships no native image metadata. Without it, a classpath `application.conf` is not in
the image, and Jooby silently falls back to defaults (port 8080, default logging). The application
had to add `META-INF/native-image/<group>/<artifact>/reachability-metadata.json` with
`{"resources": [{"glob": "application*.conf"}]}`. Jooby could ship that resource entry in its own
jar.

## Jooby bugs outside this module

- **Log4j `logOff` does not restore exactly configured loggers.** `Log4jService.setLog4jLevel` reads
  `logger.getLevel()` after `Configurator.setLevel`, which sets an existing `LoggerConfig` with that
  exact name in place (checked against log4j-core 2.26.1). The saved level is OFF, so a muted logger
  configured by name in `log4j2.xml` stays off. The first mute also adds a `LoggerConfig` with that
  name, so after one start and stop every muted logger is affected on the next start in the same JVM.
  Fix: read the level before setting it.
- **Jetty mutes `org.eclipse.jetty.server.Server` twice.** `JettyServer.getLoggerOff()` lists it
  twice. Logback and Log4j `logOff` record the previous level per name, so the second entry records
  OFF over the original and the logger stays off after startup. Fix: drop the duplicate, or apply
  `distinct()` in `MutedServer`.
