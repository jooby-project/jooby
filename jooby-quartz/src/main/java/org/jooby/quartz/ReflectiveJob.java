package org.jooby.quartz;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.google.inject.Injector;

public class ReflectiveJob implements Job {

  private Injector injector;

  @Inject
  public ReflectiveJob(final Injector injector) {
    this.injector = requireNonNull(injector, "An injector is required.");
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    JobDetail detail = context.getJobDetail();
    JobKey key = detail.getKey();
    try {
      String[] names = key.getName().split("\\.");
      String classname = key.getGroup() + "." + names[0];
      String methodname = names[1];
      Object job = this.injector.getInstance(getClass().getClassLoader().loadClass(classname));
      Method method = Arrays.stream(job.getClass().getDeclaredMethods())
        .filter(m-> m.getName().equals(methodname))
        .findFirst()
        .get();
      if (method.getParameterCount() == 1) {
        method.invoke(job, context);
      } else {
        method.invoke(job);
      }
    } catch (InvocationTargetException ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex.getCause());
    } catch (ClassNotFoundException | IllegalAccessException ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex);
    }
  }

}
