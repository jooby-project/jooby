/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;

import io.jooby.Registry;
import io.jooby.exception.RegistryException;

public class JobDelegate implements InterruptableJob {

  private static final Object[] NO_ARGS = new Object[0];

  private AtomicBoolean interrupted = new AtomicBoolean();

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    JobDetail detail = context.getJobDetail();
    context.put("interrupted", interrupted);
    JobKey key = detail.getKey();
    try {
      var scheduler = context.getScheduler();
      var schedulerContext = scheduler.getContext();
      var method = (Method) schedulerContext.get(key.toString());

      Registry registry = (Registry) context.getScheduler().getContext().get("registry");
      // Set registry
      context.put("registry", registry);
      Object job = newInstance(registry, method.getDeclaringClass());
      final Object result;
      final Object[] args =
          method.getParameterCount() > 0 ? new Object[method.getParameterCount()] : NO_ARGS;
      Class<?>[] parameterTypes = method.getParameterTypes();
      for (int i = 0; i < args.length; i++) {
        Class parameterType = parameterTypes[i];
        if (JobExecutionContext.class.isAssignableFrom(parameterType)) {
          args[i] = new ExtendedJobExecutionContextImpl(context, registry);
        } else if (parameterType == Registry.class) {
          args[i] = registry;
        } else {
          // must be AtomicBoolean we already check at early stage
          args[i] = interrupted;
        }
      }
      result = method.invoke(job, args);
      if (method.getReturnType() != void.class) {
        context.setResult(result);
      }
    } catch (InvocationTargetException ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex.getCause());
    } catch (Exception ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex);
    } finally {
      interrupted.set(false);
    }
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    interrupted.set(true);
  }

  private Object newInstance(Registry registry, Class<?> jobClass)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
    try {
      return registry.require(jobClass);
    } catch (RegistryException x) {
      if (jobClass.getDeclaredConstructors().length == 1
          && jobClass.getDeclaredConstructors()[0].getParameterCount() == 0) {
        return jobClass.getDeclaredConstructors()[0].newInstance();
      }
      throw x;
    }
  }
}
