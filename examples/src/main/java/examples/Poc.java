package examples;

import io.jooby.Jooby;
import io.jooby.Route;
import io.reactivex.Single;

import javax.annotation.Nonnull;

public class Poc extends Jooby {

  {

    decorator(next -> ctx -> {
      return next.apply(ctx);
    });

    decorator(next -> ctx -> {
      return next.apply(ctx);
    });

    decorator(next -> ctx -> {
      Single<String> single = Single.just("v");
      single.doOnSuccess(value -> {
        next.apply(ctx);
      });
      return next.apply(ctx);
    });

  }
}
