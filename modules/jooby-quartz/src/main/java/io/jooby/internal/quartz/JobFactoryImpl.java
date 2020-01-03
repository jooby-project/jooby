/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import io.jooby.Registry;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class JobFactoryImpl implements JobFactory {
  private final JobFactory next;

  private Registry registry;

  public JobFactoryImpl(Registry registry, JobFactory next) {
    this.registry = registry;
    this.next = next;
  }

  @Override public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
    try {
      return next.newJob(bundle, scheduler);
    } catch (SchedulerException x) {
      return registry.require(bundle.getJobDetail().getJobClass());
    }
  }
}
