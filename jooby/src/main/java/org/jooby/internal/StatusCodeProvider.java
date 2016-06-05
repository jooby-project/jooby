package org.jooby.internal;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.jooby.Err;
import org.jooby.Status;

import com.typesafe.config.Config;

public class StatusCodeProvider {

  private Config conf;

  @Inject
  public StatusCodeProvider(final Config conf) {
    this.conf = conf;
  }

  public Status apply(final Throwable cause) {
    if (cause instanceof Err) {
      return Status.valueOf(((Err) cause).statusCode());
    }
    /**
     * usually a class name, except for inner classes where '$' is replaced it by '.'
     */
    Function<Class<?>, String> name = type -> Optional.ofNullable(type.getDeclaringClass())
        .map(dc -> new StringBuilder(dc.getName())
            .append('.')
            .append(type.getSimpleName())
            .toString())
        .orElse(type.getName());

    Config err = conf.getConfig("err");
    int status = -1;
    Class<?> type = cause.getClass();
    while (type != Throwable.class && status == -1) {
      String classname = name.apply(type);
      if (err.hasPath(classname)) {
        status = err.getInt(classname);
      } else {
        type = type.getSuperclass();
      }
    }
    return status == -1 ? Status.SERVER_ERROR : Status.valueOf(status);
  }
}
