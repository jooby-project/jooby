/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.typesafe.config.Config;

/**
 * Controls the level of information logged during startup.
 *
 * @author edgar
 * @since 3.0.0
 */
public interface StartupSummary {

  /**
   * Print application name, process ID, server options, execution mode, environment and
   * configuration files, application/tmp directory and log file location.
   */
  StartupSummary VERBOSE =
      (application, server) -> {
        Logger logger = application.getLog();
        Environment env = application.getEnvironment();
        Config config = application.getConfig();
        logger.info("{} started with:", application.getName());
        logger.info("    PID: {}", config.getString(AvailableSettings.PID));
        logger.info("    {}", server.getOptions());
        logger.info("    execution mode: {}", application.getExecutionMode().name().toLowerCase());
        logger.info("    environment: {}", env);
        logger.info("    app dir: {}", config.getString("user.dir"));
        logger.info("    tmp dir: {}", application.getTmpdir());
        if (config.hasPath(AvailableSettings.LOG_FILE)) {
          logger.info("    log file: {}", config.getString(AvailableSettings.LOG_FILE));
        }
      };

  /** Print a single line with application and environment names. */
  StartupSummary DEFAULT =
      (application, server) -> {
        Logger logger = application.getLog();
        Environment env = application.getEnvironment();
        List<String> activeNames = env.getActiveNames();
        String environment = activeNames.size() == 1 ? activeNames.get(0) : activeNames.toString();
        logger.info("{} ({}) started", application.getName(), environment);
      };

  /** Print nothing. */
  StartupSummary NONE = (application, server) -> {};

  /** Print application routes and server path. */
  StartupSummary ROUTES =
      (application, server) -> {
        Logger logger = application.getLog();
        List<Object> args = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        buff.append("routes: \n\n{}\n\nlistening on:\n");
        args.add(application.getRouter());

        ServerOptions options = server.getOptions();
        String host = options.getHost().replace("0.0.0.0", "localhost");
        if (!options.isHttpsOnly()) {
          args.add(host);
          args.add(options.getPort());
          args.add(application.getContextPath());
          buff.append("  http://{}:{}{}\n");
        }

        if (options.isSSLEnabled()) {
          args.add(host);
          args.add(options.getSecurePort());
          args.add(application.getContextPath());
          buff.append("  https://{}:{}{}\n");
        }

        logger.info(buff.toString(), args.toArray(new Object[0]));
      };

  /**
   * Creates a summary level from string value.
   *
   * @param value String value.
   * @return Summary level or <code>{@link #DEFAULT}</code>.
   */
  static StartupSummary create(String value) {
    return switch (value.toLowerCase()) {
      case "verbose" -> VERBOSE;
      case "none" -> NONE;
      case "routes" -> ROUTES;
      // fallback
      default -> DEFAULT;
    };
  }

  /**
   * Log application summary.
   *
   * @param application Application.
   * @param server Server.
   */
  void log(Jooby application, Server server);
}
