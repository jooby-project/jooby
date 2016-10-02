package org.jooby.internal.mapper;

import java.util.concurrent.Callable;

import org.jooby.Deferred;
import org.jooby.Route;

@SuppressWarnings("rawtypes")
public class CallableMapper implements Route.Mapper<Callable> {

  @Override
  public Object map(final Callable callable) throws Throwable {
    return new Deferred(deferred -> {
      try {
        deferred.resolve(callable.call());
      } catch (Throwable x) {
        deferred.reject(x);
      }
    });
  }

}
