/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.jdbc.AutodetectJdbcCustomization;
import com.github.kagkarlsson.scheduler.jdbc.JdbcCustomization;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.stats.StatsRegistry;
import com.github.kagkarlsson.scheduler.task.OnStartup;
import com.github.kagkarlsson.scheduler.task.Task;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.dbscheduler.ClassLoaderJavaSerializer;
import io.jooby.internal.dbscheduler.DbTable;

/**
 * Db-scheduler module: https://github.com/kagkarlsson/db-scheduler
 *
 * <pre>{@code
 * import io.jooby.dbscheduler.BeanTasks;
 *
 * {
 *     install(new HikariModule());
 *     install(new DbSchedulerModule(BeanTasks.recurring(this, SampleJob.class)));
 * }
 * }</pre>
 *
 * SampleJob.java:
 *
 * <pre>{@code
 * import io.jooby.dbscheduler.Scheduled;
 *
 * public class SampleJob {
 *
 *   @Scheduled("1m")
 *   public void everyMinute() {
 *     ...
 *   }
 * }
 *
 * }</pre>
 *
 * @since 3.2.10
 * @author edgar
 */
public class DbSchedulerModule implements Extension {
  private static final Predicate<Task<?>> SHOULD_BE_STARTED = task -> task instanceof OnStartup;
  private final List<Task<?>> tasks = new ArrayList<>();
  /* Optional configuration options: */
  private StatsRegistry statsRegistry;
  private SchedulerName schedulerName;
  private Serializer serializer;
  private ExecutorService executorService;
  private ExecutorService dueExecutor;
  private ScheduledExecutorService housekeeperExecutor;
  private JdbcCustomization jdbcCustomization;

  public DbSchedulerModule(@NonNull List<Task<?>> tasks) {
    this.tasks.addAll(tasks);
  }

  public DbSchedulerModule(@NonNull Task<?> task, Task<?>... tail) {
    this(Stream.concat(Stream.of(task), Stream.of(tail)).toList());
  }

  public DbSchedulerModule withTasks(@NonNull List<Task<?>> tasks) {
    this.tasks.addAll(tasks);
    return this;
  }

  public DbSchedulerModule withStatsRegistry(@NonNull StatsRegistry statsRegistry) {
    this.statsRegistry = statsRegistry;
    return this;
  }

  public DbSchedulerModule withSchedulerName(@NonNull SchedulerName schedulerName) {
    this.schedulerName = schedulerName;
    return this;
  }

  public DbSchedulerModule withSerializer(@NonNull Serializer serializer) {
    this.serializer = serializer;
    return this;
  }

  public DbSchedulerModule withExecutorService(@NonNull ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  public DbSchedulerModule withDueExecutor(@NonNull ExecutorService dueExecutor) {
    this.dueExecutor = dueExecutor;
    return this;
  }

  public DbSchedulerModule withHousekeeperExecutor(
      @NonNull ScheduledExecutorService housekeeperExecutor) {
    this.housekeeperExecutor = housekeeperExecutor;
    return this;
  }

  public DbSchedulerModule withJdbcCustomization(@NonNull JdbcCustomization jdbcCustomization) {
    this.jdbcCustomization = jdbcCustomization;
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) throws SQLException {
    var properties =
        DbSchedulerProperties.from(app.getConfig(), "db-scheduler")
            .orElseGet(DbSchedulerProperties::new);

    if (properties.isEnabled()) {
      var dataSource = app.require(DataSource.class);
      // Instantiate a new builder
      var builder = Scheduler.create(dataSource, nonStartupTasks(tasks));

      builder.threads(properties.getThreads());

      // Polling
      builder.pollingInterval(properties.getPollingInterval());

      // Polling strategy
      switch (properties.getPollingStrategy()) {
        case FETCH ->
            builder.pollUsingFetchAndLockOnExecute(
                properties.getPollingStrategyLowerLimitFractionOfThreads(),
                properties.getPollingStrategyUpperLimitFractionOfThreads());
        case LOCK_AND_FETCH ->
            builder.pollUsingLockAndFetch(
                properties.getPollingStrategyLowerLimitFractionOfThreads(),
                properties.getPollingStrategyUpperLimitFractionOfThreads());

        default ->
            throw new IllegalArgumentException(
                "Unknown polling-strategy: " + properties.getPollingStrategy());
      }

      builder.heartbeatInterval(properties.getHeartbeatInterval());

      // Use scheduler name implementation from customizer if available, otherwise use
      // configured scheduler name (String). If both is absent, use the library default
      var schedulerName =
          Optional.ofNullable(this.schedulerName)
              .orElseGet(() -> new SchedulerName.Fixed(properties.getSchedulerName()));
      builder.schedulerName(schedulerName);

      builder.tableName(properties.getTableName());

      // Use custom serializer if provided. Otherwise use devtools friendly serializer.
      var serializer =
          Optional.ofNullable(this.serializer)
              .orElseGet(() -> new ClassLoaderJavaSerializer(app.getClassLoader()));
      builder.serializer(serializer);

      JdbcCustomization jdbcCustomization =
          Optional.ofNullable(this.jdbcCustomization)
              .orElseGet(
                  () ->
                      new AutodetectJdbcCustomization(
                          dataSource, properties.isAlwaysPersistTimestampInUtc()));
      if (properties.isAutoCreateTable()) {
        createTableIfNotExists(jdbcCustomization.getName(), properties.getTableName(), dataSource);
      }

      // Use custom JdbcCustomizer if provided.
      builder.jdbcCustomization(jdbcCustomization);

      if (properties.isAlwaysPersistTimestampInUtc()) {
        builder.alwaysPersistTimestampInUTC();
      }

      if (properties.isImmediateExecutionEnabled()) {
        builder.enableImmediateExecution();
      }

      // Use custom executor service if provided
      Optional.ofNullable(executorService).ifPresent(builder::executorService);

      // Use custom due executor if provided
      Optional.ofNullable(dueExecutor).ifPresent(builder::dueExecutor);

      // Use housekeeper executor service if provided
      Optional.ofNullable(housekeeperExecutor).ifPresent(builder::housekeeperExecutor);

      builder.deleteUnresolvedAfter(properties.getDeleteUnresolvedAfter());

      // Add recurring jobs and jobs that implements OnStartup
      builder.startTasks(startupTasks(tasks));

      // Expose metrics
      Optional.ofNullable(statsRegistry).ifPresent(builder::statsRegistry);

      // Failure logging
      builder.failureLogging(
          properties.getFailureLoggerLevel(), properties.isFailureLoggerLogStackTrace());

      // Shutdown max wait
      builder.shutdownMaxWait(properties.getShutdownMaxWait());

      // Register listeners
      // schedulerListeners.forEach(builder::addSchedulerListener);

      // Register interceptors
      // executionInterceptors.forEach(builder::addExecutionInterceptor);
      var scheduler = builder.build();

      app.getServices().put(Scheduler.class, scheduler);

      app.onStarted(scheduler::start);

      app.onStop(scheduler::stop);
    } else {
      LoggerFactory.getLogger(Scheduler.class).info("Scheduler is not enabled");
    }
  }

  private static void createTableIfNotExists(String db, String tableName, DataSource dataSource)
      throws SQLException {
    try (var connection = dataSource.getConnection()) {
      try (var rs =
          connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
        if (!rs.next()) {
          // Table not found
          var databaseName = db.split("\\s")[0].toLowerCase();
          var createTable =
              switch (databaseName) {
                case "mariadb" -> DbTable.MariaDB;
                case "mssql" -> DbTable.MSSQL;
                case "postgresql" -> DbTable.POSTGRESQL;
                case "oracle" -> DbTable.ORACLE;
                case "mysql" -> DbTable.MY_SQL;
                // Assume mysql as default
                default -> DbTable.MY_SQL;
              };
          try (var createTableStt = connection.createStatement()) {
            createTableStt.execute(createTable);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Task<?> & OnStartup> List<T> startupTasks(List<Task<?>> tasks) {
    return tasks.stream().filter(SHOULD_BE_STARTED).map(task -> (T) task).toList();
  }

  private static List<Task<?>> nonStartupTasks(List<Task<?>> tasks) {
    return tasks.stream().filter(SHOULD_BE_STARTED.negate()).toList();
  }
}
