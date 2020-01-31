package examples;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class RouteReturnTypeApp extends Jooby {
  static class Statics {

    public static String computeStatic() {
      return "static";
    }
  }

  static class Instance {
    public String newInstance(int x, String v) {
      return "static";
    }
  }

  {
    path("/literal", () -> {
      get("/1", ctx -> {
        return "str";
      });

      get("/2", ctx -> {
        return 57;
      });

      get("/3", ctx -> {
        return null;
      });

      get("/4", ctx -> {
        return true;
      });
    });

    path("/call", () -> {
      get("/1", ctx -> {
        return new RouteReturnTypeApp();
      });

      get("/2", ctx -> {
        return Statics.computeStatic();
      });

      get("/3", ctx -> {
        Instance instance = new Instance();
        return instance.newInstance(0, "c");
      });

      get("/4", ctx -> something());

      get("/5", ctx -> throwsIAE());

      get("/6", ctx -> someGeneric());
    });

    path("/generic", () -> {
      get("/1", ctx ->
          supplyAsync(() -> ctx.query("n").intValue(1))
              .thenApply(x -> x * 2)
              .whenComplete((v, x) -> {
                ctx.render(v);
              })
      );

      get("/2", ctx -> {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(0)
            .thenApply(x -> x * 2)
            .thenApply(x -> x * 3);
        return future;
      });

      get("/3", ctx -> CompletableFuture
          .supplyAsync(() -> "foo"));

      get("/4", ctx -> {
        Callable<Byte> callable = () -> Byte.MIN_VALUE;
        return callable;
      });

      get("/5", ctx -> (Callable<Character>) () -> 'x');
    });

    path("/localvar", () -> {
      get("/1", ctx -> {
        String q = ctx.query("q").value();
        return q;
      });

      get("/2", ctx -> {
        int q = ctx.query("q").intValue();
        return q;
      });

      get("/3", ctx -> {
        String[] values = ctx.path("v").toList().toArray(new String[0]);

        return values;
      });

      get("/4", ctx -> {
        float[] values = {ctx.query("f1").floatValue(), ctx.query("f2").floatValue()};

        return values;
      });
    });

    path("/complexType", () -> {
      get("/1", ctx ->
          ctx.query("q").toList());

      get("/2", ctx -> {
        List<String> q = ctx.query("q").toList();
        return q;
      });

      get("/3", ctx -> {
        List<List<String>> values = new ArrayList<>();
        values.stream().filter(Objects::nonNull).toArray();
        return values;
      });
    });

    path("/multipleTypes", () -> {
      get("/1", ctx -> {
        if (ctx.isInIoThread()) {
          return new ArrayList<String>();
        } else {
          return new LinkedList<String>();
        }
      });

      get("/2", ctx -> {
        if (ctx.isInIoThread()) {
          return new ABean();
        } else {
          return new BBean();
        }
      });

      get("/3", ctx -> {
        Bean user;
        if (ctx.isInIoThread()) {
          user = new ABean();
          return user;
        } else {
          user = new BBean();
          return user;
        }
      });
    });

    path("/array", () -> {
      get("/1", ctx -> new boolean[]{true, false, false});

      get("/2", ctx -> new RouteReturnTypeApp[0]);

      get("/3", ctx -> new int[0]);

      get("/4", ctx -> new String[]{"foo", "bar"});
    });
  }

  private String throwsIAE() throws IllegalAccessException {
    throw new IllegalAccessException("no-access");
  }

  public String something() {
    throw new StatusCodeException(StatusCode.UNAUTHORIZED, "test");
  }

  public List<String> someGeneric() {
    return new ArrayList<>();
  }
}
