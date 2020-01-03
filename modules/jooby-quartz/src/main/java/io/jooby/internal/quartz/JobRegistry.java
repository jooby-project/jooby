/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import io.jooby.Registry;
import org.quartz.JobKey;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JobRegistry {

  private static final ConcurrentMap<JobKey, JobRegistry> jobs = new ConcurrentHashMap<>();

  private Registry registry;

  private Method jobMethod;

  public JobRegistry(Registry registry, Method jobMethod) {
    this.registry = registry;
    this.jobMethod = jobMethod;
  }

  public Registry getRegistry() {
    return registry;
  }

  public Method getJobMethod() {
    return jobMethod;
  }

  public static void put(JobKey jobKey, Registry registry, Method jobMethod) {
    jobs.put(jobKey, new JobRegistry(registry, jobMethod));
  }

  public static JobRegistry get(JobKey jobKey) {
    return jobs.get(jobKey);
  }
}
