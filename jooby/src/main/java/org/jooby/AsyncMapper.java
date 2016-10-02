package org.jooby;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.jooby.internal.mapper.CallableMapper;
import org.jooby.internal.mapper.CompletableFutureMapper;

/**
 * <h1>async-mapper</h1>
 * <p>
 * Map {@link Callable} and {@link CompletableFuture} results to {@link Deferred Deferred API}.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   map(new AsyncMapper());
 *
 *   get("/callable", () -> {
 *     return new Callable<String> () {
 *       public String call() {
 *         return "OK";
 *       }
 *     };
 *   });
 *
 *   get("/completable-future", () -> {
 *     return CompletableFuture.supplyAsync(() -> "OK");
 *   });
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0
 */
@SuppressWarnings("rawtypes")
public class AsyncMapper implements Route.Mapper {

  @Override
  public Object map(final Object value) throws Throwable {
    if (value instanceof Callable) {
      return new CallableMapper().map((Callable) value);
    } else if (value instanceof CompletableFuture) {
      return new CompletableFutureMapper().map((CompletableFuture) value);
    }
    return value;
  }

}
