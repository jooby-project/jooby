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
      Class<?> loadedClass = getClass().getClassLoader().loadClass(classname);
      String methodname = names[1];
      Object job = this.injector.getInstance(loadedClass);
      Method method = Arrays.stream(loadedClass.getDeclaredMethods())
        .filter(m-> m.getName().equals(methodname))
        .findFirst()
        .get();
      final Object result;
      if (method.getParameterCount() == 1) {
        result = method.invoke(job, context);
      } else {
        result = method.invoke(job);
      }
      if (method.getReturnType() != void.class) {
        context.setResult(result);
      }
    } catch (InvocationTargetException ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex.getCause());
    } catch (Exception ex) {
      throw new JobExecutionException("Job execution resulted in error: " + key, ex);
    }
  }

}
