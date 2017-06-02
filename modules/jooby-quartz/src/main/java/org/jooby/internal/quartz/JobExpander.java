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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

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
    String expr = scheduled.value();
    // hack
    Object value = eval(key, config, expr);
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
      Long[] interval = (Long[]) value;

      SimpleScheduleBuilder sb = SimpleScheduleBuilder
          .simpleSchedule()
          .withIntervalInMilliseconds(interval[0]);
      if (interval[2] > 0) {
        sb = sb.withRepeatCount(interval[2].intValue());
      } else {
        sb = sb.repeatForever();
      }

      return TriggerBuilder.newTrigger()
          .withSchedule(sb)
          .withIdentity(TriggerKey.triggerKey(key.getName(), key.getGroup()))
          .startAt(new Date(System.currentTimeMillis() + interval[1]))
          .build();
    }
  }

  private static Object eval(final JobKey key, final Config config, final String expr) {
    // full expression with possible delay and repeat values
    return eval(config, expr, (values, resolved) -> {
      if (resolved instanceof Long) {
        // interval with delay and repeat
        Long[] inverval = new Long[]{(Long) resolved, 0L, 0L };
        for (int i = 1; i < values.length; i++) {
          String[] attr = values[i].split("=");
          if ("delay".equals(attr[0].trim())) {
            inverval[1] = (Long) eval(config, attr[1], (v, r) -> r);
          } else if ("repeat".equals(attr[0].trim())) {
            if (!"*".equals(attr[1].trim())) {
              inverval[2] = (Long) eval(config, attr[1], (v, r) -> r);
            }
          } else {
            throw new IllegalArgumentException("Unknown attribute: " + attr[0] + " at " + key);
          }
        }
        return inverval;
      }
      return resolved;
    });
  }

  private static Object eval(final Config config, final String expr,
      final BiFunction<String[], Object, Object> mapper) {
    String value = expr.trim();
    try {
      value = config.getString(value);
    } catch (ConfigException.BadPath | ConfigException.Missing ex) {
      // shh
    }
    String[] values = value.split(";");
    Config eval = ConfigFactory.empty()
        .withValue("expr", ConfigValueFactory.fromAnyRef(values[0]));
    try {
      return mapper.apply(values, eval.getDuration("expr", TimeUnit.MILLISECONDS));
    } catch (ConfigException.WrongType | ConfigException.BadValue ex) {
      return mapper.apply(values, value);
    }
  }
}
