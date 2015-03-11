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

import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.jooby.Env;
import org.jooby.Jooby;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Quartz implements Jooby.Module {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private List<Class<?>> jobs;

  private Scheduler scheduler;

  private Config config;

  public Quartz(final Class<?>... jobs) {
    this.jobs = Lists.newArrayList(jobs);
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    try {
      this.config = config;

      this.scheduler = new StdSchedulerFactory(properties(config)).getScheduler();

      binder.bind(Scheduler.class).toInstance(scheduler);

      binder.bind(QuartzConfigurer.class).asEagerSingleton();

    } catch (Exception ex) {
      Throwables.propagateIfInstanceOf(ex, RuntimeException.class);
      throw new IllegalStateException("Can't create quartz", ex);
    }
  }

  private Properties properties(final Config config) {
    Properties props = new Properties();

    // dump
    config.getConfig("org.quartz").entrySet().forEach(
        e -> props.setProperty("org.quartz." + e.getKey(), e.getValue().unwrapped().toString()));

    String store = props.getProperty("org.quartz.jobStore.class");
    if (JobStoreTX.class.getName().equals(store)) {
      String ds = props.getProperty(QuartzConfigurer.DS);
      if (ds == null) {
        throw new IllegalArgumentException("Missing property: " + QuartzConfigurer.DS);
      }
    } else {
      props.remove("org.quartz.jobStore.dataSource");
    }

    return props;
  }

  @Override
  public void start() {
    try {
      scheduler.start();
      for (Entry<JobDetail, Trigger> job : JobExpander.triggers(config, jobs).entrySet()) {
        JobDetail detail = job.getKey();
        Trigger trigger = job.getValue();
        log.info("  {} {}", detail.getKey(), trigger.getDescription());
        scheduler.scheduleJob(detail, trigger);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Can't start quartz", ex);
    }
  }

  @Override
  public void stop() {
    try {
      if (scheduler != null) {
        scheduler.shutdown();
      }
    } catch (SchedulerException ex) {
      throw new IllegalStateException("Can't shutdown quartz", ex);
    }
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Job.class, "quartz.properties");
  }
}
