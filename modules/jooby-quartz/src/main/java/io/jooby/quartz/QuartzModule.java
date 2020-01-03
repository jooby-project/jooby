/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.quartz;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import io.jooby.internal.quartz.ConnectionProviderImpl;
import io.jooby.internal.quartz.JobFactoryImpl;
import io.jooby.internal.quartz.JobGenerator;
import io.jooby.internal.quartz.JobMethodDetail;
import io.jooby.internal.quartz.JobRegistry;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.utils.DBConnectionManager;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_INSTANCE_ID;

/**
 * Scheduler module using Quartz: http://www.quartz-scheduler.org.
 *
 * <pre>{@code
 * {
 *    install(new QuartzModule(SampleJob.class));
 *
 * }
 *
 * public class SampleJob implements Job {
 *
 *   #64;Scheduled("10m")
 *   public void execute(JobExecutionContext context) {
 *
 *   }
 * }
 * }</pre>
 *
 * Implementation of {@link org.quartz.Job} is optional:
 *
 * <pre>{@code
 * public class MyJobs {
 *
 *   #64;Scheduled("1m")
 *   public void everyMinute() {
 *     ....
 *   }
 *
 *   #64;Scheduled("1h")
 *  *   public void everyHour() {
 *  *     ....
 *  *   }
 * }
 * }</pre>
 *
 * Cron expression are supported too. Check the {@link Scheduled} annotation for possible schedule
 * expressions.
 *
 * Job key are generated from container class and method name. Example: SampleJob.execute,
 * MyJob.everyMinute, MyJob.everyHour.
 *
 * Jobs can be enabled/disabled (paused) at start up time by setting the <code>enabled</code>
 * property for each job key:
 *
 * <pre>
 *   org.quartz.jobs.SampleJob.execute.enabled = false
 * </pre>
 *
 * Now the <code>SampleJob.execute</code> is going to be paused at startup time.
 *
 * The {@link QuartzApp} added a REST API to trigger, interrupt, pause, resume jobs.
 *
 * @author edgar
 * @since 2.5.1
 */
public class QuartzModule implements Extension {

  private List<Class<?>> jobs;
  private Scheduler scheduler;

  /**
   * Creates Quartz module and register the given jobs.
   *
   * @param jobs Job classes.
   */
  public QuartzModule(final Class<?>... jobs) {
    this.jobs = Arrays.asList(jobs);
  }

  /**
   * Creates Quartz module and register the given jobs. Uses an user provided schedule, schedule
   * is started at application start up time and shutdown on application shutdown.
   *
   * @param scheduler Provided scheduler.
   * @param jobs Job classes.
   */
  public QuartzModule(@Nonnull Scheduler scheduler, final Class<?>... jobs) {
    this.scheduler = scheduler;
    this.jobs = Arrays.asList(jobs);
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Config config = application.getConfig();
    Map<JobDetail, Trigger> jobMap = JobGenerator.build(application, jobs);

    Properties properties = properties(config);

    Scheduler scheduler = this.scheduler == null
        ? newScheduler(application)
        : this.scheduler;

    jobMap.keySet().stream()
        .filter(JobMethodDetail.class::isInstance)
        .map(JobMethodDetail.class::cast)
        .forEach(detail -> {
          /** We need a registry in case of non-ram store: */
          JobRegistry.put(detail.getKey(), application, detail.getJobMethod());
        });

    ServiceRegistry services = application.getServices();
    services.putIfAbsent(Scheduler.class, scheduler);
    String schedulerName = scheduler.getSchedulerName();
    if (services.put(ServiceKey.key(Scheduler.class, schedulerName), scheduler) != null) {
      throw new IllegalStateException("Scheduler already exists: " + schedulerName);
    }
    application.onStarted(() -> {
      for (Map.Entry<JobDetail, Trigger> e : jobMap.entrySet()) {
        JobDetail jobDetail = e.getKey();
        Trigger trigger = e.getValue();
        boolean jobEnabled = isJobPaused(properties, jobDetail.getKey());
        if (scheduler.checkExists(jobDetail.getKey())) {
          // make sure trigger is updated
          scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else {
          scheduler.scheduleJob(jobDetail, trigger);
        }
        if (jobEnabled) {
          application.getLog()
              .info("{} {}", jobDetail.getKey(), trigger.getDescription());
        } else {
          scheduler.pauseJob(jobDetail.getKey());
          application.getLog()
              .info("{} {} (PAUSED)", jobDetail.getKey(), trigger.getDescription());
        }
      }
      if (!scheduler.isStarted()) {
        scheduler.start();
      }
    });
    boolean waitForJobsToComplete = Boolean
        .parseBoolean(properties.getProperty("org.quartz.scheduler.waitForJobsToComplete"));
    application.onStop(() -> scheduler.shutdown(waitForJobsToComplete));
  }

  /**
   * Creates a new scheduler.
   *
   * @param application Application.
   * @return New scheduler.
   */
  public static @Nonnull Scheduler newScheduler(@Nonnull Jooby application) {
    try {
      Scheduler scheduler = newScheduleFactory(application).getScheduler();
      scheduler.setJobFactory(new JobFactoryImpl(application, new PropertySettingJobFactory()));
      return scheduler;
    } catch (SchedulerException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  /**
   * Creates a new scheduler factory.
   *
   * @param application Application.
   * @return New scheduler factory.
   */
  public static @Nonnull StdSchedulerFactory newScheduleFactory(@Nonnull Jooby application) {
    try {
      Properties properties = properties(application.getConfig());
      if (JobStoreTX.class.getName().equals(properties.getProperty("org.quartz.jobStore.class"))) {
        configureJdbcStore(application, properties);
      }
      return new StdSchedulerFactory(properties);
    } catch (SchedulerException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  private static boolean isJobPaused(Properties properties, JobKey key) {
    return Stream.of(
        "org.quartz.jobs." + key.toString() + ".enabled",
        "org.quartz.jobs." + key.getGroup() + ".enabled"
    ).map(k -> properties.getProperty(k))
        .filter(Objects::nonNull)
        .findFirst()
        .map(v -> v.equals("true"))
        .orElse(true);
  }

  private static Properties properties(final Config config) {
    Properties props = new Properties();

    props.setProperty("org.quartz.scheduler.waitForJobsToComplete", "false");

    hostName().ifPresent(hostname -> props.setProperty(PROP_SCHED_INSTANCE_ID, hostname));

    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");

    props.setProperty("org.quartz.threadPool.threadNamePrefix", "scheduler");
    props.setProperty("org.quartz.threadPool.threadCount",
        Integer.toString(Runtime.getRuntime().availableProcessors()));

    if (config.hasPath("org.quartz")) {
      // dump
      config.getConfig("org.quartz").entrySet().forEach(
          e -> props.setProperty("org.quartz." + e.getKey(), e.getValue().unwrapped().toString()));
    }
    return props;
  }

  private static Optional<String> hostName() {
    try {
      return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      return Optional.empty();
    }
  }

  private static void configureJdbcStore(Jooby application, Properties properties) {
    String dataSourceName = properties.getProperty("org.quartz.jobStore.dataSource");
    ServiceRegistry registry = application.getServices();
    DataSource dataSource = Optional.ofNullable(dataSourceName)
        .map(key -> registry.getOrNull(ServiceKey.key(DataSource.class, key)))
        .orElseGet(() -> registry.getOrNull(DataSource.class));
    if (dataSource == null) {
      // TODO: replace with usage exception
      throw new IllegalArgumentException("DataSource not found: " + dataSourceName);
    }
    String dataSourceKey = Optional.ofNullable(dataSourceName).orElse("db");
    properties.setProperty("org.quartz.jobStore.dataSource", dataSourceKey);
    DBConnectionManager
        .getInstance().addConnectionProvider(dataSourceKey, new ConnectionProviderImpl(dataSource));
  }
}
