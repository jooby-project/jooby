/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import io.jooby.internal.UrlParser;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;
import jakarta.inject.Inject;

public class Issue2525 {

  public class VC2525 implements Converter {
    @Override
    public Object convert(@NotNull Type type, @NotNull Value value, @NotNull ConversionHint hint) {
      return new MyID2525(value.value());
    }
  }

  public class MyID2525 {

    private String value;

    @Inject
    public MyID2525(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "MyID:" + value;
    }
  }

  public static class Foo2525 {
    public Integer a;
    public Integer b;

    public Foo2525(Integer a, Integer b) {
      Objects.requireNonNull(a);
      Objects.requireNonNull(b);

      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return "{" + "a=" + a + ", b=" + b + '}';
    }
  }

  public static class JavaBeanParam {
    private String foo;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

    @Override
    public String toString() {
      return foo;
    }
  }

  @Test
  public void shouldBeMoreClever() {
    queryString(
        "foo=1234",
        queryString -> {
          assertEquals("[1234]", queryString.toList(JavaBeanParam.class).toString());
        });
    queryString(
        "something=else&foo[0][a]=10&foo[0][b]=20&[0][a]=30&[0][b]=40",
        queryString -> {
          assertEquals("[{a=30, b=40}]", queryString.toList(Foo2525.class).toString());
          assertEquals("[{a=10, b=20}]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "id=1234",
        queryString -> {
          assertEquals("MyID:1234", queryString.get("id").to(MyID2525.class).toString());
        },
        Map.of(MyID2525.class, new VC2525()));
    queryString(
        "a=1&b=2&foo.a=3&foo.b=4",
        queryString -> {
          assertEquals("{a=1, b=2}", queryString.to(Foo2525.class).toString());
          assertEquals("{a=3, b=4}", queryString.get("foo").to(Foo2525.class).toString());
        });
    queryString(
        "something=else&foo[0][a]=10&foo[0][b]=20",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[{a=10, b=20}]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "something=else",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[]", queryString.get("foo").toList(Foo2525.class).toString());
        });
  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    queryString(queryString, consumer, Map.of());
  }

  private void queryString(
      String queryString, Consumer<QueryString> consumer, Map<Type, Converter> converters) {
    var factory = new ValueFactory();
    converters.forEach(factory::put);
    consumer.accept(UrlParser.queryString(factory, queryString));
  }
}
