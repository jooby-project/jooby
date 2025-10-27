/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.github.kagkarlsson.scheduler.PollingStrategyConfig;
import com.github.kagkarlsson.scheduler.SchedulerBuilder;
import com.github.kagkarlsson.scheduler.jdbc.JdbcTaskRepository;
import com.github.kagkarlsson.scheduler.logging.LogLevel;
import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Default schedule properties. It can be created from configuration files using {@link
 * #from(Config, String)}.
 *
 * @since 3.2.10
 * @author edgar
 */
public class DbSchedulerProperties {

  /** Whether to enable the db-scheduler. */
  private boolean enabled = true;

  /***
   * <p>Number of threads.
   */
  private int threads = 10;

  /** How often to update the heartbeat timestamp for running executions. */
  private Duration heartbeatInterval = SchedulerBuilder.DEFAULT_HEARTBEAT_INTERVAL;

  /**
   * Name of this scheduler-instance. The name is stored in the database when an execution is picked
   * by a scheduler.
   *
   * <p>If the name is {@code null} or not configured, the hostname of the running machine will be
   * used.
   */
  private String schedulerName;

  /**
   * Name of the table used to track task-executions. Must match the database. Change name in the
   * table definitions accordingly when creating or modifying the table.
   */
  private String tableName = JdbcTaskRepository.DEFAULT_TABLE_NAME;

  /** True for check and create {@link #tableName} when not exists. */
  private boolean autoCreateTable = true;

  /**
   * If this is enabled, the scheduler will attempt to directly execute tasks that are scheduled to
   * {@code now()}, or a time in the past. For this to work, the call to {@code schedule(..)} must
   * not occur from within a transaction, because the record will not yet be visible to the
   * scheduler (if this is a requirement, see the method {@code
   * scheduler.triggerCheckForDueExecutions())}
   */
  private boolean immediateExecutionEnabled = false;

  /** How often the scheduler checks the database for due executions. */
  private Duration pollingInterval = SchedulerBuilder.DEFAULT_POLLING_INTERVAL;

  /** What polling-strategy to use. Valid values are: FETCH,LOCK_AND_FETCH */
  private PollingStrategyConfig.Type pollingStrategy =
      SchedulerBuilder.DEFAULT_POLLING_STRATEGY.type;

  /**
   * The limit at which more executions are fetched from the database after fetching a full batch.
   */
  private double pollingStrategyLowerLimitFractionOfThreads =
      SchedulerBuilder.DEFAULT_POLLING_STRATEGY.lowerLimitFractionOfThreads;

  /**
   * For Type=FETCH, the number of due executions fetched from the database in each batch.
   *
   * <p>For Type=LOCK_AND_FETCH, the maximum number of executions to pick and queue for execution.
   */
  private double pollingStrategyUpperLimitFractionOfThreads =
      SchedulerBuilder.DEFAULT_POLLING_STRATEGY.upperLimitFractionOfThreads;

  /** The time after which executions with unknown tasks are automatically deleted. */
  private Duration deleteUnresolvedAfter =
      SchedulerBuilder.DEFAULT_DELETION_OF_UNRESOLVED_TASKS_DURATION;

  /**
   * How long the scheduler will wait before interrupting executor-service threads. If you find
   * yourself using this, consider if it is possible to instead regularly check <code>
   * executionContext.getSchedulerState().isShuttingDown()</code> in the ExecutionHandler and abort
   * long-running task. Default is 1 second.
   */
  private Duration shutdownMaxWait = Duration.ofSeconds(1);

  /**
   * Store timestamps in UTC timezone even though the schema supports storing timezone information
   */
  private boolean alwaysPersistTimestampInUtc = true;

  /** Which log level to use when logging task failures. Defaults to {@link LogLevel#DEBUG}. */
  private LogLevel failureLoggerLevel = SchedulerBuilder.DEFAULT_FAILURE_LOG_LEVEL;

  /** Whether or not to log the {@link Throwable} that caused a task to fail. */
  private boolean failureLoggerLogStackTrace = SchedulerBuilder.LOG_STACK_TRACE_ON_FAILURE;

  /**
   * Attempt to parse a configuration options from prooperty files.
   *
   * <pre>
   *   # Set number of threads to use, default is to use the number of available processor
   *   db-scheduler.threads = 8
   *   db-scheduler.pollingInterval = 10s
   *   db-scheduler.alwaysPersistTimestampInUTC = true
   *   db-scheduler.enableImmediateExecution = false
   *   # No need to use registerShutdownHook, the scheduler is shutdown on application shutdown
   *   db-scheduler.registerShutdownHook = false
   *   db-scheduler.shutdownMaxWait = 1s
   * </pre>
   *
   * If the path doesn't exists, it returns a default instance.
   *
   * @param config Configuration.
   * @param path Root path.
   * @return Properties.
   */
  public static Optional<DbSchedulerProperties> from(Config config, String path) {
    if (config.hasPath(path)) {
      var properties = new DbSchedulerProperties();
      with(config, path, "enabled", (cfg, prop) -> properties.setEnabled(cfg.getBoolean(prop)));
      with(config, path, "threads", (cfg, prop) -> properties.setThreads(cfg.getInt(prop)));
      with(
          config,
          path,
          "heartbeatInterval",
          (cfg, prop) -> properties.setHeartbeatInterval(cfg.getDuration(prop)));
      with(
          config,
          path,
          "schedulerName",
          (cfg, prop) -> properties.setSchedulerName(cfg.getString(prop)));
      with(config, path, "tableName", (cfg, prop) -> properties.setTableName(cfg.getString(prop)));
      with(
          config,
          path,
          "autoCreateTable",
          (cfg, prop) -> properties.setAutoCreateTable(cfg.getBoolean(prop)));
      with(
          config,
          path,
          "immediateExecutionEnabled",
          (cfg, prop) -> properties.setImmediateExecutionEnabled(cfg.getBoolean(prop)));
      with(
          config,
          path,
          "pollingInterval",
          (cfg, prop) -> properties.setPollingInterval(cfg.getDuration(prop)));
      with(
          config,
          path,
          "pollingStrategy",
          (cfg, prop) ->
              properties.setPollingStrategy(
                  PollingStrategyConfig.Type.valueOf(cfg.getString(prop).toUpperCase())));
      with(
          config,
          path,
          "pollingStrategyLowerLimitFractionOfThreads",
          (cfg, prop) ->
              properties.setPollingStrategyLowerLimitFractionOfThreads(cfg.getDouble(prop)));
      with(
          config,
          path,
          "pollingStrategyUpperLimitFractionOfThreads",
          (cfg, prop) ->
              properties.setPollingStrategyUpperLimitFractionOfThreads(cfg.getDouble(prop)));
      with(
          config,
          path,
          "deleteUnresolvedAfter",
          (cfg, prop) -> properties.setDeleteUnresolvedAfter(cfg.getDuration(prop)));
      with(
          config,
          path,
          "shutdownMaxWait",
          (cfg, prop) -> properties.setShutdownMaxWait(cfg.getDuration(prop)));
      with(
          config,
          path,
          "alwaysPersistTimestampInUtc",
          (cfg, prop) -> properties.setAlwaysPersistTimestampInUtc(cfg.getBoolean(prop)));
      with(
          config,
          path,
          "failureLoggerLevel",
          (cfg, prop) ->
              properties.setFailureLoggerLevel(
                  LogLevel.valueOf(cfg.getString(prop).toUpperCase())));
      with(
          config,
          path,
          "failureLoggerLogStackTrace",
          (cfg, prop) -> properties.setFailureLoggerLogStackTrace(cfg.getBoolean(prop)));
      return Optional.of(properties);
    }
    return Optional.empty();
  }

  private static void with(
      Config config, String path, String property, BiConsumer<Config, String> consumer) {
    var propertyPath = path + "." + property;
    if (config.hasPath(propertyPath)) {
      consumer.accept(config, propertyPath);
    }
  }

  /**
   * Turn on/off the scheduler.
   *
   * @return True when on.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set false to turn off the scheduler.
   *
   * @param enabled True for on. Otherwise, false.
   * @return This instance.
   */
  public DbSchedulerProperties setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Number of threads to use. Default: <code>10</code>.
   *
   * @return Number of threads to use. Default: <code>10</code>.
   */
  public int getThreads() {
    return threads;
  }

  /**
   * Set the number of threads to use.
   *
   * @param threads Number of threads to use. Default: <code>10</code>.
   * @return This instance.
   */
  public DbSchedulerProperties setThreads(int threads) {
    this.threads = threads;
    return this;
  }

  /**
   * How often to update the heartbeat timestamp for running executions. Default 5m.
   *
   * @return How often to update the heartbeat timestamp for running executions. Default 5m.
   */
  public Duration getHeartbeatInterval() {
    return heartbeatInterval;
  }

  /**
   * Set how often to update the heartbeat timestamp for running executions.
   *
   * @param heartbeatInterval How often to update the heartbeat timestamp for running executions.
   * @return This instance.
   */
  public DbSchedulerProperties setHeartbeatInterval(@NonNull Duration heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
    return this;
  }

  /**
   * Name of this scheduler-instance. The name is stored in the database when an execution is picked
   * by a scheduler. Default: <code>hostname</code>.
   *
   * @return Name of this scheduler-instance. The name is stored in the database when an execution
   *     is picked by a scheduler. Default <code>hostname</code>.
   */
  public String getSchedulerName() {
    return schedulerName;
  }

  /**
   * Set name of this scheduler-instance. The name is stored in the database when an execution is
   * picked by a scheduler. Default <code>hostname</code>.
   *
   * @param schedulerName Scheduler's name.
   * @return This instance.
   */
  public DbSchedulerProperties setSchedulerName(@NonNull String schedulerName) {
    this.schedulerName = schedulerName;
    return this;
  }

  /**
   * Name of the table used to track task-executions. Change name in the table definitions
   * accordingly when creating the table. Default <code>scheduled_tasks</code>.
   *
   * @return Name of the table used to track task-executions.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Name of the table used to track task-executions.
   *
   * @param tableName Name of the table used to track task-executions. Default <code>scheduled_tasks
   *     </code>.
   * @return This module.
   */
  public DbSchedulerProperties setTableName(@NonNull String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * True for check and create {@link #tableName} when not exists.
   *
   * @return True for check and create {@link #tableName} when not exists.
   */
  public boolean isAutoCreateTable() {
    return autoCreateTable;
  }

  /**
   * For check and create {@link #tableName} when not exists.
   *
   * @param autoCreateTable true For check and create {@link #tableName} when not exists.
   * @return This instance.
   */
  public DbSchedulerProperties setAutoCreateTable(boolean autoCreateTable) {
    this.autoCreateTable = autoCreateTable;
    return this;
  }

  /**
   * If this is enabled, the scheduler will attempt to hint to the local Scheduler that there are
   * executions to be executed after they are scheduled to run now(), or a time in the past. NB: If
   * the call to schedule(..)/reschedule(..) occur from within a transaction, the scheduler might
   * attempt to run it before the update is visible (transaction has not committed). It is still
   * persisted though, so even if it is a miss, it will run before the next polling-interval. You
   * may also programmatically trigger an early check for due executions using the Scheduler-method
   * scheduler.triggerCheckForDueExecutions()). Default false.
   *
   * @return True when enabled. Default false.
   */
  public boolean isImmediateExecutionEnabled() {
    return immediateExecutionEnabled;
  }

  /**
   * Set to true to hint to the local Scheduler that there are executions to be executed after they
   * are scheduled to run now(), or a time in the past.
   *
   * @param immediateExecutionEnabled True for immediate execution. Default is false.
   * @return This instance.
   */
  public DbSchedulerProperties setImmediateExecutionEnabled(boolean immediateExecutionEnabled) {
    this.immediateExecutionEnabled = immediateExecutionEnabled;
    return this;
  }

  /**
   * How often the scheduler checks the database for due executions. Default 10s.
   *
   * @return How often the scheduler checks the database for due executions. Default 10s.
   */
  public Duration getPollingInterval() {
    return pollingInterval;
  }

  /**
   * Set How often the scheduler checks the database for due executions. Default 10s.
   *
   * @param pollingInterval How often the scheduler checks the database for due executions. Default
   *     10s.
   * @return This instance.
   */
  public DbSchedulerProperties setPollingInterval(@NonNull Duration pollingInterval) {
    this.pollingInterval = pollingInterval;
    return this;
  }

  /**
   * The time after which executions with unknown tasks are automatically deleted. These can
   * typically be old recurring tasks that are not in use anymore. This is non-zero to prevent
   * accidental removal of tasks through a configuration error (missing known-tasks) and problems
   * during rolling upgrades. Default 14d.
   *
   * @return The time after which executions with unknown tasks are automatically deleted.
   */
  public Duration getDeleteUnresolvedAfter() {
    return deleteUnresolvedAfter;
  }

  /**
   * Set the time after which executions with unknown tasks are automatically deleted.
   *
   * @param deleteUnresolvedAfter The time after which executions with unknown tasks are
   *     automatically deleted
   * @return This instance.
   */
  public DbSchedulerProperties setDeleteUnresolvedAfter(@NonNull Duration deleteUnresolvedAfter) {
    this.deleteUnresolvedAfter = deleteUnresolvedAfter;
    return this;
  }

  /**
   * How long the scheduler will wait before interrupting executor-service threads. If you find
   * yourself using this, consider if it is possible to instead regularly check
   * executionContext.getSchedulerState().isShuttingDown() in the ExecutionHandler and abort
   * long-running task. Default 1s .
   *
   * @return How long the scheduler will wait before interrupting executor-service threads.
   */
  public Duration getShutdownMaxWait() {
    return shutdownMaxWait;
  }

  /**
   * How long the scheduler will wait before interrupting executor-service threads.
   *
   * @param shutdownMaxWait How long the scheduler will wait before interrupting executor-service
   *     threads.
   * @return This instance.
   */
  public DbSchedulerProperties setShutdownMaxWait(@NonNull Duration shutdownMaxWait) {
    this.shutdownMaxWait = shutdownMaxWait;
    return this;
  }

  /**
   * Configures how to log task failures, i.e. Throwables thrown from a task execution handler. Use
   * log level OFF to disable this kind of logging completely. Default WARN, true.
   *
   * @return Failure logger level.
   */
  public LogLevel getFailureLoggerLevel() {
    return failureLoggerLevel;
  }

  /**
   * Configures how to log task failures.
   *
   * @param failureLoggerLevel Configures how to log task failures.
   * @return This instance.
   */
  public DbSchedulerProperties setFailureLoggerLevel(@NonNull LogLevel failureLoggerLevel) {
    this.failureLoggerLevel = failureLoggerLevel;
    return this;
  }

  /**
   * True for logging stack trace on failure.
   *
   * @return True for logging stack trace on failure.
   */
  public boolean isFailureLoggerLogStackTrace() {
    return failureLoggerLogStackTrace;
  }

  /**
   * Set True for logging stack trace on failure.
   *
   * @param failureLoggerLogStackTrace True for logging stack trace on failure.
   * @return This instance.
   */
  public DbSchedulerProperties setFailureLoggerLogStackTrace(boolean failureLoggerLogStackTrace) {
    this.failureLoggerLogStackTrace = failureLoggerLogStackTrace;
    return this;
  }

  /**
   * If you are running >1000 executions/s you might want to use the lock-and-fetch polling-strategy
   * for lower overhead and higher througput (read more). If not, the default
   * fetch-and-lock-on-execute will be fine.
   *
   * @return Polling strategy. Default is: <code>fetch-and-lock-on-execute</code>.
   */
  public PollingStrategyConfig.Type getPollingStrategy() {
    return pollingStrategy;
  }

  /**
   * Set polling strategy.
   *
   * @param pollingStrategy The polling strategy.
   * @return This instance.
   */
  public DbSchedulerProperties setPollingStrategy(
      @NonNull PollingStrategyConfig.Type pollingStrategy) {
    this.pollingStrategy = pollingStrategy;
    return this;
  }

  /**
   * The limit at which more executions are fetched from the database after fetching a full batch.
   *
   * @return The limit at which more executions are fetched from the database after fetching a full
   *     batch.
   */
  public double getPollingStrategyLowerLimitFractionOfThreads() {
    return pollingStrategyLowerLimitFractionOfThreads;
  }

  /**
   * Set the limit at which more executions are fetched from the database after fetching a full
   * batch.
   *
   * @param pollingStrategyLowerLimitFractionOfThreads The limit at which more executions are
   *     fetched from the database after fetching a full batch.
   * @return This intance.
   */
  public DbSchedulerProperties setPollingStrategyLowerLimitFractionOfThreads(
      double pollingStrategyLowerLimitFractionOfThreads) {
    this.pollingStrategyLowerLimitFractionOfThreads = pollingStrategyLowerLimitFractionOfThreads;
    return this;
  }

  /**
   * For Type=FETCH, the number of due executions fetched from the database in each batch. For
   * Type=LOCK_AND_FETCH, the maximum number of executions to pick and queue for execution.
   *
   * @return The number of due executions fetched from the database in each batch or the maximum
   *     number of executions to pick and queue for execution.
   */
  public double getPollingStrategyUpperLimitFractionOfThreads() {
    return pollingStrategyUpperLimitFractionOfThreads;
  }

  /**
   * Set the maximum number of executions to pick and queue for execution.
   *
   * @param pollingStrategyUpperLimitFractionOfThreads the maximum number of executions to pick and
   *     queue for execution.
   * @return Ths instance.
   */
  public DbSchedulerProperties setPollingStrategyUpperLimitFractionOfThreads(
      double pollingStrategyUpperLimitFractionOfThreads) {
    this.pollingStrategyUpperLimitFractionOfThreads = pollingStrategyUpperLimitFractionOfThreads;
    return this;
  }

  /**
   * The Scheduler assumes that columns for persisting timestamps persist Instants, not
   * LocalDateTimes, i.e. somehow tie the timestamp to a zone. However, some databases have limited
   * support for such types (which has no zone information) or other quirks, making "always store in
   * UTC" a better alternative. For such cases, use this setting to always store Instants in UTC.
   * PostgreSQL and Oracle-schemas is tested to preserve zone-information. MySQL and MariaDB-schemas
   * does not and should use this setting. NB: For backwards compatibility, the default behavior for
   * "unknown" databases is to assume the database preserves time zone. For "known" databases, see
   * the class AutodetectJdbcCustomization. Default is trye.
   *
   * @return Always persist timestamp in UTC. Default is true.
   */
  public boolean isAlwaysPersistTimestampInUtc() {
    return alwaysPersistTimestampInUtc;
  }

  /**
   * Set always persist timestamp in UTC. Default is true.
   *
   * @param alwaysPersistTimestampInUTC True for always persist in UTC.
   * @return This instance.
   */
  public DbSchedulerProperties setAlwaysPersistTimestampInUtc(boolean alwaysPersistTimestampInUTC) {
    this.alwaysPersistTimestampInUtc = alwaysPersistTimestampInUTC;
    return this;
  }
}
