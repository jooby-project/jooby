package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Value;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReturnTypeTest {

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

  interface User {
  }

  class BasicUser implements User {
  }

  class SuperUser implements User {
  }

  private RouteAnalyzer analyzer = new RouteAnalyzer(getClass().getClassLoader(), false);

  @Test
  public void literals() {
    assertType(boolean[].class, ctx -> new boolean[]{true, false, false});
    assertType(int[].class, ctx -> new int[]{1, 44, 67});
    assertType(String[].class, ctx -> new String[]{"foo", "bar"});
    assertType(ReturnTypeTest[].class, ctx -> new ReturnTypeTest[0]);
    assertType(char[].class, ctx -> new char[0]);
    assertType(int[].class, ctx -> new int[0]);
    assertType(byte[].class, ctx -> new byte[0]);
    assertType(short[].class, ctx -> new short[0]);
    assertType(long[].class, ctx -> new long[0]);
    assertType(float[].class, ctx -> new float[0]);
    assertType(double[].class, ctx -> new double[0]);

    assertType(String.class, ctx -> "string");

    assertType(Integer.class, ctx -> 1);

    assertType(Boolean.class, ctx -> true);

    assertType(Object.class, ctx -> null);

    assertType(ReturnTypeTest.class, ctx -> new ReturnTypeTest());

    assertType(String.class, ctx -> Statics.computeStatic());

    assertType(String.class, ctx -> {
      Instance instance = new Instance();
      return instance.newInstance(0, "x");
    });
  }

  @Test
  public void methodInvocation() {
    assertType(String.class, ctx -> ctx.path());

    assertType(String.class, Context::path);
  }

  @Test
  public void completableFuture() {
    assertType(CompletableFuture.class, ctx -> supplyAsync(() -> ctx.query("n").intValue(1))
        .thenApply(x -> x * 2)
        .whenComplete((v, x) -> {
          ctx.send(v);
        }));

    assertType(CompletableFuture.class, ctx -> CompletableFuture
        .supplyAsync(() -> "foo"));

    assertType(Reified.completableFuture(Integer.class), ctx -> {
      CompletableFuture<Integer> future = CompletableFuture.completedFuture(0)
          .thenApply(x -> x * 2)
          .thenApply(x -> x * 3);
      return future;
    });

    assertType(Reified.completableFuture(String.class), ctx ->
        CompletableFuture.supplyAsync(() -> 4)
            .thenApply(x -> x * 42)
            .thenApply(x -> x * 53)
            .thenApply(x -> x.toString())
    );
  }

  @Test
  public void callable() {
    assertType(Reified.getParameterized(Callable.class, Byte.class), ctx -> {
      Callable<Byte> callable = () -> Byte.MIN_VALUE;
      return callable;
    });

    assertType(Reified.getParameterized(Callable.class, Character.class), ctx ->
        (Callable<Character>) () -> 'x'
    );

    assertType(Reified.getParameterized(Callable.class, Object.class), ctx ->
        (Callable) () -> new ReturnTypeTest()
    );
  }

  @Test
  public void flowPublisher() {
    assertType(Reified.getParameterized(Flow.Publisher.class, Float.class), ctx -> {
      Flow.Publisher<Float> publisher = subscriber -> {
      };
      return publisher;
    });

    assertType(Reified.getParameterized(Flow.Publisher.class, Number.class), ctx -> newPublisher());

    assertType(Reified.getParameterized(Flow.Publisher.class, String.class),
        ctx -> newPublisher(ctx.query("q").value()));
  }

  private Flow.Publisher<Number> newPublisher() {
    return subscriber -> {
    };
  }

  private Flow.Publisher<String> newPublisher(String value) {
    return subscriber -> {
    };
  }

  @Test
  public void localVariable() {
    assertType(String.class, ctx -> {
      String q = ctx.query("q").value();
      return q;
    });

    assertType(Integer.class, ctx -> {
      int q = ctx.query("q").intValue();
      return q;
    });

    assertType(Double.class, ctx -> {
      Value value = ctx.param("f");

      Double to = value.to(Double.class);

      return to;
    });

    assertType(String[].class, ctx -> {
      String[] values = ctx.param("v").toList().toArray(new String[0]);

      compute(values);

      return values;
    });

    assertType(float[].class, ctx -> {
      float[] values = {ctx.query("f1").floatValue(), ctx.query("f2").floatValue()};

      return values;
    });
  }

  @Test
  public void complexType() {

    Reified<List<String>> listOfString = Reified.list(String.class);

    assertType(List.class, ctx ->
        ctx.query("q").toList()
    );

    assertType(listOfString, ctx -> {
      List<String> q = ctx.query("q").toList();
      return q;
    });

    assertType(Reified.getParameterized(List.class, listOfString.getType()), ctx -> {
      List<List<String>> values = new ArrayList<>();
      values.stream().filter(Objects::nonNull).toArray();
      return values;
    });

    assertType(Reified.map(String.class, listOfString.getType()), ctx -> {
      Map<String, List<String>> result = new HashMap<>();
      return result;
    });
  }

  @Test
  public void multipleReturnTypes() {
    assertType(List.class, ctx -> {
      if (ctx.isInIoThread()) {
        return new ArrayList<String>();
      } else {
        return new LinkedList<String>();
      }
    });

    assertType(Reified.list(String.class), ctx -> {
      List<String> values;
      if (ctx.isInIoThread()) {
        values = new ArrayList<>();
        return values;
      } else {
        values = new LinkedList<>();
        return values;
      }
    });

    assertType(User.class, ctx -> {
      if (ctx.isInIoThread()) {
        return new BasicUser();
      } else {
        return new SuperUser();
      }
    });

    assertType(User.class, ctx -> {
      User user;
      if (ctx.isInIoThread()) {
        user = new BasicUser();
        return user;
      } else {
        user = new SuperUser();
        return user;
      }
    });
  }

  private void assertType(Reified expected, Route.Handler handler) {
    assertType(expected.getType(), handler);
  }

  private void assertType(Type expected, Route.Handler handler) {
    assertEquals(expected.getTypeName(), analyzer.returnType(handler).getTypeName());
  }

  private int compute(String[] values) {
    return values.length * 2;
  }
}
