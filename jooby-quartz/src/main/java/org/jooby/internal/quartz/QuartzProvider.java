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

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Managed;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

public class QuartzProvider implements Provider<Scheduler>, Managed {

  public static final TypeLiteral<Provider<DataSource>> DS_TYPE =
      new TypeLiteral<Provider<DataSource>>() {
      };

  public static final String DS = StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".dataSource";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Scheduler scheduler;

  private Set<Entry<JobDetail, Trigger>> jobs;

  @Inject
  public QuartzProvider(final Injector injector, final Config config,
      @Named("org.quartz.jobs") final Map<JobDetail, Trigger> triggers) throws Exception {
    requireNonNull(injector, "An injector is required.");

    this.scheduler = new StdSchedulerFactory(properties(config)).getScheduler();
    this.jobs = triggers.entrySet();

    // override job factory
    scheduler.setJobFactory((bundle, sch) -> {
      JobDetail jobDetail = bundle.getJobDetail();
      Class<?> jobClass = jobDetail.getJobClass();

      return (Job) injector.getInstance(jobClass);
    });

    // hacky way of setting DS? quartz API sucks (it does too much or too little)
    if (config.hasPath(DS)) {
      String name = config.getString(DS);
      // get a provider, bc ds wont be ready yet.
      Provider<DataSource> ds = injector.getInstance(Key.get(DS_TYPE, Names.named(name)));
      DBConnectionManager.getInstance()
          .addConnectionProvider(name, new QuartzConnectionProvider(ds));
    }
  }

  @Override
  public void start() throws Exception {
    for (Entry<JobDetail, Trigger> job : jobs) {
      JobDetail detail = job.getKey();
      Trigger trigger = job.getValue();
      log.info("  {} {}", detail.getKey(), describe(trigger));
      scheduler.scheduleJob(detail, trigger);
    }
    scheduler.start();
  }

  @Override
  public void stop() throws Exception {
    scheduler.shutdown();
  }

  @Override
  public Scheduler get() {
    return scheduler;
  }

  private Properties properties(final Config config) {
    Properties props = new Properties();

    // dump
    config.getConfig("org.quartz").entrySet().forEach(
        e -> props.setProperty("org.quartz." + e.getKey(), e.getValue().unwrapped().toString()));

    String store = props.getProperty("org.quartz.jobStore.class");
    if (JobStoreTX.class.getName().equals(store)) {
      String ds = props.getProperty(DS);
      if (ds == null) {
        throw new IllegalArgumentException("Missing property: " + DS);
      }
    }

    return props;
  }

  private Object describe(final Trigger trigger) {
    if (trigger.getDescription() != null) {
      return trigger.getDescription();
    }
    if (trigger instanceof SimpleTrigger) {
      return "will fire every " + ((SimpleTrigger) trigger).getRepeatInterval() + "ms";
    }
    if (trigger instanceof CronTrigger) {
      return "will fire at " + ((CronTrigger) trigger).getCronExpression();
    }
    if (trigger instanceof CalendarIntervalTrigger) {
      CalendarIntervalTrigger calendar = (CalendarIntervalTrigger) trigger;
      return "will fire every " + calendar.getRepeatInterval() + " "
          + calendar.getRepeatIntervalUnit();
    }
    if (trigger instanceof DailyTimeIntervalTrigger) {
      DailyTimeIntervalTrigger daily = (DailyTimeIntervalTrigger) trigger;
      return "will fire every " + daily.getRepeatInterval() + " "
          + daily.getRepeatIntervalUnit();
    }
    return trigger;
  }
}
