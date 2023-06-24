/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Enumerates the configuration properties supported by Jooby.
 *
 * <p>The settings defined here may be specified at configuration time:
 *
 * <ul>
 *   <li>in a configuration file, for example, in <code>application.conf</code>, or
 *   <li>via program arguments, for example: <code>java -jar myapp.jar myprop=value</code>
 *   <li>via JVM properties, for example: <code>java -Dmyprop=value -jar myapp.jar</code>
 *   <li>via ENV variable, for example: <code>myprop=value java -jar myapp.jar</code>
 * </ul>
 *
 * <p>
 *
 * @since 3.0.0
 */
public interface AvailableSettings {
  /**
   * Charset used by your application. Used by template engine, HTTP encoding/decoding, database
   * driver, etc. Default value is <code>UTF-8</code>.
   */
  String CHARSET = "application.charset";

  /**
   * Control the application environment. Use to identify <code>dev</code> vs <code>non-dev</code>
   * application deployment. Jooby applies some optimizations for <code>non-dev</code> environments.
   * Default value is <code>dev</code>.
   */
  String ENV = "application.env";

  /**
   * The languages your application supports. Used by {@link Context#locale()}. Default is {@link
   * Locale#getDefault()}.
   */
  String LANG = "application.lang";

  /**
   * Location of log configuration file. Used by {@link LogConfigurer} and setting up at application
   * bootstrap time. You don't need to set this property.
   */
  String LOG_FILE = "application.logfile";

  /**
   * The base package of your application this is computed from {@link Jooby#runApp(String[],
   * Supplier)}. It may be used by modules to do package scanning.
   */
  String PACKAGE = "application.package";

  /** Application process ID. */
  String PID = "application.pid";

  /** The level of information logged during startup. See {@link StartupSummary}. */
  String STARTUP_SUMMARY = "application.startupSummary";

  /**
   * Application temporary directory. Used to dump/save temporary files. Default value is: <code>
   * Paths.get(System.getProperty("user.dir"), "tmp")</code>
   */
  String TMP_DIR = "application.tmpdir";
}
