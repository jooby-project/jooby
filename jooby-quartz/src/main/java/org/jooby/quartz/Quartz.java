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
package org.jooby.quartz;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.quartz.JobExpander;
import org.jooby.internal.quartz.QuartzProvider;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Build and create a {@link Scheduler Quartz Scheduler} and {@link Job jobs}.
 *
 * <h1>Getting Started</h1>
 *
 * <pre>
 *  {
 *    use(new Quartz().with(MyJob.class));
 *  }
 * </pre>
 *
 * Previous example will startup Quartz and schedule MyJob.
 *
 * <h1>Jobs</h1>
 * <p>
 * A job can implement the {@link Job} interface as described in the <a
 * href="http://quartz-scheduler.org/documentation">Quartz documentation</a>
 * </p>
 *
 * <p>
 * If you prefer to not implement the {@link Job} interface, all you have to do is to annotated a
 * method with the {@link Scheduled} annotation.
 * </p>
 * <p>
 * By default, job name is set the class name or to the method name. Default group is set to the
 * package name of the job class.
 * </p>
 *
 * <h2>Job methods</h2>
 * <p>
 * A job method must follow this rules:
 * </p>
 *
 * <ul>
 * <li>It must be a public method</li>
 * <li>Without a return value</li>
 * <li>Have ZERO arguments</li>
 * <li>or just ONE argument of type {@link JobExecutionContext}</li>
 * </ul>
 *
 * The next section will you show how to add a trigger to a job and some examples too.
 *
 * <h1>Triggers</h1>
 * <p>
 * Trigger are defined by the {@link Scheduled} annotation. The annotation defined a single and
 * required attributes, which is basically a trigger expression or a reference to it.
 * </p>
 *
 * <p>
 * Example 1: run every 10s
 * </p>
 *
 * <pre>
 *  public class MyJob implements Job {
 *    &#64;Scheduled("10s")
 *    public void execute(JobExecutionContext ctx) throws JobExecutionException {
 *      ...
 *    }
 *  }
 * </pre>
 *
 * <p>
 * Example 2: run every 10s (no {@link Job})
 * </p>
 *
 * <pre>
 *  public class MyJob {
 *    &#64;Scheduled("10s")
 *    public void doWork() {
 *      ...
 *    }
 *  }
 * </pre>
 *
 * <p>
 * The {@link Scheduled} define a {@link Trigger Quartz Trigger}. There you can put expressions
 * like: <code>5s</code>, <code>15minutes</code>, <code>2hours</code>, etc... or a CRON expression:
 * <code>0/3 * * * * ?</code>.
 * </p>
 * <p>
 * It is also possible to put the name of property:
 * </p>
 *
 * <pre>
 *  public class MyJob {
 *    &#64;Scheduled("job.expr")
 *    public void doWork() {
 *      ...
 *    }
 *  }
 * </pre>
 * <p>
 * And again the property: <code>job.expr</code> must be one of the previously described
 * expressions.
 * </p>
 *
 * <h1>Grouping jobs together</h1>
 * <p>
 * If you have two or more jobs doing something similar, it is possible to group all them into one
 * single class:
 * </p>
 *
 * <pre>
 *  public class MyJobs {
 *    &#64;Scheduled("5minutes")
 *    public void job1() {
 *      ...
 *    }
 *
 *    &#64;Scheduled("1h")
 *    public void job2() {
 *      ...
 *    }
 *  }
 * </pre>
 *
 * <h1>Dependency Injection</h1>
 * <p>
 * Not much to add here, just let you know jobs are created by Guice.
 * </p>
 *
 * <pre>
 *  public class MyJob {
 *
 *    private A a;
 *
 *    &#64;Inject
 *    public MyJob(A a) {
 *      this.a = a;
 *    }
 *
 *    &#64;Scheduled("5minutes")
 *    public void doWork() {
 *      this.a.doWork();
 *    }
 *  }
 * </pre>
 *
 * <p>
 * Injecting a {@link Scheduler}
 * </p>
 *
 * <pre>
 *  public class MyJobManager {
 *
 *    private Scheduler scheduler;
 *
 *    &#64;Inject
 *    public MyJobManager(Scheduler scheduler) {
 *      this.scheduler = scheduler;
 *    }
 *  }
 * </pre>
 *
 * <h1>Configuration</h1>
 * <p>
 * Example: Setting max number of threads
 * </p>
 *
 * <pre>
 *  # application.conf
 *  org.quartz.threadPool.threadCount = 1 # default is number of available processors
 * </pre>
 * <p>
 * Configuration follows the <a href="http://quartz-scheduler.org/documentation">Quartz
 * documentation</a>. The only difference is that you need to put add the properties on your
 * <code>*.conf</code> file, NOT in a custom <code>quartz.properties</code> file.
 * </p>
 *
 * <h2>Jdbc Store</h2>
 * <p>
 * Jdbc Store is fully supported but it depends on the <code>jooby-jdbc</code> module. So, in order
 * to use the Jdbc Store you need to follow these steps:
 * </p>
 *
 * <p>
 * 1. Install the Jdbc module:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jdbc());
 *   use(new Quartz(MyJob.class));
 * }
 * </pre>
 *
 * <p>
 * 2. Set the quartz properties:
 * </p>
 *
 * <pre>
 *  org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX
 *  org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
 *  org.quartz.jobStore.dataSource = db
 * </pre>
 *
 * <h1>Adding jobs programmatically</h1> When {@link Scheduled} isn't not enough and/or if you
 * prefer to build jobs manually, you can try
 * one of the available alternatives.
 *
 * <p>
 * Example 1: build the trigger and use default job naming
 * </p>
 *
 * <pre>
 *   {
 *     use(new Quartz()
 *      .with(MyJob.class, trigger {@literal ->} {
 *        trigger
 *          .withSchedule(withIntervalInDays(3))
 *          .startAt(futureDate(10, MINUTES));
 *      })
 *     );
 *   }
 * </pre>
 *
 * <p>
 * Example 2: build the job, the trigger and use default job naming
 * </p>
 *
 * <pre>
 *   {
 *     use(new Quartz()
 *      .with(MyJob.class, (job, trigger) {@literal ->} {
 *        job.withDescription("etc...");
 *
 *        trigger
 *          .withSchedule(withIntervalInDays(3))
 *          .startAt(futureDate(10, MINUTES));
 *      })
 *     );
 *   }
 * </pre>
 *
 * <p>
 * Example 3: build and set everything from scratch
 * </p>
 *
 * <pre>
 *   {
 *     use(new Quartz()
 *      .with(
 *        newJob(MyJob.class).withDescription("etc...")
 *          .build(),
 *        newTrigger()
 *          .withSchedule(withIntervalInDays(3))
 *          .startAt(futureDate(10, MINUTES))
 *          .build()
 *      })
 *     );
 *   }
 * </pre>
 *
 * Enjoy it!
 *
 * @author edgar
 * @since 0.5.0
 */
public class Quartz implements Jooby.Module {

  private List<Class<?>> jobs;

  private Map<JobDetail, Trigger> jobMap = new HashMap<>();

  /**
   * Creates a new {@link Quartz} module. Optionally add some jobs.
   *
   * @param jobs Jobs to setup. Optional.
   * @see #with(Class)
   */
  public Quartz(final Class<?>... jobs) {
    this.jobs = Lists.newArrayList(jobs);
  }

  /**
   * Schedule the provided job and trigger.
   *
   * @param job A job to schedule.
   * @param trigger A trigger for provided job.
   * @return This quartz instance.
   */
  public Quartz with(final JobDetail job, final Trigger trigger) {
    requireNonNull(job, "Job is required.");
    requireNonNull(trigger, "Trigger is required.");
    jobMap.put(job, trigger);
    return this;
  }

  /**
   * Setup and schedule the provided job, it might implement a {@link Job} and the {@link Scheduled}
   * annotation must be present.
   *
   * @param jobClass A jobClass to setup and schedule.
   * @return This quartz instance.
   */
  public Quartz with(final Class<?> jobClass) {
    jobs.add(jobClass);
    return this;
  }

  /**
   * Schedule the provided job and trigger. This method will setup a default name and group for
   * both.
   *
   * @param jobClass A jobClass to setup and schedule.
   * @param configurer A callback to setup the job and trigger.
   * @return This quartz instance.
   */
  public Quartz with(final Class<? extends Job> jobClass,
      final BiConsumer<JobBuilder, TriggerBuilder<Trigger>> configurer) {
    requireNonNull(jobClass, "Job class is required.");
    JobBuilder job = JobBuilder.newJob(jobClass)
        .withIdentity(
            JobKey.jobKey(jobClass.getSimpleName(), jobClass.getPackage().getName())
        );
    TriggerBuilder<Trigger> trigger = TriggerBuilder.newTrigger()
        .withIdentity(
            TriggerKey.triggerKey(jobClass.getSimpleName(), jobClass.getPackage().getName())
        );
    configurer.accept(job, trigger);
    return with(job.build(), trigger.build());
  }

  /**
   * Schedule the provided job and trigger. This method will setup a default name and group for
   * both.
   *
   * @param jobClass A jobClass to setup and schedule.
   * @param configurer A callback to setup the trigger.
   * @return This quartz instance.
   */
  public Quartz with(final Class<? extends Job> jobClass,
      final Consumer<TriggerBuilder<Trigger>> configurer) {
    requireNonNull(jobClass, "Job class is required.");
    JobBuilder job = JobBuilder.newJob(jobClass)
        .withIdentity(
            JobKey.jobKey(jobClass.getSimpleName(), jobClass.getPackage().getName())
        );
    TriggerBuilder<Trigger> trigger = TriggerBuilder.newTrigger()
        .withIdentity(
            TriggerKey.triggerKey(jobClass.getSimpleName(), jobClass.getPackage().getName())
        );
    configurer.accept(trigger);
    return with(job.build(), trigger.build());
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    jobMap.putAll(JobExpander.jobs(config, jobs));
    binder.bind(Scheduler.class).toProvider(QuartzProvider.class).asEagerSingleton();
    binder.bind(new TypeLiteral<Map<JobDetail, Trigger>>() {
    }).annotatedWith(Names.named("org.quartz.jobs")).toInstance(jobMap);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "quartz.conf")
        .withFallback(ConfigFactory.parseResources(Job.class, "quartz.properties"));
  }
}
