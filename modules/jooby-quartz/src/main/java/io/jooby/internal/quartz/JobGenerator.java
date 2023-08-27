/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.spi.MutableTrigger;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.Jooby;
import io.jooby.Registry;
import io.jooby.quartz.ExtendedJobExecutionContext;
import io.jooby.quartz.Scheduled;
import jakarta.inject.Named;

public class JobGenerator {

  private static final List<Class<?>> SUPPORTED_ARGS =
      Arrays.asList(
          JobExecutionContext.class,
          Registry.class,
          ExtendedJobExecutionContext.class,
          AtomicBoolean.class);

  public static List<Method> jobMethod(Class<?> jobClass) {
    List<Method> result = new ArrayList<>();
    for (Method method : jobClass.getDeclaredMethods()) {
      Scheduled scheduled = method.getAnnotation(Scheduled.class);
      if (scheduled != null) {
        int mods = method.getModifiers();
        if (!Modifier.isPublic(mods)) {
          throw new IllegalArgumentException("Job method must be public: " + method);
        }
        if (Modifier.isStatic(mods)) {
          throw new IllegalArgumentException("Job method should NOT be public: " + method);
        }
        result.add(method);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static Map<JobDetail, Trigger> build(final Jooby application, final List<Class<?>> jobs)
      throws NoSuchMethodException {
    Map<JobDetail, Trigger> triggers = new HashMap<>();
    Config config = application.getConfig();
    for (Class<?> job : jobs) {
      if (Job.class.isAssignableFrom(job)) {
        triggers.put(job((Class<? extends Job>) job), trigger(config, (Class<? extends Job>) job));
      } else {
        int size = triggers.size();
        for (Method method : jobMethod(job)) {
          Scheduled scheduled = method.getAnnotation(Scheduled.class);
          List<Class<?>> types = Stream.of(method.getParameterTypes()).collect(Collectors.toList());
          types.removeAll(SUPPORTED_ARGS);

          if (!types.isEmpty()) {
            throw new UnsupportedOperationException(
                "Argument(s) not supported on job method: "
                    + types
                    + " supported parameters are: "
                    + SUPPORTED_ARGS);
          }
          triggers.put(provisioningJob(method), newTrigger(config, scheduled, jobKey(method)));
        }
        if (size >= triggers.size()) {
          throw new IllegalArgumentException(format("Scheduled is missing on %s", job.getName()));
        }
      }
    }
    return triggers;
  }

  private static JobDetail job(final Class<? extends Job> jobType) throws NoSuchMethodException {
    return jobDetail(new JobDetailImpl(), jobKey(jobType), jobType);
  }

  private static JobDetail provisioningJob(final Method method) {
    var detail =
        jobDetail(new JobDetailImpl(), jobKey(method), findJobClass(method.getDeclaringClass()));
    detail.getJobDataMap().put("jobMethod", method);
    return detail;
  }

  private static Class<? extends Job> findJobClass(Class<?> declaringClass) {
    var persistent = declaringClass.getAnnotation(PersistJobDataAfterExecution.class);
    var nonConcurrent = declaringClass.getAnnotation(DisallowConcurrentExecution.class);
    if (persistent != null && nonConcurrent != null) {
      return StatefulJobDelegate.class;
    } else {
      if (persistent != null) {
        return PersistJobDataAfterJobDelegate.class;
      }
      if (nonConcurrent != null) {
        return DisallowConcurrentJobDelegate.class;
      }
      return JobDelegate.class;
    }
  }

  private static JobDetail jobDetail(
      JobDetailImpl detail, JobKey key, Class<? extends Job> jobClass) {
    detail.setJobClass(jobClass);
    detail.setKey(key);
    detail.setName(key.getName());
    return detail;
  }

  private static JobKey jobKey(final Class<?> jobType) throws NoSuchMethodException {
    return jobKey(jobType.getDeclaredMethod("execute", JobExecutionContext.class));
  }

  private static JobKey jobKey(final Method method) {
    String jobName =
        Optional.ofNullable(method.getAnnotation(Named.class))
            .map(Named::value)
            .orElse(method.getName());
    return JobKey.jobKey(jobName, method.getDeclaringClass().getSimpleName());
  }

  private static Trigger trigger(final Config config, final Class<? extends Job> jobType)
      throws NoSuchMethodException {
    Method execute = jobType.getDeclaredMethod("execute", JobExecutionContext.class);
    Scheduled scheduled = execute.getAnnotation(Scheduled.class);
    if (scheduled == null) {
      throw new IllegalArgumentException(
          format("Scheduled is missing on %s.%s()", jobType.getName(), execute.getName()));
    }
    return newTrigger(config, scheduled, jobKey(jobType));
  }

  private static Trigger newTrigger(
      final Config config, final Scheduled scheduled, final JobKey key) {
    ScheduledValue value = eval(key, config, scheduled.value());
    if (!scheduled.calendar().isEmpty()) {
      value.calendar = scheduled.calendar().trim();
    }
    value.priority = scheduled.priority();
    value.misfire = scheduled.misfire();
    return newTrigger(key, value);
  }

  public static Trigger newTrigger(final Config config, final String expr, final JobKey key) {
    return newTrigger(key, eval(key, config, expr));
  }

  private static Trigger newTrigger(final JobKey key, final ScheduledValue value) {
    // almost there
    TriggerBuilder<? extends Trigger> builder;
    if (value.cron != null) {
      // cron
      builder =
          TriggerBuilder.newTrigger()
              .withSchedule(misfire(value.misfire, CronScheduleBuilder.cronSchedule(value.cron)))
              .withDescription(cron(value.cron))
              .withIdentity(toTriggerKey(key));
    } else {
      SimpleScheduleBuilder sb =
          SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(value.interval);
      if (value.repeat > 0) {
        sb = sb.withRepeatCount((int) value.repeat);
      } else {
        sb = sb.repeatForever();
      }

      builder =
          TriggerBuilder.newTrigger()
              .withSchedule(misfire(value.misfire, sb))
              .withIdentity(toTriggerKey(key))
              .forJob(key)
              .withDescription(interval(value.interval, (int) value.repeat));
      if (value.delay > 0) {
        builder.startAt(new Date(System.currentTimeMillis() + value.delay));
      }
    }
    if (value.calendar != null) {
      builder.modifiedByCalendar(value.calendar.trim());
    }
    return builder.withPriority(value.priority).build();
  }

  private static TriggerKey toTriggerKey(JobKey key) {
    return TriggerKey.triggerKey(key.getName() + "Trigger", key.getGroup());
  }

  private static ScheduledValue eval(final JobKey key, final Config config, final String expr) {
    // full expression with possible delay and repeat values
    return (ScheduledValue)
        eval(
            config,
            expr,
            (values, resolved) -> {
              ScheduledValue value = new ScheduledValue();
              if (resolved instanceof Long) {
                value.interval = (Long) resolved;
              } else {
                value.cron = (String) resolved;
              }
              // attributes
              for (int i = 1; i < values.length; i++) {
                String[] attr = values[i].split("=");
                switch (attr[0].trim()) {
                  case "delay" -> value.delay = (Long) eval(config, attr[1], (v, r) -> r);
                  case "repeat" -> {
                    if (!"*".equals(attr[1].trim())) {
                      value.repeat = (Long) eval(config, attr[1], (v, r) -> r);
                    }
                  }
                  case "priority" -> value.priority = Integer.parseInt(attr[1].trim());
                  case "calendar" -> value.calendar = attr[1].trim();
                  case "misfire" -> value.misfire = Integer.parseInt(attr[1].trim());
                  default -> throw new IllegalArgumentException(
                      "Unknown attribute: " + attr[0] + " at " + key);
                }
              }
              return value;
            });
  }

  private static Object eval(
      final Config config, final String expr, final BiFunction<String[], Object, Object> mapper) {
    String value = expr.trim();
    try {
      value = config.getString(value);
    } catch (ConfigException.BadPath | ConfigException.Missing ex) {
      // shh
    }
    String[] values = value.split(";");
    Config eval = ConfigFactory.empty().withValue("expr", ConfigValueFactory.fromAnyRef(values[0]));
    try {
      return mapper.apply(values, eval.getDuration("expr", TimeUnit.MILLISECONDS));
    } catch (ConfigException.WrongType | ConfigException.BadValue ex) {
      return mapper.apply(values, value);
    }
  }

  private static String cron(String expression) {
    CronDescriptor descriptor = CronDescriptor.instance(Locale.US);
    CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
    CronParser parser = new CronParser(cronDefinition);
    String description = descriptor.describe(parser.parse(expression));
    return "run " + description + " (" + expression + ")";
  }

  private static String interval(long interval, int repeat) {
    StringBuilder buff = new StringBuilder();
    buff.append("run every ");
    TimeUnit[] units = {TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS};
    long[] values = {
      TimeUnit.HOURS.toMillis(1L), TimeUnit.MINUTES.toMillis(1L), TimeUnit.SECONDS.toMillis(1L)
    };
    int len = buff.length();
    for (int i = 0; i < values.length; i++) {
      long value = interval / values[i];
      if (value > 0) {
        interval(buff, value, units[i]);
        break;
      }
    }
    if (len != buff.length()) {
      buff.append(" (");
      interval(buff, interval, "ms");
      buff.append(")");
    } else {
      interval(buff, interval, "ms");
    }
    if (repeat > 0) {
      buff.append(" ").append(repeat).append(" times");
    }
    return buff.toString();
  }

  private static void interval(StringBuilder buff, long value, TimeUnit unit) {
    interval(buff, value, unit.name().toLowerCase());
  }

  private static void interval(StringBuilder buff, long value, String unit) {
    if (value > 1) {
      buff.append(value).append(" ").append(unit);
    } else {
      if (unit.length() > 2) {
        buff.append(unit, 0, unit.length() - 1);
      } else {
        buff.append(value).append(" ").append(unit);
      }
    }
  }

  private static ScheduleBuilder<CronTrigger> misfire(int misfire, CronScheduleBuilder builder) {
    return new ScheduleBuilder<CronTrigger>() {
      @Override
      protected MutableTrigger build() {
        MutableTrigger trigger = builder.build();
        trigger.setMisfireInstruction(misfire);
        return trigger;
      }
    };
  }

  private static ScheduleBuilder<SimpleTrigger> misfire(
      int misfire, SimpleScheduleBuilder builder) {
    return new ScheduleBuilder<SimpleTrigger>() {
      @Override
      protected MutableTrigger build() {
        MutableTrigger trigger = builder.build();
        trigger.setMisfireInstruction(misfire);
        return trigger;
      }
    };
  }

  private static class ScheduledValue {
    String cron;

    long interval;
    long delay;
    long repeat;
    String calendar;
    int priority = Trigger.DEFAULT_PRIORITY;
    int misfire = Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
  }
}
