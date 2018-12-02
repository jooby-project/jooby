package apps;

import io.jooby.App;
import io.jooby.ExecutionMode;
import io.jooby.utow.Utow;

import java.util.concurrent.Executors;

public class DispatchApp extends App {
  {

    worker(Executors.newCachedThreadPool());

    filter(next -> ctx -> {
      System.out.println(Thread.currentThread().getName());
      return next.apply(ctx);
    });

    after((ctx, value) -> {
      Number n = (Number) value;
      return n.intValue() * 2;
    });

    get("/", ctx -> ctx.query("n").intValue(2));

    dispatch(() -> {
      get("/worker", ctx -> ctx.query("n").intValue(2));
    });
  }

  public static void main(String[] args) {
    new Utow()
        .deploy(new DispatchApp().mode(ExecutionMode.EVENT_LOOP))
        .start()
        .join();
  }
}
