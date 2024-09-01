/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import io.jooby.Jooby;
import io.jooby.Registry;
import io.jooby.SneakyThrows;

public class BeanTasks {
  /**
   * Scan for {@link Scheduled} annotated method and creates {@link Tasks#recurring(String,
   * Schedule)} tasks for each of them.
   *
   * @param app Application.
   * @param beanType Bean Type.
   * @return All task from bean.
   * @param <T> Task Type.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Task<?>> List<T> recurring(Jooby app, Class<?> beanType) {
    return recurring(app, beanType, app::require, MethodHandles.publicLookup());
  }

  /**
   * Scan for {@link Scheduled} annotated method and creates {@link Tasks#recurring(String,
   * Schedule)} tasks for each of them.
   *
   * @param app Application.
   * @param bean Bean instance.
   * @return All task from bean.
   * @param <T> Task Type.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Task<?>> List<T> recurring(Jooby app, Object bean) {
    return recurring(
        app,
        bean.getClass(),
        type -> {
          if (type == bean.getClass()) return bean;
          else return app.require(type);
        },
        MethodHandles.publicLookup());
  }

  @SuppressWarnings("unchecked")
  private static <T extends Task<?>> List<T> recurring(
      Jooby app,
      Class<?> beanType,
      SneakyThrows.Function<Class, Object> factory,
      MethodHandles.Lookup lookup) {
    try {
      List<T> result = new ArrayList<>();
      var config = app.getConfig();
      for (var method : beanType.getDeclaredMethods()) {
        var scheduled = method.getAnnotation(Scheduled.class);
        if (scheduled != null) {
          var scheduleValue = scheduled.value();
          if (config.hasPath(scheduleValue)) {
            scheduleValue = config.getString(scheduleValue);
          }
          var schedule = DbScheduleParser.parseSchedule(scheduleValue);
          var taskName = taskName(beanType, method);
          var executionHandler =
              new MethodExecutionHandler(factory, beanType, method, lookup.unreflect(method));
          var builder =
              method.getReturnType() == void.class
                  ? Tasks.recurring(taskName, schedule)
                  : Tasks.recurring(taskName, schedule, method.getReturnType());
          var task =
              method.getReturnType() == void.class
                  ? builder.execute(executionHandler.toVoid())
                  : builder.executeStateful(executionHandler.toStateReturning());

          result.add((T) task);
        }
      }
      return result;
    } catch (IllegalAccessException cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  private static String taskName(Class<?> beanType, Method method) {
    var beanName = beanType.getSimpleName();
    return beanName.substring(0, 1).toLowerCase() + beanName.substring(1) + "." + method.getName();
  }

  @SuppressWarnings("rawtypes")
  private static class MethodExecutionHandler implements StateReturningExecutionHandler {
    private SneakyThrows.Function<Class, Object> factory;
    private Class beanType;
    private Method method;
    private MethodHandle handle;

    public MethodExecutionHandler(
        SneakyThrows.Function<Class, Object> factory,
        Class beanType,
        Method method,
        MethodHandle handle) {
      this.factory = factory;
      this.beanType = beanType;
      this.method = method;
      this.handle = handle;
    }

    public VoidExecutionHandler toVoid() {
      return this::execute;
    }

    public StateReturningExecutionHandler toStateReturning() {
      return this;
    }

    public Object execute(TaskInstance taskInstance, ExecutionContext executionContext) {
      try {
        var bean = factory.apply(beanType);
        var parameterTypes = method.getParameterTypes();
        var method = handle.bindTo(bean);
        for (Class<?> type : parameterTypes) {
          if (type == TaskInstance.class) {
            method = method.bindTo(taskInstance);
          } else if (type == ExecutionContext.class) {
            method = method.bindTo(executionContext);
          } else if (type == Registry.class) {
            method = method.bindTo(factory.apply(Registry.class));
          } else if (type == this.method.getReturnType()) {
            // must come from TaskInstance
            var data = taskInstance.getData();
            if (data == null || type.isInstance(data)) {
              method = method.bindTo(data);
            } else {
              throw new ClassCastException("Task data is not of type " + type.getName());
            }
          } else {
            method = method.bindTo(factory.apply(type));
          }
        }
        return method.invoke();
      } catch (Throwable cause) {
        throw SneakyThrows.propagate(cause);
      }
    }
  }
}
