/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rainbowgum;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.Environment;
import io.jooby.EnvironmentOptions;
import io.jooby.Jooby;
import io.jooby.LoggingService;
import io.jstach.rainbowgum.LogConfig;
import io.jstach.rainbowgum.LogRouter;
import io.jstach.rainbowgum.RainbowGum;

class RainbowGumServiceTest {
  @TempDir Path directory;

  @BeforeEach
  @AfterEach
  void closeLogging() {
    var gum = RainbowGum.getOrNull();
    if (gum != null) {
      gum.close();
    }
  }

  private Environment environment(Config config) {
    return new Environment(getClass().getClassLoader(), config, "test");
  }

  private Config configuration(String root) {
    return ConfigFactory.parseString(
        """
        logging.level.root = %s
        logging.level."com.example" = debug
        logging.appenders = [capture]
        logging.appender.capture.output.uri = "%s"
        logging.appender.capture.encoder.uri = pattern
        logging.appender.capture.encoder.pattern.pattern = "%%msg%%n"
        """
            .formatted(root, directory.resolve("application.log").toUri()));
  }

  private String output() throws Exception {
    return Files.readString(directory.resolve("application.log"));
  }

  @Test
  void discoversServicesAndReplaysEarlyLoggingIntoConfiguredOutput() throws Exception {
    assertInstanceOf(
        RainbowGumService.class,
        ServiceLoader.load(LoggingService.class).findFirst().orElseThrow());
    var bootstrap = RainbowGum.of();
    assertSame(LogRouter.global(), bootstrap.router());
    var early = LoggerFactory.getLogger("early.logger");
    early.info("before-environment");
    var app = new Jooby().setEnvironment(environment(configuration("debug")));
    assertNotSame(bootstrap, RainbowGum.of());
    assertTrue(early.isDebugEnabled());
    early.debug("after-environment");
    assertEquals("before-environment\nafter-environment\n", output());
    assertSame(app.getEnvironment(), app.getEnvironment());
  }

  @Test
  void readsQuotedAndUnquotedLoggerNamesAndNativeValues() {
    var config =
        ConfigFactory.parseString(
            """
            logging.level {
              root = warn
              "com.example" = debug
              org.dotted = error
              "tc.image@sha256:abc..." = trace
            }
            logging.groups = [database]
            logging.group.database = [org.postgresql]
            logging.level.database = info
            logging.change.logger."com.example" = [level]
            logging.caller.logger."com.example" = true
            logging.extra = { "user.name" = alice, retries = 3, active = true, unset = null }
            logging.nullValue = null
            """);
    var properties = new ConfigProperties(() -> config);
    assertEquals(List.of("database"), properties.listOrNull("logging.groups"));
    assertEquals(
        Map.of("user.name", "alice", "retries", "3", "active", "true"),
        properties.mapOrNull("logging.extra"));
    assertNull(properties.valueOrNull("logging.nullValue"));
    var gum = new RainbowGumService();
    gum.initialize(environment(config));
    var levels = RainbowGum.of().config().levelResolver();
    assertEquals(Level.WARNING, levels.resolveLevel("other"));
    assertEquals(Level.DEBUG, levels.resolveLevel("com.example.Child"));
    assertEquals(Level.ERROR, levels.resolveLevel("org.dotted.Child"));
    assertEquals(Level.TRACE, levels.resolveLevel("tc.image@sha256:abc..."));
    assertEquals(Level.INFO, levels.resolveLevel("org.postgresql.Driver"));
    assertTrue(RainbowGum.of().config().changePublisher().callerInfoEnabled("com.example.Child"));
  }

  @Test
  void applicationProfilesAndEnvironmentOptionsConfigureLogging() throws Exception {
    Files.writeString(directory.resolve("application.conf"), configuration("warn").root().render());
    Files.writeString(directory.resolve("application.test.conf"), "logging.level.root=error");
    var app = new Jooby();
    app.setEnvironmentOptions(
        new EnvironmentOptions().setBasedir(directory).setActiveNames("test"));
    assertEquals(Level.ERROR, RainbowGum.of().config().levelResolver().resolveLevel("other"));
    assertEquals(Level.DEBUG, RainbowGum.of().config().levelResolver().resolveLevel("com.example"));
  }

  @Test
  void lazyEnvironmentInitializesLoggingOnce() {
    var bootstrap = RainbowGum.of();
    var app = new Jooby();
    assertSame(bootstrap, RainbowGum.getOrNull());
    var environment = app.getEnvironment();
    var configured = RainbowGum.of();
    assertNotSame(bootstrap, configured);
    assertNotSame(LogRouter.global(), configured.router());
    assertSame(environment, app.getEnvironment());
    assertSame(configured, RainbowGum.of());
  }

  @Test
  void publishRereadsLevelsFromTheCurrentEnvironmentConfig() throws Exception {
    var env = environment(configuration("warn"));
    new Jooby().setEnvironment(env);
    var logger = LoggerFactory.getLogger("reload.logger");
    assertFalse(logger.isDebugEnabled());
    var gum = RainbowGum.of();
    env.setConfig(
        env.getConfig().withValue("logging.level.root", ConfigValueFactory.fromAnyRef("debug")));
    gum.config().changePublisher().publish();
    assertSame(gum, RainbowGum.of());
    assertTrue(logger.isDebugEnabled());
    logger.debug("reloaded");
    assertEquals("reloaded\n", output());
  }

  @Test
  void replacingEnvironmentClosesThePreviousOutputsAndUpdatesExistingLoggers() throws Exception {
    var app = new Jooby().setEnvironment(environment(configuration("warn")));
    var first = RainbowGum.of();
    var logger = LoggerFactory.getLogger("replacement.logger");
    assertFalse(logger.isDebugEnabled());
    app.setEnvironment(environment(configuration("debug")));
    assertNotSame(first, RainbowGum.of());
    assertTrue(logger.isDebugEnabled());
    logger.debug("replacement");
    assertEquals("replacement\n", output());
  }

  @Test
  void logOffRestoresInheritedLevelsEvenWhenNestedTaskThrows() throws Exception {
    new Jooby().setEnvironment(environment(configuration("debug")));
    var logger = LoggerFactory.getLogger("mute.logger");
    var service = new RainbowGumService();
    assertEquals(
        io.jstach.rainbowgum.LogConfig.ChangePublisher.ChangeMode.ON,
        RainbowGum.of().config().globals().changeMode());
    assertEquals(
        java.util.Set.of(io.jstach.rainbowgum.LogConfig.ChangePublisher.ChangeType.LEVEL),
        RainbowGum.of().config().changePublisher().allowedChanges("mute.logger"));
    assertThrows(
        IllegalStateException.class,
        () ->
            service.logOff(
                List.of("mute.logger"),
                () -> {
                  assertEquals(
                      Level.OFF,
                      RainbowGum.of().config().levelResolver().resolveLevel("mute.logger"));
                  assertFalse(logger.isErrorEnabled());
                  service.logOff(
                      List.of("mute.logger"), () -> assertFalse(logger.isErrorEnabled()));
                  assertFalse(logger.isErrorEnabled());
                  logger.error("hidden");
                  throw new IllegalStateException("expected");
                }));
    assertTrue(logger.isDebugEnabled());
    logger.debug("restored");
    assertEquals("restored\n", output());
  }

  @Test
  void mutingLiteralRootDoesNotMuteUnrelatedLoggers() {
    new Jooby().setEnvironment(environment(configuration("debug")));
    var root = LoggerFactory.getLogger("root");
    var other = LoggerFactory.getLogger("unrelated.logger");
    new RainbowGumService()
        .logOff(
            List.of("root"),
            () -> {
              assertFalse(root.isErrorEnabled());
              assertTrue(other.isDebugEnabled());
            });
    assertTrue(root.isDebugEnabled());
  }

  @Test
  void muteSurvivesBootstrapReplacementDuringServerStartup() {
    RainbowGum.of();
    var service = new RainbowGumService();
    service.logOff(
        List.of("server.logger"),
        () -> {
          new Jooby().setEnvironment(environment(configuration("debug")));
          assertEquals(
              Level.OFF, RainbowGum.of().config().levelResolver().resolveLevel("server.logger"));
          assertFalse(LoggerFactory.getLogger("server.logger").isErrorEnabled());
        });
    assertTrue(LoggerFactory.getLogger("server.logger").isDebugEnabled());
  }

  @Test
  void invalidConfigurationDoesNotUnbindTheWorkingInstance() throws Exception {
    var app = new Jooby().setEnvironment(environment(configuration("debug")));
    var current = RainbowGum.of();
    var invalid =
        configuration("debug")
            .withValue(
                "logging.appender.capture.encoder.uri",
                ConfigValueFactory.fromAnyRef("missing-encoder"));
    assertThrows(RuntimeException.class, () -> app.setEnvironment(environment(invalid)));
    assertSame(current, RainbowGum.of());
    LoggerFactory.getLogger("still.working").debug("still-working");
    assertEquals("still-working\n", output());
  }

  @Test
  void doesNotReplaceAnExplicitlyInstalledRainbowGum() {
    var custom = RainbowGum.builder(LogConfig.builder().build()).set();
    new Jooby().setEnvironment(environment(configuration("debug")));
    assertSame(custom, RainbowGum.of());
  }
}
