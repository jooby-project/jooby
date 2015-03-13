/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.quartz;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jooby.quartz.Scheduled;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JobExpander {

  @SuppressWarnings("unchecked")
  public static Map<JobDetail, Trigger> jobs(final Config config, final List<Class<?>> jobs) {
    Map<JobDetail, Trigger> triggers = new HashMap<>();
    for (Class<?> job : jobs) {
      if (Job.class.isAssignableFrom(job)) {
        triggers.put(
            job((Class<? extends Job>) job),
            trigger(config, (Class<? extends Job>) job)
            );
      } else {
        Method[] methods = job.getDeclaredMethods();
        int size = triggers.size();
        for (Method method : methods) {
          Scheduled scheduled = method.getAnnotation(Scheduled.class);
          if (scheduled != null) {
            int mods = method.getModifiers();
            if (!Modifier.isPublic(mods)) {
              throw new IllegalArgumentException("Job method must be public: " + method);
            }
            if (Modifier.isStatic(mods)) {
              throw new IllegalArgumentException("Job method should NOT be public: " + method);
            }
            if (method.getParameterCount() > 0) {
              if (method.getParameterCount() > 1) {
                throw new IllegalArgumentException("Job method args must be ZERO/ONE: "
                    + method);
              }
              if (method.getParameterTypes()[0] != JobExecutionContext.class) {
                throw new IllegalArgumentException("Job method args isn't a "
                    + JobExecutionContext.class.getName() + ": " + method);
              }
            }
            triggers.put(job(method), newTrigger(config, scheduled, jobKey(method)));
          }
        }
        checkArgument(size < triggers.size(), "Scheduled is missing on %s", job.getName());
      }
    }
    return triggers;
  }

  private static JobDetail job(final Class<? extends Job> jobType) {
    JobKey key = jobKey(jobType);
    return JobBuilder.newJob(jobType)
        .withIdentity(key)
        .build();
  }

  private static JobDetail job(final Method method) {
    JobDetailImpl detail = new MethodJobDetail(method);
    detail.setJobClass(ReflectiveJob.class);
    detail.setKey(jobKey(method));
    return detail;
  }

  private static JobKey jobKey(final Class<?> jobType) {
    return JobKey.jobKey(jobType.getSimpleName(), jobType.getPackage().getName());
  }

  private static JobKey jobKey(final Method method) {
    Class<?> klass = method.getDeclaringClass();
    String classname = klass.getSimpleName();
    klass = klass.getDeclaringClass();
    while (klass != null) {
      classname = klass.getSimpleName() + "$" + classname;
      klass = klass.getDeclaringClass();
    }
    return JobKey.jobKey(classname + "." + method.getName(),
        method.getDeclaringClass().getPackage().getName());
  }

  private static Trigger trigger(final Config config, final Class<? extends Job> jobType) {
    Method execute = Arrays.stream(jobType.getDeclaredMethods())
        .filter(m -> m.getName().equals("execute"))
        .findFirst()
        .get();
    Scheduled scheduled = execute.getAnnotation(Scheduled.class);
    checkArgument(scheduled != null, "Scheduled is missing on %s.%s()", jobType.getName(),
        execute.getName());
    return newTrigger(config, scheduled, jobKey(jobType));
  }

  private static Trigger newTrigger(final Config config, final Scheduled scheduled,
      final JobKey key) {
    Function<String, Boolean> hasPath = p -> {
      try {
        return config.hasPath(p);
      } catch (ConfigException.BadPath ex) {
        return false;
      }
    };

    String expr = scheduled.value();
    // hack
    final Object value;
    if (hasPath.apply(expr)) {
      value = intervalOrCron(config, expr);
    } else {
      value = intervalOrCron(
          ConfigFactory.empty().withValue("expr", ConfigValueFactory.fromAnyRef(expr)), "expr");
    }
    // almost there
    if (value instanceof String) {
      // cron
      return TriggerBuilder.newTrigger()
          .withSchedule(
              CronScheduleBuilder
                  .cronSchedule((String) value)
          )
          .withIdentity(TriggerKey.triggerKey(key.getName(), key.getGroup()))
          .build();
    } else {
      return TriggerBuilder.newTrigger()
          .withSchedule(
              SimpleScheduleBuilder
                  .simpleSchedule()
                  .withIntervalInMilliseconds((long) value)
                  .repeatForever()
          )
          .withIdentity(TriggerKey.triggerKey(key.getName(), key.getGroup()))
          .startNow()
          .build();
    }
  }

  private static Object intervalOrCron(final Config config, final String name) {
    try {
      return config.getDuration(name, TimeUnit.MILLISECONDS);
    } catch (ConfigException.WrongType | ConfigException.BadValue ex) {
      return config.getString(name);
    }
  }

}
