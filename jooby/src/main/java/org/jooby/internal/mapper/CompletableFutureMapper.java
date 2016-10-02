package org.jooby.internal.mapper;

import java.util.concurrent.CompletableFuture;

import org.jooby.Deferred;
import org.jooby.Route;

@SuppressWarnings("rawtypes")
public class CompletableFutureMapper implements Route.Mapper<CompletableFuture> {

  @SuppressWarnings("unchecked")
  @Override
  public Object map(final CompletableFuture future) throws Throwable {
    return new Deferred(deferred -> {
      future.whenComplete((value, x) -> {
        if (x != null) {
          deferred.reject((Throwable) x);
        } else {
          deferred.resolve(value);
        }
      });
    });
  }

}
